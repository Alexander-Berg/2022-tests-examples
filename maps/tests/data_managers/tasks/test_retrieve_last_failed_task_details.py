import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", ["accepted", "some_status", "completed"])
async def test_ignores_not_failed(status, factory, type_id, dm):
    await factory.create_task("executor0", type_id, status)

    got = await dm.retrieve_last_failed_task_details(type_id)

    assert got is None


async def test_ignores_failed_of_another_type(factory, type_id, another_type_id, dm):
    await factory.create_task("executor0", type_id, "failed")

    got = await dm.retrieve_last_failed_task_details(another_type_id)

    assert got is None


@pytest.mark.real_db
async def test_returns_latest_data_for_failed_task(factory, task_id, type_id, dm):
    await factory.update_task(
        "executor0", task_id, "not_failed_status", metadata={"some": "json"}
    )
    await factory.update_task(
        "executor0", task_id, "failed", metadata={"other": "json"}
    )

    got = await dm.retrieve_last_failed_task_details(type_id)

    assert got == dict(
        id=task_id, status="not_failed_status", metadata={"some": "json"}
    )


@pytest.mark.real_db
async def test_returns_latest_failed_task(factory, type_id, dm):
    await factory.create_task("executor0", type_id, "failed")
    expected_id = (await factory.create_task("executor1", type_id, "failed"))["task_id"]

    got = await dm.retrieve_last_failed_task_details(type_id)

    assert got == dict(id=expected_id, status="accepted", metadata=None)
