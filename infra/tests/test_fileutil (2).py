import contextlib
import errno
import os

from infra.rtc.certman import fileutil


@contextlib.contextmanager
def remove_after_done(path):
    yield
    try:
        os.unlink(path)
    except EnvironmentError as e:
        if e.errno == errno.ENOENT:
            pass
        else:
            raise


def test_atomic_write():
    """
    Trivial test that it somehow works.
    """
    path = './test-file'
    data = 'some data'
    with remove_after_done(path):
        fileutil.atomic_write(path, data, times=(1, 1))
        with open(path) as f:
            assert f.read() == data
            assert os.fstat(f.fileno()).st_mtime == 1


def test_read_file():
    path = './test-file'
    with remove_after_done(path):
        buf, err = fileutil.read_file(path)
        assert err is not None
        with open(path, 'w') as f:
            f.write('8' * (5 * 1024))
        buf, err = fileutil.read_file(path)
        assert err == 'file is larger than max_size'
        with open(path, 'w') as f:
            f.write('8')
        buf, err = fileutil.read_file(path)
        assert err is None
        assert buf == '8'
