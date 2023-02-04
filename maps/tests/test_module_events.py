import datetime as dt
import http.client
import pytest

import maps.garden.libs_server.log_storage.module_event_storage as elm
import maps.garden.libs_server.log_storage.module_log_storage as mls

from maps.garden.server.lib.formats.api_objects import ContourCreationInfo


TEST_DATA_EVENTS = [
    {
        "added_at": dt.datetime(2021, 1, 10, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": None,
        "build_event": {
            "event_type": "failed",
            "build_id": 1,
        },
        "module_version_event": None,
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 1, 11, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": "ilukonin",
        "build_event": {
            "event_type": "created",
            "build_id": 2,
        },
        "module_version_event": None,
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 2, 10, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": "test_user",
        "build_event": {
            "event_type": "created",
            "build_id": 3,
        },
        "module_version_event": None,
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 2, 11, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": None,
        "build_event": {
            "event_type": "completed",
            "build_id": 2,
        },
        "module_version_event": None,
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 1, 11, 10, 0, 0),
        "contour_name": "someuser_other_contour",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": "ilukonin",
        "build_event": {
            "event_type": "created",
            "build_id": 2,
        },
        "module_version_event": None,
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 3, 11, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": "ilukonin",
        "build_event": None,
        "module_version_event": {
            "event_type": "released",
        },
        "common_event": None
    },
    {
        "added_at": dt.datetime(2021, 3, 11, 10, 0, 0),
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "username": "ilukonin",
        "build_event": None,
        "module_version_event": None,
        "common_event": {
            "event_type": "autostart_enabled",
        },
    },
]


def test_module_event_types(garden_client):
    response = garden_client.get("module_event_types/")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    "query",
    [
        "contour=unittest",
        "contour=someuser_other_contour",
        "contour=unittest&limit=1",
        "contour=unittest&date_from=2021-02-01",
        "contour=unittest&login=iluk",
        "contour=unittest&event_type=failed&event_type_class=build",
        "contour=unittest&build_id=2",
        "contour=unittest&build_id=2&event_type=created&event_type_class=build",
        "contour=unittest&event_type=autostart_enabled&event_type_class=common",
        "contour=unittest&event_type=released&event_type_class=module",
    ]
)
def test_module_event(garden_client, query, db):
    creation_info = ContourCreationInfo(name="someuser_other_contour")
    garden_client.post("/contours/", json=creation_info.dict())

    collection = db.module_events

    for item in TEST_DATA_EVENTS:
        record = elm.EventRecord.parse_obj(item)
        collection.insert_one(record.dict())

    response = garden_client.get(f"modules/test_module/events/?{query}")

    assert response.status_code == http.client.OK
    return response.get_json()


def test_module_log_types(garden_client):
    response = garden_client.get("module_log_types/")
    assert response.status_code == http.client.OK
    return response.get_json()


TEST_DATA_LOGS = [
    {
        "log_type": mls.LogRecordType.AUTOSTARTER,
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "added_at": dt.datetime(2021, 1, 10, 10, 0, 0),
        "username": "ilukonin",
        "message": "some message",
        "exceptions": [
            {
                "type": "ExceptionType",
                "message": "exception message 1",
                "traceback": "traceback",
            },
            {
                "type": "ExceptionType",
                "message": "exception message 2",
                "traceback": "traceback",
            }
        ]
    },
    {
        "log_type": mls.LogRecordType.SCAN_RESOURCES,
        "contour_name": "unittest",
        "module_name": "test_module",
        "module_version": "test_version",
        "added_at": dt.datetime(2021, 1, 8, 10, 0, 0),
        "username": "other_user",
        "message": "some message",
        "exceptions": [],
    },
    {
        "log_type": mls.LogRecordType.SCAN_RESOURCES,
        "contour_name": "someuser_other_contour",
        "module_name": "test_module",
        "module_version": "test_version",
        "added_at": dt.datetime(2021, 1, 8, 10, 0, 0),
        "username": "other_user",
        "message": "some message",
        "exceptions": [],
    }
]


@pytest.mark.freeze_time(dt.datetime(2021, 1, 12,  12, 30, 45))
@pytest.mark.parametrize(
    "query",
    [
        "contour=unittest",
        "contour=someuser_other_contour",
        "contour=unittest&limit=1",
        "contour=unittest&date_from=2021-01-09",
        "contour=unittest&login=iluk",
        "contour=unittest&log_type=autostarter",
    ]
)
def test_module_log(garden_client, query, db):
    creation_info = ContourCreationInfo(name="someuser_other_contour")
    garden_client.post("/contours/", json=creation_info.dict())

    collection = db.module_logs

    for item in TEST_DATA_LOGS:
        record = mls.ModuleLog.parse_obj(item)
        collection.insert_one(record.dict())

    response = garden_client.get(f"modules/test_module/logs/?{query}")

    assert response.status_code == http.client.OK
    return response.get_json()
