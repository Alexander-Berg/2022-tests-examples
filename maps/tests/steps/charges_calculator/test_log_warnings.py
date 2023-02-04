import logging
from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("caplog_set_level_error")]


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
                    }
                ],
            }
        ],
    }


async def test_warns_about_over_daily_budget_campaigns(
    caplog, charges_calculator_step, common_input_data
):
    campaign_data = common_input_data["orders"][0]["campaigns"][0]
    campaign_data["daily_charged"] = Decimal("110")

    await charges_calculator_step.run(common_input_data)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert (
        record.msg == "Negative charge limit detected for campaign 11 (daily_budget): "
        "daily_budget=%f, daily_charged=%f (%s)"
    )
    assert record.args == (Decimal("100"), Decimal("110"), campaign_data)


async def test_warns_about_over_budget_campaigns(
    caplog, charges_calculator_step, common_input_data
):
    campaign_data = common_input_data["orders"][0]["campaigns"][0]
    campaign_data["charged"] = Decimal("220")

    await charges_calculator_step.run(common_input_data)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert (
        record.msg == "Negative charge limit detected for campaign 11 (budget): "
        "budget=%f, charged=%f (%s)"
    )
    assert record.args == (Decimal("200"), Decimal("220"), campaign_data)


async def test_warns_about_multiple_over_limits(
    caplog, charges_calculator_step, common_input_data
):
    campaign_data = common_input_data["orders"][0]["campaigns"][0]
    campaign_data["daily_charged"] = Decimal("110")
    campaign_data["charged"] = Decimal("220")

    await charges_calculator_step.run(common_input_data)

    assert len(caplog.records) == 2
    assert (
        caplog.records[0].msg
        == "Negative charge limit detected for campaign 11 (daily_budget): "
        "daily_budget=%f, daily_charged=%f (%s)"
    )
    assert caplog.records[0].args == (Decimal("100"), Decimal("110"), campaign_data)

    assert (
        caplog.records[1].msg
        == "Negative charge limit detected for campaign 11 (budget): "
        "budget=%f, charged=%f (%s)"
    )
    assert caplog.records[1].args == (Decimal("200"), Decimal("220"), campaign_data)


async def test_warns_about_multiple_overbudget_campaigns(
    caplog, charges_calculator_step
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
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 10,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("115"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 10,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("230"),
                        "daily_charged": Decimal("20"),
                    },
                ],
            },
            {
                "order_id": 102,
                "balance": Decimal("3000"),
                "campaigns": [
                    {
                        "campaign_id": 13,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW"],
                        "paid_events_count": 10,
                        "budget": Decimal("1000"),
                        "daily_budget": Decimal("500"),
                        "charged": Decimal("700"),
                        "daily_charged": Decimal("600"),
                    }
                ],
            },
        ],
    }

    await charges_calculator_step.run(input_data)

    assert len(caplog.records) == 3
    assert (
        caplog.records[0].msg
        == "Negative charge limit detected for campaign 11 (daily_budget): "
        "daily_budget=%f, daily_charged=%f (%s)"
    )
    assert caplog.records[0].args == (
        Decimal("100"),
        Decimal("115"),
        input_data["orders"][0]["campaigns"][0],
    )

    assert (
        caplog.records[1].msg
        == "Negative charge limit detected for campaign 12 (budget): "
        "budget=%f, charged=%f (%s)"
    )
    assert caplog.records[1].args == (
        Decimal("200"),
        Decimal("230"),
        input_data["orders"][0]["campaigns"][1],
    )

    assert (
        caplog.records[2].msg
        == "Negative charge limit detected for campaign 13 (daily_budget): "
        "daily_budget=%f, daily_charged=%f (%s)"
    )
    assert caplog.records[2].args == (
        Decimal("500"),
        Decimal("600"),
        input_data["orders"][1]["campaigns"][0],
    )
