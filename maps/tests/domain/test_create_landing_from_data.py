from decimal import Decimal
from unittest import mock

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import dt
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import NoBizIdForOrg

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("logging_warning"),
    pytest.mark.freeze_time(dt("2022-01-07 06:03:00")),
]


@pytest.fixture(autouse=True)
async def common_mocks(dm, bvm):
    dm.fetch_biz_state.coro.return_value = None
    dm.fetch_biz_state_by_slug.coro.return_value = None
    dm.create_biz_state.cor.return_value = None
    dm.save_landing_data_for_biz_id.coro.return_value = {"some": "data"}


async def test_check_used_clients(domain, bvm, geosearch):
    await domain.create_landing_from_data(
        permalink=54321,
        name="name",
        categories=["Category"],
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
    )

    bvm.fetch_biz_id_by_permalink.assert_called_with(permalink=54321)
    geosearch.resolve_org.assert_not_called()


async def test_raises_if_no_biz_id_found_for_permalink(domain, bvm):
    bvm.fetch_biz_id_by_permalink.coro.return_value = None

    with pytest.raises(NoBizIdForOrg):
        await domain.create_landing_from_data(
            permalink=54321,
            name="name",
            categories=["Category"],
            contacts={
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                "instagram": None,
                "facebook": None,
                "vkontakte": "http://vk.com/cafe",
                "twitter": None,
                "telegram": None,
                "viber": None,
                "whatsapp": None,
            },
        )


async def test_uses_dm_to_create_landing_data(domain, dm):
    await domain.create_landing_from_data(
        permalink=54321,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
    )

    dm.fetch_biz_state.assert_any_call(biz_id=15)
    dm.create_biz_state.assert_called_with(
        biz_id=15, permalink="54321", slug="kafe-s-edoj"
    )

    dm.save_landing_data_for_biz_id.assert_has_calls(
        [
            mock.call(
                biz_id=15,
                landing_data={
                    "name": "Кафе с едой",
                    "categories": ["Общепит", "Ресторан"],
                    "contacts": {
                        "phone": "+7 (495) 739-70-00",
                        "website": "http://cafe.ru",
                        "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                        "instagram": None,
                        "facebook": None,
                        "vkontakte": "http://vk.com/cafe",
                        "twitter": None,
                        "telegram": None,
                        "viber": None,
                        "whatsapp": None,
                    },
                    "extras": Any(dict),
                    "preferences": {
                        "color_theme": {"theme": "LIGHT", "preset": "YELLOW"},
                        "cta_button": {
                            "predefined": "CALL",
                            "value": "+7 (495) 739-70-00",
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
                },
                version=LandingVersion.STABLE,
                updated_from_geosearch=True,
            ),
            mock.call(
                biz_id=15,
                landing_data={
                    "name": "Кафе с едой",
                    "categories": ["Общепит", "Ресторан"],
                    "contacts": {
                        "phone": "+7 (495) 739-70-00",
                        "website": "http://cafe.ru",
                        "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                        "instagram": None,
                        "facebook": None,
                        "vkontakte": "http://vk.com/cafe",
                        "twitter": None,
                        "telegram": None,
                        "viber": None,
                        "whatsapp": None,
                    },
                    "extras": Any(dict),
                    "preferences": {
                        "color_theme": {"theme": "LIGHT", "preset": "YELLOW"},
                        "cta_button": {
                            "predefined": "CALL",
                            "value": "+7 (495) 739-70-00",
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
                },
                version=LandingVersion.UNSTABLE,
                updated_from_geosearch=True,
            ),
        ],
        any_order=True,
    )


async def test_updates_biz_state_head_permalink_if_has_new(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "kafe-s-edoj",
        "permalink": "35687",
        "stable_version": None,
        "unstable_version": None,
    }
    await domain.create_landing_from_data(
        permalink=54321,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
    )

    dm.create_biz_state.assert_not_called()
    dm.update_biz_state_permalink.assert_called_with(biz_id=15, permalink="54321")


async def test_sets_landing_published(domain, dm):
    await domain.create_landing_from_data(
        permalink=54321,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
    )

    dm.set_landing_publicity.assert_called_with(biz_id=15, is_published=True)


async def test_returns_created_slug(domain):
    assert (
        await domain.create_landing_from_data(
            permalink=54321,
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            contacts={
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                "instagram": None,
                "facebook": None,
                "vkontakte": "http://vk.com/cafe",
                "twitter": None,
                "telegram": None,
                "viber": None,
                "whatsapp": None,
            },
        )
        == "kafe-s-edoj"
    )


async def test_return_slug_if_biz_state_exists(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "permalink": "54321",
        "stable_version": 88,
        "unstable_version": 991,
        "published": False,
    }

    await domain.create_landing_from_data(
        permalink=54321,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
            "instagram": None,
            "facebook": None,
            "vkontakte": "http://vk.com/cafe",
            "twitter": None,
            "telegram": None,
            "viber": None,
            "whatsapp": None,
        },
    )

    dm.save_landing_data_for_biz_id.assert_not_called()


async def test_uses_landing_data_to_slugify(domain, dm):
    dm.fetch_biz_state_by_slug.coro.side_effect = [
        {"stable_version": 1, "unstable_version": 2, "slug": "slug"},
        None,
    ]

    assert (
        await domain.create_landing_from_data(
            permalink=54321,
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            contacts={
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                "instagram": None,
                "facebook": None,
                "vkontakte": "http://vk.com/cafe",
                "twitter": None,
                "telegram": None,
                "viber": None,
                "whatsapp": None,
                "geo": {
                    "permalink": "54321",
                    "lon": Decimal("11.22"),
                    "lat": Decimal("22.33"),
                    "address": "Город, Улица, 1",
                    "street_address": "Улица, 1",
                },
            },
        )
    ) == "kafe-s-edoj-ulitsa-1"


async def test_slugifies_without_landing_data(domain, dm):
    dm.fetch_biz_state_by_slug.coro.side_effect = [
        {"stable_version": 1, "unstable_version": 2, "slug": "slug"},
        None,
    ]
    assert (
        await domain.create_landing_from_data(
            permalink=54321,
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            contacts={
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                "instagram": None,
                "facebook": None,
                "vkontakte": "http://vk.com/cafe",
                "twitter": None,
                "telegram": None,
                "viber": None,
                "whatsapp": None,
            },
        )
    ) == "kafe-s-edoj-1641535380"  # 1641535380 = 2022-01-07 06:03:00
