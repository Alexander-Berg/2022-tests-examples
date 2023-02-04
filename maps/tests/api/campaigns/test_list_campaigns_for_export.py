from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.adv_store.api.proto.action_pb2 import (
    Action,
    ActionType,
    AddPointToRoute,
    DownloadApp,
    OpenSite,
    PhoneCall,
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
)
from maps_adv.adv_store.api.proto.creative_pb2 import (
    AudioBanner,
    Banner,
    Billboard,
    Creative,
    Icon,
    Image,
    LogoAndText,
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
from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OverviewPositionEnum,
    ResolveUriTargetEnum,
    PublicationEnvEnum,
)
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import CampaignType

pytestmark = [pytest.mark.asyncio]


API_URL = "/campaigns/export/"


def moscow_now():
    gmt_3 = timedelta(hours=3)
    return (datetime.utcnow() + gmt_3).replace(tzinfo=timezone(gmt_3))


_cpm = {
    "cost": Decimal("50"),
    "budget": Decimal("66000"),
    "daily_budget": Decimal("5000"),
    "auto_daily_budget": False,
}

_cpa = {
    "cost": Decimal("50"),
    "budget": Decimal("700"),
    "daily_budget": Decimal("100"),
    "auto_daily_budget": False,
}


async def test_campaign_retrieved(factory, api):
    now = moscow_now()
    images = [
        dict(
            type="img type 2", group_id="gid1", image_name="img1", alias_template="at1"
        ),
        dict(
            type="img type 2", group_id="gid2", image_name="img2", alias_template="at2"
        ),
    ]
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(
                version=1,
                areas=[
                    dict(
                        name="poly1",
                        preset_id="preset_1",
                        points=[dict(lat=1, lon=2), dict(lat=3.3, lon=4.4)],
                    )
                ],
            ),
            actions=[
                dict(type_="open_site", title=None, url="http://url", main=False),
                dict(
                    type_="phone_call",
                    title=None,
                    phone="+000 (00) 000 00 00",
                    main=False,
                ),
                dict(
                    type_="search",
                    title=None,
                    organizations=[1, 2, 3],
                    history_text="history text",
                    main=False,
                ),
                dict(
                    type_="download_app",
                    title=None,
                    url="http://download.url",
                    app_store_id="apple1",
                    google_play_id="google1",
                    main=False,
                ),
                dict(
                    type_="resolve_uri",
                    uri="magic://url",
                    action_type=ActionTypeEnum.OPEN_SITE,
                    target=ResolveUriTargetEnum.WEB_VIEW,
                    main=False,
                ),
                dict(
                    type_="add_point_to_route", latitude=1.2, longitude=3.4, main=False
                ),
            ],
            creatives=[
                {
                    "type_": "pin",
                    "images": images,
                    "title": "pin title",
                    "subtitle": "pin subtitle",
                },
                {
                    "type_": "pin_search",
                    "images": images,
                    "title": "pin search title",
                    "organizations": [1, 2, 3],
                },
                {
                    "type_": "billboard",
                    "images": images,
                    "title": "billboard title",
                    "description": "billboard description",
                },
                {"type_": "logo_and_text", "images": images, "text": "logo and text"},
                {
                    "type_": "banner",
                    "images": images,
                    "disclaimer": "banner disclaimer",
                    "show_ads_label": True,
                    "title": "banner title",
                    "description": "banner description",
                    "terms": "banner terms",
                },
                {
                    "type_": "text",
                    "images": images,
                    "text": "text",
                    "disclaimer": "disclaimer",
                },
                {
                    "type_": "icon",
                    "images": images,
                    "position": 123,
                    "title": "icon title",
                },
                {
                    "type_": "via_point",
                    "images": images,
                    "button_text_active": "via active",
                    "button_text_inactive": "via inactive",
                    "description": "via description",
                },
                {
                    "type_": "audio_banner",
                    "images": images,
                    "left_anchor": 0.5,
                    "audio_file_url": "http://someurl.com",
                },
            ],
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            settings={
                "custom_page_id": "abc",
                "overview_position": OverviewPositionEnum.FINISH,
            },
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    creative_images = [
        Image(
            type="img type 2", group_id="gid1", image_name="img1", alias_template="at1"
        ),
        Image(
            type="img type 2", group_id="gid2", image_name="img2", alias_template="at2"
        ),
    ]

    assert got == CampaignExportList(
        campaigns=[
            CampaignExport(
                id=campaign_id,
                order_id=10,
                publication_envs=[PublicationEnv.PRODUCTION],
                campaign_type=CampaignType.ZERO_SPEED_BANNER,
                placing=Placing(
                    area=Area(
                        version=1,
                        areas=[
                            Polygon(
                                name="poly1",
                                preset_id="preset_1",
                                points=[Point(lat=1, lon=2), Point(lat=3.3, lon=4.4)],
                            )
                        ],
                    )
                ),
                targeting="{}",
                platforms=[Platform.NAVI],
                creatives=[
                    Creative(
                        billboard=Billboard(
                            images=creative_images,
                            title="billboard title",
                            description="billboard description",
                        )
                    ),
                    Creative(
                        pin=Pin(
                            images=creative_images,
                            title="pin title",
                            subtitle="pin subtitle",
                        )
                    ),
                    Creative(
                        banner=Banner(
                            images=creative_images,
                            disclaimer="banner disclaimer",
                            show_ads_label=True,
                            title="banner title",
                            description="banner description",
                            terms="banner terms",
                        )
                    ),
                    Creative(text=Text(text="text", disclaimer="disclaimer")),
                    Creative(
                        icon=Icon(
                            images=creative_images, position=123, title="icon title"
                        )
                    ),
                    Creative(
                        pin_search=PinSearch(
                            images=creative_images,
                            title="pin search title",
                            organizations=[1, 2, 3],
                        )
                    ),
                    Creative(
                        logo_and_text=LogoAndText(
                            images=creative_images, text="logo and text"
                        )
                    ),
                    Creative(
                        via_point=ViaPoint(
                            images=creative_images,
                            button_text_active="via active",
                            button_text_inactive="via inactive",
                            description="via description",
                        )
                    ),
                    Creative(
                        audio_banner=AudioBanner(
                            images=creative_images,
                            left_anchor="0.5",
                            audio_file_url="http://someurl.com",
                        )
                    ),
                ],
                actions=[
                    Action(open_site=OpenSite(url="http://url"), main=False),
                    Action(
                        search=Search(
                            organizations=[1, 2, 3], history_text="history text"
                        ),
                        main=False,
                    ),
                    Action(
                        phone_call=PhoneCall(phone="+000 (00) 000 00 00"), main=False
                    ),
                    Action(
                        download_app=DownloadApp(
                            url="http://download.url",
                            app_store_id="apple1",
                            google_play_id="google1",
                        ),
                        main=False,
                    ),
                    Action(
                        resolve_uri=ResolveUri(
                            uri="magic://url",
                            action_type=ActionType.OPEN_SITE,
                            target=ResolveUri.Target.WEB_VIEW,
                            dialog=None,
                        ),
                        main=False,
                    ),
                    Action(
                        add_point_to_route=AddPointToRoute(latitude=1.2, longitude=3.4),
                        main=False,
                    ),
                ],
                display_chance=50,
                settings=CampaignSettings(
                    custom_page_id="abc", overview_position=OverviewPosition.Enum.FINISH
                ),
                total_daily_display_limit=100000,
                total_daily_action_limit=None,
                total_display_minutes_left_today=59,
                total_display_minutes_today=120,
                timezone="UTC",
                cost=Decimal("50"),
            )
        ],
    )


