from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import Feature, LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "preset, expected",
    (
        [
            "YELLOW",
            dict(
                main_color_hex="FFD353",
                text_color_over_main="DARK",
                main_color_name="YELLOW",
            ),
        ],
        [
            "GREEN",
            dict(
                main_color_hex="00E087",
                text_color_over_main="DARK",
                main_color_name="GREEN",
            ),
        ],
        [
            "VIOLET",
            dict(
                main_color_hex="6951FF",
                text_color_over_main="LIGHT",
                main_color_name="VIOLET",
            ),
        ],
        [
            "RED",
            dict(
                main_color_hex="FB524F",
                text_color_over_main="LIGHT",
                main_color_name="RED",
            ),
        ],
        [
            "BLUE",
            dict(
                main_color_hex="3083FF",
                text_color_over_main="LIGHT",
                main_color_name="BLUE",
            ),
        ],
    ),
)
async def test_returns_valid_colors(preset, expected, dm, domain):
    dm.fetch_landing_data_by_slug.coro.return_value["preferences"]["color_theme"]["preset"] = preset

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["color_theme"] == dict(theme="LIGHT", **expected)


async def test_returns_default_preset_for_unknown(dm, domain):
    dm.fetch_landing_data_by_slug.coro.return_value["preferences"]["color_theme"]["preset"] = "GOVENIY"

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["color_theme"] == dict(
        theme="LIGHT",
        main_color_hex="FFD353",
        text_color_over_main="DARK",
        main_color_name="YELLOW",
    )


async def test_logs_about_unknown_preset(dm, domain, caplog):
    dm.fetch_landing_data_by_slug.coro.return_value["preferences"]["color_theme"]["preset"] = "GOVENIY"

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "Unknown color preset GOVENIY, using default" in caplog.messages


@pytest.mark.parametrize("link", ["http://any-link.ru/", "http://cafe.ru"])
async def test_returns_promoted_cta_button_if_exists(link, domain, dm):
    dm.fetch_promoted_cta.coro.return_value = {
        "custom": "Перейти на сайт",
        "value": link,
    }
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["cta_button"] == {
        "custom": "Перейти на сайт",
        "value": link,
    }


@pytest.mark.parametrize(
    "link",
    [
        "https://cafe.clients.site",
        "http://cafe.clients.site",
        "https://cafe.tst-clients.site",
        "https://cafe.clients.site/#about",
        "https://cafe.clients.site?a=b",
    ],
)
async def test_returns_saved_cta_button_if_promoted_is_landing(link, domain, dm):
    dm.fetch_promoted_cta.coro.return_value = {
        "custom": "Перейти на сайт",
        "value": link,
    }
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["cta_button"] == {
        "predefined": "BOOK_TABLE",
        "value": "https://maps.yandex.ru",
    }


async def test_returns_saved_cta_button_if_promoted_does_not_exists(domain, dm):
    dm.fetch_promoted_cta.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["cta_button"] == {
        "predefined": "BOOK_TABLE",
        "value": "https://maps.yandex.ru",
    }


async def test_returns_external_metrika_code_from_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value.metrika_counter = "my_counter_22"

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["external_metrika_code"] == "my_counter_22"


async def test_does_not_return_external_metrika_code_if_notfound_in_geosearch_response(domain, geosearch):
    geosearch.resolve_org.coro.return_value.metrika_counter = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "external_metrika_code" not in result["preferences"]


async def test_returns_sub_phone_as_cta_value_if_cta_is_call_and_has_sub_phone(domain, dm):
    dm.fetch_landing_data_by_slug.coro.return_value = {
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {
                "permalink": "54321",
                "lat": Decimal("11.22"),
                "lon": Decimal("22.33"),
                "address": "Город, Улица, 1",
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
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "CALL",
                "value": "+7 (876) 124-23-97",
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
    }
    dm.fetch_substitution_phone.coro.return_value = "+7 (800) 200-06-00"

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["cta_button"] == {
        "predefined": "CALL",
        "value": "+7 (800) 200-06-00",
    }


async def test_returns_saved_phone_as_cta_value_if_cta_is_call_and_no_sub_phone(domain, dm):
    dm.fetch_landing_data_by_slug.coro.return_value = {
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {
            "geo": {
                "permalink": "54321",
                "lat": Decimal("11.22"),
                "lon": Decimal("22.33"),
                "address": "Город, Улица, 1",
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
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "CALL",
                "value": "+7 (876) 124-23-97",
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
    }
    dm.fetch_substitution_phone.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["cta_button"] == {
        "predefined": "CALL",
        "value": "+7 (876) 124-23-97",
    }


async def test_returns_google_counters_from_yt(domain, async_yt_client):
    async_yt_client.get_google_counters_for_permalink.coro.return_value = [
        {
            "id": "GoogleId1",
            "goals": {"click": "GoogleId111", "route": "GoogleId112"},
        }
    ]
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["google_counters"] == [
        {
            "id": "GoogleId1",
            "goals": {"click": "GoogleId111", "route": "GoogleId112"},
        }
    ]


async def test_returns_google_counters_from_db(domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.USE_LOADED_GOOGLE_COUNTERS else None
    )
    dm.fetch_google_counters_for_permalink.coro.return_value = [
        {
            "id": "GoogleId1",
            "goals": {"click": "GoogleId111", "route": "GoogleId112"},
        }
    ]

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["google_counters"] == [
        {
            "id": "GoogleId1",
            "goals": {"click": "GoogleId111", "route": "GoogleId112"},
        }
    ]


async def test_does_not_return_google_counters_for_for_unstable(domain, dm):
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.UNSTABLE
    )

    assert result["preferences"].get("google_counters") is None


@pytest.mark.parametrize(
    ("cart_enabled"),
    [(True), (False), (None)],
)
async def test_returns_cart_enabled(domain, dm, cart_enabled):
    if cart_enabled is not None:
        dm.fetch_landing_data_by_slug.coro.return_value["preferences"]["cart_enabled"] = cart_enabled

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"].get("cart_enabled") == cart_enabled


async def test_returns_tiktok_pixels(domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.USE_LOADED_TIKTOK_PIXELS else None
    )
    dm.fetch_tiktok_pixels_for_permalink.coro.return_value = [
        {
            "id": "PixelId1",
            "goals": {"click": "PixelId111", "route": "PixelId112"},
        }
    ]

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["tiktok_pixels"] == [
        {
            "id": "PixelId1",
            "goals": {"click": "PixelId111", "route": "PixelId112"},
        }
    ]


async def test_does_not_return_tiktok_pixels_for_for_unstable(domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.USE_LOADED_TIKTOK_PIXELS else None
    )
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.UNSTABLE
    )

    assert result["preferences"].get("tiktok_pixels") is None


async def test_does_not_return_tiktok_pixels_if_feature_disabled(domain, dm):
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"].get("tiktok_pixels") is None


async def test_returns_vk_pixels(domain, dm):

    dm.fetch_vk_pixels_for_permalink.coro.return_value = [
        {
            "id": "PixelId1",
            "goals": {"click": "PixelId111", "route": "PixelId112"},
        }
    ]

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["preferences"]["vk_pixels"] == [
        {
            "id": "PixelId1",
            "goals": {"click": "PixelId111", "route": "PixelId112"},
        }
    ]


async def test_does_not_return_vk_pixels_for_for_unstable(domain, dm):
    dm.fetch_vk_pixels_for_permalink.coro.return_value = [
        {
            "id": "PixelId1",
            "goals": {"click": "PixelId111", "route": "PixelId112"},
        }
    ]

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.UNSTABLE
    )

    assert result["preferences"].get("vk_pixels") is None
