from decimal import Decimal

import pytest
from aiohttp.web import json_response
from marshmallow import ValidationError

from maps_adv.stat_controller.client.lib.charger import (
    NoChargerTaskFound,
    TaskStatus,
    UnknownResponse,
)
from maps_adv.stat_controller.client.tests import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def rmock(rmock):
    return lambda h: rmock("/tasks/charger/", "post", h)


@pytest.mark.parametrize(
    "state, parsed_state",
    [
        (None, None),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "20",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal(100),
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal(3),
                            "budget": Decimal(200),
                            "daily_budget": Decimal(20),
                            "charged": Decimal(8),
                            "charged_daily": Decimal(4),
                            "events_count": 2,
                        }
                    ],
                }
            ],
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "Infinity",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": "3.12",
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("8"),
                            "charged_daily": Decimal("4"),
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": Decimal("3.12"),
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
        ),
        (
            [
                {
                    "order_id": None,
                    "budget_balance": "100",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "Infinity",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": "3.12",
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": None,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("8"),
                            "charged_daily": Decimal("4"),
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": Decimal("3.12"),
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
        ),
        (
            [
                {
                    "order_id": None,
                    "budget_balance": "Infinity",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "Infinity",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": "3.12",
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": None,
                    "budget_balance": Decimal("Inf"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("8"),
                            "charged_daily": Decimal("4"),
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": Decimal("3.12"),
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "amount_to_bill": None,
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "Infinity",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": "3.12",
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": None,
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("8"),
                            "charged_daily": Decimal("4"),
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": Decimal("3.12"),
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "amount_to_bill": "0",
                    "billing_success": None,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": "3",
                            "budget": "200",
                            "daily_budget": "Infinity",
                            "charged": "8",
                            "charged_daily": "4",
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": "3.12",
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": None,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("8"),
                            "charged_daily": Decimal("4"),
                            "events_count": 2,
                            "cost_per_event": None,
                            "cost_per_last_event": Decimal("3.12"),
                            "events_to_charge": 0,
                        }
                    ],
                }
            ],
        ),
    ],
)
async def test_returns_created_task_data(state, parsed_state, charger_client, rmock):
    rmock(
        json_response(
            data={
                "id": 10,
                "timing_from": "2019-01-01T12:00:00+00:00",
                "timing_to": "2019-01-01T12:05:00+00:00",
                "status": "accepted",
                "execution_state": state,
            },
            status=201,
        )
    )

    got = await charger_client.find_new_task(executor_id="executor0")

    assert got == {
        "id": 10,
        "timing_from": dt("2019-01-01 12:00:00"),
        "timing_to": dt("2019-01-01 12:05:00"),
        "status": TaskStatus.accepted,
        "execution_state": parsed_state,
    }


async def test_request_data_passed_correctly(charger_client, rmock):
    async def _handler(request):
        assert await request.json() == {"executor_id": "executor0"}
        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-01-01T12:00:00+00:00",
                "timing_to": "2019-01-01T12:05:00+00:00",
                "status": "completed",
                "execution_state": None,
            },
            status=201,
        )

    rmock(_handler)

    await charger_client.find_new_task(executor_id="executor0")


async def test_raises_if_no_task_found(charger_client, rmock):
    rmock(json_response(data={}, status=200))

    with pytest.raises(NoChargerTaskFound):
        await charger_client.find_new_task(executor_id="executor0")


async def test_raises_for_unknown_response(charger_client, rmock):
    rmock(json_response(data={"error_message": "Something happened."}, status=555))

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_client.find_new_task(executor_id="executor0")

    assert (
        'Status=555, payload=b\'{"error_message": "Something happened."}\''
    ) in exc_info.value.args


@pytest.mark.parametrize("value", ("", None))
async def test_raises_for_invalid_executor_id(value, charger_client):
    with pytest.raises(ValidationError):
        await charger_client.find_new_task(executor_id=value)
