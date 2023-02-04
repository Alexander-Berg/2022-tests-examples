from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.adv_store.v2.lib.data_managers.exceptions import CampaignNotFound
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    OverviewPositionEnum,
    PlatformEnum,
    PublicationEnvEnum,
    RubricEnum,
)
from maps_adv.adv_store.v2.tests import Any, all_combinations, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


def _dict_values(d):
    keys = sorted(d.keys())
    values = itemgetter(*keys)(d)
    return values


def _sort_dicts(l):
    return sorted(l, key=_dict_values)


async def test_return_data(factory, campaigns_dm):
    campaign = await factory.create_campaign()

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result == {
        "id": campaign["id"],
        "author_id": 123,
        "created_datetime": Any(datetime),
        "name": "campaign0",
        "timezone": "UTC",
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
        "end_datetime": datetime(2019, 2, 1, tzinfo=timezone.utc),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 10,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [],
        "creatives": [],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": None,
        "settings": {},
        "moderation_verdicts": None,
        "direct_moderation_id": None,
        "paid_till": None,
    }


@pytest.mark.parametrize(
    "field_values",
    [
        {"author_id": 22},
        {"name": "Название кампании"},
        {"timezone": "Asia/Novosibirsk"},
        {"comment": "Комментарий текстом"},
        {"user_display_limit": 10},
        {"user_daily_display_limit": 2},
        {"targeting": {"age": "25-34"}},
        {"settings": {"custom_page_id": "abacaba"}},
        {"settings": {"overview_position": OverviewPositionEnum.FINISH}},
    ],
)
async def test_returns_common_campaign_data(factory, campaigns_dm, field_values):
    campaign = await factory.create_campaign(**field_values)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    values_to_check = {key: result[key] for key in field_values.keys()}

    assert values_to_check == field_values


