from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.client import (
    Client,
    UnknownResponse,
)
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersStatInfo,
    OrdersStatInfoInput,
    OrderStatInfo,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

example_proto_result = OrdersStatInfo(
    orders_info=[
        OrderStatInfo(order_id=234, balance="34.56"),
        OrderStatInfo(order_id=837, balance="214326"),
        OrderStatInfo(order_id=669, balance="1324"),
        OrderStatInfo(order_id=324, balance="2467"),
    ]
)


example_result = {
    234: Decimal("34.56"),
    837: Decimal("214326"),
    669: Decimal("1324"),
    324: Decimal("2467"),
}


@pytest.mark.parametrize("order_ids", ([1, 2, 3, 4], [1]))
async def test_requests_data_correctly(order_ids, mock_fetch_orders_balance_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=200, body=example_proto_result.SerializeToString())

    mock_fetch_orders_balance_api(_handler)

    async with Client(BILLING_API_URL) as client:
        await client.fetch_orders_balance(*order_ids)

    assert req_details["path"] == "/orders/stats/"
    proto_body = OrdersStatInfoInput.FromString(req_details["body"])
    assert proto_body == OrdersStatInfoInput(order_ids=order_ids)


async def test_returns_empty_dict_if_server_returns_nothing(
    mock_fetch_orders_balance_api,
):
    proto = OrdersStatInfo(orders_info=[])
    mock_fetch_orders_balance_api(Response(status=200, body=proto.SerializeToString()))

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_balance(1, 2, 3)

    assert got == {}


async def test_parse_response_data_correctly(mock_fetch_orders_balance_api):
    mock_fetch_orders_balance_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_balance(234, 837, 669, 324)

    assert got == example_result


async def test_raises_for_unexpected_status(mock_fetch_orders_balance_api):
    mock_fetch_orders_balance_api(Response(status=409))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(UnknownResponse):
            await client.fetch_orders_balance(234, 837, 669, 324)


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_fetch_orders_balance_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_fetch_orders_balance_api(Response(status=status))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(expected_exc):
            await client.fetch_orders_balance(234, 837, 669, 324)


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_fetch_orders_balance_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_fetch_orders_balance_api(Response(status=status))
    mock_fetch_orders_balance_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_orders_balance(234, 837, 669, 324)

    assert got == example_result
