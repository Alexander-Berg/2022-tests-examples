from datetime import datetime
from decimal import Decimal

import pytest

from maps_adv.adv_store.lib.data_managers.for_charger import campaign_list_for_charger
from maps_adv.adv_store.v2.lib.db import tables
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
async def status_history_setup(db, faker, factory):
    cpm_data_one = {
        "cost": Decimal("55.66"),
        "budget": Decimal("5566"),
        "daily_budget": Decimal("556.6"),
    }
    cpm_data_two = {
        "cost": Decimal("88.77"),
        "budget": Decimal("8877"),
        "daily_budget": Decimal("887.7"),
    }
    cpm_id_one = (await factory.create_cpm(**cpm_data_one))["id"]
    cpm_id_two = (await factory.create_cpm(**cpm_data_two))["id"]
    billing_id_one = (await factory.create_billing(cpm_id=cpm_id_one))["id"]
    billing_id_two = (await factory.create_billing(cpm_id=cpm_id_two))["id"]
    campaign_id_one = (
        await factory.create_campaign(
            billing_id=billing_id_one,
            order_id=11,
            timezone="Europe/Minsk",
            start_datetime=datetime.fromtimestamp(0),
            end_datetime=datetime.fromtimestamp(99999999),
        )
    )["id"]
    campaign_id_two = (
        await factory.create_campaign(
            billing_id=billing_id_two,
            order_id=None,
            timezone="Europe/Moscow",
            start_datetime=datetime.fromtimestamp(0),
            end_datetime=datetime.fromtimestamp(99999999),
        )
    )["id"]

    statuses = [
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.DRAFT,
            "changed_datetime": datetime.fromtimestamp(1_000),
        },
        {
            "campaign_id": campaign_id_two,
            "status": CampaignStatusEnum.DRAFT,
            "changed_datetime": datetime.fromtimestamp(2_000),
        },
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.REVIEW,
            "changed_datetime": datetime.fromtimestamp(3_000),
        },
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.ACTIVE,
            "changed_datetime": datetime.fromtimestamp(4_000),
        },
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.ACTIVE,
            "changed_datetime": datetime.fromtimestamp(5_000),
        },
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.PAUSED,
            "changed_datetime": datetime.fromtimestamp(6_000),
        },
        {
            "campaign_id": campaign_id_one,
            "status": CampaignStatusEnum.ACTIVE,
            "changed_datetime": datetime.fromtimestamp(7_000),
        },
        {
            "campaign_id": campaign_id_two,
            "status": CampaignStatusEnum.ACTIVE,
            "changed_datetime": datetime.fromtimestamp(8_000),
        },
        {
            "campaign_id": campaign_id_two,
            "status": CampaignStatusEnum.PAUSED,
            "changed_datetime": datetime.fromtimestamp(9_000),
        },
    ]
    faker.random.shuffle(statuses)

    for status in statuses:
        status["author_id"] = 1234

    await db.rw.execute_many(tables.status_history.insert(), statuses)

    yield campaign_id_one, campaign_id_two


async def test_no_campaigns_if_active_at_too_early(status_history_setup):
    got = await campaign_list_for_charger(datetime.fromtimestamp(50))

    assert got == []


async def test_campaign_returned_if_its_last_state_is_active(status_history_setup):
    got = await campaign_list_for_charger(datetime.fromtimestamp(100_000))

    assert got == [
        {
            "campaign_id": status_history_setup[0],
            "order_id": 11,
            "cost": Decimal("55.66"),
            "budget": Decimal("5566"),
            "daily_budget": Decimal("556.6"),
            "timezone": "Europe/Minsk",
        }
    ]


async def test_campaign_returned_when_it_was_active_at_some_period(
    status_history_setup
):
    got = await campaign_list_for_charger(datetime.fromtimestamp(5_500))

    assert got == [
        {
            "campaign_id": status_history_setup[0],
            "order_id": 11,
            "cost": Decimal("55.66"),
            "budget": Decimal("5566"),
            "daily_budget": Decimal("556.6"),
            "timezone": "Europe/Minsk",
        }
    ]


async def test_two_active_campaigns_returned(status_history_setup):
    got = await campaign_list_for_charger(datetime.fromtimestamp(8_500))

    expected = [
        {
            "campaign_id": status_history_setup[0],
            "order_id": 11,
            "cost": Decimal("55.66"),
            "budget": Decimal("5566"),
            "daily_budget": Decimal("556.6"),
            "timezone": "Europe/Minsk",
        },
        {
            "campaign_id": status_history_setup[1],
            "order_id": None,
            "cost": Decimal("88.77"),
            "budget": Decimal("8877"),
            "daily_budget": Decimal("887.7"),
            "timezone": "Europe/Moscow",
        },
    ]
    assert got == expected or got == expected[::-1]


async def test_returns_empty_if_all_campaigns_are_inactive(status_history_setup):
    got = await campaign_list_for_charger(datetime.fromtimestamp(3_500))

    assert got == []


async def test_not_crashes_on_empty_db(db):
    got = await campaign_list_for_charger(datetime.fromtimestamp(3_500))

    assert got == []


async def test_fix_campaigns_not_returned(db, factory):
    fix = await factory.create_fix()
    billing = await factory.create_billing(fix_id=fix["id"])
    campaign = await factory.create_campaign(billing_id=billing["id"])
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=datetime.fromtimestamp(0),
    )

    got = await campaign_list_for_charger(datetime.fromtimestamp(1))

    assert got == []


async def test_cpa_campaigns_not_returned(db, factory):
    cpa = await factory.create_cpa()
    billing = await factory.create_billing(cpa_id=cpa["id"])
    campaign = await factory.create_campaign(billing_id=billing["id"])
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=datetime.fromtimestamp(0),
    )

    got = await campaign_list_for_charger(datetime.fromtimestamp(1))

    assert got == []


@pytest.mark.parametrize(
    ("start_datetime", "end_datetime"),
    [
        (datetime.fromtimestamp(100), datetime.fromtimestamp(200)),
        (datetime.fromtimestamp(600), datetime.fromtimestamp(700)),
    ],
)
async def test_campaign_not_returned_if_not_active_by_dates(
    db, factory, start_datetime, end_datetime
):
    cpm = await factory.create_cpm()
    billing = await factory.create_billing(cpm_id=cpm["id"])
    campaign = await factory.create_campaign(
        billing_id=billing["id"],
        start_datetime=start_datetime,
        end_datetime=end_datetime,
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=datetime.fromtimestamp(0),
    )

    got = await campaign_list_for_charger(datetime.fromtimestamp(500))

    assert got == []
