from datetime import timedelta
from decimal import Decimal
from copy import deepcopy

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def landing_data():
    return {
        "name": "Кафе с едой",
        "categories": ["Общепит", "Ресторан"],
        "logo": "https://images.ru/logo/%s",
        "cover": "https://images.ru/cover/%s",
        "description": "Описание",
        "contacts": {
            "phone": "+7 (495) 111-22-33",
            "website": "http://not-cafe.ru",
            "vkontakte": "http://vk.com/not-cafe",
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "888",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "blocks_options": {
            "show_cover": True,
            "show_logo": False,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    }


@pytest.mark.parametrize(
    ("saved_phone", "phone", "phones", "expected_phone"),
    [
        ("+7 (495) 739-70-00", "+7 (495) 739-70-00", ["+7 (495) 739-70-00"], "+7 (495) 739-70-00"),
        ("+7 (495) 739-70-01", "+7 (495) 739-70-00", ["+7 (495) 739-70-00"], "+7 (495) 739-70-00"),
        (
            "+7 (495) 739-70-01",
            "+7 (495) 739-70-00",
            ["+7 (495) 739-70-00", "+7 (495) 739-70-01"],
            "+7 (495) 739-70-01"
        ),
        ("+7 (495) 739-70-01", None, [], None)
    ]
)
async def test_saves_geosearch_data(factory, dm, landing_data, saved_phone, phone, phones, expected_phone):
    await factory.insert_biz_state(biz_id=15)

    landing_data = deepcopy(landing_data)
    landing_data["contacts"]["phone"] = saved_phone
    landing_data["contacts"]["phones"] = [saved_phone]

    await dm.save_landing_data_for_biz_id(biz_id=15, landing_data=landing_data, version=LandingVersion.UNSTABLE)
    await dm.save_landing_data_for_biz_id(biz_id=15, landing_data=landing_data, version=LandingVersion.STABLE)

    await dm.update_landing_data_with_geosearch(
        "54321",
        1234,
        {
            "geo": {
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "address_region": "Область",
                "country_code": "RU",
                "lat": Decimal("11.22"),
                "locality": "Город",
                "lon": Decimal("22.33"),
                "postal_code": "1234567",
                "street_address": "Улица, 1",
            },
            "phone": phone,
            "phones": phones,
            "website": "http://cafe.ru",
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
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
        ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
        "7777777",
    )

    result = await factory.list_all_landing_data()

    expected_contacts = {
        "geo": {
            "address": "Город, Улица, 1",
            "address_is_accurate": True,
            "address_region": "Область",
            "country_code": "RU",
            "lat": "11.22",
            "locality": "Город",
            "lon": "22.33",
            "postal_code": "1234567",
            "street_address": "Улица, 1",
        },
        "phone": expected_phone,
        "phones": phones,
        "website": "http://cafe.ru",
        "instagram": None,
        "facebook": None,
        "vkontakte": "http://vk.com/cafe",
        "twitter": None,
        "telegram": None,
        "viber": None,
        "whatsapp": None,
    }

    assert result == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": expected_contacts,
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "888",
                "external_metrika_code": "7777777",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "schedule": {
                "tz_offset": "3:00:00",
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
            "photos": ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
            "photo_settings": None,
            "chain_id": 1234,
            "is_updated_from_geosearch": True,
        },
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": expected_contacts,
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "888",
                "external_metrika_code": "7777777",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "schedule": {
                "tz_offset": "3:00:00",
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
            "photos": ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
            "photo_settings": None,
            "chain_id": 1234,
            "is_updated_from_geosearch": True,
        },
    ]
