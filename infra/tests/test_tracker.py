import random
import time
import uuid

import msgpack
import pyjack
import pytest

from skybone_coord import tracker
from conftest import benchmark


class MockSock(object):
    def __init__(self):
        self.buf = []

    def sendto(self, data, peer):
        self.buf.append((data, peer))


@pytest.fixture(scope='function')
def trk():
    return tracker.Tracker(
        interval=(60, 60),
        interval_leech=(60, 60),
        return_peers=5,
        return_seeders_ratio=0.4
    )


@pytest.fixture(scope='function')
def fpeers():
    peers = []
    fbips_v4 = set()  # returned to fb-aware ipv4 peer
    fbips_v6 = set()  # returned to fb-aware ipv6 peer (this includes ipv4-only fb peers)
    bbips_v4 = set()  # returned to not fb ipv4 peer
    bbips_v6 = set()  # returned to not fb ipv6 peer (this includes ipv4-only bb peers)

    now = int(time.time())

    for x in range(16):
        ip = ip6 = fbip = fbip6 = None

        port = 1000 + x

        if x & 0b1000:
            ip = '127.0.0.' + str(x)
            bbips_v4.add((ip, port))

            if not x & 0b0100:
                bbips_v6.add((ip, port))

        if x & 0b0100:
            ip6 = '::' + str(x)
            bbips_v6.add((ip6, port))

        if x & 0b0010:
            fbip = '128.0.0.' + str(x)
            fbips_v4.add((fbip, port))

            if not x & 0b0001:
                fbips_v6.add((fbip, port))

        if x & 0b0001:
            fbip6 = '1::' + str(x)
            fbips_v6.add((fbip6, port))

        #if not ip or ip6 or fbip or fbip6:

        peer = tracker.Peer(str(x), ip, ip6, fbip, fbip6, port, now)
        peers.append(peer)
    return (
        peers,
        sorted(bbips_v4),
        sorted(bbips_v6),
        sorted(fbips_v4),
        sorted(fbips_v6),
    )


@pytest.fixture(scope='function')
def peersfifty():
    import socket
    import struct

    peers = []
    now = int(time.time())

    for i in range(1000):
        for x in range(1, 16):
            ip = ip6 = fbip = fbip6 = None

            if x & 0b1000:
                ip = socket.inet_ntoa(struct.pack('!I', 167772160 + i))
            if x & 0b0100:
                ip6 = '::' + str(i)
            if x & 0b0010:
                fbip = socket.inet_ntoa(struct.pack('!I', 0 + i))
            if x & 0b0001:
                fbip6 = '1::' + str(i)

            uid = struct.pack('!Q', i + (2**32 * x))
            peers.append(tracker.Peer(uid, ip, ip6, fbip, fbip6, 42, now))

    return peers


def test_new_conn(trk):
    tid = random.randint(0, 2**32-1)
    uid = uuid.uuid4().get_bytes()

    reply = trk.handle_packet((
        trk.INITIAL_CID, 0, tid, uid, None, None, None, None, 2399
    ), '127.0.0.1', 12345)

    tid2, action, cid = reply
    assert tid2 == tid
    assert action == 0
    assert cid != trk.INITIAL_CID
    assert 0 <= cid <= 2**32-1

    # Check if we will try to make connection again
    reply = trk.handle_packet((
        trk.INITIAL_CID, 0, tid, uid, None, None, None, None, 2400
    ), '127.0.0.1', 12346)
    tid3, action, cid2 = reply

    assert tid3 == tid2 == tid
    assert action == 0
    assert cid2 == cid

    assert len(trk.conns.by_cid) == 1
    assert len(trk.conns.by_peer) == 1


