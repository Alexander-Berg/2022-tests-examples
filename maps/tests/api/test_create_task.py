import asyncio
from datetime import datetime
from unittest.mock import patch

import pytest

from maps_adv.common.helpers import Any, dt
from maps_adv.warden.proto.errors_pb2 import Error
from maps_adv.warden.proto.tasks_pb2 import CreateTaskInput, TaskDetails

pytestmark = [pytest.mark.asyncio]

url = "/tasks/"


@pytest.mark.usefixtures("type_id")
async def test_returns_task_details(api):
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor0"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got == TaskDetails(task_id=got.task_id, status="accepted", time_limit=300)


@pytest.mark.usefixtures("type_id", "another_type_id")
async def test_execution_of_another_task_type_does_not_block(api):
    await api.post(
        url,
        proto=CreateTaskInput(type_name="another_task_type", executor_id="executor0"),
        expected_status=201,
    )

    await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        expected_status=201,
    )


@pytest.mark.real_db
async def test_will_return_new_task_if_previous_completed(factory, type_id, api):
    prev_task = await factory.create_task("executor0", type_id, status="completed")

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got.task_id != prev_task["task_id"]


@pytest.mark.real_db
@pytest.mark.parametrize(
    "metadata, expected_metadata",
    [(None, None), ({"some": "json"}, '{"some": "json"}')],
)
async def test_restores_failed_task_if_allowed_by_type(
    metadata, expected_metadata, restorable_type_id, factory, api
):
    task_id = (
        await factory.create_task("executor0", restorable_type_id, metadata=metadata)
    )["task_id"]
    await factory.update_task("executor0", task_id, "failed")

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got == TaskDetails(
        task_id=task_id, status="accepted", metadata=expected_metadata, time_limit=300
    )


@pytest.mark.real_db
@pytest.mark.freeze_time("2019-12-01 06:00:00")
@pytest.mark.parametrize("metadata", [None, {"some": "json"}])
async def test_updates_task_status_on_restore(
    metadata, restorable_type_id, factory, api
):
    task_id = (await factory.create_task("executor0", restorable_type_id))["task_id"]
    await factory.update_task("executor0", task_id, "some_status", metadata=metadata)
    await factory.update_task("executor0", task_id, "failed")

    await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    task_data = await factory.task_details(task_id)
    assert task_data == dict(
        executor_id="executor1",
        status="some_status",
        metadata=metadata,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )
    task_logs = await factory.task_logs(task_id)
    assert task_logs[-1] == {
        "id": Any(int),
        "task_id": task_id,
        "status": "some_status",
        "metadata": metadata,
        "executor_id": "executor1",
        "created": Any(datetime),
    }


@pytest.mark.real_db
async def test_returns_new_task_if_previous_completed_and_schedule_allows(
    type_id, freezer, factory, api
):
    freezer.move_to(dt("2019-12-01 06:00:00"))
    previous = await factory.create_task("executor0", type_id, status="completed")

    freezer.move_to(dt("2019-12-01 06:07:00"))
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got.task_id != previous["task_id"]


@pytest.mark.real_db
async def test_returns_409_if_previous_completed_and_schedule_doesnt_allow(
    factory, type_id, freezer, api
):
    freezer.move_to(dt("2019-12-01 06:00:00"))
    await factory.create_task("executor0", type_id, status="completed")

    freezer.move_to(dt("2019-12-01 06:03:00"))
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        decode_as=Error,
        expected_status=409,
    )

    assert got == Error(
        code=Error.TOO_EARLY_FOR_NEW_TASK_OF_REQUESTED_TYPE,
        scheduled_time=dt("2019-12-01 06:05:00", as_proto=True),
    )


@pytest.mark.real_db
async def test_returns_new_task_if_previous_failed_and_not_restorable(
    type_id, factory, api
):
    failed = await factory.create_task("executor0", type_id, status="failed")

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got.task_id != failed["task_id"]


@pytest.mark.real_db
@pytest.mark.freeze_time("2019-12-01 06:11:00")
async def test_returns_409_if_previous_failed_but_was_restored_not_long_ago(
    restorable_type_id, factory, api
):
    failed = await factory.create_task(
        "executor0", restorable_type_id, created=dt("2019-12-01 06:00:00")
    )
    await factory.update_task("executor0", failed["task_id"], "failed")
    await factory.update_task(
        "executor1",
        failed["task_id"],
        "accepted",
        intake_time=dt("2019-12-01 06:10:00"),
    )

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor2"),
        decode_as=Error,
        expected_status=409,
    )

    assert got == Error(code=Error.TASK_TYPE_ALREADY_ASSIGNED)


@pytest.mark.real_db
async def test_returns_failed_despite_schedule(
    restorable_type_id, factory, freezer, api
):
    freezer.move_to(dt("2019-12-01 06:00:00"))
    failed = await factory.create_task("executor0", restorable_type_id, status="failed")

    freezer.move_to(dt("2019-12-01 06:03:00"))
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got.task_id == failed["task_id"]


