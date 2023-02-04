from __future__ import print_function, division

import pytest
import gevent
import time

from skybone.rbtorrent.netproxy import Bucket

from test_skybit_peer_collection import benchmark


def test_bucket():
    b = Bucket(10, burst=1)
    b.start()

    for i in range(4):
        ts = time.time()
        leaked = b.leak(3)
        assert leaked == 3

        spent = time.time() - ts

        if i == 3:
            assert 0.21 > spent > 0.2
        else:
            assert spent < 0.001

    b.stop()


def test_bucket_parallel():
    b = Bucket(100 * 1000)
    b.leak(100 * 1000)
    b.start()

    w_state = {}

    def _leaker(n, to_leak=1000):
        leaked = 0
        while leaked < to_leak:
            leaked += b.leak(min(100, to_leak - leaked))
            w_state[n] = leaked
            gevent.sleep(0.01)

    workers = [gevent.spawn(_leaker, n=idx) for idx in range(1, 101)]
    gevent.sleep(0.50)

    # Check what bucket was distributed in fair fashion
    for v in set(w_state.values()):
        assert 400 <= v <= 600

    [grn.join() for grn in workers]

    assert set(w_state.values()) == set([1000])


@pytest.mark.benchmark
def test_bucket_speed():
    print()

    b = Bucket(10 * 1024 * 1024)
    b.leak(10 * 1024 * 1024)  # empty bucket

    def doleak(bucket, amount, block):
        while amount:
            amount -= bucket.leak(min(amount, block))

    with benchmark(1.8, '10mb/s: 15mb leak (8192b block)'):
        doleak(b, 15 * 1024 * 1024, 8192)

    with benchmark(1.8, '10mb/s: 15mb leak (1024b block)'):
        doleak(b, 15 * 1024 * 1024, 1024)

    with benchmark(3.30, '10mb/s: 15mb leak (16b block)'):
        doleak(b, 15 * 1024 * 1024, 16)

    b = Bucket(10 ** 12)
    b.leak(10 ** 12)

    with benchmark(0.40, 'max leak speed: 1GB (8192b block)'):
        doleak(b, 10 ** 9, 8192)

    with benchmark(3.00, 'max leak speed: 1GB (1024b block)'):
        doleak(b, 10 ** 9, 1024)

    with benchmark(1.20, 'max leak speed: 100MB (100b block)'):
        doleak(b, 10 ** 8, 256)
