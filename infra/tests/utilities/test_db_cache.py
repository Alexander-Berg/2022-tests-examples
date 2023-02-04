"""Tests database cache."""

from unittest.mock import call, Mock

import pytest

from sepelib.mongo.mock import ObjectMocker
from walle.models import timestamp, monkeypatch_timestamp
from walle.util import db_cache
from walle.util.db_cache import _Cache
from walle.util.db_cache_gc import _gc, MAX_CACHE_TTL


class DbCacheTTLExpired(Exception):
    pass


class DbCacheNotFound(Exception):
    pass


def get_cache_value(cache_id, ttl):
    value_time, value = db_cache.get_value(cache_id)
    if value_time is None:
        raise DbCacheNotFound("cache key '{}' not found".format(cache_id))
    if timestamp() - value_time >= ttl:
        raise DbCacheTTLExpired("cache key '{}' ttl expired".format(cache_id))
    return value


@pytest.fixture(autouse=True)
def test(database):
    pass


class MockException(Exception):
    pass


def function_mock(gen=None, **kwargs):
    mock = Mock(**kwargs)

    if gen:
        fn = gen(mock)
    else:

        def fn(*args, **kwargs):
            return mock(*args, **kwargs)

    fn.mock_calls = mock.mock_calls
    return fn


def test_empty():
    cache = ObjectMocker(db_cache._Cache)
    assert db_cache.get_value("test") == (None, None)
    cache.assert_equal()


def test_get_and_set(monkeypatch):
    cache_mocker = ObjectMocker(db_cache._Cache)

    monkeypatch_timestamp(monkeypatch, 1)
    cache1 = _Cache(id="test1", time=1, value="test1-value", expire=11)
    cache2 = _Cache(id="test2", time=1, value="test2-value", expire=11)
    for cache in cache1, cache2:
        db_cache.set_value(cache.id, cache.value, 10)
        cache_mocker.add(cache)
    cache_mocker.assert_equal()

    assert db_cache.get_value("missing") == (None, None)
    for cache in cache1, cache2:
        assert db_cache.get_value(cache.id) == (cache.time, cache.value)

    for index in range(2):
        monkeypatch_timestamp(monkeypatch, index + 1)
        cache1.time = index + 1
        cache1.value = "test1-updated-value-1"
        db_cache.set_value(cache1.id, cache1.value, 10 - index)
        cache_mocker.assert_equal()


def test_get_cache_key(monkeypatch):
    monkeypatch_timestamp(monkeypatch, 1)
    db_cache.set_value('test1', "test1-value", 10)
    db_cache.set_value('test2', "test2-value", 10)
    assert get_cache_value('test1', 1) == "test1-value"
    assert get_cache_value('test2', 1) == "test2-value"


def test_get_cache_key_not_found(monkeypatch):
    monkeypatch_timestamp(monkeypatch, 1)
    with pytest.raises(DbCacheNotFound):
        get_cache_value('test2', 1)


def test_get_cache_key_expired(monkeypatch):
    monkeypatch_timestamp(monkeypatch, 1)
    db_cache.set_value('test1', "test1-value", 1)
    monkeypatch_timestamp(monkeypatch, 2)
    with pytest.raises(DbCacheTTLExpired):
        get_cache_value('test1', 1)


def test_cache_gc(walle_test, monkeypatch):
    mock_ttl = 10
    monkeypatch_timestamp(monkeypatch, 0)
    db_cache.set_value("test", "test-value", mock_ttl)
    _gc()
    assert get_cache_value("test", 1) == "test-value"
    monkeypatch_timestamp(monkeypatch, mock_ttl + 1)
    _gc()
    with pytest.raises(DbCacheNotFound):
        get_cache_value("test", 1)


def test_cache_legacy_gc(walle_test, monkeypatch):
    monkeypatch_timestamp(monkeypatch, 0)
    db_cache._Cache.objects(id="test").update(set__time=0, set__value="test-value", multi=False, upsert=True)
    _gc()
    assert get_cache_value("test", 1) == "test-value"
    monkeypatch_timestamp(monkeypatch, MAX_CACHE_TTL + 1)
    _gc()
    with pytest.raises(DbCacheNotFound):
        get_cache_value("test", 1)


