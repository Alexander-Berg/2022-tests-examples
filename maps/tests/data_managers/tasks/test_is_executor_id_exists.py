import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_if_executor_exists(type_id, factory, dm):
    await factory.create_task("executor0", type_id)

    got = await dm.is_executor_id_exists("executor0")

    assert got


async def test_returns_false_if_executor_does_not_exists(type_id, factory, dm):
    await factory.create_task("executor0", type_id)

    got = await dm.is_executor_id_exists("executor1")

    assert not got