def test_cid_selection_retry(trk):
    def fake_random(random_orig):
        fake_random.rnd_count += 1
        if fake_random.rnd_count >= 10:
            rnd = random_orig()
            fake_random.last_rand = rnd
            return rnd
        return 0.098e-07
    fake_random.rnd_count = 0
    fake_random.last_rand = None

    try:
        pyjack.connect(random.random, fake_random)

        for i in range(2):
            reply = trk.handle_packet((
                trk.INITIAL_CID, 0, 0, hex(i), None, None, None, None, 2399
            ), '127.0.0.1', 12345)
            tid2, action, cid = reply
            if i == 0:
                assert cid == 42
            else:
                assert cid != 42
                assert cid == int(fake_random.last_rand * (2 ** 32 - 1))
    finally:
        random.random.restore()

    assert fake_random.rnd_count == 10


def test_cid_selection_fail(trk):
    def fake_random(random_orig):
        return 0.098e-07

    try:
        pyjack.connect(random.random, fake_random)

        req1 = (
            trk.INITIAL_CID, 0, 0, '\x00', None, None, None, None, 2399
        )
        req2 = (
            trk.INITIAL_CID, 0, 0, '\x01', None, None, None, None, 2399
        )
        trk.handle_packet(req1, '127.0.0.1', 12345)
        with pytest.raises(tracker.EntropyError):
            trk.handle_packet(req2, '127.0.0.1', 12345)
    finally:
        random.random.restore()


def test_peer_obj():
    p = tracker.Peer('uid', None, None, None, None, 42, int(time.time()))
    assert p.uid == 'uid'
    assert p.port == 42
    assert (
        p.ip is p.ipp is p.ip6 is p.ip6p is
        p.fbip is p.fbipp is p.fbip6 is p.fbip6p is
        None
    )
    assert repr(p) == '<Peer %s>' % ('uid'.encode('hex'), )

    p = tracker.Peer('diu', '127.0.0.1', '::1', '127.0.0.2', '::2', 43, int(time.time()))
    assert p.uid == 'diu'
    assert p.port == 43
    assert p.ip == '127.0.0.1'
    assert p.ip6 == '::1'
    assert p.fbip == '127.0.0.2'
    assert p.fbip6 == '::2'
    assert p.ipp == '\x7f\x00\x00\x01'
    assert p.fbipp == '\x7f\x00\x00\x02'
    assert p.ip6p == '\x00' * 15 + '\x01'
    assert p.fbip6p == '\x00' * 15 + '\x02'
    assert repr(p) == '<Peer %s>' % ('diu'.encode('hex'), )

    try:
        def tm(time):
            return 100.23
        pyjack.connect(time.time, tm)

        p = tracker.Peer('zzz', None, None, None, None, 44, int(time.time()))
        assert p.ttl == (100 + p.TTL)
    finally:
        time.time.restore()


def test_peer_connection_obj():
    pc = tracker.PeerConnection(10, None, 123)
    assert pc.ttl == (123 + pc.TTL)


def test_announce_invalid_cid(trk):
    # TODO: check warning log
    reply = trk.handle_packet(msgpack.dumps((
        123, 10, 0, 0, None
    )), '127.0.0.1', 12345)

    assert reply is None


def test_announce_first(trk):
    reply = trk.handle_packet((
        trk.INITIAL_CID, 0, 42, '\x00', '127.0.0.1', None, None, None, 2399
    ), '127.0.0.1', 12345)
    tid, action, cid = reply

    assert tid == 42
    assert action == 0

    reply = trk.handle_packet((
        cid, 10, 42, tracker.HashState.LEECHING, '0' * 40, trk.NET_AUTO
    ), '127.0.0.1', 12345)
    print(repr(reply))
    tid, action, interval, seeders, leechers, peers = reply

    assert tid == 42
    assert action == 10
    assert interval == 60
    assert seeders == 0
    assert leechers == 1
    assert len(peers) == 0

    reply = trk.handle_packet((
        cid, 10, 42, tracker.HashState.SEEDING, '0' * 40, trk.NET_AUTO
    ), '127.0.0.1', 12345)
    tid, action, interval = reply

    assert tid == 42
    assert action == 10
    assert interval == 60

    reply = trk.handle_packet((
        cid, 10, 42, tracker.HashState.STOPPED, '0' * 40, trk.NET_AUTO
    ), '127.0.0.1', 12345)
    tid, action = reply

    assert tid == 42
    assert action == 10

    assert len(trk.peers.peers) == 1
    assert len(trk.conns.by_cid) == 1
    assert len(trk.hashes.hashes) == 0
    #assert len(trk.hashes.hashes_by_peer) == 1
    #assert len(trk.hashes.hashes_by_peer.values()[0]) == 0