@pytest.mark.usefixtures("monkeypatch_timestamp")
class TestCachedFunction:
    def test_stores_returned_value(self):
        expected_value = "value-mock"

        func = function_mock(return_value=expected_value)
        cached = db_cache.cached("cache-id-mock", 10)(func)

        assert cached() == expected_value  # first call
        assert cached() == expected_value  # second call
        assert func.mock_calls == [call()]

    def test_uses_provided_key_to_store_values(self):
        for i in range(2):
            expected_value = "value-mock-{}".format(i)
            cache_key = "cache-id-mock-{}".format(i)

            func = function_mock(return_value=expected_value)
            cached = db_cache.cached(cache_key, 10)(func)

            assert cached() == expected_value
            assert func.mock_calls == [call()]

    def test_returned_value_expires_after_timeout(self, monkeypatch):
        expected_value = "value-mock"
        func = function_mock(return_value=expected_value)

        expected_ttl = 10
        cached = db_cache.cached("cache-id-mock", expected_ttl)(func)

        assert cached() == expected_value  # first call
        monkeypatch_timestamp(monkeypatch, timestamp() + expected_ttl)
        assert cached() == expected_value  # second call
        assert func.mock_calls == [call(), call()]

    def test_caches_error_too(self):
        func = function_mock(side_effect=MockException())
        cached = db_cache.cached("cache-id-mock", 10)(func)

        with pytest.raises(MockException):
            cached()  # first call
        with pytest.raises(MockException):
            cached()  # second call

        assert func.mock_calls == [call()]

    def test_caches_error_with_ttl(self, monkeypatch):
        expected_error_ttl = 5

        func = function_mock(side_effect=MockException())
        cached = db_cache.cached("cache-id-mock", 10, error_ttl=expected_error_ttl)(func)

        with pytest.raises(MockException):
            cached()  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + expected_error_ttl + 1)
        with pytest.raises(MockException):
            cached()  # second call

        assert func.mock_calls == [call(), call()]

    def test_returns_expired_value_on_error_if_present(self, monkeypatch):
        expected_value = "value-mock"
        value_ttl = 10

        func = function_mock(side_effect=[expected_value, Exception()])
        cached = db_cache.cached("cache-id-mock", value_ttl)(func)

        assert cached() == expected_value  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + value_ttl)
        assert cached() == expected_value  # second call
        assert func.mock_calls == [call(), call()]

    def test_returns_expired_value_on_cached_error_if_present(self, monkeypatch):
        value_ttl = 10
        expected_value = "value-mock"

        func = function_mock(side_effect=[expected_value, Exception()])
        cached = db_cache.cached("cache-id-mock", value_ttl)(func)

        assert cached() == expected_value  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + value_ttl)
        assert cached() == expected_value  # second call
        assert cached() == expected_value  # second call
        assert func.mock_calls == [call(), call()]


