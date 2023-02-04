"""Integration tests for scenarios."""
import http.client
from unittest.mock import Mock, call

import pytest

from infra.walle.server.tests.lib.scenario_util import launch_scenario
from infra.walle.server.tests.lib.util import (
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_method,
    TestCase,
)
from walle import authorization
from walle.clients import inventory, bot, ipmiproxy
from walle.clients.qloud import QloudHostStates
from walle.constants import FLEXY_EINE_PROFILE
from walle.hosts import HostLocation, Host, HostState, HostStatus, HostOperationState
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.physical_location_tree import LocationNamesMap
from walle.scenario.constants import (
    ScriptArgs,
    ScenarioFsmStatus,
    TicketStatus,
    TemplatePath,
    TICKET_RESOLUTION,
    TicketTransition,
    SchedulerName,
)
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario, HostStageStatus

# noinspection PyProtectedMember
from walle.scenario.scenario_fsm import _run_scenario
from walle.scenario.script import hosts_add_script
from walle.scenario.stage.approve_stage import ApproveStage
from walle.scenario.stage_info import StageAction, StageStatus
from walle.scenario.stages import WaitForLabelOrTimeStage
from walle.stages import Stages
from walle.util.template_loader import JinjaTemplateRenderer

DC_NAMES = ("a", "dcmock")
SOURCE_PROJECT_ID = "source"
TARGET_PROJECT_ID = "target"
TARGET_SEGMENT = "ext.mock"


@pytest.fixture
def test(mp, monkeypatch_timestamp, monkeypatch_locks, authorized_scenario_user, startrek_client, request):
    mp.function(inventory.get_eine_profiles, return_value=["profile-mock", FLEXY_EINE_PROFILE])
    mp.function(inventory.check_deploy_configuration)
    mp.function(bot.missed_preordered_hosts, return_value={})
    mp.config("scenario.stages.add_hosts_stage.project", SOURCE_PROJECT_ID)

    monkeypatch_method(mp, ipmiproxy.IpmiProxyClient.is_power_on, obj=ipmiproxy.IpmiProxyClient, return_value=False)

    monkeypatch_inventory_get_host_info_and_check_status(mp)
    return TestCase.create(request)


def expand_startrek_client_mock(mock_startrek_client):
    mock_startrek_client.attach_mock(Mock(return_value={"status": {"key": TicketStatus.OPEN}}), "get_issue")
    mock_startrek_client.attach_mock(Mock(), "execute_transition")
    mock_startrek_client.attach_mock(Mock(), "add_comment")
    return mock_startrek_client


def _make_hosts_for_scenario_test(test, source_project, count_per_dc=5):
    hosts = []
    next_id = 0
    location_for_dc = {
        dc: HostLocation(
            country="country-mock",
            city="city-mock",
            datacenter=dc,
            queue="queue-mock",
            rack="rack-mock",
            physical_timestamp=timestamp(),
        )
        for dc in DC_NAMES
    }

    for dc in DC_NAMES:
        for i in range(count_per_dc):
            hosts.append(
                test.mock_host(
                    {
                        "state": HostState.ASSIGNED,
                        "inv": next_id,
                        "project": source_project.id,
                        "location": location_for_dc[dc],
                    }
                )
            )
            next_id += 1

    return hosts


def get_fresh_scenario_host_objects(scenario, group_num=None):
    if group_num is None:
        return Host.objects(inv__in=[host_info.inv for host_info in scenario.hosts.values()])
    else:
        invs = [int(host_info.inv) for host_info in scenario.hosts.values() if host_info.group == group_num]
        return Host.objects.filter(inv__in=invs)


def get_fresh_host_stage_infos(scenario, group_num=None):
    if group_num is None:
        uuids = [host.uuid for host in Host.objects(inv__in=[host_info.inv for host_info in scenario.hosts.values()])]
    else:
        invs = [int(host_info.inv) for host_info in scenario.hosts.values() if host_info.group == group_num]
        uuids = [host.uuid for host in Host.objects.filter(inv__in=invs)]
    return HostStageInfo.objects(host_uuid__in=uuids)


