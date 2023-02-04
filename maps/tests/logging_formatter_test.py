import traceback
import logging
from io import StringIO
from maps.garden.sdk.utils import logging_config


class CppException(Exception):
    pass


def _raise_cpp_exception():
    ex = CppException("something went wrong")
    ex.c_trace = traceback.StackSummary.from_list((traceback.FrameSummary("file1", 1, "test1"),
                                                   traceback.FrameSummary("file2", 1, "test2")))
    ex.c_exception = "some_exception"
    raise ex


def _raise_simple_exception():
    raise RuntimeError("Some error")


def _exception_wrapper(func):
    def wrapped(*args, **kwargs):
        string_io = StringIO()
        my_logger = logging.getLogger()
        my_logger.handlers.insert(0, logging.StreamHandler(string_io))
        my_logger.handlers[0].setFormatter(logging_config.CustomFormatter())

        try:
            func()
        except Exception:
            logging.exception("\nSome exception occurred")

        my_logger.handlers.pop(0)
        # throw away line with time
        return "\n".join(string_io.getvalue().split("\n")[1:])
    return wrapped


@_exception_wrapper
def test_logging_formatter_cpp_exception():
    _raise_cpp_exception()


@_exception_wrapper
def test_logging_formatter_simple_exception():
    _raise_simple_exception()
