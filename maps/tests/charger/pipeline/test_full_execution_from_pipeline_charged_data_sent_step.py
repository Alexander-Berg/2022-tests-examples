from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.stat_controller.client.lib.charger import TaskStatus
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def pipeline_from_charged_data_sent(
    mock_charger_find_new_task,
    mock_charger_update_task,
    adv_store_send_campaigns_to_stop_rmock,
    charger_pipeline,
):
    adv_store_send_campaigns_to_stop_rmock(Response(status=200))
    mock_charger_find_new_task.return_value = {
        "timing_from": dt(86500),
        "timing_to": dt(86800),
        "id": 100,
        "status": TaskStatus.charged_data_sent,
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("15"),
                "amount_to_bill": Decimal("15"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("3000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("41"),
                        "charged": Decimal("50"),
                        "charged_daily": Decimal("30"),
                        "cost_per_event": Decimal("3"),
                        "cost_per_last_event": Decimal("2"),
                        "events_count": 4,
                        "events_to_charge": 4,
                    },
                    {
                        "campaign_id": 3456,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("300"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("15"),
                        "charged_daily": Decimal("10"),
                        "cost_per_event": Decimal("4"),
                        "cost_per_last_event": Decimal("4"),
                        "events_count": 2,
                        "events_to_charge": 1,
                    },
                ],
            },
            {
                "order_id": 423773,
                "budget_balance": Decimal("300"),
                "amount_to_bill": Decimal("10"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1242,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("30"),
                        "charged": Decimal("12"),
                        "charged_daily": Decimal("10"),
                        "cost_per_event": Decimal("5"),
                        "cost_per_last_event": Decimal("5"),
                        "events_count": 2,
                        "events_to_charge": 2,
                    },
                    {
                        "campaign_id": 3456,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("30"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": None,
                        "cost_per_last_event": None,
                        "events_count": 0,
                        "events_to_charge": 0,
                    },
                ],
            },
        ],
    }
    mock_charger_update_task.side_effect = [
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "current_log_id": 102,
            "status": TaskStatus.completed,
        }
    ]

    return charger_pipeline


async def test_full_execution_from_charged_data_sent_step(
    pipeline_from_charged_data_sent
):
    await pipeline_from_charged_data_sent()
