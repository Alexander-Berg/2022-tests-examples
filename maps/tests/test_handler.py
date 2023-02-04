import logging
import pytest
import time
from datetime import datetime, timedelta
from unittest import mock

import yt.wrapper as yt

from maps.pylibs.utils.lib import common

from maps.garden.sdk import core
from maps.garden.sdk.utils import MB
from maps.garden.sdk.utils.decorators import time_measurer

from maps.garden.libs_server.common.exceptions import (
    ModuleOperationException, ModuleOperationCrash, TaskError)
from maps.garden.libs_server.graph.versioned_task import VersionedTask
from maps.garden.libs_server.common.scheduler import Scheduler, SchedulerAdaptor
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.task.task_storage import TaskInvocationStatus, TaskStorage
from maps.garden.libs_server.yt_task_handler.pymod.adaptor import YtHandlerAdaptor
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple, ModuleGraphManager
from maps.garden.libs_server.yt_task_handler.pymod.utils import YtOperationStartError  # noqa

from .conftest import TEST_MODULE_YT_PATH

logger = logging.getLogger("test_handler")

TIMEOUT = 160

# Statistically, each vanilla operation from the tests in this module eats 130M of RAM in average.
# Adjust the predictions accordingly.
_DEFAULT_PREDICT_CONSUMPTION = {"ram": 200 * MB}

TEST_INVOCATION_STATUS_TO_CHECK_TIME = {
    TaskInvocationStatus.RUNNING: timedelta(seconds=60),
    TaskInvocationStatus.UNKNOWN: timedelta(seconds=60)
}


@pytest.fixture
def scheduler():
    scheduler = Scheduler()
    scheduler.start()
    yield scheduler
    scheduler.stop()


class FakeTask(core.Task):
    """
    Passes `input_data` to YT handler which writes it to Cypress.
    Then `input_data` is streamed to stdin of binary module.

    NB: this class is a counterpart of _IsolatedTask from garden server
    """
    def __init__(self, input_data, predict_consumption):
        super().__init__()
        self.input_data = input_data
        self.module_name = "test_module"
        self._predict_consumption = predict_consumption if predict_consumption else _DEFAULT_PREDICT_CONSUMPTION

    def __call__(self, *args, **kwargs):
        """It should not be called"""
        assert False

    @property
    def remote_path(self):
        return TEST_MODULE_YT_PATH

    def make_task_call_stdin(self, demands, creates, env):
        return self.input_data

    def predict_consumption(self, demands, creates):
        return self._predict_consumption


