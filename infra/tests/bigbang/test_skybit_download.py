import hashlib
import gevent
import pytest

from cStringIO import StringIO

from skybone.rbtorrent.skbn.world import World
from skybone.rbtorrent.skbn.shmem import SharedMemory

from tools import gen_resource


def _gen_worlds(cnt, **kwargs):
    for i in range(1, cnt + 1):
        world = World(
            uid=hashlib.sha1(str(i)).hexdigest(),
            desc='W_%d' % (i, ),
            extended_logging=True,
            **kwargs
        )
        world.start()
        yield world


@pytest.mark.parametrize(
    'compression,encryption', [
        (None, None),
        pytest.param(
            {'request': 'zstd/1', 'reply': ['zstd']}, None, id='zstd'
        ),
        pytest.param(
            {'reply': ['zstd']}, None, id='zstd_no_leech'
        ),
        pytest.param(
            {'request': 'zstd/1'}, None,
            marks=pytest.mark.xfail(strict=True), id='zstd_no_seed'
        ),
        pytest.param(
            None, ['aes_ctr'], id='aes'
        ),
        pytest.param(
            {'request': 'zstd/1', 'reply': ['zstd']}, ['aes_ctr'], id='zstd_aes'
        ),
    ]
)
def test_simple_download(compression, encryption, tmpdir):
    time_coeff = 5 if encryption else 1  # wait longer for key generation
    uid, head = gen_resource()
    world1, world2 = _gen_worlds(2, compression_params=compression, encryption_config=encryption)

    written_blocks = []

    def _get_block_seed(md5, start, length, memory_segment, sha1):
        if md5 == '484c5624910e6288fad69e572a0637f7' and sha1 == '195ae9b612afbf92d2515cfee898f82f601a9107':
            memory_segment.write('\1' * length)
            memory_segment.rewind()
            return True

        raise 1

    def _get_block(fn, start, memory_segment, length, sha1):
        raise 1

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        assert fn == tmpdir.join('testfile').strpath
        written_blocks.append((fn, start, memory_segment.read(), sha1))
        return True

    seed_shmem = SharedMemory()
    seed_shmem.create_mmap(size=32 * 1024 * 1024)

    try:
        handle1 = world1.handle(uid)
        handle1.set_head(head)
        handle1.set_shared_memory(seed_shmem)
        handle1.seed(_get_block_seed)

        try:
            shmem = SharedMemory()
            shmem.create_mmap(size=32 * 1024 * 1024)

            handle2 = world2.handle(uid)
            handle2.set_shared_memory(shmem)
            handle2.get_head()
            handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])
            handle2.head_res.wait(timeout=0.3 * time_coeff)
            assert handle2.head_res.ready()

            gevent.sleep(0.1)

            tmpdir.join('testfile').open(mode='wb')
            handle2.get_data(tmpdir, _get_block, _put_block)

            handle2.dl_res.wait(timeout=1)

            assert len(written_blocks) == 1
            assert written_blocks[0][0] == tmpdir.join('testfile').strpath
            assert written_blocks[0][1] == 0
            assert written_blocks[0][2] == '\1' * 10
            assert written_blocks[0][3] == '195ae9b612afbf92d2515cfee898f82f601a9107'

            handle1.stop()
            handle2.stop()

            world1.stop()
            world2.stop()
        finally:
            pass
    finally:
        pass


