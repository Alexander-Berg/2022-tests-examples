"""Tests API for setting host maintenance."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    hosts_api_url,
    monkeypatch_locks,
    generate_host_action_authentication_tests,
    mock_task,
    mock_schedule_assigned,
)
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none

ALLOWED_STATES = set(HostState.ALL_ASSIGNED)
ALLOWED_STATUSES = {HostStatus.READY, HostStatus.MANUAL, HostStatus.DEAD}
FORBIDDEN_STATES = set(HostState.ALL) - ALLOWED_STATES

generate_host_action_authentication_tests(globals(), "/set-assigned", {})


@pytest.fixture
def test(request, monkeypatch, monkeypatch_timestamp, monkeypatch_audit_log, cms_accept):
    monkeypatch_locks(monkeypatch)
    return TestCase.create(request)


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_status", HostStatus.ALL_ASSIGNED + [None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_schedules_a_task(test, state, status, target_status, host_id_field):
    host = test.mock_host({"inv": 0, "state": state, "status": status, "status_author": test.api_issuer})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-assigned"), data=drop_none({"status": target_status})
    )
    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, status=target_status)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_status", HostStatus.ALL_ASSIGNED + [None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_schedules_a_task_with_power_on(test, state, status, target_status, host_id_field):
    host = test.mock_host({"inv": 0, "state": state, "status": status, "status_author": test.api_issuer})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-assigned"), data=drop_none({"status": target_status, "power_on": True})
    )
    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, status=target_status, power_on=True)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", HostStatus.ALL_TASK)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_from_task_is_forbidden(test, state, status, host_id_field):
    host = test.mock_host({"inv": 0, "state": state, "status": status, "task": mock_task()})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/set-assigned"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_TASK) - ALLOWED_STATUSES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_from_forbidden_statuses_is_not_allowed(test, state, status, host_id_field):
    host = test.mock_host({"inv": 0, "state": state, "status": status})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/set-assigned"), data={})
    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_rejects_when_issuer_does_not_own_maintenance(test, host_id_field, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/set-assigned"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_allows_when_issuer_ignores_maintenance(test, host_id_field, mock_maintenance_host):
    host = mock_maintenance_host(test)
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-assigned"),
        data={},
        query_string={"ignore_maintenance": True},
    )
    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, status=None)
    test.hosts.assert_equal()


def test_missing_host(test):
    result = test.api_client.post("/v1/hosts/1/set-assigned", data={})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()

    result = test.api_client.post("/v1/hosts/missing/set-assigned", data={})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()


if FORBIDDEN_STATES:

    @pytest.mark.parametrize("state", FORBIDDEN_STATES)
    @pytest.mark.parametrize("status", ALLOWED_STATUSES)
    @pytest.mark.parametrize("host_id_field", ["inv", "name"])
    def test_set_assigned_from_forbidden_states_is_not_allowed(test, state, status, host_id_field):
        host = test.mock_host({"inv": 0, "state": state, "status": status})

        result = test.api_client.post(hosts_api_url(host, host_id_field, "/set-assigned"), data={})
        assert result.status_code == http.client.CONFLICT
        test.hosts.assert_equal()

    @pytest.mark.parametrize("state", FORBIDDEN_STATES)
    @pytest.mark.parametrize("status", HostStatus.ALL_TASK)
    @pytest.mark.parametrize("host_id_field", ["inv", "name"])
    def test_set_assigned_from_forbidden_states_with_task_is_not_allowed(test, state, status, host_id_field):
        host = test.mock_host({"inv": 0, "state": state, "status": status, "task": mock_task()})

        result = test.api_client.post(hosts_api_url(host, host_id_field, "/set-assigned"), data={})
        assert result.status_code == http.client.CONFLICT
        test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_assigned_allows_with_empty_ipmi(host_id_field, test, mock_maintenance_host, monkeypatch):
    host = mock_maintenance_host(test)
    host.ipmi_mac = None
    host.save()

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-assigned"),
        data={},
        query_string={"ignore_maintenance": True},
    )
    assert result.status_code == http.client.OK

    mock_schedule_assigned(host, status=None)
    test.hosts.assert_equal()
