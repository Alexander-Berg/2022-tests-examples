"""Tests rate limiter"""

import time

import gevent
import pytest

from sepelib.core.exceptions import LogicalError
from walle.errors import TooManyRequestsError
from walle.util.rate_limiter import RateLimiter, LimitManager, RPSRateLimiter, ConcurrentRateLimiter


def test_rate_limiter_with_size_one():
    limiter = RPSRateLimiter("mock", 1)
    with limiter.check():
        pass
    with pytest.raises(TooManyRequestsError):
        with limiter.check():
            pass
    time.sleep(1)
    with limiter.check():
        pass
    with pytest.raises(TooManyRequestsError):
        with limiter.check():
            pass


def test_rate_limiter_with_size_ten():
    limiter = RPSRateLimiter("mock", 10)
    for _ in range(10):
        with limiter.check():
            pass
    with pytest.raises(TooManyRequestsError):
        with limiter.check():
            pass
    time.sleep(1)
    for _ in range(10):
        with limiter.check():
            pass
    with pytest.raises(TooManyRequestsError):
        with limiter.check():
            pass


def test_limit_manager_initialize():
    manager = LimitManager()
    assert len(manager.limiters.keys()) == 0

    func_name, rps = 'test', 0
    manager.initialize(func_name, rps, None)
    assert len(manager.limiters.keys()) == 1
    assert func_name in manager.limiters


def test_limiter_selector_rps():
    manager = LimitManager()
    manager.initialize("mock", rps=1)
    assert isinstance(manager.limiters["mock"], RPSRateLimiter)


def test_limiter_selector_concurrency():
    manager = LimitManager()
    manager.initialize("mock", max_concurrent=1)
    assert isinstance(manager.limiters["mock"], ConcurrentRateLimiter)


def test_limiter_selector_empty():
    manager = LimitManager()
    manager.initialize("mock")
    assert isinstance(manager.limiters["mock"], RateLimiter)


def test_limiter_selector_error():
    manager = LimitManager()
    with pytest.raises(LogicalError):
        manager.initialize("mock", rps=1, max_concurrent=1)


@pytest.mark.slow
@pytest.mark.flaky(reruns=3, reruns_delay=20)
class TestConcurrentLimiter:
    @staticmethod
    def _concurrent_spawn(limiter):
        try:
            with limiter.check():
                gevent.sleep(1)
                return True
        except TooManyRequestsError:
            return False

    def test_limited_concurrency(self):
        limiter = ConcurrentRateLimiter("mock", 2, 0.1)
        greenlets = []

        for _ in range(3):
            greenlets.append(gevent.spawn(lambda: self._concurrent_spawn(limiter)))

        assert sorted([greenlet.get() for greenlet in greenlets]) == sorted([False, True, True])

    def test_delayed_concurrency(self):
        limiter = ConcurrentRateLimiter("mock", 2, 2)
        greenlets = []

        for _ in range(3):
            greenlets.append(gevent.spawn(lambda: self._concurrent_spawn(limiter)))

        assert sorted([greenlet.get() for greenlet in greenlets]) == sorted([True, True, True])
