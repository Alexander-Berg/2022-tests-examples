from unittest.mock import Mock, ANY

import pytest

from infra.walle.server.tests.lib.util import (
    mock_schedule_maintenance,
    mock_schedule_project_switching,
    mock_schedule_host_preparing,
    monkeypatch_method,
    mock_schedule_assigned,
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_function,
    mock_task,
    get_issuer,
    mock_schedule_host_power_off,
    TestCase,
    mock_host_adding,
    monkeypatch_network_get_current_host_switch_port,
    mock_host_adding_in_maintenance,
    ObjectMocker,
    mock_schedule_host_profiling,
    mock_schedule_switch_vlans,
    mock_schedule_host_redeployment,
    mock_schedule_host_reboot,
    mock_schedule_check_host_dns,
    mock_schedule_release_host,
)
from infra.walle.server.tests.scenario.utils import (
    get_scenario_params,
    mock_scenario,
    mock_host_inside_qloud,
    make_mock_stage,
    make_mock_host_stage,
)
from sepelib.core.constants import MINUTE_SECONDS
from sepelib.core.exceptions import LogicalError
from walle import authorization, host_operations
from walle.clients import bot, inventory, startrek, qloud, ipmiproxy, cms, eine
from walle.clients.eine import ProfileMode
from walle.constants import PROVISIONER_EINE, PROVISIONER_LUI, NetworkTarget, EINE_NOP_PROFILE, FLEXY_EINE_PROFILE
from walle.errors import InvalidHostStateError
from walle.expert.types import CheckType, CheckStatus
from walle.host_status import MIN_STATE_TIMEOUT
from walle.hosts import HealthStatus, HostStatus, HostOperationState, StateExpire, TaskType, NonIpmiHostError, HostState
from walle.maintenance_plot import constants as maintenance_plot_constants
from walle.maintenance_plot.model import MaintenancePlotMetaInfo, MaintenancePlotScenarioSettings
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.operations_log.operations import OperationLog
from walle.physical_location_tree import LocationNamesMap
from walle.scenario.constants import (
    HostScenarioStatus,
    ScriptArgs,
    TemplatePath,
    TicketStatus,
    TICKET_RESOLUTION,
    TicketTransition,
    WORKMATE_STATUS_LABEL_NAME,
    WORKMATE_STATUS_TARGET_LABEL_VALUE,
    ScenarioWorkStatus,
    WORK_STATUS_LABEL_NAME,
    ScriptName,
)
from walle.scenario.data_storage.types import HostGroupSource
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.scenario import Scenario, ScenarioHostState, HostStageStatus
from walle.scenario.stage_info import StageAction, StageInfo
from walle.scenario.stages import (
    NoopStage,
    ScenarioRootStage,
    HostRootStage,
    AddHostsStage,
    SetLabelStage,
    WaitForLabelOrTimeStage,
    PrepareForWorkStage,
    SetAssignedStage,
    PrepareHostStage,
    SwitchProjectHostStage,
    ExecuteTicketTransitionStage,
    AddStartrekMessageStage,
    AddStartrekTagStage,
    PowerOffHostStage,
    WaitStateStatusHostStage,
    SetLabelIfAllScheduledStage,
    LiberateFromQloudHostStage,
    RemoveHostsStage,
    ProfileHostStage,
    SwitchVlansHostStage,
    RedeployHostStage,
    RebootHostStage,
    CheckDnsHostStage,
    ITDCWorkflowStage,
    WaitEineProfileHostStage,
    ReleaseHostStage,
    SetABCServiceIdStage,
    CollectHealthChecksStage,
    OptionalRedeployHostStage,
    SwitchToMaintenanceHostStage,
    CallOnResponsibleForLoadReturn,
)
from walle.util.template_loader import JinjaTemplateRenderer


@pytest.fixture
def shortnames(walle_test):
    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)


class TestNoopStage:

    stage = NoopStage()

    def test_action(self):
        stage_info = StageInfo(action_type=StageAction.ACTION)
        result = self.stage.run(stage_info, Mock())
        assert result == Marker.in_progress(message="Stage did nothing and finished")
        assert stage_info.action_type == StageAction.CHECK

    def test_check(self):
        result = self.stage.run(StageInfo(action_type=StageAction.CHECK), Mock())
        assert result == Marker.in_progress(message="Stage did nothing and continued")


