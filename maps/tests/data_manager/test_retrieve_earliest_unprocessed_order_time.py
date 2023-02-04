import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_earliest_unprocessed_time(factory, dm):
    await factory.create_empty_order(
        yang_task_created_at=dt("2020-02-02 00:00:00"), task_result_got_at=None
    )
    await factory.create_empty_order(
        yang_task_created_at=dt("2020-01-01 00:00:00"), task_result_got_at=None
    )
    await factory.create_empty_order(
        yang_task_created_at=dt("2020-03-03 00:00:00"), task_result_got_at=None
    )

    got = await dm.retrieve_earliest_unprocessed_order_time()

    assert got == dt("2020-01-01 00:00:00")


async def test_ignores_processed_order(factory, dm):
    await factory.create_empty_order(
        yang_task_created_at=dt("2020-02-02 00:00:00"), task_result_got_at=None
    )
    await factory.create_empty_order(
        yang_task_created_at=dt("2020-01-01 00:00:00"),
        task_result_got_at=dt("2020-01-01 00:00:00"),
    )

    got = await dm.retrieve_earliest_unprocessed_order_time()

    assert got == dt("2020-02-02 00:00:00")


async def test_returns_nothing_if_no_unprocessed_orders(factory, dm):
    got = await dm.retrieve_earliest_unprocessed_order_time()

    assert got is None
