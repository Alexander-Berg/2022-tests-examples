from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.adv_store.v2.lib.data_managers.campaigns import WrongBillingParameters
from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.tests import Any, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]
_type_getter = itemgetter("type_")


@pytest.fixture
def campaign_update_data(con, factory):
    async def func(campaign, **overrides):
        kwargs = dict(
            name=campaign["name"],
            author_id=campaign["author_id"],
            publication_envs=list(
                PublicationEnvEnum[pe] for pe in campaign["publication_envs"]
            ),
            start_datetime=campaign["start_datetime"],
            end_datetime=campaign["end_datetime"],
            timezone=campaign["timezone"],
            platforms=list(PlatformEnum[p] for p in campaign["platforms"]),
            rubric=RubricEnum[campaign["rubric"]] if campaign["rubric"] else None,
            order_size=OrderSizeEnum[campaign["order_size"]]
            if campaign["order_size"]
            else None,
            targeting=campaign["targeting"],
            user_daily_display_limit=campaign["user_daily_display_limit"],
            user_display_limit=campaign["user_display_limit"],
            comment=campaign["comment"],
            datatesting_expires_at=campaign["datatesting_expires_at"],
            settings=campaign["settings"],
        )

        kwargs["creatives"] = await factory.list_campaign_creatives(campaign["id"])
        kwargs["actions"] = await factory.list_campaign_actions(campaign["id"])
        kwargs["week_schedule"] = await factory.list_campaign_week_schedule(
            campaign["id"]
        )
        kwargs["placing"] = await factory.find_campaign_placing(campaign["id"])
        kwargs["status"] = CampaignStatusEnum[
            await con.fetchval(
                """
                    SELECT status
                    FROM status_history
                    WHERE campaign_id = $1
                    ORDER BY changed_datetime DESC
                    LIMIT 1
                """,
                campaign["id"],
            )
        ]
        kwargs["discounts"] = await factory.list_campaign_discounts(campaign["id"])

        kwargs.update(overrides)

        if not any(billing in overrides for billing in ("cpm", "cpa", "fix")):
            kwargs.update(await factory.find_campaign_billing(campaign["id"]))

        return kwargs

    return func


@pytest.mark.parametrize(
    ("field", "old_value", "new_value", "expected_new_value"),
    [
        ("name", "Old name", "New name", "New name"),
        (
            "publication_envs",
            [PublicationEnvEnum.DATA_TESTING],
            [PublicationEnvEnum.PRODUCTION],
            ["PRODUCTION"],
        ),
        (
            "start_datetime",
            datetime(2018, 1, 1, tzinfo=timezone.utc),
            datetime(2018, 2, 2, tzinfo=timezone.utc),
            datetime(2018, 2, 2, tzinfo=timezone.utc),
        ),
        (
            "end_datetime",
            datetime(2020, 3, 3, tzinfo=timezone.utc),
            datetime(2020, 4, 4, tzinfo=timezone.utc),
            datetime(2020, 4, 4, tzinfo=timezone.utc),
        ),
        ("timezone", "Europe/Moscow", "Asia/Novosibirsk", "Asia/Novosibirsk"),
        (
            "platforms",
            [PlatformEnum.NAVI],
            [PlatformEnum.MAPS, PlatformEnum.METRO],
            ["MAPS", "METRO"],
        ),
        ("rubric", RubricEnum.COMMON, RubricEnum.REALTY, "REALTY"),
        ("order_size", OrderSizeEnum.SMALL, OrderSizeEnum.BIG, "BIG"),
        (
            "targeting",
            {"tag": "gender", "content": "male"},
            {"tag": "gender", "content": "female"},
            {"tag": "gender", "content": "female"},
        ),
        ("user_daily_display_limit", 2, 3, 3),
        ("user_display_limit", 3, 4, 4),
        ("comment", "Old comment", "New comment", "New comment"),
        ("order_id", 12, 13, 13),
        ("manul_order_id", 22, 23, 23),
        ("settings", {}, {"custom_page_id": "aba"}, {"custom_page_id": "aba"}),
        (
            "settings",
            {},
            {"overview_position": OverviewPositionEnum.FINISH},
            {"overview_position": "FINISH"},
        ),
    ],
)
async def test_updates_simple_fields(
    factory,
    con,
    campaigns_dm,
    campaign_update_data,
    field,
    old_value,
    new_value,
    expected_new_value,
):
    campaign = await factory.create_campaign(**{field: old_value})

    update_data = await campaign_update_data(campaign, **{field: new_value})
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    actual_new_value = await con.fetchval(
        f"""
            SELECT {field}
            FROM campaign
            WHERE id = $1
        """,
        campaign["id"],
    )

    assert actual_new_value == expected_new_value


