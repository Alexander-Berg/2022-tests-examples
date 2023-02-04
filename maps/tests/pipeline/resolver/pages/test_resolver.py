import pytest

from maps_adv.adv_store.api.schemas.enums import (
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
)
from maps_adv.export.lib.core.enum import (
    ActionType,
    CampaignType,
    CreativeType,
)
from maps_adv.export.lib.pipeline.exceptions import StepException
from maps_adv.export.lib.pipeline.steps import ResolvePagesStep
from maps_adv.export.lib.pipeline.steps.resolve_pages import feature
from maps_adv.export.lib.pipeline.steps.resolve_pages.page import Page

pytestmark = [pytest.mark.asyncio]


async def test_returns_expected_pages_for_campaign_type_of_pin_on_route(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.PIN_ON_ROUTE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=2,
            campaign_type=CampaignType.PIN_ON_ROUTE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[
                "mobile_maps_route_pins_1",
                "mobile_maps_route_pins_1/datatesting",
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=1,
            campaign_type=CampaignType.PIN_ON_ROUTE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_1",
                "mobile_maps_route_pins_1/datatesting",
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=2,
            campaign_type=CampaignType.PIN_ON_ROUTE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
    ]


async def test_returns_expected_pages_for_campaign_type_of_billboard(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=2,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.SEARCH)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=3,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=4,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BANNER: dict(show_ads_label=False)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=5,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=10,
            user_daily_display_limit=11,
            settings={},
        ),
        dict(
            id=6,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images_v2=[{}])},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=7,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images=[{}], split=True)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            id=8,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images=[{}], split=False)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_1",
                "navi_billboard_1/datatesting",
                "navi_billboard_2",
                "navi_billboard_2/datatesting",
                "navi_billboard_3",
                "navi_billboard_3/datatesting",
                "navi_billboard_4",
                "navi_billboard_4/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=1,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_4",
                "navi_billboard_4/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=2,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.SEARCH)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=3,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=4,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BANNER: dict(show_ads_label=False)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_3",
                "navi_billboard_3/datatesting",
                "navi_billboard_4",
                "navi_billboard_4/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=5,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            user_display_limit=10,
            user_daily_display_limit=11,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=6,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images_v2=[{}])},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "navi_billboard_1",
                "navi_billboard_1/datatesting",
                "navi_billboard_2",
                "navi_billboard_2/datatesting",
                "navi_billboard_3",
                "navi_billboard_3/datatesting",
                "navi_billboard_4",
                "navi_billboard_4/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
            ],
            id=7,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images=[{}], split=True)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
        dict(
            pages=[
                "mobile_maps_route_pins_2",
                "mobile_maps_route_pins_2/datatesting",
                "navi_billboard_1",
                "navi_billboard_1/datatesting",
                "navi_billboard_2",
                "navi_billboard_2/datatesting",
                "navi_billboard_3",
                "navi_billboard_3/datatesting",
                "navi_billboard_4",
                "navi_billboard_4/datatesting",
                "navi_billboard_5",
                "navi_billboard_5/datatesting",
                "navi_billboard_6",
                "navi_billboard_6/datatesting",
                "navi_billboard_7",
                "navi_billboard_7/datatesting",
                "navi_billboard_8",
                "navi_billboard_8/datatesting",
            ],
            id=8,
            campaign_type=CampaignType.BILLBOARD,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={CreativeType.BILLBOARD: dict(images=[{}], split=False)},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        ),
    ]


async def test_returns_expected_pages_for_campaign_type_of_category_search(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.CATEGORY_SEARCH,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[],
            id=1,
            campaign_type=CampaignType.CATEGORY_SEARCH,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
        )
    ]


