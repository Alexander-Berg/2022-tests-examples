from itertools import combinations_with_replacement

import pytest

from infra.walle.server.tests.scenario.utils import mock_scenario
from sepelib.core.exceptions import LogicalError
from walle.clients import juggler as juggler_client
from walle.hosts import HostState, HostStatus
from walle.models import timestamp, monkeypatch_timestamp
from walle.scenario.constants import (
    ScenarioWorkStatus,
    WORK_STATUS_LABEL_NAME,
    HostScenarioStatus,
    NOC_SOFT_WORK_TIMEOUT_JUGGLER_SERVICE_NAME,
    ALL_TERMINATION_WORK_STATUSES,
    ScenarioFsmStatus,
)
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import ParentStageHandler, Stage
from walle.scenario.scenario import ScenarioHostState
from walle.scenario.stage.noc_maintenance_stage import (
    NocMaintenanceStage,
    FinishNocMaintenanceStage,
    _collect_guilty_hosts,
)
from walle.scenario.stage.scheduler_stage import HostSchedulerStage, host_scheduler_stage
from walle.scenario.stage_info import StageAction, StageInfo
from walle.scenario.stages import HostRootStage, WaitStateStatusHostStage
from walle.scenario.scenario import Scenario, HostScenarioStatus


@pytest.fixture(autouse=True)
def test(walle_test):
    pass


class MockSchedulerStage(HostSchedulerStage):
    def __init__(self, host_root_stage, action_type):
        self.action_type = action_type
        super().__init__(host_root_stage, group_scheduler=None, actions=[])

    def run(self, stage_info, scenario, *args, **kwargs):
        return self.host_root_stage.run(stage_info, scenario, *args, **kwargs)

    def serialize(self, uid="0"):
        stage_info = super().serialize(uid)
        stage_info.action_type = self.action_type

        return stage_info


class MockHostStage(ParentStageHandler, Stage):
    def __init__(self, expected_result=None, **params):
        self.expected_result = expected_result

        super().__init__([Stage()], **params)

    def run(self, stage_info, scenario):
        if self.expected_result is not None:
            return self.expected_result
        else:
            return Marker.success(message="mock host stage succeed")


def _mk_mock_scheduler_stage(scheduler_result=None, scheduler_action=StageAction.CHECK):
    return MockSchedulerStage(MockHostStage(expected_result=scheduler_result), action_type=scheduler_action)


def _mk_maintenance_stage(
    scheduler_stage=None, scheduler_result=None, scheduler_action=StageAction.CHECK, execution_time=None, work_time=None
):
    if scheduler_stage is None:
        scheduler_stage = _mk_mock_scheduler_stage(scheduler_result=scheduler_result, scheduler_action=scheduler_action)

    return NocMaintenanceStage(scheduler_stage, execution_timeout=execution_time, work_timeout=work_time)


class TestOnlySchedulerStageAcceptedAsAChild:
    def test_single_scheduler_stage_accepted_as_child(self):
        scheduler_stage = _mk_mock_scheduler_stage()
        noc_maintenance_stage = NocMaintenanceStage(scheduler_stage)
        assert noc_maintenance_stage.children == [scheduler_stage]

    def test_list_of_single_scheduler_stage_accepted_as_child(self):
        scheduler_stage = _mk_mock_scheduler_stage()
        noc_maintenance_stage = NocMaintenanceStage([scheduler_stage])
        assert noc_maintenance_stage.children == [scheduler_stage]

    def test_list_of_multiple_scheduler_stages_is_not_accepted(self):
        scheduler_stage = _mk_mock_scheduler_stage()
        with pytest.raises(LogicalError):
            NocMaintenanceStage([scheduler_stage, scheduler_stage])

    def test_list_of_non_scheduler_stages_is_not_accepted(self):
        with pytest.raises(LogicalError):
            NocMaintenanceStage([MockHostStage()])

    def test_empty_list_of_children_is_not_accepted(self):
        with pytest.raises(LogicalError):
            NocMaintenanceStage([])


