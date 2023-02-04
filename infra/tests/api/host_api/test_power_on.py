"""Tests enqueuing host powering on task."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_host_power_on,
)
from walle import restrictions
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/power-on", {})


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.all_status_owner_combinations()
def test_enqueueing(monkeypatch, test, host_id_field, status, owner):
    expected_notify_fsm_calls = []
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/power-on"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, expected_notify_fsm_calls=expected_notify_fsm_calls)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("check", (None, True, False))
def test_enqueueing_with_check_parameter(test, check):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/power-on"), data=drop_none({"check": check}))
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, check=check is not False)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("with_auto_healing", (None, True, False))
def test_enqueueing_with_auto_healing_parameter(test, with_auto_healing):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, action="/power-on"), data=drop_none({"with_auto_healing": with_auto_healing})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, with_auto_healing=with_auto_healing)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("disable_admin_requests", (None, True, False))
def test_enqueueing_with_disable_admin_requests_parameter(test, disable_admin_requests):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, action="/power-on"), data=drop_none({"disable_admin_requests": disable_admin_requests})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, disable_admin_requests=disable_admin_requests)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_status(monkeypatch, test, host_id_field, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/power-on"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_maintenance(test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/power-on"), data={})

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_allow_by_ignore_maintenance(test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/power-on"), query_string="ignore_maintenance=true", data={}
    )

    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_reject_by_restriction(test):
    host = test.mock_host(
        {"state": HostState.ASSIGNED, "status": HostStatus.READY, "restrictions": [restrictions.REBOOT]}
    )

    result = test.api_client.post(hosts_api_url(host, action="/power-on"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("restriction", [restrictions.PROFILE, restrictions.AUTOMATED_PROFILE])
@pytest.mark.all_status_owner_combinations()
def test_power_on_without_post_check_on_supported_platform_without_healing(
    test, platform_name, restriction, status, owner
):
    host = test.mock_host(
        {
            "restrictions": [restriction],
            "platform": platform_name,
            "state": HostState.ASSIGNED,
            "status": status,
            "status_author": owner,
        }
    )

    result = test.api_client.post(hosts_api_url(host, action="/power-on"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, check_post_code_override=False)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize(
    "platform_name", ({"system": "T174-N40", "board": None}, {"system": None, "board": "MY70-EX0-Y3N"})
)
@pytest.mark.parametrize("restriction", [restrictions.PROFILE, restrictions.AUTOMATED_PROFILE])
@pytest.mark.all_status_owner_combinations()
def test_power_on_without_post_check_on_supported_platform_with_healing(
    test, platform_name, restriction, status, owner
):
    host = test.mock_host(
        {
            "restrictions": [restriction],
            "platform": platform_name,
            "state": HostState.ASSIGNED,
            "status": status,
            "status_author": owner,
        }
    )

    result = test.api_client.post(hosts_api_url(host, action="/power-on"), data={"with_auto_healing": True})
    assert result.status_code == http.client.OK

    mock_schedule_host_power_on(host, check_post_code_override=False, with_auto_healing=True)

    test.hosts.assert_equal()
