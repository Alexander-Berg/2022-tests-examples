from __future__ import print_function, division

import contextlib
import gevent
import gevent.queue
import pytest
import random
import time

from skybone.rbtorrent.skbn.peer_collection import PeerConnectCandidates


class FakePeer(object):
    def __init__(self, weight):
        self.weight = weight
        self.is_local = False


@contextlib.contextmanager
def benchmark(max_time, name):
    ts = time.time()

    try:
        yield
    finally:
        te = time.time()
        tt = te - ts

        percent = tt / max_time * 100

        if percent <= 100:
            blocks = '=' * int(percent // 5)
            blocks_txt = '[%-20s]' % (blocks, )
        else:
            blocks_txt = '[===== too much =====]'

        print('%-35s:   %0.4fs   %s %4d%%' % (name, tt, blocks_txt, tt / max_time * 100))

        if tt > max_time:
            raise Exception('Benchmark fail: %s: %0.4fs > %0.4fs' % (name, tt, max_time))


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_discarding(local_prob):
    peer1 = FakePeer(0)

    pcc = PeerConnectCandidates(local_prob=local_prob)
    pcc.put(peer1)

    assert pcc.queue.qsize() == 1
    assert pcc.cnt_deferred == 0
    assert pcc.cnt_queued == 1

    pcc.discard(peer1)

    assert pcc.queue.qsize() == 0
    assert pcc.cnt_deferred == 0
    assert pcc.cnt_queued == 0


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_queue_many_times(local_prob):
    peer1 = FakePeer(0)

    pcc = PeerConnectCandidates(local_prob=local_prob)

    for i in range(100):
        pcc.put(peer1)

    assert pcc.queue.qsize() == 1
    assert pcc.cnt_deferred == 0
    assert pcc.cnt_queued == 1


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_defer_many_times(local_prob):
    peer1 = FakePeer(0)

    pcc = PeerConnectCandidates(local_prob=local_prob)

    for i in range(100):
        pcc.put(peer1, defer=10)

    assert pcc.queue.qsize() == 0
    assert len(pcc.deferred) == 1
    assert pcc.cnt_deferred == 1
    assert pcc.cnt_queued == 0


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_defering_loop(local_prob):
    pcc = PeerConnectCandidates(local_prob=local_prob)
    pcc.start()

    gevent.sleep()

    peer = FakePeer(0)
    pcc.put(peer, defer=0.1)

    gevent.sleep(0.05)

    with pytest.raises(gevent.queue.Empty):
        pcc.get(block=False)

    gevent.sleep(0.025)

    with pytest.raises(gevent.queue.Empty):
        pcc.get(block=False)

    gevent.sleep(0.025)

    assert pcc.get(block=False) == peer

    pcc.stop()


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_defering_loop_many_items(local_prob):
    pcc = PeerConnectCandidates(local_prob=local_prob)
    pcc.start()

    gevent.sleep()

    peers = [FakePeer(0) for i in range(5000)]

    for peer in peers:
        pcc.put(peer, defer=random.random() / 2)

    ts = time.time()
    for _ in range(5000):
        pcc.get(timeout=1.0 / 50)
    te = time.time()

    assert te - ts > 0.45

    pcc.stop()


@pytest.mark.benchmark
def test_re_adding_benchmark():
    count = 50000
    count_txt = '50k'

    peers = [FakePeer(i) for i in range(count)]

    pcc = PeerConnectCandidates()

    print()

    with benchmark(0.60, 'put %s peers' % (count_txt, )):
        for peer in peers:
            pcc.put(peer)

    with benchmark(2.17, 'put %s peers again' % (count_txt, )):
        for peer in peers:
            pcc.put(peer)

    with benchmark(0.107, 'discard %s non-existing' % (count_txt, )):
        for i in range(count):
            pcc.discard(FakePeer(i))

    with benchmark(0.85, 'discard %s' % (count_txt, )):
        for peer in peers:
            pcc.discard(peer)

    with benchmark(0.60, 'put %s deferred' % (count_txt, )):
        for peer in peers:
            pcc.put(peer, defer=60)

    with benchmark(2.02, 'put %s deferred again' % (count_txt, )):
        for peer in peers:
            pcc.put(peer, defer=60)

    with benchmark(0.103, 'discard %s non-existing deferred' % (count_txt, )):
        for i in range(count):
            pcc.discard(FakePeer(i))

    with benchmark(0.84, 'discard %s deferred' % (count_txt, )):
        for peer in peers:
            pcc.discard(peer)

    with benchmark(0.072, 'discard %s non-existing' % (count_txt, )):
        for peer in peers:
            pcc.discard(peer)

    assert pcc.cnt_queued == 0
    assert pcc.cnt_deferred == 0


@pytest.mark.parametrize('local_prob', [0, 0.5])
def test_peer_weight(local_prob):
    peer_lo = FakePeer(-1)
    peer_hi = FakePeer(1000)

    pcc = PeerConnectCandidates(local_prob=local_prob)
    pcc.start()

    pcc.put(peer_lo)
    pcc.put(peer_hi)

    for i in range(100):
        pcc.put(FakePeer(i + 1), defer=0.1 if i >= 50 else None)

    assert pcc.queue.qsize() == 52
    assert len(pcc.deferred) == 50
    assert pcc.cnt_deferred == 50
    assert pcc.cnt_queued == 52

    assert pcc.get() is peer_hi

    #gevent.sleep(0.3)

    for i in range(50):
        # Now we should have regular 1..50 non-deferred peers
        gevent.sleep()
        p = pcc.get(block=False)
        assert 1 <= p.weight <= 50

    # Sleep, so our deferred peers comes in place
    gevent.sleep(0.2)

    # Grab 20 of deferred, just to test them out
    for i in range(20):
        p = pcc.get(block=False)
        assert 81 <= p.weight <= 100

    # Add hi-weight peer and check it is still returned
    pcc.put(peer_hi)
    assert pcc.get() is peer_hi

    # Grab final deferred peers
    for i in range(30):
        p = pcc.get(block=False)
        assert 51 <= p.weight <= 80

    # And now it's time for our low peer
    assert pcc.get() is peer_lo
