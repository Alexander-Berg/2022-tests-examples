import http.client

from walle.errors import ErrorType
from walle.scenario.constants import ScenarioFsmStatus
from walle.scenario.scenario import StageError
from walle.scenario.script import wait_time_script


def test_get_not_existing_scenario(walle_test):
    result = walle_test.api_client.get("/v1/scenarios/0")
    assert result.status_code == http.client.NOT_FOUND


def test_get_existing_scenario(walle_test):
    scenario = walle_test.mock_scenario()
    result = walle_test.api_client.get("/v1/scenarios/{}".format(scenario.scenario_id))
    assert result.json == scenario.to_api_obj()


def test_get_scenario_with_hosts(walle_test):
    invs = [1, 2, 3]

    scenario = walle_test.mock_scenario(
        dict(
            scenario_id=2,
            name="result",
            scenario_type=wait_time_script.name,
            issuer="some guy",
            ticket_key="TEST-2",
            status=ScenarioFsmStatus.FINISHED,
            hosts=invs,
        )
    )
    result = walle_test.api_client.get("/v1/scenarios/2")
    assert result.status_code == http.client.OK
    assert result.json == scenario.to_api_obj()


def test_get_with_fields(walle_test):
    scenario = walle_test.mock_scenario()
    result = walle_test.api_client.get(
        "/v1/scenarios/{}".format(scenario.scenario_id), query_string={"fields": "scenario_id,name"}
    )
    assert result.json == scenario.to_api_obj(["scenario_id", "name"])


def test_get_scenario_with_errors(walle_test):
    errors = {"1": StageError(id="1", type=ErrorType.FIXABLE)}

    scenario = walle_test.mock_scenario(
        dict(
            scenario_id=1,
            name="result",
            scenario_type=wait_time_script.name,
            issuer="some guy",
            ticket_key="TEST-2",
            status=ScenarioFsmStatus.FINISHED,
            errors=errors,
        )
    )
    result = walle_test.api_client.get("/v1/scenarios/1")
    assert result.status_code == http.client.OK
    assert result.json == scenario.to_api_obj()
