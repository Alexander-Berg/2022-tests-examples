import http.client

import pytest

from infra.walle.server.tests.lib.util import mock_startrek_client, mock_physical_location_kwargs, TestCase
from tests.api.scenario_api.utils import get_scenario_json
from walle.constants import NETWORK_SOURCE_RACKTABLES
from walle.hosts import HostLocation
from walle.models import timestamp, monkeypatch_timestamp
from walle.scenario.constants import (
    ScenarioFsmStatus,
    ScenarioWorkStatus,
    WORK_STATUS_LABEL_NAME,
    SWITCH_LABEL_NAME,
    ScriptName,
    WORKMATE_STATUS_LABEL_NAME,
    WORKMATE_STATUS_TARGET_LABEL_VALUE,
    WORK_STATUS_API_PARAM,
)
from walle.scenario.scenario import Scenario
from walle.scenario.script import noc_soft_maintenance_script, noop_script

TEST_SWITCH_NAME = "test-switch"


@pytest.fixture
def test(request, mp):
    mock_startrek_client(mp)
    monkeypatch_timestamp(mp, cur_time=0)
    return TestCase.create(request)


@pytest.fixture(params=("POST", "PATCH"))
def scenario_start_method(test, request):
    return lambda scenario_id: test.api_client.open(
        "/v1/scenarios/{}/start".format(scenario_id),
        method=request.param,
        data={"reason": "reason-mock"},
    )


def scenario_finish_method(test, scenario_id):
    return test.api_client.open(
        "/v1/scenarios/{}".format(scenario_id),
        method="PATCH",
        data={"labels": {WORKMATE_STATUS_LABEL_NAME: WORKMATE_STATUS_TARGET_LABEL_VALUE}},
    )


@pytest.fixture(params=("POST", "PATCH"))
def scenario_cancel_method(test, request):
    return lambda scenario_id: test.api_client.open(
        "/v1/scenarios/{}/cancel".format(scenario_id),
        method=request.param,
        data={"reason": "reason-mock"},
    )


def _mock_location(switch=TEST_SWITCH_NAME):
    return HostLocation(
        switch=switch,
        port="test-port",
        network_source=NETWORK_SOURCE_RACKTABLES,
        physical_timestamp=timestamp(),
        **mock_physical_location_kwargs(),
    )


@pytest.fixture
def hosts_in_switch(test):
    location = _mock_location()
    return [test.mock_host({"inv": i, "location": location}) for i in range(10)]


def get_noc_soft_scenario_json(
    switch=TEST_SWITCH_NAME, urgently=False, project_id=None, name="scenario_mock_name", status=None, issuer=None
):
    script_args = {
        "switch": switch,
        "urgently": urgently,
    }
    if project_id:
        script_args["project_id"] = project_id
    return get_scenario_json(
        name=name,
        script_name=ScriptName.NOC_SOFT,
        ticket_key="FOO-19",
        script_args=script_args,
        status=status,
        issuer=issuer,
    )


@pytest.mark.parametrize("switch_param_value", [TEST_SWITCH_NAME, "{}.yndx.net".format(TEST_SWITCH_NAME)])
@pytest.mark.parametrize("urgently", [True, False])
def test_init_noc_scenario_only_one_switch(
    test, hosts_in_switch, authorized_scenario_user, switch_param_value, urgently
):
    host_not_in_switch = test.mock_host({"location": _mock_location("another-switch")})
    result = test.api_client.post("/v1/scenarios", data=get_noc_soft_scenario_json(switch_param_value, urgently))
    assert result.status_code == http.client.CREATED

    scenario = Scenario.objects.get(result.json["scenario_id"])
    assert host_not_in_switch.inv not in scenario.hosts
    assert {h.id for h in hosts_in_switch} == set(scenario.hosts)


def test_init_noc_scenario_with_hosts_acquired_by_another_scenario(test, authorized_scenario_user):
    location = _mock_location()
    for i in range(4):
        test.mock_host({"inv": i, "location": location, "scenario_id": i % 2 + 1, "name": str(i)})

    result = test.api_client.post("/v1/scenarios", data=get_noc_soft_scenario_json())

    assert result.status_code == http.client.CONFLICT
    assert (
        "Can't create scenario, one or more hosts are already acquired by other scenarios: "
        "hosts with fqdns 0, 2 acquired by scenario with id 1, "
        "hosts with fqdns 1, 3 acquired by scenario with id 2" == result.json["message"]
    )
    test.scenarios.assert_equal()
    test.hosts.assert_equal()


def test_init_noc_scenario_with_switch_and_project(test, hosts_in_switch, authorized_scenario_user):
    host_not_in_project = test.mock_host({"location": _mock_location(), "project": "another-project-id"})
    result = test.api_client.post(
        "/v1/scenarios",
        data=get_noc_soft_scenario_json(project_id="mocked-default-project"),
    )

    assert result.status_code == http.client.CREATED
    scenario = Scenario.objects.get(result.json["scenario_id"])
    assert host_not_in_project.inv not in scenario.hosts


