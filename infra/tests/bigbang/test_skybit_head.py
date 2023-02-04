import gevent
import hashlib
import pytest
import random

from skybone.rbtorrent.resource.resource import Resource
from skybone.rbtorrent.bencode import bencode
from skybone.rbtorrent.skbn.world import World


simple_resource = {
    'structure': {
        'testfile': {
            'executable': 0L,
            'md5sum': '_\xeb\xbe\xf1C\x89\xeb\xcf\xc3\xe5\x01\xfa\x10\x91\xad\xcb',
            'mode': 'rw-r--r--',
            'path': 'testfile',
            'resource': {
                'id': 'c051cd727d2a86c82904ca13550a81e854ecf93f', 'type': 'torrent'
            },
            'size': 10L,
            'type': 'file'
        }
    },
    'torrents': {
        'c051cd727d2a86c82904ca13550a81e854ecf93f': {
            'info': {
                'length': 10L,
                'name': 'data',
                'piece length': 4194304L,
                'pieces': '\x00Hi\xf9\xec2\x8d\x1c\xb1\xe6MPjC\xf9#\xcf\xad\xeb\x97'
            }
        }
    }
}


def _gen_worlds(cnt):
    for i in range(1, cnt + 1):
        world = World(uid=hashlib.sha1(str(i)).hexdigest(), desc='W_%d' % (i, ), extended_logging=True, port=0)
        world.start()
        yield world


def _gen_resource():
    res = Resource(None, None)
    metatorrent = res._generate_rbtorrent1(simple_resource['structure'], simple_resource['torrents'], None)
    uid = hashlib.sha1(bencode(metatorrent.torrent_info)).hexdigest()
    return uid, simple_resource


def test_simple_download():
    uid, head = _gen_resource()
    world1, world2 = _gen_worlds(2)

    handle1 = world1.handle(uid)
    handle1.set_head(simple_resource)
    handle1.seed(None)

    handle2 = world2.handle(uid)
    handle2.get_head()
    handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])

    handle2.head_res.wait(timeout=0.3)
    assert handle2.head_res.get(timeout=0) == simple_resource

    assert 1


def test_simple_download_no_resource():
    uid, head = _gen_resource()
    world1, world2 = _gen_worlds(2)

    handle1 = world1.handle(uid[:-1] + 'a')
    handle1.set_head(head)

    with pytest.raises(handle1.NoHead):
        handle1.seed(None)

    handle2 = world2.handle(uid)
    handle2.get_head()
    handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])

    handle2.head_res.wait(timeout=0.05)

    assert not handle2.head_res.ready()
    assert len(handle1.swarm.conns_active) == 0
    assert len(handle2.swarm.conns_active) == 0

    assert len(handle2.swarm.peers.no_resource) == 1


@pytest.mark.slow
def test_simple_download_invalid_head():
    uid, head = _gen_resource()

    # Make invalid head
    uid = '0' + uid[1:]

    world1, world2 = _gen_worlds(2)

    handle1 = world1.handle(uid)
    handle1.set_head(simple_resource)

    with pytest.raises(handle1.NoHead):
        handle1.seed(None)

    handle2 = world2.handle(uid)
    handle2.get_head()
    handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])

    handle2.head_res.wait(timeout=0.3)
    assert not handle2.head_res.ready()


def test_skybit_choose_one_conn():
    uid, head = _gen_resource()
    world1, world2 = _gen_worlds(2)

    handle1 = world1.handle(uid)
    handle1.get_head()

    handle2 = world2.handle(uid)
    handle2.get_head()

    handle1.add_peer('127.0.0.1', world2.sock.getsockname()[1])
    handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])

    handle2.head_res.wait(timeout=0.2)

    assert len(handle1.swarm.conns_halfopen) == 0
    assert len(handle2.swarm.conns_halfopen) == 0

    assert len(handle1.swarm.conns_active) == 1
    assert len(handle2.swarm.conns_active) == 1

    handle1.set_head(simple_resource)
    handle2.head_res.wait(timeout=0.3)

    assert handle2.head_res.get(timeout=0) == simple_resource

    gevent.sleep(0.1)

    handle1.stop()
    handle2.stop()

    assert len(handle1.swarm.conns_active) == 0
    assert len(handle2.swarm.conns_active) == 0


def test_skybit_connect_to_ourselves():
    uid, head = _gen_resource()
    world1 = _gen_worlds(1).next()

    handle1 = world1.handle(uid)
    handle1.get_head()
    handle1.add_peer('127.0.0.1', world1.sock.getsockname()[1])

    handle1.head_res.wait(timeout=0.2)

    assert len(handle1.swarm.conns_active) == 0
    assert len(handle1.swarm.peers.disconnected) == 0


def test_simple_download_in_many_peers():
    uid, head = _gen_resource()

    world1 = World(uid=hashlib.sha1('S').hexdigest(), desc='S', extended_logging=True, port=30000)
    world1.start()

    handle1 = world1.handle(uid)
    handle1.set_head(simple_resource)
    handle1.seed(None)

    worlds = []
    res_queue = gevent.queue.Queue()

    count = 60

    for i in range(count):
        world = World(
            uid=hashlib.sha1(str(i + 1)).hexdigest(),
            desc=str(i + 1),
            extended_logging=True,
            port=30000 + i + 1
        )
        world.start()
        worlds.append(world)

        handle = world.handle(uid)
        handle.get_head()

        added = 0
        while added < 12:
            port_offset = random.randint(0, count)
            if port_offset == i + 1:
                # Do not add ourselves
                continue

            handle.add_peer('::1', 30000 + port_offset)
            added += 1

        handle.head_res.rawlink(res_queue.put)

    with gevent.Timeout(5) as tout:
        try:
            for i in range(count):
                assert res_queue.get().get_nowait() == simple_resource
        except gevent.Timeout as ex:
            if ex != tout:
                raise
            assert 0, 'timed out %s' % (ex, )

    assert 1
