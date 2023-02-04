"""Test for handle_failure api handle"""

from unittest.mock import ANY

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    mock_status_reasons,
    monkeypatch_automation_plot_id,
    monkeypatch_expert,
    monkeypatch_function,
    TestCase,
    hosts_api_url,
    mock_schedule_host_reboot,
    generate_host_action_authentication_tests,
    fail_check,
)
from walle.expert import juggler, dmc
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.constants import NETWORK_CHECKS_REACTION_TIMEOUT
from walle.expert.decision import Decision
from walle.expert.types import WalleAction, CheckType
from walle.hosts import HostState, HostStatus
from walle.models import timestamp


@pytest.fixture
def test(request, mp, monkeypatch_timestamp, monkeypatch_check_percentage, mp_juggler_source, monkeypatch_audit_log):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
    monkeypatch_expert(mp, enabled=True)

    return TestCase.create(request, healthdb=True)


generate_host_action_authentication_tests(globals(), "/handle-failure", {})


def _mock_and_patch_reasons(mp, failing_check=None, **kwargs):
    reasons = mock_status_reasons(**kwargs)
    if failing_check:
        fail_check(reasons, failing_check)

    monkeypatch_function(mp, juggler.get_host_health_reasons, return_value=reasons)


def _stub_schedule_action_to_reboot(mp):
    orig_schedule_action = dmc.schedule_action

    def _mock_schedule_action(host, decision, *args, **kwargs):
        mock_decision = Decision(WalleAction.REBOOT, reason=", ".join(decision.checks))
        return orig_schedule_action(host, mock_decision, *args, **kwargs)

    # stub it to always schedule reboot. keep all other parameters.
    mp.function(dmc.schedule_action, side_effect=_mock_schedule_action)


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("check_type", (set(CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB))))
@pytest.mark.parametrize("host_state", HostState.ALL_ASSIGNED)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_passive_checks_failing(test, mp, check_type, host_id_field, host_state):
    host = test.mock_host({"state": host_state})

    _mock_and_patch_reasons(mp, check_type, checks_min_time=timestamp() - 1)
    _stub_schedule_action_to_reboot(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.OK

    # reason is mocked to be a check name
    # this task is meant to be automated, although started by a user action.
    mock_schedule_host_reboot(host, manual=False, issuer=test.api_issuer, reason=check_type)
    _compare_response(result, host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
@pytest.mark.parametrize("host_state", HostState.ALL_ASSIGNED)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_active_checks_failing(test, mp, check_type, host_id_field, host_state):
    host = test.mock_host({"state": host_state})

    check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
    _mock_and_patch_reasons(
        mp,
        check_type,
        effective_timestamp=check_min_time,
        status_mtime=check_min_time - 1,
        check_min_time=check_min_time,
    )
    _stub_schedule_action_to_reboot(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.OK

    # reason is mocked to be a check name
    # this task is meant to be automated, although started by a user action.
    mock_schedule_host_reboot(host, manual=False, issuer=test.api_issuer, reason=check_type)
    _compare_response(result, host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("host_state", HostState.ALL_ASSIGNED)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_all_checks_passing(test, mp, host_id_field, host_state):
    host = test.mock_host({"state": host_state})

    _mock_and_patch_reasons(mp, failing_check=None, checks_min_time=timestamp() - 1)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.OK
    _compare_response(result, host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_admin_only(test, mp, host_id_field):
    host = test.mock_host({"state": HostState.ASSIGNED})
    _mock_and_patch_reasons(mp, CheckType.MEMORY, checks_min_time=timestamp() - 1)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.FORBIDDEN

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("host_state", set(HostState.ALL) - set(HostState.ALL_ASSIGNED))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_only_all_assigned_states(test, mp, host_id_field, host_state):
    host = test.mock_host({"state": host_state})
    _mock_and_patch_reasons(mp, CheckType.MEMORY, checks_min_time=timestamp() - 1)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("host_state", HostState.ALL_ASSIGNED)
@pytest.mark.parametrize("host_status", HostStatus.ALL_TASK)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_only_steady_statuses(test, mp, host_id_field, host_state, host_status):
    host = test.mock_host({"state": host_state, "status": host_status})
    _mock_and_patch_reasons(mp, CheckType.MEMORY, checks_min_time=timestamp() - 1)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_host_in_maintenance_by_other_issuer_not_allowed(test, mp, host_id_field):
    host = test.mock_host({"state": HostState.MAINTENANCE, "state_author": "other-guy"})

    _mock_and_patch_reasons(mp, CheckType.MEMORY, checks_min_time=timestamp() - 1)
    _stub_schedule_action_to_reboot(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/handle-failure"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_host_in_maintenance_by_other_issuer_allowed_with_ignore_maintenance(test, mp, host_id_field):
    host = test.mock_host({"state": HostState.MAINTENANCE, "state_author": "other-guy"})

    _mock_and_patch_reasons(mp, CheckType.MEMORY, checks_min_time=timestamp() - 1)
    _stub_schedule_action_to_reboot(mp)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/handle-failure"), query_string={"ignore_maintenance": True}, data={}
    )
    assert result.status_code == http.client.OK

    # reason is mocked to be a check name
    # this task is meant to be automated, although started by a user action.
    mock_schedule_host_reboot(host, manual=False, issuer=test.api_issuer, reason=CheckType.MEMORY)
    _compare_response(result, host)

    test.hosts.assert_equal()


def _compare_response(result, host):
    assert result.json == {"host": host.to_api_obj(), "dmc_message": ANY}
