import json
from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mapkit]


async def test_normalizes_events_name(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "EventName": "geoadv.bb.pin.show"},
            {"_timestamp": 102, "EventName": "geoadv.bb.pin.tap"},
            {"_timestamp": 103, "EventName": "geoadv.bb.action.makeRoute"},
            {"_timestamp": 104, "EventName": "geoadv.bb.action.call"},
            {"_timestamp": 105, "EventName": "geoadv.bb.action.openSite"},
            {"_timestamp": 106, "EventName": "geoadv.bb.action.search"},
            {"_timestamp": 107, "EventName": "geoadv.bb.action.saveOffer"},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_event_names = factory.get_all_normalized(1)
    assert existing_event_names == [
        "BILLBOARD_SHOW",
        "BILLBOARD_TAP",
        "ACTION_MAKE_ROUTE",
        "ACTION_CALL",
        "ACTION_OPEN_SITE",
        "ACTION_SEARCH",
        "ACTION_SAVE_OFFER",
    ]


async def test_normalizes_campaign_id(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "CampaignID": 1001},
            {"_timestamp": 102, "CampaignID": 1002},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_campaign_ids = factory.get_all_normalized(2)
    assert existing_campaign_ids == [1001, 1002]


async def test_normalizes_event_group_id(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "EventGroupId": "EvGr1"},
            {"_timestamp": 102, "EventGroupId": "EvGr1"},
            {"_timestamp": 103, "EventGroupId": "EvGr2"},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_event_group_ids = factory.get_all_normalized(3)
    assert existing_event_group_ids == ["EvGr1", "EvGr1", "EvGr2"]


async def test_normalizes_device_id(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "DeviceID": "device1"},
            {"_timestamp": 102, "DeviceID": "device1"},
            {"_timestamp": 103, "DeviceID": "device2"},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_device_ids = factory.get_all_normalized(4)
    assert existing_device_ids == ["device1", "device1", "device2"]


async def test_normalizes_app_name(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "APIKey": 2},
            {"_timestamp": 102, "APIKey": 4},
            {"_timestamp": 103, "APIKey": 30488},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_applications = factory.get_all_normalized(5)
    assert existing_applications == ["METRO", "MOBILE_MAPS", "NAVIGATOR"]


async def test_normalizes_app_platform(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "AppPlatform": "iOS"},
            {"_timestamp": 102, "AppPlatform": "android"},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_platforms = factory.get_all_normalized(6)
    assert existing_platforms == ["IOS", "ANDROID"]


async def test_normalizes_app_version_name(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "AppVersionName": "1.2.3"},
            {"_timestamp": 102, "AppVersionName": "1.2.3"},
            {"_timestamp": 103, "AppVersionName": "1.3.5"},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_app_version_names = factory.get_all_normalized(7)
    assert existing_app_version_names == ["1.2.3", "1.2.3", "1.3.5"]


async def test_normalizes_app_build_number(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "AppBuildNumber": 123},
            {"_timestamp": 102, "AppBuildNumber": 123},
            {"_timestamp": 103, "AppBuildNumber": 135},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_app_build_numbers = factory.get_all_normalized(8)
    assert existing_app_build_numbers == [123, 123, 135]


async def test_normalizes_latitude(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "Latitude": 57.22},
            {"_timestamp": 102, "Latitude": 57.22},
            {"_timestamp": 103, "Latitude": 35.33},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_user_latitudes = factory.get_all_normalized(9)
    assert existing_user_latitudes == [
        Decimal("57.22"),
        Decimal("57.22"),
        Decimal("35.33"),
    ]


async def test_normalizes_longitude(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101, "Longitude": 57.22},
            {"_timestamp": 102, "Longitude": 57.22},
            {"_timestamp": 103, "Longitude": 35.33},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_user_longitudes = factory.get_all_normalized(10)
    assert existing_user_longitudes == [
        Decimal("57.22"),
        Decimal("57.22"),
        Decimal("35.33"),
    ]


async def test_normalizes_place_id_to_null(factory, task_factory, warden_client_mock):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101},
            {"_timestamp": 102},
            {"_timestamp": 103},
        ]
    )

    await task_factory()(warden_client=warden_client_mock)

    existing_place_ids = factory.get_all_normalized(11)
    assert existing_place_ids == [None, None, None]


@pytest.mark.parametrize(
    ["app_filter", "expected_app_filter"],
    [
        (
            {},
            {
                "android_maps_build": 4294967296,
                "android_metro_build": 4294967296,
                "android_navi_build": 4294967296,
                "ios_maps_build": 4294967296,
                "ios_metro_build": 4294967296,
                "ios_navi_build": 4294967296,
            },
        ),
        (
            {
                "android_maps_build": 301,
                "android_metro_build": 302,
                "android_navi_build": 303,
                "ios_maps_build": 304,
                "ios_metro_build": 305,
                "ios_navi_build": 306,
            },
            {
                "android_maps_build": 301,
                "android_metro_build": 302,
                "android_navi_build": 303,
                "ios_maps_build": 304,
                "ios_metro_build": 305,
                "ios_navi_build": 306,
            },
        ),
    ],
)
async def test_inserts_metadata(
    app_filter, expected_app_filter, factory, task_factory, warden_client_mock
):
    factory.insert_source_datatube(
        [
            {"_timestamp": 101},
            {"_timestamp": 102},
            {"_timestamp": 103},
        ]
    )

    await task_factory(app_filter=app_filter)(warden_client=warden_client_mock)

    existing_data_in_db = factory.get_all_normalized(12)
    existing_metadata = list(map(json.loads, existing_data_in_db))
    assert existing_metadata == [
        {
            "source": "datatube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
        {
            "source": "datatube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
        {
            "source": "datatube",
            "warden_executor_id": warden_client_mock.executor_id,
            "app_filter": expected_app_filter,
        },
    ]
