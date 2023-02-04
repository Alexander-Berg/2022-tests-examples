import traceback

from maps.garden.sdk.core import GardenError
from maps.garden.libs_server.common.exceptions import (
    ModuleOperationException, TaskError, ExceptionInfo)


def test_exception_info_from_general_exception():
    exception = None
    try:
        raise GardenError("Hello")
    except Exception as e:
        exception = e

    exc_info = ExceptionInfo.from_exception(exception)

    assert exc_info.type == "maps.garden.sdk.core.exceptions.GardenError"
    assert exc_info.message == "Hello"
    assert "raise GardenError(\"Hello\")" in exc_info.traceback


def test_exception_info_from_task_error():
    exception = None
    try:
        raise GardenError("Hello")
    except Exception:
        exception = TaskError.capture()

    exc_info = ExceptionInfo.from_exception(exception)

    assert exc_info.type == "maps.garden.sdk.core.exceptions.GardenError"
    assert exc_info.message == "Hello"
    assert "raise GardenError(\"Hello\")" in exc_info.traceback


def test_exception_info_from_module_operation_exception():
    exception = None
    try:
        raise GardenError("Hello")
    except Exception as e:
        exception = ModuleOperationException(
            operation_name="invoke_task",
            exception_typename="maps.garden.sdk.core.exceptions.GardenError",
            exception_parents=["Exception"],
            exception_message="Hello",
            traceback=traceback.extract_tb(e.__traceback__),
        )

    exc_info = ExceptionInfo.from_exception(exception)

    assert exc_info.type == "maps.garden.sdk.core.exceptions.GardenError"
    assert exc_info.message == "Hello"
    assert "raise GardenError(\"Hello\")" in exc_info.traceback