def assert_stage_info_action_and_status(stage_info_owner, seq_num, action_type, status):
    stage_info = stage_info_owner.stage_info.stages[seq_num]
    assert stage_info.action_type == action_type
    assert stage_info.status == status


def assert_transition_to_next_stage(scenario, group_num, seq_num):
    for hsi in get_fresh_host_stage_infos(scenario, group_num):
        assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.FINISHED)
        assert hsi.stage_info.seq_num == seq_num + 1
        assert_stage_info_action_and_status(hsi, seq_num + 1, StageAction.ACTION, StageStatus.QUEUE)


@pytest.mark.slow
def test_add_hosts_scenario_execution(mp, test, startrek_client, qloud_client):
    expand_startrek_client_mock(startrek_client)

    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)
    source_project = test.mock_project({"id": SOURCE_PROJECT_ID, "cms_settings": [{"cms": "non-default"}]})
    target_project = test.mock_project({"id": TARGET_PROJECT_ID, "cms_settings": [{"cms": "non-default"}]})
    hosts = _make_hosts_for_scenario_test(test, source_project)

    scenario_params = dict(
        name="scenario-execution-test",
        scenario_type=hosts_add_script.name,
        autostart=True,
        hosts=[h.inv for h in hosts],
        reason="Test switch project scenario",
        ticket_key="WALLE-2791",
        script_args={
            ScriptArgs.TARGET_PROJECT: target_project.id,
            ScriptArgs.SCHEDULE_TYPE: SchedulerName.DATACENTER,
            ScriptArgs.TARGET_SEGMENT: TARGET_SEGMENT,
        },
        labels={"test": "yes"},
    )
    response = test.api_client.post("v1/scenarios", data=scenario_params)
    assert response.status_code == http.client.CREATED

    scenario = Scenario.objects.get(scenario_id=response.json["scenario_id"])

    assert scenario.status == ScenarioFsmStatus.STARTED
    assert {inv for inv in scenario_params["hosts"]} == {host_info.inv for host_info in scenario.hosts.values()}

    # AcquirePermission
    scenario = launch_scenario(scenario)

    # AddStartrekMessageStage
    scenario = launch_scenario(scenario)
    call_args = dict(
        text=JinjaTemplateRenderer().render_template(
            TemplatePath.STARTREK_START_SCENARIO, scenario_id=scenario.scenario_id
        ),
        issue_id=scenario.ticket_key,
    )
    startrek_client.add_comment.assert_has_calls([call(**call_args)])

    # ExecuteTicketTransitionStage - ReadyForDev
    scenario = launch_scenario(scenario)
    startrek_client.execute_transition.assert_has_calls(
        [
            call(issue_id=scenario.ticket_key, transition=TicketTransition.READY_FOR_DEV),
        ]
    )

    # ApproveStage
    monkeypatch_method(mp, ApproveStage.run, obj=ApproveStage, return_value=Marker.success())
    scenario = launch_scenario(scenario)

    # AddStartrekMessageStage
    scenario = launch_scenario(scenario)
    call_args = dict(
        text=JinjaTemplateRenderer().render_template(TemplatePath.STARTREK_EMERGENCY, scenario_id=scenario.scenario_id),
        issue_id=scenario.ticket_key,
    )
    startrek_client.add_comment.assert_has_calls([call(**call_args)])

    # ExecuteTicketTransitionStage - InProgress
    scenario = launch_scenario(scenario)
    startrek_client.execute_transition.assert_has_calls(
        [call(issue_id=scenario.ticket_key, transition=TicketTransition.IN_PROGRESS)]
    )

    # WaitForLabelOrTimeStage.run()
    monkeypatch_method(mp, WaitForLabelOrTimeStage.run, obj=WaitForLabelOrTimeStage, return_value=Marker.success())
    scenario = launch_scenario(scenario)

    scenario = launch_scenario(scenario)
    # AddHostsStage: Proceed to the next stage now that the host has been added
    assert_stage_info_action_and_status(
        scenario, scenario.stage_info.seq_num - 1, StageAction.ACTION, StageStatus.FINISHED
    )

    scenario = launch_scenario(scenario)
    # SetHostUUIDStage: Keys in scenario object must be converted to UUIDs
    assert_stage_info_action_and_status(
        scenario, scenario.stage_info.seq_num - 1, StageAction.ACTION, StageStatus.FINISHED
    )
    assert set(scenario.hosts.keys()) == {host.uuid for host in hosts}

    scenario = launch_scenario(scenario)
    # Check initial group
    assert scenario.current_group == 0

    scenario = launch_scenario(scenario)
    # Check that hosts in scenario object have been assigned to a group
    for host_info in scenario.hosts.values():
        assert host_info.group is not None

    # HOST STAGES
    for group_num in range(len(DC_NAMES)):
        old_tasks = {}
        seq_num = 0
        _run_scenario(scenario.id)
        # Check HostRootStage serialization on hosts in current group
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            assert host.scenario_id == scenario.scenario_id

            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert hsi.stage_info

        scenario = launch_scenario(scenario)
        # SwitchToMaintenanceHostStage.run() - schedule switch-to-maintenance task
        # Check that task has been scheduled with required params
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.status == Operation.SWITCH_TO_MAINTENANCE.host_status
            assert host.ticket == scenario.ticket_key
            assert not host.task.ignore_cms
            assert host.task.owner == authorization.ISSUER_WALLE
            old_tasks[host.inv] = host.task

            stage_info_hosts_data = scenario.stage_info.stages[9].stages[0].stages[0].hosts
            assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.PROCESSING

        scenario = launch_scenario(scenario)
        # SwitchToMaintenanceHostStage.run()
        # Check that task has not been recreated. Complete maintenance switching for next iteration to finish
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.task == old_tasks[host.inv]
            host.state = HostState.MAINTENANCE
            host.status = HostStatus.default(HostState.MAINTENANCE)
            host.state_author = scenario.issuer
            host.operation_state = HostOperationState.DECOMMISSIONED
            del host.task
            host.save()

        scenario = launch_scenario(scenario)
        # SwitchToMaintenanceHostStage.run()
        # Check transition to the next stage
        assert_transition_to_next_stage(scenario, group_num, seq_num)
        seq_num += 1

        for host in get_fresh_scenario_host_objects(scenario, group_num):
            stage_info_hosts_data = scenario.stage_info.stages[9].stages[0].stages[0].hosts
            assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.FINISHED

        scenario = launch_scenario(scenario)
        # PowerOffHostStage.run()
        # Check host's maintenance
        assert_transition_to_next_stage(scenario, group_num, seq_num)
        seq_num += 1

        scenario = launch_scenario(scenario)
        # WaitDayStage.run()
        # Check transition to the next stage
        assert_transition_to_next_stage(scenario, group_num, seq_num)
        seq_num += 1

        # LiberateFromQloudHostStage.run()
        # Check that task has been scheduled with required params
        scenario = launch_scenario(scenario)
        assert_transition_to_next_stage(scenario, group_num, seq_num)
        seq_num += 1

        scenario = launch_scenario(scenario)
        # SwitchProjectHostStage.run()
        # Check that task has been scheduled with required params
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.status == Operation.SWITCH_PROJECT.host_status

            switch_project_stage = next(stage for stage in host.task.stages if stage.name == Stages.SWITCH_PROJECT)
            assert Stages.COMPLETE_RELEASING in [stage.name for stage in host.task.stages]
            assert switch_project_stage.params["project"] == target_project.id
            assert not host.task.ignore_cms
            assert host.task.owner == authorization.ISSUER_WALLE
            old_tasks[host.inv] = host.task

            stage_info_hosts_data = scenario.stage_info.stages[9].stages[0].stages[4].hosts
            assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.PROCESSING

        scenario = launch_scenario(scenario)
        # SwitchProjectHostStage.run()
        # Check that task has not been recreated. Complete project switching for the next iteration to finish
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.task == old_tasks[host.inv]

            host.state = HostState.FREE
            host.status = HostStatus.default(HostState.FREE)
            host.project = target_project.id
            del host.task
            host.save()

        scenario = launch_scenario(scenario)
        # SwitchProjectHostStage.run()
        # Check transition to another task
        assert_transition_to_next_stage(scenario, group_num, seq_num)
        seq_num += 1

        for host in get_fresh_scenario_host_objects(scenario, group_num):
            stage_info_hosts_data = scenario.stage_info.stages[9].stages[0].stages[4].hosts
            assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.FINISHED

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Check that task has been scheduled with required params
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.status == Operation.PREPARE.host_status
            assert not host.task.ignore_cms
            assert host.task.owner == authorization.ISSUER_WALLE
            old_tasks[host.inv] = host.task

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Check that task has not been recreated. Remove task and set the PROBATION state
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)

            assert host.task == old_tasks[host.inv]

            host.state = HostState.PROBATION
            host.status = HostStatus.default(HostState.PROBATION)
            host.project = target_project.id
            del host.task
            host.save()

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Check hosts added to qloud. Set them to assigned state
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)
            qhost = qloud_client.find_host(host.name)
            assert qhost.state == QloudHostStates.INITIAL and qhost.segment == TARGET_SEGMENT
            host.state = HostState.ASSIGNED
            host.save()

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Qloud meta called
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)
            qhost = qloud_client.find_host(host.name)
            assert qhost.meta_called and qhost.state == QloudHostStates.INITIAL and qhost.segment == TARGET_SEGMENT
            # Dirty patch meta
            qhost.is_data_filled = True

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Check hosts changing qloud state to UP
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.PROCESSING)
            qhost = qloud_client.find_host(host.name)
            assert qhost.state == QloudHostStates.UP and qhost.segment == TARGET_SEGMENT

        scenario = launch_scenario(scenario)
        # PrepareHostStage.run()
        # Hosts are in target project in assigned-ready. Check stage completion
        for host in get_fresh_scenario_host_objects(scenario, group_num):
            hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
            assert_stage_info_action_and_status(hsi, seq_num, StageAction.ACTION, StageStatus.FINISHED)
            assert hsi.stage_info.status == StageStatus.FINISHED  # HostRootStage has been completed

        # SchedulerStage.check()
        # Must proceed to the next group after checking that all hosts in current group have been processed
        scenario = launch_scenario(scenario)
        assert scenario.current_group == group_num + 1
        assert scenario.stage_info.stages[0].action_type == StageAction.ACTION  # Proceed to the next group

    # AddStartrekMessageStage
    scenario = launch_scenario(scenario)
    call_args = dict(
        text=JinjaTemplateRenderer().render_template(
            TemplatePath.STARTREK_END_SCENARIO, scenario_id=scenario.scenario_id
        ),
        issue_id=scenario.ticket_key,
    )
    startrek_client.add_comment.assert_has_calls([call(**call_args)])

    # ExecuteTicketTransitionStage - CLOSED
    scenario = launch_scenario(scenario)
    startrek_client.execute_transition.assert_has_calls(
        [
            call(
                issue_id=scenario.ticket_key,
                transition=TicketTransition.CLOSE,
                issue_params={"resolution": TICKET_RESOLUTION},
            )
        ]
    )

    # All stages have finished.
    for host in get_fresh_scenario_host_objects(scenario):
        assert host.scenario_id is None

    assert HostStageInfo.objects(scenario_id=scenario.scenario_id).count() == 0

    assert scenario.status == ScenarioFsmStatus.FINISHED
