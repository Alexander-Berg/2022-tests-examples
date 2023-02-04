import pytest
import http.client

from tests.api.scenario_api.utils import create_scenarios
from walle.scenario.constants import ScenarioFsmStatus
from walle.scenario.script import wait_time_script


@pytest.mark.parametrize(
    ["arg_name", "arg_value"],
    [
        ["scenario_id", 2],
        ["name", "result"],
        ["scenario_type", wait_time_script.name],
        ["issuer", "some guy"],
        ["ticket_key", "TEST-2"],
        ["status", ScenarioFsmStatus.FINISHED],
    ],
)
def test_get_scenarios_by_specified_argument(walle_test, arg_name, arg_value):
    walle_test.mock_scenario()
    result_scenario = walle_test.mock_scenario(
        dict(
            scenario_id=2,
            name="result",
            scenario_type=wait_time_script.name,
            issuer="some guy",
            ticket_key="TEST-2",
            status=ScenarioFsmStatus.FINISHED,
        )
    )

    result = walle_test.api_client.get("/v1/scenarios?{}={}".format(arg_name, arg_value))
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"] == [result_scenario.to_api_obj()]


def test_get_scenarios_from_empty_db(walle_test):
    result = walle_test.api_client.get("/v1/scenarios")
    assert result.status_code == http.client.OK
    assert result.json["total"] == 0


@pytest.mark.parametrize("reverse", [True, False])
def test_get_all_scenarios_from_db(walle_test, reverse):
    test_data = create_scenarios(walle_test, 5, reverse=reverse)
    result = walle_test.api_client.get("/v1/scenarios", query_string={"reverse": reverse})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 5
    assert result.json["result"] == test_data


def test_get_all_scenarios_with_offset_zero_and_limit_zero(walle_test):
    create_scenarios(walle_test, 5)
    result = walle_test.api_client.get("/v1/scenarios", query_string={"offset": 0, "limit": 0})
    assert len(result.json["result"]) == 0


def test_get_all_scenarios_with_offset_zero_and_limit_one(walle_test):
    test_data = create_scenarios(walle_test, 5)
    result = walle_test.api_client.get("/v1/scenarios", query_string={"offset": 0, "limit": 1})
    assert len(result.json["result"]) == 1
    assert test_data[0] == result.json["result"][0]


def test_get_all_scenarios_with_offset_one_and_limit_one(walle_test):
    test_data = create_scenarios(walle_test, 5)
    result = walle_test.api_client.get("/v1/scenarios", query_string={"offset": 1, "limit": 1})
    assert len(result.json["result"]) == 1
    assert test_data[1] == result.json["result"][0]


def test_get_all_scenarios_with_fields(walle_test):
    scenario = walle_test.mock_scenario()

    result = walle_test.api_client.get("/v1/scenarios", query_string={"fields": ["scenario_id,name"]})
    assert len(result.json["result"]) == 1
    assert [scenario.to_api_obj(["scenario_id", "name"])] == result.json["result"]


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
    result = walle_test.api_client.get("/v1/scenarios")
    assert result.status_code == http.client.OK
    assert result.json["result"] == [scenario.to_api_obj()]