async def test_campaign_with_schedule_retrieved(factory, api):
    now = moscow_now()
    week_minutes = now.weekday() * 60 * 24 + now.hour * 60 + now.minute
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[]),
            timezone="Europe/Moscow",
            week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert len(got.campaigns) == 1


async def test_no_retrieved_campaign_if_miss_week_schedule(factory, api):
    now = moscow_now()
    week_minutes = now.weekday() * 60 * 24 + now.hour * 60 + now.minute
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[]),
            timezone="Europe/Moscow",
            week_schedule=[{"start": week_minutes + 2, "end": week_minutes + 3}],
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got == CampaignExportList(
        campaigns=[],
    )


@pytest.mark.parametrize(
    "status", list(set(CampaignStatusEnum) - {CampaignStatusEnum.ACTIVE})
)
async def test_returns_empty_exporting_campaigns_for_no_exporting_statuses(
    factory, status, api
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[]),
            timezone="Europe/Moscow",
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
        )
    )["id"]
    await factory.set_status(campaign_id, status=status, changed_datetime=now)

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got == CampaignExportList(
        campaigns=[],
    )


async def test_several_campaign_retrieved(factory, api):
    now = moscow_now()

    week_minutes = now.weekday() * 60 * 24 + now.hour * 60 + now.minute

    campaign_ids = []
    for _ in range(2):
        campaign_ids.append(
            (
                await factory.create_campaign(
                    cpm=_cpm,
                    order_id=10,
                    area=dict(version=1, areas=[]),
                    timezone="Europe/Moscow",
                    week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
                    start_datetime=now - timedelta(hours=1),
                    end_datetime=now + timedelta(hours=1),
                    publication_envs=[PublicationEnvEnum.PRODUCTION],
                )
            )["id"]
        )
        await factory.set_status(
            campaign_ids[-1], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
        )

    await factory.create_campaign(  # no returns by week schedule
        cpm=_cpm,
        order_id=10,
        area=dict(version=1, areas=[]),
        timezone="Europe/Moscow",
        week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
        start_datetime=now - timedelta(hours=1),
        end_datetime=now + timedelta(hours=1),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )
    await factory.create_campaign(  # no return by start/end date
        cpm=_cpm,
        order_id=10,
        area=dict(version=1, areas=[]),
        timezone="Europe/Moscow",
        week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
        start_datetime=now - timedelta(hours=2),
        end_datetime=now - timedelta(hours=1),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )
    campaign_no_return_by_status = await factory.create_campaign(  # no return by status
        cpm=_cpm,
        order_id=10,
        area=dict(version=1, areas=[]),
        timezone="Europe/Moscow",
        week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
        start_datetime=now - timedelta(hours=1),
        end_datetime=now + timedelta(hours=1),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
    )

    await factory.set_status(
        campaign_no_return_by_status["id"],
        status=CampaignStatusEnum.PAUSED,
        changed_datetime=now,
    )

    campaign_ids.append(
        (
            await factory.create_campaign(
                cpm=_cpm,
                order_id=10,
                area=dict(version=1, areas=[]),
                timezone="Europe/Moscow",
                week_schedule=[
                    {
                        "start": week_minutes - 24 * 60 - 60,
                        "end": week_minutes - 24 * 60 - 50,
                    },
                    {"start": week_minutes - 10, "end": week_minutes + 11},
                    {
                        "start": week_minutes + 24 * 60 + 1,
                        "end": week_minutes + 24 * 60 + 61,
                    },
                ],
                start_datetime=now - timedelta(hours=1),
                end_datetime=now + timedelta(hours=1),
                publication_envs=[PublicationEnvEnum.PRODUCTION],
            )
        )["id"]
    )
    await factory.set_status(
        campaign_ids[-1], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )
    campaign_ids.append(
        (
            await factory.create_campaign(
                campaign_type=CampaignTypeEnum.VIA_POINTS,
                cpa=_cpa,
                order_id=10,
                organizations={"permalinks": [1, 2, 3]},
                timezone="Europe/Moscow",
                week_schedule=[{"start": week_minutes, "end": week_minutes + 1}],
                start_datetime=now - timedelta(hours=1),
                end_datetime=now + timedelta(hours=1),
                publication_envs=[PublicationEnvEnum.PRODUCTION],
            )
        )["id"]
    )
    await factory.set_status(
        campaign_ids[-1], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got == CampaignExportList(
        campaigns=[
            CampaignExport(
                id=campaign_ids[0],
                order_id=10,
                publication_envs=[PublicationEnv.PRODUCTION],
                campaign_type=CampaignType.ZERO_SPEED_BANNER,
                placing=Placing(area=Area(version=1, areas=[])),
                targeting="{}",
                platforms=[Platform.NAVI],
                creatives=[],
                actions=[],
                display_chance=50,
                settings=CampaignSettings(),
                total_daily_display_limit=100000,
                total_daily_action_limit=None,
                total_display_minutes_left_today=0,
                total_display_minutes_today=1,
                timezone="Europe/Moscow",
                cost=Decimal("50"),
            ),
            CampaignExport(
                id=campaign_ids[1],
                order_id=10,
                publication_envs=[PublicationEnv.PRODUCTION],
                campaign_type=CampaignType.ZERO_SPEED_BANNER,
                placing=Placing(area=Area(version=1, areas=[])),
                targeting="{}",
                platforms=[Platform.NAVI],
                creatives=[],
                actions=[],
                display_chance=50,
                settings=CampaignSettings(),
                total_daily_display_limit=100000,
                total_daily_action_limit=None,
                total_display_minutes_left_today=0,
                total_display_minutes_today=1,
                timezone="Europe/Moscow",
                cost=Decimal("50"),
            ),
            CampaignExport(
                id=campaign_ids[2],
                order_id=10,
                publication_envs=[PublicationEnv.PRODUCTION],
                campaign_type=CampaignType.ZERO_SPEED_BANNER,
                placing=Placing(area=Area(version=1, areas=[])),
                targeting="{}",
                platforms=[Platform.NAVI],
                creatives=[],
                actions=[],
                display_chance=50,
                settings=CampaignSettings(),
                total_daily_display_limit=100000,
                total_daily_action_limit=None,
                total_display_minutes_left_today=10,
                total_display_minutes_today=21,
                timezone="Europe/Moscow",
                cost=Decimal("50"),
            ),
            CampaignExport(
                id=campaign_ids[3],
                order_id=10,
                publication_envs=[PublicationEnv.PRODUCTION],
                campaign_type=CampaignType.VIA_POINTS,
                placing=Placing(organizations=Organizations(permalinks=[1, 2, 3])),
                targeting="{}",
                platforms=[Platform.NAVI],
                creatives=[],
                actions=[],
                settings=CampaignSettings(),
                total_daily_display_limit=None,
                total_daily_action_limit=2,
                total_display_minutes_left_today=0,
                total_display_minutes_today=1,
                timezone="Europe/Moscow",
                cost=Decimal("50"),
            ),
        ],
    )


@pytest.mark.parametrize(
    "probability, probability_auto, expected_probability",
    [
        (0.1, 0.11, "0.100000"),
        (0.11, 0.1, "0.110000"),
        (None, 0.11, ""),
        (0.11, None, "0.110000"),
    ],
)
async def test_returns_display_probability_if_set(
    factory, api, probability, probability_auto, expected_probability
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[]),
            display_probability=probability,
            display_probability_auto=probability_auto,
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got.campaigns[0].display_probability == expected_probability


async def test_returns_no_display_probability_if_not_set(factory, api):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[]),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert not got.campaigns[0].HasField("display_probability")


