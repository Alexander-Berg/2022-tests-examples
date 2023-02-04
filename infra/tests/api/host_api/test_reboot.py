"""Tests enqueuing host reboot task."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_host_reboot,
    monkeypatch_hw_client_for_host,
    monkeypatch_config,
)
from tests.api.common import toggle_reboot_via_ssh
from walle import restrictions
from walle.constants import SshOperation, HostType
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/reboot", {})


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.all_status_owner_combinations()
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing(monkeypatch, test, host_id_field, status, owner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    expected_notify_fsm_calls = []
    hw_client = monkeypatch_hw_client_for_host(monkeypatch, host)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/reboot"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_host_reboot(host, manual=True, expected_notify_fsm_calls=expected_notify_fsm_calls)
    assert result.json == host.to_api_obj()

    assert hw_client.method_calls == []
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("check", (None, True, False))
def test_enqueueing_with_check_parameter(test, check):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data=drop_none({"check": check}))
    assert result.status_code == http.client.OK

    mock_schedule_host_reboot(host, manual=True, check=check is not False)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("with_auto_healing", (None, True, False))
def test_enqueueing_with_auto_healing_parameter(test, with_auto_healing):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, action="/reboot"), data=drop_none({"with_auto_healing": with_auto_healing})
    )
    assert result.status_code == http.client.OK

    if with_auto_healing is True:
        override = True
    else:
        override = False

    mock_schedule_host_reboot(host, manual=True, with_auto_healing=with_auto_healing, check_post_code_override=override)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("disable_admin_requests", (None, True, False))
def test_enqueueing_with_disable_admin_requests_parameter(test, disable_admin_requests):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, action="/reboot"), data=drop_none({"disable_admin_requests": disable_admin_requests})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_reboot(host, manual=True, disable_admin_requests=disable_admin_requests)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept", "monkeypatch_locks")
@pytest.mark.parametrize("ssh", (SshOperation.ALL + [None]))
def test_enqueueing_with_ssh_parameter(test, ssh):
    toggle_reboot_via_ssh(test.default_project, enable=True)
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data=drop_none({"ssh": ssh}))
    assert result.status_code == http.client.OK

    mock_schedule_host_reboot(host, manual=True, ssh=ssh)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("ssh", (SshOperation.ONLY, SshOperation.FALLBACK))
def test_disallows_ssh_reboot_if_not_enabled_in_project(test, ssh):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data=drop_none({"ssh": ssh}))
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == "SSH rebooting is disabled in project"

    test.hosts.assert_equal()


@pytest.mark.parametrize("ssh", SshOperation.ALL)
def test_without_ipmi(test, mp, ssh):
    toggle_reboot_via_ssh(test.default_project, enable=True)
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "type": HostType.MAC})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data=drop_none({"ssh": ssh}))
    if ssh != SshOperation.FORBID:
        assert result.status_code == http.client.BAD_REQUEST
    else:
        assert result.status_code == http.client.OK
        mock_schedule_host_reboot(host, manual=True, ssh=ssh)
        assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_reject")
def test_ignore_cms(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data=drop_none({"ignore_cms": True}))
    assert result.status_code == http.client.OK

    mock_schedule_host_reboot(host, manual=True, ignore_cms=True)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_status(monkeypatch, test, host_id_field, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    hw_client = monkeypatch_hw_client_for_host(monkeypatch, host)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/reboot"), data={})
    assert result.status_code == http.client.CONFLICT

    assert hw_client.method_calls == []
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_maintenance(monkeypatch, test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)

    hw_client = monkeypatch_hw_client_for_host(monkeypatch, host)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/reboot"), data={})

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )
    assert hw_client.method_calls == []

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_allow_by_ignore_maintenance(monkeypatch, test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)
    monkeypatch_hw_client_for_host(monkeypatch, host)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/reboot"), query_string="ignore_maintenance=true", data={}
    )

    mock_schedule_host_reboot(host, manual=True)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_reject_by_restriction(test):
    host = test.mock_host(
        {"state": HostState.ASSIGNED, "status": HostStatus.READY, "restrictions": [restrictions.REBOOT]}
    )

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_reject")
def test_reject_by_cms(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/reboot"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()
