import errno
import os
import contextlib

from infra.ya_salt.lib import persist
from infra.ya_salt.proto import ya_salt_pb2


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


def test_marshaller_load():
    path = './salt.state'
    marsh = persist.Marshaller(path, ya_salt_pb2.HostmanStatus)
    with remove_after_done(path):
        # Non existing file
        m, err = marsh.load()
        assert m is None
        assert err
        # Too big file
        with open(path, 'w') as f:
            f.write('1' * (persist.Marshaller.MAX_SIZE + 3))
        m, err = marsh.load()
        assert m is None
        assert err == 'file is larger than max_size'
        # Non valid file
        with open(path, 'w') as f:
            f.write('123')
        m, err = marsh.load()
        assert m is None
        assert err.startswith('failed to deserialize')
        # Good case
        m = ya_salt_pb2.HostmanStatus()
        m.apt.last_update_ok.status = 'True'
        with open(path, 'w') as f:
            f.write(m.SerializeToString())
        loaded, err = marsh.load()
        assert err is None
        assert m == loaded


def test_persist_save():
    m = ya_salt_pb2.HostmanStatus()
    m.apt.last_update_ok.status = 'True'
    path = './salt.state'
    marsh = persist.Marshaller(path, ya_salt_pb2.HostmanStatus)
    with remove_after_done(path):
        assert marsh.save(m) is None
        # Check that we can read after saving
        loaded, err = marsh.load()
        assert err is None
        assert m == loaded
