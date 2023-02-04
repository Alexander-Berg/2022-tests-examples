from typing import Callable

import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.core.exception import NotFoundBannerAndLogoText
from maps_adv.export.lib.pipeline.xml.transform.pin_data import pin_data_transform

pytestmark = [pytest.mark.asyncio]

SUPPORTED_PLATFORMS = [PlatformEnum.NAVI, PlatformEnum.MAPS]
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
async def test_will_transform_campaign_type_pin_on_route_as_expected(
    platform: PlatformEnum,
    limits: dict,
    is_ads: bool,
    expected_limits: dict,
    expected_fields: dict,
    avatars_batch: Callable,
    actions_factory: Callable,
    config,
):
    campaign_id = 1
    product = "pin_on_route_v2"
    actions, expected_actions = await actions_factory()
    avatars = avatars_batch(
        dict(
            pin=ImageType.PIN,
            pin_hover=ImageType.PIN_HOVER,
            pin_visited=ImageType.PIN_VISITED,
            pin_round=ImageType.PIN_ROUND,
            pin_selected=ImageType.PIN_SELECTED,
            pin_left=ImageType.PIN_LEFT,
            pin_right=ImageType.PIN_RIGHT,
            dust=ImageType.DUST,
            dust_hover=ImageType.DUST_HOVER,
            dust_visited=ImageType.DUST_VISITED,
            banner=ImageType.BANNER,
        )
    )
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        publication_envs=[
            PublicationEnvEnum.DATA_TESTING,
            PublicationEnvEnum.PRODUCTION,
        ],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[platform],
        actions=actions,
        targeting=dict(tag="and"),
        creatives={
            CreativeType.PIN: dict(
                title="pin title",
                subtitle="pin subtitle",
                images=[
                    avatars["pin"].avatar,
                    avatars["pin_hover"].avatar,
                    avatars["pin_visited"].avatar,
                    avatars["pin_round"].avatar,
                    avatars["pin_left"].avatar,
                    avatars["pin_right"].avatar,
                    avatars["pin_selected"].avatar,
                    avatars["dust"].avatar,
                    avatars["dust_hover"].avatar,
                    avatars["dust_visited"].avatar,
                ],
            ),
            CreativeType.BANNER: dict(
                images=[avatars["banner"].avatar],
                title="banner title",
                disclaimer="banner disclaimer",
                show_ads_label=is_ads,
            ),
        },
        cost=0.0,
        **limits,
    )

    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=["bb:1", "bb:2"],
            polygons=[],
            disclaimer="banner disclaimer",
            fields=dict(
                campaignId=campaign_id,
                product=product,
                title="banner title",
                ageCategory="",
                chains="",
                hasDiscounts="false",
                pinTitle="pin title",
                pinSubtitle="pin subtitle",
                anchorDust="0.5 0.5",
                anchorDustHover="0.5 0.5",
                anchorDustVisited="0.5 0.5",
                anchorIcon="0.5 0.89",
                anchorIconHover="0.5 0.89",
                anchorIconVisited="0.5 0.89",
                anchorSelected="0.5 0.94",
                sizeDust="18 18",
                sizeDustHover="18 18",
                sizeDustVisited="18 18",
                sizeIcon="32 38",
                sizeIconHover="32 38",
                sizeIconVisited="32 38",
                sizeSelected="60 68",
                styleDust=avatars["dust"].filename,
                styleDustHover=avatars["dust_hover"].filename,
                styleDustVisited=avatars["dust_visited"].filename,
                styleIcon=avatars["pin"].filename,
                styleIconHover=avatars["pin_hover"].filename,
                styleIconVisited=avatars["pin_visited"].filename,
                stylePin=avatars["pin_round"].filename,
                stylePinLeft=avatars["pin_left"].filename,
                stylePinRight=avatars["pin_right"].filename,
                styleSelected=avatars["pin_selected"].filename,
                **expected_fields,
            ),
            creatives=[
                dict(
                    id=avatars["banner"].hash,
                    type="banner",
                    fields=dict(styleBalloonBanner=avatars["banner"].filename),
                )
            ],
            actions=expected_actions,
            limits=expected_limits,
            target=dict(tag="and"),
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=0.0,
        )
    ]


