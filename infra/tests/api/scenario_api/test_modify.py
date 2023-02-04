import pytest
import http.client

from infra.walle.server.tests.lib.util import mock_startrek_client
from tests.api.scenario_api.utils import get_scenario_json
from walle.scenario.constants import ScenarioFsmStatus, WORK_STATUS_LABEL_NAME
from walle.scenario.errors import ScenarioValidationError
from walle.scenario.scenario import Scenario


@pytest.mark.parametrize(
    ["modified_args", "status"],
    [
        [{"name": "test_name"}, ScenarioFsmStatus.CREATED],
        [{"name": "test_name", "labels": {3: 3}}, ScenarioFsmStatus.CREATED],
        [{"labels": {3: 3}}, ScenarioFsmStatus.CREATED],
        [{"labels": {3: 3}}, ScenarioFsmStatus.CANCELED],
        [{"labels": {3: 3}}, ScenarioFsmStatus.STARTED],
        [{"labels": {3: 3}}, ScenarioFsmStatus.FINISHED],
    ],
)
def test_modify_scenario(walle_test, modified_args, status, authorized_scenario_user):
    scenario = walle_test.mock_scenario(dict(status=status))
    for arg, value in modified_args.items():
        scenario[arg] = value

    response = walle_test.api_client.patch("/v1/scenarios/{}".format(scenario.scenario_id), data=modified_args)

    assert response.status_code == http.client.ACCEPTED
    scenario = Scenario.objects.get(scenario.scenario_id)
    for arg_name, arg_val in modified_args.items():
        if arg_name == "labels":
            assert {(str(k), v) for k, v in arg_val.items()} <= set(scenario.labels.items())
            assert WORK_STATUS_LABEL_NAME in scenario.labels  # NOTE(rocco66): update labels while modify, not replace
        else:
            assert getattr(scenario, arg_name) == arg_val


def test_modify_scenario_with_existed_labels(walle_test, authorized_scenario_user):
    payload = {"labels": {"b": 2}}
    scenario = walle_test.mock_scenario(dict(labels={"a": 1}))
    scenario.labels = payload["labels"]

    response = walle_test.api_client.patch("/v1/scenarios/{}".format(scenario.scenario_id), data=payload)

    assert response.status_code == http.client.ACCEPTED
    assert set(Scenario.objects.get(scenario.scenario_id).labels) == {"a", "b", WORK_STATUS_LABEL_NAME}


@pytest.mark.parametrize(
    ["modified_args", "status"],
    [
        [{"name": "test_name", "labels": {3: 3}}, ScenarioFsmStatus.STARTED],
        [{"name": "test_name"}, ScenarioFsmStatus.CANCELED],
        [{"name": "test_name"}, ScenarioFsmStatus.STARTED],
        [{"name": "test_name"}, ScenarioFsmStatus.FINISHED],
    ],
)
def test_modify_prohibited_fields_in_started_scenario(walle_test, modified_args, status):
    scenario = walle_test.mock_scenario(dict(status=status))

    result = walle_test.api_client.patch("/v1/scenarios/{}".format(scenario.scenario_id), data=modified_args)

    assert result.status_code == http.client.CONFLICT
    walle_test.scenarios.assert_equal()


def test_modify_scenario_failed_validation_of_labels(walle_test):
    scenario = walle_test.mock_scenario()

    result = walle_test.api_client.patch("/v1/scenarios/{}".format(scenario.scenario_id), data=dict(labels={1: [1, 2]}))

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Labels validation errors: "
        "Key or value isn't [String]/[Integer], wrong pair - 1:[1, 2]"
    )
    walle_test.scenarios.assert_equal()


def test_modify_scenario_failed_validation_of_ticket_key(walle_test, mp):
    st_client = mock_startrek_client(mp)
    st_client.get_issue.side_effect = ScenarioValidationError("mock-error")
    scenario = walle_test.mock_scenario(get_scenario_json())

    result = walle_test.api_client.patch(
        "/v1/scenarios/{}".format(scenario.scenario_id), data={"ticket_key": "WALLE-2"}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert scenario.ticket_key == "WALLE-2413"
    walle_test.scenarios.assert_equal()
