import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    AddPointToRoute,
    OpenSite,
    PhoneCall,
)
from maps_adv.adv_store.api.proto.campaign_pb2 import (
    CampaignExport,
    CampaignExportList,
    OverviewPosition,
    Platform,
    PublicationEnv,
)
from maps_adv.adv_store.api.proto.creative_pb2 import Billboard, Creative, Image, Pin
from maps_adv.adv_store.api.proto.placing_pb2 import (
    Area,
    Organizations,
    Placing,
    Point,
    Polygon,
)
from maps_adv.adv_store.api.schemas.enums import (
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
)
from maps_adv.adv_store.client import Client, UnknownResponse
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import CampaignType

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
                            Image(
                                type="billboard",
                                group_id="some id1",
                                image_name="image name1",
                                alias_template="some template",
                            )
                        ]
                    )
                )
            ],
            actions=[
                Action(open_site=OpenSite(url="site url"), main=True),
                Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"), main=False),
                Action(
                    add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4),
                    main=False,
                ),
            ],
            order_id=3532,
            user_display_limit=12,
            user_daily_display_limit=8,
            total_daily_display_limit=10000,
            total_daily_action_limit=None,
            total_display_minutes_left_today=100,
            total_display_minutes_today=100,
            cost=0.0,
            targeting="{}",
            settings={
                "overview_position": OverviewPosition.Enum.FINISH,
                "verification_data": [],
            },
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
                )
            ],
            actions=[],
        ),
    ]
).SerializeToString()


async def test_returns_list_of_campaigns_with_data(mock_export):
    mock_export(Response(body=message, status=200))

    async with Client("http://adv_store.server") as client:
        got = await client.list_campaigns_for_export()

    assert got == {
        "campaigns": [
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
                                    {"lon": 93.23, "lat": -12.3},
                                    {"lon": 3.0, "lat": 23.0},
                                    {"lon": -30.2, "lat": 100.3},
                                ],
                                "preset_id": None,
                            },
                            {
                                "name": "kek",
                                "points": [
                                    {"lon": 30.0, "lat": 20.0},
                                    {"lon": 45.0, "lat": 95.0},
                                    {"lon": 42.0, "lat": 14.0},
                                ],
                                "preset_id": None,
                            },
                        ],
                    }
                },
                "platforms": [PlatformEnum.MAPS, PlatformEnum.NAVI],
                "creatives": [
                    {
                        "images": [
                            {
                                "type": "billboard",
                                "group_id": "some id1",
                                "image_name": "image name1",
                                "alias_template": "some template",
                            }
                        ],
                        "images_v2": [],
                        "type_": "billboard",
                    }
                ],
                "actions": [
                    {
                        "type_": "open_site",
                        "url": "site url",
                        "main": True,
                        "title": None,
                    },
                    {
                        "type_": "phone_call",
                        "phone": "+7-xxx-xxx-xx",
                        "main": False,
                        "title": None,
                    },
                    {
                        "type_": "add_point_to_route",
                        "latitude": 1.2,
                        "longitude": 3.4,
                        "main": False,
                    },
                ],
                "order_id": 3532,
                "user_display_limit": 12,
                "user_daily_display_limit": 8,
                "targeting": {},
                "settings": {
                    "overview_position": OverviewPositionEnum.FINISH,
                    "verification_data": [],
                },
                "total_daily_display_limit": 10000,
                "total_daily_action_limit": None,
                "total_display_minutes_left_today": 100,
                "total_display_minutes_today": 100,
                "cost": 0.0,
            },
            {
                "id": 9734,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
                "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE,
                "placing": {
                    "organizations": {"permalinks": [8345, 23523, 13532, 253246]}
                },
                "platforms": [PlatformEnum.METRO],
                "creatives": [
                    {
                        "images": [
                            {
                                "type": "pin",
                                "group_id": "some id2",
                                "image_name": "image name2",
                            }
                        ],
                        "title": "pin title",
                        "subtitle": "pin subtitle",
                        "type_": "pin",
                    }
                ],
                "actions": [],
                "targeting": {},
                "total_daily_display_limit": None,
                "total_daily_action_limit": None,
                "total_display_minutes_left_today": None,
                "total_display_minutes_today": None,
            },
        ]
    }


async def test_returns_empty_list_if_no_campaigns(mock_export):
    mock_export(
        Response(body=CampaignExportList(campaigns=[]).SerializeToString(), status=200)
    )

    async with Client("http://adv_store.server") as client:
        got = await client.list_campaigns_for_export()

    assert got == {"campaigns": []}


async def test_raises_for_unknown_error(mock_export):
    mock_export(Response(body=b"", status=409))

    async with Client("http://adv_store.server") as client:
        with pytest.raises(UnknownResponse) as exc_info:
            await client.list_campaigns_for_export()

    assert 409 in exc_info.value.args
