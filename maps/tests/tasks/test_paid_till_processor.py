from decimal import Decimal
from datetime import datetime, timedelta, timezone
from unittest.mock import call

from maps_adv.statistics.beekeeper.lib.tasks import paid_till_processor
from maps_adv.adv_store.api.schemas.enums import FixTimeIntervalEnum

import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.freeze_time("2022-01-01")
async def test_returns_early_if_no_unaccounted_campaigns(
    adv_store_client_mock, billing_client_mock, config
):
    now = datetime.now(timezone.utc)
    adv_store_client_mock.list_active_fix_campaigns.coro.return_value = [
        {"order_id": 1, "paid_till": now + timedelta(days=1)},
        {"order_id": 2, "paid_till": now + timedelta(days=2)},
        {"order_id": 3, "paid_till": now + timedelta(days=3)},
    ]
    await paid_till_processor(
        None,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        config=config,
    )
    adv_store_client_mock.list_active_fix_campaigns.assert_called_with(
        on_datetime=datetime.now(tz=timezone.utc)
    )
    billing_client_mock.fetch_orders_debits.assert_not_called()


@pytest.mark.freeze_time("2022-01-01")
async def test_return_early_if_the_experiment_is_no_enabled(
    adv_store_client_mock, billing_client_mock, config
):
    config["EXPERIMENTAL_CHARGE_FIX_CAMPAIGNS"] = False
    now = datetime.now(timezone.utc)
    adv_store_client_mock.list_active_fix_campaigns.coro.return_value = [
        {"order_id": 1, "paid_till": now + timedelta(days=1)},
        {"order_id": 2, "paid_till": now + timedelta(days=2)},
        {"order_id": 3, "paid_till": now + timedelta(days=3)},
    ]
    await paid_till_processor(
        None,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        config=config,
    )
    adv_store_client_mock.list_active_fix_campaigns.assert_not_called()
    adv_store_client_mock.update_paid_till.assert_not_called()
    billing_client_mock.fetch_orders_debits.assert_not_called()
    billing_client_mock.submit_orders_charges.assert_not_called()


@pytest.mark.freeze_time("2022-01-01")
async def test_charges_unpaid_campaigns(
    adv_store_client_mock, billing_client_mock, config
):
    now = datetime.now(timezone.utc)

    adv_store_client_mock.list_active_fix_campaigns.coro.return_value = [
        {
            "campaign_id": 1000,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.DAILY,
            "order_id": 1,
            "paid_till": now + timedelta(days=1),
            "cost": Decimal("100"),
        },
        {
            "campaign_id": 1001,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.DAILY,
            "order_id": 1,
            "paid_till": now + timedelta(days=7),
            "cost": Decimal("200"),
        },
        {
            "campaign_id": 1002,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.DAILY,
            "order_id": 2,
            "paid_till": now + timedelta(days=31),
            "cost": Decimal("300"),
        },
        {
            "campaign_id": 1003,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.WEEKLY,
            "order_id": 2,
            "paid_till": None,
            "cost": Decimal("400"),
        },
        {
            "campaign_id": 1004,
            "timezone": "Europe/Berlin",
            "time_interval": FixTimeIntervalEnum.MONTHLY,
            "order_id": 2,
            "paid_till": now - timedelta(days=1),
            "cost": Decimal("500"),
        },
        {
            "campaign_id": 1005,
            "timezone": "UTC",
            "time_interval": FixTimeIntervalEnum.DAILY,
            "order_id": 3,
            "paid_till": now - timedelta(days=31),
            "cost": Decimal("600"),
        },
    ]
    billing_client_mock.fetch_orders_debits.coro.return_value = {
        2: [],
        3: [],
    }
    billing_client_mock.submit_orders_charges.coro.return_value = (
        True,
        {2: True, 3: True},
    )

    await paid_till_processor(
        None,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        config=config,
    )
    adv_store_client_mock.list_active_fix_campaigns.assert_called_with(on_datetime=now)
    billing_client_mock.fetch_orders_debits.assert_called_with(
        {2, 3}, paid_from=now - timedelta(days=31)
    )
    billing_client_mock.submit_orders_charges.assert_has_calls(
        [
            call(charges={2: Decimal("400")}, bill_due_to=now),
            call(charges={2: Decimal("500")}, bill_due_to=now),
            call(charges={3: Decimal("600")}, bill_due_to=now),
        ]
    )
    adv_store_client_mock.update_paid_till.assert_has_calls(
        [
            call(
                1003,
                # Moscow timezone offset is +02:30 in the tesinng env
                datetime(2022, 1, 7, 21, 30, tzinfo=timezone.utc),
            ),
            call(
                1004,
                # Berlin timezone offset is +00:53 in the tesinng env
                datetime(2022, 1, 31, 23, 7, tzinfo=timezone.utc),
            ),
            call(
                1005,
                datetime(2022, 1, 2, tzinfo=timezone.utc),
            ),
        ]
    )


