from decimal import Decimal

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    PublicationEnvEnum,
)
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_campaigns_for_all_requested_orders(factory, campaigns_dm):
    campaign1_id = (await factory.create_campaign(order_id=42))["id"]
    campaign2_id = (await factory.create_campaign(order_id=42))["id"]
    campaign3_id = (await factory.create_campaign(order_id=22))["id"]
    campaign4_id = (await factory.create_campaign(manul_order_id=52))["id"]
    # noise
    await factory.create_campaign(manul_order_id=777)
    await factory.create_campaign(order_id=888)

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[42, 22, 15], manul_order_ids=[52, 15]
    )
    assert sorted([el["id"] for el in got]) == sorted(
        [campaign1_id, campaign2_id, campaign3_id, campaign4_id]
    )


@pytest.mark.parametrize(
    "billing_extra, expected_budget_value",
    (
        (
            {
                "cpm": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            Decimal("500"),
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            Decimal("300"),
        ),
        (
            {
                "fix": {
                    "time_interval": FixTimeIntervalEnum.DAILY,
                    "cost": Decimal("10.1234"),
                }
            },
            None,
        ),
    ),
)
async def test_returns_budgets_for_all_campaign_budget_types(
    billing_extra, expected_budget_value, factory, campaigns_dm
):
    await factory.create_campaign(order_id=42, **billing_extra)

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[42], manul_order_ids=[]
    )

    assert got[0]["budget"] == expected_budget_value


@pytest.mark.real_db
@pytest.mark.parametrize("orders_extra", [{"order_id": 4200}, {"manul_order_id": 5200}])
async def test_returns_last_status_for_campaigns_in_order(
    orders_extra, factory, campaigns_dm
):
    campaign_id1 = (await factory.create_campaign(**orders_extra))["id"]
    await factory.set_status(campaign_id=campaign_id1, status=CampaignStatusEnum.ACTIVE)
    await factory.set_status(
        campaign_id=campaign_id1, status=CampaignStatusEnum.ARCHIVED
    )

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[4200], manul_order_ids=[5200]
    )

    assert got[0]["status"] == CampaignStatusEnum.ARCHIVED


@pytest.mark.real_db
@pytest.mark.parametrize("orders_extra", [{"order_id": 4200}, {"manul_order_id": 5200}])
@pytest.mark.parametrize(
    ["reason_stopped", "expected_status"],
    [
        ("", CampaignStatusEnum.PAUSED),
    ],
)
async def test_returns_campaign_as_active_if_they_have_technical_pause_status(
    orders_extra,
    reason_stopped: str,
    expected_status: CampaignStatusEnum,
    factory,
    campaigns_dm,
):
    campaign_id1 = (await factory.create_campaign(**orders_extra))["id"]
    await factory.set_status(campaign_id=campaign_id1, status=CampaignStatusEnum.DRAFT)
    await factory.set_status(
        campaign_id=campaign_id1,
        status=CampaignStatusEnum.PAUSED,
        metadata={"reason_stopped": reason_stopped},
    )

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[4200], manul_order_ids=[5200]
    )

    assert got[0]["status"] == expected_status


@pytest.mark.parametrize("order_extra", [{"order_id": 42}, {"manul_order_id": 24}])
async def test_does_not_return_campaigns_of_another_order_type(
    order_extra, factory, campaigns_dm
):
    await factory.create_campaign(**order_extra)

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[24], manul_order_ids=[42]
    )

    assert got == []


@pytest.mark.parametrize("order_extra", [{"order_id": 10}, {"manul_order_id": 10}])
async def test_returns_nothing_if_nothing_in_listed_orders(
    order_extra, factory, campaigns_dm
):
    await factory.create_campaign(**order_extra)

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[42], manul_order_ids=[24]
    )

    assert got == []


@pytest.mark.parametrize(
    "billing_extra, expected_budget_value",
    (
        (
            {
                "cpm": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            Decimal("500"),
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                }
            },
            Decimal("300"),
        ),
        (
            {
                "fix": {
                    "time_interval": FixTimeIntervalEnum.DAILY,
                    "cost": Decimal("10.1234"),
                }
            },
            None,
        ),
    ),
)
async def test_returns_campaigns_data(
    billing_extra, expected_budget_value, factory, campaigns_dm
):
    campaign_id = (
        await factory.create_campaign(
            order_id=42,
            publication_envs=[
                PublicationEnvEnum.DATA_TESTING,
                PublicationEnvEnum.PRODUCTION,
            ],
            **billing_extra,
        )
    )["id"]

    got = await campaigns_dm.list_campaigns_by_orders(
        order_ids=[42], manul_order_ids=[]
    )

    assert got == [
        {
            "id": campaign_id,
            "name": "campaign0",
            "start_datetime": dt("2019-01-01 00:00:00"),
            "end_datetime": dt("2019-02-01 00:00:00"),
            "timezone": "UTC",
            "publication_envs": [
                PublicationEnvEnum.DATA_TESTING,
                PublicationEnvEnum.PRODUCTION,
            ],
            "status": CampaignStatusEnum.DRAFT,
            "budget": expected_budget_value,
            "datatesting_expires_at": None,
        }
    ]
