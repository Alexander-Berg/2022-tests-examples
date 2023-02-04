import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.proto.orders_pb2 import (
    ActualOrderItem,
    ActualOrdersInput,
    ActualOrdersOutput,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.config(DOORMAN_URL="http://doorman.server"),
]

url = "/v1/fetch_actual_orders/"


async def test_returns_future_successful_orders(api, factory):
    await factory.create_order(
        yang_suite_id="11",
        client_id=111,
        permalink=111111,
        customer_passport_uid=7654332,
        reservation_datetime=dt("2020-01-25 11:00:00"),
        reservation_timezone="Europe/Moscow",
        booking_verdict="booked",
    )
    # missed passport
    await factory.create_order(
        yang_suite_id="22",
        client_id=222,
        permalink=222222,
        customer_passport_uid=None,
        reservation_datetime=dt("2019-12-29 11:00:00"),
        reservation_timezone="Asia/Yekaterinburg",
        booking_verdict="booked",
    )
    # missed client_id
    await factory.create_order(
        yang_suite_id="33",
        client_id=None,
        permalink=333333,
        customer_passport_uid=333000,
        reservation_datetime=dt("2019-12-25 11:00:00"),
        reservation_timezone="Asia/Omsk",
        booking_verdict="booked",
    )

    got = await api.post(
        url,
        proto=ActualOrdersInput(actual_on=dt("2019-12-25 11:00:00", as_proto=True)),
        decode_as=ActualOrdersOutput,
        expected_status=200,
    )

    assert got == ActualOrdersOutput(
        orders=[
            ActualOrderItem(
                permalink=333333,
                passport_uid=333000,
                reservation_datetime=dt("2019-12-25 11:00:00", as_proto=True),
                reservation_timezone="Asia/Omsk",
            ),
            ActualOrderItem(
                client_id=222,
                permalink=222222,
                reservation_datetime=dt("2019-12-29 11:00:00", as_proto=True),
                reservation_timezone="Asia/Yekaterinburg",
            ),
            ActualOrderItem(
                client_id=111,
                permalink=111111,
                passport_uid=7654332,
                reservation_datetime=dt("2020-01-25 11:00:00", as_proto=True),
                reservation_timezone="Europe/Moscow",
            ),
        ]
    )


async def test_does_not_return_orders_with_reservation_time_gt_passed(api, factory):
    await factory.create_order(
        yang_suite_id="11",
        reservation_datetime=dt("2019-01-25 11:00:00"),
        booking_verdict="booked",
    )
    await factory.create_order(
        yang_suite_id="22",
        reservation_datetime=dt("2019-12-25 10:59:59"),
        booking_verdict="booked",
    )

    got = await api.post(
        url,
        proto=ActualOrdersInput(actual_on=dt("2019-12-25 11:00:00", as_proto=True)),
        decode_as=ActualOrdersOutput,
        expected_status=200,
    )

    assert got == ActualOrdersOutput(orders=[])


@pytest.mark.parametrize(
    "booking_verdict",
    [
        None,
        "call_failed",
        "generic_failure",
        "no_place",
        "no_place_contacts_transfered",
        "not_enough_information",
        "not_enough_information_contacts_transfered",
    ],
)
async def test_does_not_return_orders_if_no_success_orders(
    booking_verdict, api, factory
):
    await factory.create_order(
        reservation_datetime=dt("2020-01-25 12:00:00"),
        booking_verdict=booking_verdict,
    )

    got = await api.post(
        url,
        proto=ActualOrdersInput(actual_on=dt("2019-12-25 11:00:00", as_proto=True)),
        decode_as=ActualOrdersOutput,
        expected_status=200,
    )

    assert got == ActualOrdersOutput(orders=[])


async def test_does_not_return_orders_without_passport_and_client_id(api, factory):
    await factory.create_order(
        client_id=None,
        customer_passport_uid=None,
        reservation_datetime=dt("2020-12-25 12:00:00"),
        booking_verdict="booked",
    )

    got = await api.post(
        url,
        proto=ActualOrdersInput(actual_on=dt("2019-12-25 11:00:00", as_proto=True)),
        decode_as=ActualOrdersOutput,
        expected_status=200,
    )

    assert got == ActualOrdersOutput(orders=[])
