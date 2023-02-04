import json
import mongomock
import pytest
import yatest.common

from maps.garden.libs_server.build.build_defs import Build

import maps.garden.libs_server.log_storage.module_event_storage as elm
from maps.garden.libs_server.common.module_event_types import BuildEventType, CommonEventType, ModuleVersionEventType


TEST_BUILD = Build(
    id=123,
    name="test_module",
    contour_name="test_contour",
    module_version="test_module",
    author="test_user",
)


def test_adding_events():
    database = mongomock.MongoClient(tz_aware=True).db
    manager = elm.ModuleEventStorage(database)

    manager.add_build_event(
        build=TEST_BUILD,
        username=TEST_BUILD.author,
        event_type=BuildEventType.CREATED,
        extra_info={
            "extra": "info",
        }
    )

    manager.add_build_event(
        build=TEST_BUILD,
        event_type=BuildEventType.COMPLETED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        event_type=BuildEventType.FAILED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        username="test_user",
        event_type=BuildEventType.CANCELED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        username="test_user",
        event_type=BuildEventType.REMOVED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        username="test_user",
        event_type=BuildEventType.RESTARTED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        username="test_user",
        event_type=BuildEventType.PINED,
    )

    manager.add_build_event(
        build=TEST_BUILD,
        username="test_user",
        event_type=BuildEventType.UNPINED,
    )

    manager.add_module_version_event(
        contour_name="test_contour",
        module_name="test_module",
        module_version="test_module",
        username="test_user",
        event_type=ModuleVersionEventType.RELEASED,
    )

    manager.add_module_version_event(
        contour_name="test_contour",
        module_name="test_module",
        module_version="test_module",
        username="test_user",
        event_type=ModuleVersionEventType.ROLLEDBACK,
    )

    manager.add_common_event(
        contour_name="test_contour",
        module_name="test_module",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED,
    )

    manager.add_common_event(
        contour_name="test_contour",
        module_name="test_module",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_DISABLED,
    )

    records = [elm.EventRecord.parse_obj(record) for record in database.module_events.find()]
    assert len(records) == 12

    return [dict(record) for record in database.module_events.find({}, {"added_at": 0, "_id": 0})]


@pytest.mark.parametrize(
    "args",
    [
        {},
        {
            "contour_name": "other_contour",
        },
        {
            "module_name": "ymapsdf",
        },
        {
            "username": "ilu",
        },
        {

            "event_type": elm.BuildEvent(event_type=BuildEventType.CREATED, build_id=0),
        },
        {
            "event_type": elm.ModuleVersionEvent(event_type=ModuleVersionEventType.RELEASED),
        },
        {
            "event_type": elm.CommonEvent(event_type=CommonEventType.AUTOSTART_DISABLED),
        },
        {
            "build_id": 5,
        },
        {
            "date_less_than": "2021-04-12T00:00:00.000Z",
        },
        {
            "date_less_than": "2021-04-12T00:00:00.000Z",
            "build_id": 1,
            "event_type": elm.BuildEvent(event_type=BuildEventType.CREATED, build_id=0),
            "username": "ilu",
        },
        {
            "build_id": 100500,  # no one record
        },
    ]
)
def test_get_events(args):
    database = mongomock.MongoClient(tz_aware=True).db
    with open(yatest.common.source_path("maps/garden/libs_server/log_storage/tests/test_data/event_test_data.json"), "r") as f:
        test_events = json.load(f)

    database.module_events.insert_many(test_events)

    manager = elm.ModuleEventStorage(database)

    args["limit"] = 100
    args.setdefault("contour_name", "development")
    args.setdefault("module_name", "altay")

    return [o.dict() for o in manager.find_module_events(**args)]


def test_removing():
    database = mongomock.MongoClient(tz_aware=True).db
    manager = elm.ModuleEventStorage(database)

    assert database.module_events.count() == 0, "collection not empty"

    manager.add_common_event(
        contour_name="contour_1",
        module_name="module_1",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    manager.add_common_event(
        contour_name="contour_1",
        module_name="module_2",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    manager.add_common_event(
        contour_name="contour_2",
        module_name="module_1",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    manager.add_common_event(
        contour_name="contour_2",
        module_name="module_2",
        username="test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    assert database.module_events.count() == 4

    manager.remove_events(module_name="module_1")
    assert len(manager.find_module_events(contour_name="contour_1", module_name="module_1", limit=100)) == 0
    assert len(manager.find_module_events(contour_name="contour_2", module_name="module_1", limit=100)) == 0
    assert len(manager.find_module_events(contour_name="contour_1", module_name="module_2", limit=100)) == 1
    assert len(manager.find_module_events(contour_name="contour_2", module_name="module_2", limit=100)) == 1

    manager.remove_events(contour_name="contour_1")
    assert len(manager.find_module_events(contour_name="contour_1", module_name="module_2", limit=100)) == 0
    assert len(manager.find_module_events(contour_name="contour_2", module_name="module_2", limit=100)) == 1
