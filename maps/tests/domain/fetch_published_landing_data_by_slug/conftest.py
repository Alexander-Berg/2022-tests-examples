import pytest


@pytest.fixture(autouse=True)
def common_mocks(dm):
    dm.fetch_substitution_phone.coro.return_value = None
    dm.fetch_org_promoted_services.coro.return_value = []
    dm.fetch_promoted_cta.coro.return_value = None
    dm.fetch_org_promos.coro.return_value = {"promotion": []}
    dm.fetch_biz_state_by_slug.coro.return_value = {
        "biz_id": 15,
        "permalink": "54321",
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": None,
        "published": True,
        "blocked": False,
        "blocking_data": {},
    }
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "permalink": "54321",
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": None,
        "published": True,
        "blocked": False,
        "blocking_data": {},
    }
    dm.fetch_landing_data_by_slug.coro.return_value = {
        "biz_id": 15,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {"permalink": "54321"},
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
        "chain_id": None,
    }
