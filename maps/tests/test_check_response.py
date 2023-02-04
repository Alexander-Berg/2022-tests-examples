from asyncio import coroutine
from unittest.mock import Mock

import pytest

from maps_adv.warden.client.lib.client import Client
from maps_adv.warden.client.lib.exceptions import (
    BadGateway,
    Conflict,
    ExecutorIdAlreadyUsed,
    GatewayTimeout,
    InternalServerError,
    StatusSequenceViolation,
    TaskInProgressByAnotherExecutor,
    TaskTypeAlreadyAssigned,
    TooEarlyForNewTask,
    UnknownError,
    UnknownResponse,
    UnknownResponseBody,
    UnknownTaskOrType,
    UpdateStatusToInitial,
    ValidationError,
)
from maps_adv.warden.client.tests import dt, dt_to_proto
from maps_adv.warden.proto.errors_pb2 import Error

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status_code", [200, 500, 502, 504, 555])
async def test_does_not_raise_exception_for_expected_status_response(status_code):
    response = Mock()
    response.status = status_code

    await Client._check_response(response, [status_code])


async def test_raises_exception_for_unknown_error_status_code():
    response = Mock()
    response.status = 555
    response.read = coroutine(lambda: "error message")

    with pytest.raises(UnknownResponse) as exc_info:
        await Client._check_response(response, [200])

    assert exc_info.value.args == ("Status=555, payload=error message",)


@pytest.mark.parametrize(
    ["status_code", "expected_exception"],
    [(500, InternalServerError), (502, BadGateway), (504, GatewayTimeout)],
)
async def test_raises_exception_for_not_protobuf_error(status_code, expected_exception):
    response = Mock()
    response.status = status_code

    with pytest.raises(expected_exception):
        await Client._check_response(response, [200])


@pytest.mark.parametrize("response_status", (400, 403, 404, 409))
@pytest.mark.parametrize(
    ["proto_error", "expected_exception", "expected_exception_args"],
    [
        (
            Error(
                code=Error.ERROR_CODE.TOO_EARLY_FOR_NEW_TASK_OF_REQUESTED_TYPE,
                scheduled_time=dt_to_proto(dt("2019-12-01 06:07:00")),
            ),
            TooEarlyForNewTask,
            ("Try to start new task after 2019-12-01 06:07:00",),
        ),
        (Error(code=Error.ERROR_CODE.CONFLICT), Conflict, tuple()),
        (
            Error(
                code=Error.ERROR_CODE.STATUS_SEQUENCE_VIOLATION,
                description="Update from time_1 to time_2 is not allowed.",
            ),
            StatusSequenceViolation,
            ("Update from time_1 to time_2 is not allowed.",),
        ),
        (
            Error(code=Error.ERROR_CODE.TASK_IN_PROGRESS_BY_ANOTHER_EXECUTOR),
            TaskInProgressByAnotherExecutor,
            tuple(),
        ),
        (
            Error(code=Error.ERROR_CODE.TASK_TYPE_ALREADY_ASSIGNED),
            TaskTypeAlreadyAssigned,
            tuple(),
        ),
        (Error(code=Error.ERROR_CODE.UNKNOWN_TASK_OR_TYPE), UnknownTaskOrType, tuple()),
        (
            Error(code=Error.ERROR_CODE.UPDATE_STATUS_TO_INITIAL),
            UpdateStatusToInitial,
            tuple(),
        ),
        (
            Error(code=Error.ERROR_CODE.EXECUTOR_ID_ALREADY_USED),
            ExecutorIdAlreadyUsed,
            tuple(),
        ),
        (
            Error(
                code=Error.ERROR_CODE.VALIDATION_ERROR,
                description="point_type: Invalid value",
            ),
            ValidationError,
            ("point_type: Invalid value",),
        ),
    ],
)
async def test_raises_exception_for_protobuf_error(
    response_status, proto_error, expected_exception, expected_exception_args
):
    response = Mock()
    response.status = response_status
    response.read = coroutine(lambda: proto_error.SerializeToString())

    with pytest.raises(expected_exception) as exc_info:
        await Client._check_response(response, [200])

    assert exc_info.value.args == expected_exception_args


async def test_raises_exception_for_bad_proto_error():
    response = Mock()
    response.status = 409
    response.read = coroutine(lambda: b"corrupted proto error content")

    with pytest.raises(UnknownResponseBody) as exc_info:
        await Client._check_response(response, [200])

    assert exc_info.value.args == (
        "Status=409, payload=b'corrupted proto error content'",
    )


async def test_raises_exception_for_unknown_proto_error(mocker):
    def make_error_mock(_):
        error_mock = Mock()
        error_mock.code = 55555
        error_mock.description = "error description"
        return error_mock

    mocker.patch(
        "maps_adv.warden.client.lib.client.Error.FromString", new=make_error_mock
    )

    response = Mock()
    response.status = 409
    response.read = coroutine(lambda: b"proto error content")

    with pytest.raises(UnknownError) as exc_info:
        await Client._check_response(response, [200])

    assert exc_info.value.args == (
        "Status=409, error_code=55555, description=error description",
    )