@pytest.mark.parametrize(("field", "old_value", "new_value"), [("author_id", 123, 321)])
async def test_not_updates_some_fields(
    factory, con, campaigns_dm, campaign_update_data, field, old_value, new_value
):
    campaign = await factory.create_campaign(**{field: old_value})

    update_data = await campaign_update_data(campaign, **{field: new_value})
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    actual_new_value = await con.fetchval(
        f"""
            SELECT {field}
            FROM campaign
            WHERE id = $1
        """,
        campaign["id"],
    )

    assert actual_new_value == old_value


async def test_returns_campaign_details(factory, campaigns_dm, campaign_update_data):
    campaign = await factory.create_campaign()

    update_data = await campaign_update_data(
        campaign,
        name="New name",
        author_id=321,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        start_datetime=dt("2019-03-03 00:00:00"),
        end_datetime=dt("2019-04-04 00:00:00"),
        timezone="Europe/Moscow",
        platforms=[PlatformEnum.NAVI, PlatformEnum.MAPS],
        cpa={
            "cost": Decimal("20"),
            "budget": Decimal("3000"),
            "daily_budget": Decimal("1000"),
            "auto_daily_budget": False,
        },
        comment="New comment",
        actions=[
            {"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}
        ],
        creatives=[
            {
                "type_": "billboard",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "images_v2": [],
            }
        ],
        placing={
            "area": {
                "areas": [
                    {
                        "points": [
                            {"lon": 12.345, "lat": 23.567},
                            {"lon": 14.345, "lat": 23.567},
                            {"lon": 14.345, "lat": 25.567},
                            {"lon": 12.345, "lat": 23.567},
                        ],
                        "name": "Танковый полигон",
                    }
                ],
                "version": 1,
            }
        },
        rubric=RubricEnum.REALTY,
        order_size=OrderSizeEnum.BIG,
        status=CampaignStatusEnum.REVIEW,
        targeting={"tag": "gender", "content": "male"},
        user_daily_display_limit=5,
        user_display_limit=6,
        week_schedule=[{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
        order_id=None,
        manul_order_id=12,
        datatesting_expires_at=dt("2019-04-07 00:00:00"),
        settings={},
    )
    result = await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert result == dict(
        id=Any(int),
        name="New name",
        author_id=123,
        created_datetime=Any(datetime),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        start_datetime=dt("2019-03-03 00:00:00"),
        end_datetime=dt("2019-04-04 00:00:00"),
        timezone="Europe/Moscow",
        platforms=[PlatformEnum.NAVI, PlatformEnum.MAPS],
        billing={
            "cpa": {
                "cost": Decimal("20"),
                "budget": Decimal("3000"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }
        },
        comment="New comment",
        actions=[
            {"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}
        ],
        creatives=[
            {
                "type_": "billboard",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "images_v2": [],
                "title": None,
                "description": None,
            }
        ],
        placing={
            "area": {
                "areas": [
                    {
                        "points": [
                            {"lon": 12.345, "lat": 23.567},
                            {"lon": 14.345, "lat": 23.567},
                            {"lon": 14.345, "lat": 25.567},
                            {"lon": 12.345, "lat": 23.567},
                        ],
                        "name": "Танковый полигон",
                    }
                ],
                "version": 1,
            }
        },
        rubric=RubricEnum.REALTY,
        order_size=OrderSizeEnum.BIG,
        status=CampaignStatusEnum.REVIEW,
        targeting={"tag": "gender", "content": "male"},
        user_daily_display_limit=5,
        user_display_limit=6,
        week_schedule=[{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
        discounts=[],
        order_id=None,
        manul_order_id=12,
        datatesting_expires_at=dt("2019-04-07 00:00:00"),
        settings={},
        moderation_verdicts=None,
        direct_moderation_id=None,
        paid_till=None,
    )


@pytest.mark.parametrize(
    ("old_billing_data", "new_billing_data"),
    [
        (
            {
                "cpm": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "cpm": {
                    "cost": Decimal("20.0"),
                    "budget": Decimal("6000"),
                    "daily_budget": Decimal("2000"),
                    "auto_daily_budget": False,
                }
            },
        ),
        (
            {
                "cpm": {
                    "cost": Decimal("10.0"),
                    "budget": Decimal("5000"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {
                "fix": {
                    "cost": Decimal("20.0"),
                    "time_interval": FixTimeIntervalEnum.MONTHLY,
                }
            },
        ),
    ],
)
async def test_updates_billing(
    factory, campaigns_dm, campaign_update_data, old_billing_data, new_billing_data
):
    campaign = await factory.create_campaign(**old_billing_data)

    update_data = await campaign_update_data(campaign, **new_billing_data)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.find_campaign_billing(campaign["id"]) == new_billing_data


@pytest.mark.parametrize(
    "billing_extra",
    (
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            },
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": True,
            },
        },
        {
            "cpa": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": True,
            },
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            },
        },
        {
            "cpm": {
                "cost": Decimal("10.1234"),
                "budget": Decimal("500"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            },
            "fix": {
                "time_interval": FixTimeIntervalEnum.DAILY,
                "cost": Decimal("10.1234"),
            },
        },
    ),
)
async def test_raises_for_many_billing_arguments(
    factory, campaigns_dm, campaign_update_data, billing_extra
):
    campaign = await factory.create_campaign()

    update_data = await campaign_update_data(campaign, **billing_extra)
    with pytest.raises(WrongBillingParameters):
        await campaigns_dm.update_campaign(campaign["id"], **update_data)


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
@pytest.mark.real_db
async def test_adds_status_log_entry(
    factory, campaigns_dm, campaign_update_data, status
):
    campaign = await factory.create_campaign(author_id=123)

    update_data = await campaign_update_data(campaign, author_id=321, status=status)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    status_entries = await factory.list_campaign_status_history(campaign["id"])
    assert status_entries == [
        {
            "campaign_id": campaign["id"],
            "author_id": 123,
            "status": CampaignStatusEnum.DRAFT.name,
            "metadata": {},
            "changed_datetime": Any(datetime),
        },
        {
            "campaign_id": campaign["id"],
            "author_id": 321,
            "status": status.name,
            "metadata": {},
            "changed_datetime": Any(datetime),
        },
    ]


@pytest.mark.parametrize(
    ("old_creatives", "new_creatives"),
    [
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                }
            ],
            [
                {
                    "type_": "pin",
                    "title": "Заголовок 2",
                    "subtitle": "Подзаголовок 2",
                    "images": [{"file1": "filename3"}, {"file2": "filename4"}],
                }
            ],
        ),
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                }
            ],
            [
                {
                    "type_": "billboard",
                    "title": "Заголовок",
                    "description": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "images_v2": [],
                }
            ],
        ),
        (
            [],
            [
                {
                    "type_": "pin",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                },
                {
                    "type_": "billboard",
                    "title": "Заголовок",
                    "description": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "images_v2": [],
                },
                {
                    "type_": "icon",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "position": 8,
                    "title": "Заголовок",
                },
                {
                    "type_": "pin_search",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "title": "Заголовок",
                    "organizations": [1, 2, 4],
                },
                {
                    "type_": "logo_and_text",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "text": "Текст",
                },
                {"type_": "text", "text": "Текст", "disclaimer": "Дисклеймер"},
                {
                    "type_": "via_point",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "button_text_active": "Кнопкотекст",
                    "button_text_inactive": "Неактивнокнопкотекст",
                    "description": "Описание",
                },
                {
                    "type_": "banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                    "terms": "Условия",
                },
                {
                    "type_": "audio_banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "left_anchor": 0.5,
                    "audio_file_url": "http://filestorage.ru/my_file.mp3",
                },
            ],
        ),
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                },
                {
                    "type_": "billboard",
                    "title": "Заголовок",
                    "description": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "images_v2": [],
                },
                {
                    "type_": "icon",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "position": 8,
                    "title": "Заголовок",
                },
                {
                    "type_": "pin_search",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "title": "Заголовок",
                    "organizations": [1, 2, 4],
                },
                {
                    "type_": "logo_and_text",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "text": "Текст",
                },
                {"type_": "text", "text": "Текст", "disclaimer": "Дисклеймер"},
                {"type_": "text", "text": "Текст 1", "disclaimer": "Дисклеймер 2"},
                {
                    "type_": "via_point",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "button_text_active": "Кнопкотекст",
                    "button_text_inactive": "Неактивнокнопкотекст",
                    "description": "Описание",
                },
                {
                    "type_": "banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "disclaimer": "Дисклеймер",
                    "show_ads_label": True,
                    "description": "Описание",
                    "title": "Заголовок",
                },
                {
                    "type_": "audio_banner",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "left_anchor": 0.5,
                    "audio_file_url": "http://filestorage.ru/my_file.mp3",
                },
            ],
            [],
        ),
    ],
)
async def test_updates_creatives(
    factory, campaigns_dm, campaign_update_data, old_creatives, new_creatives
):
    campaign = await factory.create_campaign(creatives=old_creatives)

    update_data = await campaign_update_data(campaign, creatives=new_creatives)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    creatives = await factory.list_campaign_creatives(campaign["id"])

    assert sorted(creatives, key=_type_getter) == sorted(
        new_creatives, key=_type_getter
    )