@pytest.mark.freeze_time("2022-01-01")
async def test_dosnt_charge_already_paid_campaigns(
    adv_store_client_mock, billing_client_mock, config
):
    now = datetime.now(timezone.utc)

    adv_store_client_mock.list_active_fix_campaigns.coro.return_value = [
        {
            "campaign_id": 1003,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.WEEKLY,
            "order_id": 2,
            "paid_till": None,
            "cost": Decimal("400"),
        },
        {
            "campaign_id": 1004,
            "timezone": "Europe/Berlin",
            "time_interval": FixTimeIntervalEnum.MONTHLY,
            "order_id": 2,
            "paid_till": now - timedelta(days=1),
            "cost": Decimal("500"),
        },
        {
            "campaign_id": 1005,
            "timezone": "UTC",
            "time_interval": FixTimeIntervalEnum.DAILY,
            "order_id": 3,
            "paid_till": now - timedelta(days=31),
            "cost": Decimal("600"),
        },
    ]
    billing_client_mock.fetch_orders_debits.coro.return_value = {
        2: [{"amount": Decimal("400"), "billed_at": now - timedelta(days=3)}],
        3: [{"amount": Decimal("600"), "billed_at": now - timedelta(hours=3)}],
    }
    billing_client_mock.submit_orders_charges.coro.return_value = (
        True,
        {2: True, 3: True},
    )

    await paid_till_processor(
        None,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        config=config,
    )
    adv_store_client_mock.list_active_fix_campaigns.assert_called_with(on_datetime=now)
    billing_client_mock.fetch_orders_debits.assert_called_with(
        {2, 3}, paid_from=now - timedelta(days=31)
    )
    billing_client_mock.submit_orders_charges.assert_has_calls(
        [
            call(charges={2: Decimal("500")}, bill_due_to=now),
        ]
    )
    adv_store_client_mock.update_paid_till.assert_has_calls(
        [
            call(
                1003,
                # Moscow timezone offset is +02:30 in the tesinng env
                datetime(2022, 1, 4, 21, 30, tzinfo=timezone.utc),
            ),
            call(
                1004,
                # Berlin timezone offset is +00:53 in the tesinng env
                datetime(2022, 1, 31, 23, 7, tzinfo=timezone.utc),
            ),
            call(
                1005,
                datetime(2022, 1, 2, tzinfo=timezone.utc),
            ),
        ]
    )


@pytest.mark.freeze_time("2022-01-01")
async def test_dosnt_charge_accounted_campaigns(
    adv_store_client_mock, billing_client_mock, config
):
    now = datetime.now(timezone.utc)

    adv_store_client_mock.list_active_fix_campaigns.coro.return_value = [
        {
            "campaign_id": 1001,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.WEEKLY,
            "order_id": 1,
            "paid_till": now + timedelta(days=5),
            "cost": Decimal("1000"),
        },
        {
            "campaign_id": 1002,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.WEEKLY,
            "order_id": 1,
            "paid_till": None,
            "cost": Decimal("1000"),
        },
        {
            "campaign_id": 1003,
            "timezone": "Europe/Moscow",
            "time_interval": FixTimeIntervalEnum.WEEKLY,
            "order_id": 1,
            "paid_till": None,
            "cost": Decimal("1000"),
        },
    ]
    billing_client_mock.fetch_orders_debits.coro.return_value = {
        1: [
            {"amount": Decimal("1000"), "billed_at": now - timedelta(days=2)},
            {"amount": Decimal("1000"), "billed_at": now - timedelta(days=2)},
        ],
    }
    billing_client_mock.submit_orders_charges.coro.return_value = (True, {1: True})

    await paid_till_processor(
        None,
        adv_store_client=adv_store_client_mock,
        billing_proxy_client=billing_client_mock,
        config=config,
    )
    adv_store_client_mock.list_active_fix_campaigns.assert_called_with(on_datetime=now)
    billing_client_mock.fetch_orders_debits.assert_called_with(
        {1}, paid_from=now - timedelta(days=31)
    )
    billing_client_mock.submit_orders_charges.assert_has_calls(
        [
            call(charges={1: Decimal("1000")}, bill_due_to=now),
        ]
    )
    adv_store_client_mock.update_paid_till.assert_has_calls(
        [
            call(
                1002,
                # Moscow timezone offset is +02:30 in the tesinng env
                datetime(2022, 1, 5, 21, 30, tzinfo=timezone.utc),
            ),
            call(
                1003,
                datetime(2022, 1, 7, 21, 30, tzinfo=timezone.utc),
            ),
        ]
    )
