import json
import os
import tempfile
from aiohttp.web import Response
from asyncio import coroutine
from collections import defaultdict
from unittest.mock import Mock

import pytest

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    ActionType,
    AddPointToRoute,
    Dialog,
    OpenSite,
    PhoneCall,
    Promocode,
    ResolveUri,
    Search,
)
from maps_adv.adv_store.api.proto.campaign_pb2 import (
    CampaignExport,
    CampaignExportList,
    CampaignSettings,
    OverviewPosition,
    Platform,
    PublicationEnv,
    VerificationData,
)
from maps_adv.adv_store.api.proto.creative_pb2 import (
    Banner,
    Billboard,
    Creative,
    Icon,
    Image,
    Pin,
    PinSearch,
    Text,
    ViaPoint,
)
from maps_adv.adv_store.api.proto.placing_pb2 import (
    Area,
    Organizations,
    Placing,
    Point,
    Polygon,
)
from maps_adv.adv_store.api.schemas.enums import PublicationEnvEnum
from maps_adv.billing_proxy.proto.orders_pb2 import OrderIds
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrderDiscountInfo,
    OrdersDiscountInfo,
)
from maps_adv.common.proto.campaign_pb2 import CampaignType
from maps_adv.export.lib.pipeline.base import export
from maps_adv.points.proto import points_in_polygons_pb2, primitives_pb2
from maps_adv.statistics.dashboard.proto.campaign_stat_pb2 import (
    CampaignEvents,
    CampaignEventsForPeriodOutput,
)

pytestmark = [pytest.mark.asyncio]

pin_search_images = [
    Image(
        type="dust",
        group_id="g100",
        image_name="dust_name_1",
        alias_template="alias_dust_{zoom}",
    ),
    Image(
        type="dust_hover",
        group_id="g100",
        image_name="dust_name_2",
        alias_template="alias_dust_hover",
    ),
    Image(
        type="dust_visited",
        group_id="g100",
        image_name="dust_name_2",
        alias_template="alias_dust_visited",
    ),
    Image(
        type="pin",
        group_id="g100",
        image_name="pin_name_1",
        alias_template="alias_pin_{zoom}",
    ),
    Image(
        type="pin_hover",
        group_id="g100",
        image_name="pin_name_2",
        alias_template="alias_pin_hover",
    ),
    Image(
        type="pin_visited",
        group_id="g100",
        image_name="pin_name_3",
        alias_template="alias_pin_visited",
    ),
    Image(
        type="pin_selected",
        group_id="g100",
        image_name="pin_name_4",
        alias_template="alias_pin_selected_{zoom}",
    ),
    Image(
        type="pin_round",
        group_id="g100",
        image_name="pin_name_5",
        alias_template="alias_pin_round",
    ),
]

route_via_point_images = pin_on_route_images = [
    Image(
        type="dust",
        group_id="g102",
        image_name="dust_name_1",
        alias_template="alias_dust_{zoom}",
    ),
    Image(
        type="dust_hover",
        group_id="g102",
        image_name="dust_name_2",
        alias_template="alias_dust_hover",
    ),
    Image(
        type="dust_visited",
        group_id="g102",
        image_name="dust_name_3",
        alias_template="alias_dust_visited",
    ),
    Image(
        type="pin",
        group_id="g102",
        image_name="pin_name_1",
        alias_template="alias_pin_{zoom}",
    ),
    Image(
        type="pin_hover",
        group_id="g102",
        image_name="pin_name_2",
        alias_template="alias_pin_hover",
    ),
    Image(
        type="pin_visited",
        group_id="g102",
        image_name="pin_name_3",
        alias_template="alias_pin_visited",
    ),
    Image(
        type="pin_selected",
        group_id="g102",
        image_name="pin_name_4",
        alias_template="alias_pin_selected_{zoom}",
    ),
    Image(
        type="pin_round",
        group_id="g102",
        image_name="pin_name_5",
        alias_template="alias_pin_round",
    ),
    Image(
        type="pin_left",
        group_id="g102",
        image_name="pin_name_6",
        alias_template="alias_pin_left",
    ),
    Image(
        type="pin_right",
        group_id="g102",
        image_name="pin_name_7",
        alias_template="alias_pin_right",
    ),
]

