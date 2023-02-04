import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.client.old_geoadv import OrgPlace
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.base import pipeline_data_transform
from maps_adv.export.lib.pipeline.xml.transform.place import GenericPlace
from maps_adv.points.client.lib import ResultPoint

pytestmark = [pytest.mark.asyncio]


async def test_returns_pipeline_data_transform_as_expected(
    avatars_batch, actions_factory, companies_factory, navi_uri_signer, config
):
    companies = companies_factory(count=3)
    limits = dict(
        user_daily_display_limit=3,
        display_probability="0.123",
        total_daily_display_limit=100,
        total_displays=50,
    )
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
            logo=ImageType.LOGO,
            banner=ImageType.BANNER,
            category=ImageType.CATEGORY,
        )
    )

    campaign1 = dict(
        id=1,
        pages=["p1", "p2"],
        places=["altay:1", "altay:2"],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
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
                show_ads_label=True,
            ),
        },
        **limits,
    )

    campaign2_category = dict(
        id=2,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon",
                title="icon title",
                text="icon text",
                position=5,
                search_text="icon search text",
                images=[avatars["category"].avatar],
                organizations={item.permalink: item for item in companies},
            )
        },
        actions=[],
    )

    campaign2_pin_search = dict(
        id=2,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={}),
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    style="style pin_search",
                    organizations={item.permalink: item for item in companies},
                    images=[
                        avatars["pin"].avatar,
                        avatars["pin_hover"].avatar,
                        avatars["pin_visited"].avatar,
                        avatars["pin_round"].avatar,
                        avatars["pin_selected"].avatar,
                        avatars["dust"].avatar,
                        avatars["dust_hover"].avatar,
                        avatars["dust_visited"].avatar,
                    ],
                )
            ],
        },
        actions=[],
    )

    campaign3_category = dict(
        id=3,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.MAPS],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon",
                title="icon title",
                text="icon text",
                position=5,
                search_text="icon search text",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    campaign3_pin_search = dict(
        id=3,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[PlatformEnum.MAPS],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={}),
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    style="style pin_search",
                    organizations={companies[0].permalink: companies[0]},
                    images=[
                        avatars["logo"].avatar,
                        avatars["banner"].avatar,
                        avatars["pin"].avatar,
                        avatars["pin_hover"].avatar,
                        avatars["pin_visited"].avatar,
                        avatars["pin_round"].avatar,
                        avatars["pin_selected"].avatar,
                        avatars["dust"].avatar,
                        avatars["dust_hover"].avatar,
                        avatars["dust_visited"].avatar,
                    ],
                )
            ],
        },
        actions=[],
    )

    payload = dict(
        campaigns=[
            campaign1,
            campaign2_category,
            campaign2_pin_search,
            campaign3_category,
            campaign3_pin_search,
        ],
        places={
            "altay:1": OrgPlace(
                latitude=10.23,
                longitude=40.56,
                title="place altay 1 title",
                address="place altay 1 addresses",
                permalink=1,
            ),
            "1": ResultPoint(id=1, latitude=100.23, longitude=400.56),
        },
        search_tags=[
            dict(id="s:1", companies=[1, 2, 3]),
            dict(id="s:2", companies=[5, 6, 7]),
        ],
        polygons={
            "campaign:1.2": dict(
                id="campaign:1.2",
                points=[
                    {"latitude": 3, "longitude": 5},
                    {"latitude": 3, "longitude": 4},
                ],
            ),
            "campaign:1.1": dict(
                id="campaign:1.1",
                points=[
                    {"latitude": 1, "longitude": 2},
                    {"latitude": 3, "longitude": 4},
                ],
            ),
        },
    )

    xml_data, attrs = await pipeline_data_transform(
        payload, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert xml_data == {
        "menu_items": [
            {
                "id": "ac_auto_log_id_campaign_2",
                "pages": ["p1", "p2"],
                "companies": [item.permalink for item in companies],
                "style": "style icon",
                "title": [{"value": "icon title"}],
                "search_text": "icon search text",
                "position": 5,
            },
            {
                "id": "ac_auto_log_id_campaign_3",
                "pages": ["p1", "p2"],
                "companies": [companies[0].permalink],
                "style": "style icon",
                "title": [{"value": "icon title"}],
                "search_text": "icon search text",
                "position": 5,
            },
        ],
        "advert_data_list": [
            {
                "pages": ["p1", "p2"],
                "log_id": "ac_auto_log_id_campaign_2",
                "text": "text",
                "disclaimer": "disclaimer",
                "fields": {
                    "advert_type": "menu_icon",
                    "anchorDust": "0.5 0.5",
                    "anchorDustHover": "0.5 0.5",
                    "anchorDustVisited": "0.5 0.5",
                    "anchorIcon": "0.5 0.5",
                    "anchorIconHover": "0.5 0.5",
                    "anchorIconVisited": "0.5 0.5",
                    "anchorSelected": "0.5 0.94",
                    "sizeDust": "18 18",
                    "sizeDustHover": "18 18",
                    "sizeDustVisited": "18 18",
                    "sizeIcon": "32 32",
                    "sizeIconHover": "32 32",
                    "sizeIconVisited": "32 32",
                    "sizeSelected": "60 68",
                    "styleDust": avatars["dust"].filename,
                    "styleDustHover": avatars["dust_hover"].filename,
                    "styleDustVisited": avatars["dust_visited"].filename,
                    "styleIcon": avatars["pin_round"].filename,
                    "styleIconHover": avatars["pin_round"].filename,
                    "styleIconVisited": avatars["pin_round"].filename,
                    "styleSelected": avatars["pin_selected"].filename,
                },
                "title": companies[0].title,
                "companies": [0],
            },
            {
                "pages": ["p1", "p2"],
                "log_id": "ac_auto_log_id_campaign_2",
                "text": "text",
                "disclaimer": "disclaimer",
                "fields": {
                    "advert_type": "menu_icon",
                    "anchorDust": "0.5 0.5",
                    "anchorDustHover": "0.5 0.5",
                    "anchorDustVisited": "0.5 0.5",
                    "anchorIcon": "0.5 0.5",
                    "anchorIconHover": "0.5 0.5",
                    "anchorIconVisited": "0.5 0.5",
                    "anchorSelected": "0.5 0.94",
                    "sizeDust": "18 18",
                    "sizeDustHover": "18 18",
                    "sizeDustVisited": "18 18",
                    "sizeIcon": "32 32",
                    "sizeIconHover": "32 32",
                    "sizeIconVisited": "32 32",
                    "sizeSelected": "60 68",
                    "styleDust": avatars["dust"].filename,
                    "styleDustHover": avatars["dust_hover"].filename,
                    "styleDustVisited": avatars["dust_visited"].filename,
                    "styleIcon": avatars["pin_round"].filename,
                    "styleIconHover": avatars["pin_round"].filename,
                    "styleIconVisited": avatars["pin_round"].filename,
                    "styleSelected": avatars["pin_selected"].filename,
                },
                "title": companies[1].title,
                "companies": [1],
            },
            {
                "pages": ["p1", "p2"],
                "log_id": "ac_auto_log_id_campaign_2",
                "text": "text",
                "disclaimer": "disclaimer",
                "fields": {
                    "advert_type": "menu_icon",
                    "anchorDust": "0.5 0.5",
                    "anchorDustHover": "0.5 0.5",
                    "anchorDustVisited": "0.5 0.5",
                    "anchorIcon": "0.5 0.5",
                    "anchorIconHover": "0.5 0.5",
                    "anchorIconVisited": "0.5 0.5",
                    "anchorSelected": "0.5 0.94",
                    "sizeDust": "18 18",
                    "sizeDustHover": "18 18",
                    "sizeDustVisited": "18 18",
                    "sizeIcon": "32 32",
                    "sizeIconHover": "32 32",
                    "sizeIconVisited": "32 32",
                    "sizeSelected": "60 68",
                    "styleDust": avatars["dust"].filename,
                    "styleDustHover": avatars["dust_hover"].filename,
                    "styleDustVisited": avatars["dust_visited"].filename,
                    "styleIcon": avatars["pin_round"].filename,
                    "styleIconHover": avatars["pin_round"].filename,
                    "styleIconVisited": avatars["pin_round"].filename,
                    "styleSelected": avatars["pin_selected"].filename,
                },
                "title": companies[2].title,
                "companies": [2],
            },
            {
                "pages": ["p1", "p2"],
                "log_id": "ac_auto_log_id_campaign_3",
                "text": "text",
                "disclaimer": "disclaimer",
                "fields": {
                    "advert_type": "menu_icon",
                    "anchorDust": "0.5 0.5",
                    "anchorDustHover": "0.5 0.5",
                    "anchorDustVisited": "0.5 0.5",
                    "anchorIcon": "0.5 0.89",
                    "anchorIconHover": "0.5 0.89",
                    "anchorIconVisited": "0.5 0.89",
                    "anchorSelected": "0.5 0.94",
                    "sizeDust": "18 18",
                    "sizeDustHover": "18 18",
                    "sizeDustVisited": "18 18",
                    "sizeIcon": "32 38",
                    "sizeIconHover": "32 38",
                    "sizeIconVisited": "32 38",
                    "sizeSelected": "60 68",
                    "styleDust": avatars["dust"].filename,
                    "styleDustHover": avatars["dust_hover"].filename,
                    "styleDustVisited": avatars["dust_visited"].filename,
                    "styleIcon": avatars["pin"].filename,
                    "styleIconHover": avatars["pin_hover"].filename,
                    "styleIconVisited": avatars["pin_visited"].filename,
                    "styleSelected": avatars["pin_selected"].filename,
                    "styleLogo": avatars["logo"].filename,
                    "styleBalloonBanner": avatars["banner"].filename,
                },
                "title": companies[0].title,
                "companies": [0],
            },
        ],
        "advert_tags": [
            {"id": "s:1", "companies": [1, 2, 3]},
            {"id": "s:2", "companies": [5, 6, 7]},
        ],
        "places": [
            GenericPlace(
                id="1",
                latitude=100.23,
                longitude=400.56,
                title=[],
                address=[],
                permalink=None,
            ),
            GenericPlace(
                id="altay:1",
                latitude=10.23,
                longitude=40.56,
                title=[{"value": "place altay 1 title"}],
                address=[{"value": "place altay 1 addresses"}],
                permalink=1,
            ),
        ],
        "polygons": [
            {"id": "campaign:1.1", "polygon": "POLYGON ((2 1, 4 3))"},
            {"id": "campaign:1.2", "polygon": "POLYGON ((5 3, 4 3))"},
        ],
        "pin_data_list": [
            {
                "pages": ["p1", "p2"],
                "places": ["altay:1", "altay:2"],
                "polygons": [],
                "disclaimer": "banner disclaimer",
                "target": {"tag": "and"},
                "fields": {
                    "ageCategory": "",
                    "hasDiscounts": "false",
                    "chains": "",
                    "campaignId": 1,
                    "product": "pin_on_route_v2",
                    "title": "banner title",
                    "isAds": "true",
                    "pinTitle": "pin title",
                    "pinSubtitle": "pin subtitle",
                    "anchorDust": "0.5 0.5",
                    "anchorDustHover": "0.5 0.5",
                    "anchorDustVisited": "0.5 0.5",
                    "anchorIcon": "0.5 0.89",
                    "anchorIconHover": "0.5 0.89",
                    "anchorIconVisited": "0.5 0.89",
                    "anchorSelected": "0.5 0.94",
                    "sizeDust": "18 18",
                    "sizeDustHover": "18 18",
                    "sizeDustVisited": "18 18",
                    "sizeIcon": "32 38",
                    "sizeIconHover": "32 38",
                    "sizeIconVisited": "32 38",
                    "sizeSelected": "60 68",
                    "styleDust": avatars["dust"].filename,
                    "styleDustHover": avatars["dust_hover"].filename,
                    "styleDustVisited": avatars["dust_visited"].filename,
                    "styleIcon": avatars["pin"].filename,
                    "styleIconHover": avatars["pin_hover"].filename,
                    "styleIconVisited": avatars["pin_visited"].filename,
                    "stylePin": avatars["pin_round"].filename,
                    "stylePinLeft": avatars["pin_left"].filename,
                    "stylePinRight": avatars["pin_right"].filename,
                    "styleSelected": avatars["pin_selected"].filename,
                    "limitImpressionsPerDay": 3,
                },
                "creatives": [
                    {
                        "id": avatars["banner"].hash,
                        "type": "banner",
                        "fields": {"styleBalloonBanner": avatars["banner"].filename},
                    }
                ],
                "actions": expected_actions,
                "limits": {
                    "displayProbability": "0.123",
                    "impressions": {"dailyPerUser": 3, "quarterHour": 50},
                },
                "log_info": {
                    "advertiserId": None,
                    "campaignId": "1",
                    "product": "pin_on_route_v2",
                },
                "cost": None,
            }
        ],
    }

    assert attrs == {
        "advert_tag_hash": "34b09945712147cfa061dec46d1d9b3d",
    }


async def test_handle_duplicated_campaigns_for_data_testing(
    companies_factory, avatars_batch, navi_uri_signer, config
):

    companies = companies_factory(count=3)
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
            logo=ImageType.LOGO,
            banner=ImageType.BANNER,
            category=ImageType.CATEGORY,
        )
    )

    campaign1 = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 1",
                title="icon title 1",
                text="icon text 1",
                position=1,
                search_text="icon search text 1",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    campaign1_dt = dict(  # the same campaign as 1, but for testing mode
        id=1,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 1",
                title="icon title 1",
                text="icon text 1",
                position=1,
                search_text="icon search text 1",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    campaign2 = dict(
        id=2,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 2",
                title="icon title 2",
                text="icon text 2",
                position=2,
                search_text="icon search text 2",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    payload = dict(
        campaigns=[campaign1, campaign1_dt, campaign2],
        places={},
        search_tags=[],
        polygons={},
    )

    xml_data, attrs = await pipeline_data_transform(
        payload, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert xml_data == {
        "menu_items": [
            {
                "id": "ac_auto_log_id_campaign_1",
                "pages": ["p1", "p2"],
                "companies": [0, 1, 2],
                "style": "style icon 1",
                "title": [{"value": "icon title 1"}],
                "position": 1,
                "search_text": "icon search text 1",
            },
            {
                "id": "ac_auto_log_id_campaign_1_data_testing",
                "pages": ["p1", "p2"],
                "companies": [0, 1, 2],
                "style": "style icon 1",
                "title": [{"value": "icon title 1"}],
                "position": 1,
                "search_text": "icon search text 1",
            },
            {
                "id": "ac_auto_log_id_campaign_2",
                "pages": ["p1", "p2"],
                "companies": [0, 1, 2],
                "style": "style icon 2",
                "title": [{"value": "icon title 2"}],
                "position": 2,
                "search_text": "icon search text 2",
            },
        ],
        "advert_data_list": [],
        "advert_tags": [],
        "places": [],
        "polygons": [],
        "pin_data_list": [],
    }

    assert attrs == {
        "advert_tag_hash": "d41d8cd98f00b204e9800998ecf8427e",
    }


async def test_reports_incidentally_duplicated_campaigns(
    companies_factory, avatars_batch, navi_uri_signer, config
):

    companies = companies_factory(count=3)
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
            logo=ImageType.LOGO,
            banner=ImageType.BANNER,
            category=ImageType.CATEGORY,
        )
    )

    campaign1 = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 1",
                title="icon title 1",
                text="icon text 1",
                position=1,
                search_text="icon search text 1",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    campaign1_dt = dict(  # the same campaign as 1
        id=1,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 1",
                title="icon title 1",
                text="icon text 1",
                position=1,
                search_text="icon search text 1",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    campaign2 = dict(
        id=2,
        campaign_type=CampaignType.CATEGORY,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={item.permalink: item for item in companies}),
        creatives={
            CreativeType.ICON: dict(
                style="style icon 2",
                title="icon title 2",
                text="icon text 2",
                position=2,
                search_text="icon search text 2",
                images=[avatars["category"].avatar],
                organizations={companies[0].permalink: companies[0]},
            )
        },
        actions=[],
    )

    payload = dict(
        campaigns=[campaign1, campaign1_dt, campaign2],
        places={},
        search_tags=[],
        polygons={},
    )

    xml_data, attrs = await pipeline_data_transform(
        payload, navi_uri_signer, config.INSTANCE_TAG_CTYPE
    )

    assert xml_data == {
        "menu_items": [
            {
                "id": "ac_auto_log_id_campaign_1",
                "pages": ["p1", "p2"],
                "companies": [0, 1, 2],
                "style": "style icon 1",
                "title": [{"value": "icon title 1"}],
                "position": 1,
                "search_text": "icon search text 1",
            },
            {
                "id": "ac_auto_log_id_campaign_2",
                "pages": ["p1", "p2"],
                "companies": [0, 1, 2],
                "style": "style icon 2",
                "title": [{"value": "icon title 2"}],
                "position": 2,
                "search_text": "icon search text 2",
            },
        ],
        "advert_data_list": [],
        "advert_tags": [],
        "places": [],
        "polygons": [],
        "pin_data_list": [],
    }

    assert attrs == {
        "advert_tag_hash": "d41d8cd98f00b204e9800998ecf8427e",
    }