@pytest.mark.parametrize(
    'compression', [
        None,
        pytest.param(
            {'request': 'zstd/1', 'reply': ['zstd']},
            id='zstd'
        ),
    ]
)
def test_simple_download_chain_peers(compression, tmpdir):
    uid, head = gen_resource()

    world1 = World(
        uid=hashlib.sha1('S').hexdigest(),
        desc='S',
        extended_logging=True,
        port=0,
        compression_params=compression,
    )
    world1.start()

    written_blocks = []

    def _get_block_seed(md5, start, length, mem, sha1):
        if md5 == '484c5624910e6288fad69e572a0637f7' and sha1 == '195ae9b612afbf92d2515cfee898f82f601a9107':
            mem.write('\1' * length)
            mem.rewind()
            return True
        raise 1

    def _get_block(fn, start, length, memory_segment, sha1):
        possible_fns = [
            tmpdir.join('%d' % (idx, )).join('testfile').strpath for idx in range(10)
        ]

        if fn in possible_fns and sha1 == '195ae9b612afbf92d2515cfee898f82f601a9107':
            assert start == 0
            assert length == 10
            memory_segment.write('\1' * length)
            memory_segment.rewind()
            return True
        raise 1

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        written_blocks.append((fn, start, memory_segment.read(), sha1))
        return True

    seed_shmem = SharedMemory()
    seed_shmem.create_mmap(size=32 * 1024 * 1024)

    try:
        handle1 = world1.handle(uid)
        handle1.set_head(head)
        handle1.set_shared_memory(seed_shmem)
        handle1.seed(_get_block_seed)

        handles = []
        worlds = []

        count = 10

        last_world = world1

        shared_memories = []

        try:
            for i in range(count):
                world = World(
                    uid=hashlib.sha1(str(i + 1)).hexdigest(),
                    desc=str(i + 1),
                    extended_logging=True,
                    port=0,
                    compression_params=compression,
                )
                world.start()
                worlds.append(world)

                shmem = SharedMemory()
                shmem.create_mmap(size=32 * 1024 * 1024)
                shared_memories.append(shmem)

                handle = world.handle(uid)
                handle.set_shared_memory(shmem)
                handle.get_head()
                handle.add_peer('127.0.0.1', last_world.sock.getsockname()[1])
                handle.head_res.wait(timeout=0.3)
                assert handle.head_res.ready()

                handles.append(handle)

                last_world = world

            for idx, handle in enumerate(handles):
                tmpdir.join(str(idx)).ensure(dir=1).join('testfile').open(mode='wb').write('v' * 10)
                handle.get_data(
                    tmpdir.join(str(idx)),
                    _get_block, _put_block,
                    nocheck=[tmpdir.join(str(idx), 'testfile')]
                )

            for handle in handles:
                handle.dl_res.wait(timeout=1)
                assert handle.dl_res.ready()

            assert len(written_blocks) == 10
            assert written_blocks[0][0] == tmpdir.join('0', 'testfile').strpath
            assert written_blocks[0][1] == 0
            assert written_blocks[0][2] == '\1' * 10
            assert written_blocks[0][3] == '195ae9b612afbf92d2515cfee898f82f601a9107'

            assert written_blocks[1][0] == tmpdir.join('1', 'testfile').strpath
            assert written_blocks[1][1] == 0
            assert written_blocks[1][2] == '\1' * 10
            assert written_blocks[1][3] == '195ae9b612afbf92d2515cfee898f82f601a9107'

            handle1.stop()

            [hdl.stop() for hdl in handles]
            [wrld.stop() for wrld in worlds]

            world1.stop()
        finally:
            pass
    finally:
        pass


def test_simple_download_big_file(tmpdir):
    uid, head = gen_resource({'testfile': {'size': 32 * 1024 * 1024 + 16 * 1024, 'data': '\1'}})
    world1, world2 = _gen_worlds(2)

    result_data = StringIO()

    def _get_block_seed(md5, start, length, mem, sha1):
        mem.write('\1' * length)
        mem.rewind()
        return True

    def _get_block(fn, start, length, sha1):
        raise 1

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        assert fn == tmpdir.join('testfile').strpath
        result_data.seek(start)
        result_data.write(memory_segment.read())
        return True

    seed_shmem = SharedMemory()
    seed_shmem.create_mmap(size=32 * 1024 * 1024)

    try:
        handle1 = world1.handle(uid)
        handle1.set_head(head)
        handle1.set_shared_memory(seed_shmem)
        handle1.seed(_get_block_seed)

        try:
            shmem = SharedMemory()
            shmem.create_mmap(size=32 * 1024 * 1024)

            handle2 = world2.handle(uid)
            handle2.set_shared_memory(shmem)
            handle2.get_head()
            handle2.add_peer('127.0.0.1', world1.sock.getsockname()[1])
            handle2.head_res.wait(timeout=0.3)
            assert handle2.head_res.ready()

            gevent.sleep(0.1)

            tmpdir.join('testfile').open('wb')

            handle2.get_data(tmpdir, _get_block, _put_block)

            handle2.dl_res.wait(timeout=1)

            assert handle2.dl_res.ready()

            result = result_data.getvalue()
            assert len(result) == (32 * 1024 * 1024 + 16 * 1024)

            if result != '\1' * (32 * 1024 * 1024 + 16 * 1024):
                assert 0, 'bad data'

            handle1.stop()
            handle2.stop()

            world1.stop()
            world2.stop()
        finally:
            pass
    finally:
        pass


def test_simple_download_big_file_many_peers(tmpdir):
    uid, head = gen_resource({'testfile': {'size': 5 * 1024 * 1024, 'data': '\2'}})
    world1 = World(uid=hashlib.sha1('S').hexdigest(), desc='S', extended_logging=True, port=0)
    world1.start()

    result_datas = {}

    def _get_block_seed(md5, start, length, mem, sha1):
        mem.write('\2' * length)
        mem.rewind()
        return True

    def _get_block(fn, start, length, mem, sha1):
        mem.write('\2' * length)
        mem.rewind()
        return True

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        result_datas.setdefault(fn, StringIO())
        result_datas[fn].seek(start)
        result_datas[fn].write(memory_segment.read())
        return True

    seed_shmem = SharedMemory()
    seed_shmem.create_mmap(size=32 * 1024 * 1024)

    try:
        handle1 = world1.handle(uid)
        handle1.set_head(head)
        handle1.set_shared_memory(seed_shmem)
        handle1.seed(_get_block_seed)

        count = 30
        worlds = []
        handles = []
        last_world = world1

        shared_memories = []

        try:
            for i in range(count):
                world = World(
                    uid=hashlib.sha1(str(i + 1)).hexdigest(),
                    desc=str(i + 1),
                    extended_logging=True,
                    port=0,
                )
                world.start()
                worlds.append(world)

                shmem = SharedMemory()
                shmem.create_mmap(size=32 * 1024 * 1024)
                shared_memories.append(shmem)

                handle = world.handle(uid)
                handle.set_shared_memory(shmem)
                handle.get_head()
                handle.add_peer('127.0.0.1', last_world.sock.getsockname()[1])
                handle.head_res.wait(timeout=0.3)
                assert handle.head_res.ready()

                handles.append(handle)

                last_world = world

            for idx, handle in enumerate(handles):
                tmpdir.join(str(idx)).ensure(dir=1).join('testfile').open(mode='wb')
                handle.get_data(
                    tmpdir.join(str(idx)), _get_block, _put_block
                )

            for handle in handles:
                handle.dl_res.wait(timeout=1)
                assert handle.dl_res.ready()

            for fn, data in result_datas.items():
                value = data.getvalue()
                assert len(value) == 5 * 1024 * 1024
                assert value == '\2' * 5 * 1024 * 1024

            [hdl.stop() for hdl in handles]
            [wrld.stop() for wrld in worlds]

            handle1.stop()
            world1.stop()
        finally:
            pass
    finally:
        pass


