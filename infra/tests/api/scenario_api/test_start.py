import http.client

import pytest

from walle.models import monkeypatch_timestamp
from walle.scenario.constants import ScenarioFsmStatus, ALL_CAN_START_SCENARIO_STATUSES, ScenarioWorkStatus


@pytest.fixture(params=("POST", "PATCH"))
def scenario_start_method(walle_test, request):
    return lambda scenario_id: walle_test.api_client.open(
        "/v1/scenarios/{}/start".format(scenario_id), method=request.param, data={"reason": "reason-mock"}
    )


@pytest.mark.parametrize("scenario_status", ALL_CAN_START_SCENARIO_STATUSES)
def test_start_scenario_successfully(mp, walle_test, scenario_start_method, scenario_status, authorized_scenario_user):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario({"status": scenario_status}, work_status=ScenarioWorkStatus.STARTED)
    scenario.status = ScenarioFsmStatus.STARTED

    result = scenario_start_method(scenario.scenario_id)

    assert result.status_code == http.client.ACCEPTED
    assert result.json["status"] == ScenarioFsmStatus.STARTED
    walle_test.scenarios.assert_equal()


@pytest.mark.parametrize("status", set(ScenarioFsmStatus) - set(ALL_CAN_START_SCENARIO_STATUSES))
def test_start_scenario_not_in_created_status(walle_test, scenario_start_method, status, authorized_scenario_user):
    scenario = walle_test.mock_scenario({"status": status})

    result = scenario_start_method(scenario.scenario_id)

    assert result.status_code == http.client.CONFLICT
    walle_test.scenarios.assert_equal()


def test_start_not_existing_scenario(walle_test, scenario_start_method, authorized_scenario_user):
    result = scenario_start_method(0)

    assert result.status_code == http.client.NOT_FOUND
    walle_test.scenarios.assert_equal()


def test_start_scenario_unauthorized(walle_test, scenario_start_method):
    scenario = walle_test.mock_scenario()

    result = scenario_start_method(scenario.scenario_id)

    assert result.status_code == http.client.FORBIDDEN
    walle_test.scenarios.assert_equal()
