"""Tests host force status API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    AUDIT_LOG_ID,
    hosts_api_url,
    monkeypatch_locks,
    generate_host_action_authentication_tests,
    any_task_status,
    mock_schedule_assigned,
    monkeypatch_function,
)
from walle.clients import bot
from walle.hosts import HostState, HostStatus

ALLOWED_STATES = set(HostState.ALL)
FORBIDDEN_STATES = set(HostState.ALL) - ALLOWED_STATES
ALLOWED_STATUSES = set(HostStatus.ALL) - {HostStatus.INVALID, HostStatus.MANUAL}
ALLOWED_TO_STATUSES = set(HostStatus.ALL_STEADY) - {HostStatus.MANUAL}

generate_host_action_authentication_tests(globals(), "/force-status", {"status": list(ALLOWED_TO_STATUSES)[0]})


@pytest.fixture
def test(request, monkeypatch, monkeypatch_timestamp, monkeypatch_audit_log):
    monkeypatch_locks(monkeypatch)
    return TestCase.create(request)


@pytest.mark.parametrize("state", ALLOWED_STATES - {HostState.MAINTENANCE})
@pytest.mark.all_status_owner_combinations(include_manual=False)
@pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_status_forcing(test, state, status, owner, to_status, host_id_field):
    host = test.mock_host({"inv": 0, "state": state, "status": status, "status_author": owner})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.OK

    host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
    del host.task

    test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_force_status_maintenance(test, mock_maintenance_host, to_status, host_id_field):
    host = mock_maintenance_host(test)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_force_status_ignore_maintenance(test, mock_maintenance_host, to_status, host_id_field):
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/force-status"),
        query_string="ignore_maintenance=true",
        data={"status": to_status},
    )

    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, to_status)
    test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES - {HostStatus.READY})
def test_stores_provided_ticket_key(monkeypatch, test, to_status):
    monkeypatch_locks(monkeypatch)
    ticket_key = "BURNE-1001"

    host = test.mock_host(
        {"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY, "status_author": "other-user@"}
    )

    result = test.api_client.post(
        hosts_api_url(host, action="/force-status"), data={"status": to_status, "ticket_key": ticket_key}
    )

    assert result.status_code == http.client.OK
    host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
    host.ticket = ticket_key

    test.hosts.assert_equal()


def test_clears_ticket_key_when_forced_to_ready(monkeypatch, test):
    monkeypatch_locks(monkeypatch)
    ticket_key = "BURNE-1001"

    host = test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "ticket": ticket_key,
            "status": HostStatus.DEAD,
            "status_author": "wall-e",
        }
    )

    result = test.api_client.post(hosts_api_url(host, action="/force-status"), data={"status": HostStatus.READY})

    assert result.status_code == http.client.OK
    host.set_status(HostStatus.READY, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
    del host.ticket

    test.hosts.assert_equal()


def test_remembers_ticket_key_when_forced_to_dead(monkeypatch, test):
    monkeypatch_locks(monkeypatch)
    ticket_key = "BURNE-1001"

    host = test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "ticket": ticket_key,
            "status": any_task_status(),
            "status_author": "wall-e",
        }
    )

    result = test.api_client.post(hosts_api_url(host, action="/force-status"), data={"status": HostStatus.DEAD})

    assert result.status_code == http.client.OK

    host.set_status(HostStatus.DEAD, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
    del host.task
    host.ticket = ticket_key

    test.hosts.assert_equal()


def test_missing_host(test):
    result = test.api_client.post("/v1/hosts/1/force-status", data={"status": HostStatus.READY})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()

    result = test.api_client.post("/v1/hosts/missing/force-status", data={"status": HostStatus.READY})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()


if FORBIDDEN_STATES:

    @pytest.mark.parametrize("state", FORBIDDEN_STATES)
    @pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES)
    @pytest.mark.parametrize("host_id_field", ["inv", "name"])
    def test_forbidden_states(test, state, to_status, host_id_field):
        host = test.mock_host(
            {
                "inv": 0,
                "name": "test",
                "state": state,
                "status": HostStatus.READY,
            }
        )

        result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
        assert result.status_code == http.client.CONFLICT
        test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", set(HostStatus.ALL) - ALLOWED_TO_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_forbidden_to_statuses(test, to_status, host_id_field):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
        }
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("to_status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_from_invalid_to_not_allowed_statuses(mp, test, host_id_field, state, to_status):
    monkeypatch_function(mp, bot.get_host_info, module=bot, return_value={})
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": state,
            "status": HostStatus.INVALID,
        }
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", HostStatus.ALL_STEADY)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_from_maintenance_invalid(mp, test, host_id_field, to_status):
    monkeypatch_function(mp, bot.get_host_info, module=bot, return_value={})
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.MAINTENANCE,
            "status": HostStatus.INVALID,
        }
    )
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    if to_status == HostStatus.READY:
        assert result.status_code == http.client.CONFLICT
    else:
        assert result.status_code == http.client.OK
        host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
        test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES - {HostState.MAINTENANCE})
@pytest.mark.parametrize("to_status", set(HostStatus.ALL_STEADY) - {HostStatus.MANUAL})
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_from_assigned_free_or_probation_invalid(mp, test, host_id_field, state, to_status):
    monkeypatch_function(mp, bot.get_host_info, module=bot, return_value={})
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": state,
            "status": HostStatus.INVALID,
        }
    )
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.OK
    host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES - {HostState.MAINTENANCE})
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_from_assigned_free_or_probation_invalid_to_manual_is_forbidden(mp, test, host_id_field, state):
    monkeypatch_function(mp, bot.get_host_info, module=bot, return_value={})
    host = test.mock_host(
        {
            "state": state,
            "status": HostStatus.INVALID,
        }
    )
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/force-status"), data={"status": HostStatus.MANUAL}
    )
    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES - {HostState.MAINTENANCE, HostState.PROBATION})
@pytest.mark.all_status_owner_combinations(include_manual=False)
@pytest.mark.parametrize("to_status", [HostStatus.READY, HostStatus.DEAD])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_downtime_after_force_status_for_all_states_but_maintenance_to_ready_and_dead(
    test, state, status, owner, to_status, host_id_field
):
    host = test.mock_host({"inv": 0, "state": state, "status": status, "status_author": owner, "on_downtime": True})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.OK

    host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False, downtime=False)
    del host.task

    test.hosts.assert_equal()


@pytest.mark.all_status_owner_combinations(include_manual=False)
@pytest.mark.parametrize("to_status", [HostStatus.READY, HostStatus.DEAD])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_downtime_after_force_status_for_probation_to_ready_and_dead(test, status, owner, to_status, host_id_field):
    host = test.mock_host(
        {"inv": 0, "state": HostState.PROBATION, "status": status, "status_author": owner, "on_downtime": True}
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/force-status"), data={"status": to_status})
    assert result.status_code == http.client.OK

    host.set_status(to_status, test.api_issuer, AUDIT_LOG_ID, confirmed=False, downtime=True)
    del host.task

    test.hosts.assert_equal()


@pytest.mark.parametrize("to_status", ALLOWED_TO_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_downtime_for_maintenance(test, mock_maintenance_host, to_status, host_id_field):
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/force-status"),
        query_string="ignore_maintenance=true",
        data={"status": to_status},
    )

    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, to_status)
    test.hosts.assert_equal()
    assert host.on_downtime is True


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_setting_maintenance_is_forbidden_with_further_instructions(test, host_id_field):
    host = test.mock_host({"state": HostState.ASSIGNED})
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/force-status"), data={"status": HostStatus.MANUAL}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Setting maintenance by forcing status \"manual\" "
        "is deprecated. Please use the \"set-maintenance\" method instead."
    )
