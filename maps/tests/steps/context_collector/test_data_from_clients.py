from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.common.helpers import Any, dt
from maps_adv.statistics.beekeeper.lib.steps.base import FreeEventProcessingMode
from maps_adv.statistics.beekeeper.lib.steps.context_collector import (
    UnsupportedPrecision,
)

pytestmark = [pytest.mark.asyncio]


async def test_uses_adv_store_client(context_collector_step, adv_store_client_mock):
    await context_collector_step.run()

    adv_store_client_mock.list_active_cpm_campaigns.assert_called_with(
        on_datetime=dt("2000-02-02 01:20:00")
    )


async def test_uses_billing_proxy_client(
    context_collector_step, adv_store_client_mock, billing_client_mock
):
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value = [
        {
            "campaign_id": 11,
            "order_id": 101,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
        },
        {
            "campaign_id": 12,
            "order_id": 102,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
        },
        {
            "campaign_id": 13,
            "order_id": 101,
            "cost": Decimal("1.3"),
            "budget": Decimal("330"),
            "daily_budget": Decimal("23"),
            "timezone": "Asia/Novosibirsk",
        },
        {
            "campaign_id": 14,
            "order_id": None,
            "cost": Decimal("1.4"),
            "budget": Decimal("340"),
            "daily_budget": Decimal("24"),
            "timezone": "UTC",
        },
    ]
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value = [
        {
            "campaign_id": 21,
            "order_id": 201,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
        {
            "campaign_id": 22,
            "order_id": 202,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
    ]
    billing_client_mock.fetch_orders_balance.coro.return_value = {
        101: Decimal("100"),
        102: Decimal("200"),
        201: Decimal("100"),
        202: Decimal("200"),
    }
    billing_client_mock.fetch_orders_discounts.coro.return_value = {
        101: Decimal("1.0"),
        102: Decimal("1.0"),
        201: Decimal("1.0"),
        202: Decimal("1.0"),
    }

    await context_collector_step.run()

    assert billing_client_mock.fetch_orders_balance.called
    call_args = billing_client_mock.fetch_orders_balance.call_args[0]
    assert sorted(call_args) == [101, 102, 201, 202]


async def test_returns_valid_order_data(
    context_collector_step, adv_store_client_mock, billing_client_mock
):
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value = [
        {
            "campaign_id": 11,
            "order_id": 101,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
        },
        {
            "campaign_id": 12,
            "order_id": 102,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
        },
        {
            "campaign_id": 13,
            "order_id": 101,
            "cost": Decimal("1.3"),
            "budget": Decimal("330"),
            "daily_budget": Decimal("23"),
            "timezone": "Europe/Moscow",
        },
    ]
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value = [
        {
            "campaign_id": 21,
            "order_id": 201,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
        {
            "campaign_id": 22,
            "order_id": 202,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
    ]
    billing_client_mock.fetch_orders_balance.coro.return_value = {
        101: Decimal("100"),
        102: Decimal("0"),
        201: Decimal("100"),
        202: Decimal("0"),
    }
    billing_client_mock.fetch_orders_discounts.coro.return_value = {
        101: Decimal("1.0"),
        102: Decimal("1.0"),
        201: Decimal("1.0"),
        202: Decimal("1.0"),
    }

    result = await context_collector_step.run()

    orders_data = sorted(result["orders"], key=itemgetter("order_id"))
    assert orders_data == [
        {
            "order_id": 101,
            "balance": Decimal("100"),
            "campaigns": Any(list).of_len(2),
        },
        {
            "order_id": 102,
            "balance": Decimal("0"),
            "campaigns": Any(list).of_len(1),
        },
        {
            "order_id": 201,
            "balance": Decimal("100"),
            "campaigns": Any(list).of_len(1),
        },
        {
            "order_id": 202,
            "balance": Decimal("0"),
            "campaigns": Any(list).of_len(1),
        },
    ]


async def test_adds_none_order_for_campaigns_without_order(
    context_collector_step, adv_store_client_mock, billing_client_mock
):
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value = [
        {
            "campaign_id": 11,
            "order_id": None,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
        },
        {
            "campaign_id": 12,
            "order_id": None,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
        },
    ]
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value = [
        {
            "campaign_id": 21,
            "order_id": None,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
        {
            "campaign_id": 22,
            "order_id": None,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
    ]

    billing_client_mock.fetch_orders_balance.coro.return_value = {}

    result = await context_collector_step.run()

    orders_data = result["orders"]
    assert orders_data == [
        {
            "order_id": None,
            "balance": Decimal("Infinity"),
            "campaigns": Any(list).of_len(4),
        }
    ]


async def test_not_returns_campaigns_not_returned_from_adv_store_client(
    factory, context_collector_step
):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 500,
                "receive_timestamp": dt("2000-02-02 01:11:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 501,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    result = await context_collector_step.run()

    campaign_ids = list(
        campaign["campaign_id"]
        for order in result["orders"]
        for campaign in order["campaigns"]
    )
    assert campaign_ids == [11, 22]


async def test_returns_valid_adv_store_campaign_data(
    context_collector_step, adv_store_client_mock, billing_client_mock
):
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value = [
        {
            "campaign_id": 11,
            "order_id": 101,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
        },
        {
            "campaign_id": 12,
            "order_id": 101,
            "cost": Decimal("1.2"),
            "budget": Decimal("32"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
        },
    ]
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value = [
        {
            "campaign_id": 21,
            "order_id": 201,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
        {
            "campaign_id": 22,
            "order_id": 201,
            "cost": Decimal("1.2"),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "timezone": "Europe/Moscow",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        },
    ]
    billing_client_mock.fetch_orders_balance.coro.return_value = {
        101: Decimal("100"),
        201: Decimal("100"),
    }

    billing_client_mock.fetch_orders_discounts.coro.return_value = {
        101: Decimal("1.0"),
        201: Decimal("0.5"),
    }

    result = await context_collector_step.run()

    campaigns_data = sorted(
        result["orders"][0]["campaigns"], key=itemgetter("campaign_id")
    )
    campaigns_data.extend(
        sorted(result["orders"][1]["campaigns"], key=itemgetter("campaign_id"))
    )

    assert len(campaigns_data) == 4
    assert campaigns_data == [
        {
            "campaign_id": 11,
            "billing_type": "cpm",
            "tz_name": "UTC",
            "paid_event_cost": Decimal("0.0011"),
            "paid_events_names": ["BILLBOARD_SHOW"],
            "paid_events_count": Any(int),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "charged": Any(Decimal),
            "daily_charged": Any(Decimal),
            "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,
        },
        {
            "campaign_id": 12,
            "billing_type": "cpm",
            "tz_name": "Europe/Moscow",
            "paid_event_cost": Decimal("0.0012"),
            "paid_events_names": ["BILLBOARD_SHOW"],
            "paid_events_count": Any(int),
            "budget": Decimal("32"),
            "daily_budget": Decimal("22"),
            "charged": Any(Decimal),
            "daily_charged": Any(Decimal),
            "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,
        },
        {
            "campaign_id": 21,
            "billing_type": "cpa",
            "tz_name": "UTC",
            "paid_event_cost": Decimal("0.55"),
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
            "paid_events_count": Any(int),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "charged": Any(Decimal),
            "daily_charged": Any(Decimal),
            "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,
        },
        {
            "campaign_id": 22,
            "billing_type": "cpa",
            "tz_name": "Europe/Moscow",
            "paid_event_cost": Decimal("0.6"),
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
            "paid_events_count": Any(int),
            "budget": Decimal("320"),
            "daily_budget": Decimal("22"),
            "charged": Any(Decimal),
            "daily_charged": Any(Decimal),
            "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,
        },
    ]


@pytest.mark.parametrize("order_id", [101, None])
@pytest.mark.parametrize("field", ["daily_budget", "budget"])
async def test_returns_null_budgets_as_infinity(
    order_id, field, context_collector_step, adv_store_client_mock
):
    campaign = adv_store_client_mock.list_active_cpm_campaigns.coro.return_value[0]
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value = []
    campaign.update({field: None, "order_id": order_id})

    result = await context_collector_step.run()

    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data[field] == Decimal("Infinity")


@pytest.mark.parametrize("order_id", [202, None])
@pytest.mark.parametrize("field", ["daily_budget", "budget"])
async def test_returns_null_budgets_as_infinity_cpa(
    order_id, field, context_collector_step, adv_store_client_mock
):
    campaign = adv_store_client_mock.list_active_cpa_campaigns.coro.return_value[0]
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value = []
    campaign.update({field: None, "order_id": order_id})

    result = await context_collector_step.run()

    campaign_data = result["orders"][0]["campaigns"][0]
    assert campaign_data[field] == Decimal("Infinity")


@pytest.mark.parametrize(
    "cost", [Decimal("1"), Decimal("0.001"), Decimal("0.003"), Decimal("0.1100000000")]
)
async def test_not_raises_for_maximum_supported_precision(
    context_collector_step, adv_store_client_mock, cost
):
    adv_store_client_mock.list_active_cpm_campaigns.coro.return_value[0]["cost"] = cost
    adv_store_client_mock.list_active_cpa_campaigns.coro.return_value[0]["cost"] = cost
    try:
        await context_collector_step.run()
    except UnsupportedPrecision:
        pytest.fail("Should not raise UnsupportedPrecision")


@pytest.mark.parametrize("order_id", [101, None])
@pytest.mark.parametrize(
    "cost",
    [Decimal("0.0003"), Decimal("0.0001"), Decimal("0.0011"), Decimal("0.00011")],
)
async def test_raises_for_unsupported_precision(
    order_id, cost, context_collector_step, adv_store_client_mock
):
    campaign = adv_store_client_mock.list_active_cpm_campaigns.coro.return_value[0]
    campaign.update({"cost": cost, "order_id": order_id})

    with pytest.raises(UnsupportedPrecision):
        await context_collector_step.run()


@pytest.mark.parametrize("order_id", [202, None])
@pytest.mark.parametrize(
    "cost",
    [
        Decimal("0.0000003"),
        Decimal("0.0000001"),
        Decimal("0.0000011"),
        Decimal("0.00000011"),
    ],
)
async def test_raises_for_unsupported_precision_cpa(
    order_id, cost, context_collector_step, adv_store_client_mock
):
    campaign = adv_store_client_mock.list_active_cpa_campaigns.coro.return_value[0]
    campaign.update({"cost": cost, "order_id": order_id})

    with pytest.raises(UnsupportedPrecision):
        await context_collector_step.run()
