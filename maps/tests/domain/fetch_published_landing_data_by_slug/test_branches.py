import datetime
from decimal import Decimal

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.landlord.server.lib.enums import Feature, LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_return_data_with_loaded_geosearch(dm, domain):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.USE_LOADED_GEOSEARCH_DATA
        else None
    )

    dm.fetch_landing_data_by_slug.coro.return_value = {
        "biz_id": 12,
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
            "show_branches": True,
        },
        "permalink": "54321",
        "landing_type": "DEFAULT",
        "instagram": None,
        "schedule": None,
        "photos": None,
        "chain_id": 1234,
        "is_updated_from_geosearch": True,
    }

    dm.fetch_branches_for_permalink.coro.return_value = [
        {
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 2",
                    "address_is_accurate": True,
                    "lat": "22.22",
                    "locality": "Город",
                    "lon": "22.22",
                    "permalink": "6789",
                    "country_code": "RU",
                    "postal_code": "1234567",
                    "address_region": "Область",
                    "street_address": "Улица, 2",
                },
                "phone": "+7 (495) 222-20-00",
                "vkontakte": "http://vk.com/cafe2",
                "website": "http://cafe2.ru",
            },
            "cover": "https://images.com/cover2",
            "description": "Кафе 2",
            "logo": "https://images.com/logo2",
            "name": "Кафе 2",
            "permalink": "6789",
            "slug": "slug1",
            "preferences": {
                "personal_metrika_code": "metrika_code",
                "external_metrika_code": "counter_number_1",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
        },
        {
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 3",
                    "address_is_accurate": True,
                    "lat": "33.33",
                    "locality": "Город",
                    "lon": "33.33",
                    "permalink": "7890",
                    "country_code": "RU",
                    "postal_code": "1234567",
                    "address_region": "Область",
                    "street_address": "Улица, 3",
                },
                "phone": "+7 (495) 333-30-00",
                "vkontakte": "http://vk.com/cafe3",
                "website": "http://cafe3.ru",
            },
            "cover": "https://images.com/cover3",
            "description": "Кафе 3",
            "logo": "https://images.com/logo3",
            "name": "Кафе 3",
            "permalink": "7890",
            "slug": "slug2",
            "preferences": {
                "personal_metrika_code": "metrika_code",
                "external_metrika_code": "counter_number_1",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
        },
    ]
    dm.fetch_vk_pixels_for_permalink.coro.return_value = None
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 12,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "photos": None,
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
            "is_substitution_phone": False,
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "metrika_code",
            "external_metrika_code": "counter_number_2",
            "color_theme": {
                "theme": "LIGHT",
                "main_color_hex": "FB524F",
                "text_color_over_main": "LIGHT",
                "main_color_name": "RED",
            },
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "permalink": "54321",
        "schedule": None,
        "services": Any(dict),
        "rating": Any(dict),
        "promos": Any(dict),
        "blocked": False,
        "landing_type": "DEFAULT",
        "instagram": None,
        "chain_id": 1234,
        "branches": [
            {
                "contacts": {
                    "geo": {
                        "address": "Город, Улица, 2",
                        "address_is_accurate": True,
                        "lat": "22.22",
                        "locality": "Город",
                        "lon": "22.22",
                        "permalink": "6789",
                        "country_code": "RU",
                        "postal_code": "1234567",
                        "address_region": "Область",
                        "street_address": "Улица, 2",
                    },
                    "phone": "+7 (495) 222-20-00",
                    "vkontakte": "http://vk.com/cafe2",
                    "website": "http://cafe2.ru",
                },
                "cover": "https://images.com/cover2",
                "description": "Кафе 2",
                "logo": "https://images.com/logo2",
                "name": "Кафе 2",
                "permalink": "6789",
                "slug": "slug1",
                "preferences": {
                    "personal_metrika_code": "metrika_code",
                    "external_metrika_code": "counter_number_1",
                    "color_theme": {
                        "theme": "LIGHT",
                        "main_color_hex": "FB524F",
                        "text_color_over_main": "LIGHT",
                        "main_color_name": "RED",
                    },
                    "cta_button": {
                        "predefined": "BOOK_TABLE",
                        "value": "https://maps.yandex.ru",
                    },
                },
            },
            {
                "contacts": {
                    "geo": {
                        "address": "Город, Улица, 3",
                        "address_is_accurate": True,
                        "lat": "33.33",
                        "locality": "Город",
                        "lon": "33.33",
                        "permalink": "7890",
                        "country_code": "RU",
                        "postal_code": "1234567",
                        "address_region": "Область",
                        "street_address": "Улица, 3",
                    },
                    "phone": "+7 (495) 333-30-00",
                    "vkontakte": "http://vk.com/cafe3",
                    "website": "http://cafe3.ru",
                },
                "cover": "https://images.com/cover3",
                "description": "Кафе 3",
                "logo": "https://images.com/logo3",
                "name": "Кафе 3",
                "permalink": "7890",
                "slug": "slug2",
                "preferences": {
                    "personal_metrika_code": "metrika_code",
                    "external_metrika_code": "counter_number_1",
                    "color_theme": {
                        "theme": "LIGHT",
                        "main_color_hex": "FB524F",
                        "text_color_over_main": "LIGHT",
                        "main_color_name": "RED",
                    },
                    "cta_button": {
                        "predefined": "BOOK_TABLE",
                        "value": "https://maps.yandex.ru",
                    },
                },
            },
        ],
    }