def test_simple_download_many_leechers_from_slow_peer(tmpdir):
    uid, head = gen_resource()

    world1 = World(uid=hashlib.sha1('S').hexdigest(), desc='S', extended_logging=True, port=0)
    world1.start()

    written_blocks = []

    def _get_block_seed(md5, start, length, memory_segment, sha1):
        if md5 == '484c5624910e6288fad69e572a0637f7' and sha1 == '195ae9b612afbf92d2515cfee898f82f601a9107':
            gevent.sleep(0.1)
            memory_segment.write('\1' * length)
            memory_segment.rewind()
            return True
        raise 1

    def _get_block(fn, start, length, memory_segment, sha1):
        possible_fns = [
            tmpdir.join('%d' % (idx, )).join('testfile').strpath for idx in range(10)
        ]

        if fn in possible_fns and sha1 == '195ae9b612afbf92d2515cfee898f82f601a9107':
            assert start == 0
            assert length == 10
            memory_segment.write('\1' * length)
            memory_segment.rewind()
            return True
        raise 1

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        written_blocks.append((fn, start, memory_segment.read(), sha1))
        return True

    seed_shmem = SharedMemory()
    seed_shmem.create_mmap(size=32 * 1024 * 1024)

    try:
        handle1 = world1.handle(uid)
        handle1.set_head(head)
        handle1.set_shared_memory(seed_shmem)
        handle1.seed(_get_block_seed)

        handles = []
        worlds = []

        count = 2

        last_world = world1

        shared_memories = []

        try:
            for i in range(count):
                world = World(
                    uid=hashlib.sha1(str(i + 1)).hexdigest(),
                    desc=str(i + 1),
                    extended_logging=True,
                    port=0,
                )
                world.start()
                worlds.append(world)

                shmem = SharedMemory()
                shmem.create_mmap(size=32 * 1024 * 1024)
                shared_memories.append(shmem)

                handle = world.handle(uid)
                handle.set_shared_memory(shmem)
                handle.get_head()
                handle.add_peer('127.0.0.1', last_world.sock.getsockname()[1])
                handle.head_res.wait(timeout=0.3)
                assert handle.head_res.ready()

                handles.append(handle)

                last_world = world

                tmpdir.join(str(i)).ensure(dir=1).join('testfile').open(mode='wb').write('v' * 10)
                handle.get_data(
                    tmpdir.join(str(i)),
                    _get_block, _put_block,
                    nocheck=[tmpdir.join(str(i), 'testfile')]
                )

                handle.dl_res.wait(timeout=1)
                assert handle.dl_res.ready()

                # After our 1st peer will receive data (which takes 0.1 sec here)
                # it should send it to others very quickly

            with gevent.Timeout(1) as tout:
                try:
                    for handle in handles:
                        handle.dl_res.wait()
                        assert handle.dl_res.ready()
                except gevent.Timeout as ex:
                    if ex != tout:
                        raise

                    assert False, 'Timed out 1 sec [%r]' % (repr([handle.dl_res.ready() for handle in handles]), )

            assert len(written_blocks) == count
            assert written_blocks[0][0] == tmpdir.join('0', 'testfile').strpath
            assert written_blocks[0][1] == 0
            assert written_blocks[0][2] == '\1' * 10
            assert written_blocks[0][3] == '195ae9b612afbf92d2515cfee898f82f601a9107'

            assert written_blocks[1][0] == tmpdir.join('1', 'testfile').strpath
            assert written_blocks[1][1] == 0
            assert written_blocks[1][2] == '\1' * 10
            assert written_blocks[1][3] == '195ae9b612afbf92d2515cfee898f82f601a9107'

            handle1.stop()

            [hdl.stop() for hdl in handles]
            [wrld.stop() for wrld in worlds]

            world1.stop()
        finally:
            pass
    finally:
        pass
