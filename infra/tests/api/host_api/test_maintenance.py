"""Tests API for setting host maintenance."""

from unittest.mock import Mock, call

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    hosts_api_url,
    monkeypatch_locks,
    generate_host_action_authentication_tests,
    mock_schedule_maintenance,
    mock_task,
    mock_startrek_client,
)
from sepelib.core.constants import MINUTE_SECONDS
from sepelib.yandex.startrek import StartrekConnectionError, StartrekRequestError
from walle import restrictions
from walle.clients.cms import CmsTaskAction
from walle.host_status import MIN_STATE_TIMEOUT, MAX_STATE_TIMEOUT
from walle.hosts import HostState, HostOperationState, HostStatus, StateExpire
from walle.models import timestamp
from walle.util.misc import drop_none

ALLOWED_STATES = {HostState.ASSIGNED, HostState.PROBATION}
ALLOWED_TARGET_OPERATION_STATES = {HostOperationState.OPERATION, HostOperationState.DECOMMISSIONED}
FORBIDDEN_TARGET_OPERATION_STATES = set(HostOperationState.ALL) - ALLOWED_TARGET_OPERATION_STATES
FORBIDDEN_STATES = set(HostState.ALL) - ALLOWED_STATES
ALLOWED_STATUSES = {HostStatus.READY, HostStatus.DEAD}
ALLOWED_TIMEOUT_STATUS = {HostStatus.READY, HostStatus.DEAD}

generate_host_action_authentication_tests(globals(), "/set-maintenance", {"ticket_key": "MOCK-1234"})


def _mock_state_expire(ticket_key, timeout_status=HostStatus.READY, issuer=TestCase.api_issuer, timeout_time=None):
    if timeout_time is False:
        timeout_time = None
    elif not timeout_time:
        timeout_time = timestamp() + MIN_STATE_TIMEOUT + 10 * MINUTE_SECONDS

    return StateExpire(
        time=timeout_time,
        ticket=ticket_key,
        status=timeout_status,
        issuer=issuer,
    )


@pytest.fixture
def test(request, monkeypatch, monkeypatch_timestamp, monkeypatch_audit_log, cms_accept):
    monkeypatch_locks(monkeypatch)
    return TestCase.create(request)


@pytest.fixture
def timeout_time():
    # now + min timeout + some time
    return timestamp() + MIN_STATE_TIMEOUT + 202


@pytest.fixture
def ticket_key():
    return "MOCK-1234"


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_maintenance_task(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(host, operation_state=target_operation_state, ticket_key=ticket_key)
    test.hosts.assert_equal()


@pytest.mark.parametrize("ignore_cms", [True, False, None])
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_maintenance_task_with_ignore_cms(
    test, target_operation_state, ignore_cms, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data=drop_none({"ignore_cms": ignore_cms, "ticket_key": ticket_key, "operation_state": target_operation_state}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(
        host, ticket_key=ticket_key, ignore_cms=ignore_cms or False, operation_state=target_operation_state
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("power_off", [True, False, None])
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_maintenance_task_with_power_off(
    test, power_off, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data=drop_none({"power_off": power_off, "ticket_key": ticket_key, "operation_state": target_operation_state}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(host, ticket_key=ticket_key, power_off=power_off, operation_state=target_operation_state)
    test.hosts.assert_equal()


@pytest.mark.parametrize("disable_admin_requests", [True, False, None])
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_maintenance_task_with_disable_admin_requests(
    test, disable_admin_requests, target_operation_state, host_id_field, startrek_client, ticket_key
):

    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data=drop_none(
            {
                "power_off": True,
                "disable_admin_requests": disable_admin_requests,
                "ticket_key": ticket_key,
                "operation_state": target_operation_state,
            }
        ),
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(
        host,
        ticket_key=ticket_key,
        power_off=True,
        disable_admin_requests=disable_admin_requests,
        operation_state=target_operation_state,
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_does_not_schedule_a_maintenance_task_with_power_off_if_reboot_restricted(
    test, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "status_author": "previous-user@",
            "restrictions": [restrictions.REBOOT],
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data=drop_none({"power_off": True, "ticket_key": ticket_key, "operation_state": target_operation_state}),
    )
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_maintenance_task_without_power_off_even_if_reboot_restricted(
    test, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "status_author": "previous-user@",
            "restrictions": [restrictions.REBOOT],
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data=drop_none({"power_off": False, "ticket_key": ticket_key, "operation_state": target_operation_state}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(host, ticket_key=ticket_key, operation_state=target_operation_state)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("timeout_status", ALLOWED_TIMEOUT_STATUS)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_task_with_requested_target_status(
    test, state, status, timeout_status, target_operation_state, host_id_field, startrek_client, ticket_key
):

    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"timeout_status": timeout_status, "ticket_key": ticket_key, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.OK

    mock_schedule_maintenance(
        host, ticket_key=ticket_key, timeout_status=timeout_status, operation_state=target_operation_state
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_schedules_a_task_with_requested_timeout(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.OK

    mock_schedule_maintenance(host, ticket_key=ticket_key, operation_state=target_operation_state)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("cms_task_action", [CmsTaskAction.PROFILE, CmsTaskAction.REBOOT])
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_with_specified_cms_task(
    test, state, status, target_operation_state, cms_task_action, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "cms_task_action": cms_task_action, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.OK

    mock_schedule_maintenance(
        host, ticket_key=ticket_key, cms_task_action=cms_task_action, operation_state=target_operation_state
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("cms_task_action", set(CmsTaskAction.ALL) - {CmsTaskAction.PROFILE, CmsTaskAction.REBOOT})
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_with_invalid_cms_task(
    test, cms_task_action, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "cms_task-action": cms_task_action, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_stores_ticket_key(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@"})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.OK
    startrek_client.get_issue.assert_called_once_with(ticket_key)

    mock_schedule_maintenance(host, ticket_key=ticket_key, operation_state=target_operation_state)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_remembers_ticket_key_when_not_ticket_provided(
    test, state, status, target_operation_state, host_id_field, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "status_author": "previous-user@", "ticket": ticket_key})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"), data={"operation_state": target_operation_state}
    )
    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_with_invalid_ticket_key(
    test, state, status, target_operation_state, host_id_field, startrek_client
):
    host = test.mock_host({"state": state, "status": status})

    ticket_key = "0000"
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.BAD_REQUEST
    startrek_client.get_issue.assert_not_called()

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_missing_ticket_key_in_st(
    test, state, status, target_operation_state, host_id_field, startrek_client
):
    host = test.mock_host({"state": state, "status": status})

    startrek_client.get_issue.side_effect = StartrekRequestError(Mock(status_code=404), "Some error.")

    ticket_key = "MOCK-1234"
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: "
        "Cannot use the ticket for maintenance: specified ticket does not exist."
    )
    startrek_client.get_issue.assert_called_once_with(ticket_key)

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_restricted_ticket_key_in_st(test, host_id_field, startrek_client):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    startrek_client.get_issue.side_effect = StartrekRequestError(Mock(status_code=403), "Some error.")

    ticket_key = "MOCK-1234"
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": HostOperationState.OPERATION},
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == (
        "Request validation error: Cannot use the ticket for maintenance: "
        "Wall-E does not have permission to read the ticket. "
        "Try adding robot-walle@ to followers."
    )
    startrek_client.get_issue.assert_called_once_with(ticket_key)

    test.hosts.assert_equal()


@pytest.mark.slow  # Marked as slow because retries are used
@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_st_connection_error(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status})

    startrek_client.get_issue.side_effect = StartrekConnectionError("Some error.")

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Cannot use the ticket for maintenance: "
        "Startrek is not responding. "
        "Please use timeout if you want to set maintenance immediately."
    )
    startrek_client.get_issue.assert_has_calls([call(ticket_key) for _ in range(3)])

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_without_ticket_and_timeout(test, state, status, target_operation_state, host_id_field):
    host = test.mock_host({"state": state, "status": status})
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"), data={"operation_state": target_operation_state}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert "data must contain ['ticket_key'] properties" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_do_not_check_ticket_if_timeout_specified(
    test, state, status, target_operation_state, host_id_field, timeout_time, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "timeout_time": timeout_time, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.OK

    startrek_client.get_issue.assert_not_called()
    mock_schedule_maintenance(
        host, ticket_key=ticket_key, timeout_time=timeout_time, operation_state=target_operation_state
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_ticket_closed(test, mp, state, status, target_operation_state, host_id_field, ticket_key):
    host = test.mock_host({"state": state, "status": status})
    startrek_client = mock_startrek_client(mp, "closed")

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.CONFLICT

    startrek_client.get_issue.assert_called_once_with(ticket_key)
    test.hosts.assert_equal()


def test_missing_host(test, ticket_key):
    result = test.api_client.post("/v1/hosts/1/set-maintenance", data={"ticket_key": ticket_key})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()

    result = test.api_client.post("/v1/hosts/missing/set-maintenance", data={"ticket_key": ticket_key})
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", FORBIDDEN_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_from_forbidden_states_is_not_allowed(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", FORBIDDEN_STATES)
@pytest.mark.parametrize("status", HostStatus.ALL_TASK)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_from_forbidden_states_with_task_is_not_allowed(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "task": mock_task()})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()


if FORBIDDEN_TARGET_OPERATION_STATES:

    @pytest.mark.parametrize("state", ALLOWED_STATES)
    @pytest.mark.parametrize("status", ALLOWED_STATUSES)
    @pytest.mark.parametrize("target_operation_state", FORBIDDEN_TARGET_OPERATION_STATES)
    @pytest.mark.parametrize("host_id_field", ["inv", "name"])
    def test_set_maintenance_to_forbidden_operation_state(
        test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
    ):

        host = test.mock_host({"state": state, "status": status})

        result = test.api_client.post(
            hosts_api_url(host, host_id_field, "/set-maintenance"),
            data={"ticket_key": ticket_key, "operation_state": target_operation_state},
        )

        assert result.status_code == http.client.BAD_REQUEST
        test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", HostStatus.ALL_TASK)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_from_task_is_allowed(
    test, state, status, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status, "task": mock_task()})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.OK

    mock_schedule_maintenance(host, ticket_key=ticket_key, operation_state=target_operation_state)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("timeout_status", set(HostStatus.ALL) - ALLOWED_TIMEOUT_STATUS)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_to_forbidden_statuses_is_not_allowed(
    test, state, status, target_operation_state, host_id_field, timeout_status, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={"timeout_status": timeout_status, "ticket_key": ticket_key, "operation_state": target_operation_state},
    )
    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


_NETWORK_DELAY_OFFSET = 5 * MINUTE_SECONDS


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize(
    "timeout", [0, MIN_STATE_TIMEOUT - _NETWORK_DELAY_OFFSET - 1, MAX_STATE_TIMEOUT + _NETWORK_DELAY_OFFSET + 1]
)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_maintenance_with_bad_timeout_is_not_allowed(
    test, state, status, timeout, target_operation_state, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host({"state": state, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/set-maintenance"),
        data={
            "timeout_time": timestamp() + timeout,
            "ticket_key": ticket_key,
            "operation_state": target_operation_state,
        },
    )
    assert result.status_code == http.client.BAD_REQUEST


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + HostStatus.ALL_TASK)
@pytest.mark.parametrize("timeout_status", ALLOWED_TIMEOUT_STATUS)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_changes_requested_target_status(
    test, status, timeout_status, host_id_field, startrek_client, ticket_key
):

    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key, timeout_status, issuer="previous-user@"),
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "timeout_status": timeout_status,
        },
    )

    assert result.status_code == http.client.OK

    host.state_expire = _mock_state_expire(ticket_key, timeout_status)
    test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + HostStatus.ALL_TASK)
@pytest.mark.parametrize("operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_changes_requested_operation_state(
    test, status, operation_state, host_id_field, startrek_client, ticket_key
):

    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key),
            "operation_state": operation_state,
        }
    )

    if operation_state == HostOperationState.OPERATION:
        target_operation_state = HostOperationState.DECOMMISSIONED
    else:
        target_operation_state = HostOperationState.OPERATION

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"), data={"operation_state": target_operation_state}
    )

    assert result.status_code == http.client.OK

    host.operation_state = target_operation_state
    test.hosts.assert_equal()


if FORBIDDEN_TARGET_OPERATION_STATES:
    # there are no forbidden operation states actually
    @pytest.mark.parametrize("status", HostStatus.ALL_STEADY + HostStatus.ALL_TASK)
    @pytest.mark.parametrize("operation_state", ALLOWED_TARGET_OPERATION_STATES)
    @pytest.mark.parametrize("target_operation_state", FORBIDDEN_TARGET_OPERATION_STATES)
    @pytest.mark.parametrize("host_id_field", ["inv", "name"])
    def test_change_maintenance_to_forbidden_operation_state(
        test, status, operation_state, target_operation_state, host_id_field, startrek_client, ticket_key
    ):

        host = test.mock_host(
            {
                "state": HostState.MAINTENANCE,
                "state_author": "previous-user@",
                "status": status,
                "status_author": "previous-user@",
                "ticket": ticket_key,
                "operation_state": operation_state,
            }
        )

        result = test.api_client.post(
            hosts_api_url(host, host_id_field, "/change-maintenance"), data={"operation_state": target_operation_state}
        )

        assert result.status_code == http.client.BAD_REQUEST

        test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + HostStatus.ALL_TASK)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_changes_requested_maintenance_timeout(
    test, status, host_id_field, startrek_client, ticket_key
):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key, timeout_time=timestamp(), issuer="previous-user@"),
        }
    )

    timeout_time = timestamp() + 45 * MINUTE_SECONDS
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "timeout_time": timeout_time,
        },
    )

    assert result.status_code == http.client.OK

    host.state_expire = _mock_state_expire(ticket_key, timeout_time=timeout_time)
    test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + HostStatus.ALL_TASK)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_changes_requested_ticket_key(test, status, host_id_field, startrek_client, ticket_key):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key, issuer="previous-user@"),
        }
    )

    new_ticket_key = "BURNE-0001"
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "ticket_key": new_ticket_key,
        },
    )

    assert result.status_code == http.client.OK
    startrek_client.get_issue.assert_called_once_with(new_ticket_key)

    # NB: host ticket does not change.
    host.state_expire = _mock_state_expire(new_ticket_key, issuer=test.api_issuer)
    test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + [HostStatus.ALL_TASK[0]])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_remove_timeout(test, status, host_id_field, ticket_key):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key, issuer="previous-user@"),
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "remove_timeout": True,
        },
    )

    assert result.status_code == http.client.OK

    host.state_expire = _mock_state_expire(ticket_key, issuer=test.api_issuer, timeout_time=False)
    test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + [HostStatus.ALL_TASK[0]])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_remove_timeout_and_set_ticket(test, status, host_id_field, startrek_client, ticket_key):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "state_expire": _mock_state_expire("old-ticket", issuer="previous-user@"),
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={"remove_timeout": True, "ticket_key": ticket_key},
    )

    assert result.status_code == http.client.OK
    startrek_client.get_issue.assert_called_once_with(ticket_key)

    host.state_expire = _mock_state_expire(ticket_key, timeout_time=False)
    host.ticket = ticket_key  # set ticket because there wan't any

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_STEADY + [HostStatus.ALL_TASK[0]])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_remove_and_set_timeout(test, status, host_id_field, ticket_key):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key),
        }
    )

    timeout_time = timestamp() + 45 * MINUTE_SECONDS
    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "remove_timeout": True,
            "timeout_time": timeout_time,
        },
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert "Cannot set and remove timeout at the same time." in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", set(HostState.ALL) - {HostState.MAINTENANCE})
@pytest.mark.parametrize("status", HostStatus.ALL)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_change_maintenance_forbidden_for_non_maintenance_hosts(test, state, status, host_id_field, ticket_key):
    host = test.mock_host(
        {
            "state": state,
            "state_author": "previous-user@",
            "status": status,
            "status_author": "previous-user@",
            "ticket": ticket_key,
            "state_expire": _mock_state_expire(ticket_key),
        }
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/change-maintenance"),
        data={
            "ticket_key": "BURNE-0001",
            "timeout_status": HostStatus.READY,
            "timeout_time": timestamp(),
        },
    )

    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()