class Helper:
    def __init__(
        self,
        db,
        server_settings: dict,
        environment_settings: dict,
        scheduler: Scheduler,
    ):
        self._db = db
        self._server_settigs = server_settings
        self._environment_settings = environment_settings
        self._scheduler = scheduler
        self._task_storage = TaskStorage(db)

        self.task_monitor = mock.Mock()

        yt_config = server_settings["yt"]["config"]
        self._yt_client = yt.YtClient(config=yt_config)

        # clear_cypress_state
        self._yt_client.remove("//tmp/one_try", force=True)
        self._yt_client.remove("//tmp/stop_work", force=True)

    @property
    def yt_client(self):
        return self._yt_client

    @staticmethod
    def create_versioned_task(input_data: str, predict_consumption: dict[str, any] = None) -> VersionedTask:
        task = VersionedTask(
            FakeTask(input_data=input_data, predict_consumption=predict_consumption),
            demands=core.Demands(),
            creates=core.Creates(),
            contour_name="contour_name")
        task.module_name = "test_module"
        task.build_id = 1
        task._hash = hash(input_data)
        return task

    def create_yt_adaptor(self) -> YtHandlerAdaptor:
        environment_settings_provider = EnvironmentSettingsProviderSimple(self._environment_settings)

        return YtHandlerAdaptor(
            db=self._db,
            server_settings=self._server_settigs,
            environment_settings_provider=environment_settings_provider,
            resource_storage=ResourceStorage(self._db, environment_settings_provider),
            graph_manager=ModuleGraphManager(core.TaskGraphBuilder(), "contour_name"),
            task_monitor=self.task_monitor,
            delay_executor=SchedulerAdaptor(self._scheduler),
        )

    @time_measurer(logger)
    def run_task_and_wait_running(self, task: VersionedTask):
        with self.create_yt_adaptor() as yt_adaptor:
            yt_adaptor.execute_if_has_slot(task, input_resources={})

            assert common.wait_until(
                lambda: self.has_operations_in_yt(task, state="running"),
                timeout=TIMEOUT
            )

    @time_measurer(logger)
    def run_task_and_wait_finished(self, task: VersionedTask) -> list[tuple[VersionedTask, Exception]]:
        with self.create_yt_adaptor() as yt_adaptor:
            yt_adaptor.execute_if_has_slot(task, input_resources={})
            result = self.wait_finished(yt_adaptor)
            return result

    @time_measurer(logger)
    def wait_finished(self, yt_adaptor: YtHandlerAdaptor) -> list[tuple[VersionedTask, Exception]]:
        finish_waiting_time = time.time() + TIMEOUT

        while time.time() < finish_waiting_time:
            finished_tasks = yt_adaptor.pop_finished()
            if finished_tasks:
                return finished_tasks
            time.sleep(1)
        return []

    def len_tasks_in_mongo(self) -> int:
        return len(self.get_tasks_in_mongo())

    def get_tasks_in_mongo(self) -> list[dict[str, any]]:
        tasks = self._task_storage.find_all()
        return list(tasks)

    def find_operations_in_yt(self, task: VersionedTask, state: str) -> list[dict[str, any]]:
        result = self._yt_client.list_operations(
            filter=str(task.task_id()),
            state=state,
            from_time=datetime.utcnow() - timedelta(hours=1),
            to_time=datetime.utcnow(),
            include_archive=True,
            include_counters=False)

        return [
            op
            for op in result["operations"]
            if op["state"] == state
        ]

    def has_operations_in_yt(self, task: VersionedTask, state: str) -> bool:
        operations = self.find_operations_in_yt(task, state)
        return len(operations) > 0

    def stop_long_running_operation(self):
        self._yt_client.create("map_node", "//tmp/stop_work")


@pytest.fixture
def helper(mocker, db, server_config, environment_settings, scheduler):
    mocker.patch.dict(
        "maps.garden.libs_server.yt_task_handler.pymod.constants.INVOCATION_STATUS_TO_CHECK_TIME",
        TEST_INVOCATION_STATUS_TO_CHECK_TIME
    )
    return Helper(db, server_config, environment_settings, scheduler)


# TODO: When garden will use several jobs in one vanilla operation, make more tests with several tasks in one operation.
@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_success")
def test_success(instance_mock, helper):
    """
    * Run a task and wait until it is finished
    * Expect: one finished task without exception
    * Restart server
    * Try to run the same task with another logging_tab
    * Expect: one new finished task without exception and two records in mongo
    """
    task = helper.create_versioned_task(input_data="good input 1")

    finished_tasks_1 = helper.run_task_and_wait_finished(task)
    exception = finished_tasks_1[0][1]
    assert len(finished_tasks_1) == 1
    assert exception is None

    # Emulate server restart: try to run the same task again with another build_id

    task.build_id = 2

    finished_tasks_2 = helper.run_task_and_wait_finished(task)
    exception = finished_tasks_2[0][1]
    assert len(finished_tasks_2) == 1
    assert exception is None
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_exceptions_from_task")
def test_exceptions_from_task(instance_mock, helper):
    """
    * Run a task and wait until it is finished with an exception
    * Expect: one finished task with exception
    * Restart server
    * Try to run the same task
    * Expect: one finished task with exception and two records in mongo
    """
    task = helper.create_versioned_task(input_data="bad input 2")

    finished_tasks_1 = helper.run_task_and_wait_finished(task)

    assert len(finished_tasks_1) == 1
    exception = finished_tasks_1[0][1]
    assert isinstance(exception, ModuleOperationException)
    assert "Wrong input_data" in exception.original_message()

    # Emulate server restart: try to run the same task again with another build_id

    task.build_id = 2

    finished_tasks_2 = helper.run_task_and_wait_finished(task)
    exception = finished_tasks_2[0][1]
    assert isinstance(exception, ModuleOperationException)
    assert "Wrong input_data" in exception.original_message()

    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_unexpected_exit")