@pytest.mark.parametrize(
    "polygon, expected_polygon",
    [
        [
            {"name": "polygon 1", "preset_id": None, "points": []},
            Polygon(points=[], name="polygon 1"),
        ],
        [
            {"name": "polygon 1", "preset_id": 10, "points": []},
            Polygon(points=[], name="polygon 1", preset_id="10"),
        ],
    ],
)
async def test_expected_polygon_for_campaign(polygon, expected_polygon, api, factory):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            order_id=10,
            area=dict(version=1, areas=[polygon]),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got.campaigns[0].placing == Placing(
        area=Area(version=1, areas=[expected_polygon])
    )


@pytest.mark.parametrize(
    ["cpm", "expected_display_chance"],
    [(Decimal("59.0"), 59), (Decimal("59.1"), 60), (Decimal("59.5"), 60)],
)
async def test_returns_display_chance_as_calculated_from_campaign_cpm(
    cpm, expected_display_chance, api, factory
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm={
                "cost": cpm,
                "budget": 20,
                "daily_budget": None,
                "auto_daily_budget": False,
            },
            order_id=10,
            area=dict(version=1, areas=[]),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert got.campaigns[0].display_chance == expected_display_chance


@pytest.mark.parametrize(
    "billing",
    [
        {"fix": {"cost": Decimal("60"), "time_interval": FixTimeIntervalEnum.MONTHLY}},
        {
            "cpa": {
                "cost": Decimal("61"),
                "budget": 20,
                "daily_budget": None,
                "auto_daily_budget": False,
            }
        },
    ],
)
async def test_hasnt_display_chance_field_for_no_cpm_billing_types(
    billing, api, factory
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            publication_envs=[PublicationEnvEnum.PRODUCTION],
            order_id=10,
            area=dict(version=1, areas=[]),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            **billing,
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await api.get(API_URL, decode_as=CampaignExportList, expected_status=200)

    assert not got.campaigns[0].HasField("display_chance")
