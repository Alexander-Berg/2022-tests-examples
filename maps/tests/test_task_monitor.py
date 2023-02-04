from datetime import datetime

from maps.garden.libs_server.common.scheduler import SchedulerAdaptor
from maps.garden.libs_server.graph.versioned_task import VersionedTask
from maps.garden.libs_server.common.task_monitor_storage import (
    MonitorTaskInfo, MonitoredTaskStatus, TaskMonitorStorage
)
from maps.garden.scheduler.lib.task_monitor import TaskMonitor


class _Task:
    displayed_name = "name"

    def pretty_str(self):
        return "_Task"


def test_task_monitor(db, mocker, scheduler_mock, thread_executor_mock):
    task_monitor_storage = TaskMonitorStorage(db)
    delay_executor = SchedulerAdaptor(scheduler_mock)

    task_monitor = TaskMonitor(db, delay_executor)

    versioned_task = VersionedTask(_Task(), {}, {})
    versioned_task.contour_name = "test_contour_name"
    versioned_task.module_name = "test_module_name"
    versioned_task.build_id = 1
    task = MonitorTaskInfo(versioned_task=versioned_task)

    with task_monitor:
        task_monitor.notify_waiting(task)
        task_monitor.notify_running(task)

        scheduler_mock.execute_background_task()

        tasks = task_monitor_storage.find_tasks()
        assert len(tasks) == 1
        task = tasks[0]
        assert task.status == MonitoredTaskStatus.RUNNING
        assert task.task_name == "name"
        assert task.details == "_Task"
        assert isinstance(task.added_at, datetime)
