import http.client
import time
from unittest.mock import Mock

import attr
import gevent
import pytest
from mock import call

from infra.walle.server.tests.lib.util import TestCase, mock_startrek_client
from walle.clients import ok
from walle.hosts import HostLocation, Host, HostState
from walle.scenario.constants import ScriptName, ScenarioWorkStatus, CustomTemplatePath
from walle.scenario.scenario import Scenario
from walle.scenario.scenario_fsm import _run_scenario
from walle.scenario.script_args import NocHardParams
from walle.util.template_loader import JinjaTemplateRenderer


@pytest.fixture
def test(mp, monkeypatch_timestamp, monkeypatch_locks, authorized_scenario_user, startrek_client, request):
    startrek_client.attach_mock(Mock(), "close_issue")
    startrek_client.attach_mock(Mock(return_value={"key": "TEST-1"}), "create_issue")
    return TestCase.create(request)


def wait(scenario_id, predicate, fail_message):
    deadline = time.time() + 5

    while predicate():
        if time.time() > deadline:
            raise RuntimeError(fail_message)
        _run_scenario(scenario_id)
        gevent.idle()


def wait_task(scenario_id, host_inv):
    wait(
        scenario_id,
        lambda: not Host.get_collection().find_one({"inv": host_inv, "task": {"$exists": True}}),
        fail_message="host task was not created",
    )


def wait_work_status(scenario_id, target_status: ScenarioWorkStatus):
    wait(
        scenario_id,
        lambda: Scenario.objects.get(scenario_id=scenario_id).get_works_status() != target_status,
        fail_message=f"Scenario has not achieved '{target_status}' work status",
    )


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_dump_approvers_list(mp, test, authorized_scenario_user, ok_client):
    st_client = mock_startrek_client(mp)
    st_client.create_issue.return_value = {"key": "MOCKED-1"}
    project1 = test.mock_project({"id": "some-project-1", "maintenance_plot_id": "mp1-id"})
    project2 = test.mock_project({"id": "some-project-2", "maintenance_plot_id": "mp2-id"})
    test.mock_maintenance_plot(
        {"id": "mp1-id", "common_settings": {"maintenance_approvers": {"logins": ["mp1_login1", "mp1_login2"]}}}
    )
    test.mock_maintenance_plot(
        {"id": "mp2-id", "common_settings": {"maintenance_approvers": {"logins": ["mp2_login1", "mp2_login2"]}}}
    )

    host1_inv, host2_inv, host3_inv = 911, 921, 922
    switch = "switch01"
    test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "inv": host1_inv,
            "project": project1.id,
            "location": HostLocation(switch=switch),
        }
    )
    test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "inv": host2_inv,
            "project": project2.id,
            "location": HostLocation(switch=switch),
        }
    )
    test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "inv": host3_inv,
            "project": project2.id,
            "location": HostLocation(switch=switch),
        }
    )
    scenario_params = dict(
        name="noc-hard-test",
        scenario_type=ScriptName.NOC_HARD,
        autostart=True,
        hosts=[host1_inv, host2_inv, host3_inv],
        reason="Test switch project scenario",
        ticket_key="WALLE-2791",
        script_args={attr.fields(NocHardParams).switch.name: switch},
    )
    response = test.api_client.post("v1/scenarios", data=scenario_params)
    assert response.status_code == http.client.CREATED
    scenario_id = response.json['scenario_id']

    ok_approvement = ok.Approvement(
        None,
        None,
        None,
        status=ok.ApprovementStatus.CLOSED,
        is_approved=True,
        uuid=1,
        resolution=ok.ApprovementResolution.NO_RESOLUTION,
    )
    ok_client.attach_mock(Mock(return_value=ok_approvement), "get_approvement")
    ok_client.attach_mock(Mock(return_value=ok_approvement), "create_approvement")
    wait_work_status(scenario_id, ScenarioWorkStatus.APPROVEMENT)
    ok_approvement.resolution = ok.ApprovementResolution.APPROVED
    wait_task(scenario_id, host1_inv)

    params = {
        'scenario_id': scenario_id,
        'host_groups_info': {
            0: {
                'maintenance_plot_id': 'mp1-id',
                'approvers': [
                    'mp1_login1',
                    'mp1_login2',
                ],
                'hosts': [
                    {'inv': 911, 'name': 'mocked-911.mock'},
                ],
            },
            1: {
                'maintenance_plot_id': 'mp2-id',
                'approvers': [
                    'mp2_login1',
                    'mp2_login2',
                ],
                'hosts': [
                    {'inv': 921, 'name': 'mocked-921.mock'},
                    {'inv': 922, 'name': 'mocked-922.mock'},
                ],
            },
        },
    }
    call_args = dict(
        text=JinjaTemplateRenderer().render_template(
            CustomTemplatePath.STARTREK_DUMP_APPROVERS_LIST,
            **params,
        ),
        issue_id="WALLE-2791",
    )
    st_client.add_comment.assert_has_calls([call(**call_args)])
