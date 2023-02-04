import sys

from maps.garden.sdk.core import GardenError
from maps.garden.libs_server.common.exceptions import TaskError


def test_task_error():
    e = TaskError(GardenError("asdf"), "backtrace")
    e.operation_id = "asdf-1234"
    e.log_url = "//tmp/garden/production/tasks/ymapsdf/5208972538986170150/log"

    assert str(e) == "Task produced an error:\nbacktrace"

    try:
        def test_wrapper():
            raise ValueError("Test message")
        test_wrapper()
    except ValueError as e:
        traceback = sys.exc_info()[2]
        exception_with_traceback = TaskError.create(e, traceback)
        exception_without_traceback = TaskError.create(e)

    assert "test_wrapper" in str(exception_with_traceback)
    assert "test_wrapper" not in str(exception_without_traceback)
