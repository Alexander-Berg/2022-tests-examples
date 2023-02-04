from unittest.mock import Mock

import pytest
from django.test import override_settings
from smarttv.droideka.utils import is_only_one_true
from smarttv.droideka.utils.decorators import cache_memoize


class TestIsOnlyOneTrue:
    TEST_DATA_OK = [
        [1],
        [''],
        ['1'],
        [1, None],
        [0, None],
        ['', None],
        ['1', None],
        [1, None, None],
        [0, None, None],
        ['', None, None],
        ['1', None, None],
    ]

    TEST_DATA_NOT_OK = [
        [],
        [None],
        [1, 2],
        ['', '1'],
        [1, 2, None],
        ['', '1', None],
    ]

    @pytest.mark.parametrize('input_data', TEST_DATA_OK)
    def test_is_ok(self, input_data):
        assert is_only_one_true(*input_data)

    @pytest.mark.parametrize('input_data', TEST_DATA_NOT_OK)
    def test_is_not_ok(self, input_data):
        assert not is_only_one_true(*input_data)


@pytest.fixture
def locmem_cache():
    """
    Self-cleaned local memory cache
    """
    with override_settings(CACHES={'default': {'BACKEND': 'django.core.cache.backends.locmem.LocMemCache'}}):
        from django.core.cache import cache
        yield
        cache.clear()


class TestCacheMemoize:

    @pytest.fixture
    def printer(self):
        return Mock(__qualname__='mock', side_effect=lambda arg: arg)

    def test_that_mock_works_correct(self, printer):
        assert printer(10) == 10

    def test_without_skip_argument_it_works_as_original(self, locmem_cache, printer):
        printer_10 = cache_memoize(timeout=10)(printer)
        assert printer_10('bar') == 'bar'
        assert printer_10('bar') == 'bar'
        assert len(printer.mock_calls) == 1  # first is executed, second is from cache

    def test_skip_argument_callable(self, locmem_cache, printer):
        # start with enabled cache
        skip = False

        printer_10 = cache_memoize(timeout=10, skipif=lambda arg: skip)(printer)
        assert printer_10('bar') == 'bar'
        assert printer_10('bar') == 'bar'
        assert len(printer.mock_calls) == 1

        # switching cache off
        skip = True
        assert printer_10('foo') == 'foo'
        assert printer_10('foo') == 'foo'

        assert len(printer.mock_calls) == 3  # and we get two more calls passed through

    def test_skip_argument_constant(self, locmem_cache, printer):
        printer_10 = cache_memoize(timeout=10, skipif=True)(printer)
        assert printer_10('bar') == 'bar'
        assert printer_10('bar') == 'bar'
        assert len(printer.mock_calls) == 2
