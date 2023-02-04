from decimal import Decimal

import pytest

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignModerationInfo,
    CampaignModerationInfoList,
)
from maps_adv.adv_store.api.proto.campaign_pb2 import Platform
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum, FixTimeIntervalEnum
from maps_adv.adv_store.v2.tests import dt
from maps_adv.common.proto.campaign_pb2 import CampaignType

pytestmark = [pytest.mark.asyncio]

url = "/moderation/list/"


async def test_returns_nothing_if_nothing_exists(api):
    got = await api.get(url, decode_as=CampaignModerationInfoList, expected_status=200)

    assert got == CampaignModerationInfoList(campaigns=[])


@pytest.mark.parametrize(
    "status", [el for el in list(CampaignStatusEnum) if el != CampaignStatusEnum.REVIEW]
)
async def test_returns_nothing_if_no_campaigns_in_moderation(status, factory, api):
    await factory.create_campaign(status=status)

    got = await api.get(url, decode_as=CampaignModerationInfoList, expected_status=200)

    assert got == CampaignModerationInfoList(campaigns=[])


@pytest.mark.real_db
async def test_returns_campaigns_in_moderation(factory, api):
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

    got = await api.get(url, decode_as=CampaignModerationInfoList, expected_status=200)

    assert [el.id for el in got.campaigns] == [campaign_id3, campaign_id1, campaign_id2]


@pytest.mark.parametrize(
    "status", [el for el in list(CampaignStatusEnum) if el != CampaignStatusEnum.REVIEW]
)
@pytest.mark.real_db
async def test_does_not_return_campaign_not_in_moderation_anymore(status, factory, api):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]
    await factory.set_status(campaign_id, status=status)

    got = await api.get(url, decode_as=CampaignModerationInfoList, expected_status=200)

    assert got == CampaignModerationInfoList(campaigns=[])


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
            {"order_id": 10, "budget": Money(value=5000000)},
        ],
        [
            {
                "manul_order_id": 20,
                "cpa": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                },
            },
            {"manul_order_id": 20, "budget": Money(value=5000000)},
        ],
        [
            {
                "order_id": 10,
                "fix": {
                    "time_interval": FixTimeIntervalEnum.DAILY,
                    "cost": Decimal("10.1234"),
                },
            },
            {"order_id": 10},
        ],
    ),
)
@pytest.mark.real_db
async def test_returns_campaigns_details(extra_input, extra_output, factory, api):
    campaign_id = (
        await factory.create_campaign(status=CampaignStatusEnum.REVIEW, **extra_input)
    )["id"]

    got = await api.get(url, decode_as=CampaignModerationInfoList, expected_status=200)

    assert got == CampaignModerationInfoList(
        campaigns=[
            CampaignModerationInfo(
                id=campaign_id,
                name="campaign0",
                campaign_type=CampaignType.Enum.Value("ZERO_SPEED_BANNER"),
                start_datetime=dt("2019-01-01 00:00:00", as_proto=True),
                end_datetime=dt("2019-02-01 00:00:00", as_proto=True),
                timezone="UTC",
                request_datetime=got.campaigns[0].request_datetime,
                platforms=[Platform.Enum.Value("NAVI")],
                **extra_output,
            )
        ]
    )