def test_init_noc_scenario_with_wrong_switch(test, authorized_scenario_user):
    request_data = get_noc_soft_scenario_json()

    result = test.api_client.post("/v1/scenarios", data=request_data)

    assert result.status_code == http.client.OK
    assert result.json["message"] == "There are no hosts associated with switch '%s'" % TEST_SWITCH_NAME


def test_init_noc_scenario_reached_limit(mp, test, authorized_scenario_user):
    def try_to_create_scenario(index):
        switch = f"switch_{index}"
        test.mock_host({"inv": index, "location": _mock_location(switch), "id": index})
        return test.api_client.post(
            "/v1/scenarios", data=get_noc_soft_scenario_json(switch, name=f"scenario_name_{index}")
        )

    mp.config("scenario.max_created_maintenance_scenarios", 2)
    for i in range(2):
        result = try_to_create_scenario(i)
        assert result.status_code == http.client.CREATED

    result = try_to_create_scenario(2)
    assert result.status_code == http.client.BAD_REQUEST
    assert "Too many switches are being processed right now. Please try again later." in result.json["message"]


def test_init_noc_scenario_unauthorized(test, hosts_in_switch):
    result = test.api_client.post("/v1/scenarios", data=get_noc_soft_scenario_json())
    assert result.status_code == http.client.FORBIDDEN


@pytest.mark.parametrize("status", [ScenarioFsmStatus.CREATED, ScenarioFsmStatus.PAUSED])
def test_start_noc_scenario(test, hosts_in_switch, authorized_scenario_user, scenario_start_method, status):
    create_result = test.api_client.post("/v1/scenarios", data=get_noc_soft_scenario_json())
    scenario_id = create_result.json["scenario_id"]

    start_result = scenario_start_method(scenario_id)
    assert start_result.status_code == http.client.ACCEPTED

    scenario = Scenario.objects.get(scenario_id)
    assert scenario.get_works_status() == ScenarioWorkStatus.STARTED


@pytest.mark.parametrize("status", set(ScenarioFsmStatus) - {ScenarioFsmStatus.CREATED, ScenarioFsmStatus.PAUSED})
def test_start_noc_scenario_in_another_status(test, authorized_scenario_user, scenario_start_method, status):
    scenario = test.mock_scenario(get_noc_soft_scenario_json(status=status))
    result = scenario_start_method(scenario.scenario_id)
    assert result.status_code == http.client.CONFLICT


def test_start_noc_scenario_not_exists(test, authorized_scenario_user, scenario_start_method):
    scenario = test.mock_scenario(get_noc_soft_scenario_json())
    result = scenario_start_method(scenario.id + 1)
    assert result.status_code == http.client.NOT_FOUND


def test_start_noc_scenario_forbidden(test, scenario_start_method):
    scenario = test.mock_scenario(get_noc_soft_scenario_json())
    result = scenario_start_method(scenario.scenario_id)
    assert result.status_code == http.client.FORBIDDEN


@pytest.mark.parametrize("status", ScenarioWorkStatus)
def test_get_noc_scenario_status(test, hosts_in_switch, authorized_scenario_user, status):
    scenario = test.mock_scenario(get_noc_soft_scenario_json())
    scenario.set_works_status_label(status)
    scenario.save()
    result = test.api_client.get("/v1/scenarios/{}".format(scenario.scenario_id))
    assert result.status_code == http.client.OK
    assert result.json["labels"][WORK_STATUS_LABEL_NAME] == status.value


def test_get_noc_scenario_status_not_found(test, authorized_scenario_user):
    scenario = test.mock_scenario(get_noc_soft_scenario_json())
    result = test.api_client.get("/v1/scenarios/{}".format(scenario.scenario_id + 1))
    assert result.status_code == http.client.NOT_FOUND


class TestFinishNocMaintenanceScenario:
    @staticmethod
    def _mk_scenario(test, works_status, issuer=None):
        scenario = test.mock_scenario(get_noc_soft_scenario_json(issuer=issuer))
        scenario.set_works_status_label(works_status)
        return scenario

    def test_finish_noc_scenario(self, test, mp, authorized_scenario_user):
        creation_time = 0
        request_time = 1
        monkeypatch_timestamp(mp, creation_time)
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY)
        assert scenario.action_time == creation_time
        monkeypatch_timestamp(mp, request_time)

        result = scenario_finish_method(test, scenario.scenario_id)
        assert result.status_code == http.client.ACCEPTED
        assert Scenario.objects.get(scenario.scenario_id).work_completed_by_workmate()

    def test_finish_noc_scenario_not_found(self, test, authorized_scenario_user):
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY)
        result = scenario_finish_method(test, scenario.scenario_id + 1)
        assert result.status_code == http.client.NOT_FOUND

    def test_finish_noc_scenario_forbidden(self, test):
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY, issuer="other@")
        result = scenario_finish_method(test, scenario.scenario_id)
        assert result.status_code == http.client.FORBIDDEN


