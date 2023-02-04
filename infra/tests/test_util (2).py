import fcntl
import os
import pytest
import re
import sys
import yatest.common

from infra.rtc.juggler.bundle import util


def test_get_file_content():
    text = util.get_file_content('/proc/uptime')
    assert bool(re.match(r'^[0-9]+\.[0-9]+ [0-9]+\.[0-9]+$', text))


def test_get_process_info__default():
    info = util.get_process_info(1)
    assert ['create_time', 'name', 'status'] == sorted(info.keys())


def test_get_process_info__custom_attrs():
    info = util.get_process_info(1, attrs=['cmdline', 'status'])
    assert ['cmdline', 'status'] == sorted(info.keys())


@pytest.mark.skipif(sys.platform != 'linux', reason='Linux supported only')
def test_flock_context_manager():
    filename = ''

    with util.flock() as lock:
        filename = lock.fd.name
        with pytest.raises(BlockingIOError):
            fcntl.flock(open(filename), fcntl.LOCK_EX | fcntl.LOCK_NB)

    assert not os.path.exists(filename)


@pytest.mark.skipif(sys.platform != 'linux', reason='Linux supported only')
def test_flock_context_manager_keep_file():
    filename = ''
    dirname = yatest.common.test_source_path()

    with util.flock(directory=dirname, keep_file=True) as lock:
        filename = lock.fd.name
        with pytest.raises(BlockingIOError):
            fcntl.flock(open(filename), fcntl.LOCK_EX | fcntl.LOCK_NB)

    fcntl.flock(open(filename), fcntl.LOCK_EX | fcntl.LOCK_NB)
    os.unlink(filename)


@pytest.mark.skipif(sys.platform != 'linux', reason='Linux supported only')
def test_flock_standalone_object(request):
    filename = yatest.common.test_source_path() + request.function.__name__
    locker = util.flock(open(filename, 'w'))

    locker.lock()
    with pytest.raises(BlockingIOError):
        fcntl.flock(open(filename), fcntl.LOCK_EX | fcntl.LOCK_NB)

    locker.unlock()
    fcntl.flock(open(filename), fcntl.LOCK_EX | fcntl.LOCK_NB)
    os.unlink(filename)  # provided files should be handled manually
