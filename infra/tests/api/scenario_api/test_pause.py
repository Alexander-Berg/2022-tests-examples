import pytest
import http.client

from walle.models import monkeypatch_timestamp
from walle.scenario.constants import ScenarioFsmStatus, ALL_CAN_PAUSE_SCENARIO_STATUSES
from walle.scenario.script import ScriptName


@pytest.fixture(params=("POST", "PATCH"))
def scenario_pause_method(walle_test, request):
    return lambda scenario_id: walle_test.api_client.open(
        "/v1/scenarios/{}/pause".format(scenario_id), method=request.param, data={"reason": "reason-mock"}
    )


@pytest.mark.usefixtures("authorized_scenario_user", "monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CAN_PAUSE_SCENARIO_STATUSES)
def test_pause_scenario_by_admin_successfully(mp, walle_test, scenario_pause_method, status):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(dict(status=status))
    result = scenario_pause_method(scenario.scenario_id)
    assert result.status_code == http.client.OK

    scenario.status = ScenarioFsmStatus.PAUSED
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CAN_PAUSE_SCENARIO_STATUSES)
def test_pause_scenario_by_issuer_successfully(mp, walle_test, scenario_pause_method, status):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(dict(status=status, issuer=walle_test.api_issuer))
    result = scenario_pause_method(scenario.scenario_id)
    assert result.status_code == http.client.OK

    scenario.status = ScenarioFsmStatus.PAUSED
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CAN_PAUSE_SCENARIO_STATUSES)
@pytest.mark.parametrize("scenario_type", ScriptName.ALL_THIRD_PARTY_INIT_SCENARIOS)
@pytest.mark.parametrize("responsible_container", ["labels", "script_args"])
def test_host_add_pause_scenario_by_responsible_successfully(
    mp, walle_test, scenario_pause_method, status, scenario_type, responsible_container
):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(
        {
            "issuer": "other-user@",
            "status": status,
            "scenario_type": scenario_type,
            responsible_container: {"responsible": walle_test.api_user},
        }
    )
    result = scenario_pause_method(scenario.scenario_id)
    assert result.status_code == http.client.OK

    scenario.status = ScenarioFsmStatus.PAUSED
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CAN_PAUSE_SCENARIO_STATUSES)
@pytest.mark.parametrize("scenario_type", ScriptName.ALL_THIRD_PARTY_INIT_SCENARIOS)
@pytest.mark.parametrize("author_container", ["labels", "script_args"])
def test_host_add_pause_scenario_by_ticket_author_successfully(
    mp, walle_test, scenario_pause_method, status, scenario_type, author_container
):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(
        {
            "issuer": "other-user@",
            "status": status,
            "scenario_type": scenario_type,
            author_container: {"ticket_created_by": walle_test.api_user},
        }
    )
    result = scenario_pause_method(scenario.scenario_id)
    assert result.status_code == http.client.OK

    scenario.status = ScenarioFsmStatus.PAUSED
    walle_test.scenarios.assert_equal()


def test_pause_scenario_by_non_admin_and_non_issuer(walle_test, scenario_pause_method):
    scenario = walle_test.mock_scenario({"issuer": "other-user@", "status": ScenarioFsmStatus.STARTED})

    result = scenario_pause_method(scenario.scenario_id)
    assert result.status_code == http.client.FORBIDDEN
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("authorized_scenario_user", "monkeypatch_locks")
@pytest.mark.parametrize("status", set(ScenarioFsmStatus) - set(ALL_CAN_PAUSE_SCENARIO_STATUSES))
def test_pause_scenario_that_cannot_be_paused(walle_test, scenario_pause_method, status):
    scenario = walle_test.mock_scenario(dict(status=status))
    result = scenario_pause_method(scenario.scenario_id)

    assert result.status_code == http.client.CONFLICT
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("authorized_scenario_user")
def test_pause_non_existing_scenario(walle_test, scenario_pause_method):
    result = scenario_pause_method(99)
    assert result.status_code == http.client.NOT_FOUND
