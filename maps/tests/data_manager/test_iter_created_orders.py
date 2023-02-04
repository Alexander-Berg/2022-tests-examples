import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_order_details(factory, dm):
    order_id_1 = await factory.create_order(
        created_at=dt("2020-01-01 11:11:11"),
        yang_suite_id="111",
        exported_as_created_at=None,
    )
    order_id_2 = await factory.create_order(
        created_at=dt("2020-01-01 11:11:11"),
        yang_suite_id="222",
        exported_as_created_at=None,
        time_to_call=None,
    )

    orders = []
    async for orders_chunk in dm.iter_created_orders(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == [
        {
            "order_id": order_id_1,
            "created_at": 1577877071000000,
            "processing_time": 1577894400000000,
        },
        {
            "order_id": order_id_2,
            "created_at": 1577877071000000,
            "processing_time": None,
        },
    ]


async def test_returns_nothing_if_no_orders(dm):
    orders = []
    async for orders_chunk in dm.iter_created_orders(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == []


async def test_does_not_return_already_exported_order(factory, dm):
    await factory.create_order(
        yang_suite_id="11-11", exported_as_created_at=dt("2020-02-02 22:22:22")
    )

    orders = []
    async for orders_chunk in dm.iter_created_orders(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == []


async def test_returns_orders_ordered_by_created_dt(factory, dm):
    order_id_1 = await factory.create_order(
        yang_suite_id="111",
        exported_as_created_at=None,
        created_at=dt("2020-03-03 00:00:00"),
    )
    order_id_2 = await factory.create_order(
        yang_suite_id="222",
        exported_as_created_at=None,
        created_at=dt("2020-01-01 00:00:00"),
    )
    order_id_3 = await factory.create_order(
        yang_suite_id="333",
        exported_as_created_at=None,
        created_at=dt("2020-02-02 00:00:00"),
    )

    orders = []
    async for orders_chunk in dm.iter_created_orders(chunk_size=10):
        orders.extend(orders_chunk)

    assert [order["order_id"] for order in orders] == [
        order_id_2,
        order_id_3,
        order_id_1,
    ]


async def test_returns_all_records(factory, dm):
    for i in range(5):
        await factory.create_order(yang_suite_id=str(i), exported_as_created_at=None)

    records = []
    async for chunk in dm.iter_created_orders(2):
        records.extend(chunk)

    assert len(records) == 5


@pytest.mark.parametrize("chunk_size", range(1, 2))
async def test_returns_records_grouped_by_chunks(factory, dm, chunk_size):
    for i in range(5):
        await factory.create_order(yang_suite_id=str(i), exported_as_created_at=None)

    records = []
    async for chunk in dm.iter_created_orders(chunk_size):
        records.append(chunk)

    assert len(records[0]) == chunk_size
    assert len(records[1]) == chunk_size
