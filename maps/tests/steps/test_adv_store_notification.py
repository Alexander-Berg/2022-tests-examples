from decimal import Decimal

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    ReasonCampaignStoppedEnum as ReasonStopped,
)
from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.steps import AdvStoreNotification

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def adv_store_notification(adv_store_client_mock):
    return AdvStoreNotification(adv_store_client_mock)


async def test_stop_campaigns_to_close(adv_store_notification, adv_store_client_mock):
    orders_list = [
        {
            "order_id": 567382,
            "balance": Decimal("200"),
            "amount_to_bill": Decimal("18"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 4242,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("1"),
                    "last_paid_event_cost": Decimal("1"),
                    "paid_events_count": 3,
                    "paid_events_to_charge": 3,
                }
            ],
        },
        {
            "order_id": 232365,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                },
            ],
        },
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={
            8764: ReasonStopped.BUDGET_REACHED,
        },
    )


async def test_ignore_order_limits_reasons_for_no_order_campaigns(
    adv_store_notification, adv_store_client_mock
):
    orders_list = [
        {
            "order_id": None,
            "balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 4242,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("1"),
                    "last_paid_event_cost": Decimal("1"),
                    "paid_events_count": 3,
                    "paid_events_to_charge": 3,
                }
            ],
        },
        {
            "order_id": None,
            "balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                },
            ],
        },
        {
            "order_id": None,
            "balance": Decimal("Inf"),
            "amount_to_bill": None,
            "billing_success": None,
            "campaigns": [
                {
                    "campaign_id": 1234,
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 4,
                    "paid_events_to_charge": 4,
                },
                {
                    "campaign_id": 3523,
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 4,
                    "paid_events_to_charge": 4,
                },
            ],
        },
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={
            8764: ReasonStopped.BUDGET_REACHED,
        },
    )


@pytest.mark.parametrize(
    ["orders", "expected_stop_campaigns"],
    [
        (
            [
                {
                    "order_id": 111111,
                    "balance": Decimal("30"),
                    "amount_to_bill": Decimal("29"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            "campaign_id": 1234,
                            "budget": Decimal("Infinity"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("0"),
                            "daily_charged": Decimal("0"),
                            "paid_event_cost": Decimal("5"),
                            "last_paid_event_cost": Decimal("5"),
                            "paid_events_count": 0,
                            "paid_events_to_charge": 0,
                        }
                    ],
                },
            ],
            {},
        ),
        (
            [
                {
                    "order_id": None,
                    "balance": Decimal("30"),
                    "amount_to_bill": Decimal("30"),
                    "billing_success": True,
                    "campaigns": [
                        {
                            # over budget
                            "campaign_id": 1234,
                            "budget": Decimal("30"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("30"),
                            "daily_charged": Decimal("0"),
                            "paid_event_cost": Decimal("5"),
                            "last_paid_event_cost": Decimal("5"),
                            "paid_events_count": 0,
                            "paid_events_to_charge": 0,
                        },
                        {
                            # haven't over daily budget
                            "campaign_id": 1236,
                            "budget": Decimal("Infinity"),
                            "daily_budget": Decimal("30"),
                            "charged": Decimal("0"),
                            "daily_charged": Decimal("29"),
                            "paid_event_cost": None,
                            "last_paid_event_cost": None,
                            "paid_events_count": 0,
                            "paid_events_to_charge": 0,
                        },
                        {
                            # haven't over budget
                            "campaign_id": 1236,
                            "budget": Decimal("30"),
                            "daily_budget": Decimal("Infinity"),
                            "charged": Decimal("29"),
                            "daily_charged": Decimal("0"),
                            "paid_event_cost": None,
                            "last_paid_event_cost": None,
                            "paid_events_count": 0,
                            "paid_events_to_charge": 0,
                        },
                    ],
                }
            ],
            {
                1234: ReasonStopped.BUDGET_REACHED,
            },
        ),
    ],
)
async def test_stop_campaigns_to_close_if_zero_events_charged(
    orders, expected_stop_campaigns, adv_store_notification, adv_store_client_mock
):
    await adv_store_notification.run(
        {"orders": orders, "packet_end": dt(1566313386)}, None
    )

    if not expected_stop_campaigns:
        return

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386), campaigns_to_stop=expected_stop_campaigns
    )