def test_unexpected_exit(instance_mock, helper):
    """
    * Run a task that exits unexpectedly (exit_code = 1)
    * Exit code from the task is ignored. Exit code from the operation as a whole is always 0.
    * Expect: one finished task with exit_code = 1
    * Restart server
    * Try to run the same task
    * Expect: one finished task with exit_code = 1 and two records in mongo
    """
    task = helper.create_versioned_task(input_data="force exit 3")

    finished_tasks_1 = helper.run_task_and_wait_finished(task)

    assert len(finished_tasks_1) == 1

    exception = finished_tasks_1[0][1]
    assert isinstance(exception, TaskError)
    assert isinstance(exception.exception, ModuleOperationCrash)

    # Emulate server restart: try to run the same task again

    finished_tasks_2 = helper.run_task_and_wait_finished(task)

    assert len(finished_tasks_2) == 1

    exception = finished_tasks_2[0][1]
    assert isinstance(exception, TaskError)
    assert isinstance(exception.exception, ModuleOperationCrash)

    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_operation_failed")
def test_operation_failed(instance_mock, helper):
    """
    * Run a task
    * Emulate operation failure by external reason (by aborting it)
    * Expect: two records in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then success 4")

    with helper.create_yt_adaptor() as yt_adaptor:
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="running"), timeout=TIMEOUT)

        operations = helper.find_operations_in_yt(task, state="running")

        # Emulate some external event that leads to operation failure
        yt.operation_commands.abort_operation(operations[0]["id"], client=helper.yt_client)

        assert common.wait_until(lambda: helper.len_tasks_in_mongo() == 2, timeout=TIMEOUT)

        helper.stop_long_running_operation()

        finished_tasks = helper.wait_finished(yt_adaptor)

        assert len(finished_tasks) == 1
        exception = finished_tasks[0][1]
        assert exception is None, str(exception)


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_infinite_retries")
def test_infinite_retries(instance_mock, helper, environment_settings):
    """
    * Run a task and wait until it is finished with RetryTaskError
    * Expect: two records in mongo
    * Restart server
    * Try to run the same task
    * Expect: four records in mongo
    * YtHandler use retry policy from config
    """
    retries_count = environment_settings["error_retry_policy"]["try_number"]
    assert retries_count == 2  # it has to be mocked previously
    task = helper.create_versioned_task(input_data="retry always 5")

    finished_tasks = helper.run_task_and_wait_finished(task)

    assert len(finished_tasks) == 1

    exception = finished_tasks[0][1]
    assert isinstance(exception, ModuleOperationException)
    assert exception.original_classname() == "RetryTaskError"
    assert helper.len_tasks_in_mongo() == retries_count

    # Emulate server restart: try to run the same task again
    task.retries = 0

    helper.run_task_and_wait_finished(task)

    assert helper.len_tasks_in_mongo() == 2 * retries_count


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_one_retry")
def test_one_retry(instance_mock, helper):
    """
    * Run a task and wait until it is finished
    * Expect: two records in mongo
    """
    task = helper.create_versioned_task(input_data="retry once 6")
    finished_tasks = helper.run_task_and_wait_finished(task)
    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None, str(exception)
    assert helper.yt_client.exists("//tmp/one_try")  # test_executor has to create it in first try
    # one failed try and one successful try
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch(
    "maps.garden.libs_server.yt_task_handler.pymod.handler.instance",
    return_value="test_long_working_operation_case1"
)
def test_long_working_operation_case1(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Restart server
    * Wait until the operation is finished
    * Try to run the same task
    * Expect: one records in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then success 7")

    helper.run_task_and_wait_running(task)

    # Emulate server restart
    with helper.create_yt_adaptor() as yt_adaptor:
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        helper.stop_long_running_operation()

        # Try to run the same task again
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        finished_tasks = helper.wait_finished(yt_adaptor)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 1


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch(
    "maps.garden.libs_server.yt_task_handler.pymod.handler.instance",
    return_value="test_long_working_operation_case2"
)
def test_long_working_operation_case2(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Restart server
    * Try to run the same task
    * Wait until the operation is finished
    * Expect: one records in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then success 8")

    helper.run_task_and_wait_running(task)

    helper.task_monitor.notify_running.reset_mock()

    # Emulate server restart
    with helper.create_yt_adaptor() as yt_adaptor:
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        # Try to run the same task again
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        assert common.wait_until(
            lambda: helper.task_monitor.notify_running.call_count == 1,
            timeout=TIMEOUT
        )

        helper.stop_long_running_operation()

        finished_tasks = helper.wait_finished(yt_adaptor)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 1


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch(
    "maps.garden.libs_server.yt_task_handler.pymod.handler.instance",
    return_value="test_long_working_operation_retry_once_case1"
)
# TODO: Update this test after MAPSGARDEN-20544
def test_long_working_operation_retry_once_case1(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Restart server
    * Wait until the operation is finished with RetryTaskError in Mongo
    * Try to run the same task
    * Wait until the new operation is finished
    * Expect: two records in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then retry 9")

    helper.run_task_and_wait_running(task)

    # Emulate server restart
    with helper.create_yt_adaptor() as yt_adaptor:
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        helper.stop_long_running_operation()

        # Try to run the same task again
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        finished_tasks = helper.wait_finished(yt_adaptor)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch(
    "maps.garden.libs_server.yt_task_handler.pymod.handler.instance",
    return_value="test_long_working_operation_retry_once_case2"
)
# TODO: Update this test after MAPSGARDEN-20544
def test_long_working_operation_retry_once_case2(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Restart server
    * Try to run the same task
    * Wait until the new operation is finished
    * Expect: two records in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then retry 10")

    helper.run_task_and_wait_running(task)

    helper.task_monitor.notify_running.reset_mock()

    # Emulate server restart
    with helper.create_yt_adaptor() as yt_adaptor:
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        # Try to run the same task again
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        assert common.wait_until(
            lambda: helper.task_monitor.notify_running.call_count == 1,
            timeout=TIMEOUT
        )

        helper.stop_long_running_operation()

        finished_tasks = helper.wait_finished(yt_adaptor)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_abort_operation_case1")
def test_abort_operation_case1(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Cancel the task
    * Expect: one aborted operation in YT
    * Try to run the same task
    * Wait until the new operation is finished
    * Expect: two record in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then success 11")

    with helper.create_yt_adaptor() as yt_adaptor:
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="running"), timeout=TIMEOUT)

        yt_adaptor.remove_tasks([task])

        assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="aborted"), timeout=TIMEOUT)
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 0, timeout=TIMEOUT)

        # Fire up a flag to speed up the second run
        helper.stop_long_running_operation()

        # Try to run the same task again
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        finished_tasks = helper.wait_finished(yt_adaptor)

        assert len(finished_tasks) == 1
        exception = finished_tasks[0][1]
        assert exception is None
        assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_abort_operation_case2")
def test_abort_operation_case2(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Cancel the task
    * Expect: one aborted operation in YT
    * Restart server
    * Try to run the same task
    * Expect: two record in mongo
    """
    task = helper.create_versioned_task(input_data="work until condition then success 12")

    with helper.create_yt_adaptor() as yt_adaptor:
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="running"), timeout=TIMEOUT)

        yt_adaptor.remove_tasks([task])

        assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="aborted"), timeout=TIMEOUT)

    # Fire up a flag to speed up the second run
    helper.stop_long_running_operation()

    # Emulate server restart: try to run the same task again
    finished_tasks = helper.run_task_and_wait_finished(task)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch(
    "maps.garden.libs_server.yt_task_handler.pymod.handler.instance",
    return_value="test_update_resource_consumption_on_task_restart"
)
def test_update_resource_consumption_on_task_restart(instance_mock, helper):
    """
    * Run a task and wait until it is started
    * Expect: one running operation in YT
    * Restart server
    * Wait until the operation is finished with RetryTaskError pickled in Cypress
    * Try to run the same task with new resource consumption
    * Wait until the new operation is finished
    * Expect: two record in mongo
    """
    task_input_data = "work until condition then retry 13"
    task = helper.create_versioned_task(input_data=task_input_data)

    helper.run_task_and_wait_running(task)

    # Emulate server restart
    with helper.create_yt_adaptor() as yt_adaptor:
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        # Try to run the same task again with new resource consumption
        new_consumption = dict(_DEFAULT_PREDICT_CONSUMPTION)
        new_consumption["ram"] *= 2
        task = helper.create_versioned_task(input_data=task_input_data, predict_consumption=new_consumption)
        yt_adaptor.execute_if_has_slot(task, input_resources={})

        helper.stop_long_running_operation()

        assert common.wait_until(lambda: len(yt_adaptor._handler._restarting_tasks) == 1, timeout=TIMEOUT)
        assert common.wait_until(lambda: len(yt_adaptor._handler._running_tasks) == 1, timeout=TIMEOUT)

        finished_tasks = helper.wait_finished(yt_adaptor)

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert exception is None
    assert helper.len_tasks_in_mongo() == 2


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_failed_task_start")
def test_failed_task_start(instanse_mock, helper, environment_settings):
    """
    * Fail to start a task operation
    """
    class TestAdaptedTask:
        def __init__(self, task, error):
            self.task_id = task.task_id()
            self.operation_id = None
            self.error = error

    mock.patch.dict(
        TEST_INVOCATION_STATUS_TO_CHECK_TIME,
        {
            TaskInvocationStatus.RUNNING: timedelta(seconds=30),
            TaskInvocationStatus.UNKNOWN: timedelta(seconds=30)
        }
    )

    retries_count = environment_settings["error_retry_policy"]["try_number"]

    # some problem on YT
    task = helper.create_versioned_task(input_data="good input 14")

    runner_mock = mock.Mock()
    runner_mock.pop_started_tasks = mock.Mock(return_value=[TestAdaptedTask(task, "some_error")])

    with mock.patch(
        "maps.garden.libs_server.yt_task_handler.pymod.task_tracker.PyTaskRunner",
        return_value=runner_mock
    ):
        finished_tasks = helper.run_task_and_wait_finished(task)
        assert runner_mock.enqueue_task_start.called
        assert runner_mock.pop_started_tasks.called

    assert len(finished_tasks) == 1
    exception = finished_tasks[0][1]
    assert isinstance(exception, TaskError)
    assert isinstance(exception.exception, YtOperationStartError)
    assert finished_tasks[0][0].retries == retries_count