def test_ping(trk):
    reply = trk.handle_packet(('PI', 'NG'), None, None)
    assert reply == 'PONG'


def test_invalid_action(trk):
    reply = trk.handle_packet((0, 0), None, None)
    assert reply is None


def test_invalid_packet(trk):
    with pytest.raises(tracker.MallformedPacket):
        trk.handle_packet((0, ), None, None)


def test_peer_selection_auto(trk, fpeers):
    peers, bbips_v4, bbips_v6, fbips_v4, fbips_v6 = fpeers

    for peer in peers:
        if not (peer.ip or peer.ip6 or peer.fbip or peer.fbip6):
            exp = bbips_v6[:]
        else:
            exp = (
                (fbips_v4[:] if not peer.fbip6 else fbips_v6[:])
                if peer.has_fb else
                (bbips_v4[:] if not peer.ip6 else bbips_v6[:])
            )

        ret = sorted(trk.get_ips_for_peer(peer, trk.NET_AUTO, peers, False, False))
        assert exp == ret


def test_peer_selection_bbonly(trk, fpeers):
    peers, bbips_v4, bbips_v6, _, _ = fpeers

    for peer in peers:
        if not (peer.ip or peer.ip6):
            exp = bbips_v6[:]
        else:
            exp = bbips_v4[:] if not peer.ip6 else bbips_v6[:]
        ret = sorted(trk.get_ips_for_peer(peer, trk.NET_BB_ONLY, peers, False, False))
        assert exp == ret


def test_peer_selection_fbonly(trk, fpeers):
    peers, _, _, fbips_v4, fbips_v6 = fpeers

    for peer in peers:
        if not (peer.fbip or peer.fbip6):
            exp = fbips_v6[:]
        else:
            exp = fbips_v4[:] if not peer.fbip6 else fbips_v6[:]

        ret = sorted(trk.get_ips_for_peer(peer, trk.NET_FB_ONLY, peers, False, False))
        assert exp == ret


def test_peer_selection_invalid_net(trk, fpeers):
    peers, bbips_v4, bbips_v6, fbips_v4, fbips_v6 = fpeers

    for peer in peers:
        assert trk.get_ips_for_peer(peer, 42, peers, False, False) == []


def test_ttl_window():
    w = tracker.TTLWindow(100, 10, 1)
    w.update('a', 11)
    w.update('b', 12)
    w.update('c', 45)
    w.update('e1', 89.99)
    w.update('e2', 90)

    assert w.idx == w.start == 0
    assert list(w.iterate_outdated(0)) == []
    assert list(w.iterate_outdated(5)) == []
    assert list(w.iterate_outdated(12)) == []
    assert w.idx == w.start == 1
    assert list(w.iterate_outdated(20)) == ['a', 'b']
    assert w.idx == w.start == 2
    assert list(w.iterate_outdated(140)) == ['c', 'e1', 'e2']
    assert w.start == 14
    assert w.idx == 4

    w.update('a', 145)
    w.update('a', 155)

    assert list(w.iterate_outdated(150)) == []
    assert list(w.iterate_outdated(160)) == ['a']

    with pytest.raises(tracker.TTLOutOfWindow):
        w.update('a', 10)

    with pytest.raises(tracker.TTLOutOfWindow):
        w.update('a', 159)

    w.update('a1', 160)
    w.update('a2', 259)

    with pytest.raises(tracker.TTLOutOfWindow):
        w.update('a2', 260)

    with pytest.raises(tracker.TTLOutOfWindow):
        w.update('a2', 1000)

    assert list(w.iterate_outdated(170)) == ['a1']
    assert list(w.iterate_outdated(250)) == []
    assert list(w.iterate_outdated(260)) == ['a2']
    assert list(w.iterate_outdated(260)) == []
    assert list(w.iterate_outdated(265)) == []

    assert not w.objs


