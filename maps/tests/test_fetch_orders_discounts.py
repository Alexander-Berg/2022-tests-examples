from decimal import Decimal
from datetime import datetime, timezone

import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.client import (
    Client,
    UnknownResponse,
)
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersDiscountInfo,
    OrdersDiscountInfoInput,
    OrderDiscountInfo,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

example_proto_result = OrdersDiscountInfo(
    discount_info=[
        OrderDiscountInfo(order_id=234, discount="1.0"),
        OrderDiscountInfo(order_id=837, discount="0.7"),
        OrderDiscountInfo(order_id=669, discount="0.7"),
        OrderDiscountInfo(order_id=324, discount="1.0"),
    ]
)

example_result = {
    234: Decimal("1.0"),
    837: Decimal("0.7"),
    669: Decimal("0.7"),
    324: Decimal("1.0"),
}


@pytest.mark.parametrize("order_ids", ([1, 2, 3, 4], [1]))
async def test_requests_data_correctly(order_ids, mock_fetch_orders_discounts_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=200, body=example_proto_result.SerializeToString())

    mock_fetch_orders_discounts_api(_handler)

    async with Client(BILLING_API_URL) as client:
        await client.fetch_orders_discounts(
            datetime(2000, 1, 1, tzinfo=timezone.utc), *order_ids
        )

    assert req_details["path"] == "/orders/discounts/"
    proto_body = OrdersDiscountInfoInput.FromString(req_details["body"])
    assert proto_body == OrdersDiscountInfoInput(
        order_ids=order_ids, billed_at=dt("2000-01-01 00:00:00", as_proto=True)
    )


async def test_returns_empty_dict_if_server_returns_nothing(
    mock_fetch_orders_discounts_api,
):
    proto = OrdersDiscountInfo(discount_info=[])
    mock_fetch_orders_discounts_api(
        Response(status=200, body=proto.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_discounts(
            datetime(2000, 1, 1, tzinfo=timezone.utc), 1, 2, 3
        )

    assert got == {}


async def test_parse_response_data_correctly(mock_fetch_orders_discounts_api):
    mock_fetch_orders_discounts_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_discounts(
            datetime(2000, 1, 1, tzinfo=timezone.utc), 234, 837, 669, 324
        )

    assert got == example_result


async def test_raises_for_unexpected_status(mock_fetch_orders_discounts_api):
    mock_fetch_orders_discounts_api(Response(status=409))

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(UnknownResponse):
            await client.fetch_orders_discounts(
                datetime(2000, 1, 1, tzinfo=timezone.utc), 234, 837, 669, 324
            )


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_fetch_orders_discounts_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_fetch_orders_discounts_api(Response(status=status))

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(expected_exc):
            await client.fetch_orders_discounts(
                datetime(2000, 1, 1, tzinfo=timezone.utc), 234, 837, 669, 324
            )


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_fetch_orders_discounts_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_fetch_orders_discounts_api(Response(status=status))
    mock_fetch_orders_discounts_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_discounts(
            datetime(2000, 1, 1, tzinfo=timezone.utc), 234, 837, 669, 324
        )

    assert got == example_result
