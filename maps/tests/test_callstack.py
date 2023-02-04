import traceback

from maps.garden.sdk.core import CppException
from maps.garden.sdk.module_rpc import callstack
from maps.garden.sdk.module_rpc.common import convert_exception_to_proto
from maps.garden.sdk.module_rpc.proto.common_pb2 import Exception
from . import callstack_pb2 as proto


def callee():
    return traceback.extract_stack()


def caller():
    return callee()


def test_traceback_capture():
    backtrace = caller()
    assert backtrace[-1].name == "callee"
    assert backtrace[-1].lineno == 11
    assert backtrace[-1].filename.endswith("test_callstack.py")
    assert backtrace[-1].line == "return traceback.extract_stack()"

    assert backtrace[-2].name == "caller"
    assert backtrace[-2].lineno == 15
    assert backtrace[-2].filename.endswith("test_callstack.py")
    assert backtrace[-2].line == "return callee()"


def test_formatting_traceback():
    backtrace = caller()
    formatted = traceback.format_list(backtrace)
    assert "test_callstack.py" in "".join(formatted)


def test_there_and_back_again_traceback():
    backtrace = caller()
    msg = proto.Traceback()
    callstack.convert_traceback_to_proto(backtrace, msg.traceback)

    decoded_backtrace = callstack.decode_traceback_from_proto(msg.traceback)

    assert decoded_backtrace[-1].name == "callee"
    assert decoded_backtrace[-1].lineno == 11
    assert decoded_backtrace[-1].filename.endswith("test_callstack.py")
    assert decoded_backtrace[-1].line == "return traceback.extract_stack()"

    assert decoded_backtrace[-2].name == "caller"
    assert decoded_backtrace[-2].lineno == 15
    assert decoded_backtrace[-2].filename.endswith("test_callstack.py")
    assert decoded_backtrace[-2].line == "return callee()"

    formatted = traceback.format_list(decoded_backtrace)
    assert "test_callstack.py" in "".join(formatted)


def raise_cpp_exception():
    ex = CppException('something went wrong')
    ex.c_trace = traceback.StackSummary.from_list((traceback.FrameSummary('file1', 1, 'test'),
                                                  traceback.FrameSummary('file2', 1, 'test')))
    ex.c_exception = "some_exception"
    raise ex


def test_exception_from_cpp():
    try:
        raise_cpp_exception()
    except CppException as e:
        msg = Exception()
        convert_exception_to_proto(e, e.__traceback__, msg)
        decoded_traceback = callstack.decode_traceback_from_proto(msg.traceback)

        py_traceback_len = len(traceback.extract_tb(e.__traceback__))
        assert len(msg.traceback) == py_traceback_len + len(e.c_trace)
        assert decoded_traceback[-1].filename == e.c_trace[-1].filename
