import datetime as dt
import pytest
from unittest import mock

from bson.objectid import ObjectId
import pytz

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import PythonResource
from maps.garden.libs_server.task.yt_task import (
    ContourSettings,
    SchedulerTaskEvent,
    YtTask
)
from maps.garden.libs_server.task import task_storage

CONTOUR_NAME = "contour_name"

NOW = dt.datetime(2020, 8, 25, 16, 20, 00, tzinfo=pytz.utc)


@pytest.fixture
def tasks_collection(db):
    return db.task_log


@pytest.mark.freeze_time(NOW)
def test_save_task(db, freezer):
    storage = task_storage.TaskStorage(db)
    yt_task = _make_yt_task()

    storage.insert(task_storage.TaskRecord.from_yt_task(yt_task))

    expected_task_record = task_storage.TaskRecord(
        mongo_id=yt_task.task_key,
        name="task_name",
        garden_task_id="1",
        module_name="test_module",
        build_id=10,
        contour_name=CONTOUR_NAME,
        input_proto=b"mocked input",
        output_proto=None,
        created_at=NOW,
        updated_at=NOW,
    )
    actual_task_record = storage.find_by_key(yt_task.task_key)
    assert actual_task_record == expected_task_record

    freezer.move_to(NOW + dt.timedelta(hours=1))
    storage.update(
        task_key=yt_task.task_key,
        invocation_status=task_storage.TaskInvocationStatus.RUNNING,
    )
    expected_task_record.updated_at = NOW + dt.timedelta(hours=1)
    expected_task_record.invocation_status = task_storage.TaskInvocationStatus.RUNNING
    actual_task_record = storage.find_by_key(yt_task.task_key)
    assert actual_task_record == expected_task_record

    freezer.move_to(NOW + dt.timedelta(hours=2))
    storage.update(
        task_key=yt_task.task_key,
        invocation_status=task_storage.TaskInvocationStatus.FINISHED,
        output_proto=b"mocked output"
    )
    expected_task_record.updated_at = NOW + dt.timedelta(hours=2)
    expected_task_record.invocation_status = task_storage.TaskInvocationStatus.FINISHED
    expected_task_record.output_proto = b"mocked output"
    actual_task_record = storage.find_by_key(yt_task.task_key)
    assert actual_task_record == expected_task_record

    freezer.move_to(NOW + dt.timedelta(hours=3))
    storage.update(
        task_key=yt_task.task_key,
        invocation_status=task_storage.TaskInvocationStatus.RUNNING
    )
    actual_task_record = storage.find_by_key(yt_task.task_key)
    assert actual_task_record == expected_task_record


@pytest.mark.freeze_time(NOW)
def test_find_tasks(db, freezer):
    storage = task_storage.TaskStorage(db)

    yt_task1 = _make_yt_task()
    storage.insert(task_storage.TaskRecord.from_yt_task(yt_task1))
    storage.update(
        task_key=yt_task1.task_key,
        invocation_status=task_storage.TaskInvocationStatus.RUNNING
    )

    freezer.move_to(NOW + dt.timedelta(hours=2))
    yt_task2 = _make_yt_task()
    storage.insert(task_storage.TaskRecord.from_yt_task(yt_task2))

    assert set(
        str(t.mongo_id) for t in storage.find(invocation_statuses=[task_storage.TaskInvocationStatus.RUNNING])
    ) == {yt_task1.task_key}

    assert set(str(t.mongo_id) for t in storage.find(updated_before=NOW + dt.timedelta(hours=1))) == {yt_task1.task_key}
    assert set(str(t.mongo_id) for t in storage.find(updated_after=NOW + dt.timedelta(hours=1))) == {yt_task2.task_key}


@pytest.mark.use_local_mongo
def test_pick_by_key_and_update(db):
    # use local db, because current version of mongomock can't work with "$cond" into update()
    storage = task_storage.TaskStorage(db)

    yt_task = _make_yt_task()
    storage.insert(task_storage.TaskRecord.from_yt_task(yt_task))
    storage.update(task_key=yt_task.task_key)

    old_task_record = storage.pick_by_key_and_update(
        task_key=yt_task.task_key,
        operation_id="new_operation_id",
        job_id="new_job_id"
    )

    assert not old_task_record.input_was_used
    assert old_task_record.operation_id is None
    assert old_task_record.job_id is None

    new_task_record = storage.find_by_key(yt_task.task_key)

    assert new_task_record.input_was_used
    assert new_task_record.operation_id == "new_operation_id"
    assert new_task_record.job_id == "new_job_id"

    storage.pick_by_key_and_update(
        task_key=yt_task.task_key,
        operation_id="another_operation_id",
        job_id="another_job_id"
    )

    extra_new_task_record = storage.find_by_key(yt_task.task_key)
    assert extra_new_task_record.operation_id == "new_operation_id"
    assert extra_new_task_record.job_id == "new_job_id"

    unknown_task_record = storage.pick_by_key_and_update(
        task_key=str(ObjectId()),
        operation_id="any_operation_id",
        job_id="any_job_id"
    )
    assert unknown_task_record is None


def _make_yt_task():
    task = mock.Mock(
        displayed_name="task_name",
        insert_traceback="some traceback",
    )
    task.make_task_call_stdin.return_value = b"mocked input"

    versioned_task = mock.Mock(
        request_ids=[1, 2],
        task=task,
        contour_name=CONTOUR_NAME,
        module_name="test_module",
        module_version="1234",
        retries=1,
        ready_for_execution_at=dt.datetime.now(pytz.utc),
    )

    demands_resource = PythonResource("demands_resource")
    demands_resource.version = Version(properties={"date": "dateA"})

    creates_resource = PythonResource("creates_resource")
    creates_resource.version = Version(properties={"date": "dateB"})

    task = YtTask(
        task_id=1,
        contour_name=CONTOUR_NAME,
        module_name=versioned_task.module_name,
        build_id=10,
        task_key=str(ObjectId()),
        consumption={"cpu": 1},
        log_file_basename="log_file_basename",
        versioned_task=versioned_task,
        operation_id="vanilla_operation_id",
        job_id="job_id",
        demands={
            "demands_resource": demands_resource
        },
        creates={
            "creates_resource": creates_resource
        },
        contour_settings=ContourSettings(
            environment_settings={
                "logs_storage": {
                    "type": "s3",
                    "bucket": "bucket-for-logs",
                    "key_prefix": ""
                },
                "s3": {
                    "host": "localhost",
                    "port": 1000
                }
            },
            environment_settings_ypath="",
            yt_work_dir="",
        )
    )
    task.register_event(SchedulerTaskEvent.RESOURCES_ALLOCATED)
    return task
