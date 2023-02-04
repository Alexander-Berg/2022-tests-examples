from datetime import datetime

import pytest
import pytz
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.client.lib.exceptions import InvalidDatetimeTZ
from maps_adv.geosmb.booking_yang.proto.orders_pb2 import (
    ActualOrderItem,
    ActualOrdersInput,
    ActualOrdersOutput,
)

pytestmark = [pytest.mark.asyncio]

response_pb = ActualOrdersOutput(
    orders=[
        ActualOrderItem(
            passport_uid=6543,
            client_id=111,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00", as_proto=True),
            reservation_timezone="Europe/Moscow",
        ),
        ActualOrderItem(
            passport_uid=4567,
            client_id=222,
            permalink=222222,
            reservation_datetime=dt("2020-01-25 11:00:00", as_proto=True),
            reservation_timezone="Asia/Yekaterinburg",
        ),
    ]
).SerializeToString()


async def test_sends_correct_request_for_order_event(client, mock_fetch_actual_orders):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=response_pb)

    mock_fetch_actual_orders(_handler)

    await client.fetch_actual_orders(actual_on=dt("2020-01-25 18:00:00"))

    assert request_path == "/v1/fetch_actual_orders/"
    proto_body = ActualOrdersInput.FromString(request_body)
    assert proto_body == ActualOrdersInput(
        actual_on=dt("2020-01-25 18:00:00", as_proto=True)
    )


async def test_returns_received_data(client, mock_fetch_actual_orders):
    mock_fetch_actual_orders(Response(status=200, body=response_pb))

    got = await client.fetch_actual_orders(actual_on=dt("2020-01-25 18:00:00"))

    assert got == [
        dict(
            passport_uid=6543,
            client_id=111,
            permalink=111111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
            reservation_timezone="Europe/Moscow",
        ),
        dict(
            passport_uid=4567,
            client_id=222,
            permalink=222222,
            reservation_datetime=dt("2020-01-25 11:00:00"),
            reservation_timezone="Asia/Yekaterinburg",
        ),
    ]


async def test_raises_if_naive_datetime_passed(client):
    with pytest.raises(
        InvalidDatetimeTZ,
        match="Passed active_on date should be in UTC, active_on=2020-02-02T12:00:00",
    ):
        await client.fetch_actual_orders(actual_on=datetime(2020, 2, 2, 12, 00))


async def test_raises_if_not_utc_datetime_passed(client):
    with pytest.raises(InvalidDatetimeTZ) as exc:
        await client.fetch_actual_orders(
            actual_on=datetime(
                2020, 2, 2, 12, 00, tzinfo=pytz.timezone("Europe/Moscow")
            )
        )

    assert exc.value.args == (
        "Passed active_on date should be in UTC, active_on=2020-02-02T12:00:00+02:30",
    )
