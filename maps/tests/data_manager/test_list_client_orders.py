import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.enums import OrderStatus

pytestmark = [pytest.mark.asyncio]


async def test_returns_client_orders_details(dm, factory):
    order_id_1 = await factory.create_order(
        yang_suite_id="111",
        person_count=4,
        reservation_datetime=dt("2020-01-13 18:00:00"),
        created_at=dt("2019-12-12 18:00:00"),
        booking_verdict="booked",
    )
    order_id_2 = await factory.create_order(
        yang_suite_id="222",
        person_count=1,
        reservation_datetime=dt("2020-01-14 18:00:00"),
        created_at=dt("2019-12-13 18:00:00"),
        booking_verdict=None,
    )

    got = await dm.list_client_orders(
        biz_id=888,
        client_id=777,
        datetime_from=dt("2020-01-11 18:00:00"),
        datetime_to=dt("2020-01-14 18:00:00"),
    )

    assert got == dict(
        events_before=0,
        events_after=0,
        orders=[
            dict(
                order_id=order_id_2,
                created_at=dt("2019-12-13 18:00:00"),
                reservation_datetime=dt("2020-01-14 18:00:00"),
                reservation_timezone="Europe/Moscow",
                person_count=1,
                status=OrderStatus.UNPROCESSED,
            ),
            dict(
                order_id=order_id_1,
                created_at=dt("2019-12-12 18:00:00"),
                reservation_datetime=dt("2020-01-13 18:00:00"),
                reservation_timezone="Europe/Moscow",
                person_count=4,
                status=OrderStatus.ACCEPTED,
            ),
        ],
    )


@pytest.mark.parametrize(
    "period_kw, expected_order_ids",
    [
        (dict(), [400, 300, 200, 100]),
        (
            dict(
                datetime_from=dt("2020-01-12 18:00:00"),
                datetime_to=dt("2020-01-13 18:00:00"),
            ),
            [300, 200],
        ),
        (dict(datetime_from=dt("2020-01-13 18:00:00")), [400, 300]),
        (dict(datetime_to=dt("2020-01-13 18:00:00")), [300, 200, 100]),
    ],
)
async def test_returns_result_according_to_requested_period(
    period_kw, expected_order_ids, dm, factory
):
    await factory.create_order(
        yang_suite_id="111",
        order_id=100,
        reservation_datetime=dt("2020-01-11 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="222",
        order_id=200,
        reservation_datetime=dt("2020-01-12 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="333",
        order_id=300,
        reservation_datetime=dt("2020-01-13 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="444",
        order_id=400,
        reservation_datetime=dt("2020-01-14 18:00:00"),
    )

    got = await dm.list_client_orders(biz_id=888, client_id=777, **period_kw)

    assert [order["order_id"] for order in got["orders"]] == expected_order_ids


async def test_sorts_result_by_reservation_datetime(dm, factory):
    order_id_1 = await factory.create_order(
        yang_suite_id="111",
        person_count=4,
        reservation_datetime=dt("2020-01-13 18:00:00"),
        created_at=dt("2019-12-12 18:00:00"),
    )
    order_id_2 = await factory.create_order(
        yang_suite_id="222",
        person_count=1,
        reservation_datetime=dt("2020-01-11 18:00:00"),
        created_at=dt("2019-12-13 18:00:00"),
    )

    got = await dm.list_client_orders(biz_id=888, client_id=777)

    assert [order["order_id"] for order in got["orders"]] == [order_id_1, order_id_2]


@pytest.mark.parametrize(
    "booking_verdict, expected_status",
    [
        (None, OrderStatus.UNPROCESSED),
        ("booked", OrderStatus.ACCEPTED),
        ("any_other_verdict", OrderStatus.REJECTED),
    ],
)
async def test_returns_correct_status_for_order(
    booking_verdict, expected_status, dm, factory
):
    await factory.create_order(booking_verdict=booking_verdict)

    got = await dm.list_client_orders(biz_id=888, client_id=777)

    assert got["orders"][0]["status"] == expected_status


async def test_returns_empty_list_as_orders_if_nothing_found(dm, factory):
    await factory.create_order(
        yang_suite_id="111", reservation_datetime=dt("2019-01-13 18:00:00")
    )
    await factory.create_order(
        yang_suite_id="222", reservation_datetime=dt("2021-01-13 18:00:00")
    )

    got = await dm.list_client_orders(
        biz_id=888,
        client_id=777,
        datetime_from=dt("2020-01-11 18:00:00"),
        datetime_to=dt("2020-01-13 18:00:00"),
    )

    assert got == dict(events_before=1, events_after=1, orders=[])


async def test_returns_nothing_if_client_not_found(dm, factory):
    await factory.create_order(yang_suite_id="111", biz_id=888, client_id=678)
    await factory.create_order(yang_suite_id="222", biz_id=245, client_id=777)

    got = await dm.list_client_orders(biz_id=888, client_id=777)

    assert got == dict(events_before=0, events_after=0, orders=[])


@pytest.mark.parametrize(
    "period_kw, expected_counts",
    [
        (dict(), dict(before=0, after=0)),
        (
            dict(
                datetime_from=dt("2020-01-13 00:00:00"),
                datetime_to=dt("2020-01-13 18:00:00"),
            ),
            dict(before=2, after=1),
        ),
        (
            dict(
                datetime_from=dt("2020-01-12 00:00:00"),
                datetime_to=dt("2020-01-12 18:00:00"),
            ),
            dict(before=1, after=2),
        ),
        (dict(datetime_from=dt("2020-01-13 18:00:00")), dict(before=2, after=0)),
        (dict(datetime_to=dt("2020-01-13 18:00:00")), dict(before=0, after=1)),
    ],
)
async def test_counts_before_and_after_events_correctly(
    period_kw, expected_counts, dm, factory
):
    await factory.create_order(
        yang_suite_id="111",
        order_id=100,
        reservation_datetime=dt("2020-01-11 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="222",
        order_id=200,
        reservation_datetime=dt("2020-01-12 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="333",
        order_id=300,
        reservation_datetime=dt("2020-01-13 18:00:00"),
    )
    await factory.create_order(
        yang_suite_id="444",
        order_id=400,
        reservation_datetime=dt("2020-01-14 18:00:00"),
    )

    got = await dm.list_client_orders(biz_id=888, client_id=777, **period_kw)

    assert got["events_before"] == expected_counts["before"]
    assert got["events_after"] == expected_counts["after"]
