import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.client import (
    Client,
    OrderDoesNotExist,
    UnknownResponse,
)
from maps_adv.billing_proxy.client.lib.enums import Currency
from maps_adv.billing_proxy.proto.common_pb2 import CurrencyType, Error
from maps_adv.billing_proxy.proto.orders_pb2 import Order
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

example_proto_result = Order(
    id=1,
    title="Test",
    created_at=dt("2020-10-20 00:00:00", as_proto=True),
    client_id=1,
    product_id=1,
    currency=CurrencyType.RUB,
)

example_result = {"id": 1, "currency": Currency.RUB}


async def test_requests_data_correctly(mock_fetch_order_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=200, body=example_proto_result.SerializeToString())

    mock_fetch_order_api(_handler)

    async with Client(BILLING_API_URL) as client:
        await client.fetch_order(1)

    assert req_details["path"] == "/orders/1/"
    proto_body = req_details["body"]
    assert proto_body == b""


async def test_parse_response_data_correctly(mock_fetch_order_api):
    mock_fetch_order_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_order(1)

    assert got == example_result


async def test_raises_if_order_not_found(mock_fetch_order_api):
    mock_fetch_order_api(
        Response(
            status=404,
            body=Error(
                code=Error.ORDER_DOES_NOT_EXIST, description="1"
            ).SerializeToString(),
        )
    )

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(OrderDoesNotExist) as exc_info:
            await client.fetch_order(1)

    assert exc_info.value.args == ("1",)


async def test_raises_for_unexpected_status(mock_fetch_order_api):
    mock_fetch_order_api(Response(status=409))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(UnknownResponse):
            await client.fetch_order(1)


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_fetch_order_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_fetch_order_api(Response(status=status))

    async with Client(BILLING_API_URL) as client:

        with pytest.raises(expected_exc):
            await client.fetch_order(1)


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_fetch_order_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_fetch_order_api(Response(status=status))
    mock_fetch_order_api(
        Response(status=200, body=example_proto_result.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_order(1)

    assert got == example_result
