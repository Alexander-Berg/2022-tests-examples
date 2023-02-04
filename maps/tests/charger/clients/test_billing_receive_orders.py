from decimal import Decimal

import marshmallow
import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersStatInfo,
    OrderStatInfo,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.billing import (
    BillingClient as Client,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse

pytestmark = [pytest.mark.asyncio]


async def test_returns_balance_data_for_sent_orders(billing_receive_orders_rmock):
    message = OrdersStatInfo(
        orders_info=[
            OrderStatInfo(order_id=234, balance="34.56"),
            OrderStatInfo(order_id=837, balance="214326"),
            OrderStatInfo(order_id=669, balance="1324"),
            OrderStatInfo(order_id=324, balance="2467"),
        ]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        got = await client.receive_orders([234, 837, 669, 324])

    assert got == [
        {"order_id": 234, "balance": Decimal("34.56")},
        {"order_id": 837, "balance": Decimal("214326")},
        {"order_id": 669, "balance": Decimal("1324")},
        {"order_id": 324, "balance": Decimal("2467")},
    ]


async def test_returns_zero_if_zero_budget(billing_receive_orders_rmock):
    message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=234, balance="0")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        got = await client.receive_orders([234])

    assert got == [{"order_id": 234, "balance": Decimal(0)}]


async def test_returns_nothing_if_empty_order_list(billing_receive_orders_rmock):
    message = OrdersStatInfo(orders_info=[]).SerializeToString()
    billing_receive_orders_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        got = await client.receive_orders([])

    assert got == []


async def test_raises_for_unknown_error(billing_receive_orders_rmock):
    billing_receive_orders_rmock(Response(body=b"{}", status=404))

    async with Client("http://somedomain.com") as client:
        with pytest.raises(UnknownResponse) as exc_info:
            await client.receive_orders([234, 837, 669, 324])

    assert "Status=404, payload=b'{}'" in exc_info.value.args


async def test_raises_for_unsupported_precision_in_balance(
    billing_receive_orders_rmock
):
    message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=1, balance="1.2345678")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        with pytest.raises(marshmallow.exceptions.ValidationError):
            await client.receive_orders([1, 2, 3])