@pytest.mark.parametrize(
    ("old_actions", "new_actions"),
    [
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                }
            ],
        ),
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
        ),
        (
            [],
            [
                {
                    "type_": "open_site",
                    "title": "Перейти",
                    "url": "ya.ru",
                    "main": False,
                },
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": "Позвонить",
                    "phone": "322-223",
                    "main": False,
                },
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                },
                {
                    "type_": "download_app",
                    "title": "Позвонить",
                    "google_play_id": "qwerty",
                    "app_store_id": "ytrewq",
                    "url": "yandex.ru",
                    "main": False,
                },
                {"type_": "promocode", "promocode": "123", "main": False},
                {
                    "type_": "resolve_uri",
                    "uri": "magic://ya.ru",
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
        ),
        (
            [
                {
                    "type_": "open_site",
                    "title": "Перейти",
                    "url": "ya.ru",
                    "main": False,
                },
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": "Позвонить",
                    "phone": "322-223",
                    "main": False,
                },
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                },
                {
                    "type_": "download_app",
                    "title": "Позвонить",
                    "google_play_id": "qwerty",
                    "app_store_id": "ytrewq",
                    "url": "yandex.ru",
                    "main": False,
                },
                {"type_": "promocode", "promocode": "123", "main": False},
                {
                    "type_": "resolve_uri",
                    "uri": "magic://ya.ru",
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
            [],
        ),
    ],
)
async def test_updates_actions(
    factory, campaigns_dm, campaign_update_data, old_actions, new_actions
):
    campaign = await factory.create_campaign(actions=old_actions)

    update_data = await campaign_update_data(campaign, actions=new_actions)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    actions = await factory.list_campaign_actions(campaign["id"])
    assert sorted(actions, key=_type_getter) == sorted(new_actions, key=_type_getter)


