import logging
import time
import threading
import pytest

from maps.garden.sdk.core import Task, Demands, Creates, GardenError

from maps.garden.sdk.test_utils.internal.resource_storage import UnittestResourceStorage
from maps.garden.sdk.test_utils.internal.task_handler import UnittestTaskHandler, TaskNode, TaskError

logger = logging.getLogger("garden")

TIMEOUT = 10


class EmptyTask(Task):
    def __call__(self):
        pass


class ErrorTask(Task):
    def __call__(self):
        raise GardenError("Foo")


class PredictConsumptionErrorTask(Task):
    def predict_consumption(self, demands, creates):
        raise GardenError("Bar")


@pytest.fixture
def task_handler(environment_settings):
    resource_storage = UnittestResourceStorage(environment_settings)
    finished_condition = threading.Condition()

    with UnittestTaskHandler(resource_storage, finished_condition) as task_handler:
        yield task_handler


def _execute_task(task_handler, task):
    task_node = TaskNode(
        task=task,
        demand_versions=Demands(),
        create_resources=Creates(),
        demands_resource_names=set(),
    )

    task_handler.schedule(task_node, demand_resources=Demands())

    result = _wait_finished(task_handler)

    assert len(result) == 1
    result_task, result_error = result[0]
    assert result_task == task_node
    return result_error


def _wait_finished(task_handler):
    finish_waiting_time = time.time() + TIMEOUT

    with task_handler.finished_condition:
        while time.time() < finish_waiting_time:
            task_handler.finished_condition.wait(finish_waiting_time - time.time())
            finished_tasks = task_handler.pop_finished()
            if finished_tasks:
                return finished_tasks
    return []


def test_task_invocation_success(task_handler):
    task = EmptyTask()

    result_error = _execute_task(task_handler, task)

    assert result_error is None, f"{result_error=}"


def test_task_invocation_exception(task_handler):
    task = ErrorTask()

    result_error = _execute_task(task_handler, task)

    assert isinstance(result_error, TaskError)
    assert isinstance(result_error.original_exception, GardenError)
    assert "Foo" in str(result_error.original_exception)


def test_predict_consumption_error(task_handler):
    task = PredictConsumptionErrorTask()

    result_error = _execute_task(task_handler, task)

    assert isinstance(result_error, TaskError)
    assert isinstance(result_error.original_exception, GardenError)
    assert "Bar" in str(result_error.original_exception)
