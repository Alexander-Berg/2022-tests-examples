import datetime
from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio]


async def test_fetches_branches(factory, dm):
    ld1 = await factory.insert_landing_data(
        name="Кафе 1",
        description="Кафе 1",
        logo="https://images.com/logo1",
        cover="https://images.com/cover1",
        contacts={
            "phone": "+7 (495) 111-10-00",
            "website": "http://cafe1.ru",
            "vkontakte": "http://vk.com/cafe1",
            "geo": {
                "permalink": "54321",
                "lat": "11.11",
                "lon": "11.11",
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        chain_id=1234,
        is_updated_from_geosearch=True,
    )
    await factory.insert_biz_state(
        biz_id=15, slug="slug", stable_version=ld1, published=True
    )

    ld2 = await factory.insert_landing_data(
        name="Кафе 2",
        description="Кафе 2",
        logo="https://images.com/logo2",
        cover="https://images.com/cover2",
        contacts={
            "phone": "+7 (495) 222-20-00",
            "website": "http://cafe2.ru",
            "vkontakte": "http://vk.com/cafe2",
            "geo": {
                "permalink": "6789",
                "lat": "22.22",
                "lon": "22.22",
                "address": "Город, Улица, 2",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        chain_id=1234,
        schedule={
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
        is_updated_from_geosearch=True,
    )
    await factory.insert_biz_state(
        biz_id=16, slug="slug1", permalink="6789", stable_version=ld2, published=True
    )

    ld3 = await factory.insert_landing_data(
        name="Кафе 3",
        description="Кафе 3",
        logo="https://images.com/logo3",
        cover="https://images.com/cover3",
        contacts={
            "phone": "+7 (495) 333-30-00",
            "website": "http://cafe3.ru",
            "vkontakte": "http://vk.com/cafe3",
            "geo": {
                "permalink": "7890",
                "lat": "33.33",
                "lon": "33.33",
                "address": "Город, Улица, 3",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        chain_id=1234,
        schedule={
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
        is_updated_from_geosearch=True,
    )
    await factory.insert_biz_state(
        biz_id=17, slug="slug2", permalink="7890", stable_version=ld3, published=True
    )

    ld4 = await factory.insert_landing_data(
        name="Кафе 4",
        description="Кафе 4",
        logo="https://images.com/logo3",
        cover="https://images.com/cover3",
        contacts={
            "phone": "+7 (495) 333-30-00",
            "website": "http://cafe3.ru",
            "vkontakte": "http://vk.com/cafe3",
            "geo": {
                "permalink": "7890",
                "lat": "33.33",
                "lon": "33.33",
                "address": "Город, Улица, 3",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        chain_id=1234,
        schedule={
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
        is_updated_from_geosearch=True,
        landing_type="INSTAGRAM"
    )
    await factory.insert_biz_state(
        biz_id=18, slug="slug_instaslug", permalink="7891", stable_version=ld4, published=True
    )

    result = await dm.fetch_branches_for_permalink("54321", LandingVersion.STABLE, 1234)
    assert result == [
        {
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 2",
                    "address_is_accurate": True,
                    "lat": Decimal("22.22"),
                    "locality": "Город",
                    "lon": Decimal("22.22"),
                    "permalink": "6789",
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
            "photos": [],
            "preferences": {
                "personal_metrika_code": "metrika_code",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "social_buttons": [{
                    "custom_text": "VK",
                    "type": "VK",
                    "url": "https://vk.com"
                }],
            },
            "schedule": {
                "tz_offset": datetime.timedelta(seconds=10800),
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
        },
        {
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 3",
                    "address_is_accurate": True,
                    "lat": Decimal("33.33"),
                    "locality": "Город",
                    "lon": Decimal("33.33"),
                    "permalink": "7890",
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
            "photos": [],
            "preferences": {
                "personal_metrika_code": "metrika_code",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "social_buttons": [{
                    "custom_text": "VK",
                    "type": "VK",
                    "url": "https://vk.com"
                }],
            },
            "schedule": {
                "tz_offset": datetime.timedelta(seconds=10800),
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
        },
    ]


async def test_fetches_limited_amount_of_branches(factory, dm):

    ld = await factory.insert_landing_data(
        name="Кафе 1",
        description="Кафе 1",
        logo="https://images.com/logo1",
        cover="https://images.com/cover1",
        contacts={
            "phone": "+7 (495) 111-10-00",
            "website": "http://cafe1.ru",
            "vkontakte": "http://vk.com/cafe1",
            "geo": {
                "permalink": "54321",
                "lat": "11.11",
                "lon": "11.11",
                "address": "Город, Улица, 1",
                "address_is_accurate": True,
                "locality": "Город",
            },
        },
        chain_id=1234,
        is_updated_from_geosearch=True,
    )
    await factory.insert_biz_state(
        biz_id=15, slug="slug", stable_version=ld, published=True
    )

    for permalink in range(1000, 1020):
        ld = await factory.insert_landing_data(
            chain_id=1234, is_updated_from_geosearch=True
        )
        await factory.insert_biz_state(
            biz_id=permalink,
            slug="slug" + str(permalink),
            permalink="5555" + str(permalink),
            stable_version=ld,
            published=True,
        )

    assert 12 == len(
        await dm.fetch_branches_for_permalink("54321", LandingVersion.STABLE, 1234)
    )
