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

SUPPORTED_PLATFORMS = [PlatformEnum.NAVI]
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
@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_will_transform_campaign_type_zero_speed_banner_as_expected(
    platform: PlatformEnum,
    limits: dict,
    is_ads: bool,
    expected_limits: dict,
    expected_fields: dict,
    avatars_factory,
    actions_factory: Callable,
    navi_uri_signer,
    config,
):
    campaign_id = 1
    actions, expected_actions = await actions_factory()
    product = "zero_speed_banner"
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=is_ads,
                terms="zsb terms",
            )
        },
        cost=0.0,
        **limits,
    )

    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=[],
            disclaimer="banner disclaimer",
            fields=dict(
                ageCategory="",
                campaignId=campaign_id,
                product=product,
                title="banner title",
                description="banner description",
                stylePin="",
                terms="zsb terms",
                **expected_fields,
            ),
            creatives=[
                dict(
                    id=banner.hash,
                    type="banner",
                    fields=dict(styleBalloonBanner=banner.filename),
                )
            ],
            actions=expected_actions,
            limits=expected_limits,
            target=dict(tag="and"),
            polygons=["polygon:1", "polygon:2"],
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=0.0,
        )
    ]


@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_will_transform_zero_speed_banner_with_audio_banner_as_expected(
    platform: PlatformEnum,
    avatars_factory,
    actions_factory: Callable,
    navi_uri_signer,
    config,
):
    campaign_id = 1
    actions, expected_actions = await actions_factory()
    product = "zero_speed_banner"
    audio_banner = avatars_factory(ImageType.BANNER)
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.AUDIO_BANNER: dict(
                images=[audio_banner.avatar],
                audio_file_url="https://s3.yandex.ru/audio",
                left_anchor="0.2",
            ),
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=True,
                terms="zsb terms",
            ),
        },
    )

    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=[],
            disclaimer="banner disclaimer",
            fields=dict(
                ageCategory="",
                campaignId=campaign_id,
                product=product,
                title="banner title",
                description="banner description",
                stylePin="",
                terms="zsb terms",
                isAds="true",
            ),
            creatives=[
                dict(
                    id=audio_banner.hash,
                    type="audio_banner",
                    fields=dict(
                        audioUrl="https://s3.yandex.ru/audio",
                        buttonLeftAnchor="0.2",
                        styleBalloonBanner=audio_banner.filename,
                    ),
                ),
                dict(
                    id=banner.hash,
                    type="banner",
                    fields=dict(styleBalloonBanner=banner.filename),
                ),
            ],
            actions=expected_actions,
            limits={},
            target=dict(tag="and"),
            polygons=["polygon:1", "polygon:2"],
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=None,
        )
    ]


@pytest.mark.parametrize("platform", UNSUPPORTED_PLATFORMS)
async def test_returns_empty_result_if_zero_speed_banner_is_not_supported_on_platform(
    platform, navi_uri_signer, config
):
    campaign = dict(campaign_type=CampaignType.ZERO_SPEED_BANNER, platforms=[platform])
    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert result == []


@pytest.mark.parametrize("terms", ["", None])
@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_skips_terms_field_for_empty_term(
    terms, platform, avatars_factory, actions_factory, navi_uri_signer, config
):
    campaign_id = 1
    actions, expected_actions = await actions_factory()
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=True,
                terms=terms,
            )
        },
    )

    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert "terms" not in result[0]["fields"]


@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_skips_terms_field_if_terms_missing_in_banner_creative(
    platform, avatars_factory, actions_factory, navi_uri_signer, config
):
    campaign_id = 1
    actions, expected_actions = await actions_factory()
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=[],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=True,
            )
        },
    )

    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert "terms" not in result[0]["fields"]