async def test_stop_campaigns_with_budgets_overrun(
    adv_store_notification, adv_store_client_mock
):
    orders_list = [
        {
            "order_id": 734582,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget overrun by calculation
                    "campaign_id": 6528,
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 6,
                },
                {
                    # correct campaign
                    # will be ignored because of limits are not reached
                    "campaign_id": 8432,
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 5,
                    "paid_events_to_charge": 4,
                },
            ],
        },
        {
            "order_id": 976532,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget overrun by data in charged field
                    "campaign_id": 1234,
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("10"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": None,
                    "last_paid_event_cost": None,
                    "paid_events_count": 10,
                    "paid_events_to_charge": 0,
                },
                {
                    # correct campaign
                    # will be ignored because of limits are not reached
                    "campaign_id": 3456,
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 5,
                    "paid_events_to_charge": 4,
                },
            ],
        },
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={
            6528: ReasonStopped.BUDGET_REACHED,
            1234: ReasonStopped.BUDGET_REACHED,
        },
    )


async def test_no_called_adv_client_if_nothing_to_close(
    adv_store_notification, adv_store_client_mock
):
    orders_list = [
        {
            "order_id": 567382,
            "balance": Decimal("200"),
            "amount_to_bill": Decimal("18"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 4242,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("1"),
                    "last_paid_event_cost": Decimal("1"),
                    "paid_events_count": 3,
                    "paid_events_to_charge": 3,
                }
            ],
        },
        {
            "order_id": 232365,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 9786,
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("100"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 4,
                    "paid_events_to_charge": 4,
                }
            ],
        },
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    assert not adv_store_client_mock.stop_campaigns.called


async def test_stop_closing_by_budget_if_daily_limit_reached_too(
    adv_store_notification, adv_store_client_mock
):
    orders_list = [
        {
            "order_id": 232365,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    # budget limit reached
                    # => expecting closing by budget
                    "campaign_id": 9786,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                }
            ],
        }
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={9786: ReasonStopped.BUDGET_REACHED},
    )


async def test_ignores_not_billed_orders(adv_store_notification, adv_store_client_mock):
    orders_list = [
        {
            "order_id": 232365,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            # will be ignored because of
            # unsuccess billing attempt
            "billing_success": False,
            "campaigns": [
                {
                    "campaign_id": 9786,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                }
            ],
        },
        {
            "order_id": 342566,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # daily limit reached
                    # budget limit reached
                    # => expecting closing by budget
                    "campaign_id": 23534,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                }
            ],
        },
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={23534: ReasonStopped.BUDGET_REACHED},
    )


async def test_stop_campaigns_with_budgets_overrun_if_nothing_to_charge(
    adv_store_notification, adv_store_client_mock
):
    orders_list = [
        {
            "order_id": 734582,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("0"),
            "billing_success": None,
            "campaigns": [
                {
                    # budget overrun
                    "campaign_id": 6528,
                    "budget": Decimal("5"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("20"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": None,
                    "last_paid_event_cost": None,
                    "paid_events_count": 10,
                    "paid_events_to_charge": 0,
                },
                {
                    # correct campaign,
                    # will be ignored because no limit reached
                    "campaign_id": 9786,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("20"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": None,
                    "last_paid_event_cost": None,
                    "paid_events_count": 0,
                    "paid_events_to_charge": 0,
                },
            ],
        }
    ]

    await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    adv_store_client_mock.stop_campaigns.assert_called_once_with(
        processed_at=dt(1566313386),
        campaigns_to_stop={
            6528: ReasonStopped.BUDGET_REACHED,
        },
    )


async def test_return_list_stopped_campaigns(adv_store_notification):
    orders_list = [
        {
            "order_id": 567382,
            "balance": Decimal("200"),
            "amount_to_bill": Decimal("18"),
            "billing_success": True,
            "campaigns": [
                {
                    # all limits ok, will be ignored
                    "campaign_id": 4242,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("1"),
                    "last_paid_event_cost": Decimal("1"),
                    "paid_events_count": 3,
                    "paid_events_to_charge": 3,
                }
            ],
        },
        {
            "order_id": 232365,
            "balance": Decimal("30"),
            "amount_to_bill": Decimal("20"),
            "billing_success": True,
            "campaigns": [
                {
                    # budget limit reached
                    "campaign_id": 8764,
                    "budget": Decimal("20"),
                    "daily_budget": Decimal("Infinity"),
                    "charged": Decimal("0"),
                    "daily_charged": Decimal("0"),
                    "paid_event_cost": Decimal("5"),
                    "last_paid_event_cost": Decimal("5"),
                    "paid_events_count": 10,
                    "paid_events_to_charge": 4,
                },
            ],
        },
    ]

    data = await adv_store_notification.run(
        {"orders": orders_list, "packet_end": dt(1566313386)}, None
    )

    assert data["stopped_campaigns"] == {
        8764: ReasonStopped.BUDGET_REACHED,
    }
