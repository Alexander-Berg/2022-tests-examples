import json
from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mapkit]

APP_FILTER = {
    "ios_navi_build": 201,
    "ios_maps_build": 201,
    "ios_metro_build": 201,
    "android_navi_build": 201,
    "android_maps_build": 201,
    "android_metro_build": 201,
}


async def test_normalizes_events_name(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "event_name": "billboard.show"},
            {"receive_timestamp": 102, "event_name": "billboard.click"},
            {"receive_timestamp": 103, "event_name": "billboard.navigation.via"},
            {"receive_timestamp": 104, "event_name": "billboard.action.call"},
            {"receive_timestamp": 105, "event_name": "billboard.action.open_site"},
            {"receive_timestamp": 106, "event_name": "billboard.action.search"},
            {"receive_timestamp": 107, "event_name": "billboard.action.save_offer"},
            {"receive_timestamp": 108, "event_name": "billboard.action.open_app"},
            {"receive_timestamp": 109, "event_name": "billboard.action.Call"},
            {"receive_timestamp": 110, "event_name": "billboard.action.OpenSite"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_event_names = factory.get_all_normalized(1)
    assert existing_event_names == [
        "BILLBOARD_SHOW",
        "BILLBOARD_TAP",
        "ACTION_MAKE_ROUTE",
        "ACTION_CALL",
        "ACTION_OPEN_SITE",
        "ACTION_SEARCH",
        "ACTION_SAVE_OFFER",
        "ACTION_OPEN_APP",
        "ACTION_CALL",
        "ACTION_OPEN_SITE",
    ]


async def test_normalizes_campaign_id(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "campaign_id": 1001},
            {"receive_timestamp": 102, "campaign_id": 1002},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_campaign_ids = factory.get_all_normalized(2)
    assert existing_campaign_ids == [1001, 1002]


async def test_normalizes_event_group_id(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "event_group_id": "EvGr1"},
            {"receive_timestamp": 102, "event_group_id": "EvGr1"},
            {"receive_timestamp": 103, "event_group_id": "EvGr2"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_event_group_ids = factory.get_all_normalized(3)
    assert existing_event_group_ids == ["EvGr1", "EvGr1", "EvGr2"]


async def test_normalizes_device_id(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "device_id": "device1"},
            {"receive_timestamp": 102, "device_id": "device1"},
            {"receive_timestamp": 103, "device_id": "device2"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_device_ids = factory.get_all_normalized(4)
    assert existing_device_ids == ["device1", "device1", "device2"]


async def test_normalizes_app_name(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "app_package": "ru.yandex.yandexnavi"},
            {"receive_timestamp": 102, "app_package": "ru.yandex.yandexmaps"},
            {"receive_timestamp": 103, "app_package": "ru.yandex.mobile.navigator"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_applications = factory.get_all_normalized(5)
    assert existing_applications == ["NAVIGATOR", "MOBILE_MAPS", "NAVIGATOR"]


async def test_normalizes_app_platform(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "app_platform": "iphoneos"},
            {"receive_timestamp": 102, "app_platform": "android"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_platforms = factory.get_all_normalized(6)
    assert existing_platforms == ["IOS", "ANDROID"]


async def test_normalizes_app_version_name(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "app_version": "1.2.3"},
            {"receive_timestamp": 102, "app_version": "1.2.3"},
            {"receive_timestamp": 103, "app_version": "2.3.5"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_app_version_names = factory.get_all_normalized(7)
    assert existing_app_version_names == ["1.2.3", "1.2.3", "2.3.5"]


async def test_normalizes_app_build_number(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "app_build": 223},
            {"receive_timestamp": 102, "app_build": 223},
            {"receive_timestamp": 103, "app_build": 235},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_app_build_numbers = factory.get_all_normalized(8)
    assert existing_app_build_numbers == [223, 223, 235]


async def test_normalizes_latitude(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "user_lat": 57.22},
            {"receive_timestamp": 102, "user_lat": 57.22},
            {"receive_timestamp": 103, "user_lat": 35.33},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_user_latitudes = factory.get_all_normalized(9)
    assert existing_user_latitudes == [
        Decimal("57.22"),
        Decimal("57.22"),
        Decimal("35.33"),
    ]


async def test_normalizes_longitude(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "user_lon": 57.22},
            {"receive_timestamp": 102, "user_lon": 57.22},
            {"receive_timestamp": 103, "user_lon": 35.33},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_user_longitudes = factory.get_all_normalized(10)
    assert existing_user_longitudes == [
        Decimal("57.22"),
        Decimal("57.22"),
        Decimal("35.33"),
    ]


async def test_normalizes_place_id(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101, "place_id": ""},
            {"receive_timestamp": 102, "place_id": "123"},
            {"receive_timestamp": 103, "place_id": "kek:oops"},
        ]
    )

    await task_factory(app_filter=APP_FILTER)(warden_client=warden_client_mock)

    existing_place_ids = factory.get_all_normalized(11)
    assert existing_place_ids == [None, "123", "kek:oops"]


async def test_inserts_metadata(factory, task_factory, warden_client_mock):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 101},
            {"receive_timestamp": 102},
            {"receive_timestamp": 103},
        ]
    )
    expected_app_filter = {
        "android_maps_build": 1,
        "android_metro_build": 2,
        "android_navi_build": 3,
        "ios_maps_build": 4,
        "ios_metro_build": 5,
        "ios_navi_build": 6,
    }

    await task_factory(
        app_filter={
            "android_maps_build": 1,
            "android_metro_build": 2,
            "android_navi_build": 3,
            "ios_maps_build": 4,
            "ios_metro_build": 5,
            "ios_navi_build": 6,
        }
    )(warden_client=warden_client_mock)

    existing_data_in_db = factory.get_all_normalized(12)
    existing_metadata = list(map(json.loads, existing_data_in_db))
    assert existing_metadata == [
        {
            "source": "mapkittube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
        {
            "source": "mapkittube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
        {
            "source": "mapkittube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
    ]
