import http.client
import typing
from unittest.mock import Mock, call

import pytest

from infra.walle.server.tests.lib.scenario_util import launch_scenario
from infra.walle.server.tests.lib.util import TestCase
from walle.clients import abc
from walle.hosts import Host, HostState
from walle.scenario.constants import FixedMaintenanceApproversLogins, TemplatePath
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario
from walle.scenario.script import switch_to_maintenance_with_groups_approves_script
from walle.scenario.stage.maintenance_approvers_workflow_stage import (
    GroupStagesContainer,
    MaintenanceApproversWorkflowStage,
)
from walle.scenario.stage_info import StageInfo
from walle.scenario.stages import PowerOffHostStage, SwitchToMaintenanceHostStage
from walle.util.template_loader import JinjaTemplateRenderer

# Mocks hosts and projects. These specific hosts will be splitted into four groups.
MOCK_PROJECT_INV_MAP = {
    "yt-project-1": {"tags": ["yt", "some-yt-tag"], "invs": [1], "expected_group": "yt"},
    "yt-project-2": {"tags": ["yt", "another-yt-tag"], "invs": [2, 3], "expected_group": "yt"},
    "yp-project": {"tags": ["yp", "some-yp-tag"], "invs": [4], "expected_group": "yp"},
    "yabs-project": {"tags": ["yabs", "yabs-foo", "yabs-bar"], "invs": [5, 6], "expected_group": "yabs"},
    "other-project-1": {"tags": ["other-foo"], "invs": [7, 8, 9], "expected_group": "other"},
    "other-project-2": {"tags": [], "invs": [10, 11, 12], "expected_group": "other"},
}


@pytest.fixture
def walle_test(
    mp, monkeypatch, monkeypatch_locks, monkeypatch_timestamp, authorized_scenario_user, request, startrek_client
):
    def _mocked_switch_to_maintenance_host_stage(_self, _stage_info, _scenario, host, *_args, **_kwargs):
        host.set_state(HostState.MAINTENANCE, "mock-issuer", "mock-audit-log-id")
        host.save()
        return Marker.success()

    monkeypatch.setattr(SwitchToMaintenanceHostStage, "run", _mocked_switch_to_maintenance_host_stage)

    def _mocked_power_off_host_stage(_self, _stage_info, _scenario, _host, *_args, **_kwargs):
        return Marker.success()

    monkeypatch.setattr(PowerOffHostStage, "run", _mocked_power_off_host_stage)

    mp.function(abc.get_service_on_duty_logins, return_value=["yp-approver-login-1"])
    monkeypatch.setattr(FixedMaintenanceApproversLogins, "YABS_MAINTENANCE_APPROVERS_LOGINS", ["yabs-approver-login-1"])
    monkeypatch.setattr(
        FixedMaintenanceApproversLogins,
        "YT_MAINTENANCE_APPROVERS_LOGINS",
        ["yt-approver-login-1", "yt-approver-login-2"],
    )
    monkeypatch.setattr(
        FixedMaintenanceApproversLogins, "DEFAULT_MAINTENANCE_APPROVERS_LOGINS", ["default-approver-login-1"]
    )

    return TestCase.create(request)


def expand_startrek_client_mock(mock_startrek_client):
    mock_startrek_client.attach_mock(Mock(), "add_comment")
    return mock_startrek_client


# Removed @pytest.mark.xfail(), but need to test scenario flow further.
def test_switch_to_maintenance_with_groups_approves_script(walle_test, startrek_client):
    """
    Test sample scenario script to demonstrate how parallel execution of stages work.
    """
    expand_startrek_client_mock(startrek_client)
    hosts, expected_number_of_groups = _make_mocked_hosts(walle_test)

    scenario_params = dict(
        name="scenario-execution-test",
        scenario_type=switch_to_maintenance_with_groups_approves_script.name,
        autostart=True,
        hosts=[host.inv for host in hosts],
        reason="Test 'switch to maintenance with groups approves' scenario",
        ticket_key="WALLE-3898",
    )
    response = walle_test.api_client.post("v1/scenarios", data=scenario_params)
    assert response.status_code == http.client.CREATED

    scenario = Scenario.objects.get(scenario_id=response.json["scenario_id"])

    # First run: AddStartrekMessageStage.
    scenario = launch_scenario(scenario)
    assert startrek_client.add_comment.call_count == 1
    expected_startrek_calls_list = [
        call(**_get_startrek_call_args(TemplatePath.STARTREK_START_ITDC_MAINTENANCE_SCENARIO, scenario))
    ]
    startrek_client.add_comment.assert_has_calls(expected_startrek_calls_list)

    # Second run: Split hosts into groups and quit.
    scenario = launch_scenario(scenario)
    # Check that hosts are splitted into groups, and groups' approvers are in 'data_storage'.
    # Group ids starts from 0.
    assert max(host.group for host in scenario.hosts.values()) == expected_number_of_groups - 1
    assert len(get_data_storage(scenario).read()) == expected_number_of_groups

    # Check that there are copies of stages for each group.
    itdc_workflow_stage_info = scenario.stage_info.stages[1]
    maintenance_approvers_workflow_stage_info = itdc_workflow_stage_info.stages[0]
    group_stages_containers = _get_group_stages_containers(maintenance_approvers_workflow_stage_info)
    assert len(group_stages_containers) == expected_number_of_groups
    for group_stages_container in group_stages_containers:
        _check_assertions_for_group_stages_container(group_stages_container)


def _check_assertions_for_group_stages_container(group_stages_container: GroupStagesContainer):
    assert len(group_stages_container.stages) == 2
    assert group_stages_container.stages[0].name == "HostGroupApproveStage"
    assert group_stages_container.stages[1].name == "HostGroupSchedulerStage"

    assert group_stages_container.group_name
    assert group_stages_container.number_of_hosts

    expected_number_of_hosts_in_group = 0
    for project_data in MOCK_PROJECT_INV_MAP.values():
        if project_data["expected_group"] == group_stages_container.group_name:
            expected_number_of_hosts_in_group += len(project_data["invs"])

    assert expected_number_of_hosts_in_group == group_stages_container.number_of_hosts


# noinspection PyProtectedMember
def _get_group_stages_containers(
    maintenance_approvers_workflow_stage_info: StageInfo,
) -> typing.List[GroupStagesContainer]:
    return [
        GroupStagesContainer(**group_stages_container_dict)
        for group_stages_container_dict in maintenance_approvers_workflow_stage_info.get_data(
            MaintenanceApproversWorkflowStage._group_stages_containers_data_field_name
        )
    ]


def _get_startrek_call_args(template, scenario):
    return dict(
        text=JinjaTemplateRenderer().render_template(template, scenario_id=scenario.scenario_id),
        issue_id=scenario.ticket_key,
    )


def _get_startrek_host_group_call_args(template, scenario, host_group_id):
    return dict(
        text=JinjaTemplateRenderer().render_template(
            template, scenario_id=scenario.scenario_id, host_group_id=host_group_id
        ),
        issue_id=scenario.ticket_key,
    )


# noinspection DuplicatedCode
def _make_mocked_hosts(walle_test) -> (typing.List[Host], int):
    hosts = []
    for project, project_values in MOCK_PROJECT_INV_MAP.items():
        walle_test.mock_project({"id": project, "tags": project_values["tags"]})
        for inv in project_values["invs"]:
            hosts.append(
                walle_test.mock_host(
                    {
                        "inv": inv,
                        "project": project,
                    }
                )
            )

    return hosts, 4
