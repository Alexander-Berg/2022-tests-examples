import datetime as dt
from unittest import mock

from bson.objectid import ObjectId
import pytest
import pytz

from yt.common import datetime_to_string
from yql.client.operation import YqlOperationShareIdRequest

from maps.garden.libs_server.common.param_storage import (
    ParamStorage,
    ParameterName
)
from maps.garden.libs_server.task.operation_storage import (
    OperationStorage,
    TaskAnnotations,
    TaskOperation,
    YtOperation
)
from maps.garden.libs_server.task.task_log_manager import (
    TaskLogManager,
    TaskStatisticsRecord
)
from maps.garden.tools.operation_fetcher.lib.fetcher import (
    LIMIT_PER_OPERATION_ITERATION_REQUEST,
    OperationFetcher
)

NOW = dt.datetime(2020, 8, 25, 16, 20, 00, tzinfo=pytz.utc)

SEARCH_MARKER = "garden_search_marker_in_testing"
COMPLETED_MR_OPERATION_ID = "547cc71e-39e29570-3fe03e8-8c3b2e56"
COMPLETED_MR_OPERATION = {
    "id": COMPLETED_MR_OPERATION_ID,
    "state": "completed",
    "type": "map",
    "spec": {
        "annotations": {
            "contour_name": "",
            "garden_search_marker": SEARCH_MARKER,
            "instance": "testing",
            "build_id": 199,
            "module_name": "ymapsdf",
            "task_id": 16823692443886867843,
            "task_name": "TestTask",
            "task_key": "5fac0e6c9c24b63044ddcab1",
        },
        "pool": "garden_development",
        "description": {
            "yql_op_id": "60119320d2b70c6385c33032"
        }
    },
    "alerts": {
        "legacy_live_preview_suppressed": {
            "code": 1,
            "message": "Legacy live preview is suppressed"
        },
        "important_alert": {
            "message": "Important alert"
        }
    },
    "start_time": datetime_to_string(NOW - dt.timedelta(minutes=10)),
    "finish_time": datetime_to_string(NOW - dt.timedelta(minutes=9)),
}

COMPLETED_VANILLA_OPERATION_MULTIPLE_JOB = {
    "id": "operation3",
    "type": "vanilla",
    "start_time": datetime_to_string(NOW - dt.timedelta(minutes=8)),
    "finish_time": datetime_to_string(NOW - dt.timedelta(minutes=3)),
    "state": "completed",
    "spec": {
        "pool": "garden_development",
        "annotations": {
            "contour_name": "",
            "garden_search_marker": SEARCH_MARKER,
            "instance": "testing",
            "build_id": 1,
            "module_name": "test_module",
            "tasks": {
                "2": {
                    "consumption": {},
                    "task_name": "TestTask",
                    "task_key": "5fac0e6c9c24b63044ddcab2",
                },
                "3": {
                    "consumption": {},
                    "task_name": "TestTask",
                    "task_key": "5fac0e6c9c24b63044ddcab3",
                }
            }
        }
    }
}


CLUSTER = "hahn"


@pytest.fixture
def param_storage(db):
    return ParamStorage(db)


@pytest.fixture
def operation_storage(db):
    return OperationStorage(db)


@pytest.fixture
def task_log_manager(db):
    return TaskLogManager(db)


@pytest.fixture
def yt_client(mocker):
    yt_client_mock = mock.Mock()
    mocker.patch("maps.garden.tools.operation_fetcher.lib.fetcher.yt.YtClient", return_value=yt_client_mock)
    return yt_client_mock


@pytest.fixture
def operation_fetcher(db, operation_storage, param_storage, task_log_manager):
    return OperationFetcher(
        yt_config={},
        operation_storage=operation_storage,
        task_log_manager=task_log_manager,
        param_storage=param_storage,
        search_marker=SEARCH_MARKER
    )


@pytest.mark.freeze_time(NOW)
def test_fetch_recent_operations(mocker, param_storage, operation_storage, operation_fetcher, yt_client):
    mocker.patch.object(YqlOperationShareIdRequest, "run")
    mocker.patch.object(YqlOperationShareIdRequest, "is_ok", new_callable=mocker.PropertyMock).return_value = True
    mocker.patch.object(YqlOperationShareIdRequest, "json", new_callable=mocker.PropertyMock).return_value = "1001"

    yt_client.iterate_operations.return_value = [COMPLETED_MR_OPERATION, COMPLETED_VANILLA_OPERATION_MULTIPLE_JOB]

    param_storage[ParameterName.LAST_OPERATION_FETCH_TIMESTAMP] = \
        str(int((NOW - dt.timedelta(minutes=20)).timestamp()))

    operation_fetcher.fetch_recent_operations()

    yt_client.iterate_operations.assert_called_with(
        filter=SEARCH_MARKER,
        include_archive=True,
        from_time=NOW - dt.timedelta(minutes=21),
        to_time=NOW,
        attributes=["type", "spec"],
        limit_per_request=LIMIT_PER_OPERATION_ITERATION_REQUEST,
    )

    operation_ids = [COMPLETED_MR_OPERATION_ID, COMPLETED_VANILLA_OPERATION_MULTIPLE_JOB["id"]]
    items = [x.dict() for x in operation_storage.find_by_operation_ids(operation_ids)]
    assert len(items) == 3
    for item in items:
        item["operation"]["started_at"] = item["operation"]["started_at"].isoformat()
        item["operation"]["finished_at"] = item["operation"]["finished_at"].isoformat()
        item["task"]["key"] = str(item["task"]["key"])
        item.pop("_id")
    return items