@pytest.mark.parametrize(
    "campaign_type", [CampaignType.CATEGORY, CampaignType.PIN_SEARCH]
)
async def test_no_returns_navi_page_for_category_search_campaign_with_id_3305(
    campaign_type, config
):
    # Monkey patch for ignoring one campaign. GEOPROD-4441
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI]

    campaigns = [
        dict(
            id=3305,
            campaign_type=campaign_type,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns[0]["pages"] == [
        "navi_menu_icon_1",
        "navi_menu_icon_1/datatesting",
    ]


async def test_returns_expected_pages_for_campaign_type_of_category(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.CATEGORY,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[
                "mobile_maps_menu_icon_1",
                "mobile_maps_menu_icon_1/datatesting",
                "navi_menu_icon_1",
                "navi_menu_icon_1/datatesting",
            ],
            id=1,
            campaign_type=CampaignType.CATEGORY,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]


async def test_returns_expected_pages_for_campaign_type_of_pin_search(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[
                "mobile_maps_menu_icon_1",
                "mobile_maps_menu_icon_1/datatesting",
                "navi_menu_icon_1",
                "navi_menu_icon_1/datatesting",
            ],
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]


async def test_returns_expected_pages_for_campaign_type_of_zero_speed_banner(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        ),
        dict(
            id=2,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            settings={},
        ),
        dict(
            id=3,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            id=4,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[
                dict(type=ActionType.OPEN_SITE),
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.BROWSER),
            ],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            id=5,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[
                dict(type=ActionType.OPEN_SITE, main=True),
                dict(type=ActionType.RESOLVE_URI),
            ],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            id=6,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.ADD_POINT_TO_ROUTE)],
            creatives={},
            settings={},
        ),
        dict(
            id=7,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={"verification_data": [{}]},
        ),
        dict(
            id=8,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={"verification_data": [{}, {}]},
        ),
        dict(
            id=9,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [{"platform": "dcm", "params": {"url": ""}}]
            },
        ),
        dict(
            id=10,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {"url": "https://some.url/and?param=value"},
                    }
                ]
            },
        ),
        dict(
            id=11,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {"url": "https://some.url/and?param=value;dc_rdid="},
                    }
                ]
            },
        ),
        dict(
            id=12,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {
                            "url": "https://some.url/and?param=value;dc_rdid=[rdid]"
                        },
                    },
                    {},
                ]
            },
        ),
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=[
                "navi_zero_speed_banner_1",
                "navi_zero_speed_banner_1/datatesting",
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_2",
                "navi_zero_speed_banner_2/datatesting",
                "navi_zero_speed_banner_3",
                "navi_zero_speed_banner_3/datatesting",
                "navi_zero_speed_banner_4",
                "navi_zero_speed_banner_4/datatesting",
                "navi_zero_speed_banner_5",
                "navi_zero_speed_banner_5/datatesting",
                "navi_zero_speed_banner_6",
                "navi_zero_speed_banner_6/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=1,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_2",
                "navi_zero_speed_banner_2/datatesting",
                "navi_zero_speed_banner_3",
                "navi_zero_speed_banner_3/datatesting",
                "navi_zero_speed_banner_4",
                "navi_zero_speed_banner_4/datatesting",
                "navi_zero_speed_banner_5",
                "navi_zero_speed_banner_5/datatesting",
                "navi_zero_speed_banner_6",
                "navi_zero_speed_banner_6/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=2,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_3",
                "navi_zero_speed_banner_3/datatesting",
                "navi_zero_speed_banner_4",
                "navi_zero_speed_banner_4/datatesting",
                "navi_zero_speed_banner_5",
                "navi_zero_speed_banner_5/datatesting",
                "navi_zero_speed_banner_6",
                "navi_zero_speed_banner_6/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=3,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_4",
                "navi_zero_speed_banner_4/datatesting",
                "navi_zero_speed_banner_5",
                "navi_zero_speed_banner_5/datatesting",
                "navi_zero_speed_banner_6",
                "navi_zero_speed_banner_6/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=4,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[
                dict(type=ActionType.OPEN_SITE),
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.BROWSER),
            ],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_5",
                "navi_zero_speed_banner_5/datatesting",
                "navi_zero_speed_banner_6",
                "navi_zero_speed_banner_6/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=5,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[
                dict(type=ActionType.OPEN_SITE, main=True),
                dict(type=ActionType.RESOLVE_URI),
            ],
            creatives={CreativeType.AUDIO_BANNER: {}},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=6,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.ADD_POINT_TO_ROUTE)],
            creatives={},
            settings={},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_7",
                "navi_zero_speed_banner_7/datatesting",
                "navi_zero_speed_banner_8",
                "navi_zero_speed_banner_8/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=7,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={"verification_data": [{}]},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=8,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={"verification_data": [{}, {}]},
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
            ],
            id=9,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [{"platform": "dcm", "params": {"url": ""}}]
            },
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
            ],
            id=10,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {"url": "https://some.url/and?param=value"},
                    },
                ]
            },
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
            ],
            id=11,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {"url": "https://some.url/and?param=value;dc_rdid="},
                    },
                ]
            },
        ),
        dict(
            pages=[
                "navi_zero_speed_banner_10",
                "navi_zero_speed_banner_10/datatesting",
                "navi_zero_speed_banner_9",
                "navi_zero_speed_banner_9/datatesting",
            ],
            id=12,
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={
                "verification_data": [
                    {
                        "platform": "dcm",
                        "params": {
                            "url": "https://some.url/and?param=value;dc_rdid=[rdid]"
                        },
                    },
                    {},
                ]
            },
        ),
    ]


