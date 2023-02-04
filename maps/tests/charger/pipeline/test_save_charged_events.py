from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.tests.tools import dt, make_event

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def setup_normalize_table(ch_client):
    norm_events = [
        make_event(*args)
        for args in (
            (1, 10000),
            (1, 10100),
            (1, 10200),
            (2, 10000),
            (2, 10300),
            (3, 10999),
            (9999, 10999),
        )
    ]
    ch_client.execute("INSERT INTO stat.normalized_sample VALUES", norm_events)


async def test_saves_charged_events_for_all_orders(
    ch_client, charger_pipeline, mock_charger_update_task
):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1.5),
                        "cost_per_last_event": Decimal(0.5),
                        "events_count": 3,
                        "events_to_charge": 2,  # Only 2 from existed 3
                    },
                    {
                        "campaign_id": 2,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(2.5),
                        "cost_per_last_event": Decimal(2.5),
                        "events_count": 3,
                        "events_to_charge": 2,
                    },
                ],
            },
            {
                "order_id": 567383,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 3,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(3.5),
                        "cost_per_last_event": Decimal(3.5),
                        "events_count": 3,
                        "events_to_charge": 1,
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
                        "campaign_id": 9999,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("20000"),
                        "daily_budget": Decimal("20000"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal("1"),
                        "cost_per_last_event": Decimal("1"),
                        "events_count": 1,
                        "events_to_charge": 1,
                    }
                ],
            },
        ],
    }

    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert set(got) == {
        make_event(1, 10100, "pin.show", Decimal(0.5)),
        make_event(1, 10200, "pin.show", Decimal(1.5)),
        make_event(2, 10000, "pin.show", Decimal(2.5)),
        make_event(2, 10300, "pin.show", Decimal(2.5)),
        make_event(3, 10999, "pin.show", Decimal(3.5)),
        make_event(9999, 10999, "pin.show", Decimal(1)),
    }


async def test_charges_no_order_campaign(charger_pipeline, ch_client):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": None,
                "budget_balance": Decimal("Inf"),
                "amount_to_bill": None,
                "billing_success": None,
                "campaigns": [
                    {
                        "campaign_id": 9999,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1),
                        "cost_per_last_event": Decimal(1),
                        "events_count": 1,
                        "events_to_charge": 1,
                    }
                ],
            }
        ],
    }
    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert set(got) == {make_event(9999, 10999, "pin.show", Decimal(1))}


async def test_skips_not_billed_orders(charger_pipeline, ch_client):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": False,
                "campaigns": [
                    {
                        "campaign_id": 1,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1.5),
                        "cost_per_last_event": Decimal(0.5),
                        "events_count": 3,
                        "events_to_charge": 3,
                    }
                ],
            }
        ],
    }

    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert got == []


async def test_charges_nothing_if_no_orders(charger_pipeline, ch_client):
    await charger_pipeline.save_charged_events(
        task_data={
            "timing_from": dt(0),
            "timing_to": dt(11000),
            "id": 100,
            "status": "charged_data_sent",
            "execution_state": [],
        }
    )

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert got == []


async def test_charges_nothing_if_no_events_in_time_range(charger_pipeline, ch_client):
    task_data = {
        "timing_from": dt(11000),
        "timing_to": dt(12000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1.5),
                        "cost_per_last_event": Decimal(0.5),
                        "events_count": 3,
                        "events_to_charge": 3,
                    }
                ],
            }
        ],
    }

    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert got == []


async def test_skips_out_of_limits_events(charger_pipeline, ch_client):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1.5),
                        "cost_per_last_event": Decimal(0.5),
                        "events_count": 3,
                        "events_to_charge": 2,
                    }
                ],
            }
        ],
    }

    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert set(got) == {
        make_event(1, 10100, "pin.show", Decimal(0.5)),
        make_event(1, 10200, "pin.show", Decimal(1.5)),
    }


async def test_charges_as_last_for_single_event(charger_pipeline, ch_client):
    task_data = {
        "timing_from": dt(0),
        "timing_to": dt(11000),
        "id": 100,
        "status": "charged_data_sent",
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("100"),
                "amount_to_bill": Decimal("4"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 1,
                        "tz_name": "UTC",
                        "cpm": Decimal("1000"),
                        "budget": Decimal("2"),
                        "daily_budget": Decimal("20"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "cost_per_event": Decimal(1.5),
                        "cost_per_last_event": Decimal(0.5),
                        "events_count": 3,
                        "events_to_charge": 1,
                    }
                ],
            }
        ],
    }

    await charger_pipeline.save_charged_events(task_data)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert set(got) == {make_event(1, 10200, "pin.show", Decimal(0.5))}