class TestFinishesOnCancel:
    @staticmethod
    def _execute_stage(walle_test, stage, labels):
        stage_info = stage.serialize("0")

        scenario = walle_test.mock_scenario(overrides=dict(labels=labels))

        return stage.run(stage_info, scenario)

    ALL_NON_TERMINAL_STATUSES = (
        set(ScenarioWorkStatus)
        - {ScenarioWorkStatus.CREATED}  # this status can not be processed
        - {ScenarioWorkStatus.APPROVEMENT}  # NOTE(rocco66): there is not 'approvement' status for noc_soft scenario
        - {
            ScenarioWorkStatus.ACQUIRING_PERMISSION
        }  # NOTE(alexsmirnov): there is not 'acquiring_permission' status for noc_soft
        - set(ALL_TERMINATION_WORK_STATUSES)
    )

    @pytest.mark.parametrize("status", ALL_TERMINATION_WORK_STATUSES)
    def test_stage_finishes_when_status_is_terminal(self, walle_test, status):
        nms = _mk_maintenance_stage()

        marker = self._execute_stage(walle_test, nms, labels={WORK_STATUS_LABEL_NAME: status})

        assert marker.status == MarkerStatus.SUCCESS
        assert status in marker.message

    @pytest.mark.parametrize("status", ALL_NON_TERMINAL_STATUSES)
    def test_stage_proceeds_when_status_other(self, walle_test, status):
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress())

        marker = self._execute_stage(walle_test, nms, labels={WORK_STATUS_LABEL_NAME: status})
        assert marker.status == MarkerStatus.IN_PROGRESS

    def test_stage_finishes_when_fsm_status_canceling(self, walle_test):
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress())
        expected_marker_message = "Scenario was canceled"
        stage_info = nms.serialize("0")
        scenario = walle_test.mock_scenario(overrides=dict(status=ScenarioFsmStatus.CANCELING))
        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.SUCCESS
        assert marker.message == expected_marker_message

    @pytest.mark.parametrize(
        "status",
        set(ScenarioWorkStatus)
        - {
            ScenarioWorkStatus.CREATED,
            ScenarioWorkStatus.READY,
            ScenarioWorkStatus.APPROVEMENT,  # NOTE(rocco66): there is not 'approvement' status for noc_soft scenario
            ScenarioWorkStatus.ACQUIRING_PERMISSION,  # NOTE(alexsmirnov): there is not 'acquiring_permission' status for noc_soft
        },
    )
    def test_finishes_when_status_not_ready_but_timed_out(self, walle_test, status):
        ts = timestamp()
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress(), execution_time=ts)
        stage_info = nms.serialize("0")
        stage_info.data[NocMaintenanceStage.STAGE_END_DEADLINE] = timestamp() - 10
        host = walle_test.mock_host(dict(state=HostState.ASSIGNED, status=HostStatus.READY))
        hosts_states = Scenario.create_list_of_host_states([host.inv], resolve_uuids=True)
        for key, host_state in hosts_states.items():
            host_state.is_acquired = True
            host_state.status = HostScenarioStatus.ACQUIRED
        scenario = walle_test.mock_scenario(dict(labels={WORK_STATUS_LABEL_NAME: status}, hosts_states=hosts_states))

        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.SUCCESS

    def test_not_finishes_when_status_ready_but_timed_out(self):
        ts = timestamp()
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress(), execution_time=ts)
        stage_info = nms.serialize("0")
        stage_info.data[NocMaintenanceStage.STAGE_END_DEADLINE] = timestamp() - 10
        scenario = mock_scenario(labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.READY})

        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.IN_PROGRESS


