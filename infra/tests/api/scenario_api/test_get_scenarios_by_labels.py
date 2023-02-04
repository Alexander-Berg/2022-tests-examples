import pytest
import http.client

from tests.api.scenario_api.utils import create_scenarios
from walle.scenario.constants import ScenarioFsmStatus
from walle.scenario.script import wait_time_script


def test_get_scenarios_by_specified_labels(walle_test):
    a_labels = {"a": "a"}
    b_labels = {"b": "b"}
    scenarios_with_a_labels = create_scenarios(walle_test, 1, labels=a_labels)
    scenarios_with_b_labels = create_scenarios(walle_test, 2, start_idx=1, labels=b_labels)

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": b_labels})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 2
    assert result.json["result"] == scenarios_with_b_labels

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": a_labels})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"] == scenarios_with_a_labels


@pytest.mark.parametrize(
    ["arg_name", "arg_value"],
    [
        ["scenario_id", 2],
        ["name", "test2"],
        ["scenario_type", wait_time_script.name],
        ["issuer", "some little guy"],
        ["ticket_key", "TEST-2"],
        ["status", ScenarioFsmStatus.FINISHED.value],
    ],
)
def test_get_scenarios_with_specified_labels_and_query_arguments(walle_test, arg_name, arg_value):
    b_labels = {"b": "b"}
    create_scenarios(walle_test, 1, labels={"a": "a"})
    first_scenario_with_b_labels = create_scenarios(
        walle_test, 1, start_idx=1, labels=b_labels, status=ScenarioFsmStatus.CREATED
    )
    second_scenario_with_b_labels = create_scenarios(
        walle_test,
        1,
        start_idx=2,
        labels=b_labels,
        status=ScenarioFsmStatus.FINISHED,
        scenario_type=wait_time_script.name,
        issuer="some little guy",
    )

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": b_labels})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 2
    assert result.json["result"] == second_scenario_with_b_labels + first_scenario_with_b_labels

    result = walle_test.api_client.post(
        "/v1/scenarios/labels", query_string={arg_name: arg_value}, data={"labels": b_labels}
    )
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"] == second_scenario_with_b_labels


def test_get_scenario_with_few_specified_labels(walle_test):
    ab = create_scenarios(walle_test, 1, labels={"a": "a", "b": "b"})[0]
    bc = create_scenarios(walle_test, 1, start_idx=1, labels={"b": "b", "c": "c"})[0]
    ac = create_scenarios(walle_test, 1, start_idx=2, labels={"a": "a", "c": "c"})[0]

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": {"a": "a", "b": "b"}})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"][0] == ab

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": {"a": "a"}})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 2
    assert result.json["result"] == [ac, ab]

    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": {"b": "b"}})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 2
    assert result.json["result"] == [bc, ab]


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
            labels={"a": "a"},
        )
    )
    result = walle_test.api_client.post("/v1/scenarios/labels", data={"labels": {"a": "a"}})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 1
    assert result.json["result"] == [scenario.to_api_obj()]


@pytest.mark.parametrize("reverse", [True, False])
def test_get_all_scenarios_from_db(walle_test, reverse):
    test_data = create_scenarios(walle_test, 5, reverse=reverse)
    result = walle_test.api_client.post("/v1/scenarios/labels", query_string={"reverse": reverse}, data={})
    assert result.status_code == http.client.OK
    assert result.json["total"] == 5
    assert result.json["result"] == test_data