@pytest.mark.parametrize(
    "value",
    [
        [PublicationEnvEnum.PRODUCTION],
        [PublicationEnvEnum.DATA_TESTING],
        [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
    ],
)
async def test_returns_publications_envs(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(publication_envs=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["publication_envs"] == value


@pytest.mark.parametrize("value", list(CampaignTypeEnum))
async def test_returns_campaign_type(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(campaign_type=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["campaign_type"] == value


@pytest.mark.parametrize("value", all_combinations(PlatformEnum))
async def test_returns_platforms(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(platforms=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["platforms"] == list(value)


@pytest.mark.parametrize("value", [None] + list(RubricEnum))
async def test_return_rubric(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(rubric=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["rubric"] == value


@pytest.mark.parametrize("value", [None] + list(OrderSizeEnum))
async def test_return_order_size(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(order_size=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["order_size"] == value


@pytest.mark.parametrize("value", [dt("2000-04-07 10:30:00"), None])
async def test_return_datatesting_expires_at(factory, campaigns_dm, value):
    campaign = await factory.create_campaign(datatesting_expires_at=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["datatesting_expires_at"] == value


@pytest.mark.parametrize(("order_id", "manul_order_id"), [(1, None), (None, 2)])
async def test_order_id_and_manul_order_id(
    factory, campaigns_dm, order_id, manul_order_id
):
    campaign = await factory.create_campaign(
        order_id=order_id, manul_order_id=manul_order_id
    )

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["order_id"] == order_id
    assert result["manul_order_id"] == manul_order_id


@pytest.mark.parametrize("budget", [None, Decimal("12.3456")])
@pytest.mark.parametrize("daily_budget", [None, Decimal("12.3456")])
@pytest.mark.parametrize("auto_daily_budget", [True, False])
async def test_returns_billing_cpm_data(
    budget, daily_budget, auto_daily_budget, factory, campaigns_dm
):
    value = {
        "cost": Decimal("10.20"),
        "budget": budget,
        "daily_budget": daily_budget,
        "auto_daily_budget": auto_daily_budget,
    }
    campaign = await factory.create_campaign(cpm=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["billing"] == {"cpm": value}


@pytest.mark.parametrize("budget", [None, Decimal("12.3456")])
@pytest.mark.parametrize("daily_budget", [None, Decimal("12.3456")])
@pytest.mark.parametrize("auto_daily_budget", [True, False])
async def test_returns_billing_cpa_data(
    budget, daily_budget, auto_daily_budget, factory, campaigns_dm
):
    value = {
        "cost": Decimal("10.20"),
        "budget": budget,
        "daily_budget": daily_budget,
        "auto_daily_budget": auto_daily_budget,
    }
    campaign = await factory.create_campaign(cpa=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["billing"] == {"cpa": value}


@pytest.mark.parametrize("time_interval", list(FixTimeIntervalEnum))
async def test_returns_billing_fix_data(factory, campaigns_dm, time_interval):
    value = {"cost": Decimal("10.20"), "time_interval": time_interval}
    campaign = await factory.create_campaign(fix=value)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["billing"] == {"fix": value}


@pytest.mark.parametrize(
    "actions",
    [
        [],
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
        [
            {
                "type_": "phone_call",
                "title": "Позвонить",
                "phone": "322-223",
                "main": False,
            }
        ],
        [
            {
                "type_": "download_app",
                "title": "Установить",
                "google_play_id": "qwerty",
                "app_store_id": "ytrewq",
                "url": "yandex.ru",
                "main": False,
            }
        ],
        [{"type_": "promocode", "promocode": "123", "main": False}],
        [
            {
                "type_": "add_point_to_route",
                "latitude": 1.2,
                "longitude": 3.4,
                "main": False,
            }
        ],
        [
            {
                "type_": "open_site",
                "title": "Перейти туда",
                "url": "ya.ru",
                "main": False,
            },
            {
                "type_": "open_site",
                "title": "Перейти сюда",
                "url": "yandex.ru",
                "main": False,
            },
            {
                "type_": "phone_call",
                "title": "Позвонить",
                "phone": "322-223",
                "main": False,
            },
        ],
        [
            {
                "type_": "phone_call",
                "title": "Позвонить",
                "phone": "322-223",
                "main": True,
            }
        ],
    ],
)
async def test_returns_actions_data(factory, campaigns_dm, actions):
    campaign = await factory.create_campaign(actions=actions)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert _sort_dicts(result["actions"]) == _sort_dicts(actions)


@pytest.mark.parametrize(
    "creatives",
    [
        [],
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
        [
            {
                "type_": "icon",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "position": 8,
                "title": "Заголовок",
            }
        ],
        [
            {
                "type_": "pin_search",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "title": "Заголовок",
                "organizations": [1, 2, 4],
            }
        ],
        [
            {
                "type_": "logo_and_text",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "text": "Текст",
            }
        ],
        [{"type_": "text", "text": "Текст", "disclaimer": "Дисклеймер"}],
        [
            {
                "type_": "via_point",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "button_text_active": "Кнопкотекст",
                "button_text_inactive": "Неактивнокнопкотекст",
                "description": "Описание",
            }
        ],
        [
            {
                "type_": "banner",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "Условия",
            }
        ],
        [
            {
                "type_": "audio_banner",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "left_anchor": 0.5,
                "audio_file_url": "http://filestorage.ru/my_file.mp3",
            }
        ],
        [
            {"type_": "text", "text": "Текст 1", "disclaimer": "Дисклеймер 1"},
            {"type_": "text", "text": "Текст 2", "disclaimer": "Дисклеймер 2"},
            {
                "type_": "banner",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "Условия",
            },
        ],
    ],
)
async def test_returns_creatives_data(factory, campaigns_dm, creatives):
    campaign = await factory.create_campaign(creatives=creatives)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert _sort_dicts(result["creatives"]) == _sort_dicts(creatives)


@pytest.mark.parametrize(
    ("organizations", "areas"),
    [
        ({"permalinks": [765, 123]}, None),
        (
            {},
            {
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
            },
        ),
        (
            {"permalinks": [765, 123]},
            {
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
            },
        ),
        ({}, {}),
    ],
)
async def test_returns_placing_data(factory, campaigns_dm, organizations, areas):
    campaign = await factory.create_campaign(organizations=organizations, area=areas)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    expected_data = {}
    if organizations:
        expected_data["organizations"] = organizations
    if areas:
        expected_data["area"] = areas
    assert result["placing"] == expected_data


@pytest.mark.parametrize(
    "week_schedule",
    [
        [{"start": 60 * 24 * 3, "end": 60 * 24 * 4}],
        [
            {"start": 60 * 24 * 3, "end": 60 * 24 * 4},
            {"start": 60 * 24 * 5, "end": 60 * 24 * 6},
        ],
        [],
    ],
)
async def test_returns_week_schedule_data(factory, campaigns_dm, week_schedule):
    campaign = await factory.create_campaign(week_schedule=week_schedule)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["week_schedule"] == week_schedule


@pytest.mark.parametrize(
    ("statuses", "expected_status"),
    list(map(lambda s: ([s], s), CampaignStatusEnum))
    + [
        (
            [CampaignStatusEnum.DONE, CampaignStatusEnum.ACTIVE],
            CampaignStatusEnum.ACTIVE,
        ),
        (
            [
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
            ],
            CampaignStatusEnum.PAUSED,
        ),
    ],
)
async def test_returns_current_status(factory, campaigns_dm, statuses, expected_status):
    campaign = await factory.create_campaign()
    for ts, status in enumerate(statuses, start=1):
        await factory.set_status(
            campaign["id"],
            status=status,
            changed_datetime=datetime.fromtimestamp(ts, tz=timezone.utc),
        )

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["status"] == expected_status


@pytest.mark.parametrize(
    "discounts",
    [
        [],
        [
            {
                "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
                "end_datetime": datetime(2019, 2, 2, tzinfo=timezone.utc),
                "cost_multiplier": Decimal("0.75"),
            }
        ],
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
    ],
)
async def test_returns_discounts(factory, campaigns_dm, discounts):
    campaign = await factory.create_campaign(discounts=discounts)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["discounts"] == discounts


async def test_returns_created_datetime(factory, con, campaigns_dm):
    campaign = await factory.create_campaign()
    await con.execute(
        "DELETE FROM status_history WHERE campaign_id = $1", campaign["id"]
    )
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-01-02 21:34:00"))
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-05-05 21:34:00"))
    await factory.set_status(campaign["id"], changed_datetime=dt("2000-05-05 10:30:00"))

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["created_datetime"] == dt("2000-01-02 21:34:00")


async def test_raises_if_campaign_not_exists(campaigns_dm):
    with pytest.raises(CampaignNotFound):
        await campaigns_dm.retrieve_campaign(123)


async def test_return_data_with_moderation_verdicts(factory, campaigns_dm):
    campaign = await factory.create_campaign()
    moderation_id = await factory.create_campaign_direct_moderation(
        campaign_id=campaign["id"], verdicts=[111222, 222333]
    )
    await campaigns_dm.set_direct_moderation(campaign["id"], moderation_id)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result == {
        "id": campaign["id"],
        "author_id": 123,
        "created_datetime": Any(datetime),
        "name": "campaign0",
        "timezone": "UTC",
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "start_datetime": datetime(2019, 1, 1, tzinfo=timezone.utc),
        "end_datetime": datetime(2019, 2, 1, tzinfo=timezone.utc),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 10,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [],
        "creatives": [],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": None,
        "settings": {},
        "moderation_verdicts": [111222, 222333],
        "direct_moderation_id": moderation_id,
        "paid_till": None,
    }