class TestScenarioRootStage:
    def test_run_complex_script(self, mock_stage_registry):
        script = ScenarioRootStage([ScenarioRootStage([make_mock_stage(), make_mock_stage()]), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())

        for _ in range(2):
            assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS

        for i in range(2):
            assert script.children[0].children[i].run.call_count == 1
        assert script.children[1].run.call_count == 1

    def test_run_simple_in_root_stage(self, mock_stage_registry):
        simple_stage_mock = make_mock_stage()
        script = ScenarioRootStage([simple_stage_mock])
        scenario = mock_scenario(stage_info=script.serialize())
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS
        assert simple_stage_mock.run.call_count == 1

    def test_run_root_in_root_stage(self, mock_stage_registry):
        script = ScenarioRootStage([ScenarioRootStage([make_mock_stage()])])
        scenario = mock_scenario(stage_info=script.serialize())
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS

    def test_init_root_without_children(self):
        with pytest.raises(LogicalError):
            ScenarioRootStage([])


class TestITDCWorkflowStage:
    def test_run_script_successfully(self, mock_stage_registry):
        script = ScenarioRootStage([ITDCWorkflowStage([make_mock_stage(), make_mock_stage()]), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())

        for _ in range(2):
            assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS

        for i in range(2):
            assert script.children[0].children[i].run.call_count == 1
        assert script.children[1].run.call_count == 1

    def test_run_script_with_cancelled_label(self, mock_stage_registry):
        script = ScenarioRootStage([ITDCWorkflowStage([make_mock_stage()]), make_mock_stage()])
        scenario = mock_scenario(
            stage_info=script.serialize(), labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.CANCELING}
        )

        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS

        assert script.children[0].children[0].run.call_count == 0
        assert script.children[1].run.call_count == 1


class TestHostRootStage:
    def test_flat_stages(self, walle_test):
        host_script = HostRootStage([make_mock_host_stage(), make_mock_host_stage(), make_mock_host_stage()])
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario()
        for _ in range(2):
            assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.in_progress(
                message="Stage runs child stages"
            )
        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.success(
            message="Stage has successfully executed all of its child stages"
        )

    def test_host_states_saving(self, walle_test, mp):
        STAGE_INFO_HOST_STATE = {"status_time": 0, "status": HostStageStatus.FINISHED, "msg": "no message"}
        monkeypatch_timestamp(mp, cur_time=0)
        host_script = HostRootStage([make_mock_host_stage(), make_mock_host_stage()])
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario(stage_info=scenario_stage_info)

        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.in_progress(
            message="Stage runs child stages"
        )
        scenario.update_stage_info_hosts_for_all_child_stages()
        assert scenario_stage_info.stages[0].hosts[host.uuid] == STAGE_INFO_HOST_STATE

        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.success(
            message="Stage has successfully executed all of its child stages"
        )
        scenario.update_stage_info_hosts_for_all_child_stages()
        assert scenario_stage_info.stages[1].hosts[host.uuid] == STAGE_INFO_HOST_STATE

    def test_run_complex_script(self, walle_test, mock_stage_registry):
        host_script = HostRootStage(
            [HostRootStage([make_mock_host_stage(), make_mock_host_stage()]), make_mock_host_stage()]
        )
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario()

        for _ in range(2):
            assert host_script.run(stage_info, scenario, host, scenario_stage_info).status == MarkerStatus.IN_PROGRESS
        assert host_script.run(stage_info, scenario, host, scenario_stage_info).status == MarkerStatus.SUCCESS

        for i in range(2):
            assert host_script.children[0].children[i].run.call_count == 1
        assert host_script.children[1].run.call_count == 1

    def test_run_simple_in_root_stage(self, walle_test, mock_stage_registry):
        simple_stage_mock = make_mock_host_stage()
        host_script = HostRootStage([simple_stage_mock])
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario()

        assert host_script.run(stage_info, scenario, host, scenario_stage_info).status == MarkerStatus.SUCCESS
        assert simple_stage_mock.run.call_count == 1

    def test_run_root_in_root_stage(self, walle_test, mock_stage_registry):
        host_script = HostRootStage([HostRootStage([make_mock_host_stage()])])
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario()
        assert host_script.run(stage_info, scenario, host, scenario_stage_info).status == MarkerStatus.SUCCESS

    def test_init_root_without_children(self, mock_stage_registry):
        with pytest.raises(LogicalError):
            HostRootStage([])

    def test_init_with_only_host_stages(self, mock_stage_registry):
        with pytest.raises(LogicalError):
            HostRootStage([make_mock_host_stage(), make_mock_stage(), make_mock_host_stage()])

    def test_hrs_saved_shared_data_from_marker(self, walle_test, mock_stage_registry):
        SHARED_DATA = {"test": "test"}
        host_script = HostRootStage([make_mock_host_stage(data=SHARED_DATA)])
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()
        host = walle_test.mock_host()
        scenario = mock_scenario()
        assert not stage_info.shared_data
        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.success(
            data=SHARED_DATA, message="Stage has successfully executed all of its child stages"
        )
        assert stage_info.shared_data == SHARED_DATA


class TestRemoveHostsStage:

    INV = 1
    TEST_PROJECT_ID = "search-delete"
    MOCK_TICKET = "TEST-1"

    def _run_stage(self):
        scenario_params = get_scenario_params()
        scenario = Scenario(
            hosts={str(self.INV): ScenarioHostState(inv=self.INV, status="queue", timestamp=timestamp())},
            ticket_key=self.MOCK_TICKET,
            **scenario_params,
        )
        stage_info = StageInfo()
        stage = RemoveHostsStage(intermediate_project=self.TEST_PROJECT_ID)
        return stage.run(stage_info, scenario)

    def test_remove_not_called_host_in_wrong_project(self, mp, walle_test):
        delete_host_mock = monkeypatch_function(mp, host_operations.instant_delete_host, module=host_operations)
        walle_test.mock_host({"inv": 1, "project": "wrong"})
        result = self._run_stage()
        assert not delete_host_mock.called
        assert result.status == MarkerStatus.IN_PROGRESS

    def test_remove_called_host_in_right_project(self, mp, walle_test):
        delete_host_mock = monkeypatch_function(mp, host_operations.instant_delete_host, module=host_operations)
        walle_test.mock_host({"inv": 1, "project": self.TEST_PROJECT_ID})
        result = self._run_stage()
        assert delete_host_mock.called
        assert result.status == MarkerStatus.IN_PROGRESS

    def test_host_already_removed(self, walle_test):
        result = self._run_stage()
        assert result.status == MarkerStatus.SUCCESS


class TestAddHostsStage:

    INV = 1
    TEST_PROJECT_ID = "search-delete"
    MOCK_TICKET = "TEST-1"

    def _run_stage(self):
        scenario_params = get_scenario_params()
        scenario = Scenario(
            hosts={str(self.INV): ScenarioHostState(inv=self.INV, status="queue", timestamp=timestamp())},
            ticket_key=self.MOCK_TICKET,
            **scenario_params,
        )
        stage_info = StageInfo()
        stage = AddHostsStage(intermediate_project=self.TEST_PROJECT_ID)
        return stage.run(stage_info, scenario)

    def _mp_dependencies(self, mp, hostname):
        monkeypatch_inventory_get_host_info_and_check_status(mp, hostname=hostname)
        monkeypatch_network_get_current_host_switch_port(mp)
        mp.function(bot.missed_preordered_hosts, return_value={})
        monkeypatch_function(mp, startrek.get_client, module=startrek)
        mp.function(host_operations._exists_in_dns, side_effect=lambda fqdn: fqdn != "missing-in-dns.mock")

    @pytest.mark.usefixtures("shortnames", "monkeypatch_locks", "monkeypatch_audit_log", "monkeypatch_host_uuid")
    def test_add_host_not_in_walle_in_free(self, walle_test, mp):
        hostname = None

        self._mp_dependencies(mp, hostname)

        project = walle_test.mock_project({"id": "search-delete"})
        host_params = dict(
            state=HostState.FREE, status=HostStatus.READY, project=project.id, inv=self.INV, name=hostname
        )
        host = walle_test.mock_host(host_params, save=False)
        mock_host_adding(host, manual=False, reason=self.MOCK_TICKET)

        result = self._run_stage()

        assert result.status == MarkerStatus.IN_PROGRESS

        walle_test.hosts.assert_equal()

    @pytest.mark.usefixtures("shortnames", "monkeypatch_locks", "monkeypatch_audit_log", "monkeypatch_host_uuid")
    def test_add_host_not_in_walle_in_maintenance(self, walle_test, mp):
        hostname = "test-walle.ya.net"

        self._mp_dependencies(mp, hostname)

        project = walle_test.mock_project({"id": self.TEST_PROJECT_ID})
        host_params = dict(
            state=HostState.MAINTENANCE, status=HostStatus.MANUAL, project=project.id, inv=self.INV, name=hostname
        )
        maintenance_properties = dict(ticket_key=self.MOCK_TICKET, operation_state=HostOperationState.DECOMMISSIONED)
        host = walle_test.mock_host(host_params, save=False)

        mock_host_adding_in_maintenance(
            host, maintenance_properties=maintenance_properties, manual=False, reason=self.MOCK_TICKET, task=False
        )

        result = self._run_stage()

        assert result.status == MarkerStatus.IN_PROGRESS

        walle_test.hosts.assert_equal()

    def test_all_hosts_already_in_db(self, mp, walle_test):
        add_host_mock = mp.function(host_operations.add_host)
        walle_test.mock_host({"inv": 1})

        result = self._run_stage()

        assert not add_host_mock.called
        assert result.status == MarkerStatus.SUCCESS

        walle_test.hosts.assert_equal()


class TestSetLabelStage:
    def test_run_stage(self, walle_test):
        host = walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED})
        label_name = "test_label"
        label_value = "test_value"

        hosts = {str(host.inv): ScenarioHostState(inv=host.inv)}
        scenario = Scenario(hosts=hosts, **get_scenario_params())
        stage_info = StageInfo()
        stage = SetLabelStage(**{ScriptArgs.LABEL_NAME: label_name, ScriptArgs.LABEL_VALUE: label_value})

        assert scenario.labels == {}
        assert stage.run(stage_info, scenario) == Marker.success(
            message="Set a value 'test_label' for the label 'test_value'"
        )
        assert scenario.labels == {label_name: label_value}

        walle_test.hosts.assert_equal()


class TestSetLabelIfAllScheduledStage:
    LABEL_NAME = "test_label"
    LABEL_VALUE = "if_all_scheduled"
    ALTERNATIVE_LABEL_VALUE = "if_not_all_scheduled"

    def _setup_scenario(self, hosts):
        scenario = Scenario(hosts=hosts, **get_scenario_params())
        stage = SetLabelIfAllScheduledStage(
            **{
                ScriptArgs.LABEL_NAME: self.LABEL_NAME,
                ScriptArgs.LABEL_VALUE: self.LABEL_VALUE,
                ScriptArgs.LABEL_VALUE_ALTERNATIVE: self.ALTERNATIVE_LABEL_VALUE,
            }
        )
        return scenario, stage

    @pytest.mark.parametrize("all_hosts_acquired", [True, False])
    def test_set_label_all_hosts(self, walle_test, all_hosts_acquired):
        hosts = [
            walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED}),
            walle_test.mock_host({"inv": 2, "state": HostState.ASSIGNED}),
        ]
        hosts = {str(host.inv): ScenarioHostState(inv=host.inv, is_acquired=all_hosts_acquired) for host in hosts}
        scenario, stage = self._setup_scenario(hosts)

        assert scenario.labels == {}
        if all_hosts_acquired:
            assert stage.run(StageInfo(), scenario) == Marker.success(
                message="Set a value 'test_label' for the label 'if_all_scheduled'"
            )
            assert scenario.labels == {self.LABEL_NAME: self.LABEL_VALUE}
        else:
            assert stage.run(StageInfo(), scenario) == Marker.success(
                message="Set a value 'test_label' for the label 'if_not_all_scheduled'"
            )
            assert scenario.labels == {self.LABEL_NAME: self.ALTERNATIVE_LABEL_VALUE}

        walle_test.hosts.assert_equal()

    def test_set_label_some_hosts_acquired(self, walle_test):
        hosts = [
            walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED}),
            walle_test.mock_host({"inv": 2, "state": HostState.ASSIGNED}),
        ]
        hosts = {
            str(hosts[0].inv): ScenarioHostState(inv=hosts[0].inv, is_acquired=True),
            str(hosts[1].inv): ScenarioHostState(inv=hosts[0].inv, is_acquired=False),
        }
        scenario, stage = self._setup_scenario(hosts)

        assert scenario.labels == {}
        assert stage.run(StageInfo(), scenario) == Marker.success(
            message="Set a value 'test_label' for the label 'if_not_all_scheduled'"
        )
        assert scenario.labels == {self.LABEL_NAME: self.ALTERNATIVE_LABEL_VALUE}

        walle_test.hosts.assert_equal()