adv_store_proto_message = CampaignExportList(
    campaigns=[
        # Campaign CATEGORY_SEARCH
        CampaignExport(
            id=1355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[7])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 1355",
                        position=35,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_1",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(
                    text=Text(text="text value 1355", disclaimer="text disclaimer 1355")
                ),
                Creative(
                    pin_search=PinSearch(organizations=[1, 2], images=pin_search_images)
                ),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 1355/2",
                        organizations=[3],
                        images=pin_search_images,
                    )
                ),
            ],
            actions=[],
            order_id=3532,
        ),
        CampaignExport(
            id=1356,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[])),
            platforms=[Platform.MAPS],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 1356",
                        position=36,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_2",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(text=Text(text="text value 1356", disclaimer="")),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 1356/1",
                        organizations=[5],
                        images=pin_search_images
                        + [
                            Image(
                                type="logo",
                                group_id="g100",
                                image_name="logo_name_1",
                                alias_template="alias_logo_{zoom}",
                            ),
                            Image(
                                type="banner",
                                group_id="g100",
                                image_name="banner_name_2",
                                alias_template="alias_banner_{zoom}",
                            ),
                        ],
                    )
                ),
            ],
            actions=[],
            order_id=3532,
        ),
        CampaignExport(
            id=1357,
            publication_envs=[PublicationEnv.PRODUCTION],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 1357",
                        position=35,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_1",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(
                    text=Text(text="text value 1357", disclaimer="text disclaimer 1357")
                ),
                Creative(
                    pin_search=PinSearch(organizations=[1, 2], images=pin_search_images)
                ),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 1357/2",
                        organizations=[3],
                        images=pin_search_images,
                    )
                ),
            ],
            actions=[],
            order_id=3532,
        ),
        CampaignExport(  # the same campaign as above but for data testing; should be deduplicated  # noqa: E501
            id=1357,
            publication_envs=[PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 1357",
                        position=35,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_1",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(
                    text=Text(text="text value 1357", disclaimer="text disclaimer 1357")
                ),
                Creative(
                    pin_search=PinSearch(organizations=[1, 2], images=pin_search_images)
                ),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 1357/2",
                        organizations=[3],
                        images=pin_search_images,
                    )
                ),
            ],
            actions=[],
            order_id=3532,
        ),
        CampaignExport(  # For testing monkey patch exclude category campaigns
            id=200000,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[])),
            platforms=[Platform.MAPS],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 200000",
                        position=36,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_2",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(text=Text(text="text value 200000", disclaimer="")),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 200000/1",
                        organizations=[1],
                        images=pin_search_images
                        + [
                            Image(
                                type="logo",
                                group_id="g100",
                                image_name="logo_name_1",
                                alias_template="alias_logo_{zoom}",
                            ),
                            Image(
                                type="banner",
                                group_id="g100",
                                image_name="banner_name_2",
                                alias_template="alias_banner_{zoom}",
                            ),
                        ],
                    )
                ),
            ],
            actions=[],
            order_id=3532,
        ),
        # Campaign ZERO_SPEED_BANNER
        CampaignExport(
            id=2355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            placing=Placing(
                area=Area(
                    version=2,
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
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    banner=Banner(
                        disclaimer="zero_speed_banner banner disclaimer",
                        show_ads_label=True,
                        title="zero_speed_banner banner title",
                        description="zero_speed_banner banner description",
                        terms="zero_speed_banner banner terms",
                        images=[
                            Image(
                                type="banner",
                                group_id="g101",
                                image_name="banner_name_1",
                                alias_template="alias_banner",
                            )
                        ],
                    )
                )
            ],
            actions=[
                Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"), main=True),
                Action(
                    resolve_uri=ResolveUri(
                        uri="https://uri", action_type=ActionType.OPEN_SITE
                    )
                ),
                Action(add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4)),
            ],
            targeting=json.dumps(
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [{"tag": "income", "content": ["high", "middle"]}],
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "542", "keywordId": "216"},
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "1031", "keywordId": "547"},
                        },
                    ],
                }
            ),
            order_id=3532,
            user_daily_display_limit=2,
            total_daily_display_limit=20000,
            settings=CampaignSettings(
                verification_data=[
                    VerificationData(
                        platform="weborama",
                        params={
                            "account": "1",
                            "tte": "2",
                            "app": "3",
                            "ignored_param": "4",
                        },
                    ),
                    VerificationData(
                        platform="dcm",
                        params={"url": "https://ad.doubleclick.net/dcm_path"},
                    ),
                ]
            ),
        ),
        # Campaign PIN_ON_ROUTE
        CampaignExport(
            id=3355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            placing=Placing(organizations=Organizations(permalinks=[6, 7])),
            platforms=[Platform.NAVI, Platform.MAPS],
            creatives=[
                Creative(
                    pin=Pin(
                        title="pin_on_route pin title",
                        subtitle="pin_on_route pin subtitle",
                        images=pin_on_route_images,
                    )
                ),
                Creative(
                    banner=Banner(
                        title="pin_on_route banner title",
                        disclaimer="pin_on_route banner disclaimer",
                        show_ads_label=True,
                        images=[
                            Image(
                                type="banner",
                                group_id="g102",
                                image_name="banner_name_1",
                                alias_template="alias_banner_{zoom}",
                            )
                        ],
                    )
                ),
            ],
            actions=[Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"))],
            order_id=3532,
            display_chance=555,
            total_daily_display_limit=10000,
            cost=0.0,
        ),
        # Campaign BILLBOARD
        CampaignExport(
            id=4355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.BILLBOARD,
            placing=Placing(
                area=Area(
                    version=4,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        )
                    ],
                )
            ),
            platforms=[Platform.NAVI, Platform.MAPS],
            creatives=[
                Creative(
                    billboard=Billboard(
                        images=[
                            Image(
                                type="pin",
                                group_id="g104",
                                image_name="pin_name_1",
                                alias_template="alias_pin_billboard",
                            )
                        ]
                    )
                ),
                Creative(
                    billboard=Billboard(
                        images_v2=[
                            Image(
                                type="pin",
                                group_id="g104",
                                image_name="pin_name_1",
                                alias_template="alias_pin_billboard",
                            )
                        ],
                        title="title",
                        description="description",
                    )
                ),
                Creative(
                    banner=Banner(
                        title="billboard banner title",
                        description="billboard banner description",
                        disclaimer="billboard banner disclaimer",
                        show_ads_label=True,
                        images=[
                            Image(
                                type="banner",
                                group_id="g104",
                                image_name="banner_name_1",
                                alias_template="alias_banner_{zoom}",
                            )
                        ],
                    )
                ),
            ],
            actions=[
                Action(
                    title="action title/1",
                    search=Search(history_text="history text", organizations=[5]),
                ),
                Action(
                    title="action title/2", open_site=OpenSite(url="http://example.com")
                ),
            ],
            order_id=5124,
            total_daily_display_limit=20000,
        ),
        # Campaign ROUTE_BANNER
        CampaignExport(
            id=5355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.ROUTE_BANNER,
            placing=Placing(
                area=Area(
                    version=4,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        )
                    ],
                )
            ),
            platforms=[Platform.METRO],
            creatives=[
                Creative(
                    banner=Banner(
                        title="metro banner title",
                        disclaimer="metro banner disclaimer",
                        description="metro banner description",
                        show_ads_label=True,
                        images=[
                            Image(
                                type="banner",
                                group_id="g105",
                                image_name="banner_name_1",
                                alias_template="alias_banner_{zoom}",
                            ),
                            Image(
                                type="big_banner",
                                group_id="g105",
                                image_name="big_banner_name_1",
                                alias_template="alias_big_banner",
                            ),
                        ],
                    )
                )
            ],
            actions=[],
            order_id=5124,
            display_probability="0.123456789",
            total_daily_display_limit=None,
        ),
        # Campaign ROUTE_VIA_POINT
        CampaignExport(
            id=6355,
            # Comeback TESTING env if remove campaign 6356
            publication_envs=[PublicationEnv.PRODUCTION],
            campaign_type=CampaignType.VIA_POINTS,
            placing=Placing(organizations=Organizations(permalinks=[6, 7])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    via_point=ViaPoint(
                        button_text_active="route_via_point active text",
                        button_text_inactive="route_via_point inactive text",
                        description="route_via_point description",
                        images=[
                            Image(
                                type="pin",
                                group_id="g106",
                                image_name="pin_name_1",
                                alias_template="route_via_point_pin_{size}",
                            )
                        ],
                    )
                )
            ],
            order_id=3532,
            total_daily_display_limit=20000,
        ),
        CampaignExport(  # Experiment with remove stylePin field
            id=6356,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.VIA_POINTS,
            placing=Placing(organizations=Organizations(permalinks=[6, 7])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    via_point=ViaPoint(
                        button_text_active="route_via_point active text",
                        button_text_inactive="route_via_point inactive text",
                        description="route_via_point description",
                        images=[
                            Image(
                                type="pin",
                                group_id="g106",
                                image_name="pin_name_1",
                                alias_template="route_via_point_pin_{size}",
                            )
                        ],
                    )
                )
            ],
            order_id=3532,
            total_daily_display_limit=None,
        ),
        # Campaign OVERVIEW_BANNER
        CampaignExport(
            id=7355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.OVERVIEW_BANNER,
            placing=Placing(
                area=Area(
                    version=2,
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
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    banner=Banner(
                        disclaimer="overview_banner banner disclaimer",
                        show_ads_label=True,
                        title="overview_banner banner title",
                        description="overview_banner banner description",
                        images=[
                            Image(
                                type="banner",
                                group_id="g101",
                                image_name="banner_name_1",
                                alias_template="alias_banner",
                            )
                        ],
                    )
                )
            ],
            actions=[],
            targeting=json.dumps(
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [{"tag": "income", "content": ["high", "middle"]}],
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "542", "keywordId": "216"},
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "1031", "keywordId": "547"},
                        },
                    ],
                }
            ),
            order_id=3532,
            user_daily_display_limit=2,
            total_daily_display_limit=20000,
            settings=CampaignSettings(
                verification_data=[
                    VerificationData(
                        platform="weborama",
                        params={
                            "account": "1",
                            "tte": "2",
                            "app": "3",
                            "ignored_param": "4",
                        },
                    ),
                ]
            ),
        ),
        # Overview banner with Resolve URI action
        CampaignExport(
            id=7356,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.OVERVIEW_BANNER,
            placing=Placing(
                area=Area(
                    version=2,
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
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    banner=Banner(
                        disclaimer="overview_banner banner disclaimer 2",
                        show_ads_label=True,
                        title="overview_banner banner title 2",
                        description="overview_banner banner description 2",
                        images=[
                            Image(
                                type="banner",
                                group_id="g101",
                                image_name="banner_name_2",
                                alias_template="alias_banner",
                            )
                        ],
                    )
                )
            ],
            actions=[
                Action(
                    resolve_uri=ResolveUri(
                        uri="https://uri", action_type=ActionType.OPEN_SITE
                    )
                ),
                Action(
                    resolve_uri=ResolveUri(
                        uri="https://example.com",
                        action_type=ActionType.OPEN_SITE,
                        target=ResolveUri.Target.BROWSER,
                        dialog=Dialog(
                            content="Давайте установим автору",
                            title="Установка приложения",
                            ok="Установить",
                            cancel="Позже",
                            event_ok="SomeTextOk",
                            event_cancel="SomeTextCancel",
                        ),
                    )
                ),
            ],
            targeting=json.dumps(
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [{"tag": "income", "content": ["high", "middle"]}],
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "542", "keywordId": "216"},
                        },
                        {
                            "tag": "segment",
                            "not": True,
                            "attributes": {"id": "1031", "keywordId": "547"},
                        },
                    ],
                }
            ),
            order_id=3532,
            user_daily_display_limit=2,
            total_daily_display_limit=20000,
            settings=CampaignSettings(overview_position=OverviewPosition.Enum.ALL),
        ),
        CampaignExport(
            id=8355,
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.PROMOCODE,
            placing=Placing(
                area=Area(
                    version=4,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        )
                    ],
                )
            ),
            platforms=[Platform.METRO],
            creatives=[
                Creative(
                    banner=Banner(
                        title="metro promocode title",
                        disclaimer="metro promocode disclaimer",
                        description="metro promocode description",
                        show_ads_label=True,
                        terms="metro promocode terms",
                        images=[
                            Image(
                                type="logo",
                                group_id="l105",
                                image_name="logo_name_1",
                                alias_template="alias_logo_{zoom}",
                            ),
                            Image(
                                type="banner",
                                group_id="g105",
                                image_name="banner_name_1",
                                alias_template="alias_banner_{zoom}",
                            ),
                        ],
                    )
                )
            ],
            actions=[Action(promocode=Promocode(promocode="promo123"))],
            order_id=5124,
            display_probability="0.123456789",
        ),
        CampaignExport(
            id=9355,
            # Only DATA_TESTING should remain due to display limits
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            placing=Placing(
                area=Area(
                    version=2,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        ),
                    ],
                )
            ),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    banner=Banner(
                        disclaimer="zero_speed_banner banner disclaimer",
                        show_ads_label=True,
                        title="zero_speed_banner banner title",
                        description="zero_speed_banner banner description",
                        terms="zero_speed_banner banner terms",
                        images=[
                            Image(
                                type="banner",
                                group_id="g101",
                                image_name="banner_name_1",
                                alias_template="alias_banner",
                            )
                        ],
                    )
                )
            ],
            actions=[
                Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"), main=True),
            ],
            targeting=json.dumps(
                {
                    "tag": "and",
                }
            ),
            order_id=3532,
            user_daily_display_limit=1,
            total_daily_display_limit=1,
        ),
        CampaignExport(
            id=9356,
            # Should be removed due to total_daily_display divided by 2 down to 7
            publication_envs=[PublicationEnv.PRODUCTION],
            campaign_type=CampaignType.ZERO_SPEED_BANNER,
            placing=Placing(
                area=Area(
                    version=2,
                    areas=[
                        Polygon(
                            points=[
                                Point(lon=93.23, lat=-12.3),
                                Point(lon=3.0, lat=23.0),
                                Point(lon=-30.20, lat=100.30),
                            ],
                            name="kek",
                        ),
                    ],
                )
            ),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    banner=Banner(
                        disclaimer="zero_speed_banner banner disclaimer",
                        show_ads_label=True,
                        title="zero_speed_banner banner title",
                        description="zero_speed_banner banner description",
                        terms="zero_speed_banner banner terms",
                        images=[
                            Image(
                                type="banner",
                                group_id="g101",
                                image_name="banner_name_1",
                                alias_template="alias_banner",
                            )
                        ],
                    )
                )
            ],
            actions=[
                Action(phone_call=PhoneCall(phone="+7-xxx-xxx-xx"), main=True),
            ],
            targeting=json.dumps(
                {
                    "tag": "and",
                }
            ),
            order_id=9999,
            user_daily_display_limit=1,
            total_daily_display_limit=15,
        ),
        # campaigns for StepExceptions tests
        CampaignExport(
            id=10001,
            # bad custom page id
            publication_envs=[PublicationEnv.PRODUCTION, PublicationEnv.DATA_TESTING],
            campaign_type=CampaignType.CATEGORY_SEARCH,
            placing=Placing(organizations=Organizations(permalinks=[7])),
            platforms=[Platform.NAVI],
            creatives=[
                Creative(
                    icon=Icon(
                        title="icon title 1355",
                        position=35,
                        images=[
                            Image(
                                type="category",
                                group_id="g100",
                                image_name="category_name_1",
                                alias_template="alias_category",
                            )
                        ],
                    )
                ),
                Creative(
                    text=Text(text="text value 1355", disclaimer="text disclaimer 1355")
                ),
                Creative(
                    pin_search=PinSearch(organizations=[1, 2], images=pin_search_images)
                ),
                Creative(
                    pin_search=PinSearch(
                        title="pin_search title 1355/2",
                        organizations=[3],
                        images=pin_search_images,
                    )
                ),
            ],
            actions=[],
            order_id=3532,
            settings=CampaignSettings(custom_page_id="Bad_PAGE-iD"),
        ),
    ]
).SerializeToString()

