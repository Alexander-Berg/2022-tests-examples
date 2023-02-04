"""Tests host deletion API."""

from unittest.mock import call

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    hosts_api_url,
    monkeypatch_deploy_client_for_host,
    generate_host_action_authentication_tests,
    mock_schedule_host_deletion,
)
from walle import constants as walle_constants
from walle.hosts import HostState, HostStatus
from walle.network import BlockedHostName


@pytest.fixture
def test(request, monkeypatch_audit_log, monkeypatch_timestamp):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), methods="DELETE")

ALLOWED_STATES = set(HostState.ALL)
ALLOWED_STATUSES = set(HostStatus.ALL_STEADY) | {HostStatus.INVALID}


@pytest.mark.parametrize("provisioner", walle_constants.PROVISIONERS)
@pytest.mark.parametrize("state", ALLOWED_STATES - {HostState.MAINTENANCE})
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.all_status_owner_combinations(include_invalid=True)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_host(test, instant, provisioner, state, status, owner, host_id_field):
    test.mock_host({"inv": 0, "provisioner": provisioner, "state": state, "status": status})
    host = test.mock_host(
        {"inv": 1, "provisioner": provisioner, "state": state, "status": status, "status_author": owner},
        add=not instant,
    )
    test.mock_host({"inv": 2, "provisioner": provisioner, "state": state, "status": status})

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": instant})
    assert result.status_code == http.client.NO_CONTENT
    if instant:
        # delete host immediately
        test.hosts.assert_equal()
    else:
        # create a task.
        mock_schedule_host_deletion(host)


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_shadow_host(test, host_id_field):
    host = test.mock_host({"inv": 0, "type": walle_constants.HostType.SHADOW_SERVER})

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": False})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("ipmi_mac", ("aa:bb:cc:dd:ee:ff", None))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_host_maintenance(test, state, host_id_field, mock_maintenance_host, instant, ipmi_mac):
    provisioner = walle_constants.PROVISIONER_LUI

    mock_maintenance_host(test, {"provisioner": provisioner})
    host = mock_maintenance_host(test, {"provisioner": provisioner, "ipmi_mac": ipmi_mac})
    mock_maintenance_host(test, {"provisioner": provisioner})

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": instant})

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", ALLOWED_STATUSES)
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("ipmi_mac", ("aa:bb:cc:dd:ee:ff", None))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_host_maintenance_by_issuer(test, status, host_id_field, instant, ipmi_mac):
    provisioner = walle_constants.PROVISIONER_LUI

    host = test.mock_host(
        {
            "provisioner": provisioner,
            "state_author": test.api_issuer,
            "status_author": "other-user@",
            "state": HostState.MAINTENANCE,
            "status": status,
            "ipmi_mac": ipmi_mac,
        },
        add=not instant,
    )

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": instant})

    assert result.status_code == http.client.NO_CONTENT

    if not instant:
        mock_schedule_host_deletion(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("state", ALLOWED_STATES)
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("ipmi_mac", ("aa:bb:cc:dd:ee:ff", None))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_host_ignore_maintenance(test, state, instant, host_id_field, next_inv, mock_maintenance_host, ipmi_mac):
    provisioner = walle_constants.PROVISIONER_LUI

    mock_maintenance_host(test, {"provisioner": provisioner})
    host = mock_maintenance_host(test, {"provisioner": provisioner, "ipmi_mac": ipmi_mac}, add=not instant)
    mock_maintenance_host(test, {"provisioner": provisioner})

    result = test.api_client.delete(
        hosts_api_url(host, host_id_field), query_string={"ignore_maintenance": True, "instant": instant}
    )

    assert result.status_code == http.client.NO_CONTENT
    if instant:
        # delete host immediately
        test.hosts.assert_equal()
    else:
        # create a task.
        mock_schedule_host_deletion(host)


def test_delete_missing_host(test):
    test.mock_host({"inv": 0})

    result = test.api_client.delete("/v1/hosts/1")
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()

    result = test.api_client.delete("/v1/hosts/missing")
    assert result.status_code == http.client.NOT_FOUND
    test.hosts.assert_equal()


@pytest.mark.parametrize("lui", (None, True, False, 1, 0))
@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_with_lui(mp, test, instant, host_id_field, lui):
    test.mock_host({"inv": 0})
    host = test.mock_host({"inv": 1, "state": HostState.ASSIGNED}, add=not instant)
    client = monkeypatch_deploy_client_for_host(mp, host)

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"lui": lui, "instant": instant})

    assert result.status_code == http.client.NO_CONTENT

    if instant:
        # delete host immediately
        test.hosts.assert_equal()
        assert client.mock_calls == ([call.remove(host.name)] if lui else [])
    else:
        # create a task.
        mock_schedule_host_deletion(host, lui=lui)


@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_free_host(mp, test, instant, host_id_field):
    test.mock_host({"inv": 0})
    host = test.mock_host({"inv": 1, "state": HostState.FREE}, add=False)
    monkeypatch_deploy_client_for_host(mp, host)

    result = test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": instant})

    assert result.status_code == http.client.NO_CONTENT

    # state=FREE always mean delete host immediately
    test.hosts.assert_equal()


@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_block_host_name_on_instant_delete(mp, test, instant, state, host_id_field):
    mock_block_hostname = mp.method(BlockedHostName.store, obj=BlockedHostName)

    test.mock_host({"inv": 0})
    host = test.mock_host({"inv": 1, "state": state})
    monkeypatch_deploy_client_for_host(mp, host)

    test.api_client.delete(hosts_api_url(host, host_id_field), query_string={"instant": instant})

    if instant or state == HostState.FREE:
        mock_block_hostname.assert_called_once_with(BlockedHostName, host.name)
    else:
        assert not mock_block_hostname.called


@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("ignore_cms", [True, False, None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_with_ignore_cms(test, instant, ignore_cms, host_id_field):
    test.mock_host({"inv": 0})
    host = test.mock_host({"inv": 1, "state": HostState.ASSIGNED}, add=not instant)

    result = test.api_client.delete(
        hosts_api_url(host, host_id_field),
        data={"reason": "some reason"},
        query_string={"instant": instant, "ignore_cms": ignore_cms},
    )
    assert result.status_code == http.client.NO_CONTENT

    if instant:
        # delete host immediately.
        test.hosts.assert_equal()
    else:
        # create a task.
        mock_schedule_host_deletion(host, ignore_cms=ignore_cms)


@pytest.mark.parametrize("instant", [True, False, None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_with_reason(test, instant, host_id_field):
    test.mock_host({"inv": 0})
    host = test.mock_host({"inv": 1, "state": HostState.ASSIGNED}, add=not instant)

    result = test.api_client.delete(
        hosts_api_url(host, host_id_field), data={"reason": "some reason"}, query_string={"instant": instant}
    )
    assert result.status_code == http.client.NO_CONTENT

    if instant:
        # delete host immediately
        test.hosts.assert_equal()
    else:
        # create a task.
        mock_schedule_host_deletion(host)


@pytest.mark.parametrize(
    "state,status",
    [(state, status) for state in set(HostState.ALL) - ALLOWED_STATES for status in HostStatus.ALL]
    + [(state, status) for state in ALLOWED_STATES for status in set(HostStatus.ALL) - ALLOWED_STATUSES],
)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_delete_host_in_forbidden_state(test, state, status, host_id_field):
    test.mock_host({"inv": 0, "state": state, "status": status})
    host = test.mock_host({"inv": 1, "state": state, "status": status})

    result = test.api_client.delete(hosts_api_url(host, host_id_field))
    assert result.status_code == http.client.CONFLICT
    test.hosts.assert_equal()
