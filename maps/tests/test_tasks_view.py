import datetime as dt
import pytest
import pytz
import http.client
from bson.objectid import ObjectId

from maps.garden.libs_server.common.task_monitor_storage import MonitoredTaskStatus


CONTOUR_1="test_contour1"
CONTOUR_2="test_contour2"
MODULE_1="test_module1"
MODULE_2="test_module2"
BUILD_ID_1=15
BUILD_ID_2=16
BUILD_ID_3=17

MONITORED_TASK_RECORDS = [
    {
        "mongo_id": ObjectId("5dfb8483bbca76ec4f5dbef9"),
        "task_id": "16163868054298391606",
        "contour_name": CONTOUR_1,
        "module_name": MODULE_1,
        "build_id": BUILD_ID_1,
        "status": MonitoredTaskStatus.PENDING,
        "added_at": dt.datetime(2020, 12, 17, 15, 2, 20, tzinfo=pytz.utc),
        "task_name": "ExampleTask1",
        "details": "",
        "resources": None,
        "retries": 1,
        "sort_rank": 1
    },
    {
        "mongo_id": ObjectId("5fdb657505ff1f6bcd86baef"),
        "task_id": "16163868054298391607",
        "contour_name": CONTOUR_2,
        "module_name": MODULE_2,
        "build_id": BUILD_ID_3,
        "status": MonitoredTaskStatus.RUNNING,
        "added_at": dt.datetime(2020, 12, 17, 15, 1, 22, tzinfo=pytz.utc),
        "task_name": "ExampleTask3",
        "details": "",
        "resources": None,
        "retries": 0,
        "url": {
            "href": "https://yt.yandex-team.ru/hahn/operation?mode=detail&id=2fb2a0dc-ff4f63f0-3fe03e8-840c7640",
            "text": "the text"
        },
        "sort_rank": 0
    },
    {
        "mongo_id": ObjectId("5fdb657505ff1f6bcd86baea"),
        "task_id": "16163868054298391608",
        "contour_name": CONTOUR_2,
        "module_name": MODULE_2,
        "build_id": BUILD_ID_2,
        "status": MonitoredTaskStatus.RUNNING,
        "added_at": dt.datetime(2020, 12, 17, 15, 1, 21, tzinfo=pytz.utc),
        "task_name": "ExampleTask2",
        "details": "",
        "resources": None,
        "retries": 0,
        "sort_rank": 0
    }
]


def test_tasks_view_get_empty_storage(garden_client, db):
    response = garden_client.get("/tasks/")
    assert response.status_code == http.client.OK
    return response.get_json()


def test_tasks_view_get_all(garden_client, db):
    db.task_monitor.insert_many(MONITORED_TASK_RECORDS)
    response = garden_client.get("/tasks/")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    ("limit"),
    [
        1,
        2
    ]
)
def test_tasks_view_get_with_limit(garden_client, db, limit):
    db.task_monitor.insert_many(MONITORED_TASK_RECORDS)
    response = garden_client.get(f"/tasks/?limit={limit}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    (
        "contour",
        "module",
        "build_id",
    ),
    [
        (CONTOUR_1, MODULE_1, BUILD_ID_1),
        (CONTOUR_2, MODULE_2, BUILD_ID_2),
        (CONTOUR_2, MODULE_1, BUILD_ID_2)
    ]
)
def test_tasks_view_get_with_args(garden_client, db, contour, module, build_id):
    db.task_monitor.insert_many(MONITORED_TASK_RECORDS)
    response = garden_client.get(f"/tasks/?contour={contour}&module={module}&build_id={build_id}")
    assert response.status_code == http.client.OK
    return response.get_json()
