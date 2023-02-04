import contextlib
import copy

import mock
import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    monkeypatch_locks,
    monkeypatch_automation_plot_id,
    handle_host as fsm_handle_host,
    monkeypatch_config,
)
from infra.walle.server.tests.expert.util import monkeypatch_health_data
from walle.expert import types, screening, triage
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.hosts import HostState, HostStatus
from walle.util import mongo
from walle.clients import dmc
from walle.expert.decision import Decision
from walle.expert.types import WalleAction
from infra.walle.server.tests.lib.util import (
    monkeypatch_function,
)


@pytest.fixture(autouse=True)
def test(request, mp, monkeypatch):
    monkeypatch_locks(mp)
    monkeypatch_automation_plot_id(monkeypatch, AUTOMATION_PLOT_FULL_FEATURED_ID)
    return TestCase.create(request)


@contextlib.contextmanager
def _restore_health(target_host_health_checks):
    # NOTE(rocco66): health metadata is deserialized while loading from mongo, next run is not ready for that
    # TODO(rocco66): do not modify health checks while loading
    unparsed_metadata_health = copy.deepcopy(target_host_health_checks)
    yield
    target_host_health_checks[:] = copy.deepcopy(unparsed_metadata_health)


def _fsm(host, target_host_health_checks):
    host.reload()
    with _restore_health(target_host_health_checks):
        fsm_handle_host(host)
    host.reload()


def _triage(target_host_health_checks):
    shard = mongo.MongoPartitionerShard("0", mock.MagicMock())
    triage_processor = triage.TriageShardProcessor(1)
    with _restore_health(target_host_health_checks):
        triage_processor._triage(shard)


def _screening(host, target_host_health_checks):
    shard = mongo.MongoPartitionerShard("0", mock.MagicMock())
    screening_processor = screening.ScreeningShardProcessor(1)
    with _restore_health(target_host_health_checks):
        screening_processor._receive_health(shard)
    host.reload()

    # NOTE(rocco66): failure type is string in ORM, but array in python and in mongodb (OMFG)
    # TODO(rocco66): use only string type for failure_type everywhere and merge two Decisions (orm + python class)
    host.health.decision.failure_type = host.health.decision.failure_type[0]
    host.save()


def _break_check(all_checks, check_type):
    memory_check = next(c for c in all_checks if c["type"] == check_type)
    memory_check["status"] = types.CheckStatus.FAILED


@pytest.mark.usefixtures("enable_modern_automation_plot")
def test_new_task_on_higher_priority_failure(test, mp, mock_health_data, mp_juggler_source):
    host = test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": test.default_project.id,
        }
    )
    many_hosts_healths = monkeypatch_health_data(
        mp, [host], copy.deepcopy(mock_health_data), status=types.CheckStatus.PASSED
    )
    target_host_health_checks = many_hosts_healths[0].checks

    _break_check(target_host_health_checks, types.CheckType.MEMORY)
    mock_handler = monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(
            None,
            Decision(
                action=WalleAction.REPAIR_MEMORY, reason='Mock.', restrictions=[], checks=[types.CheckType.MEMORY]
            ),
        ),
    )

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)
    _fsm(host, target_host_health_checks)

    assert host.health.decision.checks == [types.CheckType.MEMORY]
    assert host.task

    # NOTE(rocco66): break second check (but with same priority)
    _break_check(target_host_health_checks, types.CheckType.SSH)
    mock_handler.return_value = (
        None,
        Decision(action=WalleAction.REBOOT, reason='Mock.', restrictions=[], checks=[types.CheckType.SSH]),
    )

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)
    _fsm(host, target_host_health_checks)

    # NOTE(rocco66): no priority changes - no task changes
    assert host.health.decision.checks == [types.CheckType.MEMORY]
    assert host.task

    # NOTE(rocco66): increase second broken check priority
    monkeypatch_config(mp, f"expert_system.check_type_priorities.{types.CheckType.SSH}", 20)

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)
    _fsm(host, target_host_health_checks)  # NOTE(rocco66): task will be cancelled here

    assert host.health.decision.checks == [types.CheckType.MEMORY]
    assert not host.task

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)  # NOTE(rocco66): new task will be created here
    _fsm(host, target_host_health_checks)

    assert host.health.decision.checks == [types.CheckType.SSH]
    assert host.task


@pytest.mark.usefixtures("enable_modern_automation_plot")
def do_not_cancel_task_with_same_priority_but_higher_rule_order(test, mp, mock_health_data, mp_juggler_source):
    host = test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": test.default_project.id,
        }
    )
    many_hosts_healths = monkeypatch_health_data(
        mp, [host], copy.deepcopy(mock_health_data), status=types.CheckStatus.PASSED
    )
    target_host_health_checks = many_hosts_healths[0].checks

    _break_check(target_host_health_checks, types.CheckType.INFINIBAND)

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)
    _fsm(host, target_host_health_checks)

    assert host.health.decision.checks == [types.CheckType.INFINIBAND]
    assert host.task

    # NOTE(rocco66): break second check (but with same priority)
    _break_check(target_host_health_checks, types.CheckType.MEMORY)

    _screening(host, target_host_health_checks)
    _triage(target_host_health_checks)
    _fsm(host, target_host_health_checks)

    # NOTE(rocco66): no priority changes - no task changes
    assert host.health.decision.checks == [types.CheckType.INFINIBAND]
    assert host.task