billing_proto_message = OrderIds(order_ids=[5124, 3532]).SerializeToString()
billing_orders_discounts_message = OrdersDiscountInfo(
    discount_info=[
        OrderDiscountInfo(order_id=9999, discount="2.0"),
    ]
).SerializeToString()
dashboard_proto_message = CampaignEventsForPeriodOutput(
    campaigns_events=[
        CampaignEvents(campaign_id=1355, events=0),
        CampaignEvents(campaign_id=1356, events=0),
        CampaignEvents(campaign_id=1357, events=0),
        CampaignEvents(campaign_id=200000, events=0),
        CampaignEvents(campaign_id=2355, events=0),
        CampaignEvents(campaign_id=3355, events=0),
        CampaignEvents(campaign_id=4355, events=0),
        CampaignEvents(campaign_id=5355, events=0),
        CampaignEvents(campaign_id=6355, events=0),
        CampaignEvents(campaign_id=6356, events=0),
        CampaignEvents(campaign_id=7355, events=0),
        CampaignEvents(campaign_id=7356, events=1000),
        CampaignEvents(campaign_id=8355, events=0),
        CampaignEvents(campaign_id=9355, events=1),
        CampaignEvents(campaign_id=9356, events=10),
        CampaignEvents(campaign_id=10001, events=0),
    ]
).SerializeToString()
points_proto_message = points_in_polygons_pb2.PointsInPolygonsOutput(
    points=[
        primitives_pb2.IdentifiedPoint(
            id=100000, longitude="38.7261199951", latitude="55.8496856689"
        ),
        primitives_pb2.IdentifiedPoint(
            id=100001, longitude="38.7261199951", latitude="55.8496856689"
        ),
        primitives_pb2.IdentifiedPoint(
            id=100002, longitude="38.7261199951", latitude="55.8496856689"
        ),
    ]
).SerializeToString()
old_geoadv_json_response = {
    "orgs": [
        {
            "address": "organization address 1",
            "chain_id": 1417549642,
            "geo_id": 36,
            "latitude": 45.044921,
            "longitude": 41.970578,
            "name": "organization title 1",
            "permalink": 1,
            "providers": [
                "burgerking:i608",
                "edadeal:37c69999-2008-4efa-b092-334e119b8cd8",
                "tripadvisor:13139561",
                "zoon:5a3bfe15a24fd925c23e749c",
            ],
        },
        {
            "address": "organization address 2",
            "chain_id": 49194079143,
            "geo_id": 104673,
            "latitude": 39.572656,
            "longitude": 32.1223651,
            "name": "organization title 2",
            "permalink": 2,
            "providers": ["bridgestone_tr:bridgestone0394"],
        },
        {
            "address": "organization address 3",
            "chain_id": 122181296232,
            "geo_id": 106120,
            "latitude": 41.04977073,
            "longitude": 28.8138199,
            "name": "organization title 3",
            "permalink": 3,
            "providers": ["tr-pedestrians:737031517"],
        },
        {
            "address": "organization address 4",
            "chain_id": 6003206,
            "geo_id": 10727,
            "latitude": 55.371772,
            "longitude": 39.058628,
            "name": "organization title 4",
            "permalink": 4,
            "providers": [
                "aqua_pyaterochka:c0dc42568056f17b95ba6cf44c33b6b9b653c1b3",
                "edadeal:4c8f07bf-b248-42ba-b902-d2938482d39e",
                "pyaterochka:X288",
                "tornadosp:11318",
            ],
        },
        {
            "address": "organization address 5",
            "chain_id": 6003206,
            "geo_id": 10727,
            "latitude": 55.371772,
            "longitude": 39.058628,
            "name": "organization title 5",
            "permalink": 5,
            "providers": [
                "aqua_pyaterochka:c0dc42568056f17b95ba6cf44c33b6b9b653c1b3",
                "edadeal:4c8f07bf-b248-42ba-b902-d2938482d39e",
                "pyaterochka:X288",
                "tornadosp:11318",
            ],
        },
        {
            "address": "organization address 6",
            "chain_id": 6003206,
            "geo_id": 10727,
            "latitude": 55.371772,
            "longitude": 39.058628,
            "name": "organization title 6",
            "permalink": 6,
            "providers": [
                "aqua_pyaterochka:c0dc42568056f17b95ba6cf44c33b6b9b653c1b3",
                "edadeal:4c8f07bf-b248-42ba-b902-d2938482d39e",
                "pyaterochka:X288",
                "tornadosp:11318",
            ],
        },
        {
            "address": "organization address 7",
            "chain_id": 6003206,
            "geo_id": 10727,
            "latitude": 55.371772,
            "longitude": 39.058628,
            "name": "organization title 7",
            "permalink": 7,
            "providers": [
                "aqua_pyaterochka:c0dc42568056f17b95ba6cf44c33b6b9b653c1b3",
                "edadeal:4c8f07bf-b248-42ba-b902-d2938482d39e",
                "pyaterochka:X288",
                "tornadosp:11318",
            ],
        },
    ],
    "chains": [],
}

