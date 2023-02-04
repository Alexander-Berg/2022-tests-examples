import pytest
import http.client

import walle
from infra.walle.server.tests.lib.util import (
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_check_host_dns,
    TestCase,
)
from walle.constants import DNS_VLAN_SCHEMES
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none

SUPPORTED_HOST_NAME = "mock.non-existent.some-project.fake.yandex.net"
UNSUPPORTED_HOST_NAME = "mock.test.google.com"
UNSUPPORTED_VLAN_SCHEME = "mock-vlan-scheme"


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


@pytest.fixture(params=DNS_VLAN_SCHEMES)
def project(test, request):
    return test.mock_project(
        {
            "id": "search-project-mock",
            "name": "search project mock",
            "vlan_scheme": request.param,
            "dns_domain": "fake.yandex.net",
        }
    )


generate_host_action_authentication_tests(globals(), "/reboot", {})


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.all_status_owner_combinations()
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing(test, project, host_id_field, status, owner):
    host = test.mock_host(
        {
            "name": SUPPORTED_HOST_NAME,
            "project": project.id,
            "state": HostState.ASSIGNED,
            "status": status,
            "status_author": owner,
        }
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_check_host_dns(host)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("check", [None, True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_monitoring(test, project, host_id_field, check):
    host = test.mock_host(
        {"name": SUPPORTED_HOST_NAME, "project": project.id, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data=drop_none({"check": check}))
    assert result.status_code == http.client.OK

    mock_schedule_check_host_dns(host, check=check is not False)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("with_auto_healing", [None, True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_auto_healing(test, project, host_id_field, with_auto_healing):
    host = test.mock_host(
        {"name": SUPPORTED_HOST_NAME, "project": project.id, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/check-dns"), data=drop_none({"with_auto_healing": with_auto_healing})
    )
    assert result.status_code == http.client.OK

    mock_schedule_check_host_dns(host, with_auto_healing=with_auto_healing)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("disable_admin_requests", [None, True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_disable_admin_requests(test, project, host_id_field, disable_admin_requests):
    host = test.mock_host(
        {"name": SUPPORTED_HOST_NAME, "project": project.id, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/check-dns"),
        data=drop_none({"disable_admin_requests": disable_admin_requests}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_check_host_dns(host, disable_admin_requests=disable_admin_requests)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_status(test, project, host_id_field, status):
    host = test.mock_host(
        {"name": SUPPORTED_HOST_NAME, "project": project.id, "state": HostState.ASSIGNED, "status": status}
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_maintenance(test, project, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test, {"name": SUPPORTED_HOST_NAME, "project": project.id})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data={})
    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_vlan_scheme(test, host_id_field):
    try:
        walle.constants.VLAN_SCHEMES.append(UNSUPPORTED_VLAN_SCHEME)
        project = test.mock_project(
            {"id": "non-search-project-mock", "name": "search project mock", "vlan_scheme": UNSUPPORTED_VLAN_SCHEME}
        )
    finally:
        walle.constants.VLAN_SCHEMES.remove(UNSUPPORTED_VLAN_SCHEME)

    host = test.mock_host(
        {"name": SUPPORTED_HOST_NAME, "project": project.id, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data={})
    assert result.status_code == http.client.CONFLICT
    assert (
        "Host name auto-assigning is supported only for"
        " static, search, mtn, mtn-hostid, cloud and mtn-without-fastbone VLAN schemes at this moment."
    ) in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_not_supported_domain(test, project, host_id_field):
    host = test.mock_host(
        {"project": project.id, "name": UNSUPPORTED_HOST_NAME, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/check-dns"), data={})
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == (
        "Host name auto-assigning is only allowed for hosts in "
        "'fake.yandex.net' domain for project 'search-project-mock'."
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_ignore_maintenance(test, project, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test, {"name": SUPPORTED_HOST_NAME, "project": project.id})

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/check-dns"), query_string="ignore_maintenance=true", data={}
    )
    assert result.status_code == http.client.OK

    mock_schedule_check_host_dns(host)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()