def test_peer_selection_round_robin():
    hi = tracker.HashInfo('a' * 40)

    assert hi.get_round_robin_population(range(15), 5, 0) == (range(5), 5)
    assert hi.get_round_robin_population(range(15), 5, 5) == (range(5, 10), 10)
    assert hi.get_round_robin_population(range(15), 5, 13) == ([13, 14, 0, 1, 2], 3)
    assert hi.get_round_robin_population(range(15), 20, 10) == (range(15), 0)

    import timeit
    res = timeit.timeit(lambda: hi.get_round_robin_population(set(range(15)), 5, 5), number=200000)
    print('%d kops/s' % (200000 / res / 1000, ))

    res = timeit.timeit(lambda: hi.get_round_robin_population(set(range(100)), 30, 90), number=100000)
    print('%d kops/s' % (100000 / res / 1000, ))

    res = timeit.timeit(lambda: hi.get_round_robin_population(set(range(1000)), 30, 990), number=20000)
    print('%d kops/s' % (20000 / res / 1000, ))


@benchmark
def test_announce_perf(peersfifty):
    import GreenletProfiler
    GreenletProfiler.set_clock_type('cpu')

    import hashlib
    import time

    trk = tracker.Tracker(interval=(14400, 14400), return_peers=25, return_seeders_ratio=0.12)

    msg_packer = msgpack.Packer()

    hashes = [msg_packer.pack(hashlib.sha1(str(i)).hexdigest()) for i in range(5000)]
    cids = []

    ts = time.time()
    for idx, peer in enumerate(peersfifty):
        ret = trk.handle_packet(
            msg_packer.pack((
                trk.INITIAL_CID, trk.ACTION_CONNECT,
                str(idx), peer.uid,
                peer.ip, peer.ip6, peer.fbip, peer.fbip6, peer.port
            )), None, peer.port
        )
        tid, action, cid = msgpack.loads(ret)
        assert tid == str(idx)
        assert action == trk.ACTION_CONNECT
        cids.append(cid)

    te = time.time()
    spent = te - ts
    print('%d connect/s' % (len(peersfifty) / spent))

    spd = [int(time.time() / 10), 0]

    msg_start = '\x96'
    msg_announce = msg_packer.pack(trk.ACTION_ANNOUNCE)
    msg_ints = [msg_packer.pack(i) for i in range(500)]
    msg_hs = msg_packer.pack(tracker.HashState.SEEDING)
    msg_net = msg_packer.pack(trk.NET_AUTO)

    hid = 0

    GreenletProfiler.start()
    try:
        for peeridx, peer in enumerate(peersfifty):
            if peeridx % 1000 == 0:
                print(peeridx)
                #if peeridx == 10000:
                #    ipdb.set_trace()

            cid = cids[peeridx]
            cid = msg_packer.pack(cid)

            for i in xrange(50):
                hsh = hashes[hid]
                hid += 1
                if hid >= len(hashes):
                    hid = 0

                #ret = trk.handle_packet(
                #    msg_packer.pack((
                #        cid, trk.ACTION_ANNOUNCE,
                #        str(i), tracker.HashState.SEEDING, hsh, trk.NET_AUTO
                #    )), None, peer.port
                #)

                ret = trk.handle_packet(
                    msg_start + cid + msg_announce + msg_ints[i] +
                    msg_hs + hsh + msg_net,
                    None, peer.port
                )

                spd[1] += 1

            now = int(time.time()) / 10
            if spd[0] != now:
                print('%d req/s' % (spd[1] / 10, ))
                spd[:] = [now, 0]

            #break

            #print('min: %0.6fs, max: %0.6fs, avg: %0.6fs' % (
            #    min(times),
            #    max(times),
            #    sum(times) / len(times)
            #))
    finally:
        GreenletProfiler.stop()
        stats = GreenletProfiler.get_func_stats()
        stats.print_all()
        stats.save('profile.callgrind', type='callgrind')