@pytest.mark.parametrize(
    ("old_placing", "new_placing"),
    [
        (
            {"organizations": {"permalinks": [213, 456]}},
            {"organizations": {"permalinks": [777, 888]}},
        ),
        (
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 112.345, "lat": 123.567},
                                {"lon": 114.345, "lat": 123.567},
                                {"lon": 114.345, "lat": 125.567},
                                {"lon": 112.345, "lat": 123.567},
                            ],
                            "name": "Танковый полигон 2",
                        },
                        {
                            "points": [
                                {"lon": 122.345, "lat": 133.567},
                                {"lon": 124.345, "lat": 133.567},
                                {"lon": 124.345, "lat": 135.567},
                                {"lon": 122.345, "lat": 133.567},
                            ],
                            "name": "Ядерный полигон 2",
                        },
                    ],
                    "version": 2,
                }
            },
        ),
        (
            {"organizations": {"permalinks": [213, 456]}},
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
        ),
        (
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
            {"organizations": {"permalinks": [213, 456]}},
        ),
    ],
)
async def test_updates_placing(
    factory, campaigns_dm, campaign_update_data, old_placing, new_placing
):
    campaign = await factory.create_campaign(**old_placing)

    update_data = await campaign_update_data(campaign, placing=new_placing)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.find_campaign_placing(campaign["id"]) == new_placing


