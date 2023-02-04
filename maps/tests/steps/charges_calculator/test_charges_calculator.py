from copy import deepcopy
from decimal import Decimal

import pytest

from maps_adv.common.helpers import Any, dt
from maps_adv.statistics.beekeeper.lib.steps.base import FreeEventProcessingMode

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def common_input_data():
    return {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 10,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    }
                ],
            }
        ],
    }


async def test_adds_expected_data(charges_calculator_step):
    input_data = {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("1"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 100,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("1"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 100,
                        "budget": Decimal("300"),
                        "daily_budget": Decimal("50"),
                        "charged": Decimal("150"),
                        "daily_charged": Decimal("120"),
                    },
                ],
            },
            {
                "order_id": None,
                "balance": Decimal("Infinity"),
                "campaigns": [
                    {
                        "campaign_id": 13,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("1"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 50,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                    }
                ],
            },
        ],
    }

    result = await charges_calculator_step.run(input_data)

    expected_result = deepcopy(input_data)
    for order_data in expected_result["orders"]:
        order_data["amount_to_bill"] = Any(Decimal)
        for campaign_data in order_data["campaigns"]:
            campaign_data["paid_events_to_charge"] = Any(int)
            campaign_data["last_paid_event_cost"] = Any(Decimal)
    assert result == expected_result


@pytest.mark.parametrize(
    ("paid_events_count", "paid_event_cost", "expected_amount_to_bill"),
    [
        (1, Decimal("0.5"), Decimal("0.5")),
        (1, Decimal("0.25"), Decimal("0.25")),
        (5, Decimal("0.25"), Decimal("1.25")),
    ],
)
async def test_charges_paid_events(
    charges_calculator_step,
    common_input_data,
    paid_events_count,
    paid_event_cost,
    expected_amount_to_bill,
):
    common_input_data["orders"][0]["campaigns"][0][
        "paid_events_count"
    ] = paid_events_count
    common_input_data["orders"][0]["campaigns"][0]["paid_event_cost"] = paid_event_cost

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == expected_amount_to_bill
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == paid_events_count
    assert campaign_data["last_paid_event_cost"] == paid_event_cost