@pytest.mark.freeze_time("2019-12-01 06:06:00")
async def test_updates_too_old_task_status_if_allowed_by_type(
    restorable_type_id, factory, api
):
    task_id = (
        await factory.create_task(
            "executor0",
            restorable_type_id,
            status="some_status",
            created=dt("2019-12-01 06:00:00"),
        )
    )["task_id"]

    await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert (await factory.task_details(task_id))["status"] == "some_status"
    task_logs = await factory.task_logs(task_id)
    assert task_logs[-2]["status"] == "failed"
    assert task_logs[-1]["status"] == "some_status"


@pytest.mark.freeze_time("2019-12-01 06:06:00")
@pytest.mark.parametrize(
    "metadata, expected_metadata",
    [(None, None), ({"some": "json"}, '{"some": "json"}')],
)
async def test_returns_too_old_task_data_if_allowed_by_type(
    metadata, expected_metadata, restorable_type_id, factory, api
):
    task_id = (
        await factory.create_task(
            "executor0", restorable_type_id, created=dt("2019-12-01 06:00:00")
        )
    )["task_id"]
    await factory.update_task("executor0", task_id, "some_status", metadata)

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="restorable", executor_id="executor1"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got == TaskDetails(
        task_id=task_id,
        status="some_status",
        metadata=expected_metadata,
        time_limit=300,
    )


@pytest.mark.freeze_time("2019-12-01 06:06:00")
async def test_returns_new_task_if_there_is_too_old_one_and_not_restorable(
    type_id, factory, api
):
    task_id = (
        await factory.create_task(
            "executor0", type_id, created=dt("2019-12-01 06:00:00")
        )
    )["task_id"]
    await factory.update_task("executor0", task_id, "some_status", {"old": "meta"})

    got = await api.post(
        url,
        proto=CreateTaskInput(
            type_name="task_type", executor_id="executor1", metadata='{"new": "meta"}'
        ),
        decode_as=TaskDetails,
        expected_status=201,
    )

    assert got.task_id != task_id


async def test_task_created(type_id, factory, api):
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor0"),
        decode_as=TaskDetails,
        expected_status=201,
    )

    details = await factory.task_details(got.task_id)
    assert details["status"] == "accepted"
    assert details["executor_id"] == "executor0"


@pytest.mark.parametrize(
    "payload, expected",
    (
        [
            {"type_name": "task_type", "executor_id": ""},
            "executor_id: ['Value should not be empty.']",
        ],
        [
            {"type_name": "", "executor_id": "executor0"},
            "type_name: ['Value should not be empty.']",
        ],
        [
            {
                "type_name": "task_type",
                "executor_id": "executor0",
                "metadata": "some_string",
            },
            "metadata: ['Invalid metadata field']",
        ],
    ),
)
async def test_errored_for_wrong_payload(payload, expected, api):
    got = await api.post(
        url, proto=CreateTaskInput(**payload), decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected)


@pytest.mark.parametrize("type_name", ("unknown", "well-known"))
async def test_returns_404_for_unknown_type_name(type_name, api):
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name=type_name, executor_id="executor0"),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(code=Error.UNKNOWN_TASK_OR_TYPE)


async def test_blocks_task_creation_while_previous_works(factory, api):
    await factory.create_task_type(name="task_type", time_limit=300)

    await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor0"),
        expected_status=201,
    )
    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor1"),
        decode_as=Error,
        expected_status=409,
    )

    assert got == Error(code=Error.TASK_TYPE_ALREADY_ASSIGNED)


@pytest.mark.real_db
async def test_cant_request_two_tasks_in_one_moment(loop, type_id, api):
    async def _retrieve_task_type_details(*args, **kwargs):
        await asyncio.sleep(0.1)
        return dict(id=type_id, time_limit=300, schedule="* * * * *", restorable=False)

    async def _request(executor_id, expected_status, sleep):
        await asyncio.sleep(sleep)
        return await api.post(
            url,
            proto=CreateTaskInput(type_name="task_type", executor_id=executor_id),
            expected_status=expected_status,
        )

    with patch(
        "maps_adv.warden.server.lib.data_managers"
        ".tasks.DataManager.retrieve_task_type_details"
    ) as _r:
        _r.side_effect = _retrieve_task_type_details

        responses = await asyncio.gather(
            _request("executor0", 201, 0), _request("executor1", 409, 0.05)
        )

    got_0 = TaskDetails.FromString(responses[0])
    assert got_0 == TaskDetails(
        task_id=got_0.task_id, status="accepted", time_limit=300
    )

    assert Error.FromString(responses[1]) == Error(code=Error.CONFLICT)


async def test_raises_if_executor_id_already_exists(loop, type_id, factory, api):
    await factory.create_task("executor0", type_id)

    got = await api.post(
        url,
        proto=CreateTaskInput(type_name="task_type", executor_id="executor0"),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.EXECUTOR_ID_ALREADY_USED)