class TestCancelNocMaintenanceScenario:
    @staticmethod
    def _mk_scenario(test, works_status, scenario_status=ScenarioFsmStatus.STARTED, issuer=None):
        scenario = test.mock_scenario(get_noc_soft_scenario_json(status=scenario_status, issuer=issuer))
        scenario.set_works_status_label(works_status.value)
        scenario.labels[SWITCH_LABEL_NAME] = TEST_SWITCH_NAME
        return scenario

    def test_cancel_noc_scenario(self, test, mp, scenario_cancel_method, authorized_scenario_user):
        creation_time = 0
        request_time = 1
        monkeypatch_timestamp(mp, creation_time)
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY)

        assert scenario.action_time == creation_time
        monkeypatch_timestamp(mp, request_time)

        result = scenario_cancel_method(scenario.scenario_id)
        assert result.status_code == http.client.OK

        scenario = Scenario.objects.get(scenario.scenario_id)
        assert scenario.status == ScenarioFsmStatus.CANCELING
        assert scenario.labels[WORK_STATUS_LABEL_NAME] == ScenarioWorkStatus.CANCELING

    def test_cancel_noc_scenario_not_found(self, test, authorized_scenario_user, scenario_cancel_method):
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY)

        result = scenario_cancel_method(scenario.scenario_id + 1)
        assert result.status_code == http.client.NOT_FOUND

    def test_cancel_noc_scenario_forbidden(self, test, scenario_cancel_method):
        scenario = self._mk_scenario(test, works_status=ScenarioWorkStatus.READY, issuer="other@")

        result = scenario_cancel_method(scenario.scenario_id)
        assert result.status_code == http.client.FORBIDDEN


def test_list_noc_scenarios_with_hosts(walle_test):
    target_scenario_name = "result"
    walle_test.mock_scenario(
        dict(
            scenario_id=2,
            name=target_scenario_name,
            scenario_type=noc_soft_maintenance_script.name,
            issuer="some guy",
            labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHED.value, SWITCH_LABEL_NAME: "test-switch"},
            ticket_key="TEST-2",
            status=ScenarioFsmStatus.FINISHED.value,
            hosts=[1, 2, 3],
        )
    )
    walle_test.mock_scenario(
        dict(
            scenario_id=3,
            name="ignored",
            scenario_type=noop_script.name,
            issuer="some guy",
            ticket_key="TEST-3",
            status=ScenarioFsmStatus.FINISHED.value,
        )
    )  # scenario_other_type

    result = walle_test.api_client.get("/v1/scenarios", query_string={"scenario_type": ScriptName.NOC_SOFT})
    assert result.status_code == http.client.OK
    assert len(result.json["result"]) == 1
    assert result.json["result"][0]["name"] == target_scenario_name


def test_filter_by_works_status(walle_test):
    target_scenario_name = "result"
    walle_test.mock_scenario(
        dict(
            scenario_id=2,
            name=target_scenario_name,
            scenario_type=noc_soft_maintenance_script.name,
            issuer="some guy",
            labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.READY.value, SWITCH_LABEL_NAME: "test-switch"},
            ticket_key="TEST-1",
            status=ScenarioFsmStatus.STARTED.value,
            hosts=[1, 2, 3],
        )
    )
    walle_test.mock_scenario(
        dict(
            scenario_id=3,
            name="other status",
            scenario_type=noc_soft_maintenance_script.name,
            issuer="some guy",
            labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHED, SWITCH_LABEL_NAME: "test-switch-2"},
            ticket_key="TEST-2",
            status=ScenarioFsmStatus.FINISHED,
            hosts=[4, 5, 6],
        )
    )  # scenario_other_status
    walle_test.mock_scenario(
        dict(
            scenario_id=4,
            name="ignored",
            scenario_type=noop_script.name,
            issuer="some guy",
            labels={WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.READY, SWITCH_LABEL_NAME: "test-switch-3"},
            ticket_key="TEST-3",
            status=ScenarioFsmStatus.FINISHED,
        )
    )  # scenario_other_type

    result = walle_test.api_client.get(
        "/v1/scenarios",
        query_string={
            WORK_STATUS_API_PARAM: [ScenarioWorkStatus.READY.value],
            "scenario_type": ScriptName.NOC_SOFT,
        },
    )
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"][0]["name"] == target_scenario_name