@pytest.mark.usefixtures("test", "monkeypatch_timestamp")
class TestMemoizedFunction:
    def test_stores_returned_value(self):
        expected_value = "value-mock"

        func = function_mock(return_value=expected_value)
        cached = db_cache.memoized("cache-id-mock", 10)(func)

        assert cached("test") == expected_value  # first call
        assert cached("test") == expected_value  # second call
        assert func.mock_calls == [call("test")]

    def test_uses_provided_key_to_store_values(self):
        for i in range(2):
            expected_value = "value-mock-{}".format(i)
            cache_key = "cache-id-mock-{}".format(i)

            func = function_mock(return_value=expected_value)
            cached = db_cache.memoized(cache_key, 10)(func)

            assert cached("test") == expected_value
            assert func.mock_calls == [call("test")]  # check it does not return value from cache

    def test_uses_call_args_to_create_cache_key(self):
        func = function_mock(side_effect=range(3))
        cached = db_cache.memoized("cache-id-mock", 10)(func)

        assert cached("arg-1", test="test") == 0
        assert cached("arg-1", test="test") == 0  # this should return from cache
        assert cached("arg-2", test="test") == 1
        assert cached("arg-2", test="test-1") == 2

        assert func.mock_calls == [
            call("arg-1", test="test"),
            call("arg-2", test="test"),
            call("arg-2", test="test-1"),
        ]

    def test_order_of_args_does_not_matter(self):
        def gen(mock):
            return lambda arg1, arg2=None, *args, **kwargs: mock(arg1, arg2, *args, **kwargs)

        func = function_mock(gen, side_effect=range(3))
        cached = db_cache.memoized("cache-id-mock", 10)(func)

        # change from positional star-arg
        assert cached("arg-1", "arg-2", "arg3", "arg4", kwarg="kwarg") == 0
        assert cached("arg-1", *["arg-2", "arg3", "arg4"], kwarg="kwarg") == 0

        # change from positional to kwargs
        assert cached("arg-1", "arg-2", kwarg="kwarg") == 1
        assert cached("arg-1", arg2="arg-2", kwarg="kwarg") == 1
        assert cached(arg1="arg-1", arg2="arg-2", kwarg="kwarg") == 1

        # use default vs provide value that equals to default
        assert cached("arg-1", None, kwarg="kwarg") == 2
        assert cached(arg1="arg-1", arg2=None, kwarg="kwarg") == 2
        assert cached(arg1="arg-1", kwarg="kwarg") == 2

        assert func.mock_calls == [
            call("arg-1", "arg-2", "arg3", "arg4", kwarg="kwarg"),
            call("arg-1", "arg-2", kwarg="kwarg"),
            call("arg-1", None, kwarg="kwarg"),
        ]

    def test_returned_value_expires_after_timeout(self, monkeypatch):
        expected_value = "value-mock"
        func = function_mock(return_value=expected_value)

        expected_ttl = 10
        cached = db_cache.memoized("cache-id-mock", expected_ttl)(func)

        assert cached("test") == expected_value  # first call
        monkeypatch_timestamp(monkeypatch, timestamp() + expected_ttl)
        assert cached("test") == expected_value  # second call
        assert func.mock_calls == [call("test"), call("test")]

    def test_updating_one_value_does_not_affect_ttl_of_other_values(self, monkeypatch):
        expected_value = "value-mock"
        func = function_mock(return_value=expected_value)

        expected_ttl = 10
        interim_ttl = expected_ttl // 2
        cached = db_cache.memoized("cache-id-mock", expected_ttl)(func)

        assert cached("test") == expected_value  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + interim_ttl)
        assert cached("production") == expected_value  # some other call
        assert cached("test") == expected_value  # second call, should return from cache

        monkeypatch_timestamp(monkeypatch, timestamp() + interim_ttl)
        assert cached("test") == expected_value  # last call, cache should expire

        assert func.mock_calls == [call("test"), call("production"), call("test")]

    def test_caches_error_too(self):
        func = function_mock(side_effect=MockException())
        cached = db_cache.memoized("cache-id-mock", 10)(func)

        with pytest.raises(MockException):
            cached("test")  # first call
        with pytest.raises(MockException):
            cached("test")  # second call

        assert func.mock_calls == [call("test")]

    def test_uses_call_args_for_error_cache_too(self):
        func = function_mock(side_effect=MockException())
        cached = db_cache.memoized("cache-id-mock", 10)(func)

        with pytest.raises(MockException):
            cached("test-1")

        with pytest.raises(MockException):
            cached("test-2")

        assert func.mock_calls == [call("test-1"), call("test-2")]

    def test_caches_error_with_ttl(self, monkeypatch):
        expected_error_ttl = 5

        func = function_mock(side_effect=MockException())
        cached = db_cache.memoized("cache-id-mock", 10, error_ttl=expected_error_ttl)(func)

        with pytest.raises(MockException):
            cached("test")  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + expected_error_ttl + 1)
        with pytest.raises(MockException):
            cached("test")  # second call

        assert func.mock_calls == [call("test"), call("test")]

    def test_returns_expired_value_on_error_if_present(self, monkeypatch):
        expected_value = "value-mock"
        value_ttl = 10

        func = function_mock(side_effect=[expected_value, Exception()])
        cached = db_cache.memoized("cache-id-mock", value_ttl)(func)

        assert cached("test") == expected_value  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + value_ttl)
        assert cached("test") == expected_value  # second call
        assert func.mock_calls == [call("test"), call("test")]

    def test_returns_expired_value_on_cached_error_if_present(self, monkeypatch):
        value_ttl = 10
        expected_value = "value-mock"

        func = function_mock(side_effect=[expected_value, Exception()])
        cached = db_cache.memoized("cache-id-mock", value_ttl)(func)

        assert cached("test") == expected_value  # first call

        monkeypatch_timestamp(monkeypatch, timestamp() + value_ttl)
        assert cached("test") == expected_value  # second call
        assert cached("test") == expected_value  # third call
        assert func.mock_calls == [call("test"), call("test")]
