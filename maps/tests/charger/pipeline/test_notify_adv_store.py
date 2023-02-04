from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_200(adv_store_send_campaigns_to_stop_rmock, charger_pipeline):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("200"),
                "amount_to_bill": Decimal("18"),
                "billing_success": True,
                "campaigns": [
                    {
                        # all limits ok, will be ignored
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("20"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("1"),
                        "cost_per_last_event": Decimal("1"),
                        "events_count": 3,
                        "events_to_charge": 3,
                    }
                ],
            },
            {
                "order_id": 232365,
                "budget_balance": Decimal("30"),
                "amount_to_bill": Decimal("20"),
                "billing_success": True,
                "campaigns": [
                    {
                        # daily limit reached
                        "campaign_id": 9786,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("Infinity"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    },
                    {
                        # budget limit reached
                        "campaign_id": 8764,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("20"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    },
                ],
            },
            {
                "order_id": 232365,
                "budget_balance": Decimal("20"),
                # order limit reached
                "amount_to_bill": Decimal("20"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1234,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("Infinity"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    }
                ],
            },
            # campaign without order
            {
                "order_id": None,
                "budget_balance": Decimal("Inf"),
                "amount_to_bill": None,
                "billing_success": None,
                "campaigns": [
                    {
                        # daily limit reached
                        "campaign_id": 9999,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("Infinity"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    }
                ],
            },
        ],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    await charger_pipeline.notify_adv_store(task_data)


async def test_returns_200_if_no_campaigns_to_stop(
    adv_store_send_campaigns_to_stop_rmock, charger_pipeline
):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("200"),
                "amount_to_bill": Decimal("18"),
                "billing_success": True,
                "campaigns": [
                    {
                        # all limits ok, will be ignored
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("20"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("1"),
                        "cost_per_last_event": Decimal("1"),
                        "events_count": 3,
                        "events_to_charge": 3,
                    }
                ],
            }
        ],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    await charger_pipeline.notify_adv_store(task_data)


async def test_returns_200_if_no_billed_orders(
    adv_store_send_campaigns_to_stop_rmock, charger_pipeline
):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 232365,
                "budget_balance": Decimal("20"),
                # order limit reached
                "amount_to_bill": Decimal("20"),
                "billing_success": False,
                "campaigns": [
                    {
                        "campaign_id": 1234,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("Infinity"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    },
                    {
                        "campaign_id": 3523,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("Infinity"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 10,
                        "events_to_charge": 4,
                    },
                ],
            }
        ],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    await charger_pipeline.notify_adv_store(task_data)


async def test_returns_200_if_empty_orders(
    adv_store_send_campaigns_to_stop_rmock, charger_pipeline
):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    await charger_pipeline.notify_adv_store(task_data)


async def test_raises_for_unknown_error(
    adv_store_send_campaigns_to_stop_rmock, charger_pipeline
):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("200"),
                "amount_to_bill": Decimal("18"),
                "billing_success": True,
                "campaigns": [
                    {
                        # all limits ok, will be ignored
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("20"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("1"),
                        "cost_per_last_event": Decimal("1"),
                        "events_count": 3,
                        "events_to_charge": 3,
                    }
                ],
            }
        ],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(body=b"", status=500))

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_pipeline.notify_adv_store(task_data)

    assert "Status=500, payload=b''" in exc_info.value.args
