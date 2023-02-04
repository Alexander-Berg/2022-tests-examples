import http.client
import time
from unittest.mock import Mock

import attr
import gevent
import pytest

import walle.scenario.stages
from infra.walle.server.tests.lib.util import TestCase, monkeypatch_eine_client_for_host
from walle.clients import ok
from walle.hosts import HostLocation, Host, HostState, HostStatus
from walle.scenario import constants as scenario_constants
from walle.scenario.constants import ScriptName, ScenarioWorkStatus
from walle.scenario.scenario import Scenario
from walle.scenario.scenario_fsm import _run_scenario
from walle.scenario.script_args import NocHardParams


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


def host_to_state(inv, state, status):
    Host.get_collection().update(
        {"inv": inv},
        {
            "$unset": {"task": 1},
            "$set": {"state": state, "status": status, "state_expire": {"time": time.time() + 100}},
        },
    )


def mock_for_finish(mp, host):
    monkeypatch_eine_client_for_host(mp, Host.objects.get(inv=host.inv))

    # NOTE(rocco66): for profile, switch_vlans ..
    mp.setattr(walle.scenario.stages, "check_last_operation_from_oplog", lambda *args, **kwargs: True)


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_noc_hard_simple_test(mp, test, authorized_scenario_user, ok_client):
    project = test.mock_project({"id": "some-project", "tags": ["rtc"]})
    test.mock_maintenance_plot()
    host_inv = 911
    switch = "switch01"
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "inv": 911,
            "project": project.id,
            "location": HostLocation(switch=switch),
        }
    )

    scenario_params = dict(
        name="noc-hard-test",
        scenario_type=ScriptName.NOC_HARD,
        autostart=True,
        hosts=[host_inv],
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

    wait_task(scenario_id, host_inv)
    host_to_state(host_inv, HostState.MAINTENANCE, HostStatus.MANUAL)
    wait_work_status(scenario_id, ScenarioWorkStatus.READY)

    mock_for_finish(mp, host)

    scenario = Scenario.objects.get(scenario_id)
    scenario.labels[
        scenario_constants.WORKMATE_STATUS_LABEL_NAME
    ] = scenario_constants.WORKMATE_STATUS_TARGET_LABEL_VALUE
    scenario.save()
    wait_work_status(scenario_id, ScenarioWorkStatus.FINISHING)

    wait_work_status(scenario_id, ScenarioWorkStatus.FINISHED)


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_noc_hard_with_hosts_list(mp, test, authorized_scenario_user, ok_client):
    project = test.mock_project({"id": "some-project"})
    test.mock_maintenance_plot()
    host1, host2 = [
        test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "inv": inv,
                "project": project.id,
            }
        )
        for inv in (42, 911)
    ]

    scenario_params = dict(
        name="noc-hard-test",
        scenario_type=ScriptName.NOC_HARD,
        autostart=True,
        hosts=[host1.inv, host2.inv],
        reason="Test switch project scenario",
        ticket_key="WALLE-2791",
        script_args={},
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

    wait_task(scenario_id, host1.inv)
    wait_task(scenario_id, host2.inv)
    host_to_state(host1.inv, HostState.MAINTENANCE, HostStatus.MANUAL)
    host_to_state(host2.inv, HostState.MAINTENANCE, HostStatus.MANUAL)
    wait_work_status(scenario_id, ScenarioWorkStatus.READY)

    mock_for_finish(mp, host1)
    mock_for_finish(mp, host2)

    scenario = Scenario.objects.get(scenario_id)
    scenario.labels[
        scenario_constants.WORKMATE_STATUS_LABEL_NAME
    ] = scenario_constants.WORKMATE_STATUS_TARGET_LABEL_VALUE
    scenario.save()
    wait_work_status(scenario_id, ScenarioWorkStatus.FINISHING)

    wait_work_status(scenario_id, ScenarioWorkStatus.FINISHED)
