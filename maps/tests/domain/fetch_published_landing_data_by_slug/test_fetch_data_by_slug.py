from decimal import Decimal

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.landlord.server.lib.enums import Feature, LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import (
    InvalidFetchToken,
    NoDataForSlug,
    NoOrginfo,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_uses_dm(domain, dm, version):
    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    dm.fetch_biz_state_by_slug.assert_called_with(slug="cafe")
    dm.fetch_landing_data_by_slug.assert_called_with(slug="cafe", version=version)
    dm.fetch_substitution_phone.assert_called_with(biz_id=15)
    dm.fetch_org_promos.assert_called_with(biz_id=15)
    dm.fetch_promoted_cta.assert_called_with(biz_id=15)
    dm.fetch_promoted_cta.assert_called_with(biz_id=15)


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_uses_geosearch_client(domain, geosearch, version):
    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_does_not_use_geosearch_client(dm, domain, geosearch):
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
        "chain_id": None,
        "is_updated_from_geosearch": True,
    }

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    geosearch.resolve_org.assert_not_called()


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_return_data(domain, version, dm):
    dm.fetch_vk_pixels_for_permalink.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    assert result == {
        "biz_id": 15,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "photos": ["https://images.ru/tpl1/%s", "https://images.ru/tpl2/%s"],
        "contacts": {
            "geo": {
                "permalink": "54321",
                "lat": Decimal("11.22"),
                "lon": Decimal("22.33"),
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "locality": "Город",
                "country_code": "RU",
                "postal_code": "1234567",
                "address_region": "Область",
                "street_address": "Улица, 1",
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
        "schedule": Any(dict),
        "services": Any(dict),
        "rating": Any(dict),
        "promos": Any(dict),
        "blocked": False,
        "landing_type": "DEFAULT",
        "instagram": None,
        "chain_id": None,
    }


async def test_return_data_with_loaded_geosearch(dm, domain):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.USE_LOADED_GEOSEARCH_DATA
        else None
    )

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
        "chain_id": None,
        "is_updated_from_geosearch": True,
    }

    dm.fetch_vk_pixels_for_permalink.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 15,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "photos": None,
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
        "chain_id": None,
    }


async def test_raises_if_dm_returns_none(domain, dm):
    dm.fetch_landing_data_by_slug.coro.return_value = None

    with pytest.raises(NoDataForSlug):
        await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
        )


async def test_raises_if_no_org_info_in_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrginfo):
        await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
        )


async def test_raises_if_version_is_stable_and_biz_is_not_published(domain, dm):
    dm.fetch_biz_state_by_slug.coro.return_value["published"] = False

    with pytest.raises(NoDataForSlug):
        await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
        )


async def test_returns_data_version_is_stable_and_biz_is_not_published(domain, dm):
    dm.fetch_biz_state_by_slug.coro.return_value["published"] = False
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.RETURN_200_FOR_UNPUBLISHED
        else None
    )

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result == {
        "name": "Кафе здесь",
        "contacts": {},
        "preferences": {
            "color_theme": {
                "theme": "LIGHT",
                "text_color_over_main": "LIGHT",
                "main_color_hex": "FFFFFF",
                "main_color_name": "WHITE",
            },
        },
        "permalink": "54321",
        "published": False,
    }


async def test_does_not_raise_if_version_is_unstable_and_biz_is_not_published(
    domain, dm
):
    dm.fetch_biz_state.coro.return_value["published"] = False

    try:
        await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="fetch_data_token", version=LandingVersion.UNSTABLE
        )
    except NoDataForSlug:
        pytest.fail("Should not raise NoDataForSlug")


async def test_raises_if_token_is_invalid(domain):
    with pytest.raises(InvalidFetchToken):
        await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="BAD_TOKEN", version=LandingVersion.STABLE
        )


async def test_returns_changed_permalink(domain, geosearch_moved_perm):
    got = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )
    assert got["permalink"] == "11111"


async def test_updates_permalink_in_db_if_it_changed(domain, dm, geosearch_moved_perm):
    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    dm.update_biz_state_permalink.assert_called_with(biz_id=15, permalink="11111")


@pytest.mark.parametrize("permalink_moved_to", [None, "54321"])
async def test_does_not_update_permalink_in_db_if_it_not_changed(
    domain, dm, geosearch, geosearch_resp, permalink_moved_to
):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "permalink": "54321",
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": None,
        "published": True,
    }
    geosearch_resp["permalink_moved_to"] = permalink_moved_to
    geosearch.resolve_org.return_value = geosearch_resp

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    dm.update_biz_state_permalink.assert_not_called()


async def test_apply_photo_filter_for_geoserch_photos(
    domain, dm, geosearch, geosearch_resp
):
    dm.fetch_landing_data_by_slug.coro.return_value = {
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
        "photo_settings": {"hidden_ids": ["2"]},
        "chain_id": None,
        "is_updated_from_geosearch": False,
    }
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "permalink": "54321",
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": None,
        "published": True,
    }
    geosearch.resolve_org.return_value = geosearch_resp

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.UNSTABLE
    )

    assert result["photos"] == ["https://images.ru/tpl1/%s"]