@pytest.mark.use_local_yt("hahn")
@pytest.mark.usefixtures("upload_files_to_cypress")
@pytest.mark.use_local_mongo
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.handler.instance", return_value="test_long_job_starting")
def test_long_job_starting(instance_mock, helper, db):
    """
    * Operation was started, but job is still not.
    * Need to wait job start more than TaskInvocationStatus.UNKNOWN but less than MAX_WAIT_TIME_OF_STARTING_YT_JOB
    """
    class LocalHelper:
        def __init__(self):
            self.yt_task_tracker_called = False

        def get_task_id_to_jobs_mock(self, operation_id):
            self.yt_task_tracker_called = True
            # simulate situation when operation was created but job was not created
            return {}

    local_helper = LocalHelper()

    with helper.create_yt_adaptor() as yt_adaptor:
        task_input_data = "silent until condition then success 15"
        task = helper.create_versioned_task(input_data=task_input_data)

        with mock.patch(
            "maps.garden.libs_server.yt_task_handler.pymod.yt_task_tracker.YtTaskTracker._get_task_id_to_jobs",
            side_effect=local_helper.get_task_id_to_jobs_mock
        ):
            yt_adaptor.execute_if_has_slot(task, input_resources={})
            # wait until yt_task_tracker check operation
            assert common.wait_until(lambda: helper.has_operations_in_yt(task, state="running"), timeout=TIMEOUT)
            assert common.wait_until(lambda: local_helper.yt_task_tracker_called, timeout=TIMEOUT)
            helper.stop_long_running_operation()

            finished_tasks = helper.wait_finished(yt_adaptor)

        assert len(finished_tasks) == 1
        exception = finished_tasks[0][1]
        assert exception is None
        assert helper.len_tasks_in_mongo() == 1
