import pytest


@pytest.fixture
def landing_data():
    return {
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {
                "permalink": "54321",
                "lat": "11.22",
                "lon": "22.33",
                "address": "Город, Улица, 1",
            },
            "phone": "+7 (495) 739-70-00",
            "phones": ["+7 (495) 739-70-00"],
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
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
            "social_buttons": [
                {
                    "type": "VK",
                    "url": "https://url1.com",
                    "custom_text": "some",
                },
            ],
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
    }


@pytest.fixture
def loaded_landing_data():
    return {
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {
                "permalink": "54321",
                "lat": "11.22",
                "lon": "22.33",
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "locality": "Город",
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
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "metrika_code",
            "external_metrika_code": "counter_number_1",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
            "social_buttons": [
                {
                    "type": "VK",
                    "url": "https://url1.com",
                    "custom_text": "some",
                },
            ],
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
        "photos": ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
    }
