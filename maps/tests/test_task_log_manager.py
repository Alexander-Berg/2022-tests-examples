import datetime as dt
import pytest
from unittest import mock

from bson.objectid import ObjectId
import pytz
import typing as tp

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs_server.task.yt_task import (
    ContourSettings,
    ModuleTaskEvent,
    TaskExecutionInfo,
    TaskExecutionResult,
    ResourceTiming,
    SchedulerTaskEvent,
    YtTask
)
from maps.garden.libs_server.task import task_log_manager

CONTOUR_NAME = "contour_name"

NOW = dt.datetime(2020, 8, 25, 16, 20, 00, tzinfo=pytz.utc)

MODULE_EVENTS = {
    ModuleTaskEvent.STARTED: (NOW - dt.timedelta(seconds=9)),
    ModuleTaskEvent.RESOURCE_PREPARATION_STARTED: (NOW - dt.timedelta(seconds=8)),
    ModuleTaskEvent.INVOCATION_STARTED: (NOW - dt.timedelta(seconds=8)),
    ModuleTaskEvent.RESOURCE_COMMITMENT_STARTED: (NOW - dt.timedelta(seconds=3)),
    ModuleTaskEvent.FINISHED: (NOW - dt.timedelta(seconds=1)),
}


@pytest.fixture
def task_log_collection(db):
    return db.task_log


@pytest.mark.freeze_time(NOW)
def test_finished_task_log_with_creates(db, task_log_collection):
    manager = task_log_manager.TaskLogManager(db)
    _save_log_with_created_resources(manager)

    saved_task_logs = [task_log_manager.TaskLogRecord.parse_obj(r) for r in task_log_collection.find()]
    assert len(saved_task_logs) == 1
    task_log = saved_task_logs[0]
    assert task_log.mongo_id

    assert task_log.resources

    assert len(task_log.resources) == 2
    assert task_log.resources[0].key
    assert task_log.resources[0].name == "demands_resource"
    assert task_log.resources[0].class_name == "PythonResource"
    assert not task_log.resources[0].uri
    assert task_log.resources[0].properties == {"date": "dateA"}
    assert task_log.resources[1].key
    assert task_log.resources[1].name == "creates_resource"
    assert task_log.resources[1].class_name == "PythonResource"
    assert not task_log.resources[1].uri
    assert task_log.resources[1].properties == {"date": "dateB"}

    assert task_log.main_operation_id == "vanilla_operation_id"

    task_info = task_log.task_info
    assert len(task_info.demands) == 1
    assert len(task_info.creates) == 1

    assert list(task_info.resource_durations.key_to_preparation.values()) == [dt.timedelta(seconds=2)]
    assert list(task_info.resource_durations.key_to_commit.values()) == [dt.timedelta(seconds=3)]

    assert task_info.scheduler_events == {
        SchedulerTaskEvent.READY_FOR_EXECUTION: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.SCHEDULING_STARTED: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.OPERATION_CREATED: (NOW - dt.timedelta(seconds=10)),
        SchedulerTaskEvent.FINISHED: NOW,
    }

    assert task_info.module_events == MODULE_EVENTS
    assert not task_info.task_durations

    copy_updates = {
        "demands": [],
        "creates": [],
        "resource_durations": None,
        "scheduler_events": {},
        "module_events": {},
        "task_durations": None,
        "created_at": None,
        "started_at": None,
        "finished_at": None,
    }
    assert task_info.copy(update=copy_updates) == task_log_manager.TaskInfo(
        name="task_name",
        garden_task_id="1",
        predicted_consumption={"cpu": 1, "ssd": 0},
        module_name="test_module",
        build_id=10,
        contour_name=CONTOUR_NAME,
        module_version="1234",
        insert_traceback="some traceback",
        retry_attempt=1,
        log_url="http://localhost/bucket-for-logs/log_file_basename",
        error=None,
        main_operation_id="vanilla_operation_id",
        creates=[],
        demands=[],
        task_durations=None,
        scheduler_events={},
        module_events={},
    )


