"""Tests extra VLANs modification API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, hosts_api_url, generate_host_action_authentication_tests
from walle.hosts import HostState, HostStatus


@pytest.fixture
def test(monkeypatch_timestamp, request):
    return TestCase.create(request)


generate_host_action_authentication_tests(
    globals(), "/extra_vlans", {"vlans": []}, methods=("PUT", "PATCH", "POST", "DELETE")
)


def test_modify(test):
    project = test.mock_project({"id": "some-project", "owned_vlans": [1, 2, 3]})
    host = test.mock_host({"project": project.id, "state": HostState.ASSIGNED})
    assert host.extra_vlans is None

    host.extra_vlans = [1, 2]
    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": [2, 1, 2]})
    assert result.status_code == http.client.OK
    assert result.json == {"extra_vlans": host.extra_vlans}
    test.hosts.assert_equal()

    del host.extra_vlans
    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": []})
    assert result.status_code == http.client.OK
    assert result.json == {"extra_vlans": []}
    test.hosts.assert_equal()

    host.extra_vlans = [2, 3]
    result = test.api_client.post(hosts_api_url(host, action="/extra_vlans"), data={"vlans": [2, 3, 2, 3]})
    assert result.status_code == http.client.OK
    assert result.json == {"extra_vlans": host.extra_vlans}
    test.hosts.assert_equal()

    host.extra_vlans.remove(2)
    result = test.api_client.delete(hosts_api_url(host, action="/extra_vlans"), data={"vlans": [2]})
    assert result.status_code == http.client.OK
    assert result.json == {"extra_vlans": host.extra_vlans}
    test.hosts.assert_equal()

    del host.extra_vlans
    result = test.api_client.delete(hosts_api_url(host, action="/extra_vlans"))
    assert result.status_code == http.client.OK
    assert result.json == {"extra_vlans": []}
    test.hosts.assert_equal()


@pytest.mark.all_status_owner_combinations()
def test_allow_by_status(test, status, owner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": []})
    assert result.status_code == http.client.OK

    assert result.json == {"extra_vlans": []}
    del host.extra_vlans

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
def test_reject_by_status(test, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": []})
    assert result.status_code == http.client.CONFLICT
    assert "The host has an invalid state for this operation" in result.json["message"]

    test.hosts.assert_equal()


def test_reject_by_maintenance(test, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": []})
    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


def test_allow_by_ignore_maintenance(test, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.put(
        hosts_api_url(host, action="/extra_vlans"), query_string="ignore_maintenance=true", data={"vlans": []}
    )
    assert result.status_code == http.client.OK

    assert result.json == {"extra_vlans": []}
    del host.extra_vlans

    test.hosts.assert_equal()


def test_reject_by_project_vlans(test):
    host = test.mock_host({"state": HostState.ASSIGNED})

    result = test.api_client.put(hosts_api_url(host, action="/extra_vlans"), data={"vlans": [1]})
    assert result.status_code == http.client.FORBIDDEN
    assert "doesn't own the following VLAN" in result.json["message"]

    test.hosts.assert_equal()
