"""Integration tests for scenarios."""
import http.client
from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.scenario_util import launch_scenario
from infra.walle.server.tests.lib.util import (
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_method,
    TestCase,
)
from walle import authorization
from walle.clients import inventory, bot, ipmiproxy
from walle.constants import FLEXY_EINE_PROFILE
from walle.hosts import HostLocation, Host, HostState, HostStatus, HostOperationState
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.physical_location_tree import LocationNamesMap
from walle.scenario.constants import ScenarioFsmStatus, TicketStatus, ScenarioWorkStatus, ScriptName
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.scenario import Scenario, HostStageStatus

# noinspection PyProtectedMember
from walle.scenario.scenario_fsm import _run_scenario
from walle.scenario.stage_info import StageAction, StageStatus

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
            switch="switch-mock",
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
def test_noc_scenario_execution(mp, test, startrek_client, qloud_client):
    expand_startrek_client_mock(startrek_client)

    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)
    source_project = test.mock_project({"id": SOURCE_PROJECT_ID})
    hosts = _make_hosts_for_scenario_test(test, source_project)

    scenario_params = dict(
        name="noc-scenario-execution-test",
        reason="Test NOC scenario",
        ticket_key="WALLE-1",
        scenario_type=ScriptName.NOC_SOFT,
        script_args={
            "switch": "switch-mock",
        },
    )
    response = test.api_client.post("v1/scenarios", data=scenario_params)
    assert response.status_code == http.client.CREATED

    scenario = Scenario.objects.get(scenario_id=response.json["scenario_id"])

    scenario.start()
    scenario.set_works_status_label(ScenarioWorkStatus.STARTED)
    scenario.save()

    assert scenario.status == ScenarioFsmStatus.STARTED
    assert {host.inv for host in hosts} == {host_info.inv for host_info in scenario.hosts.values()}

    # AcquirePermission
    scenario = launch_scenario(scenario)

    # SCHEDULER
    scenario = launch_scenario(scenario)

    old_tasks = {}

    _run_scenario(scenario.id)
    # Check HostRootStage serialization on hosts in current group
    for host in get_fresh_scenario_host_objects(scenario, 0):
        assert host.scenario_id == scenario.scenario_id

        hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
        assert hsi.stage_info

    # CollectHealthChecksStage()
    scenario = launch_scenario(scenario)
    scenario = launch_scenario(scenario)

    scenario = launch_scenario(scenario)
    # SwitchToMaintenanceHostStage.run() - schedule switch-to-maintenance task
    # Check that task has been scheduled with required params
    for host in get_fresh_scenario_host_objects(scenario, 0):
        hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
        assert_stage_info_action_and_status(hsi, 1, StageAction.ACTION, StageStatus.PROCESSING)

        assert host.status == Operation.SWITCH_TO_MAINTENANCE.host_status
        assert host.ticket == scenario.ticket_key
        assert host.task.ignore_cms
        assert host.task.owner == authorization.ISSUER_WALLE
        old_tasks[host.inv] = host.task

        stage_info_hosts_data = scenario.stage_info.stages[1].stages[0].stages[0].stages[1].hosts
        assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.PROCESSING

    assert scenario.stage_info.stages[1].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].stages[0].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].stages[0].stages[1].status == StageStatus.PROCESSING

    scenario = launch_scenario(scenario)
    # SwitchToMaintenanceHostStage.run()
    # Check that task has not been recreated. Complete maintenance switching for next iteration to finish
    for host in get_fresh_scenario_host_objects(scenario, 0):
        hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
        assert_stage_info_action_and_status(hsi, 1, StageAction.ACTION, StageStatus.PROCESSING)

        assert host.task == old_tasks[host.inv]
        host.state = HostState.MAINTENANCE
        host.status = HostStatus.default(HostState.MAINTENANCE)
        host.state_author = scenario.issuer
        host.operation_state = HostOperationState.DECOMMISSIONED
        del host.task
        host.save()

    assert scenario.stage_info.stages[1].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].stages[0].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].stages[0].stages[1].status == StageStatus.PROCESSING

    scenario = launch_scenario(scenario)
    # SwitchToMaintenanceHostStage.run()

    for host in get_fresh_scenario_host_objects(scenario, 0):
        stage_info_hosts_data = scenario.stage_info.stages[1].stages[0].stages[0].stages[1].hosts
        assert stage_info_hosts_data[host.uuid]["status"] == HostStageStatus.FINISHED

    assert scenario.stage_info.stages[1].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].status == StageStatus.PROCESSING
    assert scenario.stage_info.stages[1].stages[0].stages[0].status == StageStatus.FINISHED
    assert scenario.stage_info.stages[1].stages[0].stages[0].stages[1].status == StageStatus.FINISHED