@pytest.mark.freeze_time(NOW)
def test_in_progress_task_log(db, task_log_collection):
    manager = task_log_manager.TaskLogManager(db)
    _save_log_without_created_resources(manager)

    saved_task_logs = [task_log_manager.TaskLogRecord.parse_obj(r) for r in task_log_collection.find()]
    assert len(saved_task_logs) == 1
    task_log = saved_task_logs[0]
    assert task_log.mongo_id

    assert len(task_log.resources) == 1
    assert task_log.resources[0].key
    assert task_log.resources[0].name == "demands_resource"
    assert task_log.resources[0].class_name == "PythonResource"
    assert not task_log.resources[0].uri
    assert task_log.resources[0].properties == {"date": "dateA"}

    assert task_log.main_operation_id == "vanilla_operation_id"

    assert task_log.task_info.scheduler_events == {
        SchedulerTaskEvent.READY_FOR_EXECUTION: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.SCHEDULING_STARTED: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.OPERATION_CREATED: (NOW - dt.timedelta(seconds=10)),
    }

    assert task_log.task_info.module_events == {
        ModuleTaskEvent.STARTED: (NOW - dt.timedelta(seconds=10)),
    }

    copy_updates = {
        "demands": [],
        "creates": [],
        "resource_durations": None,
        "scheduler_events": {},
        "module_events": {},
        "task_durations": None,
        "created_at": None,
        "started_at": None,
    }
    assert task_log.task_info.copy(update=copy_updates) == task_log_manager.TaskInfo(
        name="task_name",
        garden_task_id="1",
        predicted_consumption={"cpu": 1, "ssd": 0},
        module_name="test_module",
        build_id=10,
        contour_name=CONTOUR_NAME,
        module_version="1234",
        creates=[],
        demands=[],
        insert_traceback="some traceback",
        retry_attempt=1,
        main_operation_id="vanilla_operation_id",
        module_events={},
    )


@pytest.mark.freeze_time(NOW)
def test_task_log_with_error(db, task_log_collection):
    manager = task_log_manager.TaskLogManager(db)
    manager.save_log(
        yt_task=_make_yt_task(
            started_at=(NOW - dt.timedelta(seconds=10)),
            finished_at=NOW,
        ),
        exec_result=TaskExecutionResult(
            error=RuntimeError("exception message"),
        ),
    )
    saved_task_logs = [task_log_manager.TaskLogRecord.parse_obj(r) for r in task_log_collection.find()]
    assert not saved_task_logs[0].task_info.creates
    assert saved_task_logs[0].task_info.error == "exception message"


@pytest.mark.freeze_time(NOW)
def test_finished_task_statistics(db):

    manager = task_log_manager.TaskLogManager(db)
    task_key = "5f453a30259b8722ad5df682"
    _save_log_without_created_resources(manager, task_key=task_key)
    _save_log_with_created_resources(manager, task_key=task_key)
    manager.save_normalized_tasks(manager.get_task_logs())
    task_stats_list = [x for x in manager.get_task_statistics("test_module", 10)]
    assert len(task_stats_list) == 1
    task_stats = task_stats_list[0]
    assert len([x for x in manager.get_task_statistics_by_keys([task_stats.mongo_id], "test_module")]) == 1

    assert len(task_stats.creates) == 1
    assert len(task_stats.demands) == 1
    assert list(task_stats.resource_durations.key_to_preparation.values()) == [dt.timedelta(seconds=2)]
    assert list(task_stats.resource_durations.key_to_commit.values()) == [dt.timedelta(seconds=3)]

    assert task_stats.scheduler_events == {
        SchedulerTaskEvent.READY_FOR_EXECUTION: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.SCHEDULING_STARTED: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.OPERATION_CREATED: (NOW - dt.timedelta(seconds=10)),
        SchedulerTaskEvent.FINISHED: NOW,
    }

    assert task_stats.module_events == MODULE_EVENTS

    assert not task_stats.task_durations

    update = {
        "demands": [],
        "creates": [],
        "resource_durations": None,
        "scheduler_events": {},
        "module_events": {},
        "task_durations": None,
        "created_at": None,
        "started_at": None,
        "finished_at": None,
    }
    assert task_stats.copy(update=update) == task_log_manager.TaskStatisticsRecord(
        mongo_id=ObjectId(task_key),
        main_operation_id="vanilla_operation_id",
        name="task_name",
        garden_task_id="1",
        predicted_consumption={"cpu": 1, "ssd": 0},
        module_name="test_module",
        build_id=10,
        contour_name=CONTOUR_NAME,
        module_version="1234",
        creates=[],
        demands=[],
        insert_traceback="some traceback",
        retry_attempt=1,
        log_url="http://localhost/bucket-for-logs/log_file_basename",
        error=None,
        task_durations=None,
        resource_durations=None,
    )


