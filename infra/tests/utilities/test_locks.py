import time
from datetime import timedelta, datetime

import gevent
import mongoengine
import pytest
from apscheduler.schedulers.gevent import GeventScheduler as Scheduler
from gevent.event import Event
from pymongo.errors import ConnectionFailure

from infra.walle.server.tests.lib.util import TestCase
from walle.application import app
from walle.util import cloud_tools

# noinspection PyProtectedMember
from walle.util.mongo.lock import (
    start_heartbeat,
    stop_heartbeat,
    RepeatedAcquireError,
    _HB,
    _LockWrapper,
    InterruptableLock,
    MongoLock,
    LockIsExpiredError,
    LockError,
)


# TODO(rocco66): remove it
@pytest.fixture(params=(InterruptableLock,))
def LockClass(request):
    return request.param


@pytest.fixture
def test(request, database, mp):
    scheduler = Scheduler()
    scheduler.start()
    request.addfinalizer(scheduler.shutdown)
    start_heartbeat(scheduler)
    request.addfinalizer(stop_heartbeat)
    return TestCase.create(request)


def mongolock_exists(**kwargs) -> bool:
    if "instance" not in kwargs:
        kwargs["instance"] = cloud_tools.get_process_identifier()
    try:
        MongoLock.objects(**kwargs).get()
        return True
    except mongoengine.DoesNotExist:
        return False


def test_lock_wrapper(test):
    lock = _LockWrapper("test")
    lock.acquire(timedelta(seconds=1))
    assert lock.acquired
    assert mongolock_exists(locked_object_id="test")
    lock.release()
    assert not lock.acquired
    assert not mongolock_exists(locked_object_id="test")


def test_interruptable_lock_sole(test, LockClass):
    with LockClass("test") as lock:
        assert lock.acquired()
        assert mongolock_exists(locked_object_id="test")

    assert not mongolock_exists(locked_object_id="test")


def test_interruptable_lock_different(test, LockClass):
    with LockClass("test1") as lock1:
        assert lock1.acquired()
        assert mongolock_exists(locked_object_id="test1")

        with LockClass("test2") as lock2:
            assert lock1.acquired()
            assert mongolock_exists(locked_object_id="test1")
            assert lock2.acquired()
            assert mongolock_exists(locked_object_id="test2")

    assert not mongolock_exists(locked_object_id="test1")
    assert not mongolock_exists(locked_object_id="test2")


def test_nonblocking_sole(test, LockClass):
    with LockClass("test", blocking=False) as lock:
        assert lock.acquired()
        assert mongolock_exists(locked_object_id="test")

    assert not mongolock_exists(locked_object_id="test")


def test_nonblocking(test, LockClass):
    with LockClass("test", blocking=False) as locker:
        assert locker.acquired()
        assert mongolock_exists(locked_object_id="test")

        with pytest.raises(LockError):
            with LockClass("test", blocking=False):
                pass

        assert locker.acquired()
        assert mongolock_exists(locked_object_id="test")

    assert not mongolock_exists(locked_object_id="test")


def test_lock_contenders(test, LockClass):
    event = Event()

    def contender():
        assert not LockClass("test").acquire(blocking=False)
        event.set()

        with LockClass("test"):
            pass

    with LockClass("test"):
        contender_greenlet = gevent.spawn(contender)
        event.wait(timeout=1)
        with pytest.raises(gevent.Timeout):
            contender_greenlet.get(timeout=1)

    contender_greenlet.get()

    assert not mongolock_exists(locked_object_id="test")


