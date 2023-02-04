import json
import operator
import pytest
from StringIO import StringIO

from skybone.rbtorrent.metrics import Counter, Histogram, MetricPusher

def test_counter():
    add_counter = Counter('summ', operator.add)
    assert not add_counter

    add_counter.update(1)
    add_counter.update(2)
    assert add_counter.get_value() == 3

    add_counter.reset()
    assert not add_counter

    # add an int and a long
    # NB: msgpack decodes 64-bit numbers into long
    add_counter.update(1)
    add_counter.update(long(2**42))
    assert add_counter.get_value() == 1 + 2**42

    add_counter.reset()
    add_counter.update(0.5)
    add_counter.update(1)
    assert add_counter.get_value() == 1.5

    max_counter = Counter('txxx', max)
    assert not max_counter

    max_counter.update(1)
    max_counter.update(2)
    assert max_counter.get_value() == 2

    max_counter.reset()
    assert not max_counter

    max_counter.update(1)
    max_counter.update(long(2**42))
    assert max_counter.get_value() == 2**42

    max_counter.reset()
    max_counter.update(1.5)
    max_counter.update(1)
    assert max_counter.get_value() == 1.5


def test_histogram():
    intervals = [0.5, 1.0, 2.0, 5.0, 10.0]
    hist = Histogram(intervals)
    assert not hist

    for x in [0.1, 0.5, 0.7, 1.0, 5.1, 5.2]:
        hist.update(x)
    assert hist.get_value() == [
        [0.5, 3], [1.0, 1], [2.0, 0], [5.0, 2], [10.0, 0]
    ]

    hist.update([
        [0.5, 0], [1.0, 1], [2.0, 1], [5.0, 0], [10.0, 0]
    ])
    assert hist.get_value() == [
        [0.5, 3], [1.0, 2], [2.0, 1], [5.0, 2], [10.0, 0]
    ]

    with pytest.raises(AssertionError):
        hist.update([
            [1.0, 1], [2.0, 1], [3.0, 1], [4.0, 1], [5.0, 1]
        ])

    with pytest.raises(AssertionError):
        hist.update([
            [0.5, 1], [1.0, 1], [2.0, 1], [5.0, 1], [10.0, 1], [15.0, 1]
        ])

    hist.reset()
    assert not hist

    hist.update(128)
    hist.update(long(2**42))
    assert hist.get_value() == [
        [0.5, 0], [1.0, 0], [2.0, 0], [5.0, 0], [10.0, 2]
    ]
    hist.update(0)
    assert hist.get_value() == [
        [0.5, 1], [1.0, 0], [2.0, 0], [5.0, 0], [10.0, 2]
    ]


def test_metric_pusher(mocker):
    mocker.patch(
        'skybone.rbtorrent.metrics.IO_HGRAM_INTERVALS',
        [0.5, 1.0, 2.0, 5.0, 10.0]
    )
    mock_urlopen = mocker.patch(
        'skybone.rbtorrent.metrics.gevent_urlopen',
        return_value=StringIO('{"status":"ok"}')
    )
    pusher = MetricPusher(yasmagent_url='mock', parent=None)

    pusher._push_counters(pusher.log)
    assert mock_urlopen.call_count == 0

    pusher.update_counter('dl_io_write_ms', 3.0)
    pusher.update_counter('dfs_downloads_succeeded', 1)
    pusher.update_counter('dl_io_write_ms', [
        [0.5, 0], [1.0, 0], [2.0, 0], [5.0, 1], [10.0, 0]
    ])
    pusher.update_counter('dfs_downloads_succeeded', 1)
    pusher._push_counters(pusher.log)

    assert mock_urlopen.call_count == 1
    kwargs = mock_urlopen.call_args[1]
    data = json.loads(kwargs['data'])
    assert len(data) == 1
    values = sorted(data[0]['values'], key=lambda x: x['name'])
    assert values == [
        {
            'name': 'dfs_downloads_succeeded_summ',
            'val': 2
        },
        {
            'name': 'dl_io_write_ms_hgram',
            'val': [[0.5, 0], [1.0, 0], [2.0, 1], [5.0, 1], [10.0, 0]]
        },
    ]
