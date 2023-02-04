import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.action_pb2 import Action, OpenSite, PhoneCall
from maps_adv.adv_store.api.proto.campaign_pb2 import (
    CampaignExport,
    CampaignExportList,
    CampaignSettings,
    Platform,
    PublicationEnv,
    VerificationData,
)
from maps_adv.adv_store.api.proto.creative_pb2 import (
    Billboard,
    Creative,
    Image,
    Pin,
    PinSearch,
)
from maps_adv.adv_store.api.proto.placing_pb2 import (
    Area,
    Organizations,
    Placing,
    Point,
    Polygon,
)
from maps_adv.adv_store.api.schemas.enums import (
    PublicationEnvEnum,
)
from maps_adv.adv_store.api.schemas.enums import PlatformEnum
from maps_adv.adv_store.client import CampaignsNotFound
from maps_adv.common.proto.campaign_pb2 import CampaignType
from maps_adv.export.lib.core.enum import (
    ActionType as ActionTypeEnum,
    CampaignType as CampaignTypeEnum,
    CreativeType as CreativeTypeEnum,
    ImageType as ImageTypeEnum,
)
from maps_adv.export.lib.pipeline.steps import FetchCampaignsStep

pytestmark = [pytest.mark.asyncio]


message = CampaignExportList(
    campaigns=[
        # campaign with all fields
        CampaignExport(
            id=4355,
            publication_envs=[PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.BILLBOARD,
            placing=Placing(
                area=Area(
                    version=14,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        ),
                        Polygon(
                            points=[
                                Point(lon=30, lat=20),
                                Point(lon=45, lat=95),
                                Point(lon=42, lat=14),
                            ],
                            name="kek",
                        ),
                    ],
                )
            ),
            platforms=[Platform.MAPS, Platform.NAVI],
            creatives=[
                Creative(
                    billboard=Billboard(
                        images=[
                            # Special image with impossible type for checking
                            # correct processing it situation
                            Image(
                                type="#ImpossibleImageType",
                                group_id="some id1",
                                image_name="image name1",
                                alias_template="some template1",
                            ),
                            Image(
                                type="banner",
                                group_id="some id1",
                                image_name="image name1",
                                alias_template="some template1",
                            ),
                        ],
                    ),
                ),
                Creative(
                    billboard=Billboard(
                        images_v2=[
                            # Special image with impossible type for checking
                            # correct processing it situation
                            Image(
                                type="#ImpossibleImageType",
                                group_id="some id1",
                                image_name="image name1",
                                alias_template="some template1",
                            ),
                            Image(
                                type="banner",
                                group_id="some id1",
                                image_name="image name1",
                                alias_template="some template1",
                            ),
                        ],
                        title="title",
                        description="description",
                    ),
                ),
            ],
            actions=[
                Action(
                    open_site=OpenSite(url="site url"), title="some title", main=True
                ),
                Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"), main=False),
            ],
            order_id=3532,
            user_display_limit=12,
            user_daily_display_limit=8,
            targeting="{}",
            settings=CampaignSettings(
                custom_page_id="abc",
                verification_data=[
                    VerificationData(platform="weborama", params={"a": "b", "x": "y"}),
                    VerificationData(platform="weborama", params={"a": "1", "x": "2"}),
                ],
            ),
            total_daily_display_limit=100,
            total_display_minutes_left_today=100,
            total_display_minutes_today=100,
        ),
        # campaign with only required fields
        CampaignExport(
            id=9734,
            publication_envs=[PublicationEnv.PRODUCTION],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            placing=Placing(
                organizations=Organizations(permalinks=[8345, 23523, 13532, 253246])
            ),
            platforms=[Platform.METRO],
            creatives=[
                Creative(
                    pin=Pin(
                        images=[
                            Image(
                                type="pin",
                                group_id="some id2",
                                image_name="image name2",
                            )
                        ],
                        title="pin title",
                        subtitle="pin subtitle",
                    )
                ),
                Creative(
                    pin_search=PinSearch(
                        images=[
                            Image(
                                type="category",
                                group_id="some id3",
                                image_name="image name3",
                            ),
                            Image(
                                type="dust",
                                group_id="some id4",
                                image_name="image name4",
                            ),
                        ],
                        title="pin_search title 1",
                        organizations=[9234, 3256, 236345],
                    )
                ),
                Creative(
                    pin_search=PinSearch(
                        images=[
                            Image(
                                type="pin_left",
                                group_id="some id5",
                                image_name="image name5",
                                alias_template="some template5",
                            ),
                            Image(
                                type="pin_right",
                                group_id="some id6",
                                image_name="image name6",
                                alias_template="some template6",
                            ),
                        ],
                        title="pin_search title 2",
                        organizations=[23364],
                    )
                ),
            ],
            actions=[],
        ),
    ]
).SerializeToString()


