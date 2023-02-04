from decimal import Decimal

import pytest
from smb.common.testing_utils import Any

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
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
            "geo": {
                "permalink": "54321",
                "lat": "99.99",
                "lon": "88.88",
                "address": "City, Street, 999",
                "address_is_accurate": True,
                "locality": "Город",
            },
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
        "landing_type": "INSTAGRAM",
    }


async def test_saves_geosearch_data(factory, dm, landing_data):
    st_id = await factory.insert_landing_data(**landing_data)
    unst_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(biz_id=15, stable_version=st_id, unstable_version=unst_id)

    await dm.update_instagram_landing_data_with_geosearch(
        "54321",
        {
            "permalink": "54321",
            "lat": Decimal("11.22"),
            "lon": Decimal("22.33"),
            "address": "Город, Улица, 1",
            "address_is_accurate": True,
            "locality": "Город",
        },
        "7777777",
    )

    result = await factory.list_all_landing_data()

    assert result == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": {
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
                "geo": {
                    "permalink": "54321",
                    "lat": "11.22",
                    "lon": "22.33",
                    "address": "Город, Улица, 1",
                    "address_is_accurate": True,
                    "locality": "Город",
                },
            },
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
            "landing_type": "INSTAGRAM",
            "instagram": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "chain_id": None,
            "is_updated_from_geosearch": True,
        },
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": {
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
                "geo": {
                    "permalink": "54321",
                    "lat": "11.22",
                    "lon": "22.33",
                    "address": "Город, Улица, 1",
                    "address_is_accurate": True,
                    "locality": "Город",
                },
            },
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
            "landing_type": "INSTAGRAM",
            "instagram": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "chain_id": None,
            "is_updated_from_geosearch": True,
        },
    ]
