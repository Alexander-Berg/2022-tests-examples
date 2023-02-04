from datetime import datetime

import pytest

from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("metadata", [[], {}, {"some": "json"}, None])
@pytest.mark.parametrize("status", [None, "some_status"])
@pytest.mark.real_db
async def test_returns_task_details(metadata, status, type_id, factory, dm):
    task = await factory.create_task("executor0", type_id, metadata=metadata)
    if status is not None:
        await factory.update_task(
            "executor0", task["task_id"], status=status, metadata=metadata
        )

    got = await dm.retrieve_active_task_details(type_id)

    expected_status = status if status is not None else "accepted"
    assert got == dict(
        id=task["task_id"],
        status=expected_status,
        metadata=metadata,
        intake_time=Any(datetime),
    )


async def test_returns_none_if_no_active_task_found(type_id, factory, dm):
    await factory.create_task("executor0", type_id, status="completed")
    await factory.create_task("executor1", type_id, status="failed")
    await factory.create_task("executor2", type_id, status="completed")

    assert await dm.retrieve_active_task_details(type_id) is None


async def test_returns_none_if_no_tasks_found(type_id, dm):
    assert await dm.retrieve_active_task_details(type_id) is None


@pytest.mark.parametrize(
    "status", ["accepted", "some_status", "failed", "completed", "some_status"]
)
async def test_not_returns_active_tasks_for_other_task_types(
    status, type_id, another_type_id, factory, dm
):
    await factory.create_task("executor0", another_type_id, status=status)

    assert await dm.retrieve_active_task_details(type_id) is None
