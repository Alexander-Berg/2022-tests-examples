import pytest

from smarttv.droideka.utils.decorators import ignore_errors


callback_function_called = False


def sample_callback_function():
    global callback_function_called
    callback_function_called = True


def raise_exc(exc):
    raise exc


key_error = lambda: raise_exc(KeyError)
value_error = lambda: raise_exc(ValueError)
zero_division_error = lambda: raise_exc(ZeroDivisionError)
base_exception = lambda: raise_exc(BaseException)
index_error = lambda: raise_exc(IndexError)

ignore_key_error = ignore_errors(KeyError)
ignore_value_error = ignore_errors(ValueError)
ignore_multiple_errors = ignore_errors((KeyError, ValueError, ZeroDivisionError))
ignore_all_errors = ignore_errors(BaseException)
ignore_with_callback = ignore_errors(BaseException, callback_function=sample_callback_function)


class TestIgnoreErrors:
    def setup_class(self):
        global callback_function_called
        callback_function_called = False

    @pytest.mark.parametrize('decorator,raising_function',
                             [(ignore_key_error, key_error),
                              (ignore_value_error, value_error),
                              (ignore_multiple_errors, key_error),
                              (ignore_multiple_errors, zero_division_error),
                              (ignore_all_errors, value_error),
                              (ignore_all_errors, base_exception)])
    def test_without_errors(self, decorator, raising_function):
        decorator(raising_function)()

    @pytest.mark.parametrize('decorator,raising_function,exception',
                             [(ignore_key_error, value_error, ValueError),
                              (ignore_value_error, key_error, KeyError),
                              (ignore_multiple_errors, index_error, IndexError)])
    def test_errors(self, decorator, raising_function, exception):
        try:
            decorator(raising_function)()
            assert False
        except exception:
            pass

    def test_callback_function(self):
        ignore_with_callback(base_exception)()
        assert callback_function_called