@pytest.mark.parametrize(
    ("old_week_schedule", "new_week_schedule"),
    [
        (
            [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
            [{"start": 60 * 24 * 5, "end": 60 * 24 * 6}],
        ),
        (
            [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
        ),
        (
            [],
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
        ),
        (
            [
                {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
                {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
            ],
            [],
        ),
    ],
)
async def test_updates_week_schedules(
    factory, campaigns_dm, campaign_update_data, old_week_schedule, new_week_schedule
):
    campaign = await factory.create_campaign(week_schedule=old_week_schedule)

    update_data = await campaign_update_data(campaign, week_schedule=new_week_schedule)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert (
        await factory.list_campaign_week_schedule(campaign["id"]) == new_week_schedule
    )


@pytest.mark.parametrize(
    ("old_discounts", "new_discounts"),
    [
        (
            [],
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                },
                {
                    "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("1.35"),
                },
            ],
            [],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
            [
                {
                    "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("1.35"),
                }
            ],
        ),
    ],
)
async def test_not_updates_discounts(
    factory, campaigns_dm, campaign_update_data, old_discounts, new_discounts
):
    campaign = await factory.create_campaign(discounts=old_discounts)

    update_data = await campaign_update_data(campaign, discounts=new_discounts)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.list_campaign_discounts(campaign["id"]) == old_discounts


@pytest.mark.parametrize(
    "old_billing",
    [
        {
            "cpm": {
                "cost": Decimal("20"),
                "budget": Decimal("3000"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": False,
            }
        },
        {
            "cpa": {
                "cost": Decimal("20"),
                "budget": Decimal("3000"),
                "daily_budget": Decimal("1000"),
                "auto_daily_budget": True,
            }
        },
    ],
)
@pytest.mark.parametrize(
    "new_billing",
    [
        {
            "cpm": {
                "cost": Decimal("50"),
                "budget": Decimal("6000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        {
            "cpa": {
                "cost": Decimal("50"),
                "budget": Decimal("6000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
    ],
)
async def test_not_changes_other_campaigns_billing(
    factory, campaigns_dm, campaign_update_data, old_billing, new_billing
):
    campaign = await factory.create_campaign(**old_billing)
    other_campaign = await factory.create_campaign(
        cpm={
            "cost": Decimal("30"),
            "budget": Decimal("4000"),
            "daily_budget": Decimal("2000"),
            "auto_daily_budget": False,
        }
    )

    update_data = await campaign_update_data(campaign, **new_billing)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.find_campaign_billing(other_campaign["id"]) == {
        "cpm": {
            "cost": Decimal("30"),
            "budget": Decimal("4000"),
            "daily_budget": Decimal("2000"),
            "auto_daily_budget": False,
        }
    }


@pytest.mark.parametrize(
    ("old_creatives", "new_creatives"),
    [
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок",
                    "subtitle": "Подзаголовок",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                }
            ],
            [
                {
                    "type_": "pin",
                    "title": "Заголовок 2",
                    "subtitle": "Подзаголовок 2",
                    "images": [{"file1": "filename3"}, {"file2": "filename4"}],
                }
            ],
        ),
        (
            [
                {
                    "type_": "billboard",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "images_v2": [],
                }
            ],
            [
                {
                    "type_": "pin",
                    "title": "Заголовок 2",
                    "subtitle": "Подзаголовок 2",
                    "images": [{"file1": "filename3"}, {"file2": "filename4"}],
                }
            ],
        ),
        (
            [
                {
                    "type_": "pin",
                    "title": "Заголовок 2",
                    "subtitle": "Подзаголовок 2",
                    "images": [{"file1": "filename3"}, {"file2": "filename4"}],
                }
            ],
            [
                {
                    "type_": "billboard",
                    "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                    "images_v2": [],
                }
            ],
        ),
    ],
)
async def test_not_changes_other_campaigns_creatives(
    factory, campaigns_dm, campaign_update_data, old_creatives, new_creatives
):
    campaign = await factory.create_campaign(creatives=old_creatives)
    other_campaign = await factory.create_campaign(
        creatives=[
            {
                "type_": "billboard",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "images_v2": [],
            }
        ]
    )

    update_data = await campaign_update_data(campaign, creatives=new_creatives)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    creatives = await factory.list_campaign_creatives(other_campaign["id"])
    assert creatives == [
        {
            "type_": "billboard",
            "images": [{"file1": "filename1"}, {"file2": "filename2"}],
            "images_v2": [],
            "title": None,
            "description": None,
        }
    ]


@pytest.mark.parametrize(
    ("old_actions", "new_actions"),
    [
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [
                {
                    "type_": "open_site",
                    "title": "Подойти",
                    "url": "yandex.ru",
                    "main": False,
                }
            ],
        ),
        (
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
            [
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
        ),
        (
            [
                {
                    "type_": "search",
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "main": False,
                }
            ],
            [{"type_": "open_site", "title": "Перейти", "url": "ya.ru", "main": False}],
        ),
    ],
)
async def test_not_changes_other_campaigns_actions(
    factory, campaigns_dm, campaign_update_data, old_actions, new_actions
):
    campaign = await factory.create_campaign(actions=old_actions)
    other_campaign = await factory.create_campaign(
        actions=[
            {
                "type_": "open_site",
                "title": "Перейти туда",
                "url": "yandex.ru",
                "main": False,
            }
        ]
    )

    update_data = await campaign_update_data(campaign, actions=new_actions)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    actions = await factory.list_campaign_actions(other_campaign["id"])

    assert actions == [
        {
            "type_": "open_site",
            "title": "Перейти туда",
            "url": "yandex.ru",
            "main": False,
        }
    ]


@pytest.mark.parametrize(
    ("old_placing", "new_placing"),
    [
        (
            {"organizations": {"permalinks": [213, 456]}},
            {"organizations": {"permalinks": [777, 888]}},
        ),
        (
            {"organizations": {"permalinks": [213, 456]}},
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
        ),
        (
            {
                "area": {
                    "areas": [
                        {
                            "points": [
                                {"lon": 12.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 23.567},
                                {"lon": 14.345, "lat": 25.567},
                                {"lon": 12.345, "lat": 23.567},
                            ],
                            "name": "Танковый полигон",
                        },
                        {
                            "points": [
                                {"lon": 22.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 33.567},
                                {"lon": 24.345, "lat": 35.567},
                                {"lon": 22.345, "lat": 33.567},
                            ],
                            "name": "Ядерный полигон",
                        },
                    ],
                    "version": 1,
                }
            },
            {"organizations": {"permalinks": [213, 456]}},
        ),
    ],
)
async def test_not_changes_other_campaigns_placing(
    factory, campaigns_dm, campaign_update_data, old_placing, new_placing
):
    campaign = await factory.create_campaign(**old_placing)
    other_campaign = await factory.create_campaign(
        organizations={"permalinks": [1000, 2000]}
    )

    update_data = await campaign_update_data(campaign, placing=new_placing)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.find_campaign_placing(other_campaign["id"]) == {
        "organizations": {"permalinks": [1000, 2000]}
    }


@pytest.mark.parametrize(
    ("old_week_schedule", "new_week_schedule"),
    [
        (
            [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
            [{"start": 60 * 24 * 5, "end": 60 * 24 * 6}],
        ),
        ([{"start": 60 * 24 * 3, "end": 60 * 24 * 4}], []),
        ([], [{"start": 60 * 24 * 5, "end": 60 * 24 * 6}]),
    ],
)
async def test_not_changes_other_campaigns_week_schedules(
    factory, campaigns_dm, campaign_update_data, old_week_schedule, new_week_schedule
):
    campaign = await factory.create_campaign(week_schedule=old_week_schedule)
    other_campaign = await factory.create_campaign(
        week_schedule=[{"start": 60 * 24 * 1, "end": 60 * 24 * 2}]
    )

    update_data = await campaign_update_data(campaign, week_schedule=new_week_schedule)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.list_campaign_week_schedule(other_campaign["id"]) == [
        {"start": 60 * 24 * 1, "end": 60 * 24 * 2}
    ]


@pytest.mark.parametrize(
    ("old_discounts", "new_discounts"),
    [
        (
            [],
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                },
                {
                    "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("1.35"),
                },
            ],
            [],
        ),
        (
            [
                {
                    "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("0.75"),
                }
            ],
            [
                {
                    "start_datetime": datetime(2019, 3, 3, tzinfo=timezone.utc),
                    "end_datetime": datetime(2019, 4, 4, tzinfo=timezone.utc),
                    "cost_multiplier": Decimal("1.35"),
                }
            ],
        ),
    ],
)
async def test_not_changes_other_campaigns_discounts(
    factory, campaigns_dm, campaign_update_data, old_discounts, new_discounts
):
    campaign = await factory.create_campaign(discounts=old_discounts)
    other_campaign = await factory.create_campaign(
        discounts=[
            {
                "start_datetime": datetime(2020, 5, 5, tzinfo=timezone.utc),
                "end_datetime": datetime(2020, 6, 6, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.5"),
            }
        ]
    )

    update_data = await campaign_update_data(campaign, discounts=new_discounts)
    await campaigns_dm.update_campaign(campaign["id"], **update_data)

    assert await factory.list_campaign_discounts(other_campaign["id"]) == [
        {
            "start_datetime": datetime(2020, 5, 5, tzinfo=timezone.utc),
            "end_datetime": datetime(2020, 6, 6, tzinfo=timezone.utc),
            "cost_multiplier": Decimal("0.5"),
        }
    ]


async def test_adds_change_log_record_for_updated_campaign(
    factory, campaigns_dm, campaign_update_data
):
    campaign = await factory.create_campaign()
    campaign_id = campaign["id"]

    update_data = await campaign_update_data(campaign, status=CampaignStatusEnum.PAUSED)
    await campaigns_dm.update_campaign(campaign_id, **update_data)

    result = await factory.list_campaign_change_log(campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 123,
            "status": "PAUSED",
            "system_metadata": {"action": "campaign.updated"},
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
    assert result[0]["state_before"]["current_status_history"]["status"] == "DRAFT"
    assert result[0]["state_after"]["current_status_history"]["status"] == "PAUSED"
    assert result[0]["state_before"] != result[0]["state_after"]
