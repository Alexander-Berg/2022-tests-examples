import time
from billing.apikeys.apikeys.mapper import DBLock
from billing.apikeys.apikeys.mapper.exceptions import LockedObject


def test_simple_lock_acquire(mongomock):
    lock = DBLock('TEST_LOCK', timeout=15)
    with lock:
        assert lock._locked
    assert not lock._locked


def test_locked_lock_acquire(mongomock):
    lock1 = DBLock('TEST_LOCK', timeout=150, tries_count=1, tries_timeout=1)
    lock2 = DBLock('TEST_LOCK', timeout=150, tries_count=1, tries_timeout=1)
    with lock1:
        try:
            lock2.acquire()
        except LockedObject:
            pass
        assert lock1._locked
        assert not lock2._locked
    assert not lock1._locked


def test_lock_reacquire(mongomock):
    lock = DBLock('TEST_LOCK', timeout=150, tries_count=1, tries_timeout=1)
    with lock:
        assert lock._locked
    assert not lock._locked
    with lock:
        assert lock._locked
    assert not lock._locked


def test_lock_timeout(mongomock):
    lock1 = DBLock('TEST_LOCK', timeout=1, tries_count=1, tries_timeout=1)
    lock2 = DBLock('TEST_LOCK', timeout=150, tries_count=1, tries_timeout=1)
    lock1.acquire()
    try:
        time.sleep(1.1)
        lock2.acquire()
    except LockedObject:
        assert False
    assert lock2._locked


def test_lock_timeout_retry(mongomock):
    lock1 = DBLock('TEST_LOCK', timeout=2, tries_count=1, tries_timeout=1)
    lock2 = DBLock('TEST_LOCK', timeout=150, tries_count=10, tries_timeout=1)
    lock1.acquire()
    try:
        lock2.acquire()
    except LockedObject:
        assert False
    assert lock2._locked
