import pytest
import http.client

from walle.models import monkeypatch_timestamp
from walle.scenario.constants import ScenarioFsmStatus, ALL_CANCELABLE_SCENARIO_STATUSES, ScenarioWorkStatus
from walle.scenario.scenario import Scenario
from walle.scenario.script import ScriptName


@pytest.fixture(params=("POST", "PATCH"))
def scenario_cancel_method(walle_test, request):
    return lambda scenario_id: walle_test.api_client.open(
        "/v1/scenarios/{}/cancel".format(scenario_id), method=request.param, data={"reason": "reason-mock"}
    )


def assert_is_canceling(scenario_id):
    scenario = Scenario.objects.get(scenario_id)
    assert scenario.status == ScenarioFsmStatus.CANCELING
    assert scenario.get_works_status() == ScenarioWorkStatus.CANCELING


def assert_is_canceled(scenario_id):
    scenario = Scenario.objects.get(scenario_id)
    assert scenario.status == ScenarioFsmStatus.CANCELED
    assert scenario.get_works_status() == ScenarioWorkStatus.CANCELED


@pytest.mark.usefixtures("authorized_scenario_user", "monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CANCELABLE_SCENARIO_STATUSES)
def test_cancel_cancelable_scenario_by_admin(mp, walle_test, scenario_cancel_method, status):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(dict(status=status))
    result = scenario_cancel_method(scenario.scenario_id)
    assert result.status_code == http.client.OK
    assert_is_canceling(scenario.scenario_id)


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CANCELABLE_SCENARIO_STATUSES)
def test_cancel_cancelable_scenario_by_issuer(mp, walle_test, scenario_cancel_method, status):
    monkeypatch_timestamp(mp, cur_time=0)
    scenario = walle_test.mock_scenario(dict(status=status, issuer=walle_test.api_issuer))
    result = scenario_cancel_method(scenario.scenario_id)
    assert result.status_code == http.client.OK
    assert_is_canceling(scenario.scenario_id)


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CANCELABLE_SCENARIO_STATUSES)
@pytest.mark.parametrize("scenario_type", ScriptName.ALL_THIRD_PARTY_INIT_SCENARIOS)
@pytest.mark.parametrize("responsible_container", ["labels", "script_args"])
def test_host_add_cancel_cancelable_scenario_by_responsible(
    mp, walle_test, scenario_cancel_method, status, scenario_type, responsible_container
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
    result = scenario_cancel_method(scenario.scenario_id)
    assert result.status_code == http.client.OK
    assert_is_canceling(scenario.scenario_id)


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("status", ALL_CANCELABLE_SCENARIO_STATUSES)
@pytest.mark.parametrize("scenario_type", ScriptName.ALL_THIRD_PARTY_INIT_SCENARIOS)
@pytest.mark.parametrize("author_container", ["labels", "script_args"])
def test_host_add_cancel_cancelable_scenario_by_ticket_author(
    mp, walle_test, scenario_cancel_method, status, scenario_type, author_container
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
    result = scenario_cancel_method(scenario.scenario_id)
    assert result.status_code == http.client.OK
    assert_is_canceling(scenario.scenario_id)


def test_cancel_scenario_by_non_admin_and_non_issuer_is_not_allowed(walle_test, scenario_cancel_method):
    scenario = walle_test.mock_scenario({"issuer": "other-user@"})
    assert scenario.status == ScenarioFsmStatus.CREATED

    result = scenario_cancel_method(scenario.scenario_id)
    assert result.status_code == http.client.FORBIDDEN
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("authorized_scenario_user", "monkeypatch_locks")
@pytest.mark.parametrize("status", set(ScenarioFsmStatus) - set(ALL_CANCELABLE_SCENARIO_STATUSES))
def test_cancel_non_cancelable_scenario(walle_test, scenario_cancel_method, status):
    scenario = walle_test.mock_scenario(dict(status=status))
    result = scenario_cancel_method(scenario.scenario_id)

    assert result.status_code == http.client.CONFLICT
    walle_test.scenarios.assert_equal()


@pytest.mark.usefixtures("authorized_scenario_user")
def test_cancel_non_existing_scenario(walle_test, scenario_cancel_method):
    result = scenario_cancel_method(99)
    assert result.status_code == http.client.NOT_FOUND
