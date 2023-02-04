import time
from unittest.mock import Mock

import pytest

from walle.clients.utils import retry


class TestRetryWrapper:
    class MyOwnException(Exception):
        pass

    class MyOwnExceptionChild(Exception):
        pass

    def _mock_function(self, side_effect=Exception):
        return Mock(side_effect=side_effect, __name__="mock")

    def test_passes_args_to_function_when_used_as_wrapper(self):
        mock_func = self._mock_function([42])
        wrapped = retry(mock_func, max_tries=1, interval=0)

        wrapped("arg_value", kwarg="kwarg_value")

        mock_func.assert_called_once_with("arg_value", kwarg="kwarg_value")

    def test_passes_args_to_function_when_used_as_decorator(self):
        # this is a decorator, it wraps on a mock
        mock_func = self._mock_function([42])
        wrapped = retry(max_tries=1, interval=0)(mock_func)

        wrapped("arg_value", kwarg="kwarg_value")

        mock_func.assert_called_once_with("arg_value", kwarg="kwarg_value")

    @pytest.mark.parametrize("num_failures", [0, 1, 2])
    def returns_value_when_call_succeeds_if_used_as_decorator(self, num_failures):
        success_value = "success"

        failures = [Exception] * num_failures
        mock_func = self._mock_function(failures + [success_value])
        wrapped = retry(max_tries=1, interval=0, exceptions=[Exception])(mock_func)

        assert success_value == wrapped("arg_value", kwarg="kwarg_value")

    @pytest.mark.parametrize("num_failures", [0, 1, 2])
    def returns_value_when_call_succeeds_if_used_as_wrapper(self, num_failures):
        success_value = "success"

        failures = [Exception] * num_failures
        mock_func = self._mock_function(failures + [success_value])
        wrapped = retry(mock_func, max_tries=1, interval=0, exceptions=[Exception])

        assert success_value == wrapped("arg_value", kwarg="kwarg_value")

    def test_does_requested_amount_of_tries_when_used_as_wrapper(self):
        mock_func = self._mock_function()
        wrapped = retry(mock_func, max_tries=3, interval=0, exceptions=[Exception])

        with pytest.raises(Exception):
            wrapped()

        assert 3 == len(mock_func.mock_calls)

    def test_does_requested_amount_of_tries_when_used_as_decorator(self):
        # this is a decorator but it wraps on a mock
        mock_func = self._mock_function()
        wrapped = retry(max_tries=4, interval=0, exceptions=[Exception])(mock_func)

        with pytest.raises(Exception):
            wrapped()

        assert 4 == len(mock_func.mock_calls)

    def test_does_not_retry_on_unknown_exceptions(self):
        # this is a decorator but it wraps on a mock
        mock_func = self._mock_function(self.MyOwnException)
        wrapped = retry(mock_func, max_tries=2, interval=0, exceptions=[self.MyOwnExceptionChild])

        with pytest.raises(self.MyOwnException):
            wrapped()

        mock_func.assert_called_once_with()

    def test_does_not_retry_on_skipped_exceptions(self):
        # this is a decorator but it wraps on a mock
        mock_func = self._mock_function(self.MyOwnExceptionChild)
        wrapper = retry(max_tries=2, interval=0, exceptions=[self.MyOwnException], skip=[self.MyOwnExceptionChild])
        wrapped = wrapper(mock_func)

        with pytest.raises(self.MyOwnExceptionChild):
            wrapped()

        mock_func.assert_called_once_with()

    def test_waits_for_interval_between_tries(self, mp):
        mock_func = self._mock_function()
        wrapped = retry(mock_func, max_tries=2, interval=10, jitter=0, exceptions=[Exception])

        mock_sleep = mp.function(time.sleep, module=time)

        with pytest.raises(Exception):
            wrapped()

        mock_sleep.assert_called_once_with(10)

    def test_multiplies_interval_by_backoff(self, mp):
        mock_func = self._mock_function()
        wrapped = retry(mock_func, max_tries=5, interval=10, backoff=2, jitter=0, exceptions=[Exception])

        sleeps = []
        mp.function(time.sleep, module=time, side_effect=sleeps.append)

        with pytest.raises(Exception):
            wrapped()

        assert [10, 20, 40, 80] == sleeps

    @pytest.mark.flaky(reruns=3)
    def test_offsets_interval_by_jitter(self, mp):
        mock_func = self._mock_function()
        wrapped = retry(mock_func, max_tries=5, interval=10, backoff=1, jitter=1.0, exceptions=[Exception])

        sleeps = []
        mp.function(time.sleep, module=time, side_effect=sleeps.append)

        with pytest.raises(Exception):
            wrapped()

        class NumWithOffset:
            def __init__(self, num, offset):
                self.num = num
                self.offset = offset

            def __eq__(self, other):
                return self.num < other <= self.num + self.offset

            def __repr__(self):
                return "<NumWithOffset(numer={}, offset={})>".format(self.num, self.offset)

        expected_sleeps = [NumWithOffset(10.0, 1.0)] * 4
        assert expected_sleeps == sleeps

    @pytest.mark.xfail(run=False, reason="some _mock_call crash at the end of test")
    def test_limits_by_time_for_slow_requests(self, mp):
        mock_func = self._mock_function()
        # We set wrapper to make 5 tries. Each try fails, but fails slowly.
        # By the time mix_time expires we should only have two tries attempted.
        # By time is limited, so next tries should not be attempted.
        wrapped = retry(mock_func, max_tries=5, interval=1, max_time=15, exceptions=[Exception])

        mp.function(time.sleep, module=time)
        mp.function(time.time, module=time, side_effect=[0, 10, 20])

        with pytest.raises(Exception):
            wrapped()

        assert 2 == len(mock_func.mock_calls)

    def test_returns_success_result_even_when_time_goes_off(self, monkeypatch):
        success_value = "success"
        mock_func = self._mock_function([Exception, success_value])

        # By the time mix_time expires we should only have two tries attempted.
        # When second attempt finishes, time is already expired, but we've got a result so return it.
        wrapped = retry(mock_func, max_tries=5, interval=1, max_time=15, exceptions=[Exception])

        class FixedReturn:
            values = [0, 10, 20]

            def __init__(self):
                self._index = 0

            def __call__(self):
                res = self.values[self._index]
                self._index += 1
                return res

        with monkeypatch.context() as m:
            m.setattr(time, "sleep", lambda *args, **kwargs: object())
            m.setattr(time, "time", FixedReturn())

            assert success_value == wrapped()
