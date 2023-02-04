# -*- coding: utf-8 -*-
import pytest
import time
from datetime import timedelta
from configs import SmartPingerConfig

from balancer.test.util import asserts

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, ThreeModeConfig
from balancer.test.util.proto.http.stream import HTTPReaderException


def do_requests(ctx, count, sleep=0):
    for _ in range(count):
        try:
            ctx.perform_request(http.request.get())
        except HTTPReaderException:
            pass
        time.sleep(sleep)


def test_good_at_startup(ctx):
    """
    После старта балансер должен находиться в хорошем состоянии
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def test_good_stable(ctx):
    """
    BALANCER-542
    Если бэкенд всегда отвечает, то балансер не должен переходить в плохое состояние и отдавать ответ из on_disable
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.6, hi=1.0, ttl=3,
                                         min_samples_to_disable=1, delay=2))
    for _ in range(10):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)


def test_below_lo(ctx):
    """
    Если процент успешных запросов упал ниже lo,
    то балансер должен перейти в плохое состояние
    """
    backend = ctx.start_backend(ThreeModeConfig(first=6, second=4))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.7, hi=0.9, ttl=100,
                                         min_samples_to_disable=10, delay=100))
    do_requests(ctx, 10)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_above_lo(ctx):
    """
    Если процент успешных запросов не падал ниже lo,
    то балансер должен остаться в хорошем состоянии
    """
    backend = ctx.start_backend(ThreeModeConfig(first=8, second=2))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.7, hi=0.9, ttl=100,
                                         min_samples_to_disable=10, delay=100))
    do_requests(ctx, 10)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def test_below_hi(ctx):
    """
    Если после перехода в плохое состояние процент успешных запросов не превысил hi,
    то балансер должен остаться в плохом состоянии
    """
    backend = ctx.start_backend(ThreeModeConfig(first=1, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.6, hi=0.7, ttl=100,
                                         min_samples_to_disable=10, delay=0.2))
    do_requests(ctx, 10)
    time.sleep(2)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_above_hi(ctx):
    """
    Если после перехода в плохое состояние процент успешных запросов превысил hi,
    то балансер должен перейти в хорошее состояние
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=10, first=1, second=0))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.3, hi=0.4, ttl=100,
                                         min_samples_to_disable=10, delay=0.1))
    do_requests(ctx, 10)
    time.sleep(2)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def test_ping_request_data(ctx):
    """
    В плохом состоянии балансер должен отправлять бэкенду пинговые запросы,
    указанные в ping_request_data
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=10, first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=1.0, hi=1.0, ttl=100,
                                         min_samples_to_disable=1, delay=0.1))
    do_requests(ctx, 1)
    time.sleep(0.3)
    backend.state.get_request()
    req = backend.state.get_request()
    asserts.path(req, '/ping')


def test_on_disable(ctx):
    """
    В плохом состоянии балансер должен отдавать ответ из on_disable
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=10, first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=1.0, hi=1.0, ttl=100,
                                         min_samples_to_disable=1, delay=0.1))
    do_requests(ctx, 1)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_no_on_disable(ctx):
    """
    Если балансер в плохом состоянии и отсутствует секция on_disable,
    то балансер должен закрыть соединение с клиентом
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=10, first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, on_disable=False, lo=1.0, hi=1.0, ttl=100,
                                         min_samples_to_disable=1, delay=0.1))
    do_requests(ctx, 1)
    ctx.perform_request_xfail(http.request.get())


def test_not_enough_min_samples_to_disable(ctx):
    """
    Если запросов набралось меньше, чем min_samples_to_disable,
    то балансер не переходит в плохое состояние, даже если процент успешных запросов ниже lo
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=9, first=2, second=10))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=1.0, hi=1.0, ttl=100,
                                         min_samples_to_disable=20, delay=0.1))
    do_requests(ctx, 10)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def test_ttl_disable(ctx):
    """
    При переходе из хорошего в плохое состояние должны учитываться только запросы,
    попадающие в ttl
    """
    backend = ctx.start_backend(ThreeModeConfig(first=5, second=20))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.6, hi=1.0, ttl=3,
                                         min_samples_to_disable=2, delay=2))
    do_requests(ctx, 4)
    time.sleep(2)
    do_requests(ctx, 1)
    ctx.perform_request_xfail(http.request.get())
    time.sleep(2)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_ttl_enable(ctx):
    """
    При переходе из плохого в хорошее состояние должны учитываться только запросы,
    попадающие в ttl
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=3, first=1, second=0))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.3, hi=0.4, ttl=3,
                                         min_samples_to_disable=2, delay=2))
    do_requests(ctx, 2)
    time.sleep(2)
    resp1 = ctx.perform_request(http.request.get())
    asserts.status(resp1, 500)
    time.sleep(3)
    resp2 = ctx.perform_request(http.request.get())
    asserts.status(resp2, 200)