@pytest.mark.freeze_time(NOW)
def test_update_finished_operations(task_log_manager, operation_storage, operation_fetcher, yt_client):
    task_key1 = _make_task_statistics(task_log_manager, "unfinished_op")
    task_key2 = _make_task_statistics(task_log_manager, "vanilla_op", finished_at=NOW)
    task_key3 = _make_task_statistics(task_log_manager, "vanilla_op", finished_at=NOW)

    operations_data = (
        (task_key1, "unfinished_op", "vanilla", "1"),
        (task_key2, "vanilla_op", "vanilla", "2"),
        (task_key3, "vanilla_op", "vanilla", "3"),
        (task_key3, "map_op", "map", "3"),
    )

    task_operations = []

    for task_key, op_id, op_type, task_id in operations_data:
        task_operation = TaskOperation(
            operation=YtOperation(
                id=op_id,
                type=op_type,
                cluster="hahn",
                pool="garden_development",
                started_at=(NOW - dt.timedelta(minutes=10)),
            ),
            task=TaskAnnotations(
                garden_task_id=task_id,
                name="MyTask",
                module_name="test_module",
                build_id="1",
                contour_name="testing",
                key=task_key,
            ),
            result=None,
        )

        task_operations.append(task_operation)

    assert operation_storage.create_many(task_operations) == len(operations_data)

    multiple_job_yt_operation = COMPLETED_VANILLA_OPERATION_MULTIPLE_JOB.copy()
    multiple_job_yt_operation["spec"]["annotations"]["tasks"]["2"]["task_key"] = str(task_key2)
    multiple_job_yt_operation["spec"]["annotations"]["tasks"]["3"]["task_key"] = str(task_key3)

    map_operation = COMPLETED_MR_OPERATION.copy()
    map_operation["spec"]["annotations"]["task_key"] = str(task_key3)

    yt_client.get_operation.side_effect = [
        multiple_job_yt_operation,
        map_operation,
    ]

    yt_client.list_jobs.return_value = {
        "jobs": [
            {
                "state": "completed",
                "start_time": datetime_to_string(NOW - dt.timedelta(minutes=5)),
                "finish_time": datetime_to_string(NOW - dt.timedelta(minutes=4)),
                "task_name": "2",
            },
            {
                "state": "aborted",
                "start_time": datetime_to_string(NOW - dt.timedelta(minutes=6)),
                "finish_time": datetime_to_string(NOW - dt.timedelta(minutes=5)),
                "task_name": "3",
            },
            {
                "state": "completed",
                "start_time": datetime_to_string(NOW - dt.timedelta(minutes=5)),
                "finish_time": datetime_to_string(NOW - dt.timedelta(minutes=4)),
                "task_name": "3",
            },
        ]
    }

    operation_fetcher.update_finished_operations()

    items = [
        x.dict()
        for x in operation_storage.find_by_operation_ids([op.operation.id for op in task_operations])
        if x.result
    ]
    assert set(x["task"]["key"] for x in items) == {task_key2, task_key3}

    for item in items:
        item["operation"]["started_at"] = item["operation"]["started_at"].isoformat()
        item["operation"]["finished_at"] = item["operation"]["finished_at"].isoformat()
        item["result"]["started_at"] = item["result"]["started_at"].isoformat()
        item["result"]["finished_at"] = item["result"]["finished_at"].isoformat()
        item["task"].pop("key")
        item.pop("_id")
    return items


def _make_task_statistics(task_log_manager, operation_id, finished_at=None):
    task = TaskStatisticsRecord(
        mongo_id=ObjectId(),
        name="",
        garden_task_id="1",
        module_name="",
        build_id=1,
        contour_name="",
        creates=[],
        demands=[],
        insert_traceback="",
        created_at=NOW,
        started_at=NOW,
        finished_at=finished_at,
        main_operation_id=operation_id
    )
    task_log_manager._task_statistics_storage.upsert_one(task)
    return task.mongo_id
