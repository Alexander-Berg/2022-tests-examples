import datetime
from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("version_saved", "version_param"),
    [
        ("stable_version", LandingVersion.STABLE),
        ("unstable_version", LandingVersion.UNSTABLE),
    ],
)
async def test_returns_data(factory, dm, version_saved, version_param):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
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
            "geo": {
                "permalink": "54321",
                "lat": "11.22",
                "lon": "22.33",
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        blocks_options={
            "show_cover": True,
            "show_logo": False,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
        schedule={
            "schedule": [{"day": "EVERYDAY", "opens_at": 0, "closes_at": 86400}],
            "tz_offset": "3:00:00",
            "work_now_text": "Круглосуточно",
        },
        photos=None,
        photo_settings=None,
        chain_id=None,
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", **{version_saved: data_id}
    )

    result = await dm.fetch_landing_data_by_slug(slug="cafe", version=version_param)

    assert result == {
        "biz_id": 22,
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
                "address_is_accurate": True,
                "locality": "Город",
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
                {"custom_text": "VK", "type": "VK", "url": "https://vk.com"}
            ]
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
        "permalink": "54321",
        "landing_type": "DEFAULT",
        "instagram": None,
        "chain_id": None,
        "schedule": {
            "schedule": [{"closes_at": 86400, "day": "EVERYDAY", "opens_at": 0}],
            "tz_offset": datetime.timedelta(seconds=10800),
            "work_now_text": "Круглосуточно",
        },
        "photos": [],
        "photo_settings": None,
        "is_updated_from_geosearch": False,
    }


async def test_returns_minimal_data(factory, dm):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        contacts={},
        extras={},
        preferences={"color_theme": {"theme": "LIGHT", "preset": "RED"}},
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", stable_version=data_id
    )

    result = await dm.fetch_landing_data_by_slug(
        slug="cafe", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 22,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {"geo": {"permalink": "54321"}},
        "extras": {},
        "preferences": {"color_theme": {"theme": "LIGHT", "preset": "RED"}},
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
        "chain_id": None,
        "schedule": None,
        "photos": [],
        "photo_settings": None,
        "is_updated_from_geosearch": False,
    }


async def test_returns_stable_data_if_requested(factory, dm):
    stable_data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        contacts={},
        extras={},
        preferences={"color_theme": {"theme": "LIGHT", "preset": "RED"}},
    )
    unstable_data_id = await factory.insert_landing_data(
        name="Кафе там",
        categories=["Забегаловка", "Шаурма"],
        contacts={},
        extras={},
        preferences={"color_theme": {"theme": "DARK", "preset": "GREEN"}},
    )
    await factory.insert_biz_state(
        biz_id=22,
        slug="cafe",
        permalink="54321",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )

    result = await dm.fetch_landing_data_by_slug(
        slug="cafe", version=LandingVersion.STABLE
    )

    assert result == {
        "biz_id": 22,
        "name": "Кафе здесь",
        "categories": ["Кафе", "Ресторан"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {"geo": {"permalink": "54321"}},
        "extras": {},
        "preferences": {"color_theme": {"theme": "LIGHT", "preset": "RED"}},
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
        "chain_id": None,
        "schedule": None,
        "photos": [],
        "photo_settings": None,
        "is_updated_from_geosearch": False,
    }


async def test_returns_unstable_data_if_requested(factory, dm):
    stable_data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        contacts={},
        extras={},
        preferences={"color_theme": {"theme": "LIGHT", "preset": "RED"}},
    )
    unstable_data_id = await factory.insert_landing_data(
        name="Кафе там",
        categories=["Забегаловка", "Шаурма"],
        contacts={},
        extras={},
        preferences={"color_theme": {"theme": "DARK", "preset": "GREEN"}},
    )
    await factory.insert_biz_state(
        biz_id=22,
        slug="cafe",
        permalink="54321",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )

    result = await dm.fetch_landing_data_by_slug(
        slug="cafe", version=LandingVersion.UNSTABLE
    )

    assert result == {
        "biz_id": 22,
        "name": "Кафе там",
        "categories": ["Забегаловка", "Шаурма"],
        "description": "Описание",
        "logo": "https://images.com/logo",
        "cover": "https://images.com/cover",
        "contacts": {"geo": {"permalink": "54321"}},
        "extras": {},
        "preferences": {"color_theme": {"theme": "DARK", "preset": "GREEN"}},
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
        "chain_id": None,
        "schedule": None,
        "photos": [],
        "photo_settings": None,
        "is_updated_from_geosearch": False,
    }


@pytest.mark.parametrize(
    ("version_saved", "version_param"),
    [
        ("stable_version", LandingVersion.UNSTABLE),
        ("unstable_version", LandingVersion.STABLE),
    ],
)
async def test_does_not_return_wrong_kind_of_data(
    factory, dm, version_saved, version_param
):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
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
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        schedule=None,
        photos=None,
        chain_id=None,
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", **{version_saved: data_id}
    )

    result = await dm.fetch_landing_data_by_slug(slug="cafe", version=version_param)

    assert result is None


@pytest.mark.parametrize("version_saved", ["stable_version", "unstable_version"])
@pytest.mark.parametrize(
    "version_param", [LandingVersion.STABLE, LandingVersion.UNSTABLE]
)
async def test_returns_none_if_no_data_exists_for_slug(
    factory, dm, version_saved, version_param
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="restoran", permalink="54321", **{version_saved: data_id}
    )

    result = await dm.fetch_landing_data_by_slug(slug="cafe", version=version_param)

    assert result is None


@pytest.mark.parametrize(
    ("hidden_ids", "db_photos", "out_photos"),
    [
        ([], ["url1", "url2"], ["url1", "url2"]),
        (["2"], [{"id": "1", "url": "url1"}, {"id": "2", "url": "url2"}], ["url1"]),
    ],
)
async def test_fetch_photos(factory, dm, hidden_ids, db_photos, out_photos):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        photos=db_photos,
        photo_settings={"hidden_ids": hidden_ids},
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", stable_version=data_id
    )

    result = await dm.fetch_landing_data_by_slug(
        slug="cafe", version=LandingVersion.STABLE
    )

    assert result["photos"] == out_photos
