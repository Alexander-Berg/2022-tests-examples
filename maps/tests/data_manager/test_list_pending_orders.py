import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 16:00:00"))]


async def test_returns_order_details(factory, dm):
    order_id = await factory.create_order(yang_suite_id=None)

    got = await dm.list_pending_orders()

    assert got == [
        dict(
            id=order_id,
            permalink=12345,
            reservation_datetime=dt("2020-01-01 18:00:00"),
            reservation_timezone="Europe/Moscow",
            person_count=3,
            customer_name="Иван Петров",
            customer_phone="+7 (000) 000-00-00",
            comment="Столик у окна",
            created_at=dt("2019-01-01 00:00:00"),
        )
    ]


async def test_returns_nothing_if_no_pendings(factory, dm):
    await factory.create_order(yang_suite_id="999")

    got = await dm.list_pending_orders()

    assert got == []


async def test_returns_only_orders_with_time_to_call_lt_now(factory, dm):
    order_id_1 = await factory.create_order(
        yang_suite_id=None,
        time_to_call=dt("2020-01-01 16:00:00"),
        created_at=dt("2020-01-02 00:00:00"),
    )
    order_id_2 = await factory.create_order(
        yang_suite_id=None,
        time_to_call=dt("2020-01-01 15:45:00"),
        created_at=dt("2020-01-02 00:01:00"),
    )
    await factory.create_order(
        yang_suite_id=None,
        time_to_call=dt("2020-01-01 16:02:00"),
        created_at=dt("2020-01-02 00:02:00"),
    )
    # will not return automatically rejected orders
    await factory.create_order(
        yang_suite_id=None, time_to_call=None, created_at=dt("2020-01-02 00:03:00")
    )

    got = await dm.list_pending_orders()

    assert [order["id"] for order in got] == [order_id_1, order_id_2]


async def test_returns_pending_orders_sorted_by_creation_time(factory, dm):
    order_id_1 = await factory.create_order(
        yang_suite_id=None, created_at=dt("2020-01-02 00:00:00")
    )
    await factory.create_order()
    order_id_2 = await factory.create_order(
        yang_suite_id=None, created_at=dt("2020-01-01 00:00:00")
    )

    got = await dm.list_pending_orders()

    assert [order["id"] for order in got] == [order_id_2, order_id_1]
