from __future__ import division

import pytest

import gevent
import hashlib
import logging
import math
import time

import skybone.rbtorrent.skbn.dfs
from skybone.rbtorrent.skbn.dfs import _parse_http_range
from skybone.rbtorrent.skbn.world import World
from skybone.rbtorrent.skbn.shmem import SharedMemory
from skybone.rbtorrent.utils import gevent_urlopen

import skybone.rbtorrent.skbn.handle

from tools import gen_resource


def _gen_worlds(cnt, max_dl_speed):
    for i in range(1, cnt + 1):
        world = World(
            uid=hashlib.sha1(str(i)).hexdigest(),
            desc='W_%d' % (i, ),
            extended_logging=True,
            limit_dl=max_dl_speed
        )
        world.start()
        yield world

@pytest.mark.parametrize(
    'mode', [
        'normal', 'morefiles', 'twopeers', 'twopeers_brokencon',
        'singlepeer_brokencon', 'singlepeer_timeout',
        'chunked', 'misslinks', 'limitspeed'
    ]
)
def test_dfs_dl(mode, tmpdir, monkeypatch, mocker):
    if mode == 'normal':
        fake_data_size = 6 * 1024 * 1024
        files = 1
        chunked = False
        misslinks = False
        max_dl_speed = None
    elif mode == 'morefiles':
        fake_data_size = 64 * 1024
        files = 10
        chunked = False
        misslinks = False
        max_dl_speed = None
    elif mode in ('twopeers', 'twopeers_brokencon'):
        fake_data_size = 32 * 1024 * 1024
        files = 2
        chunked = False
        misslinks = False
        max_dl_speed = None
    elif mode in ('singlepeer_brokencon', 'singlepeer_timeout'):
        fake_data_size = 32 * 1024 * 1024
        files = 1
        chunked = False
        misslinks = False
        max_dl_speed = None
    elif mode == 'chunked':
        fake_data_size = 35 * 1024 * 1024
        files = 2
        chunked = True
        misslinks = False
        max_dl_speed = None
    elif mode == 'misslinks':
        fake_data_size = 35 * 1024 * 1024
        files = 2
        chunked = True
        misslinks = True
        max_dl_speed = None
    elif mode == 'limitspeed':
        fake_data_size = 12 * 1024 * 1024
        files = 1
        chunked = False
        misslinks = False
        max_dl_speed = 4 * 1024 * 1024  # 2 sec for test
    else:
        assert 0, 'Unexpected mode'

    if mode == 'singlepeer_timeout':
        monkeypatch.setattr(skybone.rbtorrent.skbn.dfs, 'READ_TIMEOUT', 0.2)

    resource_desc = {}
    files_by_md5 = {}

    for idx in range(files):
        fake_data = ('%064s%064d' % ('fakefake', idx)) * int(math.ceil(fake_data_size / 128))
        fake_data = fake_data[:fake_data_size]

        #fake_data = open('/dev/urandom', 'rb').read(fake_data_size)
        assert len(fake_data) == fake_data_size

        resource_desc['testfile%03d' % (idx, )] = {'size': len(fake_data), 'data': fake_data}

        md5sum = hashlib.md5(fake_data).hexdigest()
        assert md5sum not in files_by_md5

        files_by_md5[md5sum] = ('testfile%03d' % (idx, ), fake_data)

    uid, head = gen_resource(files=resource_desc)

    class Lookupper(object):
        def look_head(self, infohash):
            return head, None

        def get_link(self, infohash, md5, head):
            assert md5 in files_by_md5
            if not chunked:
                return (
                    'http://mock/%s' % (md5, ),
                    'http://mock2/%s' % (md5, ),
                )
            else:
                total_len = len(files_by_md5[md5][1])

                ## Not even splits is not supported atm
                #splitby = 8 * 10 ** 6
                splitby = 8 * 1024 * 1024

                links = []
                for idx in range(int(math.ceil(total_len / splitby))):
                    start = idx * splitby
                    end = start + splitby
                    size = splitby
                    if end >= total_len:
                        size = total_len - start
                        assert size <= splitby

                    if misslinks and idx == 2:
                        # Skip and miss one chunk
                        continue

                    url_links = []

                    for i in range(1, 3):
                        url_links.append('http://mock%s/%s/%d/%d' % ('' if i == 1 else i, md5, start, size))

                    links.append((start, size, url_links))

                return links

    written_blocks = []
    written_blocks2 = []  # peer2

    class FakeHttpReq(object):
        conn_broke_count = 0
        total_sent = 0

        def __init__(self, data):
            self.data = data
            self.size = len(data)
            self.pos = 0
            self.closed = False

        def read(self, size):
            assert size > 0

            if self.pos > self.size:
                return ''

            if mode == 'twopeers_brokencon' and self.pos >= 12 * 1024 * 1024 and FakeHttpReq.conn_broke_count == 0:
                FakeHttpReq.conn_broke_count += 1
                raise Exception('conn broken!')

            if mode in ('singlepeer_brokencon', 'singlepeer_timeout'):
                def break_conn():
                    if mode == 'singlepeer_brokencon':
                        raise Exception('conn broken!')
                    else:
                        gevent.sleep(0.3)

                if self.pos >= 8 * 1024 * 1024 and FakeHttpReq.conn_broke_count == 0:
                    FakeHttpReq.conn_broke_count += 1
                    break_conn()
                elif self.pos >= 12 * 1024 * 1024 and FakeHttpReq.conn_broke_count == 1:
                    FakeHttpReq.conn_broke_count += 1
                    break_conn()

            end = self.pos + size
            #assert end <= len(self.data), 'req out of boundary %d > %d' % (end, len(self.data))

            ret = self.data[self.pos:end]
            self.pos += size

            FakeHttpReq.total_sent += len(ret)
            return ret

        def close(self):
            self.closed = True

    def _fake_urlopen(uri, data=None, headers={}, timeout=None):
        assert uri.startswith('http://mock/') or uri.startswith('http://mock2/')
        assert data is None

        if not chunked:
            md5 = uri.rsplit('/', 1)[1]
            offset, length = None, None
        else:
            _, md5, offset, length = uri.rsplit('/', 3)
            offset = int(offset)
            length = int(length)

        assert md5 in files_by_md5

        data = files_by_md5[md5][1]

        if offset is not None:
            data = data[offset:]

        if length is not None:
            data = data[:length]

        if mode not in ('twopeers_brokencon', 'singlepeer_brokencon', 'singlepeer_timeout'):
            assert headers == {'Range': 'bytes=0-{}'.format(len(data) - 1)}

        start, end = _parse_http_range(headers['Range'])
        return FakeHttpReq(data[start:end + 1])

    mock_urlopen = mocker.patch('skybone.rbtorrent.skbn.handle.gevent_urlopen', side_effect=_fake_urlopen)

    seed_world = World(
        uid=hashlib.sha1('dfs').hexdigest(),
        desc='W_DFS',
        extended_logging=True
    )
    seed_world.set_lookupper(Lookupper())
    seed_world.start()

    def _get_block(fn, start, length, memory_segment, sha1):
        if mode in ('twopeers', 'twopeers_brokencon'):
            for rfn, data in files_by_md5.values():
                if tmpdir.join(rfn) == fn or tmpdir.join('peer2').join(rfn) == fn:
                    memory_segment.write(data[start:start+length])
                    return True
        else:
            assert False, 'This should not be called in single peer mode'

    def _put_block(fn, start, memory_segment, sha1, quickhash=None):
        assert fn in [tmpdir.join(name).strpath for name, data in files_by_md5.values()]
        written_blocks.append((fn, start, memory_segment, sha1))
        return True

    def _put_block2(fn, start, memory_segment, sha1, quickhash=None):
        assert fn in [tmpdir.join('peer2').join(name).strpath for name, data in files_by_md5.values()]
        written_blocks2.append((fn, start, memory_segment, sha1))
        return True

    dl_world_1, dl_world_2 = _gen_worlds(2, max_dl_speed=max_dl_speed)

    shmem = SharedMemory()
    shmem.create_mmap(size=32 * 1024 * 1024)

    handle1 = dl_world_1.handle(uid)
    handle1.set_shared_memory(shmem)
    handle1.add_peer('127.0.0.1', seed_world.sock.getsockname()[1])
    handle1.set_allow_dfs(True)

    if mode in ('twopeers', 'twopeers_brokencon'):
        handle2 = dl_world_2.handle(uid)
        handle2.set_shared_memory(shmem)

        handle2.add_peer('127.0.0.1', seed_world.sock.getsockname()[1])
        handle2.add_peer('127.0.0.1', dl_world_1.sock.getsockname()[1])
        handle1.add_peer('127.0.0.1', dl_world_2.sock.getsockname()[1])

        handle2.set_allow_dfs(True)

    # Precreate files on disc
    for fn, _ in files_by_md5.values():
        tmpdir.join(fn).open(mode='wb')
        if mode in ('twopeers', 'twopeers_brokencon'):
            tmpdir.join('peer2').ensure(dir=1).join(fn).open(mode='wb')

    head_ready = handle1.get_head()
    head_ready.wait(timeout=1)

    ts = time.time()

    data1_ready = handle1.get_data(
        tmpdir, _get_block, _put_block,
        nocheck=[tmpdir.join(name) for name, data in files_by_md5.values()]
    )

    if mode in ('twopeers', 'twopeers_brokencon'):
        head2_ready = handle2.get_head()
        head2_ready.wait(timeout=1)

        data2_ready = handle2.get_data(
            tmpdir.join('peer2'), _get_block, _put_block2,
            nocheck=[tmpdir.join('peer2').join(name) for name, data in files_by_md5.values()]
        )

    if not misslinks:
        if mode in ('limitspeed', ):
            data1_ready.wait(timeout=5)
        elif mode in ('singlepeer_brokencon', 'singlepeer_timeout'):
            data1_ready.wait(timeout=10)
        else:
            data1_ready.wait(timeout=2)
        te = time.time()
        assert data1_ready.ready(), 'peer1 async get not ready'
        data1_ready.get_nowait()
    else:
        data1_ready.wait(timeout=0.5)
        te = time.time()
        assert not data1_ready.ready(), 'peer1 async get should not be ready with misslinks'

    if mode in ('limitspeed', ):
        time_passed = te - ts
        assert time_passed > 2.00, 'speed limiting does not working'
        assert time_passed < 2.05, 'speed limiting does not working properly'

    if mode in ('twopeers', 'twopeers_brokencon'):
        data2_ready.wait(timeout=2)
        assert data2_ready.ready(), 'peer2 async get not ready'
        data2_ready.get_nowait()

    if mode == 'twopeers_brokencon':
        assert mock_urlopen.call_count == files + 1
    elif mode in ('singlepeer_brokencon', 'singlepeer_timeout'):
        assert mock_urlopen.call_count == files + 2
        headers = [kwargs['headers'] for args, kwargs in mock_urlopen.call_args_list]
        assert headers == [
            {'Range': 'bytes=0-{}'.format(fake_data_size - 1)},
            {'Range': 'bytes={}-{}'.format(8 * 1024 * 1024, fake_data_size - 1)},
            {'Range': 'bytes={}-{}'.format(20 * 1024 * 1024, fake_data_size - 1)},
        ]
    elif mode == 'chunked':
        assert mock_urlopen.call_count == 5 * files  # 5 chunks in each file
    elif mode == 'misslinks':
        assert mock_urlopen.call_count == 0  # should not download anything
    else:
        assert mock_urlopen.call_count == files
