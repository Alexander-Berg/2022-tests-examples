import pytest
from aiohttp.web import Response

from maps_adv.warden.client.lib import Conflict, UnknownResponse
from maps_adv.warden.client.lib.exceptions import (
    TaskTypeAlreadyAssigned,
    TooEarlyForNewTask,
    UnknownTaskOrType,
    ValidationError,
)
from maps_adv.warden.client.tests import dt, dt_to_proto
from maps_adv.warden.proto.errors_pb2 import Error
from maps_adv.warden.proto.tasks_pb2 import CreateTaskInput, TaskDetails

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def rmock(rmock):
    return lambda h: rmock("/tasks/", "POST", h)


async def test_request_without_metadata_maked_as_expected(rmock, make_client):
    async def handler(req):
        assert str(req.url) == "http://warden.server/tasks/"
        assert req.headers["content-type"] == "application/octet-stream"

        in_data = await req.read()
        expected_in_data = CreateTaskInput(
            type_name="export", executor_id="executor0", metadata=None
        ).SerializeToString()
        assert in_data == expected_in_data

        out_data = TaskDetails(task_id=1, status="accepted", time_limit=300)
        return Response(body=out_data.SerializeToString(), status=201)

    rmock(handler)
    client = make_client(executor_id="executor0", task_type="export")

    await client.create_task()


async def test_request_with_metadata_maked_as_expected(rmock, make_client):
    async def handler(req):
        assert str(req.url) == "http://warden.server/tasks/"
        assert req.headers["content-type"] == "application/octet-stream"

        in_data = await req.read()
        expected_in_data = CreateTaskInput(
            type_name="export", executor_id="executor0", metadata='{"some": "json"}'
        ).SerializeToString()
        assert in_data == expected_in_data

        out_data = TaskDetails(task_id=1, status="accepted", time_limit=300)
        return Response(body=out_data.SerializeToString(), status=201)

    rmock(handler)
    client = make_client(executor_id="executor0", task_type="export")

    await client.create_task(metadata={"some": "json"})


async def test_returns_task_details_for_tasks_without_metadata(rmock, make_client):
    async def handler(_):
        out_data = TaskDetails(task_id=1, status="accepted", time_limit=300)
        return Response(body=out_data.SerializeToString(), status=201)

    rmock(handler)

    client = make_client(executor_id="executor0", task_type="export")

    got = await client.create_task()

    assert got == dict(task_id=1, status="accepted", time_limit=300)


async def test_returns_task_details_for_tasks_with_metadata(rmock, make_client):
    async def handler(_):
        out_data = TaskDetails(
            task_id=1, status="accepted", time_limit=300, metadata='{"some": "json"}'
        )
        return Response(body=out_data.SerializeToString(), status=201)

    rmock(handler)

    client = make_client(executor_id="executor0", task_type="export")

    got = await client.create_task(metadata={"some": "json"})

    assert got == dict(
        task_id=1, status="accepted", time_limit=300, metadata={"some": "json"}
    )


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
            409,
            Error(code=Error.ERROR_CODE.TASK_TYPE_ALREADY_ASSIGNED),
            TaskTypeAlreadyAssigned,
            tuple(),
        ),
        (
            409,
            Error(
                code=Error.ERROR_CODE.TOO_EARLY_FOR_NEW_TASK_OF_REQUESTED_TYPE,
                scheduled_time=dt_to_proto(dt("2019-12-01 06:07:00")),
            ),
            TooEarlyForNewTask,
            ("Try to start new task after 2019-12-01 06:07:00",),
        ),
        (409, Error(code=Error.ERROR_CODE.CONFLICT), Conflict, tuple()),
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
        await client.create_task()

    assert exc_info.value.args == expected_exception_args


async def test_raises_for_unkwnown_response(rmock, make_client):
    rmock(Response(text="some error", status=555))
    client = make_client(executor_id="executor0", task_type="export")

    with pytest.raises(UnknownResponse) as exc_info:
        await client.create_task()

    assert exc_info.value.args == ("Status=555, payload=b'some error'",)
