"""Tests host task cancellation API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    AUDIT_LOG_ID,
    hosts_api_url,
    monkeypatch_locks,
    generate_host_action_authentication_tests,
    mock_task,
    mock_task_cancellation,
)
from walle.hosts import HostState, HostStatus
from walle.operations_log.constants import OPERATION_HOST_STATUSES

HOST_ACTION_URL = "/cancel-task"

generate_host_action_authentication_tests(globals(), HOST_ACTION_URL, data={})


@pytest.fixture
def test(request, monkeypatch, monkeypatch_timestamp, monkeypatch_audit_log):
    monkeypatch_locks(monkeypatch)
    return TestCase.create(request)


def mock_host_task_cancellation(host, issuer, reason=None):
    host.set_status(HostStatus.default(host.state), issuer, AUDIT_LOG_ID, confirmed=False)
    if host.status not in HostStatus.ALL_STEADY:
        mock_task_cancellation(host)
    host.status_author = issuer
    host.status_reason = reason or host.status_reason
    host.status = HostStatus.default(host.state)
    del host.task


@pytest.mark.parametrize("state", set(HostState.ALL) - {HostState.MAINTENANCE})
@pytest.mark.parametrize("status", OPERATION_HOST_STATUSES[:5])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_cancel_task(test, state, status, host_id_field):
    host = test.mock_host(
        {
            "state": state,
            "status": status,
            "task": mock_task(),
        }
    )
    reason = "Test reason"

    result = test.api_client.post(hosts_api_url(host, host_id_field, HOST_ACTION_URL), data={"reason": reason})
    assert result.status_code == http.client.OK

    mock_host_task_cancellation(host, test.api_issuer, reason)

    test.hosts.assert_equal()


@pytest.mark.parametrize("ignore_maintenance", (True, False))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_cancel_task__maintenance(test, ignore_maintenance, host_id_field):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "status": OPERATION_HOST_STATUSES[0],
            "state_author": "other-user@",
            "status_author": "other-user@",
            "task": mock_task(),
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, HOST_ACTION_URL),
        query_string="ignore_maintenance={}".format(str(ignore_maintenance).lower()),
        data={},
    )
    if ignore_maintenance:
        assert result.status_code == http.client.OK
        mock_host_task_cancellation(host, test.api_issuer)
    else:
        assert result.status_code == http.client.CONFLICT
        assert (
            "The host is under maintenance by other-user@. "
            "Add 'ignore maintenance' flag to your request "
            "if this action won't break anything." in result.json["message"]
        )

    test.hosts.assert_equal()


@pytest.mark.parametrize("ignore_maintenance", (True, False))
@pytest.mark.parametrize("status", HostStatus.ALL_STEADY)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_cancel_task__maintenance_steady(test, ignore_maintenance, status, host_id_field):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "status": status,
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, HOST_ACTION_URL),
        query_string="ignore_maintenance={}".format(str(ignore_maintenance).lower()),
        data={},
    )

    assert result.status_code == http.client.OK
    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_no_host(test, host_id_field):
    host = test.mock_host()
    host.delete()
    result = test.api_client.post(hosts_api_url(host, host_id_field, HOST_ACTION_URL), data={})
    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "The specified host doesn't exist."


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", HostStatus.ALL_STEADY)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_no_task(test, state, status, host_id_field):
    host = test.mock_host(
        {
            "state": state,
            "status": status,
        }
    )
    result = test.api_client.post(hosts_api_url(host, host_id_field, HOST_ACTION_URL), data={})
    assert result.status_code == http.client.OK

    test.hosts.assert_equal()
