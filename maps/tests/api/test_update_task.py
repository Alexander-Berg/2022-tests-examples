import pytest

from maps_adv.common.helpers import dt
from maps_adv.warden.proto.errors_pb2 import Error
from maps_adv.warden.proto.tasks_pb2 import UpdateTaskInput, UpdateTaskOutput

pytestmark = [pytest.mark.asyncio]

url = "/tasks/"


@pytest.mark.parametrize(
    "task_params",
    [
        {"executor_id": "executor0", "metadata": '{"some": "json"}'},
        {"executor_id": "executor0"},
    ],
)
async def test_returns_nothing_if_updated_not_to_completed(task_params, task_id, api):
    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type", task_id=task_id, status="some_status", **task_params
        ),
        expected_status=204,
    )

    assert got == b""


async def test_returns_next_launch_time_if_updated_to_completed(factory, type_id, api):
    task_id = (await factory.create_task("executor0", type_id))["task_id"]

    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=task_id,
            status="completed",
            executor_id="executor0",
        ),
        decode_as=UpdateTaskOutput,
        expected_status=200,
    )

    assert got == UpdateTaskOutput(
        scheduled_time=dt("2019-12-01 06:05:00", as_proto=True)
    )


async def test_task_updated(type_id, task_id, factory, api):
    await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=task_id,
            status="completed",
            executor_id="executor0",
        ),
        expected_status=200,
    )

    details = await factory.task_details(task_id)
    assert details == dict(
        status="completed",
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )


@pytest.mark.parametrize(
    "payload, expected",
    (
        [
            {"type_name": "task_type", "executor_id": "", "status": "completed"},
            "executor_id: ['Value should not be empty.']",
        ],
        [
            {"type_name": "", "executor_id": "executor0", "status": "completed"},
            "type_name: ['Value should not be empty.']",
        ],
        [
            {"type_name": "task_type", "executor_id": "executor0", "status": ""},
            "status: ['Value should not be empty.']",
        ],
        [
            {
                "executor_id": "executor0",
                "type_name": "task_type",
                "status": "completed",
                "metadata": "some_string",
            },
            "metadata: ['Invalid metadata field']",
        ],
    ),
)
async def test_errored_for_wrong_payload(payload, expected, task_id, api):
    got = await api.put(
        url,
        proto=UpdateTaskInput(task_id=task_id, **payload),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected)


@pytest.mark.parametrize("task_type", ("unknown", "well-known"))
async def test_returns_404_for_unknown_task_type(task_type, api):
    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name=task_type,
            task_id=1,
            executor_id="executor0",
            status="some_status",
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(code=Error.UNKNOWN_TASK_OR_TYPE)


@pytest.mark.usefixtures("type_id")
async def test_returns_404_for_unknown_task_id(api):
    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=100500,
            executor_id="executor0",
            status="some_status",
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(code=Error.UNKNOWN_TASK_OR_TYPE)


async def test_errored_when_trying_to_update_with_another_executor(task_id, api):
    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=task_id,
            executor_id="executor1",
            status="completed",
        ),
        decode_as=Error,
        expected_status=403,
    )

    assert got == Error(code=Error.TASK_IN_PROGRESS_BY_ANOTHER_EXECUTOR)


@pytest.mark.real_db
@pytest.mark.parametrize(
    "current, target",
    (
        ["some_status", "some_status"],
        ["completed", "some_status"],
        ["completed", "failed"],
        ["failed", "some_status"],
        ["failed", "completed"],
    ),
)
async def test_errored_when_trying_to_update_with_unexpected_status(
    current, target, type_id, task_id, factory, api
):
    if current != "accepted":
        await factory.update_task("executor0", task_id, current)

    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=task_id,
            executor_id="executor0",
            status=target,
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.STATUS_SEQUENCE_VIOLATION,
        description=f"Update from {current} to {target} is not allowed.",
    )


@pytest.mark.parametrize(
    "current_status", ("accepted", "completed", "failed", "some_status")
)
async def test_errored_when_trying_to_update_with_initial_status(
    api, factory, task_id, current_status
):
    if current_status != "accepted":
        await factory.update_task("executor0", task_id, current_status)

    got = await api.put(
        url,
        proto=UpdateTaskInput(
            type_name="task_type",
            task_id=task_id,
            executor_id="executor0",
            status="accepted",
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.UPDATE_STATUS_TO_INITIAL)