@pytest.mark.freeze_time(NOW)
def test_in_progress_task_statistics(db):

    manager = task_log_manager.TaskLogManager(db)
    task_key = "5f453a30259b8722ad5df682"
    _save_log_without_created_resources(manager, task_key=task_key)
    manager.save_normalized_tasks(manager.get_task_logs())
    task_stats_list = [x for x in manager.get_task_statistics("test_module", 10)]
    assert len(task_stats_list) == 1
    task_stats = task_stats_list[0]

    assert len(task_stats.demands) == 1

    assert task_stats.scheduler_events == {
        SchedulerTaskEvent.READY_FOR_EXECUTION: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.SCHEDULING_STARTED: (NOW - dt.timedelta(seconds=11)),
        SchedulerTaskEvent.OPERATION_CREATED: (NOW - dt.timedelta(seconds=10)),
    }

    assert task_stats.module_events == {
        ModuleTaskEvent.STARTED: (NOW - dt.timedelta(seconds=10)),
    }

    update = {
        "demands": [],
        "resource_durations": None,
        "scheduler_events": {},
        "module_events": {},
        "task_durations": None,
        "created_at": None,
        "started_at": None,
    }
    assert task_stats.copy(update=update) == task_log_manager.TaskStatisticsRecord(
        mongo_id=ObjectId(task_key),
        main_operation_id="vanilla_operation_id",
        name="task_name",
        garden_task_id="1",
        predicted_consumption={"cpu": 1, "ssd": 0},
        module_name="test_module",
        build_id=10,
        contour_name=CONTOUR_NAME,
        module_version="1234",
        creates=[],
        demands=[],
        insert_traceback="some traceback",
        retry_attempt=1,
        log_url=None,
        error=None,
        task_durations=None,
        resource_durations=None,
    )


@pytest.mark.freeze_time(NOW)
def test_resource_statistics(db):

    manager = task_log_manager.TaskLogManager(db)
    _save_log_with_created_resources(manager)
    manager.save_normalized_tasks(manager.get_task_logs())

    task_stats_list = [x for x in manager.get_task_statistics("test_module", 10)]
    assert len(task_stats_list) == 1
    task_key = task_stats_list[0].mongo_id

    resource_stats_list = sorted(
        (x for x in manager.get_resource_statistics("test_module", 10)),
        key=lambda r: r.name
    )
    assert len(resource_stats_list) == 2

    creates_resource_key = resource_stats_list[0].key
    demands_resource_key = resource_stats_list[1].key

    creates_resource = task_log_manager.ResourceInfoRecord(
        name="creates_resource",
        key=creates_resource_key,
        class_name="PythonResource",
        uri=None,
        size={},
        properties={"date": "dateB"},
        create_task_keys={task_key},
        created_at=NOW,
        contour_name=CONTOUR_NAME,
        tags={"test_module:10"}
    )
    demands_resource = task_log_manager.ResourceInfoRecord(
        name="demands_resource",
        key=demands_resource_key,
        class_name="PythonResource",
        uri=None,
        size={},
        properties={"date": "dateA"},
        create_task_keys=set(),
        tags={"test_module:10"}
    )
    assert resource_stats_list == [creates_resource, demands_resource]

    manager._resource_statistics_storage.tag_resources("test_module:11", [demands_resource_key])
    resource_stats_list = sorted(
        (x for x in manager.get_resource_statistics("test_module", 10)),
        key=lambda r: r.name
    )
    demands_resource.tags.add("test_module:11")
    assert resource_stats_list == [creates_resource, demands_resource]

    new_task_key = ObjectId()
    manager._resource_statistics_storage.upsert_many([
        task_log_manager.ResourceInfoRecord(
            name="creates_resource",
            key=creates_resource_key,
            class_name="PythonResource",
            uri="uri",
            size={},
            properties={"date": "dateB"},
            create_task_keys={new_task_key},
            created_at=NOW,
            contour_name=CONTOUR_NAME,
            tags={"test_module:11"}
        ),
        task_log_manager.ResourceInfoRecord(
            name="demands_resource",
            key=demands_resource_key,
            class_name="PythonResource",
            uri=None,
            size={},
            created_at=NOW,
            contour_name=CONTOUR_NAME,
            properties={"date": "dateA"},
            create_task_keys=set(),
            tags={"test_module:10"}
        ),
    ])
    resource_stats_list = sorted(
        (x for x in manager.get_resource_statistics("test_module", 10)),
        key=lambda r: r.name
    )
    creates_resource.create_task_keys.add(new_task_key)
    creates_resource.tags.add("test_module:11")
    creates_resource.uri = "uri"
    assert resource_stats_list == [creates_resource, demands_resource]