@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_raises_exception_for_campaign_type_pin_on_route_as_expected(
    platform: PlatformEnum, avatars_batch: Callable, actions_factory: Callable, config
):
    actions, expected_actions = await actions_factory()
    avatars = avatars_batch(
        dict(
            pin=ImageType.PIN,
            pin_hover=ImageType.PIN_HOVER,
            pin_visited=ImageType.PIN_VISITED,
            pin_round=ImageType.PIN_ROUND,
            pin_selected=ImageType.PIN_SELECTED,
            pin_left=ImageType.PIN_LEFT,
            pin_right=ImageType.PIN_RIGHT,
            dust=ImageType.DUST,
            dust_hover=ImageType.DUST_HOVER,
            dust_visited=ImageType.DUST_VISITED,
            banner=ImageType.BANNER,
        )
    )
    campaign = dict(
        id=1,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        polygons=[],
        publication_envs=[
            PublicationEnvEnum.DATA_TESTING,
            PublicationEnvEnum.PRODUCTION,
        ],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[platform],
        actions=actions,
        targeting=dict(),
        creatives={
            CreativeType.PIN: dict(
                title="pin title",
                subtitle="pin subtitle",
                images=[
                    avatars["pin"].avatar,
                    avatars["pin_hover"].avatar,
                    avatars["pin_visited"].avatar,
                    avatars["pin_round"].avatar,
                    avatars["pin_left"].avatar,
                    avatars["pin_right"].avatar,
                    avatars["pin_selected"].avatar,
                    avatars["dust"].avatar,
                    avatars["dust_hover"].avatar,
                    avatars["dust_visited"].avatar,
                ],
            )
        },
    )

    with pytest.raises(NotFoundBannerAndLogoText):
        await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)


@pytest.mark.parametrize("platform", UNSUPPORTED_PLATFORMS)
async def test_returns_empty_result_if_pin_on_route_is_not_supported_on_platform(
    platform, config
):
    campaign = dict(campaign_type=CampaignType.PIN_ON_ROUTE, platforms=[platform])
    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == []


@pytest.mark.parametrize(
    ["environment", "page"],
    [
        (PublicationEnvEnum.DATA_TESTING, "p1/datatesting"),
        (PublicationEnvEnum.PRODUCTION, "p1"),
    ],
)
async def test_monkey_patch_navi_with_round_pin_for_pin_on_route(
    environment,
    page,
    avatars_batch: Callable,
    actions_factory: Callable,
    experimental_options,
    config,
):
    with experimental_options(
        {"MONKEY_PATCH_NAVI_WITH_ROUND_PIN_FOR_PIN_ON_ROUTE": [environment]}
    ):
        campaign_id = 1
        product = "pin_on_route_v2"
        actions, expected_actions = await actions_factory()
        avatars = avatars_batch(
            dict(
                pin=ImageType.PIN,
                pin_hover=ImageType.PIN_HOVER,
                pin_visited=ImageType.PIN_VISITED,
                pin_round=ImageType.PIN_ROUND,
                pin_selected=ImageType.PIN_SELECTED,
                pin_left=ImageType.PIN_LEFT,
                pin_right=ImageType.PIN_RIGHT,
                dust=ImageType.DUST,
                dust_hover=ImageType.DUST_HOVER,
                dust_visited=ImageType.DUST_VISITED,
                banner=ImageType.BANNER,
            )
        )
        campaign = dict(
            id=campaign_id,
            pages=[page],
            places=["bb:1", "bb:2"],
            publication_envs=[environment],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformEnum.NAVI],
            actions=actions,
            targeting=dict(tag="and"),
            creatives={
                CreativeType.PIN: dict(
                    title="pin title",
                    subtitle="pin subtitle",
                    images=[
                        avatars["pin"].avatar,
                        avatars["pin_hover"].avatar,
                        avatars["pin_visited"].avatar,
                        avatars["pin_round"].avatar,
                        avatars["pin_left"].avatar,
                        avatars["pin_right"].avatar,
                        avatars["pin_selected"].avatar,
                        avatars["dust"].avatar,
                        avatars["dust_hover"].avatar,
                        avatars["dust_visited"].avatar,
                    ],
                ),
                CreativeType.BANNER: dict(
                    images=[avatars["banner"].avatar],
                    title="banner title",
                    disclaimer="banner disclaimer",
                    show_ads_label=False,
                ),
            },
        )

        result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == [
        dict(
            pages=[page],
            places=["bb:1", "bb:2"],
            polygons=[],
            disclaimer="banner disclaimer",
            fields=dict(
                campaignId=campaign_id,
                product=product,
                title="banner title",
                ageCategory="",
                chains="",
                hasDiscounts="false",
                pinTitle="pin title",
                pinSubtitle="pin subtitle",
                anchorDust="0.5 0.5",
                anchorDustHover="0.5 0.5",
                anchorDustVisited="0.5 0.5",
                anchorIcon="0.5 0.5",
                anchorIconHover="0.5 0.5",
                anchorIconVisited="0.5 0.5",
                anchorSelected="0.5 0.94",
                sizeDust="18 18",
                sizeDustHover="18 18",
                sizeDustVisited="18 18",
                sizeIcon="48 48",
                sizeIconHover="48 48",
                sizeIconVisited="48 48",
                sizeSelected="60 68",
                styleDust=avatars["dust"].filename,
                styleDustHover=avatars["dust_hover"].filename,
                styleDustVisited=avatars["dust_visited"].filename,
                styleIcon=avatars["pin_round"].filename,
                styleIconHover=avatars["pin_round"].filename,
                styleIconVisited=avatars["pin_round"].filename,
                stylePin=avatars["pin_round"].filename,
                stylePinLeft=avatars["pin_left"].filename,
                stylePinRight=avatars["pin_right"].filename,
                styleSelected=avatars["pin_selected"].filename,
            ),
            creatives=[
                dict(
                    id=avatars["banner"].hash,
                    type="banner",
                    fields=dict(styleBalloonBanner=avatars["banner"].filename),
                )
            ],
            actions=expected_actions,
            limits={},
            target=dict(tag="and"),
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=None,
        )
    ]


