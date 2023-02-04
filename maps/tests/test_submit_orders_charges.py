from datetime import datetime
from decimal import Decimal

import pytest
from aiohttp.web import Response
from google.protobuf.timestamp_pb2 import Timestamp

from maps_adv.billing_proxy.client import (
    Client,
    UnexpectedNaiveDatetime,
    UnknownResponse,
)
from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeInput,
    OrderChargeOutput,
    OrdersChargeInput,
    OrdersChargeOutput,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

example_proto_result = OrdersChargeOutput(
    charge_result=[
        OrderChargeOutput(order_id=123, success=True),
        OrderChargeOutput(order_id=345, success=False),
    ],
    applied=True,
)

example_result = True, {123: True, 345: False}


async def test_requests_data_correctly(mock_submit_orders_charges_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=201, body=example_proto_result.SerializeToString())

    mock_submit_orders_charges_api(_handler)

    async with Client(BILLING_API_URL) as client:
        await client.submit_orders_charges(
            charges={123: Decimal("1234.56"), 345: Decimal("1234")},
            bill_due_to=dt("2020-01-01 00:00:00"),
        )

    assert req_details["path"] == "/orders/charge/"
    proto_body = OrdersChargeInput.FromString(req_details["body"])
    assert proto_body == OrdersChargeInput(
        orders_charge=[
            OrderChargeInput(order_id=123, charged_amount="1234.56"),
            OrderChargeInput(order_id=345, charged_amount="1234"),
        ],
        bill_for_timestamp=Timestamp(seconds=1577836800),
    )


async def test_parse_response_data_correctly(mock_submit_orders_charges_api):
    mock_submit_orders_charges_api(
        Response(status=201, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.submit_orders_charges(
            charges={123: Decimal("1234.56"), 345: Decimal("1234")},
            bill_due_to=dt("2020-01-01 00:00:00"),
        )

    assert got == example_result


async def test_raises_for_naive_bill_due_to():
    async with Client(BILLING_API_URL) as client:

        with pytest.raises(UnexpectedNaiveDatetime) as exc_info:
            await client.submit_orders_charges(
                charges={123: Decimal("1234.56")}, bill_due_to=datetime(2020, 1, 1)
            )

    assert datetime(2020, 1, 1) in exc_info.value.args


async def test_raises_for_unexpected_status(mock_submit_orders_charges_api):
    mock_submit_orders_charges_api(Response(status=409))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(UnknownResponse):
            await client.submit_orders_charges(
                charges={123: Decimal("1234.56")}, bill_due_to=dt("2020-01-01 00:00:00")
            )


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_submit_orders_charges_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_submit_orders_charges_api(Response(status=status))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(expected_exc):
            await client.submit_orders_charges(
                charges={123: Decimal("1234.56")}, bill_due_to=dt("2020-01-01 00:00:00")
            )


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_submit_orders_charges_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_submit_orders_charges_api(Response(status=status))
    mock_submit_orders_charges_api(
        Response(status=201, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.submit_orders_charges(
            charges={123: Decimal("1234.56"), 345: Decimal("1234")},
            bill_due_to=dt("2020-01-01 00:00:00"),
        )

    assert got == example_result
