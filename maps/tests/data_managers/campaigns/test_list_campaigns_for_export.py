from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
)
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


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


async def test_campaign_retrieved(factory, campaigns_dm):
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
                    main=True,
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
                    "audio_file_url": "http://filestorage.ru/my_file.mp3",
                },
            ],
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            settings={
                "custom_page_id": "abc",
                "overview_position": OverviewPositionEnum.FINISH,
                "forced_product_version_datetime": 1609545600,
                "verification_data": [
                    {"platform": "weborama", "params": {"a": "b", "x": "y"}},
                    {"platform": "dcm", "params": {"url": "https://dcm.url"}},
                ],
            },
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await campaigns_dm.list_campaigns_for_export()

    assert got == [
        {
            "id": campaign_id,
            "order_id": 10,
            "actions": [
                {
                    "type_": "open_site",
                    "title": None,
                    "url": "http://url",
                    "main": False,
                },
                {
                    "type_": "search",
                    "title": None,
                    "history_text": "history text",
                    "organizations": [1, 2, 3],
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": None,
                    "phone": "+000 (00) 000 00 00",
                    "main": True,
                },
                {
                    "type_": "download_app",
                    "title": None,
                    "app_store_id": "apple1",
                    "google_play_id": "google1",
                    "url": "http://download.url",
                    "main": False,
                },
                {
                    "type_": "resolve_uri",
                    "uri": "magic://url",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "dialog": None,
                    "main": False,
                },
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                },
            ],
            "billing_cpm_cost": Decimal("50.0000"),
            "cost": Decimal("50.0000"),
            "total_daily_display_limit": 100000,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 59,
            "total_display_minutes_today": 120,
            "timezone": "UTC",
            "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
            "creatives": [
                {
                    "type_": "billboard",
                    "images": images,
                    "images_v2": [],
                    "title": "billboard title",
                    "description": "billboard description",
                },
                {
                    "type_": "pin",
                    "images": images,
                    "subtitle": "pin subtitle",
                    "title": "pin title",
                },
                {
                    "type_": "banner",
                    "images": images,
                    "description": "banner description",
                    "disclaimer": "banner disclaimer",
                    "show_ads_label": True,
                    "title": "banner title",
                    "terms": "banner terms",
                },
                {"disclaimer": "disclaimer", "text": "text", "type_": "text"},
                {
                    "type_": "icon",
                    "images": images,
                    "position": 123,
                    "title": "icon title",
                },
                {
                    "type_": "pin_search",
                    "images": images,
                    "organizations": [1, 2, 3],
                    "title": "pin search title",
                },
                {"type_": "logo_and_text", "images": images, "text": "logo and text"},
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
                    "audio_file_url": "http://filestorage.ru/my_file.mp3",
                },
            ],
            "display_probability": None,
            "placing": {
                "area": {
                    "areas": [
                        {
                            "name": "poly1",
                            "points": [{"lat": 1, "lon": 2}, {"lat": 3.3, "lon": 4.4}],
                            "preset_id": "preset_1",
                        }
                    ],
                    "version": 1,
                }
            },
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "targeting": {},
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {
                "custom_page_id": "abc",
                "overview_position": OverviewPositionEnum.FINISH,
                "forced_product_version_datetime": datetime(
                    2021, 1, 2, tzinfo=timezone.utc
                ),
                "verification_data": [
                    {"platform": "weborama", "params": {"a": "b", "x": "y"}},
                    {"platform": "dcm", "params": {"url": "https://dcm.url"}},
                ],
            },
            "paid_till": None,
        }
    ]


async def test_campaign_with_schedule_retrieved(factory, campaigns_dm):
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert len(got) == 1


async def test_no_retrieved_campaign_if_miss_week_schedule(factory, campaigns_dm):
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
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await campaigns_dm.list_campaigns_for_export()

    assert got == []


