import http.client
import json
import os

import pytest

from infra.walle.server.tests.lib.util import AUDIT_LOG_ID, monkeypatch_config, mock_startrek_client
from sepelib.core.constants import DAY_SECONDS, HOUR_SECONDS
from walle.scenario.constants import ScriptName
from walle.application import app
from walle.models import timestamp, monkeypatch_timestamp, FsmHandbrake
from walle.util.misc import drop_none
from walle.scenario.scenario_fsm import _on_handbrake, _run_scenario
from walle.scenario.scenario import Scenario


def reset_settings(fsm_handbrake=None):
    settings = app.settings()

    if fsm_handbrake:
        settings.fsm_handbrake = fsm_handbrake
    else:
        del settings.fsm_handbrake

    settings.save()
    return settings


def enable_fsm_handbrake(walle_test, method, enable=True, timeout=None):
    return walle_test.api_client.open(
        "/v1/settings",
        method=method,
        data=drop_none({"enable_fsm_handbrake": enable, "fsm_handbrake_timeout": timeout}),
    )


def test_get_settings(walle_test, iterate_authentication):
    result = walle_test.api_client.get("/v1/settings")
    assert result.status_code == http.client.OK
    assert result.json == app.settings().to_api_obj()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(walle_test, unauthenticated, method):
    settings = app.settings()
    assert settings.failure_log_start_time == 0
    assert not settings.disable_healing_automation
    assert not settings.disable_dns_automation

    result = walle_test.api_client.open("/v1/settings", method=method, data={"disable_automation": True})
    assert result.status_code == http.client.UNAUTHORIZED
    assert json.loads(app.settings().to_json()) == json.loads(settings.to_json())


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(walle_test, method):
    settings = app.settings()
    assert settings.failure_log_start_time == 0
    assert not settings.disable_healing_automation
    assert not settings.disable_dns_automation

    result = walle_test.api_client.open("/v1/settings", method=method, data={"disable_automation": True})
    assert result.status_code == http.client.FORBIDDEN
    assert json.loads(app.settings().to_json()) == json.loads(settings.to_json())


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_invalid_update(walle_test, authorized_admin, method):
    settings = app.settings()

    result = walle_test.api_client.open("/v1/settings", method=method, data={})
    assert result.status_code == http.client.BAD_REQUEST

    result = walle_test.api_client.open("/v1/settings", method=method, data={"invalid": "value"})
    assert result.status_code == http.client.BAD_REQUEST

    assert json.loads(app.settings().to_json()) == json.loads(settings.to_json())


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("disable_automation", [True, False])
@pytest.mark.parametrize("healing_disabled", [True, False])
@pytest.mark.parametrize("dns_disabled", [True, False])
def test_update_disable_automation(
    monkeypatch, authorized_admin, walle_test, method, disable_automation, healing_disabled, dns_disabled
):
    monkeypatch_timestamp(monkeypatch)
    # for the sake of readability
    enable_automation = not disable_automation
    was_enabled = healing_disabled is False and dns_disabled is False

    settings = app.settings()
    assert settings.failure_log_start_time == 0

    settings.disable_healing_automation = healing_disabled
    settings.disable_dns_automation = dns_disabled
    settings.save()

    result = walle_test.api_client.open("/v1/settings", method=method, data={"disable_automation": disable_automation})

    assert result.status_code == http.client.OK, result.status + result.data

    updated = app.settings()
    assert updated.disable_healing_automation == disable_automation
    assert updated.disable_dns_automation == disable_automation

    if enable_automation and not was_enabled:
        # change this value when actually turning automation on.
        assert updated.failure_log_start_time == timestamp()
    else:
        assert updated.failure_log_start_time == 0


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("disable_healing", [True, False])
@pytest.mark.parametrize("healing_disabled", [True, False])
@pytest.mark.parametrize("dns_disabled", [True, False])
def test_update_disable_healing_automation(
    monkeypatch, authorized_admin, walle_test, method, disable_healing, healing_disabled, dns_disabled
):
    monkeypatch_timestamp(monkeypatch)
    # for the sake of readability
    enable_automation = not disable_healing

    settings = app.settings()
    assert settings.failure_log_start_time == 0

    settings.disable_healing_automation = healing_disabled
    settings.disable_dns_automation = dns_disabled
    settings.save()

    result = walle_test.api_client.open("/v1/settings", method=method, data={"disable_healing": disable_healing})

    assert result.status_code == http.client.OK, result.status + result.data

    updated = app.settings()

    assert updated.disable_healing_automation is disable_healing
    assert updated.disable_dns_automation is dns_disabled

    if enable_automation and healing_disabled:
        # change this value when actually turning automation on.
        assert updated.failure_log_start_time == timestamp()
    else:
        assert updated.failure_log_start_time == 0


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("disable_dns_automation", [True, False])
@pytest.mark.parametrize("healing_disabled", [True, False])
@pytest.mark.parametrize("dns_disabled", [True, False])
def test_update_disable_dns_automation(
    monkeypatch, authorized_admin, walle_test, method, disable_dns_automation, healing_disabled, dns_disabled
):
    monkeypatch_timestamp(monkeypatch)
    # for the sake of readability
    enable_automation = not disable_dns_automation

    settings = app.settings()
    assert settings.failure_log_start_time == 0

    settings.disable_healing_automation = healing_disabled
    settings.disable_dns_automation = dns_disabled
    settings.save()

    result = walle_test.api_client.open(
        "/v1/settings", method=method, data={"disable_dns_automation": disable_dns_automation}
    )

    assert result.status_code == http.client.OK, result.status + result.data

    updated = app.settings()
    assert updated.disable_healing_automation == healing_disabled
    assert updated.disable_dns_automation == disable_dns_automation

    if enable_automation and dns_disabled:
        # change this value when actually turning automation on.
        assert updated.failure_log_start_time == timestamp()
    else:
        assert updated.failure_log_start_time == 0

    result = walle_test.api_client.open("/v1/settings", method=method, data={"disable_automation": True})
    assert result.status_code == http.client.OK
    settings.disable_automation = True
    assert app.settings() == settings


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_bump_inventory_sync_invalid_hosts_limit(walle_test, method, authorized_admin):
    settings = app.settings()
    del settings.inventory_invalid_hosts_limit
    settings.save()

    result = walle_test.api_client.open("/v1/settings", method=method, data={"inventory_invalid_hosts_limit": 1000})
    assert result.status_code == http.client.OK, result.status + result.data

    updated = app.settings()
    assert updated.inventory_invalid_hosts_limit == 1000


