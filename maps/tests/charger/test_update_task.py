from decimal import Decimal
from enum import Enum

import pytest
from aiohttp.web import json_response
from marshmallow import ValidationError

from maps_adv.stat_controller.client.lib.charger import TaskStatus, UnknownResponse
from maps_adv.stat_controller.client.tests import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def update_rmock(rmock):
    return lambda h: rmock("/tasks/charger/10/", "put", h)


@pytest.fixture
def find_rmock(rmock):
    return lambda h: rmock("/tasks/charger/", "post", h)


class KekEnum(Enum):
    kek = "kek"
    makarek = ""


@pytest.mark.parametrize(
    "status, expected",
    (
        (TaskStatus.context_received, "context_received"),
        (TaskStatus.calculation_completed, "calculation_completed"),
        (TaskStatus.billing_notified, "billing_notified"),
        (TaskStatus.charged_data_sent, "charged_data_sent"),
        (TaskStatus.completed, "completed"),
    ),
)
async def test_request_data_passed_correctly(
    status, expected, charger_client, update_rmock
):
    async def _handler(request):
        assert await request.json() == {
            "executor_id": "executor0",
            "status": expected,
            "execution_state": [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        }
        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00.000000+00:00",
                "timing_to": "2019-05-06T01:05:00.000000+00:00",
            }
        )

    update_rmock(_handler)

    await charger_client.update_task(
        task_id=10,
        executor_id="executor0",
        status=status,
        execution_state=[
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "campaigns": [
                    {
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("3"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("8"),
                        "charged_daily": Decimal("4"),
                        "events_count": 2,
                    }
                ],
            }
        ],
    )


@pytest.mark.parametrize(
    "state, expected",
    [
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal(100),
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
                            "cpm": Decimal("3"),
                            "budget": Decimal("200"),
                            "daily_budget": Decimal("Inf"),
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
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("Inf"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
            [
                {
                    "order_id": 567382,
                    "budget_balance": "Infinity",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        ),
        (
            [
                {
                    "order_id": None,
                    "budget_balance": Decimal("Inf"),
                    "amount_to_bill": Decimal("0"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
            [
                {
                    "order_id": None,
                    "budget_balance": "Infinity",
                    "amount_to_bill": "0",
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        ),
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal("100"),
                    "amount_to_bill": None,
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "amount_to_bill": None,
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
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
        ),
        # `campaigns` field is not parsed/validate => can has any content
        (
            [
                {
                    "order_id": 567382,
                    "budget_balance": Decimal(100),
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
                            "cpm": Decimal(3),
                            "budget": "not_decimal_budget",
                        }
                    ],
                }
            ],
            [
                {
                    "order_id": 567382,
                    "budget_balance": "100",
                    "campaigns": [
                        {
                            "campaign_id": 4242,
                            "tz_name": "UTC",
                            "cpm": "3",
                            "budget": "not_decimal_budget",
                        }
                    ],
                }
            ],
        ),
    ],
)
async def test_updates_passed_payload(state, expected, charger_client, update_rmock):
    async def _handler(request):
        assert await request.json() == {
            "executor_id": "executor0",
            "status": "context_received",
            "execution_state": expected,
        }
        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00.000000+00:00",
                "timing_to": "2019-05-06T01:05:00.000000+00:00",
            }
        )

    update_rmock(_handler)

    await charger_client.update_task(
        task_id=10,
        executor_id="executor0",
        status=TaskStatus.context_received,
        execution_state=state,
    )


@pytest.mark.parametrize(
    "status, response, expected",
    (
        [
            TaskStatus.completed,
            {
                "id": 10,
                "timing_from": "2019-05-06T01:00:00+00:00",
                "timing_to": "2019-05-06T01:05:00+00:00",
            },
            {
                "id": 10,
                "timing_from": dt("2019-05-06 01:00:00"),
                "timing_to": dt("2019-05-06 01:05:00"),
            },
        ],
    ),
)
async def test_returns_updated_task_data(
    status, response, expected, charger_client, update_rmock
):
    update_rmock(json_response(data=response))

    got = await charger_client.update_task(
        task_id=10,
        status=status,
        executor_id="executor0",
        execution_state=[
            {
                "order_id": 567382,
                "budget_balance": Decimal(100),
                "campaigns": [
                    {
                        "campaign_id": 4242,
                        "tz_name": "UTC",
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
    )

    assert got == expected


async def test_raises_for_unknown_response(charger_client, update_rmock):
    update_rmock(json_response(data={"some": ["Any error text."]}, status=555))

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_client.update_task(
            task_id=10,
            status=TaskStatus.completed,
            executor_id="executor0",
            execution_state=["random state"],
        )

    assert (
        'Status=555, payload=b\'{"some": ["Any error text."]}\''
    ) in exc_info.value.args


@pytest.mark.parametrize(
    "update",
    (
        {"executor_id": ""},
        {"executor_id": None},
        {"status": KekEnum.kek},
        {"status": KekEnum.makarek},
        {"status": None},
        {"task_id": None},
        {"task_id": ""},
    ),
)
async def test_raises_for_invalid_data(update, charger_client):
    data = {"task_id": 10, "status": TaskStatus.completed, "executor_id": "executor0"}
    data.update(update)

    with pytest.raises(ValidationError):
        await charger_client.update_task(**data)


async def test_deserialize_execution_state(charger_client, update_rmock, find_rmock):
    execution_state = [
        {
            "order_id": 567382,
            "budget_balance": Decimal("Inf"),
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("3"),
                    "budget": Decimal("200"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("8"),
                    "charged_daily": Decimal("4.34"),
                    "events_count": 2,
                }
            ],
        }
    ]
    serialized_execution_state = None

    # Update task

    async def _update_task_handler(request):
        got = await request.json()

        nonlocal serialized_execution_state
        serialized_execution_state = got["execution_state"]

        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00.000000+00:00",
                "timing_to": "2019-05-06T01:05:00.000000+00:00",
            }
        )

    update_rmock(_update_task_handler)

    await charger_client.update_task(
        task_id=10,
        executor_id="executor0",
        status=TaskStatus.context_received,
        execution_state=execution_state,
    )

    # Find task

    find_rmock(
        json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00.000000+00:00",
                "timing_to": "2019-05-06T01:05:00.000000+00:00",
                "status": "context_received",
                "execution_state": serialized_execution_state,
            },
            status=201,
        )
    )

    got = await charger_client.find_new_task(executor_id="lolkek")

    assert got["execution_state"] == execution_state