class TestSendEventWhenWorkTimedOut:
    def test_send_event_when_status_ready_and_work_timed_out(self, send_event_mock):
        timeout = 10
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress(), work_time=timeout)
        stage_info = nms.serialize("0")
        ts = timestamp()
        stage_info.data[NocMaintenanceStage.WORK_END_DEADLINE] = ts - timeout
        scenario = mock_scenario(labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.READY})

        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.IN_PROGRESS
        send_event_mock.assert_called_once_with(
            NOC_SOFT_WORK_TIMEOUT_JUGGLER_SERVICE_NAME,
            juggler_client.JugglerCheckStatus.CRIT,
            """NOC maintenance stage is not finished before timeout {} from start time {}
Scenario: https://wall-e.yandex-team.ru/scenarios/{}""".format(
                timeout, ts - 2 * timeout, scenario.scenario_id
            ),
        )

    def test_not_send_events_when_status_not_ready_but_timed_out(self, walle_test, send_event_mock):
        ts = timestamp()
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress(), execution_time=ts)
        stage_info = nms.serialize("0")
        stage_info.data[NocMaintenanceStage.STAGE_END_DEADLINE] = timestamp() - 10
        host = walle_test.mock_host(dict(state=HostState.ASSIGNED, status=HostStatus.READY))
        hosts_states = Scenario.create_list_of_host_states([host.inv], resolve_uuids=True)
        for key, host_state in hosts_states.items():
            host_state.is_acquired = True
            host_state.status = HostScenarioStatus.ACQUIRED
        scenario = walle_test.mock_scenario(
            dict(labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.STARTED}, hosts_states=hosts_states)
        )

        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.SUCCESS
        send_event_mock.assert_not_called()

    @pytest.mark.parametrize("status", ALL_TERMINATION_WORK_STATUSES)
    def test_send_event_ok_when_status_is_not_ready_and_work_timed_out(self, status, send_event_mock):
        timeout = 10
        nms = _mk_maintenance_stage(scheduler_result=Marker.in_progress(), work_time=timeout)
        stage_info = nms.serialize("0")
        ts = timestamp()
        stage_info.data[NocMaintenanceStage.WORK_END_DEADLINE] = ts - timeout
        scenario = mock_scenario(labels={WORK_STATUS_LABEL_NAME: status})

        marker = nms.run(stage_info, scenario)

        assert marker.status == MarkerStatus.SUCCESS
        send_event_mock.assert_called_once_with(
            NOC_SOFT_WORK_TIMEOUT_JUGGLER_SERVICE_NAME,
            juggler_client.JugglerCheckStatus.OK,
            "NOC maintenance stage stage for scenario https://wall-e.yandex-team.ru/scenarios/{} terminated successfully".format(
                scenario.scenario_id
            ),
        )


class TestStageAllowsWorksWhenHostsReady:
    @staticmethod
    def _execute_stage(scenario, scheduler_action, result_marker=None):
        if result_marker is None:
            result_marker = Marker.in_progress()

        stage = _mk_maintenance_stage(scheduler_action=scheduler_action, scheduler_result=result_marker)

        stage_info = stage.serialize("0")
        return stage.run(stage_info, scenario)

    @staticmethod
    def _mock_scenario(walle_test, works_status, host_statuses):
        host_invs = {
            str(inv): ScenarioHostState(inv=inv, status=host_status) for inv, host_status in enumerate(host_statuses)
        }

        scenario = walle_test.mock_scenario(
            overrides=dict(hosts_states=host_invs, labels={WORK_STATUS_LABEL_NAME: works_status})
        )

        return scenario

    NON_PROCESSING_STATUSES = set(HostScenarioStatus.ALL) - HostScenarioStatus.ALL_SCHEDULED

    @pytest.mark.parametrize("host_statuses", combinations_with_replacement(NON_PROCESSING_STATUSES, 2))
    def test_allows_works_when_no_processing_hosts(self, walle_test, host_statuses):
        scenario = self._mock_scenario(walle_test, ScenarioWorkStatus.STARTED, host_statuses)

        marker = self._execute_stage(scenario, StageAction.CHECK)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.READY
        assert marker.status == MarkerStatus.IN_PROGRESS

    @pytest.mark.parametrize("scheduler_action", set(StageAction.ALL) - {StageAction.CHECK})
    @pytest.mark.parametrize("current_status", ScenarioWorkStatus)
    def test_does_not_change_status_if_scheduler_action_is_not_check(
        self, walle_test, scheduler_action, current_status
    ):
        scenario = self._mock_scenario(walle_test, current_status, [HostScenarioStatus.DONE] * 3)

        self._execute_stage(scenario, scheduler_action)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == current_status

    @pytest.mark.parametrize(
        "current_status",
        set(ScenarioWorkStatus)
        - {
            ScenarioWorkStatus.STARTED,
            ScenarioWorkStatus.CREATED,
            ScenarioWorkStatus.APPROVEMENT,  # NOTE(rocco66): there is not 'approvement' status for noc_soft scenario
            ScenarioWorkStatus.ACQUIRING_PERMISSION,
        },
    )
    def test_does_not_change_status_if_current_status_is_not_started(self, walle_test, current_status):
        scenario = self._mock_scenario(walle_test, current_status, [HostScenarioStatus.DONE] * 3)

        self._execute_stage(scenario, StageAction.CHECK)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == current_status

    @pytest.mark.parametrize("other_host_status", HostScenarioStatus.ALL)
    def test_does_not_change_status_if_any_hosts_in_processing(self, walle_test, other_host_status):
        scenario = self._mock_scenario(
            walle_test, ScenarioWorkStatus.STARTED, [other_host_status, HostScenarioStatus.PROCESSING]
        )

        self._execute_stage(scenario, StageAction.CHECK)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.STARTED