class TestPrepareForWorkStage:
    @pytest.mark.parametrize("cms_allowed", [True, False])
    def test_assigned_ready_get_scheduled(self, walle_test, mp, monkeypatch_audit_log, cms_allowed):
        project = walle_test.mock_project(
            {
                "id": "mock-project-id",
                "cms_settings": [
                    {
                        "temporary_unreachable_enabled": cms_allowed,
                        "cms_api_version": cms.CmsApiVersion.V1_4,
                        "cms_tvm_app_id": 111111,
                        "cms": "test",
                    }
                ],
            }
        )
        host = walle_test.mock_host({"project": project.id, "state": HostState.ASSIGNED, "status": HostStatus.READY})

        scenario = Scenario(
            hosts={str(host.inv): ScenarioHostState(inv=host.inv)},
            stage_info=StageInfo(),
            current_group=0,
            **get_scenario_params(),
        )
        stage = PrepareForWorkStage()
        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Launched a task that sets host's state to 'maintenance'"
        )
        mock_schedule_maintenance(
            host,
            ignore_cms=False,
            power_off=False,
            manual=False,
            issuer=authorization.ISSUER_WALLE,
            ticket_key=scenario.ticket_key,
            cms_task_group="{}".format(scenario.id),
            reason=scenario.ticket_key,
            task_type=TaskType.AUTOMATED_ACTION,
            workdays_only=False,
            cms_task_action=cms.CmsTaskAction.TEMPORARY_UNREACHABLE,
            operation_state=HostOperationState.OPERATION,
        )

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", [HostState.FREE, HostState.MAINTENANCE])
    def test_free_and_maintenance_hosts_not_scheduled(self, walle_test, state):
        host = walle_test.mock_host({"inv": 1, "state": state, "status": HostStatus.READY})
        scenario = Scenario(
            hosts={str(host.inv): ScenarioHostState(inv=host.inv)}, stage_info=StageInfo(), **get_scenario_params()
        )
        stage = PrepareForWorkStage()
        stage_info = StageInfo()

        result_marker = stage.run(stage_info, scenario, host)
        assert result_marker.status == MarkerStatus.SUCCESS

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", [HostState.ASSIGNED, HostState.MAINTENANCE])
    def test_task_hosts_wait_for_task_to_finish(self, walle_test, state):
        host = walle_test.mock_host({"inv": 1, "state": state, "status": Operation.REBOOT.host_status})
        scenario = Scenario(
            hosts={str(host.inv): ScenarioHostState(inv=host.inv)}, stage_info=StageInfo(), **get_scenario_params()
        )
        stage = PrepareForWorkStage()
        stage_info = StageInfo()

        result_marker = stage.run(stage_info, scenario, host)
        assert result_marker.status == MarkerStatus.IN_PROGRESS

        walle_test.hosts.assert_equal()

    def test_wait_for_hosts_to_switch(self, walle_test):
        host = walle_test.mock_host(
            {"inv": 1, "state": HostState.ASSIGNED, "status": Operation.SWITCH_TO_MAINTENANCE.host_status}
        )
        scenario = Scenario(
            hosts={str(host.inv): ScenarioHostState(inv=host.inv)}, stage_info=StageInfo(), **get_scenario_params()
        )
        stage = PrepareForWorkStage()
        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Host has active task, scenario will wait until it ends"
        )

        walle_test.hosts.assert_equal()

    def test_host_switched_finished(self, walle_test):
        host = walle_test.mock_host({"inv": 1, "state": HostState.MAINTENANCE, "status": HostStatus.MANUAL})
        scenario = Scenario(
            hosts={str(host.inv): ScenarioHostState(inv=host.inv)}, stage_info=StageInfo(), **get_scenario_params()
        )
        stage = PrepareForWorkStage()
        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.success(
            message="Host is in 'maintenance' or 'free' state"
        )

        walle_test.hosts.assert_equal()


class TestSetAssignedStage:
    TICKET_KEY = "MOCK-0000"

    @staticmethod
    def setup_scenario(
        walle_test, state=HostState.ASSIGNED, status=HostStatus.READY, power_on=False, enabled_checks=None
    ):
        state_expire = None
        if state == HostState.MAINTENANCE:
            state_expire = StateExpire(
                time=timestamp() + MIN_STATE_TIMEOUT + 10 * MINUTE_SECONDS,
                ticket=TestSetAssignedStage.TICKET_KEY,
                status=HostStatus.READY,
                issuer=get_issuer(manual=False),
            )
        host = walle_test.mock_host(
            {
                "inv": 1,
                "state": state,
                "status": status,
                "ticket": TestSetAssignedStage.TICKET_KEY,
                "state_expire": state_expire,
                "state_author": get_issuer(manual=False),
            }
        )

        hosts = {str(host.inv): ScenarioHostState(inv=host.inv)}
        scenario = Scenario(hosts=hosts, stage_info=StageInfo(), **get_scenario_params())
        scenario.ticket_key = TestSetAssignedStage.TICKET_KEY
        scenario.hosts[host.uuid] = ScenarioHostState(enabled_checks=enabled_checks)

        stage = SetAssignedStage(power_on=power_on, use_specific_checks=True if enabled_checks else False)
        return scenario, stage, host

    def test_already_assigned(self, walle_test):
        scenario, stage, host = self.setup_scenario(walle_test)
        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(message="Host's state is set to 'assigned'")
        assert stage_info.action_type == StageAction.CHECK

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("power_on", [True, False])
    @pytest.mark.parametrize("enabled_checks", [None, [CheckType.SSH]])
    def test_maintenance_to_assigned_my_maintenance_stage_expire(
        self, walle_test, monkeypatch_audit_log, power_on, enabled_checks
    ):
        scenario, stage, host = self.setup_scenario(
            walle_test, HostState.MAINTENANCE, HostStatus.MANUAL, power_on, enabled_checks=enabled_checks
        )
        stage_info = StageInfo()

        host.ticket = "OTHER-TICKET"
        host.state_expire.ticket = self.TICKET_KEY
        host.save()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Launched task that sets host's state to 'assigned'"
        )
        mock_schedule_assigned(
            host,
            HostStatus.READY,
            manual=False,
            with_auto_healing=True,
            reason=self.TICKET_KEY,
            from_scenario=True,
            power_on=power_on,
            checks_for_use=enabled_checks,
        )
        assert stage_info.action_type == StageAction.CHECK

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("power_on", [True, False])
    def test_maintenance_to_assigned_my_maintenance_no_stage_expire(self, walle_test, monkeypatch_audit_log, power_on):
        scenario, stage, host = self.setup_scenario(walle_test, HostState.MAINTENANCE, HostStatus.MANUAL, power_on)
        stage_info = StageInfo()

        host.ticket = self.TICKET_KEY
        del host.state_expire
        host.save()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Launched task that sets host's state to 'assigned'"
        )
        mock_schedule_assigned(
            host,
            HostStatus.READY,
            manual=False,
            with_auto_healing=True,
            reason=self.TICKET_KEY,
            from_scenario=True,
            power_on=power_on,
        )
        assert stage_info.action_type == StageAction.CHECK

        walle_test.hosts.assert_equal()

    def test_maintenance_to_assigned_not_my_maintenance_stage_expire(self, walle_test, monkeypatch_audit_log):
        scenario, stage, host = self.setup_scenario(walle_test, HostState.MAINTENANCE, HostStatus.MANUAL)

        host.state_expire.ticket = "OTHER-TICKET"
        host.ticket = self.TICKET_KEY
        host.save()

        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Host's state was set to 'maintenance' state not by Wall-e, "
            "scenario has no permission to change it to 'assigned'"
        )
        walle_test.hosts.assert_equal()

    def test_maintenance_to_assigned_not_my_maintenance_no_stage_expire(self, walle_test, monkeypatch_audit_log):
        scenario, stage, host = self.setup_scenario(walle_test, HostState.MAINTENANCE, HostStatus.MANUAL)

        del host.state_expire
        host.ticket = "OTHER-TICKET"
        host.save()
        stage_info = StageInfo()

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Host's state was set to 'maintenance' state not by Wall-e, "
            "scenario has no permission to change it to 'assigned'"
        )
        walle_test.hosts.assert_equal()

    def test_check_assigned(self, walle_test, monkeypatch_audit_log):
        scenario, stage, host = self.setup_scenario(walle_test)
        stage_info = StageInfo(action_type=StageAction.CHECK)

        assert stage.run(stage_info, scenario, host) == Marker.success(message="Host is in 'assigned' state")

        walle_test.hosts.assert_equal()

    def test_check_in_progress(self, walle_test):
        scenario, stage, host = self.setup_scenario(
            walle_test, HostState.MAINTENANCE, Operation.SWITCH_TO_ASSIGNED.host_status
        )
        stage_info = StageInfo(action_type=StageAction.CHECK)

        assert stage.run(stage_info, scenario, host) == Marker.in_progress(
            message="Host has active task, scenario wait until it finishes"
        )

        walle_test.hosts.assert_equal()


