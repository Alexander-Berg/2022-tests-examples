from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.charger.calculator import (
    UnsupportedPrecision,
    calculate_charges,
)


def test_consider_campaign_budget_limit():
    order_data = {
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
    }

    result = calculate_charges(order_data)

    assert result == {
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
    }


def test_consider_order_budget_limit():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("2"),
        "campaigns": [
            {
                "campaign_id": 4242,
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
                "campaign_id": 4356,
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
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("2"),
        # 2 events per 1 rub
        "amount_to_bill": Decimal("2"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
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
                "campaign_id": 4356,
                "tz_name": "UTC",
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
    }


def test_consider_special_last_event_in_campaign():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("10.35"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                # all 3 events fit in order budget limit
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("20"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 3,
            },
            {
                "campaign_id": 8765,
                "tz_name": "UTC",
                # only 3 events fit in order budget limit
                # last event has special price
                "cpm": Decimal("3000"),
                "budget": Decimal("300"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 4,
            },
        ],
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("10.35"),
        # 3 events per 1 rub for fist campaign
        # 2 events per 3 rub for second campaign
        # 1 event per 1.35 rub for second campaign
        "amount_to_bill": Decimal("10.35"),
        "campaigns": [
            {
                "campaign_id": 4242,
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
            },
            {
                "campaign_id": 8765,
                "tz_name": "UTC",
                "cpm": Decimal("3000"),
                "budget": Decimal("300"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("3"),
                "cost_per_last_event": Decimal("1.35"),
                "events_count": 4,
                "events_to_charge": 3,
            },
        ],
    }


def test_consider_charged_limitation_per_campaign():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("200"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                # events will be ignored because charged == budget
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("20"),
                "charged_daily": Decimal("0"),
                "events_count": 3,
            },
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                # events will be ignored because daily_charged == daily_budget
                "cpm": Decimal("2000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("30"),
                "charged_daily": Decimal("30"),
                "events_count": 2,
            },
        ],
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("200"),
        # all events ignored
        "amount_to_bill": Decimal("0"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("20"),
                "charged_daily": Decimal("0"),
                "cost_per_event": None,
                "cost_per_last_event": None,
                "events_count": 3,
                "events_to_charge": 0,
            },
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                "cpm": Decimal("2000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("30"),
                "charged_daily": Decimal("30"),
                "cost_per_event": None,
                "cost_per_last_event": None,
                "events_count": 2,
                "events_to_charge": 0,
            },
        ],
    }


def test_calculate_all_events_if_infinite_budgets_and_enough_in_order():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("200"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                # all 3 events counted
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 3,
            },
            {
                "campaign_id": 8765,
                "tz_name": "UTC",
                # all 4 events counted
                "cpm": Decimal("2000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 4,
            },
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                # all 2 events counted
                "cpm": Decimal("3500"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 2,
            },
        ],
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("200"),
        # 3 events per 1 rub for fist campaign
        # 4 events per 2 rub for second campaign
        # 2 event per 3.5 rub for third campaign
        "amount_to_bill": Decimal("18"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 3,
                "events_to_charge": 3,
            },
            {
                "campaign_id": 8765,
                "tz_name": "UTC",
                "cpm": Decimal("2000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("30"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("2"),
                "cost_per_last_event": Decimal("2"),
                "events_count": 4,
                "events_to_charge": 4,
            },
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                "cpm": Decimal("3500"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("3.5"),
                "cost_per_last_event": Decimal("3.5"),
                "events_count": 2,
                "events_to_charge": 2,
            },
        ],
    }


@pytest.mark.parametrize(
    "budget, daily_budget",
    [
        (Decimal("30"), Decimal("Infinity")),
        (Decimal("Infinity"), Decimal("30")),
        (Decimal("Infinity"), Decimal("Infinity")),
    ],
)
def test_consider_order_limit_if_infinite_budgets_in_campaign(budget, daily_budget):
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("5"),
        "campaigns": [
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                # all 2 events counted
                # one has special price because of order limit
                "cpm": Decimal("3500"),
                "budget": budget,
                "daily_budget": daily_budget,
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 4,
            }
        ],
    }
    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("5"),
        # 1 event per 3.5 rub + 1 event per 1.5 rub
        "amount_to_bill": Decimal("5"),
        "campaigns": [
            {
                "campaign_id": 4356,
                "tz_name": "UTC",
                "cpm": Decimal("3500"),
                "budget": budget,
                "daily_budget": daily_budget,
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("3.5"),
                "cost_per_last_event": Decimal("1.5"),
                "events_count": 4,
                "events_to_charge": 2,
            }
        ],
    }


def test_considers_infinite_order_budget_for_no_order_campaigns():
    order_data = {
        "order_id": None,
        "budget_balance": Decimal("Inf"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                # events will be limited by campaign budgets
                "cpm": Decimal("1000"),
                "budget": Decimal("20000"),
                "daily_budget": Decimal("20000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 9999999,
            }
        ],
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": None,
        "budget_balance": Decimal("Inf"),
        # No amount to bill if no order
        "amount_to_bill": None,
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20000"),
                "daily_budget": Decimal("20000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 9999999,
                "events_to_charge": 20000,
            }
        ],
    }