class TestStageRejectsWorksWhenFinishes:
    @staticmethod
    def _execute_stage(scenario, result_marker=None):
        if result_marker is None:
            result_marker = Marker.in_progress()

        stage = _mk_maintenance_stage(scheduler_action=StageAction.CHECK, scheduler_result=result_marker)

        stage_info = stage.serialize("0")
        return stage.run(stage_info, scenario)

    @staticmethod
    def _mock_scenario(walle_test, works_status, host_statuses):
        host_invs = {
            str(inv): ScenarioHostState(inv=inv, status=host_status) for inv, host_status in enumerate(host_statuses)
        }

        scenario = walle_test.mock_scenario(
            overrides=dict(hosts_states=host_invs, labels={WORK_STATUS_LABEL_NAME: works_status})
        )

        return scenario

    @pytest.mark.parametrize(
        "current_status", set(ScenarioWorkStatus) - {ScenarioWorkStatus.STARTED, ScenarioWorkStatus.CREATED}
    )
    def test_does_not_reject_if_already_allowed_or_finished(self, walle_test, current_status):
        scenario = self._mock_scenario(walle_test, current_status, [HostScenarioStatus.PROCESSING])

        self._execute_stage(scenario, result_marker=Marker.success())
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == current_status

    def test_does_not_reject_if_all_hosts_done(self, walle_test):
        # this checks that TestStageAllowsWorksWhenHostsReady kicks in before TestStageRejectsWorksWhenFinishes

        scenario = self._mock_scenario(walle_test, ScenarioWorkStatus.STARTED, [HostScenarioStatus.DONE])

        self._execute_stage(scenario, result_marker=Marker.success())
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.READY


class TestNocMaintenanceStageEnding:
    @staticmethod
    def _get_stage_info_and_scenario(walle_test, works_status, host_statuses, end_time=None):
        host_invs = {
            str(inv): ScenarioHostState(inv=inv, status=host_status) for inv, host_status in enumerate(host_statuses)
        }
        stage_info = StageInfo()
        scenario = walle_test.mock_scenario(
            overrides=dict(hosts_states=host_invs, labels={WORK_STATUS_LABEL_NAME: works_status})
        )

        if end_time:
            stage_info.data[NocMaintenanceStage.STAGE_END_DEADLINE] = end_time

        return stage_info, scenario

    @staticmethod
    def _execute_stage(
        stage_info,
        scenario,
        execution_time=None,
        termination_label=WORK_STATUS_LABEL_NAME,
        label_values=ALL_TERMINATION_WORK_STATUSES,
    ):
        stage = NocMaintenanceStage(
            _mk_mock_scheduler_stage(),
            execution_timeout=execution_time,
            termination_label=termination_label,
            label_values=label_values,
        )
        return stage.run(stage_info, scenario)

    def test_in_progress_if_status_is_ready(self, walle_test):
        stage_info, scenario = self._get_stage_info_and_scenario(
            walle_test, ScenarioWorkStatus.READY, [HostScenarioStatus.DONE]
        )
        marker = self._execute_stage(stage_info, scenario)
        assert marker.status == MarkerStatus.IN_PROGRESS
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.READY

    @pytest.mark.parametrize("status", ALL_TERMINATION_WORK_STATUSES)
    def test_end_if_work_terminated(self, walle_test, status):
        stage_info, scenario = self._get_stage_info_and_scenario(walle_test, status, [HostScenarioStatus.DONE])
        marker = self._execute_stage(stage_info, scenario)
        assert marker.status == MarkerStatus.SUCCESS
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == status


