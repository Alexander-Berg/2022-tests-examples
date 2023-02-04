import datetime as dt
import json
import mongomock
import pytest
import pytz
import yatest.common

from maps.garden.libs_server.common.log_types import LogRecordType
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage, ModuleLog


@pytest.mark.freeze_time(dt.datetime(2016, 11, 23, 15, 25, 22, tzinfo=pytz.utc))
def test_adding_log():
    database = mongomock.MongoClient(tz_aware=True).db
    mls = ModuleLogStorage(database)
    exception1 = Exception("some_exception_1")
    exception2 = Exception("some_exception_2")

    mls.add_log(
        log_type=LogRecordType.SCAN_RESOURCES,
        contour_name="some_contour",
        module_name="some_module",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    mls.add_log(
        log_type=LogRecordType.SCAN_RESOURCES,
        contour_name="some_contour",
        module_name="some_module",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=[exception1, exception2]
    )

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="some_contour",
        module_name="some_module",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="some_contour",
        module_name="some_module",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=exception2
    )

    records = [ModuleLog.parse_obj(r).dict() for r in database.module_logs.find()]
    assert len(records) == 4
    return [r for r in database.module_logs.find({}, {"_id": 0, "added_at": 0})]


@pytest.mark.parametrize(
    "args",
    [
        {},
        {
            "contour_name": "other_contour",
        },
        {
            "module_name": "other_module",
        },
        {
            "username": "othe",
        },
        {
            "log_type": LogRecordType.AUTOSTARTER,
        },
        {
            "log_type": LogRecordType.SCAN_RESOURCES,
        },
        {
            "date_less_than": "2021-04-12T00:00:00.000Z",
        },
        {
            "date_less_than": "2021-04-12T00:00:00.000Z",
            "log_type": LogRecordType.AUTOSTARTER,
            "username": "tes",
        },

    ]
)
def test_find_logs(args):
    database = mongomock.MongoClient(tz_aware=True).db
    with open(yatest.common.source_path("maps/garden/libs_server/log_storage/tests/test_data/log_test_data.json"), "r") as f:
        test_logs = json.load(f)

    database.module_logs.insert_many(test_logs)

    mls = ModuleLogStorage(database)

    args["limit"] = 100
    args.setdefault("contour_name", "some_contour")
    args.setdefault("module_name", "some_module")

    return [o.dict() for o in mls.find_log_records(**args)]


def test_removing_logs():
    database = mongomock.MongoClient(tz_aware=True).db
    mls = ModuleLogStorage(database)

    assert database.module_logs.count() == 0, "collection not empty"

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="contour_1",
        module_name="module_1",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="contour_1",
        module_name="module_2",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="contour_2",
        module_name="module_1",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    mls.add_log(
        log_type=LogRecordType.AUTOSTARTER,
        contour_name="contour_2",
        module_name="module_2",
        module_version="some_version",
        username="test_user",
        message="message text",
        exception=None
    )

    assert database.module_logs.count() == 4

    mls.remove_logs(module_name="module_1")
    assert len(mls.find_log_records(contour_name="contour_1", module_name="module_1", limit=100)) == 0
    assert len(mls.find_log_records(contour_name="contour_2", module_name="module_1", limit=100)) == 0
    assert len(mls.find_log_records(contour_name="contour_1", module_name="module_2", limit=100)) == 1
    assert len(mls.find_log_records(contour_name="contour_2", module_name="module_2", limit=100)) == 1

    mls.remove_logs(contour_name="contour_1")
    assert len(mls.find_log_records(contour_name="contour_1", module_name="module_2", limit=100)) == 0
    assert len(mls.find_log_records(contour_name="contour_2", module_name="module_2", limit=100)) == 1

    mls.remove_logs(contour_name="contour_2", module_name="module_2")
    assert len(mls.find_log_records(contour_name="contour_2", module_name="module_2", limit=100)) == 0

    assert database.module_logs.count() == 0, "collection not empty"
