from datetime import datetime

import pytest
from aiohttp.web import Response
from dateutil.tz import tzutc

from maps_adv.warden.client.lib import UnknownResponse
from maps_adv.warden.client.lib.exceptions import (
    StatusSequenceViolation,
    TaskInProgressByAnotherExecutor,
    UnknownTaskOrType,
    UpdateStatusToInitial,
    ExecutorIdAlreadyUsed,
    ValidationError,
)
from maps_adv.warden.client.tests import dt, dt_to_proto
from maps_adv.warden.proto.errors_pb2 import Error
from maps_adv.warden.proto.tasks_pb2 import UpdateTaskInput, UpdateTaskOutput

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def rmock(rmock):
    return lambda h: rmock("/tasks/", "PUT", h)


@pytest.mark.parametrize("status", [200, 204])
async def test_request_with_metadata_maked_as_expected(status, rmock, make_client):
    async def handler(req):
        assert str(req.url) == "http://warden.server/tasks/"
        assert req.headers["content-type"] == "application/octet-stream"

        in_data = await req.read()
        expected_in_data = UpdateTaskInput(
            type_name="export",
            task_id=10,
            executor_id="executor0",
            status="completed",
            metadata='{"some": "json"}',
        ).SerializeToString()
        assert in_data == expected_in_data

        return Response(body=UpdateTaskOutput().SerializeToString(), status=status)

    rmock(handler)
    client = make_client(executor_id="executor0", task_type="export")

    await client.update_task(10, "completed", {"some": "json"})


@pytest.mark.parametrize("status", [200, 204])
async def test_request_without_metadata_maked_as_expected(status, rmock, make_client):
    async def handler(req):
        assert str(req.url) == "http://warden.server/tasks/"
        assert req.headers["content-type"] == "application/octet-stream"

        in_data = await req.read()
        expected_in_data = UpdateTaskInput(
            type_name="export", task_id=10, executor_id="executor0", status="completed"
        ).SerializeToString()
        assert in_data == expected_in_data

        return Response(body=UpdateTaskOutput().SerializeToString(), status=status)

    rmock(handler)
    client = make_client(executor_id="executor0", task_type="export")

    await client.update_task(10, "completed")


@pytest.mark.parametrize(
    ["server_response", "expected_response"],
    [
        (UpdateTaskOutput(), {}),
        (
            UpdateTaskOutput(scheduled_time=dt_to_proto(dt("2019-01-01 00:00:00"))),
            {"scheduled_time": datetime(2019, 1, 1, 0, 0, tzinfo=tzutc())},
        ),
    ],
)
async def test_returns_expected_result(
    server_response, expected_response, rmock, make_client
):
    async def handler(_):
        return Response(body=server_response.SerializeToString(), status=200)

    rmock(handler)
    client = make_client(executor_id="executor0", task_type="export")

    got = await client.update_task(10, "completed")

    assert got == expected_response


@pytest.mark.parametrize(
    ["response_status", "proto_error", "expected_exception", "expected_exception_args"],
    [
        (
            404,
            Error(code=Error.ERROR_CODE.UNKNOWN_TASK_OR_TYPE),
            UnknownTaskOrType,
            tuple(),
        ),
        (
            400,
            Error(
                code=Error.ERROR_CODE.VALIDATION_ERROR,
                description="point_type: Invalid value",
            ),
            ValidationError,
            ("point_type: Invalid value",),
        ),
        (
            403,
            Error(code=Error.ERROR_CODE.TASK_IN_PROGRESS_BY_ANOTHER_EXECUTOR),
            TaskInProgressByAnotherExecutor,
            tuple(),
        ),
        (
            400,
            Error(
                code=Error.ERROR_CODE.STATUS_SEQUENCE_VIOLATION,
                description="Update from time_1 to time_2 is not allowed.",
            ),
            StatusSequenceViolation,
            ("Update from time_1 to time_2 is not allowed.",),
        ),
        (
            400,
            Error(code=Error.ERROR_CODE.UPDATE_STATUS_TO_INITIAL),
            UpdateStatusToInitial,
            tuple(),
        ),
        (
            400,
            Error(code=Error.ERROR_CODE.EXECUTOR_ID_ALREADY_USED),
            ExecutorIdAlreadyUsed,
            tuple(),
        ),
    ],
)
async def test_raises_exception_for_protobuf_error(
    response_status,
    proto_error,
    expected_exception,
    expected_exception_args,
    rmock,
    make_client,
):
    async def handler(_):
        return Response(body=proto_error.SerializeToString(), status=response_status)

    rmock(handler)

    client = make_client(executor_id="executor0", task_type="export")

    with pytest.raises(expected_exception) as exc_info:
        await client.update_task(10, "completed")

    assert exc_info.value.args == expected_exception_args


async def test_raises_for_unkwnown_response(rmock, make_client):
    rmock(Response(text="some error", status=555))
    client = make_client(executor_id="executor0", task_type="export")

    with pytest.raises(UnknownResponse) as exc_info:
        await client.update_task(10, "completed")

    assert exc_info.value.args == ("Status=555, payload=b'some error'",)
