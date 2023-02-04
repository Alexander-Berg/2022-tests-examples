import datetime
from decimal import Decimal
from unittest import mock

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import Feature

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_data(domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.LOAD_GEOSEARCH_DATA else None
    )
    dm.fetch_all_published_permalinks.coro.side_effect = [["54321", "65432"], []]

    await domain.update_geosearch_data()

    dm.update_landing_data_with_geosearch.assert_has_calls(
        [
            mock.call(
                permalink="54321",
                chain_id=None,
                contacts={
                    "geo": {
                        "lat": Decimal("11.22"),
                        "lon": Decimal("22.33"),
                        "address": "Город, Улица, 1",
                        "address_is_accurate": True,
                        "country_code": "RU",
                        "postal_code": "1234567",
                        "address_region": "Область",
                        "street_address": "Улица, 1",
                        "locality": "Город",
                        "permalink": "54321",
                    },
                    "website": "http://cafe.ru",
                    "phone": "+7 (495) 739-70-00",
                    "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                    "instagram": None,
                    "facebook": None,
                    "vkontakte": "http://vk.com/cafe",
                    "twitter": None,
                    "telegram": None,
                    "viber": None,
                    "whatsapp": None,
                    "email": "cafe@gmail.com",
                },
                schedule={
                    "tz_offset": datetime.timedelta(seconds=10800),
                    "work_now_text": "Сейчас оно работает",
                    "schedule": [
                        {"day": "MONDAY", "opens_at": 32400, "closes_at": 64800},
                        {"day": "TUESDAY", "opens_at": 32400, "closes_at": 64800},
                    ],
                },
                photos=[
                    {"id": "1", "url": "https://images.ru/tpl1/%s"},
                    {"id": "2", "url": "https://images.ru/tpl2/%s"},
                ],
                metrika_counter="counter_number_1",
            ),
            mock.call(
                permalink="65432",
                chain_id=7776,
                contacts={
                    "geo": {
                        "lat": Decimal("55.66"),
                        "lon": Decimal("66.77"),
                        "address": "Город, Проспект, 2",
                        "address_is_accurate": True,
                        "country_code": "RU",
                        "postal_code": "2345678",
                        "address_region": "Область",
                        "street_address": "Проспект, 2",
                        "locality": "Город",
                        "permalink": "65432",
                    },
                    "website": "http://haircut.ru",
                    "phone": "+7 (833) 654-20-00",
                    "phones": ["+7 (833) 654-20-00", "+7 (833) 654-20-22"],
                    "instagram": None,
                    "facebook": None,
                    "vkontakte": "http://vk.com/haircut",
                    "twitter": None,
                    "telegram": None,
                    "viber": None,
                    "whatsapp": None,
                    "email": "hair@mail.ru",
                },
                schedule={
                    "tz_offset": datetime.timedelta(seconds=10800),
                    "work_now_text": "Сейчас оно работает",
                    "schedule": [
                        {"day": "MONDAY", "opens_at": 32400, "closes_at": 64800},
                        {"day": "TUESDAY", "opens_at": 32400, "closes_at": 64800},
                    ],
                },
                photos=[
                    {"id": "1", "url": "https://images.ru/tpl3/%s"},
                    {"id": "2", "url": "https://images.ru/tpl4/%s"},
                ],
                metrika_counter="counter_number_2",
            ),
        ],
    )

    dm.update_instagram_landing_data_with_geosearch.assert_has_calls(
        [
            mock.call(
                permalink="54321",
                geo={
                    "address": "Город, Улица, 1",
                    "address_is_accurate": True,
                    "address_region": "Область",
                    "country_code": "RU",
                    "lat": Decimal("11.22"),
                    "locality": "Город",
                    "lon": Decimal("22.33"),
                    "postal_code": "1234567",
                    "street_address": "Улица, 1",
                    "permalink": "54321",
                },
                metrika_counter="counter_number_1",
            ),
            mock.call(
                permalink="65432",
                geo={
                    "lat": Decimal("55.66"),
                    "lon": Decimal("66.77"),
                    "address": "Город, Проспект, 2",
                    "address_is_accurate": True,
                    "country_code": "RU",
                    "postal_code": "2345678",
                    "address_region": "Область",
                    "street_address": "Проспект, 2",
                    "locality": "Город",
                    "permalink": "65432",
                },
                metrika_counter="counter_number_2",
            ),
        ]
    )


async def test_does_not_call_dm_if_no_data(domain, dm, geosearch):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.LOAD_GEOSEARCH_DATA else None
    )
    dm.fetch_all_published_permalinks.coro.side_effect = [["54321"], []]

    geosearch.resolve_orgs.coro.return_value = []

    await domain.update_geosearch_data()

    dm.update_landing_data_with_geosearch.assert_not_called()


async def test_calls_dm_if_permalink_changed(domain, dm, geosearch):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.LOAD_GEOSEARCH_DATA else None
    )
    dm.fetch_all_published_permalinks.coro.side_effect = [["54321"], []]
    geosearch.resolve_orgs.coro.return_value[0].permalink_moved_to = "123456"

    await domain.update_geosearch_data()

    dm.update_permalink_from_geosearch.assert_called_with(
        old_permalink="54321", new_permalink="123456"
    )