@pytest.mark.usefixtures("monkeypatch_timestamp", "authorized_admin")
class TestFsmHandbrake:
    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    def test_set_fsm_handbrake_without_timeout_sets_fsm_handbrake_for_a_day(self, walle_test, method):
        reset_settings()

        result = enable_fsm_handbrake(walle_test, method=method)
        assert result.status_code == http.client.OK, result.status + result.data

        updated = app.settings()
        assert updated.fsm_handbrake.timeout_time == timestamp() + DAY_SECONDS

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    def test_set_fsm_handbrake_with_timeout_sets_fsm_handbrake_for_a_timeout(self, walle_test, method):
        reset_settings()

        result = enable_fsm_handbrake(walle_test, method=method, timeout=HOUR_SECONDS)
        assert result.status_code == http.client.OK, result.status + result.data

        updated = app.settings()
        assert updated.fsm_handbrake.timeout_time == timestamp() + HOUR_SECONDS

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    @pytest.mark.parametrize("enable", [True, None])
    def test_extending_fsm_handbrake_with_new_timeout(self, walle_test, method, enable):
        reset_settings(FsmHandbrake(timeout_time=timestamp(), audit_log_id=AUDIT_LOG_ID))

        result = enable_fsm_handbrake(walle_test, method=method, enable=enable, timeout=HOUR_SECONDS)
        assert result.status_code == http.client.OK, result.status + result.data

        updated = app.settings()
        assert updated.fsm_handbrake.timeout_time == timestamp() + HOUR_SECONDS

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    def test_extending_disabled_fsm_handbrake_is_bad_request(self, walle_test, method):
        old_settings = reset_settings()

        result = enable_fsm_handbrake(walle_test, method=method, enable=None, timeout=HOUR_SECONDS)
        assert result.status_code == http.client.BAD_REQUEST, result.status + result.data

        updated = app.settings()
        assert old_settings == updated

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    @pytest.mark.parametrize("timeout", [HOUR_SECONDS, None])
    def test_disabling_disabled_fsm_handbrake_is_noop(self, walle_test, method, timeout):
        old_settings = reset_settings()

        result = enable_fsm_handbrake(walle_test, method=method, enable=False, timeout=timeout)
        assert result.status_code == http.client.OK, result.status + result.data

        updated = app.settings()
        assert old_settings == updated