@pytest.mark.parametrize(
    (
        "daily_charged",
        "expected_amount_to_bill",
        "expected_paid_events_to_charge",
        "expected_last_paid_event_cost",
    ),
    [
        (Decimal("0"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("95"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("97.5"), Decimal("2.5"), 5, Decimal("0.5")),
        (Decimal("98.7"), Decimal("1.3"), 3, Decimal("0.3")),
        (Decimal("100"), Decimal("0"), 0, Decimal("0")),
        (Decimal("150"), Decimal("0"), 0, Decimal("0")),
    ],
)
async def test_respects_campaign_daily_budget(
    charges_calculator_step,
    common_input_data,
    daily_charged,
    expected_amount_to_bill,
    expected_paid_events_to_charge,
    expected_last_paid_event_cost,
):
    common_input_data["orders"][0]["campaigns"][0]["daily_charged"] = daily_charged

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == expected_amount_to_bill
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == expected_paid_events_to_charge
    assert campaign_data["last_paid_event_cost"] == expected_last_paid_event_cost


@pytest.mark.parametrize(
    (
        "charged",
        "expected_amount_to_bill",
        "expected_paid_events_to_charge",
        "expected_last_paid_event_cost",
    ),
    [
        (Decimal("0"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("195"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("197.5"), Decimal("2.5"), 5, Decimal("0.5")),
        (Decimal("198.7"), Decimal("1.3"), 3, Decimal("0.3")),
        (Decimal("200"), Decimal("0"), 0, Decimal("0")),
        (Decimal("250"), Decimal("0"), 0, Decimal("0")),
    ],
)
async def test_respects_campaign_budget(
    charges_calculator_step,
    common_input_data,
    charged,
    expected_amount_to_bill,
    expected_paid_events_to_charge,
    expected_last_paid_event_cost,
):
    common_input_data["orders"][0]["campaigns"][0]["charged"] = charged

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == expected_amount_to_bill
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == expected_paid_events_to_charge
    assert campaign_data["last_paid_event_cost"] == expected_last_paid_event_cost


@pytest.mark.parametrize(
    (
        "balance",
        "expected_amount_to_bill",
        "expected_paid_events_to_charge",
        "expected_last_paid_event_cost",
    ),
    [
        (Decimal("100"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("5"), Decimal("5"), 10, Decimal("0.5")),
        (Decimal("2.5"), Decimal("2.5"), 5, Decimal("0.5")),
        (Decimal("1.3"), Decimal("1.3"), 3, Decimal("0.3")),
        (Decimal("0"), Decimal("0"), 0, Decimal("0")),
        (Decimal("-50"), Decimal("0"), 0, Decimal("0")),
    ],
)
async def test_respects_campaign_order_balance(
    charges_calculator_step,
    common_input_data,
    balance,
    expected_amount_to_bill,
    expected_paid_events_to_charge,
    expected_last_paid_event_cost,
):
    common_input_data["orders"][0]["balance"] = balance

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == expected_amount_to_bill
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == expected_paid_events_to_charge
    assert campaign_data["last_paid_event_cost"] == expected_last_paid_event_cost


@pytest.mark.parametrize(
    (
        "daily_charged",
        "charged",
        "balance",
        "expected_amount_to_bill",
        "expected_paid_events_to_charge",
        "expected_last_paid_event_cost",
    ),
    [
        (
            Decimal("98.7"),
            Decimal("0"),
            Decimal("100"),
            Decimal("1.3"),
            3,
            Decimal("0.3"),
        ),
        (
            Decimal("0"),
            Decimal("198.7"),
            Decimal("100"),
            Decimal("1.3"),
            3,
            Decimal("0.3"),
        ),
        (Decimal("0"), Decimal("0"), Decimal("1.3"), Decimal("1.3"), 3, Decimal("0.3")),
        (
            Decimal("98.7"),
            Decimal("199.7"),
            Decimal("0.4"),
            Decimal("0.3"),
            1,
            Decimal("0.3"),
        ),
        (
            Decimal("98.7"),
            Decimal("199.2"),
            Decimal("0.6"),
            Decimal("0.6"),
            2,
            Decimal("0.1"),
        ),
    ],
)
async def test_uses_closest_limit(
    charges_calculator_step,
    common_input_data,
    daily_charged,
    charged,
    balance,
    expected_amount_to_bill,
    expected_paid_events_to_charge,
    expected_last_paid_event_cost,
):
    common_input_data["orders"][0]["campaigns"][0]["daily_charged"] = daily_charged
    common_input_data["orders"][0]["campaigns"][0]["charged"] = charged
    common_input_data["orders"][0]["balance"] = balance

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == expected_amount_to_bill
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == expected_paid_events_to_charge
    assert campaign_data["last_paid_event_cost"] == expected_last_paid_event_cost


async def test_charges_all_events_for_campaign_with_unlimited_order(
    charges_calculator_step, common_input_data
):
    common_input_data["orders"][0]["balance"] = Decimal("Infinity")

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == Decimal("5")
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == 10
    assert campaign_data["last_paid_event_cost"] == Decimal("0.5")


async def test_charges_all_events_for_campaign_with_unlimited_daily_budget(
    charges_calculator_step, common_input_data
):
    common_input_data["orders"][0]["campaigns"][0]["daily_budget"] = Decimal("Infinity")

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == Decimal("5")
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == 10
    assert campaign_data["last_paid_event_cost"] == Decimal("0.5")


async def test_charges_all_events_for_campaign_with_unlimited_budget(
    charges_calculator_step, common_input_data
):
    common_input_data["orders"][0]["campaigns"][0]["budget"] = Decimal("Infinity")

    result = await charges_calculator_step.run(common_input_data)

    assert result["orders"][0]["amount_to_bill"] == Decimal("5")
    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data["paid_events_to_charge"] == 10
    assert campaign_data["last_paid_event_cost"] == Decimal("0.5")


async def test_charges_campaigns_from_different_orders_independently(
    charges_calculator_step,
):
    input_data = {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("1.25"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 100,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("191"),
                        "daily_charged": Decimal("20"),
                    }
                ],
            },
            {
                "order_id": 102,
                "balance": Decimal("400"),
                "campaigns": [
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("1"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 10,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                    }
                ],
            },
        ],
    }

    result = await charges_calculator_step.run(input_data)

    order1_data, order2_data = result["orders"]
    campaign1_data, campaign2_data = (
        order1_data["campaigns"][0],
        order2_data["campaigns"][0],
    )
    assert order1_data["amount_to_bill"] == Decimal("9")
    assert campaign1_data["paid_events_to_charge"] == 8
    assert campaign1_data["last_paid_event_cost"] == Decimal("0.25")
    assert order2_data["amount_to_bill"] == Decimal("10")
    assert campaign2_data["paid_events_to_charge"] == 10
    assert campaign2_data["last_paid_event_cost"] == Decimal("1")