def test_ttl_min_samples(ctx):
    """
    Если балансер находится в плохом состоянии и число запросов, не старше ttl
    стало меньше min_samples_to_disable, то балансер должен перейти в хорошее состояние
    """
    backend = ctx.start_backend(ThreeModeConfig(prefix=10, first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.3, hi=0.4, ttl=3,
                                         min_samples_to_disable=10, delay=2))
    do_requests(ctx, 10)
    time.sleep(5)
    ctx.perform_request_xfail(http.request.get())


def test_delay(ctx):
    """
    В плохом состоянии балансер должен отправлять пинговые запросы с периодом delay
    """
    delay = 1
    min_delta = timedelta(seconds=0.9 * delay)
    max_delta = timedelta(seconds=1.1 * delay)
    backend = ctx.start_backend(ThreeModeConfig(first=0, second=1))

    def get_reqs_times(count):
        result = list()
        for _ in range(count):
            result.append(backend.state.requests.get().start_time)
        return result

    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, delay=delay, min_samples_to_disable=2))
    do_requests(ctx, 2)
    time.sleep(5.5)
    get_reqs_times(2)
    ts = get_reqs_times(5)
    pairs = zip(ts[:-1], ts[1:])
    deltas = map(lambda (x, y): y - x, pairs)
    assert min(deltas) >= min_delta
    assert max(deltas) <= max_delta


def test_ping_disable_file(ctx):
    """
    При наличии ping_disable_file балансер не должен переходить в плохое состояние,
    даже если процент успешных запросов упал ниже lo
    """
    disable_file = ctx.manager.fs.create_file('disable_file')
    backend = ctx.start_backend(ThreeModeConfig(first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.7, hi=0.9, ttl=100,
                                         min_samples_to_disable=2, delay=100,
                                         ping_disable_file=disable_file))
    do_requests(ctx, 2)
    ctx.perform_request_xfail(http.request.get())


def test_ping_disable_file_removed(ctx):
    """
    Если ping_disable_file удален и в этот момент процент успешных запросов ниже lo,
    то балансер должен перейти в плохое состояние
    """
    disable_file = ctx.manager.fs.create_file('disable_file')
    backend = ctx.start_backend(ThreeModeConfig(first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.7, hi=0.9, ttl=100,
                                         min_samples_to_disable=2, delay=100,
                                         ping_disable_file=disable_file))
    do_requests(ctx, 2)
    ctx.manager.fs.remove(disable_file)
    time.sleep(1.1)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 500)


def test_ping_disable_file_appeared(ctx):
    """
    Если балансер находился в плохом состоянии и в этот момент появился файл ping_disable_file,
    то балансер должен перейти в хорошее состояние
    """
    disable_file = ctx.manager.fs.create_file('disable_file')
    ctx.manager.fs.remove(disable_file)
    backend = ctx.start_backend(ThreeModeConfig(first=0, second=1))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port, lo=0.7, hi=0.9, ttl=100,
                                         min_samples_to_disable=2, delay=100,
                                         ping_disable_file=disable_file))
    do_requests(ctx, 2)
    ctx.manager.fs.rewrite(disable_file, '')
    time.sleep(1.1)
    ctx.perform_request_xfail(http.request.get())


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_skip_keepalive_in_ping(ctx, connection_manager_required):
    """
    BALANCER-2874 ping requests should not use keepalive connections
    """
    backend = ctx.start_backend(SimpleConfig(response=http.response.gateway_timeout()))
    ctx.start_balancer(SmartPingerConfig(backend.server_config.port,
                                         delay=0.1, min_samples_to_disable=1, keepalive_count=30,
                                         connection_manager_required=connection_manager_required))
    for _ in range(10):
        try:
            ctx.perform_request(http.request.get())
        except HTTPReaderException:
            pass

    tcpdump = ctx.manager.tcpdump.start(backend.server_config.port)
    time.sleep(2)

    tcpdump.read_all()
    closed_sessions = tcpdump.get_closed_sessions()
    sessions = tcpdump.get_sessions()
    assert len(sessions) > 10
    assert len(closed_sessions) > 10
