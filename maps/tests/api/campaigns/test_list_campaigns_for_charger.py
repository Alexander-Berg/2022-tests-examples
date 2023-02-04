from decimal import Decimal
from operator import attrgetter

import pytest

from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForCharger,
    CampaignForChargerList,
    Money,
)
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    PublicationEnvEnum,
)
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio]

_cpm = {
    "cost": Decimal("50"),
    "budget": Decimal("66000"),
    "daily_budget": Decimal("5000"),
    "auto_daily_budget": False,
}

url = "/campaigns/charger/cpm/?active_at={}"


async def test_campaign_returned_if_active(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=campaign_id,
                order_id=10,
                cost=Money(value=500000),
                budget=Money(value=660000000),
                daily_budget=Money(value=50000000),
                timezone="UTC",
            )
        ]
    )


@pytest.mark.parametrize(
    "current_status",
    [
        CampaignStatusEnum.DRAFT,
        CampaignStatusEnum.PAUSED,
        CampaignStatusEnum.REVIEW,
        CampaignStatusEnum.REJECTED,
        CampaignStatusEnum.DONE,
        CampaignStatusEnum.ARCHIVED,
    ],
)
async def test_campaign_not_returned_if_never_was_active(api, factory, current_status):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=current_status, changed_datetime=dt("2019-10-10 00:00:00")
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList()


async def test_campaign_not_returned_if_not_active_yet(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-09 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList()


async def test_campaign_not_returned_if_not_active_already(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-09 00:00:00"),
    )
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.DRAFT,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList()


@pytest.mark.parametrize(
    ("start_datetime", "end_datetime"),
    [
        (dt("2019-09-01 00:00:00"), dt("2019-09-30 00:00:00")),
        (dt("2019-11-01 00:00:00"), dt("2019-11-30 00:00:00")),
    ],
)
async def test_campaign_not_returned_if_not_active_by_dates(
    api, factory, start_datetime, end_datetime
):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=start_datetime,
            end_datetime=end_datetime,
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-09 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-10 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList()


async def test_combined_campaigns(api, factory):
    campaign_active_1_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_active_1_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-09 00:00:00"),
    )

    campaign_active_2_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_active_2_id,
        status=CampaignStatusEnum.REVIEW,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )
    await factory.set_status(
        campaign_active_2_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-11 00:00:00"),
    )

    # Inactive by status
    await factory.create_campaign(
        cpm=_cpm,
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=dt("2020-01-01 00:00:00"),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )
    # Inactive by dates
    await factory.create_campaign(
        cpm=_cpm,
        start_datetime=dt("2019-09-01 00:00:00"),
        end_datetime=dt("2019-09-30 00:00:00"),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    result_ids = sorted(map(attrgetter("campaign_id"), got.campaigns))
    expected_ids = sorted([campaign_active_1_id, campaign_active_2_id])

    assert result_ids == expected_ids


@pytest.mark.parametrize(
    "billing_kwargs",
    [
        {
            "cpa": {
                "cost": Decimal("55.66"),
                "budget": Decimal("66000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        {"fix": {"cost": Decimal("55.66"), "time_interval": FixTimeIntervalEnum.DAILY}},
    ],
)
async def test_not_cpm_campaigns_not_returned(api, factory, billing_kwargs):
    await factory.create_campaign(
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=dt("2020-01-01 00:00:00"),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        **billing_kwargs,
    )

    got = await api.get(
        url.format(int(dt("2019-10-10 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got == CampaignForChargerList()


@pytest.mark.parametrize(
    "publication_envs",
    [
        [PublicationEnvEnum.PRODUCTION],
        [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
    ],
)
async def test_production_campaign_returned(api, factory, publication_envs):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=publication_envs,
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert len(got.campaigns) == 1


async def test_datatesting_campaign_not_returned(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.DATA_TESTING],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-10-11 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert len(got.campaigns) == 0


async def test_returns_error_if_active_at_not_provided(api, factory):
    await factory.create_campaign(
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=dt("2020-01-01 00:00:00"),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )

    await api.get("/campaigns/charger/cpm/", expected_status=400)


@pytest.mark.parametrize("value", ["", "123.5", "qwer"])
async def test_returns_error_if_active_at_is_invalid(api, factory, value):
    await factory.create_campaign(
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=dt("2020-01-01 00:00:00"),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )

    await api.get(url.format(value), expected_status=400)


@pytest.mark.parametrize(
    ("discounts", "expected_cpm"),
    [
        ([], Money(value=500000)),
        (
            [
                {
                    "start_datetime": dt("2019-03-01 00:00:00"),
                    "end_datetime": dt("2019-04-01 00:00:00"),
                    "cost_multiplier": Decimal("0.8"),
                }
            ],
            Money(value=400000),
        ),
        (
            [
                {
                    "start_datetime": dt("2019-01-01 00:00:00"),
                    "end_datetime": dt("2019-02-01 00:00:00"),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
            Money(value=500000),
        ),
        (
            [
                {
                    "start_datetime": dt("2019-01-01 00:00:00"),
                    "end_datetime": dt("2019-02-01 00:00:00"),
                    "cost_multiplier": Decimal("0.2"),
                },
                {
                    "start_datetime": dt("2019-03-01 00:00:00"),
                    "end_datetime": dt("2019-04-01 00:00:00"),
                    "cost_multiplier": Decimal("0.5"),
                },
                {
                    "start_datetime": dt("2019-05-01 00:00:00"),
                    "end_datetime": dt("2019-06-01 00:00:00"),
                    "cost_multiplier": Decimal("0.8"),
                },
            ],
            Money(value=250000),
        ),
    ],
)
async def test_campaign_discounts_respected(api, factory, discounts, expected_cpm):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2018-01-01 00:00:00"),
            end_datetime=dt("2020-01-01 00:00:00"),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
            discounts=discounts,
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2018-01-01 00:00:00"),
    )

    got = await api.get(
        url.format(int(dt("2019-03-03 00:00:00").timestamp())),
        decode_as=CampaignForChargerList,
        expected_status=200,
    )

    assert got.campaigns[0].cost == expected_cpm
