"""Tests Host finite-state machine."""

import time
import uuid
from collections import defaultdict
from unittest import mock

import pytest

import walle.fsm_stages.common
import walle.host_fsm
import walle.locks
import walle.operations_log.operations as operations_log
from infra.walle.server.tests.lib import util
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    monkeypatch_locks,
    handle_host,
    mock_task_initialization,
    mock_status_reasons,
    mock_task_completion,
    any_task_status,
    AUDIT_LOG_ID,
)
from sepelib.core.constants import DAY_SECONDS
from walle import authorization
from walle.application import app
from walle.expert import juggler
from walle.fsm_stages.common import register_stage, get_current_stage, complete_current_stage
from walle.host_fsm import fsm, handbrake
from walle.host_fsm.control import SHARDS_NUM_PATH, MAX_CONCURRENCY
from walle.host_fsm.fsm import HostFsm
from walle.hosts import Host, HostState, HostStatus, Task, TaskType
from walle.models import timestamp, monkeypatch_timestamp, FsmHandbrake
from walle.operations_log.constants import Operation
from walle.stages import Stage, Stages
from walle.util import mongo
from walle.util.limits import CheckResult
from walle.clients import dmc
from walle.expert.decision import Decision
from walle.expert.types import WalleAction


@pytest.yield_fixture(autouse=True)
def tier_2():
    yield from util.tier_2()


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request, healthdb=True)


@pytest.fixture
def monkeypatch_health_reasons(mp):
    reasons = mock_status_reasons()
    mp.function(juggler.get_host_health_reasons, return_value=reasons)


def test_task_initialization(test, mp):
    mp.function(
        dmc.get_decisions_from_handler,
        return_value=(
            None,
            Decision(action=WalleAction.FAILURE, reason='Mock.', restrictions=[]),
        ),
    )
    handler = mock.Mock()
    initial_status = "initial-status-mock"
    stage = Stage(name="test-task-initialization-first-stage")
    register_stage(stage.name, handler, initial_status=initial_status)

    host = test.mock_host({"task": mock_task(stages=[stage, Stage(name="invalid")])})
    assert host.task.stage_uid is None

    handle_host(host)

    mock_task_initialization(host, stage, initial_status=initial_status)
    handler.assert_called_once_with(host)

    test.hosts.assert_equal()


def mock_host_with_completed_task(
    test, target_status=HostStatus.default(HostState.MAINTENANCE), host_cms_task_id=None, **kwargs
):
    status_reason = "powering off the host just because we can"
    return test.mock_host(
        {
            "status_reason": status_reason,
            "cms_task_id": host_cms_task_id,
            "task": mock_task(
                target_status=target_status, type=TaskType.MANUAL, stage="test-task-completion-stage", **kwargs
            ),
        }
    )


def test_task_completion(test):
    host = mock_host_with_completed_task(test)

    fsm.common.complete_task(host.copy())
    mock_task_completion(host, status=host.task.target_status, owner=host.task.owner, status_reason=host.status_reason)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "host_task_kwargs",
    [
        # these variations should not affect the result: downtime should be kept
        dict(host_cms_task_id="mock-cms-task-id"),  # keep cms task
        dict(ignore_cms=True),  # do not create cms task in the first place
        dict(host_cms_task_id="mock-cms-task-id", ignore_cms=True),  # both of the above
    ],
)
def test_task_completion_with_keep_downtime(test, host_task_kwargs):
    host = mock_host_with_completed_task(test, keep_downtime=True, **host_task_kwargs)

    fsm.common.complete_task(host.copy())
    mock_task_completion(
        host, downtime=True, status=host.task.target_status, owner=host.task.owner, status_reason=host.status_reason
    )

    test.hosts.assert_equal()


def test_task_completion_with_retained_cms_task_id(test):
    host = mock_host_with_completed_task(test, host_cms_task_id="mock-cms-task-id")

    fsm.common.complete_task(host.copy())
    mock_task_completion(host, status=host.task.target_status, owner=host.task.owner, status_reason=host.status_reason)

    test.hosts.assert_equal()


def test_task_completion_with_keep_cms_task_id_and_ignore_cms(test):
    host = mock_host_with_completed_task(test, host_cms_task_id="mock-cms-task-id", ignore_cms=True)

    fsm.common.complete_task(host.copy())
    mock_task_completion(host, status=host.task.target_status, owner=host.task.owner, status_reason=host.status_reason)

    test.hosts.assert_equal()