def test_serialize_deserialize():
    stage = NocMaintenanceStage(
        host_scheduler_stage(
            [
                HostRootStage(
                    [
                        WaitStateStatusHostStage(
                            target_project="mock", wait_state=HostState.ASSIGNED, wait_status=HostStatus.READY
                        )
                    ]
                )
            ]
        )
    )
    stage_info = stage.serialize("0")

    deserialized_stage = stage_info.deserialize()
    assert type(deserialized_stage) == type(stage)
    assert deserialized_stage.children == stage.children
    assert deserialized_stage.params == stage.params


class TestFinishNocMaintenanceStage:
    @staticmethod
    def _mock_scenario(works_status):
        return mock_scenario(labels={WORK_STATUS_LABEL_NAME: works_status})

    @staticmethod
    def _execute_stage(scenario):
        stage = FinishNocMaintenanceStage()

        stage_info = stage.serialize("0")
        return stage.run(stage_info, scenario)

    @pytest.mark.parametrize(
        ["from_status", "to_status"],
        [
            (ScenarioWorkStatus.FINISHING, ScenarioWorkStatus.FINISHED),
            (ScenarioWorkStatus.CANCELING, ScenarioWorkStatus.CANCELED),
        ],
    )
    def test_sets_final_status_for_finalizing_scenario(self, from_status, to_status):
        scenario = self._mock_scenario(from_status)

        result_marker = self._execute_stage(scenario)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == to_status
        assert result_marker.status == MarkerStatus.SUCCESS

    def test_does_not_change_status_for_rejected_scenario(self):
        scenario = self._mock_scenario(ScenarioWorkStatus.REJECTED)

        result_marker = self._execute_stage(scenario)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.REJECTED
        assert result_marker.status == MarkerStatus.SUCCESS

    @pytest.mark.parametrize("scenario_status", [ScenarioWorkStatus.FINISHED, ScenarioWorkStatus.CANCELED])
    def test_does_not_change_status_for_final_statuses(self, scenario_status):
        scenario = self._mock_scenario(scenario_status)

        result_marker = self._execute_stage(scenario)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == scenario_status
        assert result_marker.status == MarkerStatus.SUCCESS

    OTHER_STATUSES = set(ScenarioWorkStatus) - {
        ScenarioWorkStatus.FINISHED,
        ScenarioWorkStatus.FINISHING,
        ScenarioWorkStatus.CANCELED,
        ScenarioWorkStatus.CANCELING,
        ScenarioWorkStatus.REJECTED,
    }

    @pytest.mark.parametrize("scenario_status", OTHER_STATUSES)
    def test_changes_status_for_other_statuses(self, scenario_status):
        scenario = self._mock_scenario(scenario_status)

        result_marker = self._execute_stage(scenario)
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.FINISHED
        assert result_marker.status == MarkerStatus.SUCCESS


def test_collect_guilty_hosts(mp, walle_test):
    monkeypatch_timestamp(mp, 1)

    host_invs = {}

    invs_and_names = [(1, "good-1"), (2, "good-2"), (3, "good-3"), (4, "failed")]
    for inv, fqdn in invs_and_names:
        host_invs[str(inv)] = ScenarioHostState(inv=inv, status=HostScenarioStatus.PROCESSING, is_acquired=True)

        if inv != 4:
            walle_test.mock_host(dict(inv=inv, status=HostStatus.MANUAL, state=HostState.MAINTENANCE, name=fqdn))
        else:
            walle_test.mock_host(dict(inv=inv, status=HostStatus.DEAD, state=HostState.MAINTENANCE, name=fqdn))

    scenario = mock_scenario(hosts=host_invs, labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.STARTED})

    fqdns = _collect_guilty_hosts(scenario)

    assert fqdns == ["failed"]