@pytest.mark.freeze_time(NOW)
def test_max_task_log_age(db, task_log_collection):
    manager = task_log_manager.TaskLogManager(db)
    assert not manager.get_max_task_log_age()

    task_log_collection.insert_many([
        {"_id": ObjectId("5f3a999cfb30a5ada39eb429")},
        {"_id": ObjectId("5f3a9a45fb30a5ada39eb480")}
    ])
    assert manager.get_max_task_log_age() == dt.timedelta(days=8, seconds=5268)


def _make_demands():
    resource = PythonResource("demands_resource")
    resource.version = Version(properties={"date": "dateA"})
    return {
        "demands_resource": resource
    }


def _make_creates():
    resource = PythonResource("creates_resource")
    resource.version = Version(properties={"date": "dateB"})
    return {
        "creates_resource": resource
    }


def _make_yt_task(
    is_finished: bool = True,
    task_key: str = None,
    started_at: tp.Optional[dt.datetime] = None,
    finished_at: tp.Optional[dt.datetime] = None,
):
    task = mock.Mock(
        displayed_name="task_name",
        insert_traceback="some traceback",
    )

    versioned_task = mock.Mock(
        request_ids=[1, 2],
        task=task,
        contour_name=CONTOUR_NAME,
        module_name="test_module",
        module_version="1234",
        retries=1,
        ready_for_execution_at=(NOW - dt.timedelta(seconds=11))
    )

    task_key = task_key if task_key else str(ObjectId())
    task = YtTask(
        task_id=1,
        contour_name=CONTOUR_NAME,
        module_name=versioned_task.module_name,
        build_id=10,
        task_key=task_key,
        consumption={"cpu": 1, "ssd": 0},
        log_file_basename="log_file_basename",
        versioned_task=versioned_task,
        operation_id="vanilla_operation_id",
        demands=_make_demands(),
        creates=_make_creates() if is_finished else [],
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
    task.register_event(SchedulerTaskEvent.SCHEDULING_STARTED, (NOW - dt.timedelta(seconds=11)))
    if started_at:
        task.register_event(SchedulerTaskEvent.OPERATION_CREATED, started_at)
    if finished_at:
        task.register_event(SchedulerTaskEvent.FINISHED, finished_at)
    return task


def _save_log_with_created_resources(manager, task_key: str = None):
    creates_resources = _make_creates()
    for resource in creates_resources.values():
        resource.contour_name = CONTOUR_NAME
        resource.created_at = NOW
    manager.save_log(
        yt_task=_make_yt_task(
            task_key=task_key,
            started_at=(NOW - dt.timedelta(seconds=10)),
            finished_at=NOW,
        ),
        exec_result=TaskExecutionResult(
            creates=creates_resources,
            execution_info=TaskExecutionInfo(
                module_events=MODULE_EVENTS,
                prepare_durations=[
                    ResourceTiming(name="demands_resource", duration=dt.timedelta(seconds=2)),
                ],
                commit_durations=[
                    ResourceTiming(name="creates_resource", duration=dt.timedelta(seconds=3)),
                ],
            )
        ),
    )


def _save_log_without_created_resources(manager, task_key: str = None):
    manager.save_log(
        yt_task=_make_yt_task(
            is_finished=False,
            task_key=task_key,
            started_at=(NOW - dt.timedelta(seconds=10)),
        ),
    )