class TestWaitForLabelOrTimeStage:
    expected_label_name = "test_label"
    expected_label_value = "test_value"

    def setup_scenario(self, walle_test, overrides=None):
        host = walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED})
        hosts = {str(host.inv): ScenarioHostState(inv=host.inv)}
        scenario = Scenario(hosts=hosts, **get_scenario_params())
        params = {ScriptArgs.IDLE_TIME: timestamp() + 100}
        if overrides:
            params.update(overrides)
        stage = WaitForLabelOrTimeStage(**params)
        return scenario, stage

    def test_user_label_set(self, walle_test):
        host = walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED})
        hosts = {str(host.inv): ScenarioHostState(inv=host.inv)}
        scenario = Scenario(hosts=hosts, **get_scenario_params())
        stage = WaitForLabelOrTimeStage(
            user_label=WORKMATE_STATUS_LABEL_NAME,
            user_label_target_value=WORKMATE_STATUS_TARGET_LABEL_VALUE,
        )
        scenario.labels = {WORKMATE_STATUS_LABEL_NAME: WORKMATE_STATUS_TARGET_LABEL_VALUE}

        assert stage.run(StageInfo(), scenario) == Marker.success(
            message=f"Label '{WORKMATE_STATUS_LABEL_NAME}' has expected value '{WORKMATE_STATUS_TARGET_LABEL_VALUE}'"
        )

    def test_label_with_wrong_value(self, walle_test):
        scenario, stage = self.setup_scenario(walle_test)
        scenario.labels = {self.expected_label_name: "wrong_value"}

        assert stage.run(StageInfo(), scenario).status == MarkerStatus.IN_PROGRESS

    def test_time_exposed(self, mp):
        monkeypatch_timestamp(mp, cur_time=0)

        stage_info = StageInfo()
        stage = WaitForLabelOrTimeStage(**{ScriptArgs.IDLE_TIME: MINUTE_SECONDS})

        assert stage._time_matched(stage_info) is False

        assert stage_info.data[WaitForLabelOrTimeStage.STAGE_END_TIME] == MINUTE_SECONDS

    @pytest.mark.parametrize(
        ["current_time", "result"], [[MINUTE_SECONDS, True], [MINUTE_SECONDS - 1, False], [MINUTE_SECONDS + 1, True]]
    )
    def test_is_time_matched(self, mp, current_time, result):
        monkeypatch_timestamp(mp, cur_time=current_time)

        stage_info = StageInfo(data={WaitForLabelOrTimeStage.STAGE_END_TIME: MINUTE_SECONDS})
        stage = WaitForLabelOrTimeStage(**{ScriptArgs.IDLE_TIME: MINUTE_SECONDS})

        assert stage._time_matched(stage_info) is result


class TestAddTagToTicketStage:
    @staticmethod
    def _get_mock_startrek_client(side_effect=None):
        mock_startrek_client = Mock()
        mock_startrek_client.attach_mock(Mock(side_effect=side_effect), "modify_issue")
        return mock_startrek_client

    @pytest.mark.parametrize(
        ("result_marker", "side_effect"),
        (
            (Marker.success(message="Added a tag 'mock-tag' to the ticket 'TEST-1'"), None),
            (Marker.in_progress(), startrek.StartrekClientError),
        ),
    )
    def test_run(self, mp, result_marker, side_effect):
        mock_startrek_client = self._get_mock_startrek_client(side_effect=side_effect)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = AddStartrekTagStage(tag="mock-tag")
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        if side_effect:
            with pytest.raises(side_effect):
                stage.run(StageInfo(), scenario)
        else:
            assert stage.run(StageInfo(), scenario) == result_marker

        mock_startrek_client.modify_issue.assert_called_once_with("TEST-1", {"tags": {"add": "mock-tag"}})


class TestAddMessageToTicketStage:
    @staticmethod
    def _get_mock_startrek_client(add_side_effect=None):
        mock_startrek_client = Mock()
        mock_startrek_client.attach_mock(Mock(side_effect=add_side_effect), "add_comment")
        return mock_startrek_client

    @pytest.mark.parametrize("template_path", list(TemplatePath))
    @pytest.mark.parametrize(
        ["result_marker", "side_effect"],
        [
            (Marker.success(message="Added a StarTrek message in the ticket 'TEST-1'"), None),
            (Marker.in_progress(), startrek.StartrekClientError),
        ],
    )
    def test_run(self, mp, template_path, result_marker, side_effect):
        mock_startrek_client = self._get_mock_startrek_client(add_side_effect=side_effect)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = AddStartrekMessageStage(template_path=template_path)
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        if side_effect:
            with pytest.raises(side_effect):
                stage.run(StageInfo(), scenario)
        else:
            assert stage.run(StageInfo(), scenario) == result_marker

        call_args = dict(
            text=JinjaTemplateRenderer().render_template(template_path, scenario_id=scenario.scenario_id),
            issue_id=scenario.ticket_key,
        )
        mock_startrek_client.add_comment.assert_called_once_with(**call_args)


class TestPrepareHostStage:

    TEST_PROJECT = "test-project-id"
    TEST_SEGMENT = "ext.mock"
    QLOUD_PROJECT = "qloud"
    PROFILE = "flexy"

    def _prepare_stage_scenario_and_stage_info(self, overrides={}):
        params = {ScriptArgs.TARGET_PROJECT: self.TEST_PROJECT}
        if overrides is not None:
            params.update(overrides)
        stage = PrepareHostStage(**params)
        scenario = Scenario(**get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "qloud_state,qloud_meta_ready,host_state,target_qloud_state,qloud_meta_called,marker",
        [
            (None, False, HostState.PROBATION, qloud.QloudHostStates.INITIAL, False, MarkerStatus.IN_PROGRESS),
            (
                qloud.QloudHostStates.UP,
                False,
                HostState.PROBATION,
                qloud.QloudHostStates.INITIAL,
                False,
                MarkerStatus.IN_PROGRESS,
            ),
            (None, False, HostState.ASSIGNED, qloud.QloudHostStates.INITIAL, False, MarkerStatus.IN_PROGRESS),
            (
                qloud.QloudHostStates.INITIAL,
                False,
                HostState.ASSIGNED,
                qloud.QloudHostStates.INITIAL,
                True,
                MarkerStatus.IN_PROGRESS,
            ),
            (
                qloud.QloudHostStates.INITIAL,
                True,
                HostState.ASSIGNED,
                qloud.QloudHostStates.UP,
                False,
                MarkerStatus.IN_PROGRESS,
            ),
            (qloud.QloudHostStates.UP, True, HostState.ASSIGNED, qloud.QloudHostStates.UP, False, MarkerStatus.SUCCESS),
        ],
    )
    def test_run_is_manipulating_qloud(
        self,
        qloud_client,
        walle_test,
        qloud_state,
        qloud_meta_ready,
        host_state,
        target_qloud_state,
        qloud_meta_called,
        marker,
    ):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(
            {ScriptArgs.TARGET_SEGMENT: self.TEST_SEGMENT}
        )
        host = walle_test.mock_host(dict(state=host_state, status=HostStatus.READY, project=self.TEST_PROJECT))
        if qloud_state is not None:
            qloud_client.add_host(host.name, self.TEST_SEGMENT, data={"state": qloud_state})
            qloud_client.find_host(host.name).is_data_filled = qloud_meta_ready

        result = stage.run(stage_info, scenario, host)

        q_host = qloud_client.find_host(host.name)
        assert all(
            (
                q_host.name == host.name,
                q_host.segment == self.TEST_SEGMENT,
                q_host.state == target_qloud_state,
                q_host.meta_called == qloud_meta_called,
            )
        )
        assert result.status == marker
        walle_test.hosts.assert_equal()

    def test_run_for_host_in_probation_ready(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(state=HostState.PROBATION, status=HostStatus.READY))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(
            message="Wait until host gets out from 'probation' state; host's status: 'ready'"
        )
        walle_test.hosts.assert_equal()

    def test_run_for_host_in_assigned_ready(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(state=HostState.ASSIGNED, status=HostStatus.READY, project=self.TEST_PROJECT))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(
            message="Host is in target project 'test-project-id', in 'assigned' state and in 'ready' status"
        )
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("ignore_cms", [True, False])
    def test_run_for_host_in_free_ready(self, walle_test, mp, monkeypatch_audit_log, ignore_cms):
        monkeypatch_function(mp, inventory.get_eine_profiles, module=inventory, return_value=[self.PROFILE])
        project = walle_test.mock_project(dict(profile=self.PROFILE, id=self.TEST_PROJECT))
        host = walle_test.mock_host(dict(state=HostState.FREE, project=project.id))

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(dict(ignore_cms=ignore_cms))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Launched a task that runs 'prepare' on the host")

        mock_schedule_host_preparing(
            host,
            profile=self.PROFILE,
            ignore_cms=True,
            disable_admin_requests=False,
            check=True,
            with_auto_healing=True,
            provisioner=PROVISIONER_EINE,
            config="project-deploy-config-mock",
            manual=False,
            update_firmware_needed=True,
            profile_mode=ProfileMode.DANGEROUS_HIGHLOAD_TEST,
        )
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_for_host_in_inappropriate_states(self, walle_test, state):
        project = walle_test.mock_project(dict(profile=self.PROFILE, id=self.TEST_PROJECT))
        host = walle_test.mock_host(dict(state=state, status=HostStatus.MANUAL, project=project.id))
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()

        with pytest.raises(InvalidHostStateError):
            stage.run(stage_info, scenario, host)

        walle_test.hosts.assert_equal()


