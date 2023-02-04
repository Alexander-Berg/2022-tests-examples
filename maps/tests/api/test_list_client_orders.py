import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.proto.errors_pb2 import Error
from maps_adv.geosmb.booking_yang.proto.orders_pb2 import (
    ClientOrder,
    ClientOrders,
    ListClientOrdersInput,
)

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_client_orders/"


async def test_returns_client_orders_details(api, factory):
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

    got = await api.post(
        url,
        proto=ListClientOrdersInput(
            biz_id=888,
            client_id=777,
            datetime_from=dt("2020-01-11 18:00:00", as_proto=True),
            datetime_to=dt("2020-01-14 18:00:00", as_proto=True),
        ),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert got == ClientOrders(
        events_before=0,
        events_after=0,
        orders=[
            ClientOrder(
                order_id=order_id_2,
                created_at=dt("2019-12-13 18:00:00", as_proto=True),
                reservation_datetime=dt("2020-01-14 18:00:00", as_proto=True),
                reservation_timezone="Europe/Moscow",
                person_count=1,
                status=ClientOrder.UNPROCESSED,
            ),
            ClientOrder(
                order_id=order_id_1,
                created_at=dt("2019-12-12 18:00:00", as_proto=True),
                reservation_datetime=dt("2020-01-13 18:00:00", as_proto=True),
                reservation_timezone="Europe/Moscow",
                person_count=4,
                status=ClientOrder.ACCEPTED,
            ),
        ],
    )


@pytest.mark.parametrize(
    "period_kw, expected_order_ids",
    [
        (dict(), [400, 300, 200, 100]),
        (
            dict(
                datetime_from=dt("2020-01-12 18:00:00", as_proto=True),
                datetime_to=dt("2020-01-13 18:00:00", as_proto=True),
            ),
            [300, 200],
        ),
        (dict(datetime_from=dt("2020-01-13 18:00:00", as_proto=True)), [400, 300]),
        (dict(datetime_to=dt("2020-01-13 18:00:00", as_proto=True)), [300, 200, 100]),
    ],
)
async def test_returns_result_according_to_requested_period(
    period_kw, expected_order_ids, api, factory
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

    got = await api.post(
        url,
        proto=ListClientOrdersInput(biz_id=888, client_id=777, **period_kw),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert [order.order_id for order in got.orders] == expected_order_ids


async def test_sorts_result_by_reservation_datetime(api, factory):
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

    got = await api.post(
        url,
        proto=ListClientOrdersInput(biz_id=888, client_id=777),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert [order.order_id for order in got.orders] == [order_id_1, order_id_2]


@pytest.mark.parametrize(
    "booking_verdict, expected_status",
    [
        (None, ClientOrder.UNPROCESSED),
        ("booked", ClientOrder.ACCEPTED),
        ("any_other_verdict", ClientOrder.REJECTED),
    ],
)
async def test_returns_correct_status_for_order(
    booking_verdict, expected_status, api, factory
):
    await factory.create_order(booking_verdict=booking_verdict)

    got = await api.post(
        url,
        proto=ListClientOrdersInput(biz_id=888, client_id=777),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert got.orders[0].status == expected_status


async def test_returns_empty_list_as_orders_if_nothing_found(api, factory):
    await factory.create_order(
        yang_suite_id="111", reservation_datetime=dt("2019-01-13 18:00:00")
    )
    await factory.create_order(
        yang_suite_id="222", reservation_datetime=dt("2021-01-13 18:00:00")
    )

    got = await api.post(
        url,
        proto=ListClientOrdersInput(
            biz_id=888,
            client_id=777,
            datetime_from=dt("2020-01-11 18:00:00", as_proto=True),
            datetime_to=dt("2020-01-13 18:00:00", as_proto=True),
        ),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert got == ClientOrders(events_before=1, events_after=1, orders=[])


async def test_returns_nothing_if_client_not_found(api, factory):
    await factory.create_order(yang_suite_id="111", biz_id=888, client_id=678)
    await factory.create_order(yang_suite_id="222", biz_id=245, client_id=777)

    got = await api.post(
        url,
        proto=ListClientOrdersInput(biz_id=888, client_id=777),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert got == ClientOrders(events_before=0, events_after=0, orders=[])


@pytest.mark.parametrize(
    "period_kw, expected_counts",
    [
        (dict(), dict(before=0, after=0)),
        (
            dict(
                datetime_from=dt("2020-01-13 00:00:00", as_proto=True),
                datetime_to=dt("2020-01-13 18:00:00", as_proto=True),
            ),
            dict(before=2, after=1),
        ),
        (
            dict(
                datetime_from=dt("2020-01-12 00:00:00", as_proto=True),
                datetime_to=dt("2020-01-12 18:00:00", as_proto=True),
            ),
            dict(before=1, after=2),
        ),
        (
            dict(datetime_from=dt("2020-01-13 18:00:00", as_proto=True)),
            dict(before=2, after=0),
        ),
        (
            dict(datetime_to=dt("2020-01-13 18:00:00", as_proto=True)),
            dict(before=0, after=1),
        ),
    ],
)
async def test_counts_before_and_after_events_correctly(
    period_kw, expected_counts, api, factory
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

    got = await api.post(
        url,
        proto=ListClientOrdersInput(biz_id=888, client_id=777, **period_kw),
        decode_as=ClientOrders,
        expected_status=200,
    )

    assert got.events_before == expected_counts["before"]
    assert got.events_after == expected_counts["after"]


async def test_errored_if_datetime_from_gt_datetime_to(api):
    got = await api.post(
        url,
        proto=ListClientOrdersInput(
            biz_id=888,
            client_id=678,
            datetime_from=dt("2020-02-01 18:00:00", as_proto=True),
            datetime_to=dt("2020-01-01 18:00:00", as_proto=True),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.DATA_VALIDATION_ERROR,
        description='{"_schema": ["datetime_to should be grater than datetime_from."]}',
    )


@pytest.mark.parametrize(
    "kw, msg",
    (
        [dict(biz_id=0, client_id=1), '{"biz_id": ["Must be at least 1."]}'],
        [dict(biz_id=1, client_id=0), '{"client_id": ["Must be at least 1."]}'],
    ),
)
async def test_errored_for_incorrect_input(kw, msg, api):
    got = await api.post(
        url, proto=ListClientOrdersInput(**kw), decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.DATA_VALIDATION_ERROR, description=msg)
