from datetime import datetime

import pytest

from maps_adv.common.helpers import Any, dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.real_db
async def test_returns_details_for_last_created_task(type_id, factory, dm):
    await factory.create_task("executor0", type_id)
    expected_id = (await factory.create_task("executor1", type_id))["task_id"]

    got = await dm.find_last_task_of_type(type_id)

    assert got == {
        "id": expected_id,
        "created": Any(datetime),
        "scheduled_time": dt("2019-12-01 06:00:00"),
    }


async def test_ignores_other_task_types(type_id, another_type_id, factory, dm):
    await factory.create_task("executor1", another_type_id)
    expected_id = (await factory.create_task("executor0", type_id))["task_id"]

    got = await dm.find_last_task_of_type(type_id)

    assert got["id"] == expected_id


async def test_returns_none_if_no_tasks_of_requested_type(type_id, factory, dm):
    got = await dm.find_last_task_of_type(type_id)

    assert got is None