async def test_returns_transformed_adv_store_data(config, mock_adv_store):
    mock_adv_store(Response(body=message, status=200))

    step = FetchCampaignsStep(config)
    got = await step([])

    assert got == [
        {
            "id": 4355,
            "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            "campaign_type": CampaignTypeEnum.BILLBOARD,
            "placing": {
                "area": {
                    "version": 14,
                    "areas": [
                        {
                            "name": "kek",
                            "points": [
                                {"longitude": 93.23, "latitude": -12.3},
                                {"longitude": 3.0, "latitude": 23.0},
                                {"longitude": -30.2, "latitude": 100.3},
                            ],
                            "preset_id": None,
                        },
                        {
                            "name": "kek",
                            "points": [
                                {"longitude": 30.0, "latitude": 20.0},
                                {"longitude": 45.0, "latitude": 95.0},
                                {"longitude": 42.0, "latitude": 14.0},
                            ],
                            "preset_id": None,
                        },
                    ],
                },
            },
            "platforms": [PlatformEnum.MAPS, PlatformEnum.NAVI],
            "creatives": {
                CreativeTypeEnum.BILLBOARD: [
                    {
                        "images": [
                            {
                                "type": ImageTypeEnum.BANNER,
                                "group_id": "some id1",
                                "image_name": "image name1",
                                "alias_template": "some template1",
                            }
                        ],
                        "images_v2": [],
                    },
                    {
                        "images": [],
                        "images_v2": [
                            {
                                "type": ImageTypeEnum.BANNER,
                                "group_id": "some id1",
                                "image_name": "image name1",
                                "alias_template": "some template1",
                            }
                        ],
                        "title": "title",
                        "description": "description",
                    },
                ],
            },
            "actions": [
                {
                    "type": ActionTypeEnum.OPEN_SITE,
                    "url": "site url",
                    "title": "some title",
                    "main": True,
                },
                {
                    "type": ActionTypeEnum.PHONE_CALL,
                    "phone": "+7-xxx-xxx-xx",
                    "title": None,
                    "main": False,
                },
            ],
            "order_id": 3532,
            "user_display_limit": 12,
            "user_daily_display_limit": 8,
            "settings": {
                "custom_page_id": "abc",
                "verification_data": [
                    {
                        "platform": "weborama",
                        "params": {
                            "a": "b",
                            "x": "y",
                        },
                    },
                    {
                        "platform": "weborama",
                        "params": {
                            "a": "1",
                            "x": "2",
                        },
                    },
                ],
            },
            "targeting": {},
            "places": [],
            "companies": [],
            "total_daily_display_limit": 100,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 100,
            "total_display_minutes_today": 100,
        },
        {
            "id": 9734,
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [8345, 23523, 13532, 253246]}},
            "platforms": [PlatformEnum.METRO],
            "creatives": {
                CreativeTypeEnum.PIN: {
                    "images": [
                        {
                            "type": ImageTypeEnum.PIN,
                            "group_id": "some id2",
                            "image_name": "image name2",
                        }
                    ],
                    "title": "pin title",
                    "subtitle": "pin subtitle",
                },
                CreativeTypeEnum.PIN_SEARCH: [
                    {
                        "images": [
                            {
                                "type": ImageTypeEnum.CATEGORY,
                                "group_id": "some id3",
                                "image_name": "image name3",
                            },
                            {
                                "type": ImageTypeEnum.DUST,
                                "group_id": "some id4",
                                "image_name": "image name4",
                            },
                        ],
                        "title": "pin_search title 1",
                        "organizations": [9234, 3256, 236345],
                    },
                    {
                        "images": [
                            {
                                "type": ImageTypeEnum.PIN_LEFT,
                                "group_id": "some id5",
                                "image_name": "image name5",
                                "alias_template": "some template5",
                            },
                            {
                                "type": ImageTypeEnum.PIN_RIGHT,
                                "group_id": "some id6",
                                "image_name": "image name6",
                                "alias_template": "some template6",
                            },
                        ],
                        "title": "pin_search title 2",
                        "organizations": [23364],
                    },
                ],
            },
            "settings": {},
            "targeting": {},
            "actions": [],
            "places": [],
            "companies": [],
            "total_daily_display_limit": None,
            "total_daily_action_limit": None,
            "total_display_minutes_today": None,
            "total_display_minutes_left_today": None,
        },
    ]


async def test_returns_empty_list_if_no_campaigns(config, mock_adv_store):
    mock_adv_store(
        Response(body=CampaignExportList(campaigns=[]).SerializeToString(), status=200)
    )

    step = FetchCampaignsStep(config)
    got = await step([])

    assert got == []


async def test_raises_campaigns_not_found(config, mock_adv_store):
    mock_adv_store(Response(body=b"", status=404))

    step = FetchCampaignsStep(config)
    with pytest.raises(CampaignsNotFound) as exc_info:
        await step([])

    assert "" in exc_info.value.args
