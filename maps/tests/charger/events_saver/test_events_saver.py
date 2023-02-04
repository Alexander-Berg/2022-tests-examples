from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.charger.events_saver import EventsSaver
from maps_adv.stat_tasks_starter.tests.tools import dt, make_event

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def setup_normalize_table(ch_client):
    norm_events = [
        make_event(*args)
        for args in (
            (1, 10000),
            (1, 10000, "lol"),
            (1, 10100),
            (1, 10100, "kek"),
            (1, 10200),
            (2, 10000),
            (2, 10000, "cheburek"),
            (2, 10300),
            (3, 10999),
            (9999, 10999),
        )
    ]
    ch_client.execute("INSERT INTO stat.normalized_sample VALUES", norm_events)


@pytest.fixture(scope="module")
def db_result(ch_client):
    return lambda: ch_client.execute("SELECT * FROM stat.accepted_sample")


@pytest.fixture
def event_saver(loop):
    return EventsSaver(
        db_config={
            "database": "stat",
            "normalized_table": "normalized_sample",
            "charged_table": "accepted_sample",
            "host": "localhost",
            "port": 9001,
        }
    )


@pytest.fixture
def base_orders():
    return [
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
    ]


def check_correctness(got, expected_events):
    expected_result = {make_event(*event_args) for event_args in expected_events}
    assert sorted(got, key=lambda e: (e[1], e[0])) == sorted(
        expected_result, key=lambda e: (e[1], e[0])
    )


# ==================================================


async def test_charges_orders_events(db_result, event_saver):
    orders = [
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
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    expected_events = (
        (1, 10100, "pin.show", Decimal(0.5)),
        (1, 10200, "pin.show", Decimal(1.5)),
        (2, 10000, "pin.show", Decimal(2.5)),
        (2, 10300, "pin.show", Decimal(2.5)),
        (3, 10999, "pin.show", Decimal(3.5)),
        (9999, 10999, "pin.show", Decimal(1)),
    )
    check_correctness(got, expected_events)


async def test_charges_only_pin_show_order_events(base_orders, db_result, event_saver):
    await event_saver(base_orders, dt(0), dt(11000))
    got = db_result()

    expected_events = (
        (1, 10000, "pin.show", Decimal(0.5)),
        (1, 10100, "pin.show", Decimal(1.5)),
        (1, 10200, "pin.show", Decimal(1.5)),
    )
    check_correctness(got, expected_events)


async def test_charges_no_order_campaign(db_result, event_saver):
    orders = [
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
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    expected_events = ((9999, 10999, "pin.show", Decimal(1)),)
    check_correctness(got, expected_events)


async def test_skips_not_billed_orders(db_result, event_saver):
    orders = [
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
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    assert got == []


async def test_charges_nothing_if_no_orders(db_result, event_saver):
    await event_saver([], dt(0), dt(11000))
    got = db_result()

    assert got == []


async def test_charges_nothing_if_no_events_in_time_range(
    base_orders, db_result, event_saver
):
    await event_saver(base_orders, dt(11000), dt(12000))
    got = db_result()

    assert got == []


async def test_skips_out_of_limits_events(db_result, event_saver):
    orders = [
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
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    expected_events = (
        (1, 10100, "pin.show", Decimal(0.5)),
        (1, 10200, "pin.show", Decimal(1.5)),
    )
    check_correctness(got, expected_events)


async def test_charges_as_last_for_single_event(db_result, event_saver):
    orders = [
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
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    expected_events = ((1, 10200, "pin.show", Decimal(0.5)),)
    check_correctness(got, expected_events)


async def test_saves_full_cost_with_supported_precision(db_result, event_saver):
    orders = [
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
                    "cost_per_event": Decimal("1.234567"),
                    "cost_per_last_event": Decimal("1.234567"),
                    "events_count": 3,
                    "events_to_charge": 1,
                }
            ],
        }
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    expected_events = ((1, 10200, "pin.show", Decimal("1.234567")),)
    check_correctness(got, expected_events)


async def test_cuts_cost_with_unsupported_precision(db_result, event_saver):
    orders = [
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
                    "cost_per_event": Decimal("1.2345678"),  # unsupported precision
                    "cost_per_last_event": Decimal("1.2345678"),
                    "events_count": 3,
                    "events_to_charge": 1,
                }
            ],
        }
    ]
    await event_saver(orders, dt(0), dt(11000))
    got = db_result()

    # cuts unsupported precision
    expected_events = ((1, 10200, "pin.show", Decimal("1.234567")),)
    check_correctness(got, expected_events)


async def test_does_not_charge_if_already_charged_in_period(db_result, event_saver):
    orders = [
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
                }
            ],
        },
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
    ]

    await event_saver(orders, dt(0), dt(11000))
    await event_saver(orders, dt(0), dt(11000))

    got = db_result()

    expected_events = (
        (1, 10100, "pin.show", Decimal(0.5)),
        (1, 10200, "pin.show", Decimal(1.5)),
        (9999, 10999, "pin.show", Decimal(1)),
    )
    check_correctness(got, expected_events)


async def test_adds_log_record_if_already_charged_in_period(
    db_result, event_saver, caplog
):
    orders = [
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
                }
            ],
        },
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
    ]

    await event_saver(orders, dt(0), dt(11000))
    await event_saver(orders, dt(0), dt(11000))

    error_messages = [r for r in caplog.records if r.levelname == "ERROR"]
    assert len(error_messages) == 1

    error_message = error_messages[0].message
    assert (
        error_message == "Events already charged in requested period "
        "(1970-01-01 00:00:00+00:00, 1970-01-01 03:03:20+00:00)"
    )