@pytest.mark.slow
@pytest.mark.parametrize("concurrency", [1, 2, 5, 10, 20])
def test_lock_concurrency(test, concurrency):
    def locker():
        for i in range(1000 // concurrency):
            with InterruptableLock("test"):
                pass

    jobs = [gevent.spawn(locker) for i in range(concurrency)]
    gevent.joinall(jobs, raise_error=True)

    assert not mongolock_exists(locked_object_id="test")


@pytest.mark.slow
def test_lock_interruption(test, LockClass):
    break_started = Event()
    break_finished = Event()

    def breaker():
        break_started.wait()
        MongoLock.objects(locked_object_id="test").delete()
        break_finished.set()
        _HB.run()

    with LockClass("test", locking_time=1):
        gevent.spawn(breaker)
        with pytest.raises(LockIsExpiredError):
            break_started.set()
            break_finished.wait()
            time.sleep(0.3)

    assert not mongolock_exists(locked_object_id="test")


@pytest.mark.slow
def test_lock_interruption_inner(test, LockClass):
    break_started = Event()
    break_finished = Event()

    def breaker():
        break_started.wait()
        MongoLock.objects(locked_object_id="test").delete()
        break_finished.set()
        _HB.run()

    with LockClass("test", locking_time=1):
        gevent.spawn(breaker)
        with pytest.raises(LockIsExpiredError):
            with LockClass("test2", locking_time=1):
                break_started.set()
                break_finished.wait()
                time.sleep(0.3)
        assert not mongolock_exists(locked_object_id="test2")

    assert not mongolock_exists(locked_object_id="test")


@pytest.mark.slow
def test_interruption_and_acquire(test, LockClass):
    break_started = Event()
    break_finished = Event()

    def breaker():
        break_started.wait()
        MongoLock.objects(locked_object_id="test").delete()
        break_finished.set()
        _HB.run()

    def contender():
        with LockClass("test"):
            pass

    with LockClass("test", locking_time=1):
        contender_greenlet = gevent.spawn(contender)
        gevent.spawn(breaker)
        with pytest.raises(LockIsExpiredError):
            break_started.set()
            break_finished.wait()
            time.sleep(0.3)

    contender_greenlet.get()
    assert not mongolock_exists(locked_object_id="test")


@pytest.mark.slow
@pytest.mark.skip
@pytest.mark.skip_on_cov
def test_timeout_extending(test, mp):
    extend_event = Event()
    acquire_event = Event()
    extend_timeout = _LockWrapper.extend_timeout

    def mock_extend(self, extending_time: timedelta):
        acquire_event.wait()
        if datetime.utcnow() + extending_time >= self.locked_until:
            extend_timeout(self, extending_time)
            extend_event.set()

    mp.method(_LockWrapper.extend_timeout, obj=_LockWrapper, side_effect=mock_extend)
    mp.setattr(InterruptableLock, 'EXTENDING_DIVISOR', 1)
    with InterruptableLock("test", locking_time=timedelta(minutes=3)):
        for i in range(2):
            acquire_event.clear()
            extend_event.clear()
            current_timeout = MongoLock.objects(locked_object_id="test").get().locked_until
            acquire_event.set()
            extend_event.wait()
            assert MongoLock.objects(locked_object_id="test").get().locked_until > current_timeout


def test_double_acquiring(test):
    lock = InterruptableLock("test")
    assert lock.acquire()
    with pytest.raises(RepeatedAcquireError):
        with lock:
            pass

    assert lock.acquired()
    lock.release()


def test_heartbeat_retry_when_release(test, mp):
    extend_event = Event()

    def mock_extend(*args, **kwargs):
        extend_event.set()
        raise ConnectionFailure()

    mp.method(_LockWrapper.extend_timeout, obj=_LockWrapper, side_effect=mock_extend)
    lock = InterruptableLock("test")
    assert lock.acquire()
    extend_event.wait()
    lock.release()
    assert not lock.acquired()


@pytest.mark.slow
@pytest.mark.skip
@pytest.mark.skip_on_cov
def test_ttl(test, mp):
    release_event = Event()
    MongoLock.ensure_indexes()

    def mock_release(*args, **kwargs):
        release_event.set()

    mp.method(_LockWrapper.release, obj=_LockWrapper, side_effect=mock_release)
    mp.method(_LockWrapper.extend_timeout, obj=_LockWrapper)
    with InterruptableLock("test", locking_time=0):
        pass
    release_event.wait()
    time.sleep(140)
    assert not mongolock_exists(locked_object_id="test")
