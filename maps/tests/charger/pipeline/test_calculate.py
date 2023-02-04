from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]
task_data = {
    "id": 100,
    "timing_from": dt(0),
    "timing_to": dt(300),
    "status": "context_received",
    "execution_state": [
        {
            "order_id": 567382,
            "budget_balance": Decimal("100"),
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    # only 2 events fit in budget limit
                    "cpm": Decimal("1000"),
                    "budget": Decimal("2"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                },
                {
                    "campaign_id": 4356,
                    "tz_name": "UTC",
                    # only 1 event fits in daily budget limit
                    "cpm": Decimal("2000"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("2"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 2,
                },
            ],
        },
        {
            "order_id": 2315,
            "budget_balance": Decimal("2"),
            "campaigns": [
                {
                    "campaign_id": 9876,
                    "tz_name": "UTC",
                    # only 2 events fit in order budget limit
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 3,
                },
                {
                    "campaign_id": 7685,
                    "tz_name": "UTC",
                    # this events will be ignored
                    "cpm": Decimal("2000"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("30"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 2,
                },
            ],
        },
        # campaign without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "campaigns": [
                {
                    "campaign_id": 9999,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20000"),
                    "daily_budget": Decimal("20000"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "events_count": 99999,
                }
            ],
        },
    ],
}
expected_execution_state = [
    {
        "order_id": 567382,
        "budget_balance": Decimal("100"),
        # 2 events per 1 rub + 1 event per 2 rub
        "amount_to_bill": Decimal("4"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("2"),
                "daily_budget": Decimal("20"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 3,
                "events_to_charge": 2,
            },
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                "cpm": Decimal("2000"),
                "budget": Decimal("300"),
                "daily_budget": Decimal("2"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("2"),
                "cost_per_last_event": Decimal("2"),
                "events_count": 2,
                "events_to_charge": 1,
            },
        ],
    },
    {
        "order_id": 2315,
        "budget_balance": Decimal("2"),
        "amount_to_bill": Decimal("2"),
        "campaigns": [
            {
                "campaign_id": 9876,
                "tz_name": "UTC",
                # only 2 events fit in order budget limit
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("20"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 3,
                "events_to_charge": 2,
            },
            {
                "campaign_id": 7685,
                "tz_name": "UTC",
                # this events will be ignored
                "cpm": Decimal("2000"),
                "budget": Decimal("300"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": None,
                "cost_per_last_event": None,
                "events_count": 2,
                "events_to_charge": 0,
            },
        ],
    },
    # campaign without order
    {
        "order_id": None,
        "budget_balance": Decimal("Inf"),
        "amount_to_bill": None,
        "campaigns": [
            {
                "campaign_id": 9999,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20000"),
                "daily_budget": Decimal("20000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 99999,
                "events_to_charge": 20000,
            }
        ],
    },
]


async def test_updates_execution_state_with_calculated_data(
    mock_charger_update_task, charger_pipeline
):
    await charger_pipeline.calculate_charges(task_data)

    assert task_data["execution_state"] == expected_execution_state
