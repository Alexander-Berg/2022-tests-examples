import os
import multiprocessing
import time
import json
from maps.b2bgeo.libs.py_unistat import global_unistat as b2bgeo_unistat
from infra.yasm.unistat import SuffixType, AggregationType, global_unistat as yasm_unistat


def reset():
    b2bgeo_unistat.reset()
    yasm_unistat.reset()


def create_histogram(name, suffix=SuffixType.Histogram, intervals=None, priority=10, always_visible=False):
    b2bgeo_unistat.create_histogram(name, suffix=suffix, intervals=intervals, priority=priority, always_visible=always_visible)
    yasm_unistat.create_histogram(name, suffix=suffix, intervals=intervals, priority=priority, always_visible=always_visible)


def create_float(name, suffix=SuffixType.Sum, priority=10, always_visible=False, aggregation_type=AggregationType.Sum):
    b2bgeo_unistat.create_float(name, suffix=suffix, priority=priority, always_visible=always_visible, aggregation_type=aggregation_type)
    yasm_unistat.create_float(name, suffix=suffix, priority=priority, always_visible=always_visible, aggregation_type=aggregation_type)


def push_signal(name, value):
    b2bgeo_unistat.push(name, value)
    yasm_unistat.push(name, value)


def compare_json_signals(j1, j2):
    l1 = json.loads(j1)
    l2 = json.loads(j2)
    assert l1 == l2


def check(level=None, all_signals=None):
    if level is None:
        level = 0
    if all_signals is None:
        all_signals = False
    yasm_unistat_value = yasm_unistat.to_json(level, all_signals)
    b2bgeo_unistat_value = b2bgeo_unistat.to_json(level, all_signals)
    compare_json_signals(yasm_unistat_value, b2bgeo_unistat_value)


def test_empty():
    reset()
    check()
    compare_json_signals(b2bgeo_unistat.to_json(), b"[]")

    create_histogram("test_histogram_empty")
    create_float("test_float_empty")
    check()
    check(level=0)
    check(level=10)


def test_simple_signal():
    reset()
    create_histogram("test_histogram", intervals=[0, 1, 5, 10])
    create_float("test_float_sum", aggregation_type=AggregationType.Sum)
    create_float("test_float_last", aggregation_type=AggregationType.LastValue)

    push_signal("test_histogram", 3)
    push_signal("test_float_sum", 1)
    push_signal("test_float_last", 2)
    check()

    push_signal("test_histogram", 3)
    push_signal("test_float_sum", 3)
    push_signal("test_float_last", 4)
    check()


def test_priority():
    reset()
    create_histogram("test_histogram", intervals=[0, 1, 5, 10], priority=5)
    create_float("test_float", priority=5)

    push_signal("test_histogram", 7)
    push_signal("test_float", 3)
    compare_json_signals(yasm_unistat.to_json(), b2bgeo_unistat.to_json())
    compare_json_signals(yasm_unistat.to_json(level=2), b2bgeo_unistat.to_json(level=2))
    compare_json_signals(yasm_unistat.to_json(level=8), b2bgeo_unistat.to_json(level=8))


def test_histogram_bucket():
    reset()
    create_histogram("test", intervals=[0])
    push_signal("test", -1.5)
    push_signal("test", 0)
    push_signal("test", 1.5)
    check()


def test_multiprocess():
    reset()
    create_histogram("test_histogram", intervals=[0, 2, 5, 10, 20])
    create_float("test_float_sum", aggregation_type=AggregationType.Sum)
    create_float("test_float_last", aggregation_type=AggregationType.LastValue)

    new_pid = os.fork()
    finished = multiprocessing.Value('i', 0)
    if new_pid == 0:
        # child process
        push_signal("test_histogram", 1)
        push_signal("test_histogram", 7)
        push_signal("test_float_sum", 2)
        push_signal("test_float_last", 1)
        finished.value = 1
        os._exit(0)
    else:
        # parent process
        push_signal("test_histogram", 7)
        push_signal("test_float_sum", 3)

    for i in range(10):
        if finished.value == 0:
            time.sleep(0.1)
    push_signal("test_float_last", 3)

    compare_json_signals(
        yasm_unistat.to_json(),
        b'[["test_float_last_summ",3],["test_float_sum_summ",3],["test_histogram_dhhh",[[0,0],[2,0],[5,1],[10,0],[20,0]]]]'
    )
    compare_json_signals(
        b2bgeo_unistat.to_json(),
        b'[["test_float_last_summ",3],["test_float_sum_summ",5],["test_histogram_dhhh",[[0,1],[2,0],[5,2],[10,0],[20,0]]]]'
    )

    finished.value = 0

    new_pid = os.fork()
    if new_pid == 0:
        # child process
        push_signal("test_histogram", 3)
        push_signal("test_float_sum", 5)
        push_signal("test_float_last", 8)
        finished.value = 1
        os._exit(0)
    else:
        # parent process
        push_signal("test_histogram", 15)
        push_signal("test_float_sum", 10)

    for i in range(10):
        if finished.value == 0:
            time.sleep(0.1)
    push_signal("test_float_last", 32)

    assert finished.value == 1
    compare_json_signals(
        b2bgeo_unistat.to_json(),
        b'[["test_float_last_summ",32],["test_float_sum_summ",20],["test_histogram_dhhh",[[0,1],[2,1],[5,2],[10,1],[20,0]]]]'
    )

    b2bgeo_unistat.reset_signals()
    assert b2bgeo_unistat.to_json() == b'[]'


# https://st.yandex-team.ru/BBGEO-6306
def test_float_limit():
    reset()
    create_float("test_float_sum", aggregation_type=AggregationType.Sum)
    push_signal("test_float_sum", 2**24)
    compare_json_signals(b2bgeo_unistat.to_json(),
                         f'[["test_float_sum_summ", {2**24}]]'.encode())
    push_signal("test_float_sum", 1)
    compare_json_signals(b2bgeo_unistat.to_json(),
                         f'[["test_float_sum_summ", {2**24+1}]]'.encode())
