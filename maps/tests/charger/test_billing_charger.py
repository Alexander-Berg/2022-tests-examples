from _decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeOutput,
    OrdersChargeOutput,
)
from maps_adv.stat_tasks_starter.lib.charger.billing_charger import BillingCharger
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_result_data(billing_submit_charges_rmock):
    message = OrdersChargeOutput(
        charge_result=[
            OrderChargeOutput(order_id=111, success=True),
            OrderChargeOutput(order_id=222, success=False),
        ]
    ).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))

    billing_charger = BillingCharger(billing_url="http://somedomain.com")

    orders = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("10.35"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        },
        {
            "order_id": 222,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("10.35"),
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        },
    ]

    await billing_charger(orders, dt(300))

    assert orders == [
        {
            "order_id": 111,
            "billing_success": True,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("10.35"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        },
        {
            "order_id": 222,
            "billing_success": False,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("10.35"),
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        },
    ]


async def test_sets_none_for_zero_charges(billing_submit_charges_rmock):
    def _handler(response):
        pytest.fail("Attempt to call Billing Charger")

    billing_submit_charges_rmock(_handler)

    billing_charger = BillingCharger(billing_url="http://somedomain.com")

    orders = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("0"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        }
    ]

    await billing_charger(orders, dt(300))

    assert orders == [
        {
            "order_id": 111,
            "billing_success": None,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("0"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        }
    ]


async def test_returns_nothing_if_empty_orders(mocker):
    billing_charger = BillingCharger(billing_url="http://somedomain.com")

    orders = []
    await billing_charger(orders, dt(300))

    assert orders == []


async def test_raises_for_unknown_error(billing_submit_charges_rmock):
    billing_submit_charges_rmock(Response(body=b"", status=500))

    billing_charger = BillingCharger(billing_url="http://somedomain.com")

    orders = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("30"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                }
            ],
        }
    ]

    with pytest.raises(UnknownResponse) as exc_info:
        await billing_charger(orders, dt(300))

    assert "Status=500, payload=b''" in exc_info.value.args