async def test_returns_expected_pages_for_campaign_type_of_route_banner(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.ROUTE_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=["metro_route_banner_1", "metro_route_banner_1/datatesting"],
            id=1,
            campaign_type=CampaignType.ROUTE_BANNER,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]


async def test_returns_expected_pages_for_campaign_type_of_route_via_point(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.ROUTE_VIA_POINT,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=["route_via_point_1", "route_via_point_1/datatesting"],
            id=1,
            campaign_type=CampaignType.ROUTE_VIA_POINT,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[dict(type=ActionType.OPEN_SITE)],
            creatives={},
            user_display_limit=None,
            user_daily_display_limit=None,
            settings={},
        )
    ]


async def test_returns_expected_pages_for_campaign_type_of_overview_banner(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.NAVI, PlatformEnum.MAPS, PlatformEnum.METRO]
    common_data = dict(
        campaign_type=CampaignType.OVERVIEW_BANNER,
        publication_envs=publication_envs,
        platforms=platforms,
        actions=[],
        creatives={},
        settings={},
    )

    campaigns = [
        dict(common_data, id=1),
        dict(
            common_data,
            id=2,
            actions=[
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.WEB_VIEW)
            ],
        ),
        dict(
            common_data,
            id=3,
            actions=[
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.BROWSER)
            ],
        ),
        dict(
            common_data,
            id=4,
            settings=dict(overview_position=OverviewPositionEnum.START),
        ),
        dict(
            common_data,
            id=5,
            settings=dict(overview_position=OverviewPositionEnum.FINISH),
        ),
        dict(
            common_data, id=6, settings=dict(overview_position=OverviewPositionEnum.ALL)
        ),
        dict(common_data, id=7, settings=dict(verification_data=[{}])),
        dict(common_data, id=8, settings=dict(verification_data=[{}, {}])),
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            common_data,
            pages=[
                "overview_banner_1",
                "overview_banner_1/datatesting",
                "overview_banner_2",
                "overview_banner_2/datatesting",
                "overview_banner_3",
                "overview_banner_3/datatesting",
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=1,
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_2",
                "overview_banner_2/datatesting",
                "overview_banner_3",
                "overview_banner_3/datatesting",
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=2,
            actions=[
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.WEB_VIEW)
            ],
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_3",
                "overview_banner_3/datatesting",
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=3,
            actions=[
                dict(type=ActionType.RESOLVE_URI, target=ResolveUriTargetEnum.BROWSER)
            ],
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_1",
                "overview_banner_1/datatesting",
                "overview_banner_2",
                "overview_banner_2/datatesting",
                "overview_banner_3",
                "overview_banner_3/datatesting",
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=4,
            settings=dict(overview_position=OverviewPositionEnum.START),
        ),
        dict(
            common_data,
            pages=[
                "overview_finish_3",
                "overview_finish_3/datatesting",
                "overview_finish_4",
                "overview_finish_4/datatesting",
                "overview_finish_5",
                "overview_finish_5/datatesting",
                "overview_finish_6",
                "overview_finish_6/datatesting",
            ],
            id=5,
            settings=dict(overview_position=OverviewPositionEnum.FINISH),
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_1",
                "overview_banner_1/datatesting",
                "overview_banner_2",
                "overview_banner_2/datatesting",
                "overview_banner_3",
                "overview_banner_3/datatesting",
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
                "overview_finish_3",
                "overview_finish_3/datatesting",
                "overview_finish_4",
                "overview_finish_4/datatesting",
                "overview_finish_5",
                "overview_finish_5/datatesting",
                "overview_finish_6",
                "overview_finish_6/datatesting",
            ],
            id=6,
            settings=dict(overview_position=OverviewPositionEnum.ALL),
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_4",
                "overview_banner_4/datatesting",
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=7,
            settings=dict(verification_data=[{}]),
        ),
        dict(
            common_data,
            pages=[
                "overview_banner_5",
                "overview_banner_5/datatesting",
                "overview_banner_6",
                "overview_banner_6/datatesting",
            ],
            id=8,
            settings=dict(verification_data=[{}, {}]),
        ),
    ]


async def test_returns_expected_pages_for_campaign_type_of_promocode(config):
    publication_envs = [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING]
    platforms = [PlatformEnum.METRO]

    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.PROMOCODE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]

    await ResolvePagesStep(config)(campaigns)

    assert campaigns == [
        dict(
            pages=["metro_promocode_1", "metro_promocode_1/datatesting"],
            id=1,
            campaign_type=CampaignType.PROMOCODE,
            publication_envs=publication_envs,
            platforms=platforms,
            actions=[],
            creatives={},
            settings={},
        )
    ]