@pytest.mark.parametrize(
    "campaigns, expected_log_snippet",
    [
        # charged > budget
        (
            [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("100"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("200"),
                    "charged_daily": Decimal("5"),
                    "events_count": 3,
                }
            ],
            "Negative charge limit detected: campaign_id=4242, "
            "order_id=567382, budget_balance=100, campaign="
            "{'campaign_id': 4242, 'tz_name': 'UTC', 'cpm': Decimal('1000'), "
            "'budget': Decimal('100'), 'daily_budget': Decimal('20'), "
            "'charged': Decimal('200'), 'charged_daily': Decimal('5'), "
            "'events_count': 3}",
        ),
        # charged_daily > daily_budget
        (
            [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("100"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("50"),
                    "charged_daily": Decimal("50"),
                    "events_count": 3,
                }
            ],
            "Negative charge limit detected: campaign_id=4242, "
            "order_id=567382, budget_balance=100, campaign="
            "{'campaign_id': 4242, 'tz_name': 'UTC', 'cpm': Decimal('1000'), "
            "'budget': Decimal('100'), 'daily_budget': Decimal('20'), "
            "'charged': Decimal('50'), 'charged_daily': Decimal('50'), "
            "'events_count': 3}",
        ),
        # multiple campaigns in order
        (
            [
                # correct campaign
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("50"),
                    "charged_daily": Decimal("50"),
                    "events_count": 3,
                },
                # campaign with negative charge
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("100"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("50"),
                    "charged_daily": Decimal("50"),
                    "events_count": 3,
                },
                # correct campaign
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal("1000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("50"),
                    "charged_daily": Decimal("50"),
                    "events_count": 3,
                },
            ],
            "Negative charge limit detected: campaign_id=4242, "
            "order_id=567382, budget_balance=100, campaign="
            "{'campaign_id': 4242, 'tz_name': 'UTC', 'cpm': Decimal('1000'), "
            "'budget': Decimal('100'), 'daily_budget': Decimal('20'), "
            "'charged': Decimal('50'), 'charged_daily': Decimal('50'), "
            "'events_count': 3}",
        ),
    ],
)
def test_logs_bad_calculation_reason(caplog, campaigns, expected_log_snippet):
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("100"),
        "campaigns": campaigns,
    }

    calculate_charges(order_data)

    error_messages = [r for r in caplog.records if r.levelname == "ERROR"]
    assert len(error_messages) == 1

    error_message = error_messages[0].message
    assert expected_log_snippet == error_message


def test_campaigns_with_bad_calculations_do_not_affect_amount_to_bill():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("100"),
        "campaigns": [
            # correct campaign
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "events_count": 3,
            },
            # charged_daily > daily_budget
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("100"),
                "daily_budget": Decimal("20"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "events_count": 3,
            },
            # charged > budget
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "events_count": 3,
            },
        ],
    }

    calculate_charges(order_data)

    assert order_data == {
        "order_id": 567382,
        "budget_balance": Decimal("100"),
        "amount_to_bill": Decimal("3"),
        "campaigns": [
            # correct campaign
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("Infinity"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "cost_per_event": Decimal("1"),
                "cost_per_last_event": Decimal("1"),
                "events_count": 3,
                "events_to_charge": 3,
            },
            # charged_daily > daily_budget
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("100"),
                "daily_budget": Decimal("20"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "cost_per_event": None,
                "cost_per_last_event": None,
                "events_count": 3,
                "events_to_charge": 0,
            },
            # charged > budget
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("1000"),
                "budget": Decimal("20"),
                "daily_budget": Decimal("Infinity"),
                "charged": Decimal("50"),
                "charged_daily": Decimal("50"),
                "cost_per_event": None,
                "cost_per_last_event": None,
                "events_count": 3,
                "events_to_charge": 0,
            },
        ],
    }


def test_calculates_with_fractional_cpm():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("1000"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("2.34"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("1000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 10_000,
            }
        ],
    }

    result = calculate_charges(order_data)

    assert result == {
        "order_id": 567382,
        "budget_balance": Decimal("1000"),
        "amount_to_bill": Decimal("23.4"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                "cpm": Decimal("2.34"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("1000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "cost_per_event": Decimal("0.00234"),
                "cost_per_last_event": Decimal("0.00234"),
                "events_count": 10_000,
                "events_to_charge": 10_000,
            }
        ],
    }


def test_raises_for_cost_per_event_with_unsupported_precision():
    order_data = {
        "order_id": 567382,
        "budget_balance": Decimal("1000"),
        "campaigns": [
            {
                "campaign_id": 4242,
                "tz_name": "UTC",
                # will lead to unsupported precision of cost_per_event
                "cpm": Decimal("2.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("1000"),
                "charged": Decimal("0"),
                "charged_daily": Decimal("0"),
                "events_count": 10_000,
            }
        ],
    }

    with pytest.raises(UnsupportedPrecision) as exc_info:
        calculate_charges(order_data)

    assert exc_info.value.args == (
        "Unsupported precision of cost_per_event=0.0023456 "
        "for campaign with id=4242. "
        "Only 6 decimal places are supported.",
    )
