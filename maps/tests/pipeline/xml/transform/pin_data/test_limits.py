import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.pin_data import pin_data_transform

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ["publication_envs", "limits", "expected_limits"],
    [
        (
            [PublicationEnvEnum.PRODUCTION],
            dict(
                total_daily_display_limit=100,
                total_displays=50,
            ),
            dict(
                impressions=dict(quarterHour=50),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=50,
            ),
            dict(
                impressions=dict(quarterHour=50),
            ),
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=50,
            ),
            dict(),
        ),
        (
            [PublicationEnvEnum.PRODUCTION],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
            ),
            dict(
                impressions=dict(quarterHour=0),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
            ),
            dict(
                impressions=dict(quarterHour=0),
            ),
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
            ),
            dict(),
        ),
    ],
)
async def test_limits_by_env(
    publication_envs,
    limits,
    expected_limits,
    navi_uri_signer,
    avatars_factory,
    actions_factory,
    config,
):
    actions, expected_actions = await actions_factory()
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=1,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[PlatformEnum.NAVI],
        publication_envs=publication_envs,
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=True,
                terms="zsb terms",
            ),
        },
        **limits,
    )

    result = await pin_data_transform(
        campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )
    assert result[0]["limits"] == expected_limits


@pytest.mark.parametrize(
    ["publication_envs", "limits", "expected_limits"],
    [
        (
            [PublicationEnvEnum.PRODUCTION],
            dict(
                total_daily_display_limit=100,
                total_displays=50,
                total_display_minutes_today=1200,
                total_display_minutes_left_today=600,
            ),
            dict(
                impressions=dict(quarterHour=2),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=1000,
                total_displays=0,
                total_display_minutes_today=15,
                total_display_minutes_left_today=15,
            ),
            dict(
                impressions=dict(quarterHour=1000),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=1000,
                total_displays=0,
                total_display_minutes_today=24 * 60,
                total_display_minutes_left_today=24 * 60,
            ),
            dict(
                impressions=dict(quarterHour=20),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=1000,
                total_displays=900,
                total_display_minutes_today=24 * 60,
                total_display_minutes_left_today=1 * 60,
            ),
            dict(
                impressions=dict(quarterHour=50),
            ),
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=50,
                total_display_minutes_today=60,
                total_display_minutes_left_today=30,
            ),
            dict(),
        ),
        (
            [PublicationEnvEnum.PRODUCTION],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
                total_display_minutes_today=60,
                total_display_minutes_left_today=30,
            ),
            dict(
                impressions=dict(quarterHour=0),
            ),
        ),
        (
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
                total_display_minutes_today=60,
                total_display_minutes_left_today=30,
            ),
            dict(
                impressions=dict(quarterHour=0),
            ),
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            dict(
                total_daily_display_limit=100,
                total_displays=100,
                total_display_minutes_today=60,
                total_display_minutes_left_today=30,
            ),
            dict(),
        ),
    ],
)
async def test_experemental_limits_by_env(
    publication_envs,
    limits,
    expected_limits,
    navi_uri_signer,
    avatars_factory,
    actions_factory,
    experimental_options,
    config,
):
    actions, expected_actions = await actions_factory()
    banner = avatars_factory(ImageType.BANNER)
    campaign = dict(
        id=1,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=["polygon:1", "polygon:2"],
        campaign_type=CampaignType.ZERO_SPEED_BANNER,
        platforms=[PlatformEnum.NAVI],
        publication_envs=publication_envs,
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.BANNER: dict(
                images=[banner.avatar],
                title="banner title",
                description="banner description",
                disclaimer="banner disclaimer",
                show_ads_label=True,
                terms="zsb terms",
            ),
        },
        **limits,
    )

    with experimental_options({"EXPERIMENT_QUARTER_HOUR_DISPLAYS_CAMPAIGNS": None}):
        result = await pin_data_transform(
            campaign, navi_uri_signer, config.INSTANCE_TAG_CTYPE
        )
    assert result[0]["limits"] == expected_limits