@pytest.mark.parametrize("task_type", TaskType.ALL)
def test_pending_task_cancellation_on_disabled_automation(test, mp, task_type):
    handler = mock.Mock()
    stage = Stage(name="test_pending_task_cancellation_on_disabled_automation-" + task_type)
    register_stage(stage.name, handler)

    host = test.mock_host({"task": mock_task(type=task_type, stages=[stage])})
    assert host.task.stage_uid is None

    mp.config("automation.enabled", False)
    handle_host(host)

    if task_type == TaskType.AUTOMATED_HEALING:
        assert handler.call_count == 0
        host.set_status(HostStatus.READY, authorization.ISSUER_WALLE, host.task.audit_log_id)
        del host.task
    else:
        mock_task_initialization(host, stage)
        handler.assert_called_once_with(host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_health_reasons")
@pytest.mark.parametrize("task_type", TaskType.ALL)
@pytest.mark.parametrize("has_limits", (True, False))
def test_pending_task_cancellation_for_healthy_host(mp, test, task_type, has_limits):
    mp.function(
        dmc.get_decisions_from_handler,
        return_value=(
            None,
            Decision(action=WalleAction.HEALTHY, reason='Mock.', restrictions=[]),
        ),
    )
    on_completed_operation = mp.function(operations_log.on_completed_operation)

    handler = mock.Mock()
    stage = Stage(name="test_pending_task_cancellation_for_healthy_host-" + task_type + ("" if has_limits else "-2"))
    register_stage(stage.name, handler)

    extra_params = {}
    if not has_limits:
        extra_params["host_limits"] = {"max_healing_cancellations": [{"period": "1h", "limit": 0}]}

    project = test.mock_project({"id": "test-project", **extra_params})
    host = test.mock_host({"project": project.id, "task": mock_task(type=task_type, stages=[stage])})
    assert host.task.stage_uid is None

    handle_host(host)

    if task_type == TaskType.AUTOMATED_HEALING and has_limits:
        assert handler.call_count == 0

        host.set_status(HostStatus.READY, authorization.ISSUER_WALLE, host.task.audit_log_id)
        del host.task

        on_completed_operation.assert_called_once_with(host, Operation.CANCEL_HEALING.type)
    else:
        mock_task_initialization(host, stage)
        handler.assert_called_once_with(host)
        assert on_completed_operation.call_count == 0

    test.hosts.assert_equal()


@pytest.mark.parametrize("task_type", TaskType.ALL)
def test_acquire_permission_task_cancellation_on_disabled_automation(test, mp, task_type):
    handler = mock.Mock()
    stage = Stage(name=Stages.ACQUIRE_PERMISSION)
    host = test.mock_host({"task": mock_task(type=task_type, stage=stage, stages=[stage])})

    mp.function(walle.fsm_stages.common.get_stage_handler, return_value=handler)
    mp.config("automation.enabled", False)

    handle_host(host)

    if task_type == TaskType.AUTOMATED_HEALING:
        assert handler.call_count == 0
        host.set_status(HostStatus.READY, authorization.ISSUER_WALLE, host.task.audit_log_id)
        del host.task
    else:
        handler.assert_called_once_with(host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_health_reasons")
@pytest.mark.parametrize("task_type", TaskType.ALL)
@pytest.mark.parametrize(
    "limit_check_result",
    [CheckResult(True), CheckResult(False)],  # _CheckResult.info should not be used so leave it blank
)
def test_acquire_permission_task_cancellation_for_healthy_host(mp, test, task_type, limit_check_result):
    mp.function(
        dmc.get_decisions_from_handler,
        return_value=(
            None,
            Decision(action=WalleAction.HEALTHY, reason='Mock.', restrictions=[]),
        ),
    )
    check_limits = mp.function(operations_log.check_limits, return_value=limit_check_result)
    on_completed_operation = mp.function(operations_log.on_completed_operation)

    handler = mock.Mock()

    stage = Stage(name=Stages.ACQUIRE_PERMISSION)
    host = test.mock_host({"task": mock_task(type=task_type, stage=stage, stages=[stage])})

    mp.function(walle.fsm_stages.common.get_stage_handler, return_value=handler)
    handle_host(host)

    if task_type == TaskType.AUTOMATED_HEALING:
        check_limits.assert_called_once_with(host, Operation.CANCEL_HEALING, mock.ANY)
    else:
        assert check_limits.call_count == 0

    if task_type == TaskType.AUTOMATED_HEALING and limit_check_result:
        assert handler.call_count == 0

        host.set_status(HostStatus.READY, authorization.ISSUER_WALLE, host.task.audit_log_id)
        del host.task

        on_completed_operation.assert_called_once_with(host, Operation.CANCEL_HEALING.type)
    else:
        handler.assert_called_once_with(host)
        assert on_completed_operation.call_count == 0

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_health_reasons")
@pytest.mark.parametrize("task_type", TaskType.ALL)
def test_no_task_cancellation_when_not_needed(test, mp, task_type):
    handler = mock.Mock()
    stage = Stage(name=Stages.ACQUIRE_PERMISSION)

    # The stage will be wrapped with other stages, so it won't be first and mustn't be cancelled
    host = test.mock_host({"task": mock_task(type=task_type, stage=stage)})

    mp.function(walle.fsm_stages.common.get_stage_handler, return_value=handler)
    mp.config("automation.enabled", False)

    handle_host(host)

    handler.assert_called_once_with(host)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("task_type", TaskType.ALL)
def test_no_processing_if_project_fsm_handbrake_is_enabled(test, mp, task_type):
    handler = mock.Mock()
    stage = Stage(name=Stages.ACQUIRE_PERMISSION)

    # The stage will be wrapped with other stages, so it won't be first and mustn't be cancelled
    project = test.mock_project(
        {
            "id": "handbrake",
            "fsm_handbrake": {"issuer": test.api_issuer, "timestamp": timestamp(), "timeout_time": timestamp() + 10},
        }
    )
    host = test.mock_host({"task": mock_task(type=task_type, stage=stage), "project": project.id})

    mp.function(walle.fsm_stages.common.get_stage_handler, return_value=handler)
    mp.config("automation.enabled", False)

    handle_host(host)

    assert not handler.called
    test.hosts.assert_equal()


def test_handling(mp, test):
    handle_log = defaultdict(int)
    uuid_0 = uuid.uuid4().hex
    uuid_1 = uuid.uuid4().hex

    def handle_host(uuid, task_id, **kwargs):
        assert Host.objects(uuid=uuid).get().task.task_id == task_id

        if uuid == uuid_1 and handle_log[uuid] == 0:
            Host.objects(uuid=uuid).modify(set__task__next_check=timestamp() - 1)

        handle_log[uuid] += 1

    max_check_interval = 30
    next_check_advance_time = 5

    mp.setattr(walle.host_fsm.fsm, "_handle_host", handle_host)
    mp.setattr(walle.host_fsm.fsm, "_CHECK_INTERVAL", max_check_interval)
    mp.setattr(walle.fsm_stages.common, "NEXT_CHECK_ADVANCE_TIME", next_check_advance_time)
    mp.config("deployment.max_concurrent_deployments", 100)

    monkeypatch_locks(mp)

    partitioner = mongo.MongoPartitionerService("some")
    partitioner.start()
    host_fsm = HostFsm(app.settings(), SHARDS_NUM_PATH, MAX_CONCURRENCY, partitioner)
    with host_fsm:
        _check_fsm_iteration(host_fsm, max_check_interval)
        test.hosts.assert_equal()

        monkeypatch_timestamp(mp)
        host0 = test.mock_host(
            {
                "uuid": uuid_0,
                "inv": 0,
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
                "task": mock_task(task_id=Task.next_task_id()),
            }
        )

        host1 = test.mock_host(
            {
                "uuid": uuid_1,
                "inv": 1,
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
                "task": mock_task(task_id=Task.next_task_id()),
            }
        )

        _check_fsm_iteration(host_fsm)
        host0.task.next_check = timestamp() + next_check_advance_time
        _check_fsm_iteration(host_fsm, max_check_interval)
        host1.task.next_check = timestamp() + next_check_advance_time
        test.hosts.assert_equal()

        _check_fsm_iteration(host_fsm, max_check_interval)
        test.hosts.assert_equal()

        status = any_task_status()
        test.mock_host(
            {
                "inv": 2,
                "state": HostState.ASSIGNED,
                "status": status,
                "task": mock_task(task_id=Task.next_task_id(), next_check=timestamp() + next_check_advance_time * 2),
            }
        )
        status = any_task_status()
        test.mock_host(
            {
                "inv": 3,
                "state": HostState.ASSIGNED,
                "status": status,
                "task": mock_task(task_id=Task.next_task_id(), next_check=timestamp() + next_check_advance_time // 2),
            }
        )
        _check_fsm_iteration(host_fsm, max_check_interval)
        test.hosts.assert_equal()

    assert handle_log == {uuid_0: 1, uuid_1: 2}


class TestStagesSwitching:
    FIRST_SIBLING_STAGE = "mock-first-sibling-stage"
    LAST_SIBLING_STAGE = "mock-last-sibling-stage"
    PROCESSED_PARENT_STAGE = "mock-processed-parent-stage"
    SKIPPED_PARENT_STAGE = "mock-skipped-parent-stage"
    FIRST_CHILD_STAGE = "mock-first-child-stage"
    LAST_CHILD_STAGE = "mock-last-child-stage"

    ALL_COMPLETE = (FIRST_SIBLING_STAGE, LAST_SIBLING_STAGE, SKIPPED_PARENT_STAGE, FIRST_CHILD_STAGE, LAST_CHILD_STAGE)

    ALL_PROCESSED = (
        FIRST_SIBLING_STAGE,
        PROCESSED_PARENT_STAGE,
        FIRST_CHILD_STAGE,
        LAST_CHILD_STAGE,
        LAST_SIBLING_STAGE,
    )

    stages_log = []

    @classmethod
    def setup_class(cls):
        cls.register_stages()

    @pytest.fixture(autouse=True)
    def clear_stage_log(self):
        self.stages_log[:] = []

    def test_switches_to_sibling(self, test, mp):
        mp.function(
            dmc.get_decisions_from_handler,
            return_value=(
                None,
                Decision(action=WalleAction.FAILURE, reason='Mock.', restrictions=[]),
            ),
        )
        stages = [
            Stage(name=self.FIRST_SIBLING_STAGE),
            Stage(name=self.LAST_SIBLING_STAGE),
        ]
        host = test.mock_host({"task": mock_task(stages=stages)})
        # processes two stages
        handle_host(host)
        handle_host(host)

        assert self.stages_log == [self.FIRST_SIBLING_STAGE, self.LAST_SIBLING_STAGE]

    def test_descends_to_children_upon_task_initialization(self, test, mp):
        mp.function(
            dmc.get_decisions_from_handler,
            return_value=(
                None,
                Decision(action=WalleAction.FAILURE, reason='Mock.', restrictions=[]),
            ),
        )
        stage = Stage(
            name=self.SKIPPED_PARENT_STAGE,
            stages=[Stage(name=self.FIRST_CHILD_STAGE), Stage(name=self.LAST_CHILD_STAGE)],
        )
        host = test.mock_host({"task": mock_task(stages=[stage])})
        assert get_current_stage(host, only_if_exists=True) is None  # this is setup assertion, not test

        # processes two stages
        handle_host(host)
        handle_host(host)

        assert self.stages_log == [self.FIRST_CHILD_STAGE, self.LAST_CHILD_STAGE]

    def test_descends_to_children_from_parent(self, test):
        stage = Stage(
            name=self.SKIPPED_PARENT_STAGE,
            stages=[Stage(name=self.FIRST_CHILD_STAGE), Stage(name=self.LAST_CHILD_STAGE)],
        )
        host = test.mock_host({"task": mock_task(stage=stage)})
        assert get_current_stage(host).name == self.SKIPPED_PARENT_STAGE  # this is not a test, it is a setup assertion

        # processes two stages
        handle_host(host)
        handle_host(host)

        assert self.stages_log == [self.FIRST_CHILD_STAGE, self.LAST_CHILD_STAGE]

    def test_ascends_to_parents_sibling(self, test, mp):
        mp.function(
            dmc.get_decisions_from_handler,
            return_value=(
                None,
                Decision(action=WalleAction.FAILURE, reason='Mock.', restrictions=[]),
            ),
        )
        stages = [
            Stage(name=self.SKIPPED_PARENT_STAGE, stages=[Stage(name=self.FIRST_CHILD_STAGE)]),
            Stage(name=self.LAST_SIBLING_STAGE),
        ]
        host = test.mock_host({"task": mock_task(stages=stages)})
        # processes two stages
        handle_host(host)
        handle_host(host)

        assert self.stages_log == [self.FIRST_CHILD_STAGE, self.LAST_SIBLING_STAGE]

    # setup methods
    @classmethod
    def register_stages(cls):
        def log_and_comlete_stage(host_):
            cls.stages_log.append(get_current_stage(host_).name)
            return complete_current_stage(host_)

        for stage in cls.ALL_COMPLETE:
            register_stage(stage, log_and_comlete_stage)


def test_host_locking(mp, test):
    def host_lock_init_mock(self, uuid, tier):
        self._allow_lock = uuid != host0.uuid

    monkeypatch_locks(mp)
    mp.setattr(walle.locks.HostInterruptableLock, "__init__", host_lock_init_mock)
    mp.setattr(walle.locks.HostInterruptableLock, "acquire", lambda self, blocking=True: self._allow_lock)

    partitioner = mongo.MongoPartitionerService("some")
    partitioner.start()
    host_fsm = HostFsm(app.settings(), SHARDS_NUM_PATH, MAX_CONCURRENCY, partitioner)
    with host_fsm:
        host0 = test.mock_host(
            {
                "inv": 0,
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
            }
        )

        host1 = test.mock_host(
            {
                "inv": 1,
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
            }
        )

        handle_host = mp.function(walle.host_fsm.fsm._handle_host)

        _check_fsm_iteration(host_fsm, walle.host_fsm.fsm._CHECK_INTERVAL)

        handle_host.assert_called_once_with(
            host1.uuid,
            host1.task.task_id,
            one_pass=False,
            suppress_no_task_error=True,
            suppress_internal_errors=(Exception,),
        )

        host0.task.next_check = timestamp() + walle.fsm_stages.common.NEXT_CHECK_ADVANCE_TIME
        host1.task.next_check = timestamp() + walle.fsm_stages.common.NEXT_CHECK_ADVANCE_TIME
        test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", HostStatus.ALL)
def test_immutability_for_host_without_tasks(test, state, status):
    host = test.hosts.mock({"state": state, "status": status})
    fsm._handle_host(host.inv, 0, suppress_no_task_error=True)
    test.hosts.assert_equal()


class TestHandbrakeGc:
    @staticmethod
    def _mock_project_with_fsm_handbrake(test, project_id, **handbrake_kwargs):
        default_fsm_handbrake = dict(
            {
                "timeout_time": timestamp(),
                "issuer": test.api_issuer,
                "timestamp": timestamp() - DAY_SECONDS,
                "audit_log_id": AUDIT_LOG_ID,
            }
        )

        return test.mock_project({"id": project_id, "fsm_handbrake": dict(default_fsm_handbrake, **handbrake_kwargs)})

    def test_removes_handbrake_from_project_with_expired_handbrake(self, test, monkeypatch_timestamp):
        project = self._mock_project_with_fsm_handbrake(test, "handbrake-expired", timeout_time=timestamp() - 1)
        self._mock_project_with_fsm_handbrake(test, "handbrake-not-expired", timeout_time=timestamp())

        handbrake._gc_project_fsm_handbrake()

        del project.fsm_handbrake
        test.projects.assert_equal()

    def test_removes_expired_handbrake_from_settings(self, test, monkeypatch_timestamp):
        settings = app.settings()
        settings.fsm_handbrake = FsmHandbrake(timeout_time=timestamp() - 1, audit_log_id=AUDIT_LOG_ID)
        settings.save()

        handbrake._gc_global_fsm_handbrake()

        assert app.settings().fsm_handbrake is None


def _run_iteration(fsm):
    """Attention: This method is only for unit tests."""

    fsm._state_machine()

    fsm._pool.join()
    # There is a possible race when greenlets are already stopped, but their completion handlers are executing.
    # TODO: solve this issue here and in stop() method
    time.sleep(0.1)

    return fsm._next_check


def _check_fsm_iteration(fsm, wait_time=None):
    next_check = _run_iteration(fsm)

    if wait_time is not None:
        _cmp_time(next_check, timestamp() + wait_time)


def _cmp_time(value, expected_value):
    assert expected_value - 1.1 < value < expected_value + 1.1
