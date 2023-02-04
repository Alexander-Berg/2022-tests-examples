from datetime import datetime

import pytest
import pytz
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.market.lib.exceptions import InvalidDatetimeTZ
from maps_adv.geosmb.clients.market.proto.orders_pb2 import (
    ActualOrderItem,
    ActualOrdersInput,
    ActualOrdersOutput,
)

pytestmark = [pytest.mark.asyncio]

response_pb = ActualOrdersOutput(
    orders=[
        ActualOrderItem(
            client_id=11,
            biz_id=111,
            reservation_datetime=dt("2019-12-25 11:00:00", as_proto=True),
        ),
        ActualOrderItem(
            client_id=22,
            biz_id=222,
            reservation_datetime=dt("2020-01-25 11:00:00", as_proto=True),
        ),
    ]
).SerializeToString()


async def test_sends_correct_request(market_client, mock_fetch_actual_bookings):
    request_path = None
    request_body = None
    request_headers = None

    async def _handler(request):
        nonlocal request_path, request_body, request_headers
        request_path = request.path
        request_body = await request.read()
        request_headers = request.headers
        return Response(status=200, body=response_pb)

    mock_fetch_actual_bookings(_handler)

    await market_client.fetch_actual_orders(actual_on=dt("2020-01-25 18:00:00"))

    assert request_path == "/v1/fetch_actual_orders"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = ActualOrdersInput.FromString(request_body)
    assert proto_body == ActualOrdersInput(
        actual_on=dt("2020-01-25 18:00:00", as_proto=True)
    )


async def test_returns_received_data(market_client, mock_fetch_actual_bookings):
    mock_fetch_actual_bookings(Response(status=200, body=response_pb))

    got = await market_client.fetch_actual_orders(actual_on=dt("2020-01-25 18:00:00"))

    assert got == [
        dict(
            client_id=11,
            biz_id=111,
            reservation_datetime=dt("2019-12-25 11:00:00"),
        ),
        dict(
            client_id=22,
            biz_id=222,
            reservation_datetime=dt("2020-01-25 11:00:00"),
        ),
    ]


async def test_raises_if_naive_datetime_passed(market_client):
    with pytest.raises(
        InvalidDatetimeTZ,
        match="active_on param must have UTC tz: 2020-02-02T12:00:00",
    ):
        await market_client.fetch_actual_orders(actual_on=datetime(2020, 2, 2, 12, 00))


async def test_raises_if_not_utc_datetime_passed(market_client):
    with pytest.raises(InvalidDatetimeTZ) as exc:
        await market_client.fetch_actual_orders(
            actual_on=datetime(
                2020, 2, 2, 12, 00, tzinfo=pytz.timezone("Europe/Moscow")
            )
        )

    assert exc.value.args == (
        "active_on param must have UTC tz: 2020-02-02T12:00:00+02:30",
    )