async def test_return_data_with_loaded_geosearch_no_branches(dm, domain):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.USE_LOADED_GEOSEARCH_DATA
        else None
    )

    dm.fetch_landing_data_by_slug.coro.return_value = {
        "biz_id": 11,
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
        "schedule": None,
        "photos": None,
        "chain_id": 1234,
        "is_updated_from_geosearch": True,
    }
    dm.fetch_vk_pixels_for_permalink.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 11,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "photos": None,
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
            "is_substitution_phone": False,
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "metrika_code",
            "external_metrika_code": "counter_number_2",
            "color_theme": {
                "theme": "LIGHT",
                "main_color_hex": "FB524F",
                "text_color_over_main": "LIGHT",
                "main_color_name": "RED",
            },
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "permalink": "54321",
        "schedule": None,
        "services": Any(dict),
        "rating": Any(dict),
        "promos": Any(dict),
        "blocked": False,
        "landing_type": "DEFAULT",
        "instagram": None,
        "chain_id": 1234,
    }


async def test_return_data_no_branches(dm, domain):
    dm.fetch_landing_data_by_slug.coro.return_value = {
        "biz_id": 10,
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
        "schedule": None,
        "photos": None,
        "chain_id": 1234,
        "is_updated_from_geosearch": False,
    }

    dm.fetch_vk_pixels_for_permalink.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 10,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "photos": ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
        "contacts": {
            "geo": {
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "lat": Decimal("11.22"),
                "locality": "Город",
                "lon": Decimal("22.33"),
                "permalink": "54321",
                "country_code": "RU",
                "postal_code": "1234567",
                "address_region": "Область",
                "street_address": "Улица, 1",
            },
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
            "facebook": "http://facebook.com/cafe",
            "instagram": "http://instagram.com/cafe",
            "twitter": "http://twitter.com/cafe",
            "telegram": "https://t.me/cafe",
            "viber": "https://viber.click/cafe",
            "whatsapp": "https://wa.me/cafe",
            "is_substitution_phone": False,
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "metrika_code",
            "external_metrika_code": "counter_number_1",
            "color_theme": {
                "theme": "LIGHT",
                "main_color_hex": "FB524F",
                "text_color_over_main": "LIGHT",
                "main_color_name": "RED",
            },
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "permalink": "54321",
        "schedule": {
            "schedule": [
                {"closes_at": 64800, "day": "MONDAY", "opens_at": 32400},
                {"closes_at": 64800, "day": "TUESDAY", "opens_at": 32400},
            ],
            "tz_offset": datetime.timedelta(seconds=10800),
            "work_now_text": "Сейчас оно работает",
        },
        "services": Any(dict),
        "rating": Any(dict),
        "promos": Any(dict),
        "blocked": False,
        "landing_type": "DEFAULT",
        "instagram": None,
        "chain_id": 1234,
    }