@pytest.mark.parametrize(
    "status", list(set(CampaignStatusEnum) - {CampaignStatusEnum.ACTIVE})
)
async def test_returns_empty_exporting_campaigns_for_no_exporting_statuses(
    factory, status, campaigns_dm
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
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(campaign_id, status=status, changed_datetime=now)

    got = await campaigns_dm.list_campaigns_for_export()

    assert got == []


async def test_several_campaign_retrieved(factory, campaigns_dm):
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert got == [
        {
            "id": campaign_ids[0],
            "order_id": 10,
            "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
            "display_probability": None,
            "placing": {"area": {"areas": [], "version": 1}},
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "targeting": {},
            "actions": [],
            "creatives": [],
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "billing_cpm_cost": Decimal("50.0000"),
            "cost": Decimal("50.0000"),
            "total_daily_display_limit": 100000,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 0,
            "total_display_minutes_today": 1,
            "timezone": "Europe/Moscow",
            "paid_till": None,
        },
        {
            "id": campaign_ids[1],
            "order_id": 10,
            "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
            "display_probability": None,
            "placing": {"area": {"areas": [], "version": 1}},
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "targeting": {},
            "actions": [],
            "creatives": [],
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "billing_cpm_cost": Decimal("50.0000"),
            "cost": Decimal("50.0000"),
            "total_daily_display_limit": 100000,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 0,
            "total_display_minutes_today": 1,
            "timezone": "Europe/Moscow",
            "paid_till": None,
        },
        {
            "id": campaign_ids[2],
            "order_id": 10,
            "campaign_type": CampaignTypeEnum.VIA_POINTS,
            "display_probability": None,
            "placing": {"organizations": {"permalinks": [1, 2, 3]}},
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "targeting": {},
            "actions": [],
            "creatives": [],
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "billing_cpm_cost": None,
            "cost": Decimal("50.0000"),
            "total_daily_display_limit": None,
            "total_daily_action_limit": 2,
            "total_display_minutes_left_today": 0,
            "total_display_minutes_today": 1,
            "timezone": "Europe/Moscow",
            "paid_till": None,
        },
    ]


@pytest.mark.parametrize(
    "probability, probability_auto, expected_probability",
    [
        (0.1, 0.11, Decimal("0.100000")),
        (0.11, 0.1, Decimal("0.110000")),
        (None, 0.11, None),
        (0.11, None, Decimal("0.110000")),
    ],
)
async def test_returns_display_probability_if_set(
    factory, campaigns_dm, probability, probability_auto, expected_probability
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["display_probability"] == expected_probability


async def test_returns_no_display_probability_if_not_set(factory, campaigns_dm):
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["display_probability"] is None


@pytest.mark.parametrize(
    "polygon, expected_polygon",
    [
        [
            {"name": "polygon 1", "preset_id": None, "points": []},
            {"name": "polygon 1", "preset_id": None, "points": []},
        ],
        [
            {"name": "polygon 1", "preset_id": 10, "points": []},
            {"name": "polygon 1", "preset_id": 10, "points": []},
        ],
    ],
)
async def test_expected_polygon_for_campaign(
    polygon, expected_polygon, campaigns_dm, factory
):
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["placing"] == {"area": {"areas": [expected_polygon], "version": 1}}


@pytest.mark.parametrize(
    ["cpm", "expected_billing_cpm_cost"],
    [
        (Decimal("59.0"), Decimal("59.0")),
        (Decimal("59.1"), Decimal("59.1")),
        (Decimal("59.5"), Decimal("59.5")),
    ],
)
async def test_returns_billing_cpm_cost(
    cpm, expected_billing_cpm_cost, campaigns_dm, factory
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

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["billing_cpm_cost"] == expected_billing_cpm_cost


async def test_placing_for_category_search_campaign_without_organizations(  # noqa: E501
    campaigns_dm, factory
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
            organizations=dict(),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["placing"] == {"organizations": {"permalinks": []}}


async def test_handle_datatesting_expires_at(factory, campaigns_dm):
    now = moscow_now()
    campaign_ids = list()

    campaign_ids.append(
        (
            await factory.create_campaign(  # not active
                campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
                order_id=1,
                organizations=dict(),
                start_datetime=now - timedelta(hours=1),
                end_datetime=now + timedelta(hours=1),
                user_daily_display_limit=1,
                user_display_limit=10,
                display_probability=0.5,
                datatesting_expires_at=now + timedelta(days=1),
                targeting={"tag": "audience", "attributes": {"id": "1111"}},
            )
        )["id"]
    )

    campaign_ids.append(
        (
            await factory.create_campaign(  # starts later
                campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
                order_id=1,
                organizations=dict(),
                start_datetime=now + timedelta(hours=1),
                end_datetime=now + timedelta(hours=2),
                user_daily_display_limit=1,
                user_display_limit=10,
                display_probability=0.5,
                datatesting_expires_at=now + timedelta(days=1),
                targeting={"tag": "audience", "attributes": {"id": "1111"}},
            )
        )["id"]
    )
    await factory.set_status(
        campaign_ids[1], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    campaign_ids.append(
        (
            await factory.create_campaign(  # ended already
                campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
                publication_envs=[PublicationEnvEnum.PRODUCTION],
                order_id=1,
                organizations=dict(),
                start_datetime=now - timedelta(hours=2),
                end_datetime=now - timedelta(hours=1),
                user_daily_display_limit=1,
                user_display_limit=10,
                display_probability=0.5,
                datatesting_expires_at=now + timedelta(days=1),
                targeting={"tag": "audience", "attributes": {"id": "1111"}},
            )
        )["id"]
    )
    await factory.set_status(
        campaign_ids[2], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    campaign_ids.append(
        (
            await factory.create_campaign(  # running
                campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
                publication_envs=[
                    PublicationEnvEnum.PRODUCTION,
                ],
                order_id=1,
                organizations=dict(),
                start_datetime=now - timedelta(hours=1),
                end_datetime=now + timedelta(hours=1),
                user_daily_display_limit=1,
                user_display_limit=10,
                display_probability=0.5,
                datatesting_expires_at=now + timedelta(days=1),
                targeting={"tag": "audience", "attributes": {"id": "1111"}},
            )
        )["id"]
    )
    await factory.set_status(
        campaign_ids[3], status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    # NOT EXPORTED
    await factory.create_campaign(  # no datatesting
        campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        organizations=dict(),
        start_datetime=now - timedelta(hours=1),
        end_datetime=now + timedelta(hours=1),
        datatesting_expires_at=None,
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )

    await factory.create_campaign(  # datatesting expired
        campaign_type=CampaignTypeEnum.CATEGORY_SEARCH,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        organizations=dict(),
        start_datetime=now - timedelta(hours=1),
        end_datetime=now + timedelta(hours=1),
        datatesting_expires_at=now - timedelta(days=1),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )

    got = await campaigns_dm.list_campaigns_for_export()

    assert got == [
        {
            "id": campaign_ids[0],
            "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH,
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            "order_id": None,
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "targeting": {},
            "display_probability": None,
            "placing": {"organizations": {"permalinks": []}},
            "actions": [],
            "creatives": [],
            "billing_cpm_cost": Decimal("12.3456"),
            "cost": Decimal("12.34560"),
            "total_daily_display_limit": 405003,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 59,
            "total_display_minutes_today": 120,
            "timezone": "UTC",
            "paid_till": None,
        },
        {
            "id": campaign_ids[1],
            "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH,
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            "order_id": None,
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "targeting": {},
            "display_probability": None,
            "placing": {"organizations": {"permalinks": []}},
            "actions": [],
            "creatives": [],
            "billing_cpm_cost": Decimal("12.3456"),
            "cost": Decimal("12.34560"),
            "total_daily_display_limit": 405003,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 60,
            "total_display_minutes_today": 60,
            "timezone": "UTC",
            "paid_till": None,
        },
        {
            "id": campaign_ids[2],
            "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH,
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            "order_id": None,
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "targeting": {},
            "display_probability": None,
            "placing": {"organizations": {"permalinks": []}},
            "actions": [],
            "creatives": [],
            "billing_cpm_cost": Decimal("12.3456"),
            "cost": Decimal("12.34560"),
            "total_daily_display_limit": 405003,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 0,
            "total_display_minutes_today": 60,
            "timezone": "UTC",
            "paid_till": None,
        },
        {
            "id": campaign_ids[3],
            "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH,
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            "order_id": None,
            "user_daily_display_limit": None,
            "user_display_limit": None,
            "settings": {},
            "targeting": {},
            "display_probability": None,
            "placing": {"organizations": {"permalinks": []}},
            "actions": [],
            "creatives": [],
            "billing_cpm_cost": Decimal("12.3456"),
            "cost": Decimal("12.34560"),
            "total_daily_display_limit": 405003,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 59,
            "total_display_minutes_today": 120,
            "timezone": "UTC",
            "paid_till": None,
        },
        {
            "id": campaign_ids[3],
            "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH,
            "platforms": [PlatformEnum.NAVI],
            "publication_envs": [PublicationEnvEnum.PRODUCTION],
            "order_id": 1,
            "user_daily_display_limit": 1,
            "user_display_limit": 10,
            "settings": {},
            "targeting": {"tag": "audience", "attributes": {"id": "1111"}},
            "display_probability": Decimal("0.5"),
            "placing": {"organizations": {"permalinks": []}},
            "actions": [],
            "creatives": [],
            "billing_cpm_cost": Decimal("12.3456"),
            "cost": Decimal("12.34560"),
            "total_daily_display_limit": 405003,
            "total_daily_action_limit": None,
            "total_display_minutes_left_today": 59,
            "total_display_minutes_today": 120,
            "timezone": "UTC",
            "paid_till": None,
        },
    ]


@pytest.mark.parametrize(
    ["cpm", "daily_budget", "discounts", "expected_limit"],
    [
        (Decimal("50"), Decimal("500"), None, 10000),
        (Decimal("50"), None, None, None),
        (
            Decimal("50"),
            Decimal("500"),
            [
                {
                    "start_datetime": moscow_now() - timedelta(hours=1),
                    "end_datetime": moscow_now() + timedelta(hours=1),
                    "cost_multiplier": 0.5,
                }
            ],
            20000,
        ),
        (  # dicount not actual
            Decimal("50"),
            Decimal("500"),
            [
                {
                    "start_datetime": moscow_now() + timedelta(hours=1),
                    "end_datetime": moscow_now() + timedelta(hours=2),
                    "cost_multiplier": 0.5,
                }
            ],
            10000,
        ),
        (  # dicount not actual
            Decimal("50"),
            Decimal("500"),
            [
                {
                    "start_datetime": moscow_now() - timedelta(hours=2),
                    "end_datetime": moscow_now() - timedelta(hours=1),
                    "cost_multiplier": 0.5,
                }
            ],
            10000,
        ),
    ],
)
async def test_returns_total_daily_display_limit(
    cpm, daily_budget, discounts, expected_limit, campaigns_dm, factory
):
    now = moscow_now()
    campaign_id = (
        await factory.create_campaign(
            cpm={
                "cost": cpm,
                "budget": 20,
                "daily_budget": daily_budget,
                "auto_daily_budget": False,
            },
            order_id=10,
            area=dict(version=1, areas=[]),
            start_datetime=now - timedelta(hours=1),
            end_datetime=now + timedelta(hours=1),
            discounts=discounts,
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=CampaignStatusEnum.ACTIVE, changed_datetime=now
    )

    got = await campaigns_dm.list_campaigns_for_export()

    assert got[0]["total_daily_display_limit"] == expected_limit
