from _decimal import Decimal

import marshmallow
import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeOutput,
    OrdersChargeOutput,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.billing import (
    BillingClient as Client,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_result_data(billing_submit_charges_rmock):
    message = OrdersChargeOutput(
        charge_result=[
            OrderChargeOutput(order_id=123, success=True),
            OrderChargeOutput(order_id=345, success=False),
        ]
    ).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))

    orders = [
        {"order_id": 111, "charged_amount": Decimal("1234.56")},
        {"order_id": 222, "charged_amount": Decimal("1234")},
    ]

    async with Client("http://somedomain.com") as client:
        got = await client.submit_charges(orders, dt(300))

    assert got == {123: True, 345: False}


async def test_returns_nothing_if_empty_orders(billing_submit_charges_rmock):
    message = OrdersChargeOutput(charge_result=[]).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))

    async with Client("http://somedomain.com") as client:
        got = await client.submit_charges([], dt(300))

    assert got == {}


async def test_raises_for_unknown_error(billing_submit_charges_rmock):
    billing_submit_charges_rmock(Response(body=b"", status=500))

    orders = [{"order_id": 111, "charged_amount": Decimal("1234.56")}]
    async with Client("http://somedomain.com") as client:
        with pytest.raises(UnknownResponse) as exc_info:
            await client.submit_charges(orders, dt(300))

    assert "Status=500, payload=b''" in exc_info.value.args


async def test_raises_for_unsupported_money_precision():
    orders = [{"order_id": 111, "charged_amount": Decimal("1.2345678")}]

    async with Client("http://somedomain.com") as client:
        with pytest.raises(marshmallow.exceptions.ValidationError):
            await client.submit_charges(orders, dt(300))
