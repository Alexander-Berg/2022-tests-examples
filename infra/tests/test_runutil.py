import os

import mock

from infra.ya_salt.lib import runutil


def test_lock_run():
    fd = None
    try:
        ok, fd = runutil.lock_run('./test-lock')
        assert ok
        assert fd > 0
    finally:
        if fd is not None:
            os.close(fd)


def test_is_allowed_to_run():
    # Test non existing path
    path = 'should_not_exist'
    f = runutil.Flag(path)
    assert f.is_enforced() == (False, False)
    open(path, 'w').close()
    assert f.is_enforced() == (True, True)
    # Modify timestamp
    st = os.stat(path)
    os.utime(path, (st.st_atime, st.st_mtime - 3 * 3600))
    assert f.is_enforced() == (False, True)


def test_touch():
    f = runutil.Flag('./test-touch-flag')
    # Test new flag creation
    existed, err = f.touch()
    assert not existed
    assert err is None
    # Test adjusting mtime
    utime = mock.Mock()
    existed, err = f.touch(_utime=utime)
    assert existed
    assert err is None
    utime.assert_called_once_with(f.get_path(), None)