expected_big_xml = """<?xml version='1.0' encoding='utf-8'?>
<AdvertDataList xmlns="http://maps.yandex.ru/advert/1.x">
  <MenuItems>
    <MenuItem id="ac_auto_log_id_campaign_1355">
      <pageId>navi_menu_icon_1</pageId>
      <pageId>navi_menu_icon_1/datatesting</pageId>
      <style>test-avatars-namespace--g100--category_name_1</style>
      <title>icon title 1355</title>
      <searchText>{"text": "", "ad": {"advert_tag_id": "search_tag-1355-a665a45920422f9d417e4867efdc4fb8"}}</searchText>
      <position>35</position>
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
        <id>7</id>
      </Companies>
    </MenuItem>
    <MenuItem id="ac_auto_log_id_campaign_1356">
      <pageId>mobile_maps_menu_icon_1</pageId>
      <pageId>mobile_maps_menu_icon_1/datatesting</pageId>
      <style>test-avatars-namespace--g100--category_name_2</style>
      <title>icon title 1356</title>
      <searchText>{"text": "", "ad": {"advert_tag_id": "search_tag-1356-e629fa6598d732768f7c726b4b621285"}}</searchText>
      <position>36</position>
      <Companies>
        <id>1</id>
        <id>5</id>
      </Companies>
    </MenuItem>
    <MenuItem id="ac_auto_log_id_campaign_1357">
      <pageId>navi_menu_icon_1</pageId>
      <style>test-avatars-namespace--g100--category_name_1</style>
      <title>icon title 1357</title>
      <searchText>{"text": "", "ad": {"advert_tag_id": "search_tag-1357-a665a45920422f9d417e4867efdc4fb8"}}</searchText>
      <position>35</position>
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
      </Companies>
    </MenuItem>
    <MenuItem id="ac_auto_log_id_campaign_1357_data_testing">
      <pageId>navi_menu_icon_1/datatesting</pageId>
      <style>test-avatars-namespace--g100--category_name_1</style>
      <title>icon title 1357</title>
      <searchText>{"text": "", "ad": {"advert_tag_id": "search_tag-1357-a665a45920422f9d417e4867efdc4fb8"}}</searchText>
      <position>35</position>
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
      </Companies>
    </MenuItem>
  </MenuItems>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1355</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 1</title>
      <text>text value 1355</text>
      <disclaimer>text disclaimer 1355</disclaimer>
    </Advert>
    <Companies>
      <id>1</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1355</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 2</title>
      <text>text value 1355</text>
      <disclaimer>text disclaimer 1355</disclaimer>
    </Advert>
    <Companies>
      <id>2</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1355</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 1355/2</title>
      <text>text value 1355</text>
      <disclaimer>text disclaimer 1355</disclaimer>
    </Advert>
    <Companies>
      <id>3</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>mobile_maps_menu_icon_1</pageId>
    <logId>ac_auto_log_id_campaign_1356</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.89</field>
      <field name="anchorIconHover">0.5 0.89</field>
      <field name="anchorIconVisited">0.5 0.89</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 38</field>
      <field name="sizeIconHover">32 38</field>
      <field name="sizeIconVisited">32 38</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleBalloonBanner">test-avatars-namespace--g100--banner_name_2--alias_banner</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_1--alias_pin</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_2--alias_pin_hover</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_3--alias_pin_visited</field>
      <field name="styleLogo">test-avatars-namespace--g100--logo_name_1--alias_logo</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 1356/1</title>
      <text>text value 1356</text>
    </Advert>
    <Companies>
      <id>5</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>mobile_maps_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1356</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.89</field>
      <field name="anchorIconHover">0.5 0.89</field>
      <field name="anchorIconVisited">0.5 0.89</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 38</field>
      <field name="sizeIconHover">32 38</field>
      <field name="sizeIconVisited">32 38</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleBalloonBanner">test-avatars-namespace--g100--banner_name_2--alias_banner</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_1--alias_pin</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_2--alias_pin_hover</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_3--alias_pin_visited</field>
      <field name="styleLogo">test-avatars-namespace--g100--logo_name_1--alias_logo</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 1356/1</title>
      <text>text value 1356</text>
    </Advert>
    <highlighted>false</highlighted>
    <Companies>
      <id>5</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 1</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>1</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 2</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>2</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 1357/2</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>3</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 1</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>1</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>organization title 2</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>2</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_1357</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.5</field>
      <field name="anchorIconHover">0.5 0.5</field>
      <field name="anchorIconVisited">0.5 0.5</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 32</field>
      <field name="sizeIconHover">32 32</field>
      <field name="sizeIconVisited">32 32</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_5--alias_pin_round</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 1357/2</title>
      <text>text value 1357</text>
      <disclaimer>text disclaimer 1357</disclaimer>
    </Advert>
    <Companies>
      <id>3</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>mobile_maps_menu_icon_1</pageId>
    <logId>ac_auto_log_id_campaign_200000</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.89</field>
      <field name="anchorIconHover">0.5 0.89</field>
      <field name="anchorIconVisited">0.5 0.89</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 38</field>
      <field name="sizeIconHover">32 38</field>
      <field name="sizeIconVisited">32 38</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleBalloonBanner">test-avatars-namespace--g100--banner_name_2--alias_banner</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_1--alias_pin</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_2--alias_pin_hover</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_3--alias_pin_visited</field>
      <field name="styleLogo">test-avatars-namespace--g100--logo_name_1--alias_logo</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 200000/1</title>
      <text>text value 200000</text>
    </Advert>
    <Companies>
      <id>1</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <pageId>mobile_maps_menu_icon_1/datatesting</pageId>
    <logId>ac_auto_log_id_campaign_200000</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="anchorDustHover">0.5 0.5</field>
      <field name="anchorDustVisited">0.5 0.5</field>
      <field name="anchorIcon">0.5 0.89</field>
      <field name="anchorIconHover">0.5 0.89</field>
      <field name="anchorIconVisited">0.5 0.89</field>
      <field name="anchorSelected">0.5 0.94</field>
      <field name="sizeDust">18 18</field>
      <field name="sizeDustHover">18 18</field>
      <field name="sizeDustVisited">18 18</field>
      <field name="sizeIcon">32 38</field>
      <field name="sizeIconHover">32 38</field>
      <field name="sizeIconVisited">32 38</field>
      <field name="sizeSelected">60 68</field>
      <field name="styleBalloonBanner">test-avatars-namespace--g100--banner_name_2--alias_banner</field>
      <field name="styleDust">test-avatars-namespace--g100--dust_name_1--alias_dust</field>
      <field name="styleDustHover">test-avatars-namespace--g100--dust_name_2--alias_dust_hover</field>
      <field name="styleDustVisited">test-avatars-namespace--g100--dust_name_2--alias_dust_visited</field>
      <field name="styleIcon">test-avatars-namespace--g100--pin_name_1--alias_pin</field>
      <field name="styleIconHover">test-avatars-namespace--g100--pin_name_2--alias_pin_hover</field>
      <field name="styleIconVisited">test-avatars-namespace--g100--pin_name_3--alias_pin_visited</field>
      <field name="styleLogo">test-avatars-namespace--g100--logo_name_1--alias_logo</field>
      <field name="styleSelected">test-avatars-namespace--g100--pin_name_4--alias_pin_selected</field>
    </Tags>
    <Advert>
      <title>pin_search title 200000/1</title>
      <text>text value 200000</text>
    </Advert>
    <highlighted>false</highlighted>
    <Companies>
      <id>1</id>
    </Companies>
  </AdvertData>
  <AdvertTags>
    <AdvertTag id="search_tag-1355-a665a45920422f9d417e4867efdc4fb8">
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
      </Companies>
    </AdvertTag>
    <AdvertTag id="search_tag-1356-e629fa6598d732768f7c726b4b621285">
      <Companies>
        <id>1</id>
        <id>5</id>
      </Companies>
    </AdvertTag>
    <AdvertTag id="search_tag-1357-a665a45920422f9d417e4867efdc4fb8">
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
      </Companies>
    </AdvertTag>
    <AdvertTag id="search_tag-4355-ef2d127de37b942baad06145e54b0c61">
      <Companies>
        <id>5</id>
      </Companies>
    </AdvertTag>
    <AdvertTag id="search_tag-10001-a665a45920422f9d417e4867efdc4fb8">
      <Companies>
        <id>1</id>
        <id>2</id>
        <id>3</id>
      </Companies>
    </AdvertTag>
  </AdvertTags>
  <Places>
    <Place>
      <id>100000</id>
      <latitude>55.8496856689</latitude>
      <longitude>38.7261199951</longitude>
    </Place>
    <Place>
      <id>100001</id>
      <latitude>55.8496856689</latitude>
      <longitude>38.7261199951</longitude>
    </Place>
    <Place>
      <id>100002</id>
      <latitude>55.8496856689</latitude>
      <longitude>38.7261199951</longitude>
    </Place>
    <Place>
      <id>altay:6</id>
      <latitude>55.371772</latitude>
      <longitude>39.058628</longitude>
      <address>organization address 6</address>
      <title>organization title 6</title>
      <permalink>6</permalink>
    </Place>
    <Place>
      <id>altay:7</id>
      <latitude>55.371772</latitude>
      <longitude>39.058628</longitude>
      <address>organization address 7</address>
      <title>organization title 7</title>
      <permalink>7</permalink>
    </Place>
  </Places>
  <Polygons>
    <Polygon>
      <id>campaign:2355.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:2355.2</id>
      <polygon>POLYGON ((30.0 20.0, 45.0 95.0, 42.0 14.0))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:5355.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:7355.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:7355.2</id>
      <polygon>POLYGON ((30.0 20.0, 45.0 95.0, 42.0 14.0))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:7356.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:7356.2</id>
      <polygon>POLYGON ((30.0 20.0, 45.0 95.0, 42.0 14.0))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:8355.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
    <Polygon>
      <id>campaign:9355.1</id>
      <polygon>POLYGON ((93.23 -12.3, 3.0 23.0, -30.2 100.3))</polygon>
    </Polygon>
  </Polygons>
  <PinDataList>
    <PinData>
      <pageId>navi_zero_speed_banner_10</pageId>
      <pageId>navi_zero_speed_banner_10/datatesting</pageId>
      <disclaimer>zero_speed_banner banner disclaimer</disclaimer>
      <Polygons>
        <id>campaign:2355.1</id>
        <id>campaign:2355.2</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="audit-pixel-click-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=cl</field>
        <field name="audit-pixel-im">https://ad.doubleclick.net/dcm_path;dc_rdid=[uadid];dc_lat=[lat]</field>
        <field name="audit-pixel-im-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=im</field>
        <field name="audit-pixel-load-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=load</field>
        <field name="audit-pixel-mrc100-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=MRC100Viewable</field>
        <field name="audit-pixel-mrc50-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=MRCViewable</field>
        <field name="campaignId">2355</field>
        <field name="description">zero_speed_banner banner description</field>
        <field name="isAds">true</field>
        <field name="limitImpressionsPerDay">2</field>
        <field name="product">zero_speed_banner</field>
        <field name="stylePin"></field>
        <field name="terms">zero_speed_banner banner terms</field>
        <field name="title">zero_speed_banner banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>68f1728a94d1b2e2fe530d572968bb9fa566cc39d5a5120ca0fd14e660fc62b7</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g101--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>AddPointToRoute</type>
          <field name="latitude">1.2</field>
          <field name="longitude">3.4</field>
        </Action>
        <Action>
          <type>Call</type>
          <field name="main">1</field>
          <field name="phone">+7xxxxxxxx</field>
        </Action>
        <Action>
          <type>ResolveUri</type>
          <field name="eventName">geoadv.bb.action.openSite</field>
          <field name="uri">yandexnavi://show_web_view?link=https%3A%2F%2Furi&amp;client=261&amp;signature=CyWveCjArKC2TEF0sHzm60ILGo%2F1b0Kzjn1SsNC7CWLDWhkyB%2Fu6FRaCbJLMnf8VyWwKPcGCVLwo9yjU60SDqA%3D%3D</field>
        </Action>
      </Actions>
      <Target>
        <And>
          <Or>
            <Or>
              <income>high</income>
              <income>middle</income>
            </Or>
          </Or>
          <Not>
            <segment id="542" keywordId="216"/>
          </Not>
          <Not>
            <segment id="1031" keywordId="547"/>
          </Not>
        </And>
      </Target>
      <Limits>
        <Impressions>
          <quarterHour>20000</quarterHour>
          <dailyPerUser>2</dailyPerUser>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "2355", "product": "zero_speed_banner"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_billboard_6</pageId>
      <pageId>navi_billboard_7</pageId>
      <pageId>navi_billboard_8</pageId>
      <disclaimer>pin_on_route banner disclaimer</disclaimer>
      <cost>0.0</cost>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="anchorDust">0.5 0.5</field>
        <field name="anchorDustHover">0.5 0.5</field>
        <field name="anchorDustVisited">0.5 0.5</field>
        <field name="anchorIcon">0.5 0.89</field>
        <field name="anchorIconHover">0.5 0.89</field>
        <field name="anchorIconVisited">0.5 0.89</field>
        <field name="anchorSelected">0.5 0.94</field>
        <field name="campaignId">3355</field>
        <field name="chains"></field>
        <field name="chance">555</field>
        <field name="hasDiscounts">false</field>
        <field name="isAds">true</field>
        <field name="pinSubtitle">pin_on_route pin subtitle</field>
        <field name="pinTitle">pin_on_route pin title</field>
        <field name="product">pin_on_route_v2</field>
        <field name="sizeDust">18 18</field>
        <field name="sizeDustHover">18 18</field>
        <field name="sizeDustVisited">18 18</field>
        <field name="sizeIcon">32 38</field>
        <field name="sizeIconHover">32 38</field>
        <field name="sizeIconVisited">32 38</field>
        <field name="sizeSelected">60 68</field>
        <field name="styleDust">test-avatars-namespace--g102--dust_name_1--alias_dust</field>
        <field name="styleDustHover">test-avatars-namespace--g102--dust_name_2--alias_dust_hover</field>
        <field name="styleDustVisited">test-avatars-namespace--g102--dust_name_3--alias_dust_visited</field>
        <field name="styleIcon">test-avatars-namespace--g102--pin_name_1--alias_pin</field>
        <field name="styleIconHover">test-avatars-namespace--g102--pin_name_2--alias_pin_hover</field>
        <field name="styleIconVisited">test-avatars-namespace--g102--pin_name_3--alias_pin_visited</field>
        <field name="stylePin">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="stylePinLeft">test-avatars-namespace--g102--pin_name_6--alias_pin_left</field>
        <field name="stylePinRight">test-avatars-namespace--g102--pin_name_7--alias_pin_right</field>
        <field name="styleSelected">test-avatars-namespace--g102--pin_name_4--alias_pin_selected</field>
        <field name="title">pin_on_route banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>32f1f903925dec2cc9ba4106a35ae179cd1e3bfd38c6e027533d7779a929a380</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g102--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>Call</type>
          <field name="phone">+7xxxxxxxx</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>10000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "3355", "product": "pin_on_route_v2"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_billboard_6/datatesting</pageId>
      <pageId>navi_billboard_7/datatesting</pageId>
      <pageId>navi_billboard_8/datatesting</pageId>
      <disclaimer>pin_on_route banner disclaimer</disclaimer>
      <cost>0.0</cost>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="anchorDust">0.5 0.5</field>
        <field name="anchorDustHover">0.5 0.5</field>
        <field name="anchorDustVisited">0.5 0.5</field>
        <field name="anchorIcon">0.5 0.5</field>
        <field name="anchorIconHover">0.5 0.5</field>
        <field name="anchorIconVisited">0.5 0.5</field>
        <field name="anchorSelected">0.5 0.94</field>
        <field name="campaignId">3355</field>
        <field name="chains"></field>
        <field name="chance">555</field>
        <field name="hasDiscounts">false</field>
        <field name="isAds">true</field>
        <field name="pinSubtitle">pin_on_route pin subtitle</field>
        <field name="pinTitle">pin_on_route pin title</field>
        <field name="product">pin_on_route_v2</field>
        <field name="sizeDust">18 18</field>
        <field name="sizeDustHover">18 18</field>
        <field name="sizeDustVisited">18 18</field>
        <field name="sizeIcon">48 48</field>
        <field name="sizeIconHover">48 48</field>
        <field name="sizeIconVisited">48 48</field>
        <field name="sizeSelected">60 68</field>
        <field name="styleDust">test-avatars-namespace--g102--dust_name_1--alias_dust</field>
        <field name="styleDustHover">test-avatars-namespace--g102--dust_name_2--alias_dust_hover</field>
        <field name="styleDustVisited">test-avatars-namespace--g102--dust_name_3--alias_dust_visited</field>
        <field name="styleIcon">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="styleIconHover">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="styleIconVisited">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="stylePin">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="stylePinLeft">test-avatars-namespace--g102--pin_name_6--alias_pin_left</field>
        <field name="stylePinRight">test-avatars-namespace--g102--pin_name_7--alias_pin_right</field>
        <field name="styleSelected">test-avatars-namespace--g102--pin_name_4--alias_pin_selected</field>
        <field name="title">pin_on_route banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>32f1f903925dec2cc9ba4106a35ae179cd1e3bfd38c6e027533d7779a929a380</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g102--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>Call</type>
          <field name="phone">+7xxxxxxxx</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>10000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "3355", "product": "pin_on_route_v2"}</logInfo>
    </PinData>
    <PinData>
      <pageId>mobile_maps_route_pins_1</pageId>
      <pageId>mobile_maps_route_pins_1/datatesting</pageId>
      <pageId>mobile_maps_route_pins_2</pageId>
      <pageId>mobile_maps_route_pins_2/datatesting</pageId>
      <disclaimer>pin_on_route banner disclaimer</disclaimer>
      <cost>0.0</cost>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="anchorDust">0.5 0.5</field>
        <field name="anchorDustHover">0.5 0.5</field>
        <field name="anchorDustVisited">0.5 0.5</field>
        <field name="anchorIcon">0.5 0.89</field>
        <field name="anchorIconHover">0.5 0.89</field>
        <field name="anchorIconVisited">0.5 0.89</field>
        <field name="anchorSelected">0.5 0.94</field>
        <field name="campaignId">3355</field>
        <field name="chains"></field>
        <field name="chance">555</field>
        <field name="hasDiscounts">false</field>
        <field name="isAds">true</field>
        <field name="pinSubtitle">pin_on_route pin subtitle</field>
        <field name="pinTitle">pin_on_route pin title</field>
        <field name="product">pin_on_route_v2</field>
        <field name="sizeDust">18 18</field>
        <field name="sizeDustHover">18 18</field>
        <field name="sizeDustVisited">18 18</field>
        <field name="sizeIcon">32 38</field>
        <field name="sizeIconHover">32 38</field>
        <field name="sizeIconVisited">32 38</field>
        <field name="sizeSelected">60 68</field>
        <field name="styleDust">test-avatars-namespace--g102--dust_name_1--alias_dust</field>
        <field name="styleDustHover">test-avatars-namespace--g102--dust_name_2--alias_dust_hover</field>
        <field name="styleDustVisited">test-avatars-namespace--g102--dust_name_3--alias_dust_visited</field>
        <field name="styleIcon">test-avatars-namespace--g102--pin_name_1--alias_pin</field>
        <field name="styleIconHover">test-avatars-namespace--g102--pin_name_2--alias_pin_hover</field>
        <field name="styleIconVisited">test-avatars-namespace--g102--pin_name_3--alias_pin_visited</field>
        <field name="stylePin">test-avatars-namespace--g102--pin_name_5--alias_pin_round</field>
        <field name="stylePinLeft">test-avatars-namespace--g102--pin_name_6--alias_pin_left</field>
        <field name="stylePinRight">test-avatars-namespace--g102--pin_name_7--alias_pin_right</field>
        <field name="styleSelected">test-avatars-namespace--g102--pin_name_4--alias_pin_selected</field>
        <field name="title">pin_on_route banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>32f1f903925dec2cc9ba4106a35ae179cd1e3bfd38c6e027533d7779a929a380</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g102--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>Call</type>
          <field name="phone">+7xxxxxxxx</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>10000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "3355", "product": "pin_on_route_v2"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_billboard_7</pageId>
      <pageId>navi_billboard_7/datatesting</pageId>
      <disclaimer>billboard banner disclaimer</disclaimer>
      <Places>
        <id>100000</id>
        <id>100001</id>
        <id>100002</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">4355</field>
        <field name="description">billboard banner description</field>
        <field name="isAds">true</field>
        <field name="product">billboard</field>
        <field name="stylePin">test-avatars-namespace--g104--pin_name_1--alias_pin_billboard</field>
        <field name="title">billboard banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>fe271389c5281bed5e3b444947acdf25bbd83b39e6b7e7abdc988023cd744be9</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g104--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>OpenSite</type>
          <field name="title">action title/2</field>
          <field name="url">http://example.com</field>
        </Action>
        <Action>
          <type>Search</type>
          <field name="searchQuery">{"ad": {"advert_tag_id": "search_tag-4355-ef2d127de37b942baad06145e54b0c61"}, "text": ""}</field>
          <field name="searchTitle">history text</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>20000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "4355", "product": "billboard"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_billboard_8</pageId>
      <pageId>navi_billboard_8/datatesting</pageId>
      <disclaimer>billboard banner disclaimer</disclaimer>
      <Places>
        <id>100000</id>
        <id>100001</id>
        <id>100002</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">4355</field>
        <field name="description">billboard banner description</field>
        <field name="isAds">true</field>
        <field name="pinSubtitle">description</field>
        <field name="pinTitle">title</field>
        <field name="product">billboard</field>
        <field name="stylePin">test-avatars-namespace--g104--pin_name_1--alias_pin_billboard</field>
        <field name="title">billboard banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>fe271389c5281bed5e3b444947acdf25bbd83b39e6b7e7abdc988023cd744be9</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g104--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>OpenSite</type>
          <field name="title">action title/2</field>
          <field name="url">http://example.com</field>
        </Action>
        <Action>
          <type>Search</type>
          <field name="searchQuery">{"ad": {"advert_tag_id": "search_tag-4355-ef2d127de37b942baad06145e54b0c61"}, "text": ""}</field>
          <field name="searchTitle">history text</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>20000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "4355", "product": "billboard"}</logInfo>
    </PinData>
    <PinData>
      <pageId>mobile_maps_route_pins_2</pageId>
      <pageId>mobile_maps_route_pins_2/datatesting</pageId>
      <disclaimer>billboard banner disclaimer</disclaimer>
      <Places>
        <id>100000</id>
        <id>100001</id>
        <id>100002</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">4355</field>
        <field name="description">billboard banner description</field>
        <field name="isAds">true</field>
        <field name="pinSubtitle">description</field>
        <field name="pinTitle">title</field>
        <field name="product">billboard</field>
        <field name="stylePin">test-avatars-namespace--g104--pin_name_1--alias_pin_billboard</field>
        <field name="title">billboard banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>fe271389c5281bed5e3b444947acdf25bbd83b39e6b7e7abdc988023cd744be9</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g104--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>OpenSite</type>
          <field name="title">action title/2</field>
          <field name="url">http://example.com</field>
        </Action>
        <Action>
          <type>Search</type>
          <field name="searchQuery">{"ad": {"advert_tag_id": "search_tag-4355-ef2d127de37b942baad06145e54b0c61"}, "text": ""}</field>
          <field name="searchTitle">history text</field>
        </Action>
      </Actions>
      <Limits>
        <Impressions>
          <quarterHour>20000</quarterHour>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "4355", "product": "billboard"}</logInfo>
    </PinData>
    <PinData>
      <pageId>metro_route_banner_1</pageId>
      <pageId>metro_route_banner_1/datatesting</pageId>
      <disclaimer>metro banner disclaimer</disclaimer>
      <Polygons>
        <id>campaign:5355.1</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">5355</field>
        <field name="description">metro banner description</field>
        <field name="isAds">true</field>
        <field name="product">metro_banner</field>
        <field name="styleBanner">test-avatars-namespace--g105--banner_name_1--alias_banner</field>
        <field name="title">metro banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>89b42b9d561fd414f584cb9b3d9cc7c5a3f3b8c3779af44cf0f02ffc9ce232fd</id>
          <type>metro_big_banner</type>
          <field name="styleBigBanner">test-avatars-namespace--g105--big_banner_name_1--alias_big_banner</field>
        </Creative>
      </Creatives>
      <Limits>
        <displayProbability>0.123456789</displayProbability>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "5355", "product": "metro_banner"}</logInfo>
    </PinData>
    <PinData>
      <pageId>route_via_point_1</pageId>
      <disclaimer></disclaimer>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="anchorViaPin">0.5 0.5</field>
        <field name="campaignId">6355</field>
        <field name="product">route_via_point</field>
        <field name="sizeViaPin">32 32</field>
        <field name="stylePin">test-avatars-namespace--g106--pin_name_1--route_via_point_pin</field>
        <field name="styleViaPin">test-avatars-namespace--g106--pin_name_1--route_via_point_pin</field>
        <field name="viaActiveTitle">route_via_point active text</field>
        <field name="viaDescription">route_via_point description</field>
        <field name="viaInactiveTitle">route_via_point inactive text</field>
      </Tags>
      <logInfo>{"advertiserId": "None", "campaignId": "6355", "product": "route_via_point"}</logInfo>
    </PinData>
    <PinData>
      <pageId>route_via_point_1</pageId>
      <disclaimer></disclaimer>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="anchorViaPin">0.5 0.5</field>
        <field name="campaignId">6356</field>
        <field name="product">route_via_point</field>
        <field name="sizeViaPin">32 32</field>
        <field name="stylePin">test-avatars-namespace--g106--pin_name_1--route_via_point_pin</field>
        <field name="styleViaPin">test-avatars-namespace--g106--pin_name_1--route_via_point_pin</field>
        <field name="viaActiveTitle">route_via_point active text</field>
        <field name="viaDescription">route_via_point description</field>
        <field name="viaInactiveTitle">route_via_point inactive text</field>
      </Tags>
      <logInfo>{"advertiserId": "None", "campaignId": "6356", "product": "route_via_point"}</logInfo>
    </PinData>
    <PinData>
      <pageId>route_via_point_1/datatesting</pageId>
      <disclaimer></disclaimer>
      <Places>
        <id>altay:6</id>
        <id>altay:7</id>
      </Places>
      <Tags>
        <field name="anchorViaPin">0.5 0.5</field>
        <field name="campaignId">6356</field>
        <field name="product">route_via_point</field>
        <field name="sizeViaPin">32 32</field>
        <field name="styleViaPin">test-avatars-namespace--g106--pin_name_1--route_via_point_pin</field>
        <field name="viaActiveTitle">route_via_point active text</field>
        <field name="viaDescription">route_via_point description</field>
        <field name="viaInactiveTitle">route_via_point inactive text</field>
      </Tags>
      <logInfo>{"advertiserId": "None", "campaignId": "6356", "product": "route_via_point"}</logInfo>
    </PinData>
    <PinData>
      <pageId>overview_banner_4</pageId>
      <pageId>overview_banner_4/datatesting</pageId>
      <pageId>overview_banner_5</pageId>
      <pageId>overview_banner_5/datatesting</pageId>
      <pageId>overview_banner_6</pageId>
      <pageId>overview_banner_6/datatesting</pageId>
      <disclaimer>overview_banner banner disclaimer</disclaimer>
      <Polygons>
        <id>campaign:7355.1</id>
        <id>campaign:7355.2</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="audit-pixel-click-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=cl</field>
        <field name="audit-pixel-im-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=im</field>
        <field name="audit-pixel-load-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=load</field>
        <field name="audit-pixel-mrc100-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=MRC100Viewable</field>
        <field name="audit-pixel-mrc50-templated">https://verify.yandex.ru/verify?platformid=4&amp;a.si=1&amp;a.te=2&amp;a.aap=3&amp;maid=[rdid]&amp;a.A=ev&amp;a.evn=MRCViewable</field>
        <field name="campaignId">7355</field>
        <field name="description">overview_banner banner description</field>
        <field name="isAds">true</field>
        <field name="limitImpressionsPerDay">2</field>
        <field name="product">overview_banner</field>
        <field name="stylePin"></field>
        <field name="title">overview_banner banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>68f1728a94d1b2e2fe530d572968bb9fa566cc39d5a5120ca0fd14e660fc62b7</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g101--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Target>
        <And>
          <Or>
            <Or>
              <income>high</income>
              <income>middle</income>
            </Or>
          </Or>
          <Not>
            <segment id="542" keywordId="216"/>
          </Not>
          <Not>
            <segment id="1031" keywordId="547"/>
          </Not>
        </And>
      </Target>
      <Limits>
        <Impressions>
          <quarterHour>20000</quarterHour>
          <dailyPerUser>2</dailyPerUser>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "7355", "product": "overview_banner"}</logInfo>
    </PinData>
    <PinData>
      <pageId>overview_banner_3</pageId>
      <pageId>overview_banner_3/datatesting</pageId>
      <pageId>overview_banner_4</pageId>
      <pageId>overview_banner_4/datatesting</pageId>
      <pageId>overview_banner_5</pageId>
      <pageId>overview_banner_5/datatesting</pageId>
      <pageId>overview_banner_6</pageId>
      <pageId>overview_banner_6/datatesting</pageId>
      <pageId>overview_finish_3</pageId>
      <pageId>overview_finish_3/datatesting</pageId>
      <pageId>overview_finish_4</pageId>
      <pageId>overview_finish_4/datatesting</pageId>
      <pageId>overview_finish_5</pageId>
      <pageId>overview_finish_5/datatesting</pageId>
      <pageId>overview_finish_6</pageId>
      <pageId>overview_finish_6/datatesting</pageId>
      <disclaimer>overview_banner banner disclaimer 2</disclaimer>
      <Polygons>
        <id>campaign:7356.1</id>
        <id>campaign:7356.2</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">7356</field>
        <field name="description">overview_banner banner description 2</field>
        <field name="isAds">true</field>
        <field name="limitImpressionsPerDay">2</field>
        <field name="product">overview_banner</field>
        <field name="stylePin"></field>
        <field name="title">overview_banner banner title 2</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>b67a022fd38f064807d293b4b0c46df78a9717bcb06a9975280821cb36c69646</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g101--banner_name_2--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>ResolveUri</type>
          <field name="eventName">geoadv.bb.action.openSite</field>
          <field name="uri">yandexnavi://show_web_view?link=https%3A%2F%2Furi&amp;client=261&amp;signature=CyWveCjArKC2TEF0sHzm60ILGo%2F1b0Kzjn1SsNC7CWLDWhkyB%2Fu6FRaCbJLMnf8VyWwKPcGCVLwo9yjU60SDqA%3D%3D</field>
        </Action>
        <Action>
          <type>ResolveUri</type>
          <field name="dialogCancel">Позже</field>
          <field name="dialogContent">Давайте установим автору</field>
          <field name="dialogEventCancel">SomeTextCancel</field>
          <field name="dialogEventOk">SomeTextOk</field>
          <field name="dialogOk">Установить</field>
          <field name="dialogTitle">Установка приложения</field>
          <field name="eventName">geoadv.bb.action.openSite</field>
          <field name="uri">https://example.com</field>
        </Action>
      </Actions>
      <Target>
        <And>
          <Or>
            <Or>
              <income>high</income>
              <income>middle</income>
            </Or>
          </Or>
          <Not>
            <segment id="542" keywordId="216"/>
          </Not>
          <Not>
            <segment id="1031" keywordId="547"/>
          </Not>
        </And>
      </Target>
      <Limits>
        <Impressions>
          <quarterHour>19000</quarterHour>
          <dailyPerUser>2</dailyPerUser>
        </Impressions>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "7356", "product": "overview_banner"}</logInfo>
    </PinData>
    <PinData>
      <pageId>metro_promocode_1</pageId>
      <pageId>metro_promocode_1/datatesting</pageId>
      <disclaimer>metro promocode disclaimer</disclaimer>
      <Polygons>
        <id>campaign:8355.1</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">8355</field>
        <field name="description">metro promocode description</field>
        <field name="isAds">true</field>
        <field name="product">metro_promocode</field>
        <field name="terms">metro promocode terms</field>
        <field name="title">metro promocode title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>98f5b18b3422c3dffd5e715540b38f6ab3c1c94c53f57ede8d20759aa411b10c</id>
          <type>banner</type>
          <field name="banner">test-avatars-namespace--g105--banner_name_1--alias_banner</field>
          <field name="logo">test-avatars-namespace--l105--logo_name_1--alias_logo</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>Promocode</type>
          <field name="promocode">promo123</field>
        </Action>
      </Actions>
      <Limits>
        <displayProbability>0.123456789</displayProbability>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "8355", "product": "metro_promocode"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_zero_speed_banner_10/datatesting</pageId>
      <pageId>navi_zero_speed_banner_5/datatesting</pageId>
      <pageId>navi_zero_speed_banner_6/datatesting</pageId>
      <pageId>navi_zero_speed_banner_7/datatesting</pageId>
      <pageId>navi_zero_speed_banner_8/datatesting</pageId>
      <pageId>navi_zero_speed_banner_9/datatesting</pageId>
      <disclaimer>zero_speed_banner banner disclaimer</disclaimer>
      <Polygons>
        <id>campaign:9355.1</id>
      </Polygons>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">9355</field>
        <field name="description">zero_speed_banner banner description</field>
        <field name="isAds">true</field>
        <field name="limitImpressionsPerDay">1</field>
        <field name="product">zero_speed_banner</field>
        <field name="stylePin"></field>
        <field name="terms">zero_speed_banner banner terms</field>
        <field name="title">zero_speed_banner banner title</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>68f1728a94d1b2e2fe530d572968bb9fa566cc39d5a5120ca0fd14e660fc62b7</id>
          <type>banner</type>
          <field name="styleBalloonBanner">test-avatars-namespace--g101--banner_name_1--alias_banner</field>
        </Creative>
      </Creatives>
      <Actions>
        <Action>
          <type>Call</type>
          <field name="main">1</field>
          <field name="phone">+7xxxxxxxx</field>
        </Action>
      </Actions>
      <logInfo>{"advertiserId": "None", "campaignId": "9355", "product": "zero_speed_banner"}</logInfo>
    </PinData>
  </PinDataList>
</AdvertDataList>
"""  # noqa: E501


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