class TestLiberateFromQloudHostStage:
    def _prepare_stage_scenario_and_stage_info(self):
        stage = LiberateFromQloudHostStage()
        scenario = Scenario(**get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    @pytest.mark.parametrize(
        "src_qloud_state,dst_qloud_state,marker_status",
        [
            (qloud.QloudHostStates.DOWN, None, MarkerStatus.IN_PROGRESS),
            (qloud.QloudHostStates.UP, qloud.QloudHostStates.DOWN, MarkerStatus.IN_PROGRESS),
            (None, None, MarkerStatus.SUCCESS),
        ],
    )
    def test_liberate_from_qloud(self, qloud_client, walle_test, src_qloud_state, dst_qloud_state, marker_status):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = mock_host_inside_qloud(walle_test, qloud_client, qloud_state=src_qloud_state)

        result = stage.run(stage_info, scenario, host)
        assert result.status == marker_status
        qloud_host = qloud_client.find_host(host.name)
        assert qloud_host is None and dst_qloud_state is None or qloud_host.state == dst_qloud_state


class TestSwitchProjectHostStage:

    TEST_PROJECT = "test-project-id"
    TEST_SEGMENT = "ext.mock"
    QLOUD_PROJECT = "qloud"

    def _prepare_stage_scenario_and_stage_info(self, overrides={}):
        params = {ScriptArgs.TARGET_PROJECT: self.TEST_PROJECT}
        if overrides is not None:
            params.update(overrides)
        stage = SwitchProjectHostStage(**params)
        scenario = Scenario(**get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_run_for_host_in_target_project(self, walle_test):
        project = walle_test.mock_project(dict(id=self.TEST_PROJECT))
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(project=project.id))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host is in target project 'test-project-id'")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "target_project,target_segment,expected_project",
        [
            (TEST_PROJECT, None, TEST_PROJECT),
            (TEST_PROJECT, TEST_SEGMENT, TEST_PROJECT),
            (None, TEST_SEGMENT, QLOUD_PROJECT),
        ],
    )
    def test_run_for_host_not_in_target_project(
        self, walle_test, monkeypatch_audit_log, target_project, target_segment, expected_project
    ):
        if target_project is None:
            walle_test.mock_project(dict(id=self.QLOUD_PROJECT))
        else:
            walle_test.mock_project(dict(id=target_project))
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(
            {ScriptArgs.TARGET_PROJECT: target_project, ScriptArgs.TARGET_SEGMENT: target_segment}
        )
        host = walle_test.mock_host(dict(state=HostState.MAINTENANCE))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Launched a task that switches project of the host")

        mock_schedule_project_switching(
            host,
            expected_project,
            release=True,
            force_new_cms_task=False,
            manual=False,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )
        walle_test.hosts.assert_equal()

    def test_run_for_host_in_probation_not_in_target_project(self, walle_test):
        walle_test.mock_project(dict(id=self.TEST_PROJECT))
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(state=HostState.PROBATION))

        with pytest.raises(InvalidHostStateError):
            stage.run(stage_info, scenario, host)

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "src_qloud_state,dst_qloud_state,switch_task",
        [
            (qloud.QloudHostStates.DOWN, None, False),
            (qloud.QloudHostStates.UP, qloud.QloudHostStates.DOWN, False),
            (None, None, True),
        ],
    )
    def test_run_for_host_in_qloud(
        self, qloud_client, walle_test, monkeypatch_audit_log, src_qloud_state, dst_qloud_state, switch_task
    ):
        walle_test.mock_project(dict(id=self.TEST_PROJECT))
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = mock_host_inside_qloud(walle_test, qloud_client, qloud_state=src_qloud_state)

        result = stage.run(stage_info, scenario, host)
        assert result.status == MarkerStatus.IN_PROGRESS
        qloud_host = qloud_client.find_host(host.name)
        assert qloud_host is None and dst_qloud_state is None or qloud_host.state == dst_qloud_state
        if switch_task:
            mock_schedule_project_switching(
                host,
                self.TEST_PROJECT,
                release=True,
                ignore_cms=True,
                force_new_cms_task=False,
                manual=False,
                task_type=TaskType.AUTOMATED_ACTION,
            )
        walle_test.hosts.assert_equal()


class TestSwitchToMaintenanceHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(
        self, operation_state=HostOperationState.DECOMMISSIONED, power_off=True, workdays_only=True
    ):
        stage = SwitchToMaintenanceHostStage(
            operation_state=operation_state, power_off=power_off, workdays_only=workdays_only
        )
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def _mock_change_maintenance(self, host, scenario, operation_state):
        host.state_expire.ticket = scenario.ticket_key
        host.operation_state = operation_state
        return host

    @staticmethod
    def _get_mock_startrek_client():
        mock_startrek_client = Mock()
        mock_startrek_client.attach_mock(Mock(return_value={"status": {"key": TicketStatus.OPEN}}), "get_issue")
        return mock_startrek_client

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario will wait until it ends")
        walle_test.hosts.assert_equal()

    def test_host_in_maintenance_by_scenario_with_power_off(self, mp, walle_test):
        monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=False)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(
                state=HostState.MAINTENANCE,
                state_author=get_issuer(manual=True),
                ticket=scenario.ticket_key,
                operation_state=HostOperationState.DECOMMISSIONED,
            )
        )

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host is in 'maintenance' or 'free' state")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("operation_state", HostOperationState.ALL)
    @pytest.mark.parametrize("power_off", [True, False])
    @pytest.mark.parametrize("workdays_only", [True, False, None])
    @pytest.mark.parametrize("state", [HostState.ASSIGNED, HostState.PROBATION])
    def test_schedule_maintenance(
        self, walle_test, mp, monkeypatch_audit_log, operation_state, power_off, workdays_only, state
    ):
        monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=True)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(
            operation_state=operation_state, power_off=power_off, workdays_only=workdays_only
        )
        host = walle_test.mock_host({"state": state})

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Launched a task that sets host's state to 'maintenance'")

        mock_schedule_maintenance(
            host,
            ignore_cms=True,
            manual=False,
            power_off=power_off,
            issuer=authorization.ISSUER_WALLE,
            ticket_key=scenario.ticket_key,
            cms_task_group="{}".format(scenario.id),
            reason=scenario.ticket_key,
            operation_state=operation_state,
            workdays_only=workdays_only,
            task_type=TaskType.AUTOMATED_ACTION,
        )
        walle_test.hosts.assert_equal()

    def test_change_maintenance_for_host_in_maintenance(self, walle_test, mp, monkeypatch_audit_log):
        NOT_SCENARIO_TICKET = "WRONG_TICKET"

        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
        monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=False)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(
                state=HostState.MAINTENANCE,
                state_author=TestCase.api_issuer,
                operation_state=HostOperationState.OPERATION,
                state_expire=StateExpire(
                    ticket=NOT_SCENARIO_TICKET, issuer=TestCase.api_issuer, status=HostStatus.READY
                ),
            )
        )

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host is in 'maintenance' or 'free' state")

        self._mock_change_maintenance(host, scenario, operation_state=HostOperationState.DECOMMISSIONED)

        walle_test.hosts.assert_equal()


class TestPowerOffHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self, operation_state=HostOperationState.DECOMMISSIONED):
        stage = PowerOffHostStage(operation_state=operation_state)
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def _mock_change_maintenance(self, host, scenario, operation_state):
        host.state_expire.ticket = host.ticket or scenario.ticket_key
        host.operation_state = operation_state
        host.status_reason = scenario.ticket_key
        return host

    @staticmethod
    def _get_mock_startrek_client():
        mock_startrek_client = Mock()
        mock_startrek_client.attach_mock(Mock(return_value={"status": {"key": TicketStatus.OPEN}}), "get_issue")
        return mock_startrek_client

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        ["is_power_on", "ticket_key", "expected_result_marker", "are_host_changes_expected"],
        [
            (True, TICKET_KEY, Marker.in_progress(message="Launched a task that powers off the host"), True),
            (True, "WRONG_TICKET", Marker.in_progress(message="Launched a task that powers off the host"), True),
            (False, TICKET_KEY, Marker.success(message="Host is powered off"), False),
            (False, "WRONG_TICKET", Marker.success(message="Host is powered off"), False),
        ],
    )
    @pytest.mark.parametrize("operation_state", HostOperationState.ALL)
    def test_schedule_power_off_for_maintenance_host(
        self,
        walle_test,
        mp,
        monkeypatch_audit_log,
        is_power_on,
        ticket_key,
        expected_result_marker,
        are_host_changes_expected,
        operation_state,
    ):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
        monkeypatch_method(
            mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=is_power_on
        )

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(
                state=HostState.MAINTENANCE,
                state_author=TestCase.api_issuer,
                operation_state=operation_state,
                state_expire=StateExpire(ticket=ticket_key, issuer=TestCase.api_issuer, status=HostStatus.READY),
            )
        )

        result = stage.run(stage_info, scenario, host)

        assert result == expected_result_marker

        mock_schedule_host_power_off(
            host,
            reason=scenario.ticket_key,
            with_auto_healing=False,
            manual=False,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )
        self._mock_change_maintenance(host, scenario, operation_state=HostOperationState.DECOMMISSIONED)

        if are_host_changes_expected:
            walle_test.hosts.assert_equal()

    def test_schedule_power_off_for_free_host(self, walle_test, mp, monkeypatch_audit_log):
        monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=True)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(state=HostState.FREE))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Launched a task that powers off the host")

        mock_schedule_host_power_off(
            host,
            reason=scenario.ticket_key,
            with_auto_healing=False,
            manual=False,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("ipmi_exception", [ipmiproxy.IpmiHostMissingError("mock"), NonIpmiHostError()])
    @pytest.mark.parametrize("operation_state", HostOperationState.ALL)
    def test_power_off_for_maintenance_host_ipmi_error(
        self, walle_test, mp, monkeypatch_audit_log, ipmi_exception, operation_state
    ):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
        monkeypatch_method(
            mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, side_effect=ipmi_exception
        )

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(
                state=HostState.MAINTENANCE,
                state_author=TestCase.api_issuer,
                operation_state=operation_state,
                state_expire=StateExpire(ticket="TEST-1", issuer=TestCase.api_issuer, status=HostStatus.READY),
            )
        )

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Launched a task that powers off the host")

        mock_schedule_host_power_off(
            host,
            reason=scenario.ticket_key,
            with_auto_healing=False,
            manual=False,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )
        self._mock_change_maintenance(host, scenario, operation_state=HostOperationState.DECOMMISSIONED)

        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "host_params",
        [
            dict(state=HostState.MAINTENANCE, ticket=TICKET_KEY, operation_state=HostOperationState.DECOMMISSIONED),
            dict(state=HostState.FREE),
        ],
    )
    def test_host_already_powered_off_and_in_right_condition(self, walle_test, mp, host_params):
        monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=False)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()

        host = walle_test.mock_host(host_params)

        result = stage.run(stage_info, scenario, host)

        assert result.status == MarkerStatus.SUCCESS

        walle_test.hosts.assert_equal()


class TestExecuteTicketTransitionStage:
    @staticmethod
    def _get_mock_startrek_client(ticket_status=TicketStatus.OPEN, side_effect=None):
        mock_startrek_client = Mock()
        mock_startrek_client.attach_mock(Mock(return_value={"status": {"key": ticket_status}}), "get_issue")
        mock_startrek_client.attach_mock(Mock(side_effect=side_effect), "execute_transition")
        return mock_startrek_client

    @pytest.mark.parametrize("transition", [TicketTransition.READY_FOR_DEV, TicketTransition.IN_PROGRESS])
    def test_execute_transition_to_intermediate_state_successfully_with_ticket_not_in_target_state(
        self, mp, transition
    ):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = ExecuteTicketTransitionStage(ticket_transition_state=transition)
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        assert stage.run(None, scenario).status == MarkerStatus.SUCCESS

        mock_startrek_client.get_issue.assert_called_once_with(scenario.ticket_key)
        mock_startrek_client.execute_transition.assert_called_once_with(
            issue_id=scenario.ticket_key, transition=transition
        )

    def test_execute_transition_to_close_successfully_with_not_closed_ticket(self, mp):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = ExecuteTicketTransitionStage(
            ticket_transition_state=TicketTransition.CLOSE, ticket_transition_resolution=TICKET_RESOLUTION
        )
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        assert stage.run(None, scenario).status == MarkerStatus.SUCCESS

        mock_startrek_client.get_issue.assert_called_once_with(scenario.ticket_key)
        mock_startrek_client.execute_transition.assert_called_once_with(
            issue_id=scenario.ticket_key,
            transition=TicketTransition.CLOSE,
            issue_params={"resolution": TICKET_RESOLUTION},
        )

    def test_execute_transition_successfully_with_ticket_in_target_state(self, mp):
        mock_startrek_client = self._get_mock_startrek_client(ticket_status=TicketTransition.READY_FOR_DEV)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = ExecuteTicketTransitionStage(ticket_transition_state=TicketTransition.READY_FOR_DEV)
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        assert stage.run(None, scenario).status == MarkerStatus.SUCCESS

        mock_startrek_client.get_issue.assert_called_once_with(scenario.ticket_key)
        assert not mock_startrek_client.execute_transition.called

    def test_execute_transition_with_startrek_exc(self, mp):
        mock_startrek_client = self._get_mock_startrek_client(side_effect=startrek.StartrekClientError("error"))
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = ExecuteTicketTransitionStage(ticket_transition_state=TicketTransition.READY_FOR_DEV)
        scenario = Scenario(scenario_id=1, ticket_key="TEST-1")

        with pytest.raises(startrek.StartrekClientError):
            stage.run(None, scenario)

        mock_startrek_client.get_issue.assert_called_once_with(scenario.ticket_key)
        mock_startrek_client.execute_transition.assert_called_once_with(
            issue_id=scenario.ticket_key, transition=TicketTransition.READY_FOR_DEV
        )


class TestWaitStateStatusHostStage:
    @pytest.mark.parametrize("project", ["a", "aaa"])
    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    @pytest.mark.parametrize("status", HostStatus.ALL_ASSIGNED)
    def test_run_success(self, walle_test, project, state, status):
        host = walle_test.mock_host(dict(inv=1, project=project, status=status, state=state))
        scenario = mock_scenario(
            hosts={
                str(host.inv): ScenarioHostState(inv=host.inv, status=HostScenarioStatus.QUEUE, timestamp=timestamp())
            }
        )
        stage = WaitStateStatusHostStage(target_project=project, wait_state=state, wait_status=status)

        result = stage.run(None, scenario, host)

        assert result == Marker.success(
            message="Host is in target project '{}', "
            "in required state '{}' and status '{}'".format(project, state, status)
        )

    @pytest.mark.parametrize(
        ["host_project", "host_status", "host_state"],
        [
            ("a", HostStatus.DEAD, HostState.FREE),
            ("b", HostStatus.READY, HostState.FREE),
            ("b", HostStatus.DEAD, HostState.ASSIGNED),
        ],
    )
    def test_run_in_progress(self, walle_test, host_project, host_status, host_state):
        host = walle_test.mock_host(dict(inv=1, project=host_project, status=host_status, state=host_state))
        scenario = mock_scenario(
            hosts={
                str(host.inv): ScenarioHostState(inv=host.inv, status=HostScenarioStatus.QUEUE, timestamp=timestamp())
            }
        )
        stage = WaitStateStatusHostStage(target_project="b", wait_state=HostState.FREE, wait_status=HostStatus.DEAD)

        result = stage.run(None, scenario, host)

        assert result.status == MarkerStatus.IN_PROGRESS


class TestProfileHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self):
        stage = ProfileHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_already_had_profile(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        log = ObjectMocker(OperationLog)

        log.mock(
            dict(
                id="1",
                audit_log_id="1",
                host_inv=host.inv,
                host_name=host.name,
                type=Operation.PROFILE.type,
                params=None,
                time=timestamp(),
                scenario_id=scenario.scenario_id,
            )
        )

        log.assert_equal()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="'Profile' task finished")
        walle_test.hosts.assert_equal()
        log.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_with_profiling(self, walle_test, monkeypatch_audit_log, state):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(status=HostStatus.READY, state=state))

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_profiling(
            host,
            task_type=TaskType.AUTOMATED_ACTION,
            manual=False,
            profile_mode=ProfileMode.SWP_UP,
            reason=scenario.ticket_key,
            ignore_cms=True,
            force_update_network_location=True,
        )

        assert result == Marker.in_progress(message="Launched a task that runs 'profile' on the host")
        walle_test.hosts.assert_equal()


class TestSwitchVlansHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self):
        stage = SwitchVlansHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_already_had_switch_vlans(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        log = ObjectMocker(OperationLog)

        log.mock(
            dict(
                id="1",
                audit_log_id="1",
                host_inv=host.inv,
                host_name=host.name,
                type=Operation.SWITCH_VLANS.type,
                params=None,
                time=timestamp(),
                scenario_id=scenario.scenario_id,
            )
        )

        log.assert_equal()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Switched VLANs of the host")
        walle_test.hosts.assert_equal()
        log.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_with_switch_vlans(self, walle_test, monkeypatch_audit_log, state):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(status=HostStatus.READY, state=state))

        result = stage.run(stage_info, scenario, host)

        mock_schedule_switch_vlans(
            host,
            manual=False,
            network=NetworkTarget.PROJECT,
            reason=scenario.ticket_key,
            task_type=TaskType.AUTOMATED_ACTION,
        )

        assert result == Marker.in_progress(message="Launched a task that switches VLANs of the host")
        walle_test.hosts.assert_equal()


class TestRedeployHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self):
        stage = RedeployHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_already_had_redeploy(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        log = ObjectMocker(OperationLog)

        log.mock(
            dict(
                id="1",
                audit_log_id="1",
                host_inv=host.inv,
                host_name=host.name,
                type=Operation.REDEPLOY.type,
                params=None,
                time=timestamp(),
                scenario_id=scenario.scenario_id,
            )
        )

        log.assert_equal()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="'Redeploy' task finished")
        walle_test.hosts.assert_equal()
        log.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_with_redeploy(self, walle_test, monkeypatch_audit_log, state):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(status=HostStatus.READY, state=state, provisioner=PROVISIONER_LUI, config="mock-config")
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_redeployment(
            host,
            task_type=TaskType.AUTOMATED_ACTION,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that runs 'redeploy' on the host")
        walle_test.hosts.assert_equal()


class TestRebootHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self):
        stage = RebootHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_already_had_reboot(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        log = ObjectMocker(OperationLog)

        log.mock(
            dict(
                id="1",
                audit_log_id="1",
                host_inv=host.inv,
                host_name=host.name,
                type=Operation.REBOOT.type,
                params=None,
                time=timestamp(),
                scenario_id=scenario.scenario_id,
            )
        )

        log.assert_equal()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host was rebooted")
        walle_test.hosts.assert_equal()
        log.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_with_reboot(self, walle_test, monkeypatch_audit_log, state):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(
            dict(status=HostStatus.READY, state=state, provisioner=PROVISIONER_LUI, config="mock-config")
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_reboot(
            host,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that reboots the host")
        walle_test.hosts.assert_equal()


class TestCheckDnsHostStage:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self):
        stage = CheckDnsHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_already_had_check_dns(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        log = ObjectMocker(OperationLog)

        log.mock(
            dict(
                id="1",
                audit_log_id="1",
                host_inv=host.inv,
                host_name=host.name,
                type=Operation.CHECK_DNS.type,
                params=None,
                time=timestamp(),
                scenario_id=scenario.scenario_id,
            )
        )

        log.assert_equal()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host's DNS checked")
        walle_test.hosts.assert_equal()
        log.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_run_with_check_dns(self, walle_test, monkeypatch_audit_log, state):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        project = walle_test.mock_project(
            {"id": "search-project-mock", "name": "search project mock", "dns_domain": "fake.yandex.net"}
        )
        host = walle_test.mock_host(
            dict(
                status=HostStatus.READY,
                state=state,
                project=project.id,
                name="mock.non-existent.some-project.fake.yandex.net",
            )
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_check_host_dns(
            host, manual=False, task_type=TaskType.AUTOMATED_ACTION, with_auto_healing=True, reason=scenario.ticket_key
        )

        assert result == Marker.in_progress(message="Launched a task that checks host's DNS")
        walle_test.hosts.assert_equal()


class TestaitEineProfileHostStage:

    TICKET_KEY = "TEST-1"

    @staticmethod
    def _get_mock_eine_host(has_profile=False, in_process=False, profile=EINE_NOP_PROFILE):
        eine_host = Mock()
        eine_host.attach_mock(Mock(return_value=has_profile), "has_profile")
        eine_host.attach_mock(Mock(return_value=in_process), "in_process")
        eine_host.attach_mock(Mock(return_value=profile), "profile")
        return eine_host

    def _prepare_stage_scenario_and_stage_info(self):
        stage = WaitEineProfileHostStage()
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_host_has_running_profile_in_eine(self, walle_test, mp):
        eine_host_mock = self._get_mock_eine_host(has_profile=True, in_process=True, profile=FLEXY_EINE_PROFILE)
        mp.method(eine.EineClient.get_host_status, return_value=eine_host_mock, obj=eine.EineClient)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Waits until Eine profiles the host")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize(
        ["has_profile", "in_process", "profile"],
        [(True, True, EINE_NOP_PROFILE), (True, False, FLEXY_EINE_PROFILE), (False, True, FLEXY_EINE_PROFILE)],
    )
    def test_host_hasnt_running_profile_in_eine(self, walle_test, mp, has_profile, in_process, profile):
        eine_host_mock = self._get_mock_eine_host(has_profile, in_process, profile)
        mp.method(eine.EineClient.get_host_status, return_value=eine_host_mock, obj=eine.EineClient)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Eine profiled the host")
        walle_test.hosts.assert_equal()


class TestReleaseHostStage:
    def _prepare_stage_scenario_and_stage_info(self, ignore_cms=False):
        stage = ReleaseHostStage(ignore_cms=ignore_cms)
        scenario = Scenario(**get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    def test_run_for_host_in_free_state(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(state=HostState.FREE))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(message="Host released")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", [HostState.ASSIGNED, HostState.ASSIGNED, HostState.PROBATION])
    @pytest.mark.parametrize("ignore_cms", [True, False])
    def test_run_for_host_not_in_free_state(self, walle_test, monkeypatch_audit_log, state, ignore_cms):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(ignore_cms=ignore_cms)
        host = walle_test.mock_host(dict(state=state))

        result = stage.run(stage_info, scenario, host)
        mock_schedule_release_host(host, manual=False, task_type=TaskType.AUTOMATED_ACTION, ignore_cms=True)

        assert result == Marker.in_progress(message="Launched a task that releases the host")
        walle_test.hosts.assert_equal()


class TestSetABCServiceIdStage:
    def _prepare_stage_scenario_and_stage_info(
        self, abc_service_id=1, target_bot_project_id=None, tried_to_change_id=False
    ):
        stage = SetABCServiceIdStage(abc_service_id=abc_service_id)
        scenario = Scenario(**get_scenario_params())
        stage_info = StageInfo()

        if target_bot_project_id:
            stage_info.data[SetABCServiceIdStage.TARGET_BOT_PROJECT_ID] = target_bot_project_id
        if tried_to_change_id:
            stage_info.data[SetABCServiceIdStage.TRIED_TO_CHANGE_SERVICE_ID] = True

        return stage, scenario, stage_info

    def test_run_for_host_with_task(self, walle_test):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info()
        host = walle_test.mock_host(dict(task=mock_task()))

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Host has active task, scenario waits until it finishes")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("target_bot_project_id", [None, 1])
    def test_assign_bot_project(self, walle_test, mp, target_bot_project_id):
        monkeypatch_function(mp, bot.get_bot_project_id_by_planner_id, module=bot, return_value=1)
        monkeypatch_function(mp, bot.assign_project_id, module=bot)

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(
            target_bot_project_id=target_bot_project_id
        )
        host = walle_test.mock_host()

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.in_progress(message="Setting new host's ABC service in Bot")
        assert stage_info.data[SetABCServiceIdStage.TARGET_BOT_PROJECT_ID] == 1
        assert stage_info.data[SetABCServiceIdStage.TRIED_TO_CHANGE_SERVICE_ID] is True
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("assigned_bot_id", [1, 2])
    def test_is_service_id_appointed(self, walle_test, mp, assigned_bot_id):
        monkeypatch_function(mp, bot.get_host_info, module=bot, return_value={"bot_project_id": assigned_bot_id})

        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(
            target_bot_project_id=1, tried_to_change_id=True
        )
        host = walle_test.mock_host()

        result = stage.run(stage_info, scenario, host)

        if assigned_bot_id == 1:
            assert result == Marker.success(message="New host's ABC service in Bot was set")
        else:
            assert result == Marker.in_progress(message="Waiting for new host's ABC service to appear in Bot")
        walle_test.hosts.assert_equal()


class TestCollectHealthChecksStage:
    def test_run(self, walle_test, mp):
        script = ScenarioRootStage([ScenarioRootStage([make_mock_stage(), make_mock_stage()]), make_mock_stage()])
        stage = CollectHealthChecksStage()
        scenario = Scenario(stage_info=script.serialize(), **get_scenario_params()).save()
        stage_info = StageInfo()
        health = HealthStatus(
            status=HealthStatus.STATUS_OK,
            check_statuses={CheckType.SSH: CheckStatus.PASSED, CheckType.UNREACHABLE: CheckStatus.FAILED},
        )
        host = walle_test.mock_host(dict(health=health))
        scenario.hosts[host.uuid] = ScenarioHostState()

        assert not scenario.hosts[host.uuid].enabled_checks

        result = stage.run(stage_info, scenario, host)
        scenario.save()

        assert result == Marker.success(message="Host's checks collected")
        scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
        assert scenario.hosts[host.uuid].enabled_checks == [CheckType.SSH]


class TestOptionalRedeployHostStage:

    TICKET_KEY = "TEST-1"

    def _get_mocked_host(
        self, walle_test, host_params=None, link_plot_to_project=True, enable_redeploy_after_change_of_mac_address=True
    ):
        if not host_params:
            host_params = {}

        scenarios_settings = [
            {
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "settings": {
                    "request_cms_x_seconds_before_maintenance_start_time": 86 * 60 * 60,
                    "enable_redeploy_after_change_of_mac_address": enable_redeploy_after_change_of_mac_address,
                },
            },
        ]
        plot = walle_test.maintenance_plots.mock({"id": "plot-id", "scenarios_settings": scenarios_settings})
        if link_plot_to_project:
            project = walle_test.mock_project({"id": "some-id", "maintenance_plot_id": plot.id})
        else:
            project = walle_test.mock_project({"id": "some-id", "maintenance_plot_id": None})

        host_params["project"] = project.id
        host = walle_test.mock_host(host_params)
        return host

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_successfull_redeploy_if_project_doesnt_have_maintenance_plot(
        self, walle_test, state, monkeypatch_audit_log
    ):
        stage = OptionalRedeployHostStage()
        scenario = Scenario(
            ticket_key=self.TICKET_KEY, **get_scenario_params(scenario_type=ScriptName.ITDC_MAINTENANCE)
        )
        stage_info = StageInfo()
        host_params = dict(status=HostStatus.READY, state=state, provisioner=PROVISIONER_LUI, config="mock-config")
        host = self._get_mocked_host(walle_test, link_plot_to_project=False, host_params=host_params)

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_redeployment(
            host,
            task_type=TaskType.AUTOMATED_ACTION,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that runs 'redeploy' on the host")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_successfull_redeploy_if_maintenance_plot_allow_it(self, walle_test, state, monkeypatch_audit_log):
        stage = OptionalRedeployHostStage()
        scenario = Scenario(
            ticket_key=self.TICKET_KEY, **get_scenario_params(scenario_type=ScriptName.ITDC_MAINTENANCE)
        )
        stage_info = StageInfo()
        host_params = dict(status=HostStatus.READY, state=state, provisioner=PROVISIONER_LUI, config="mock-config")
        host = self._get_mocked_host(walle_test, link_plot_to_project=True, host_params=host_params)

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_redeployment(
            host,
            task_type=TaskType.AUTOMATED_ACTION,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that runs 'redeploy' on the host")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
    def test_stage_success_end_if_maintenance_plot_doesnt_allow_redeploy(
        self, walle_test, state, monkeypatch_audit_log
    ):
        stage = OptionalRedeployHostStage()
        scenario = Scenario(
            ticket_key=self.TICKET_KEY, **get_scenario_params(scenario_type=ScriptName.ITDC_MAINTENANCE)
        )
        stage_info = StageInfo()
        host_params = dict(status=HostStatus.READY, state=state, provisioner=PROVISIONER_LUI, config="mock-config")
        host = self._get_mocked_host(
            walle_test,
            link_plot_to_project=True,
            host_params=host_params,
            enable_redeploy_after_change_of_mac_address=False,
        )

        result = stage.run(stage_info, scenario, host)

        assert result == Marker.success(
            message="'Redeploy' task disabled by maintenance plot with id {}".format("plot-id")
        )
        walle_test.hosts.assert_equal()


class TestCallOnResponsibleForLoadReturn:
    MOCK_MAINTENANCE_PLOT_ID = "mock-plot"
    MOCK_TICKET_KEY = "mock-ticket"

    @staticmethod
    def _get_stage_and_stage_info():
        stage = CallOnResponsibleForLoadReturn()
        stage_info = StageInfo()
        return stage, stage_info

    def _mock_scenario(
        self,
        walle_test,
        enable_manual_approval_after_hosts_power_off=True,
    ) -> Scenario:
        itdc_maintenance_settings = MaintenancePlotScenarioSettings(
            scenario_type=ScriptName.ITDC_MAINTENANCE,
            settings=maintenance_plot_constants.SCENARIO_TYPES_SETTINGS_MAP.get(ScriptName.ITDC_MAINTENANCE)(
                enable_manual_approval_after_hosts_power_off=enable_manual_approval_after_hosts_power_off,
            ),
        )

        meta_info = MaintenancePlotMetaInfo(abc_service_slug="abc-test", name="test")

        walle_test.mock_maintenance_plot(
            dict(
                id=self.MOCK_MAINTENANCE_PLOT_ID,
                meta_info=meta_info.to_dict(),
                scenarios_settings=[itdc_maintenance_settings.to_dict()],
            )
        )

        project = walle_test.mock_project(
            dict(maintenance_plot_id=self.MOCK_MAINTENANCE_PLOT_ID, id="test-plot-options")
        )
        host = walle_test.mock_host(dict(inv=1, project=project.id))

        scenario = walle_test.mock_scenario(
            {"scenario_type": ScriptName.ITDC_MAINTENANCE, "hosts": [host.inv], "ticket_key": self.MOCK_TICKET_KEY}
        )
        data_storage = get_data_storage(scenario)

        host_groups_sources = [
            HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id=self.MOCK_MAINTENANCE_PLOT_ID)),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        return scenario

    def test_add_messages_with_responsibles(self, walle_test, mp, startrek_client):
        scenario = self._mock_scenario(walle_test, enable_manual_approval_after_hosts_power_off=True)
        stage, stage_info = self._get_stage_and_stage_info()

        assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS

        startrek_client.add_comment.assert_called_once_with(
            issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=ANY
        )

    def test_skip_stage(self, walle_test, mp, startrek_client):
        scenario = self._mock_scenario(walle_test, enable_manual_approval_after_hosts_power_off=False)
        stage, stage_info = self._get_stage_and_stage_info()

        assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
        startrek_client.add_comment.assert_not_called()
