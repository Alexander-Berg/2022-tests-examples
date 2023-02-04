from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def common_input_data():
    return {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("200"),
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 40,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.625"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 50,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                    },
                ],
            }
        ],
    }


async def test_respects_all_campaigns_in_amount_to_bill(
    charges_calculator_step, common_input_data
):
    result = await charges_calculator_step.run(common_input_data)

    order_data = result["orders"][0]
    assert order_data["amount_to_bill"] == Decimal("51.25")


async def test_respects_budget_limits(charges_calculator_step, common_input_data):
    common_input_data["orders"][0]["campaigns"][0]["budget"] = Decimal("55")
    common_input_data["orders"][0]["campaigns"][1]["daily_budget"] = Decimal("32.5")

    result = await charges_calculator_step.run(common_input_data)

    order_data = result["orders"][0]
    assert order_data["amount_to_bill"] == Decimal("17.5")
    assert order_data["campaigns"][0]["paid_events_to_charge"] == 10
    assert order_data["campaigns"][0]["last_paid_event_cost"] == Decimal("0.5")
    assert order_data["campaigns"][1]["paid_events_to_charge"] == 20
    assert order_data["campaigns"][1]["last_paid_event_cost"] == Decimal("0.625")


async def test_prefers_first_campaign_if_not_enough_balance_for_both(
    charges_calculator_step, common_input_data
):
    common_input_data["orders"][0]["balance"] = Decimal("25")

    result = await charges_calculator_step.run(common_input_data)

    order_data = result["orders"][0]
    assert order_data["amount_to_bill"] == Decimal("25")
    assert order_data["campaigns"][0]["paid_events_to_charge"] == 40
    assert order_data["campaigns"][0]["last_paid_event_cost"] == Decimal("0.5")
    assert order_data["campaigns"][1]["paid_events_to_charge"] == 8
    assert order_data["campaigns"][1]["last_paid_event_cost"] == Decimal("0.625")


async def test_charges_zero_from_second_campigns_if_balance_is_zero_after_first_campaign(  # noqa: E501
    charges_calculator_step, common_input_data
):
    common_input_data["orders"][0]["balance"] = Decimal("15")

    result = await charges_calculator_step.run(common_input_data)

    order_data = result["orders"][0]
    assert order_data["amount_to_bill"] == Decimal("15")
    assert order_data["campaigns"][0]["paid_events_to_charge"] == 30
    assert order_data["campaigns"][0]["last_paid_event_cost"] == Decimal("0.5")
    assert order_data["campaigns"][1]["paid_events_to_charge"] == 0
    assert order_data["campaigns"][1]["last_paid_event_cost"] == Decimal("0")
