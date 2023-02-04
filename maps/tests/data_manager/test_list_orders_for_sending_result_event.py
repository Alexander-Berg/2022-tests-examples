import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_order_details(factory, dm):
    order_id = await factory.create_order(sent_result_event_at=None)

    got = await dm.list_orders_for_sending_result_event()

    assert got == [dict(id=order_id, biz_id=888, client_id=777, verdict="booked")]


@pytest.mark.parametrize(
    "order_params",
    [
        dict(biz_id=None, sent_result_event_at=None),
        dict(client_id=None, sent_result_event_at=None),
        dict(booking_verdict=None, sent_result_event_at=None),
        dict(sent_result_event_at=dt("2020-01-01 00:00:00")),
    ],
)
async def test_returns_nothing_if_no_matched_orders(factory, dm, order_params):
    await factory.create_order(**order_params)

    got = await dm.list_orders_for_sending_result_event()

    assert got == []


async def test_returns_orders_sorted_by_creation_time(factory, dm):
    order_id_1 = await factory.create_order(
        sent_result_event_at=None,
        created_at=dt("2020-01-02 00:00:00"),
        yang_suite_id=None,
    )
    order_id_2 = await factory.create_order(
        sent_result_event_at=None,
        created_at=dt("2020-01-01 00:00:00"),
        yang_suite_id=None,
    )
    order_id_3 = await factory.create_order(
        sent_result_event_at=None,
        created_at=dt("2020-01-03 00:00:00"),
        yang_suite_id=None,
    )

    got = await dm.list_orders_for_sending_result_event()

    assert [order["id"] for order in got] == [order_id_2, order_id_1, order_id_3]
