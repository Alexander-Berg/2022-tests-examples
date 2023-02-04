from typing import Callable

import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.pin_data import pin_data_transform

pytestmark = [pytest.mark.asyncio]

SUPPORTED_PLATFORMS = [PlatformEnum.METRO]
UNSUPPORTED_PLATFORMS = list(set(PlatformEnum) - set(SUPPORTED_PLATFORMS))


@pytest.mark.parametrize(
    ["is_ads", "limits", "expected_fields", "expected_limits"],
    [
        (
            True,
            dict(
                user_daily_display_limit=3,
                display_probability="0.123",
                display_chance=645,
            ),
            dict(limitImpressionsPerDay=3, isAds="true", chance=645),
            dict(displayProbability="0.123", impressions=dict(dailyPerUser=3)),
        ),
        (
            False,
            dict(display_probability="0.123"),
            dict(),
            dict(displayProbability="0.123"),
        ),
        (
            False,
            dict(user_daily_display_limit=3),
            dict(limitImpressionsPerDay=3),
            dict(impressions=dict(dailyPerUser=3)),
        ),
        (False, dict(display_chance=0), dict(chance=0), dict()),
        (False, {}, {}, {}),
        (
            True,
            dict(
                user_daily_display_limit=3,
                display_probability="0.123",
                total_daily_display_limit=100,
                total_displays=50,
            ),
            dict(limitImpressionsPerDay=3, isAds="true"),
            dict(
                displayProbability="0.123",
                impressions=dict(dailyPerUser=3, quarterHour=50),
            ),
        ),
    ],
)
async def test_will_transform_campaign_type_route_banner_for_metro_as_expected(
    limits: dict,
    is_ads: bool,
    expected_limits: dict,
    expected_fields: dict,
    avatars_batch,
    actions_factory: Callable,
    config,
):
    campaign_id = 1
    product = "metro_banner"
    actions, expected_actions = await actions_factory()
    avatars = avatars_batch(
        dict(banner=ImageType.BANNER, big_banner=ImageType.BIG_BANNER)
    )
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["po:1", "po:2"],
        campaign_type=CampaignType.ROUTE_BANNER,
        platforms=SUPPORTED_PLATFORMS,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[avatars["banner"].avatar, avatars["big_banner"].avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=is_ads,
            )
        },
        cost=0.0,
        **limits,
    )

    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=["bb:1", "bb:2"],
            polygons=["po:1", "po:2"],
            disclaimer="banner disclaimer",
            fields=dict(
                ageCategory="",
                campaignId=campaign_id,
                product=product,
                title="banner title",
                description="banner description",
                styleBanner=avatars["banner"].filename,
                **expected_fields,
            ),
            creatives=[
                dict(
                    id=avatars["big_banner"].hash,
                    type="metro_big_banner",
                    fields=dict(styleBigBanner=avatars["big_banner"].filename),
                )
            ],
            actions=expected_actions,
            limits=expected_limits,
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            target=dict(tag="and"),
            cost=0.0,
        )
    ]


@pytest.mark.parametrize("platform", UNSUPPORTED_PLATFORMS)
async def test_returns_empty_result_if_route_banner_is_not_supported_on_platform(
    platform, config
):
    campaign = dict(campaign_type=CampaignType.ROUTE_BANNER, platforms=[platform])
    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == []
