"""Tests enqueuing host powering off task."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_host_power_off,
    mock_schedule_maintenance,
)
from sepelib.core import constants
from walle.hosts import HostState, HostOperationState, HostStatus, StateExpire
from walle.models import timestamp
from walle.util.misc import drop_none

generate_host_action_authentication_tests(globals(), "/power-off", {"ticket_key": "MOCK-1234"})

ALLOWED_TARGET_OPERATION_STATES = {HostOperationState.OPERATION, HostOperationState.DECOMMISSIONED}
FORBIDDEN_TARGET_OPERATION_STATES = set(HostOperationState.ALL) - ALLOWED_TARGET_OPERATION_STATES


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.all_status_owner_combinations(include_manual=False)
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing(test, mp, startrek_client, target_operation_state, host_id_field, status, owner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})
    ticket_key = "WALLE-2818"

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/power-off"),
        data={
            "operation_state": target_operation_state,
            "ticket_key": ticket_key,
        },
    )

    mock_schedule_maintenance(host, power_off=True, operation_state=target_operation_state, ticket_key=ticket_key)

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("disable_admin_requests", (None, True, False))
@pytest.mark.parametrize("ignore_cms", (None, True, False))
@pytest.mark.parametrize("ignore_maintenance", (None, True, False))
@pytest.mark.parametrize("target_operation_state", ALLOWED_TARGET_OPERATION_STATES)
def test_enqueueing_with_additional_task_params(
    test, startrek_client, disable_admin_requests, ignore_cms, ignore_maintenance, target_operation_state
):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})
    ticket_key = "WALLE-2818"

    result = test.api_client.post(
        hosts_api_url(host, action="/power-off"),
        data=drop_none(
            {
                "disable_admin_requests": disable_admin_requests,
                "ignore_cms": ignore_cms,
                "operation_state": target_operation_state,
                "ticket_key": ticket_key,
            }
        ),
        query_string=drop_none({"ignore_maintenance": ignore_maintenance}),
    )

    mock_schedule_maintenance(
        host,
        power_off=True,
        disable_admin_requests=disable_admin_requests,
        ignore_cms=ignore_cms,
        operation_state=target_operation_state,
        ticket_key=ticket_key,
    )

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_maintenance_reject(test, operation_state, host_id_field, mock_maintenance_host):
    host = mock_maintenance_host(test, {"operation_state": operation_state})

    if operation_state == HostOperationState.OPERATION:
        target_operation_state = HostOperationState.DECOMMISSIONED
    else:
        target_operation_state = HostOperationState.OPERATION

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/power-off"),
        data={"operation_state": target_operation_state, "ticket_key": "MOCK-1234"},
    )

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("operation_state", ALLOWED_TARGET_OPERATION_STATES)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_updating_operation_state(test, operation_state, host_id_field, mock_maintenance_host, startrek_client):
    ticket_key = "MOCK-1234"
    host = mock_maintenance_host(test, {"operation_state": operation_state, "ticket": ticket_key})

    if operation_state == HostOperationState.OPERATION:
        target_operation_state = HostOperationState.DECOMMISSIONED
    else:
        target_operation_state = HostOperationState.OPERATION

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/power-off"),
        query_string={"ignore_maintenance": True},
        data={
            "ticket_key": ticket_key,
            "operation_state": target_operation_state,
        },
    )

    mock_schedule_host_power_off(host)
    host.operation_state = target_operation_state
    host.state_expire = StateExpire(ticket=ticket_key, status=host.state_expire.status, issuer=test.api_issuer)

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("old_ticket_key", [None, "MOCK-4321", "MOCK-1234"])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_power_off_keeps_host_ticket_overwrites_state_expire(
    test, old_ticket_key, host_id_field, mock_maintenance_host, startrek_client
):
    host = mock_maintenance_host(test, {"ticket": old_ticket_key})
    ticket_key = "MOCK-1234"

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/power-off"),
        query_string={"ignore_maintenance": True},
        data={"ticket_key": ticket_key},
    )

    mock_schedule_host_power_off(host)
    host.ticket = host.ticket or ticket_key
    host.state_expire = StateExpire(ticket=ticket_key, status=host.state_expire.status, issuer=test.api_issuer)

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


def test_power_off_maintenance_ticket(test, startrek_client):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})
    ticket_key = "MOCK-1234"

    result = test.api_client.post(hosts_api_url(host, action="/power-off"), data={"ticket_key": ticket_key})

    mock_schedule_maintenance(host, power_off=True, ticket_key=ticket_key)

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


def test_power_off_maintenance_invalid_ticket(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})
    ticket_key = "1234-MOCK-1234"

    result = test.api_client.post(hosts_api_url(host, action="/power-off"), data={"ticket_key": ticket_key})

    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


def test_power_off_maintenance_timeout_no_ticket(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})
    timeout_time = timestamp() + constants.DAY_SECONDS

    result = test.api_client.post(hosts_api_url(host, action="/power-off"), data={"timeout_time": timeout_time})

    assert result.status_code == http.client.BAD_REQUEST
    assert "data must contain ['ticket_key'] properties" in result.json["message"]


@pytest.mark.parametrize("timedelta", (-constants.DAY_SECONDS, 0, constants.MINUTE_SECONDS))
def test_power_off_invalid_timeout(test, timedelta):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})
    timeout_time = timestamp() + timedelta

    result = test.api_client.post(hosts_api_url(host, action="/power-off"), data={"timeout_time": timeout_time})

    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


def test_power_off_without_params(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/power-off"))

    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()