@pytest.fixture
async def mock_points_client(mocker):
    return mocker.patch(
        "maps_adv.points.client.lib.client.Client._request", new_callable=coro_mock
    ).coro


@pytest.fixture
async def mock_old_geoadv_client(mocker):
    return mocker.patch(
        "maps_adv.export.lib.core.client.old_geoadv.OldGeoAdvClient._request",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
async def mock_juggler_client(mocker):
    return mocker.patch(
        "maps_adv.common.third_party_clients.juggler.JugglerClient._request",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
async def mock_dashboard_client(mocker):
    return mocker.patch(
        "maps_adv.statistics.dashboard.client.Client._request", new_callable=coro_mock
    ).coro


@pytest.fixture
def mock_sandbox_upload(mocker, config):
    def wrapped():
        sandbox_data = {}

        def _handle_init(
            _,
            conf,
            attrs,
            uploaded_dir,
            juggler_client,
        ):
            nonlocal sandbox_data
            sandbox_data["params"] = dict(directory=uploaded_dir)
            sandbox_data["dirs"] = []
            sandbox_data["files"] = []

            for _, dirs, files in os.walk(uploaded_dir):
                sandbox_data["dirs"].extend(dirs)
                sandbox_data["files"].extend(files)

            xml_filename = os.path.join(uploaded_dir, config.FILENAME_XML)

            for field, filename in (("xml_data", xml_filename),):
                sandbox_data[field] = None
                if os.path.exists(filename):
                    with open(filename, "rb") as inp:
                        sandbox_data[field] = inp.read()

        async def _handle_upload(_):
            pass

        mocker.patch(
            "maps_adv.export.lib.core.client.sandbox.SandboxClient.__init__",
            new=_handle_init,
        )
        mocker.patch(
            "maps_adv.export.lib.core.client.sandbox.SandboxClient.upload",
            new=_handle_upload,
        )

        return sandbox_data

    yield wrapped


@pytest.fixture
def mock_logging_error(mocker):
    def wrapped():
        errors = defaultdict(list)

        def _handle_error(_, msg, *args, **kwargs):
            nonlocal errors
            errors[msg].append((args, kwargs))

        mocker.patch("logging.Logger.error", new_callable=lambda: _handle_error)

        return errors

    return wrapped


@pytest.mark.parametrize(
    [
        "adv_store_message",
        "billing_message",
        "dashboard_message",
        "points_pb",
        "old_geoadv_response",
        "expected_xml",
        "expected_errors",
        "expected_broken_info",
    ],
    [
        (
            adv_store_proto_message,
            billing_proto_message,
            dashboard_proto_message,
            points_proto_message,
            old_geoadv_json_response,
            expected_big_xml,
            {},
            {"ResolvePagesStep": [10001]},
        ),
        (
            CampaignExportList(campaigns=[]).SerializeToString(),
            OrderIds(order_ids=[]).SerializeToString(),
            CampaignEventsForPeriodOutput(campaigns_events=[]).SerializeToString(),
            points_in_polygons_pb2.PointsInPolygonsOutput(
                points=[]
            ).SerializeToString(),
            {"orgs": []},
            "<?xml version='1.0' encoding='utf-8'?>\n"
            '<AdvertDataList xmlns="http://maps.yandex.ru/advert/1.x"/>\n',
            {},
            {},
        ),
        (  # campaign without adv points should be filtered
            CampaignExportList(
                campaigns=[
                    # should be filtered
                    # Campaign BILLBOARD
                    CampaignExport(
                        id=4355,
                        publication_envs=[
                            PublicationEnv.PRODUCTION,
                            PublicationEnv.DATA_TESTING,
                        ],
                        campaign_type=CampaignType.BILLBOARD,
                        placing=Placing(
                            area=Area(version=4, areas=[Polygon(points=[], name="kek")])
                        ),
                        platforms=[Platform.NAVI],
                        creatives=[
                            Creative(
                                billboard=Billboard(
                                    images=[
                                        Image(
                                            type="pin",
                                            group_id="g104",
                                            image_name="pin_name_1",
                                            alias_template="alias_pin_billboard",
                                        )
                                    ]
                                )
                            ),
                            Creative(
                                banner=Banner(
                                    title="billboard banner title",
                                    disclaimer="billboard banner disclaimer",
                                    show_ads_label=True,
                                    images=[
                                        Image(
                                            type="banner",
                                            group_id="g104",
                                            image_name="banner_name_1",
                                            alias_template="alias_banner_{zoom}",
                                        )
                                    ],
                                )
                            ),
                        ],
                        actions=[
                            Action(
                                title="action title/1",
                                search=Search(
                                    history_text="history text", organizations=[5]
                                ),
                            ),
                            Action(
                                title="action title/2",
                                open_site=OpenSite(url="http://example.com"),
                            ),
                        ],
                        order_id=5124,
                    )
                ]
            ).SerializeToString(),
            OrderIds(order_ids=[5124]).SerializeToString(),
            CampaignEventsForPeriodOutput(
                campaigns_events=[CampaignEvents(campaign_id=4355, events=0)]
            ).SerializeToString(),
            points_in_polygons_pb2.PointsInPolygonsOutput(
                points=[]
            ).SerializeToString(),
            {"orgs": []},
            "<?xml version='1.0' encoding='utf-8'?>\n"
            '<AdvertDataList xmlns="http://maps.yandex.ru/advert/1.x"/>\n',
            {"Found campaigns without organizations: [4355]": [((), {})]},
            {},
        ),
    ],
)
async def test_returns_expected_xml_file(
    adv_store_message: bytes,
    billing_message: bytes,
    points_pb: bytes,
    old_geoadv_response: dict,
    dashboard_message: bytes,
    expected_xml: str,
    expected_errors: dict,
    expected_broken_info: list,
    experimental_options,
    mock_adv_store,
    mock_billing,
    mock_billing_orders_discounts,
    mock_points_client,
    mock_dashboard_client,
    mock_old_geoadv_client,
    mock_juggler_client,
    mock_sandbox_upload,
    mock_logging_error,
):
    mock_adv_store(Response(body=adv_store_message, status=200))
    mock_billing(Response(body=billing_message, status=200))
    mock_billing_orders_discounts(
        Response(body=billing_orders_discounts_message, status=200)
    )
    mock_points_client.return_value = points_pb
    mock_old_geoadv_client.return_value = old_geoadv_response
    mock_dashboard_client.return_value = dashboard_message
    mock_juggler_client.return_value = {"success": True, "events": [{"code": 200}]}

    sandbox_data = mock_sandbox_upload()
    errors_data = mock_logging_error()

    with experimental_options(
        {
            "EXPERIMENT_USE_HIGHLIGHTED_ATTR_FOR_PIN_SEARCH": [
                PublicationEnvEnum.DATA_TESTING
            ],
            "EXPERIMENT_WITHOUT_STYLE_PIN_FOR_VIAPOINT": [
                PublicationEnvEnum.DATA_TESTING
            ],
            "MONKEY_PATCH_NAVI_WITH_ROUND_PIN_FOR_PIN_ON_ROUTE": [
                PublicationEnvEnum.DATA_TESTING
            ],
            "EXPERIMENT_CAMPAIGNS_WITH_DISPLAY_LIMITS": [
                1355,
                1356,
                1357,
                200000,
                2355,
                3355,
                4355,
                5355,
                6355,
                6356,
                7355,
                7356,
                8355,
                9355,
            ],
        }
    ) as config, tempfile.TemporaryDirectory() as tmp_folder:
        await export(config, tmp_folder)

    errors = {
        key_error: value_error
        for key_error, value_error in errors_data.items()
        if not (
            key_error.startswith(
                "Unclosed client session\nclient_session: <aiohttp.client.ClientSession object at"  # noqa: E501
            )
            or key_error.startswith("Unclosed connector\nconnections: ")
        )
    }

    assert set(sandbox_data["dirs"]) == set()
    assert set(sandbox_data["files"]) == {config.FILENAME_XML}
    assert sandbox_data["xml_data"].decode() == expected_xml
    assert errors == expected_errors
    mock_juggler_client.assert_called_with(
        "POST",
        "/events",
        200,
        json={
            "events": [
                {
                    "description": ";".join(
                        "{}({})".format(step, ",".join(map(str, ids)))
                        for step, ids in expected_broken_info.items()
                    ),
                    "host": "test",
                    "service": "broken_campaigns",
                    "status": "CRIT" if expected_broken_info else "OK",
                }
            ]
        },
    )
