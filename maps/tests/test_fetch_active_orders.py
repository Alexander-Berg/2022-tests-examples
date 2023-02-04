import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_pb2 import OrderIds
from maps_adv.billing_proxy.client import UnknownResponse
from maps_adv.billing_proxy.client import Client

pytestmark = [pytest.mark.asyncio]


BILLING_API_URL = "http://billing_proxy.server"


async def test_returns_order_ids(mock_fetch_active_orders_api):
    expected_orders = [5124, 2323]
    message = OrderIds(order_ids=expected_orders).SerializeToString()
    mock_fetch_active_orders_api(Response(body=message, status=200))

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_active_orders(5124, 2323, 9509, 3253)

    assert got == expected_orders


async def test_returns_nothing_if_empty_order_list(mock_fetch_active_orders_api):
    message = OrderIds(order_ids=[]).SerializeToString()
    mock_fetch_active_orders_api(Response(body=message, status=200))

    async with Client(BILLING_API_URL) as client:
        got = await client.fetch_active_orders()

    assert got == []


async def test_raises_for_unknown_error(mock_fetch_active_orders_api):
    mock_fetch_active_orders_api(Response(body=b"", status=405))

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(UnknownResponse):
            await client.fetch_active_orders(5124, 2323, 9509, 3253)
