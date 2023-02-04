import errno

import pytest
import gevent
import pyjack

from skybone.rbtorrent.diskio.io import IO


def test_simple_read(tmpdir):
    io = IO(2)

    file1 = tmpdir.join('file')
    file1.write('0123456789abcdef\n' * 2)
    assert io.read_block(None, file1.strpath, 0, 12) == '0123456789ab'


def test_simple_truncate(tmpdir):
    io = IO(2)

    file1 = tmpdir.join('file')
    file1.write('abcde')

    io.truncate(file1.strpath, 3)
    assert file1.read() == 'abc'
    assert file1.size() == 3


def test_thread_selection():
    io = IO(32)

    for i in range(64):
        io.noop(0.001)

    assert io.threads[0].apply_count == 64
    for thread in io.threads[1:]:
        assert thread.apply_count == 0

    def _async():
        io.noop(0.01)

    io = IO(32)
    grns = [gevent.spawn(_async) for _ in range(640)]
    gevent.joinall(grns)
    [grn.get() for grn in grns]

    expected_cnt = 640 / 32

    for thr in io.threads:
        assert (expected_cnt - 3) <= thr.apply_count <= (expected_cnt + 3)

    io = IO(32)

    def _async():
        for i in range(320):
            io.noop(0.0001)

    grns = [gevent.spawn(_async) for _ in range(2)]
    gevent.joinall(grns)
    [grn.get() for grn in grns]

    assert io.threads[0].apply_count + io.threads[1].apply_count == 640

    for thr in io.threads[2:]:
        assert thr.apply_count == 0


def test_exception():
    io = IO(2)

    with pytest.raises(IOError):
        io.noop(-1)


def test_read():
    io = IO(2)
    d1 = io.read_block(None, '/etc/hosts', 0, 12)
    d2 = io.read_block(None, '/etc/hosts', 2, 10)

    assert d1[2:] == d2
    assert io.worker.stats['open_cnt'] == 2
    assert io.worker.stats['read_cnt'] == 2
    assert io.worker.stats['read_bytes'] == 12 + 10


def test_read_fail():
    io = IO(2)

    with pytest.raises(OSError):
        io.read_block(None, '/etc/nonexistent', 0, 10)

    assert io.worker.stats['open_cnt_fail'] == 1


def test_write(tmpdir):
    io = IO(2)

    file1 = tmpdir.join('file').ensure(file=1)
    io.write_block(None, file1.strpath, 0, 'some data')
    assert file1.read() == 'some data'


def test_write_truncate(tmpdir):
    io = IO(2)

    file1 = tmpdir.join('file')
    file1.write('some big data')

    io.write_block(None, file1.strpath, 5, 'lol')
    assert file1.read() == 'some lol data'

    io.write_block(None, file1.strpath, 2, ' lol', truncate=True)
    assert file1.read() == 'so lol'


def test_stats():
    io = IO(2)

    st = io.stats()

    assert st['thread_counts'] == [0, 0]


def test_read_enoent():
    io = IO(2)

    with pytest.raises(OSError):
        try:
            io.read_block(None, 'fakefile', 0, 1)
        except IOError as ex:
            assert ex.errno == errno.ENOENT
            raise


def test_write_enoent():
    io = IO(2)

    with pytest.raises(OSError):
        try:
            io.write_block(None, 'fakefile', 0, 'data')
        except IOError as ex:
            assert ex.errno == errno.ENOENT
            raise


def test_unexpected_error_during_open():
    import os

    io = IO(2)

    class Ex(Exception):
        pass

    def _open(*args, **kwargs):
        raise Ex('oops')

    new_open = pyjack.connect(os.open, _open)

    try:
        with pytest.raises(Ex):
            io.read_block(None, 'fakefile', 0, 1)
    finally:
        new_open.restore()
