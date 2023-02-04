from datetime import timedelta

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import Feature, LandingVersion
from yandex.maps.proto.search import hours_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    ("open_hours_pb", "expected_schedule"),
    [
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("MONDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 11 * 60 * 60, "to": 17 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=10800,
                state=hours_pb2.State(
                    text="Сейчас оно работает",
                    short_text="Да",
                ),
            ),
            {
                "tz_offset": timedelta(seconds=10800),
                "schedule": [
                    {
                        "day": "MONDAY",
                        "opens_at": 9 * 60 * 60,
                        "closes_at": 18 * 60 * 60,
                    },
                    {
                        "day": "TUESDAY",
                        "opens_at": 11 * 60 * 60,
                        "closes_at": 17 * 60 * 60,
                    },
                ],
                "work_now_text": "Сейчас оно работает",
            },
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                            hours_pb2.DayOfWeek.Value("TUESDAY"),
                        ],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=2020,
                state=hours_pb2.State(
                    text="Сейчас оно не работает",
                    short_text="Нет",
                ),
            ),
            {
                "tz_offset": timedelta(seconds=2020),
                "schedule": [
                    {
                        "day": "MONDAY",
                        "opens_at": 9 * 60 * 60,
                        "closes_at": 18 * 60 * 60,
                    },
                    {
                        "day": "TUESDAY",
                        "opens_at": 9 * 60 * 60,
                        "closes_at": 18 * 60 * 60,
                    },
                ],
                "work_now_text": "Сейчас оно не работает",
            },
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                        ],
                        time_range=[hours_pb2.TimeRange(all_day=True)],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=2020,
                state=hours_pb2.State(
                    text="Сегодня работает весь день",
                    short_text="Да",
                ),
            ),
            {
                "tz_offset": timedelta(seconds=2020),
                "schedule": [
                    {
                        "day": "MONDAY",
                        "opens_at": 0,
                        "closes_at": 24 * 60 * 60,
                    },
                ],
                "work_now_text": "Сегодня работает весь день",
            },
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                        ],
                        time_range=[hours_pb2.TimeRange(all_day=True)],
                    ),
                ],
            ),
            {
                "schedule": [
                    {
                        "day": "MONDAY",
                        "opens_at": 0,
                        "closes_at": 24 * 60 * 60,
                    },
                ],
            },
        ),
    ],
)
async def test_uses_geosearch_client_to_fetch_schedule(
    domain, geosearch, open_hours_pb, expected_schedule
):
    geosearch.resolve_org.coro.return_value.metas["business"].open_hours.CopyFrom(
        open_hours_pb
    )

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    geosearch.resolve_org.assert_called_with(permalink=54321)
    assert result["schedule"] == expected_schedule


async def test_returns_loaded_schedule(dm, domain):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.USE_LOADED_GEOSEARCH_DATA
        else None
    )

    dm.fetch_landing_data_by_slug.coro.return_value = {
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {"permalink": "54321"},
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
            "facebook": "http://facebook.com/cafe",
            "instagram": "http://instagram.com/cafe",
            "twitter": "http://twitter.com/cafe",
            "telegram": "https://t.me/cafe",
            "viber": "https://viber.click/cafe",
            "whatsapp": "https://wa.me/cafe",
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "metrika_code",
            "external_metrika_code": "counter_number_2",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "blocks_options": {
            "show_cover": True,
            "show_logo": True,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": True,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
        "permalink": "54321",
        "landing_type": "DEFAULT",
        "instagram": None,
        "schedule": {
            "tz_offset": timedelta(seconds=10800),
            "schedule": [
                {
                    "day": "MONDAY",
                    "opens_at": 9 * 60 * 60,
                    "closes_at": 18 * 60 * 60,
                },
                {
                    "day": "TUESDAY",
                    "opens_at": 11 * 60 * 60,
                    "closes_at": 17 * 60 * 60,
                },
            ],
            "work_now_text": "Сейчас оно работает",
        },
        "photos": None,
        "chain_id": None,
        "is_updated_from_geosearch": True,
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["schedule"] == {
        "tz_offset": timedelta(seconds=10800),
        "schedule": [
            {
                "day": "MONDAY",
                "opens_at": 9 * 60 * 60,
                "closes_at": 18 * 60 * 60,
            },
            {
                "day": "TUESDAY",
                "opens_at": 11 * 60 * 60,
                "closes_at": 17 * 60 * 60,
            },
        ],
        "work_now_text": "Сейчас оно работает",
    }


async def test_does_not_return_schedule_if_no_open_hours_in_geosearch_response(
    domain, geosearch
):
    geosearch.resolve_org.coro.return_value.metas["business"].ClearField("open_hours")
    geosearch.resolve_org.coro.return_value.open_hours = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["schedule"] is None
