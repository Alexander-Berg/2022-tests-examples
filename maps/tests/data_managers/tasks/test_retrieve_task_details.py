import pytest

from maps_adv.common.helpers import dt
from maps_adv.warden.server.lib.data_managers.tasks import TaskNotFound

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("metadata", [[], {}, {"some": "json"}, None])
async def test_returns_task_details(metadata, type_id, factory, dm):
    task = await factory.create_task("executor0", type_id, metadata=metadata)

    got = await dm.retrieve_task_details(task["task_id"])

    assert got == dict(
        status="accepted",
        executor_id="executor0",
        scheduled_time=dt("2019-12-01 06:00:00"),
        metadata=metadata,
    )


@pytest.mark.real_db
async def test_returns_current_task_status(task_id, factory, dm):
    for status in "some_status", "another_status":
        await factory.update_task("executor0", task_id, status)

    got = await dm.retrieve_task_details(task_id)

    assert got["status"] == "another_status"


async def test_raises_if_task_id_not_found(dm):
    with pytest.raises(TaskNotFound):
        await dm.retrieve_task_details(100500)