def test_get_config(walle_test, iterate_authentication):
    os.environ["BSCONFIG_ITAGS"] = "a_itype_walleapi a_ctype_test"

    result = walle_test.api_client.get("/v1/config")
    assert result.status_code == http.client.OK

    expected_response = dict(
        # needed for the yasm panel in the UI
        ctype="test",
    )
    assert result.json == expected_response


@pytest.mark.parametrize("all_write_methods", ["POST", "PATCH", "PUT", "DELETE"])
def test_config_is_readonly(walle_test, authorized_admin, all_write_methods):
    result = walle_test.api_client.open("/v1/config", method=all_write_methods)
    assert result.status_code == http.client.METHOD_NOT_ALLOWED


def test_get_checks_percentage_index(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    expected_result = {"result": [{"check": check, "percent": percent} for check, percent in overrides_mock.items()]}

    result = walle_test.api_client.open("/v1/settings/checks_percentage")
    assert expected_result == result.json


@pytest.mark.usefixtures("authorized_admin")
def test_update_checks_percentage(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    expected_result = {"check": "check_mock2", "percent": 42}

    result = walle_test.api_client.open(
        "/v1/settings/checks_percentage/check_mock2", method="PUT", data={"percent": 42}
    )
    settings = app.settings()

    assert result.status_code == http.client.OK
    assert result.json == expected_result
    assert settings.checks_percentage_overrides["check_mock2"] == 42


@pytest.mark.usefixtures("authorized_admin")
def test_create_checks_percentage(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    expected_result = {"check": "check_mock3", "percent": 42}

    result = walle_test.api_client.open(
        "/v1/settings/checks_percentage/check_mock3", method="PUT", data={"percent": 42}
    )
    settings = app.settings()

    assert result.status_code == http.client.OK
    assert result.json == expected_result
    assert settings.checks_percentage_overrides["check_mock3"] == 42


@pytest.mark.usefixtures("authorized_admin")
def test_get_checks_percentage_not_found(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    result = walle_test.api_client.open("/v1/settings/checks_percentage/check_mock3", method="GET")

    assert result.status_code == http.client.NOT_FOUND


def test_get_checks_percentage(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    expected_result = {"check": "check_mock2", "percent": 50}

    result = walle_test.api_client.open("/v1/settings/checks_percentage/check_mock2", method="GET")

    assert result.status_code == http.client.OK
    assert result.json == expected_result


@pytest.mark.usefixtures("authorized_admin")
def test_delete_checks_percentage_override(walle_test):
    overrides_mock = {"check_mock1": 100, "check_mock2": 50}
    settings = app.settings()
    settings.checks_percentage_overrides = overrides_mock
    settings.save()

    expected_result = {"check": "check_mock2", "percent": 50}
    expected_overrides = {"check_mock1": 100}

    result = walle_test.api_client.open("/v1/settings/checks_percentage/check_mock2", method="DELETE")

    settings = app.settings()

    assert expected_result == result.json
    assert expected_overrides == settings.checks_percentage_overrides


def test_update_checks_percentage_forbidden(walle_test):

    result = walle_test.api_client.open("/v1/settings/checks_percentage/check_mock", method="PUT", data={})

    assert result.status_code == http.client.FORBIDDEN


def test_delete_checks_percentage_forbidden(walle_test):

    result = walle_test.api_client.open("/v1/settings/checks_percentage/check_mock", method="DELETE", data={})

    assert result.status_code == http.client.FORBIDDEN


MOCK_DMC_RULE_JSON = {
    "rule_query": {
        "project_ids_included": ["a", "b"],
        "project_ids_excluded": ["c", "d"],
        "tiers_included": [1, 2],
        "tiers_excluded": [3, 4],
        "physical_locations_included": [
            "COUNTRY|CITY|DATACENTER|QUEUE|RACK",
        ],
        "physical_locations_excluded": [
            "COUNTRY|CITY|DATACENTER|QUEUE|RACK2",
        ],
    }
}


@pytest.mark.usefixtures("unauthenticated")
def test_create_dmc_rule_unauthenticated(walle_test):
    result = walle_test.api_client.post("/v1/settings/dmc_rules", data=MOCK_DMC_RULE_JSON)

    assert result.status_code == http.client.UNAUTHORIZED
    walle_test.dmc_rules.assert_equal()


def test_create_dmc_rule_not_authorized(walle_test):
    result = walle_test.api_client.post("/v1/settings/dmc_rules", data=MOCK_DMC_RULE_JSON)

    assert result.status_code == http.client.FORBIDDEN
    walle_test.dmc_rules.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
def test_create_dmc_rule_as_admin_successfully(walle_test):
    result = walle_test.api_client.post("/v1/settings/dmc_rules", data=MOCK_DMC_RULE_JSON)

    assert result.status_code == http.client.OK

    walle_test.dmc_rules.mock(MOCK_DMC_RULE_JSON, save=False)
    walle_test.dmc_rules.assert_equal()


def test_get_dmc_rule_by_id(walle_test):
    dmc_rule = walle_test.dmc_rules.mock(MOCK_DMC_RULE_JSON)

    result = walle_test.api_client.open("/v1/settings/dmc_rules/{}".format(dmc_rule.id), method="GET")

    assert result.status_code == http.client.OK


@pytest.mark.usefixtures("authorized_admin")
def test_delete_dmc_rule_by_id(walle_test):
    dmc_rule = walle_test.dmc_rules.mock(MOCK_DMC_RULE_JSON, add=False)

    result = walle_test.api_client.delete("/v1/settings/dmc_rules/{}".format(dmc_rule.id))

    assert result.status_code == http.client.NO_CONTENT
    walle_test.dmc_rules.assert_equal()


@pytest.fixture()
def monkeypatch_startrek_options(mp):
    monkeypatch_config(mp, "startrek.access_token", "startrek-access-token-mock")


@pytest.mark.usefixtures("authorized_admin", "monkeypatch_startrek_options")
def test_scenario_fsm_handbrake(mp, walle_test):
    monkeypatch_timestamp(mp, cur_time=0)

    walle_test.mock_host()

    scenario_params = dict(
        name="noc-scenario-execution-test",
        reason="Test NOC scenario",
        ticket_key="WALLE-1",
        scenario_type=ScriptName.NOC_SOFT,
        script_args={
            "switch": "switch-mock",
        },
    )

    mock_startrek_client(mp)
    walle_test.api_client.post("v1/scenarios", data=scenario_params)

    _run_scenario(scenario_id=0)
    assert Scenario.objects.get(scenario_id=0).revision == 1

    # Run: Call API to enable handbrake for Scenario FSM and check if it works
    walle_test.api_client.open("/v1/settings", method="POST", data={"enable_scenario_fsm_handbrake": True})
    assert _on_handbrake()
    _run_scenario(scenario_id=0)
    assert Scenario.objects.get(scenario_id=0).revision == 1

    # Run: Call API to disable handbrake for Scenario FSM
    walle_test.api_client.open("/v1/settings", method="POST", data={"enable_scenario_fsm_handbrake": False})
    assert not _on_handbrake()
    _run_scenario(scenario_id=0)
    assert Scenario.objects.get(scenario_id=0).revision == 2
