from _decimal import Decimal
from datetime import datetime

import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeOutput,
    OrdersChargeOutput,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import Any, dt

pytestmark = [pytest.mark.asyncio]


def prepare_execution_state(with_billing_success=None):
    _execution_state = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("3"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
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
            "order_id": 222,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("3"),
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("1"),
                    "cost_per_last_event": Decimal("1"),
                    "events_count": 3,
                    "events_to_charge": 3,
                }
            ],
        },
    ]

    if with_billing_success:
        for order, billing_success in zip(_execution_state, with_billing_success):
            order["billing_success"] = billing_success

    return _execution_state


def prepare_task_data(_execution_state):
    return {
        "timing_from": dt(0),
        "timing_to": dt(300),
        "id": 100,
        "status": "calculation_completed",
        "execution_state": _execution_state,
    }


execution_state = prepare_execution_state()
expected_execution_state = prepare_execution_state(with_billing_success=(True, False))
task_data = prepare_task_data(execution_state)
expected_task_data = prepare_task_data(expected_execution_state)


# ==================================================


async def test_updates_task_execution_state(
    billing_submit_charges_rmock, charger_pipeline
):
    message = OrdersChargeOutput(
        charge_result=[
            OrderChargeOutput(order_id=111, success=True),
            OrderChargeOutput(order_id=222, success=False),
        ]
    ).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))

    await charger_pipeline.charge_funds(task_data)

    assert task_data["execution_state"] == expected_execution_state


async def test_submit_charges_to_billing(mock_billing_submit_charges, charger_pipeline):
    mock_billing_submit_charges.side_effect = [{111: True, 222: False}]

    await charger_pipeline.charge_funds(task_data)

    mock_billing_submit_charges.assert_called_once_with(
        [
            {"order_id": 111, "charged_amount": Decimal("3")},
            {"order_id": 222, "charged_amount": Decimal("3")},
        ],
        Any(datetime),
    )


async def test_sets_none_for_zero_charges(
    billing_submit_charges_rmock, charger_pipeline
):
    def _handler(response):
        pytest.fail("Attempt to call Billing Charger")

    billing_submit_charges_rmock(_handler)

    execution_state = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            # zero amount
            "amount_to_bill": Decimal("0"),
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("20"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 3,
                    "events_to_charge": 0,
                }
            ],
        }
    ]
    task_data = prepare_task_data(execution_state)

    await charger_pipeline.charge_funds(task_data)

    expected_execution_state = [
        {
            "order_id": 111,
            "budget_balance": Decimal("10.35"),
            # zero amount
            "amount_to_bill": Decimal("0"),
            "billing_success": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("20"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 3,
                    "events_to_charge": 0,
                }
            ],
        }
    ]
    assert task_data == prepare_task_data(expected_execution_state)


async def test_returns_nothing_if_empty_orders(
    mock_billing_submit_charges, charger_pipeline
):
    task_data = prepare_task_data([])

    await charger_pipeline.charge_funds(task_data)

    mock_billing_submit_charges.assert_not_called()
    assert task_data == prepare_task_data([])


async def test_raises_for_unknown_error(billing_submit_charges_rmock, charger_pipeline):
    billing_submit_charges_rmock(Response(body=b"", status=500))

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_pipeline.charge_funds(task_data)

    assert "Status=500, payload=b''" in exc_info.value.args


async def test_submit_to_billing_only_existed_chargers(
    mock_billing_submit_charges, charger_pipeline
):
    execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        {
            "order_id": 222,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("3"),
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("3"),
                    "cost_per_last_event": Decimal("3"),
                    "events_count": 3,
                    "events_to_charge": 3,
                }
            ],
        },
    ]
    task_data = prepare_task_data(execution_state)

    mock_billing_submit_charges.side_effect = [{222: True}]
    await charger_pipeline.charge_funds(task_data)

    mock_billing_submit_charges.assert_called_once_with(
        [{"order_id": 222, "charged_amount": Decimal("3")}], Any(datetime)
    )


async def test_returns_result_data_for_campaigns_with_optional_order(
    billing_submit_charges_rmock, charger_pipeline
):
    execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        {
            "order_id": 222,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("3"),
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("3"),
                    "cost_per_last_event": Decimal("3"),
                    "events_count": 3,
                    "events_to_charge": 3,
                }
            ],
        },
    ]
    task_data = prepare_task_data(execution_state)

    message = OrdersChargeOutput(
        charge_result=[OrderChargeOutput(order_id=222, success=True)]
    ).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))

    await charger_pipeline.charge_funds(task_data)

    expected_execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        {
            "order_id": 222,
            "budget_balance": Decimal("10.35"),
            "amount_to_bill": Decimal("3"),
            "billing_success": True,
            "campaigns": [
                {
                    "campaign_id": 2229,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("3"),
                    "cost_per_last_event": Decimal("3"),
                    "events_count": 3,
                    "events_to_charge": 3,
                }
            ],
        },
    ]
    assert task_data == prepare_task_data(expected_execution_state)


async def test_not_submit_to_billing_missed_chargers(
    mock_billing_submit_charges, charger_pipeline
):
    execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        }
    ]
    task_data = prepare_task_data(execution_state)

    await charger_pipeline.charge_funds(task_data)

    mock_billing_submit_charges.assert_not_called()


async def test_returns_result_data_for_no_order_campaigns(charger_pipeline):
    execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        }
    ]
    task_data = prepare_task_data(execution_state)

    await charger_pipeline.charge_funds(task_data)

    expected_execution_state = [
        # campaigns without order
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    "campaign_id": 1119,
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
        }
    ]
    assert task_data == prepare_task_data(expected_execution_state)