@pytest.mark.parametrize(
    "environment", [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION]
)
async def test_monkey_patch_navi_with_round_pin_for_pin_on_route_no_affected_maps(
    environment,
    avatars_batch: Callable,
    actions_factory: Callable,
    experimental_options,
    config,
):
    with experimental_options(
        {"MONKEY_PATCH_NAVI_WITH_ROUND_PIN_FOR_PIN_ON_ROUTE": [environment]}
    ):
        campaign_id = 1
        product = "pin_on_route_v2"
        actions, expected_actions = await actions_factory()
        avatars = avatars_batch(
            dict(
                pin=ImageType.PIN,
                pin_hover=ImageType.PIN_HOVER,
                pin_visited=ImageType.PIN_VISITED,
                pin_round=ImageType.PIN_ROUND,
                pin_selected=ImageType.PIN_SELECTED,
                pin_left=ImageType.PIN_LEFT,
                pin_right=ImageType.PIN_RIGHT,
                dust=ImageType.DUST,
                dust_hover=ImageType.DUST_HOVER,
                dust_visited=ImageType.DUST_VISITED,
                banner=ImageType.BANNER,
            )
        )
        campaign = dict(
            id=campaign_id,
            pages=["p1", "p2"],
            places=["bb:1", "bb:2"],
            publication_envs=[environment],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformEnum.MAPS],
            actions=actions,
            targeting=dict(tag="and"),
            creatives={
                CreativeType.PIN: dict(
                    title="pin title",
                    subtitle="pin subtitle",
                    images=[
                        avatars["pin"].avatar,
                        avatars["pin_hover"].avatar,
                        avatars["pin_visited"].avatar,
                        avatars["pin_round"].avatar,
                        avatars["pin_left"].avatar,
                        avatars["pin_right"].avatar,
                        avatars["pin_selected"].avatar,
                        avatars["dust"].avatar,
                        avatars["dust_hover"].avatar,
                        avatars["dust_visited"].avatar,
                    ],
                ),
                CreativeType.BANNER: dict(
                    images=[avatars["banner"].avatar],
                    title="banner title",
                    disclaimer="banner disclaimer",
                    show_ads_label=False,
                ),
            },
        )

        result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=["bb:1", "bb:2"],
            polygons=[],
            disclaimer="banner disclaimer",
            fields=dict(
                campaignId=1,
                product=product,
                title="banner title",
                ageCategory="",
                chains="",
                hasDiscounts="false",
                pinTitle="pin title",
                pinSubtitle="pin subtitle",
                anchorDust="0.5 0.5",
                anchorDustHover="0.5 0.5",
                anchorDustVisited="0.5 0.5",
                anchorIcon="0.5 0.89",
                anchorIconHover="0.5 0.89",
                anchorIconVisited="0.5 0.89",
                anchorSelected="0.5 0.94",
                sizeDust="18 18",
                sizeDustHover="18 18",
                sizeDustVisited="18 18",
                sizeIcon="32 38",
                sizeIconHover="32 38",
                sizeIconVisited="32 38",
                sizeSelected="60 68",
                styleDust=avatars["dust"].filename,
                styleDustHover=avatars["dust_hover"].filename,
                styleDustVisited=avatars["dust_visited"].filename,
                styleIcon=avatars["pin"].filename,
                styleIconHover=avatars["pin_hover"].filename,
                styleIconVisited=avatars["pin_visited"].filename,
                stylePin=avatars["pin_round"].filename,
                stylePinLeft=avatars["pin_left"].filename,
                stylePinRight=avatars["pin_right"].filename,
                styleSelected=avatars["pin_selected"].filename,
            ),
            creatives=[
                dict(
                    id=avatars["banner"].hash,
                    type="banner",
                    fields=dict(styleBalloonBanner=avatars["banner"].filename),
                )
            ],
            actions=expected_actions,
            limits={},
            target=dict(tag="and"),
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=None,
        )
    ]
