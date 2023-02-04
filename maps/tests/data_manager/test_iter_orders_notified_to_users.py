import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_order_details(factory, dm):
    order_id = await factory.create_order(
        yang_suite_id="11-11",
        sms_sent_at=dt("2020-01-01 11:11:11"),
        booking_verdict="booked",
        exported_as_notified_at=None,
    )

    orders = []
    async for orders_chunk in dm.iter_orders_notified_to_users(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == [
        {
            "order_id": order_id,
            "yang_suite_id": "11-11",
            "sms_sent_at": 1577877071000000,
            "booking_verdict": "booked",
        }
    ]


async def test_returns_nothing_if_no_orders(dm):
    orders = []
    async for orders_chunk in dm.iter_orders_notified_to_users(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == []


async def test_does_not_return_order_without_yang_suite(factory, dm):
    await factory.create_order(yang_suite_id=None, exported_as_notified_at=None)

    orders = []
    async for orders_chunk in dm.iter_orders_notified_to_users(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == []


async def test_does_not_return_already_exported_order(factory, dm):
    await factory.create_order(
        yang_suite_id="11-11", exported_as_notified_at=dt("2020-02-02 22:22:22")
    )

    orders = []
    async for orders_chunk in dm.iter_orders_notified_to_users(chunk_size=10):
        orders.extend(orders_chunk)

    assert orders == []


async def test_returns_orders_ordered_by_sms_sent(factory, dm):
    order_id_1 = await factory.create_order(
        yang_suite_id="111",
        sms_sent_at=dt("2020-03-03 00:00:00"),
        exported_as_notified_at=None,
    )
    order_id_2 = await factory.create_order(
        yang_suite_id="222",
        created_at=dt("2020-01-01 00:00:00"),
        exported_as_notified_at=None,
    )
    order_id_3 = await factory.create_order(
        yang_suite_id="333",
        sms_sent_at=dt("2020-02-02 00:00:00"),
        exported_as_notified_at=None,
    )

    orders = []
    async for orders_chunk in dm.iter_orders_notified_to_users(chunk_size=10):
        orders.extend(orders_chunk)

    assert [order["order_id"] for order in orders] == [
        order_id_2,
        order_id_3,
        order_id_1,
    ]


async def test_returns_all_records(factory, dm):
    for i in range(5):
        await factory.create_order(yang_suite_id=str(i), exported_as_notified_at=None)

    records = []
    async for chunk in dm.iter_orders_notified_to_users(2):
        records.extend(chunk)

    assert len(records) == 5


@pytest.mark.parametrize("chunk_size", range(1, 2))
async def test_returns_records_grouped_by_chunks(factory, dm, chunk_size):
    for i in range(5):
        await factory.create_order(yang_suite_id=str(i), exported_as_notified_at=None)

    records = []
    async for chunk in dm.iter_orders_notified_to_users(chunk_size):
        records.append(chunk)

    assert len(records[0]) == chunk_size
    assert len(records[1]) == chunk_size
