from decimal import Decimal

from maps_adv.stat_tasks_starter.lib.charger.campaigns_stopper.filter import (
    filter_to_stop,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.adv_store.enums import (
    ReasonsToStop,
)


def test_returns_campaigns_to_close():
    orders_list = [
        {
            "order_id": 567382,
            "budget_balance": Decimal("200"),
            "amount_to_bill": Decimal("18"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
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
                }
            ],
        },
        {
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
            ],
        },
        {
            "order_id": 232365,
            "budget_balance": Decimal("40"),
            # order limit reached
            "amount_to_bill": Decimal("40"),
            "billing_success": True,
            "campaigns": [
                {
                    "campaign_id": 1234,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 4,
                    "events_to_charge": 4,
                },
                {
                    "campaign_id": 3523,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 4,
                    "events_to_charge": 4,
                },
            ],
        },
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 9786, "reason_stopped": ReasonsToStop.daily_budget_limit},
        {"campaign_id": 8764, "reason_stopped": ReasonsToStop.budget_limit},
        {"campaign_id": 1234, "reason_stopped": ReasonsToStop.order_limit},
        {"campaign_id": 3523, "reason_stopped": ReasonsToStop.order_limit},
    ]


def test_ignore_order_limits_reasons_for_no_order_campaigns():
    orders_list = [
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    # all limits ok, will be ignored
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
                    # daily limit reached
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
            ],
        },
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    "campaign_id": 1234,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 4,
                    "events_to_charge": 4,
                },
                {
                    "campaign_id": 3523,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 4,
                    "events_to_charge": 4,
                },
            ],
        },
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 9786, "reason_stopped": ReasonsToStop.daily_budget_limit},
        {"campaign_id": 8764, "reason_stopped": ReasonsToStop.budget_limit},
    ]


def test_returns_campaigns_to_close_if_zero_events_charged():
    orders_list = [
        {
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("30"),
            "billing_success": True,
            "campaigns": [
                {
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 6,
                },
                {
                    # order limit reached, nothing to charge
                    "campaign_id": 8764,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 5,
                    "events_to_charge": 0,
                },
            ],
        }
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 9786, "reason_stopped": ReasonsToStop.order_limit},
        {"campaign_id": 8764, "reason_stopped": ReasonsToStop.order_limit},
    ]


def test_returns_campaigns_with_budgets_overrun():
    orders_list = [
        {
            # order limit overrun
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("40"),
            "billing_success": True,
            "campaigns": [
                {
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 6,
                }
            ],
        },
        {
            "order_id": 734582,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget overrun by calculation
                    "campaign_id": 6528,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 6,
                },
                {
                    # daily budget overrun by calculation
                    "campaign_id": 8365,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("5"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 6,
                },
                {
                    # correct campaign
                    # will be ignored because of limits are not reached
                    "campaign_id": 8432,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 5,
                    "events_to_charge": 4,
                },
            ],
        },
        {
            "order_id": 976532,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget overrun by data in charged field
                    "campaign_id": 1234,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("10"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 10,
                    "events_to_charge": 0,
                },
                {
                    # daily budget overrun by data in charged_daily field
                    "campaign_id": 2345,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("5"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("10"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 10,
                    "events_to_charge": 0,
                },
                {
                    # correct campaign
                    # will be ignored because of limits are not reached
                    "campaign_id": 3456,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 5,
                    "events_to_charge": 4,
                },
            ],
        },
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 9786, "reason_stopped": ReasonsToStop.order_limit},
        {"campaign_id": 6528, "reason_stopped": ReasonsToStop.budget_limit},
        {"campaign_id": 8365, "reason_stopped": ReasonsToStop.daily_budget_limit},
        {"campaign_id": 1234, "reason_stopped": ReasonsToStop.budget_limit},
        {"campaign_id": 2345, "reason_stopped": ReasonsToStop.daily_budget_limit},
    ]


def test_return_empty_list_if_nothing_to_close():
    orders_list = [
        {
            "order_id": 567382,
            "budget_balance": Decimal("200"),
            "amount_to_bill": Decimal("18"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
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
                }
            ],
        },
        {
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("100"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 4,
                    "events_to_charge": 4,
                }
            ],
        },
    ]

    got = filter_to_stop(orders_list)

    assert got == []


def test_returns_closing_by_order_if_other_limits_reached_too():
    orders_list = [
        {
            "order_id": 232365,
            "budget_balance": Decimal("20"),
            # order limit reached => expecting closing by order limit
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                },
            ],
        }
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 9786, "reason_stopped": ReasonsToStop.order_limit},
        {"campaign_id": 8764, "reason_stopped": ReasonsToStop.order_limit},
    ]


def test_returns_closing_by_budget_if_daily_limit_reached_too():
    orders_list = [
        {
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    # budget limit reached
                    # => expecting closing by budget
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                }
            ],
        }
    ]

    got = filter_to_stop(orders_list)

    assert got == [{"campaign_id": 9786, "reason_stopped": ReasonsToStop.budget_limit}]


def test_ignores_not_billed_orders():
    orders_list = [
        {
            "order_id": 232365,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            # will be ignored because of
            # unsuccess billing attempt
            "billing_success": False,
            "campaigns": [
                {
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                }
            ],
        },
        {
            "order_id": 342566,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    # budget limit reached
                    # => expecting closing by budget
                    "campaign_id": 23534,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": Decimal("5"),
                    "cost_per_last_event": Decimal("5"),
                    "events_count": 10,
                    "events_to_charge": 4,
                }
            ],
        },
    ]

    got = filter_to_stop(orders_list)

    assert got == [{"campaign_id": 23534, "reason_stopped": ReasonsToStop.budget_limit}]


def test_returns_campaigns_with_budgets_overrun_if_nothing_to_charge():
    orders_list = [
        {
            "order_id": 734582,
            "budget_balance": Decimal("30"),
            "amount_to_bill": Decimal("0"),
            "billing_success": None,
            "campaigns": [
                {
                    # budget overrun
                    "campaign_id": 6528,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("20"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 10,
                    "events_to_charge": 0,
                },
                {
                    # daily budget overrun
                    "campaign_id": 8365,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("5"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("20"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 10,
                    "events_to_charge": 6,
                },
                {
                    # correct campaign,
                    # will be ignored because no limit reached
                    "campaign_id": 9786,
                    "tz_name": "UTC",
                    "cpm": Decimal("5000"),
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "charged_daily": Decimal("0"),
                    "cost_per_event": None,
                    "cost_per_last_event": None,
                    "events_count": 0,
                    "events_to_charge": 0,
                },
            ],
        }
    ]

    got = filter_to_stop(orders_list)

    assert got == [
        {"campaign_id": 6528, "reason_stopped": ReasonsToStop.budget_limit},
        {"campaign_id": 8365, "reason_stopped": ReasonsToStop.daily_budget_limit},
    ]
