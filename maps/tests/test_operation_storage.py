import datetime as dt

from bson.objectid import ObjectId
import mongomock
import pytest
import pytz

from yt.common import datetime_to_string

from maps.garden.libs_server.task import operation_storage

NOW = dt.datetime(2020, 8, 25, 16, 20, 00, tzinfo=pytz.utc)

COMPLETED_MR_OPERATION_ID = "547cc71e-39e29570-3fe03e8-8c3b2e56"
COMPLETED_MR_OPERATION_TASK_KEY = "5fac0e6c9c24b63044ddcab1"
COMPLETED_MR_OPERATION = {
    "id": COMPLETED_MR_OPERATION_ID,
    "state": "completed",
    "type": "map",
    "spec": {
        "annotations": {
            "contour_name": "development",
            "garden_search_marker": "garden_search_marker_in_testing",
            "instance": "testing",
            "build_id": 199,
            "module_name": "ymapsdf",
            "task_id": 16823692443886867843,
            "task_name": "TestTask",
            "task_key": ObjectId(COMPLETED_MR_OPERATION_TASK_KEY),
            "consumption": {},
            "output_file": "output_file",
            "log_file": "log_file",
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

RUNNING_VANILLA_OPERATION_ID = "647cc71e-39e29570-3fe03e8-8c3b2e57"
RUNNING_VANILLA_OPERATION_TASK_KEY = "5fac0e6c9c24b63044ddcab2"
RUNNING_VANILLA_OPERATION = {
    "id": RUNNING_VANILLA_OPERATION_ID,
    "state": "running",
    "type": "vanilla",
    "spec": {
        "annotations": {
            "contour_name": "development",
            "garden_search_marker": "garden_search_marker_in_testing",
            "instance": "testing",
            "build_id": 199,
            "module_name": "ymapsdf",
            "tasks": {
                "16429757709463427849": {
                    "consumption": {
                        "cpu": 1,
                        "ram": 32212254720
                    },
                    "task_name": "MkDataTask",
                    "task_key": ObjectId(RUNNING_VANILLA_OPERATION_TASK_KEY),
                    "output_file": "output_file",
                    "log_file": "log_file",
                }
            }
        },
        "pool": "garden_development",
    },
    "start_time": datetime_to_string(NOW - dt.timedelta(minutes=5)),
}


CLUSTER = "hahn"


@pytest.mark.freeze_time(NOW)
def test_db_operation_from_yt_operation():
    results = []
    for yt_operation in (COMPLETED_MR_OPERATION, RUNNING_VANILLA_OPERATION):
        for task_operation in operation_storage.TaskOperation.from_yt_operation(yt_operation, CLUSTER):
            result = task_operation.dict()
            # Help ya tools that fail to serialize ObjectId fields.
            result["task"]["key"] = str(result["task"]["key"])
            results.append(result)
    return results


@pytest.mark.freeze_time(NOW)
def test_db_operation_result_from_yt_operation():
    db_operation_results = []
    for result in operation_storage.TaskResult.from_yt_operation(COMPLETED_MR_OPERATION).values():
        db_operation_results.append(result)
    return [x.dict() for x in db_operation_results if x]


@pytest.mark.freeze_time(NOW)
def test_operation_storage():
    db = mongomock.MongoClient(tz_aware=True).db
    storage = operation_storage.OperationStorage(db)

    storage.create_many(operation_storage.TaskOperation.from_yt_operation(COMPLETED_MR_OPERATION, CLUSTER))
    storage.create_many(operation_storage.TaskOperation.from_yt_operation(RUNNING_VANILLA_OPERATION, CLUSTER))

    assert set(op.operation.id for op in storage.find_missing_yql_share_id()) == {COMPLETED_MR_OPERATION_ID}
    assert set(op.operation.id for op in storage.find_by_operation_ids([RUNNING_VANILLA_OPERATION_ID])) == {
        RUNNING_VANILLA_OPERATION_ID
    }
    assert set(op.operation.id for op in storage.find_missing_task_result()) == {
        RUNNING_VANILLA_OPERATION_ID, COMPLETED_MR_OPERATION_ID
    }

    task_operations_to_update = []
    results = operation_storage.TaskResult.from_yt_operation(COMPLETED_MR_OPERATION)
    for op in storage.find_missing_task_result():
        if op.operation.id == COMPLETED_MR_OPERATION_ID:
            op.result = results[op.task.key]
            task_operations_to_update.append(op)

    storage.update_results(task_operations_to_update)

    task_operations_to_delete = []
    missing_result_operation_ids = set()
    for op in storage.find_missing_task_result():
        missing_result_operation_ids.add(op.operation.id)
        task_operations_to_delete.append(op.mongo_id)

    assert missing_result_operation_ids == {RUNNING_VANILLA_OPERATION_ID}

    storage.update_yql_share_id(COMPLETED_MR_OPERATION_ID, COMPLETED_MR_OPERATION_TASK_KEY, "123456")
    assert not set(op.operation.id for op in storage.find_missing_yql_share_id())

    storage.delete_many(task_operations_to_delete)
    assert not set(op.operation.id for op in storage.find_missing_task_result())