async def test_returns_expected_list_pages(config):
    campaign = dict(
        id=1,
        campaign_type=CampaignType.BILLBOARD,
        publication_envs=[
            PublicationEnvEnum.PRODUCTION,
            PublicationEnvEnum.DATA_TESTING,
        ],
        settings={},
    )
    pages = [
        Page("pg", features=[feature.Init()], filters=[]),
        Page(
            "pg2",
            features=[feature.Init()],
            filters=[lambda item: item["campaign_type"] == CampaignType.ROUTE_BANNER],
        ),
    ]

    await ResolvePagesStep(config, pages)([campaign])

    assert campaign["pages"] == ["pg_1", "pg_1/datatesting"]


async def test_returns_expected_list_pages_with_custom_page_id(config):
    campaign = dict(
        id=1,
        campaign_type=CampaignType.BILLBOARD,
        publication_envs=[
            PublicationEnvEnum.PRODUCTION,
            PublicationEnvEnum.DATA_TESTING,
        ],
        settings={"custom_page_id": "abc"},
    )
    pages = [
        Page("pg", features=[feature.Init()], filters=[]),
        Page(
            "pg2",
            features=[feature.Init()],
            filters=[lambda item: item["campaign_type"] == CampaignType.ROUTE_BANNER],
        ),
    ]

    await ResolvePagesStep(config, pages)([campaign])

    assert campaign["pages"] == ["pg_1", "pg_1/testing_abc"]


@pytest.mark.parametrize(
    ["envs", "expected_pages"],
    [
        [
            [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
            ["pg_1", "pg_1/datatesting"],
        ],
        [[PublicationEnvEnum.PRODUCTION], ["pg_1"]],
        [[PublicationEnvEnum.DATA_TESTING], ["pg_1/datatesting"]],
    ],
)
async def test_return_expected_list_pages_for_publication_envs(
    envs, expected_pages, config
):
    campaign = dict(id=1, publication_envs=envs, settings={})

    page = Page("pg", features=[feature.Init()], filters=[])

    await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == expected_pages


async def test_correct_filtering_of_campaigns(config):
    campaign = dict(
        id=1,
        type=CampaignType.BILLBOARD,
        publication_envs=[
            PublicationEnvEnum.PRODUCTION,
            PublicationEnvEnum.DATA_TESTING,
        ],
        settings={},
    )
    features = [feature.Init()]
    filters = [lambda item: item["type"] == CampaignType.ROUTE_BANNER]
    page = Page("pg", features=features, filters=filters)

    await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == []


async def test_returns_pages_with_correct_versions(config):
    campaign = dict(
        id=1,
        type=CampaignType.BILLBOARD,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        actions=[dict(type=ActionType.OPEN_SITE)],
        settings={},
    )
    features = [
        feature.Init(),
        feature.WithActionType(ActionType.OPEN_SITE, min_version=2),
        feature.WithActionType(ActionType.PHONE_CALL, min_version=3),
    ]
    filters = []
    page = Page("pg", features=features, filters=filters)

    await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == ["pg_2", "pg_3"]


async def test_returns_page_id_without_version_number_if_it_is_zero(config):
    campaign = dict(id=1, publication_envs=[PublicationEnvEnum.PRODUCTION], settings={})
    features = [feature.Init(0)]
    page = Page("pg", features=features, filters=[])

    await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == ["pg"]


async def test_returns_pages_for_custom_page_ids(config):
    campaign = dict(
        id=1,
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        actions=[],
        settings={"custom_page_id": "abc"},
    )
    features = [
        feature.Init(),
        feature.WithActionType(ActionType.OPEN_SITE, min_version=2),
    ]
    page = Page("pg", features=features, filters=[])

    await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == ["pg_1/testing_abc", "pg_2/testing_abc"]


async def test_bad_custom_page_id(config):
    campaign = dict(
        id=42,
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        actions=[],
        settings={"custom_page_id": "Bad_PAGE-iD"},
    )
    features = [
        feature.Init(),
    ]
    page = Page("pg", features=features, filters=[])

    with pytest.raises(StepException) as exc:
        await ResolvePagesStep(config, [page])([campaign])

    assert exc.type == StepException
    assert exc.value.args[0] == [campaign["id"]]


async def test_cluster_environment(experimental_options):
    campaign = dict(
        id=42,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )
    features = [
        feature.Init(),
        feature.Init(2, for_testing_only=True),
    ]
    page = Page("pg", features=features, filters=[])

    with experimental_options({}) as config:
        await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == ["pg_2"]

    with experimental_options({"INSTANCE_TAG_CTYPE": "production"}) as config:
        await ResolvePagesStep(config, [page])([campaign])

    assert campaign["pages"] == ["pg_1", "pg_2"]
