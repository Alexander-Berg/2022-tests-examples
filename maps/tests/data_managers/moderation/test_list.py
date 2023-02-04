from decimal import Decimal

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    PlatformEnum,
)
from maps_adv.adv_store.v2.tests import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_nothing_exists(moderation_dm):
    got = await moderation_dm.list_campaigns()

    assert got == []


@pytest.mark.parametrize(
    "status", [el for el in list(CampaignStatusEnum) if el != CampaignStatusEnum.REVIEW]
)
async def test_returns_nothing_if_no_campaigns_in_moderation(
    status, factory, moderation_dm
):
    await factory.create_campaign(status=status)

    got = await moderation_dm.list_campaigns()

    assert got == []


@pytest.mark.real_db
async def test_returns_campaigns_in_moderation(factory, moderation_dm):
    # noise
    await factory.create_campaign(status=CampaignStatusEnum.REJECTED)

    # by last review creation time: 3 < 1 < 2
    campaign_id1 = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]
    await factory.set_status(
        campaign_id=campaign_id1,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-01-01 00:00:00"),
    )
    await factory.set_status(
        campaign_id=campaign_id1,
        status=CampaignStatusEnum.REVIEW,
        changed_datetime=dt("2019-01-03 00:00:00"),
    )

    campaign_id2 = (await factory.create_campaign())["id"]
    await factory.set_status(
        campaign_id=campaign_id2,
        status=CampaignStatusEnum.REVIEW,
        changed_datetime=dt("2019-01-02 00:00:00"),
    )

    campaign_id3 = (await factory.create_campaign())["id"]
    await factory.set_status(
        campaign_id=campaign_id3,
        status=CampaignStatusEnum.REVIEW,
        changed_datetime=dt("2019-01-04 00:00:00"),
    )

    got = await moderation_dm.list_campaigns()

    assert [el["id"] for el in got] == [campaign_id3, campaign_id1, campaign_id2]


@pytest.mark.parametrize(
    "status", [el for el in list(CampaignStatusEnum) if el != CampaignStatusEnum.REVIEW]
)
@pytest.mark.real_db
async def test_does_not_return_campaign_not_in_moderation_anymore(
    status, factory, moderation_dm
):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]
    await factory.set_status(campaign_id, status=status)

    got = await moderation_dm.list_campaigns()

    assert got == []


@pytest.mark.parametrize(
    "extra_input, extra_output",
    (
        [
            {
                "order_id": 10,
                "cpm": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                },
            },
            {"order_id": 10, "budget": Decimal("500.0000")},
        ],
        [
            {
                "manul_order_id": 20,
                "cpa": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": True,
                },
            },
            {"manul_order_id": 20, "budget": Decimal("500.0000")},
        ],
        [
            {
                "order_id": 10,
                "fix": {
                    "time_interval": FixTimeIntervalEnum.DAILY,
                    "cost": Decimal("10.1234"),
                },
            },
            {"order_id": 10, "budget": None},
        ],
    ),
)
@pytest.mark.real_db
async def test_returns_campaigns_details(
    extra_input, extra_output, factory, moderation_dm
):
    campaign_id = (
        await factory.create_campaign(status=CampaignStatusEnum.REVIEW, **extra_input)
    )["id"]

    got = await moderation_dm.list_campaigns()

    assert got == [
        dict(
            id=campaign_id,
            name="campaign0",
            campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2019-02-01 00:00:00"),
            timezone="UTC",
            request_datetime=got[0]["request_datetime"],
            platforms=[PlatformEnum.NAVI],
            **extra_output,
        )
    ]
