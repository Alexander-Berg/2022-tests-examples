"""Tests project modification API: set/change/remove bot_project_id."""

import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase, drop_none

_DNS_DOMAIN_SEARCH = "search.yandex.net"
_DNS_DOMAIN_QLOUD = "qloud-h.yandex.net"


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id), method=method, data={"dns_domain": _DNS_DOMAIN_SEARCH}
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized_admin(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id), method=method, data={"dns_domain": _DNS_DOMAIN_SEARCH}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("dns_domain", [None, _DNS_DOMAIN_QLOUD])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_dns_domain(test, method, reason, dns_domain):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "dns_domain": dns_domain}))
    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id),
        method=method,
        data=drop_none({"dns_domain": _DNS_DOMAIN_SEARCH, "reason": reason}),
    )
    assert result.status_code == http.client.NO_CONTENT

    project.dns_domain = _DNS_DOMAIN_SEARCH
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_yc_dns_zone(test, mp):
    test_yc_dns_zone_id = "some-dns-zone-id"
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name"}))
    url = f"/v1/projects/{project.id}/dns_domain"
    result = test.api_client.post(url, data={"dns_domain": _DNS_DOMAIN_SEARCH, "yc_dns_zone_id": test_yc_dns_zone_id})
    assert result.status_code == http.client.NO_CONTENT
    assert project.reload().yc_dns_zone_id == test_yc_dns_zone_id

    result = test.api_client.post(url, data={"dns_domain": _DNS_DOMAIN_SEARCH, "yc_dns_zone_id": ""})
    assert result.status_code == http.client.NO_CONTENT
    assert not project.reload().yc_dns_zone_id


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("dns_domain", [None, _DNS_DOMAIN_QLOUD])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_unset_dns_domain(test, reason, dns_domain):
    project = test.mock_project({"id": "some-id", "name": "Some name", "dns_domain": dns_domain})

    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id), method="DELETE", data=drop_none({"reason": reason})
    )
    assert result.status_code == http.client.NO_CONTENT

    del project.dns_domain
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin", "mock_certificator_allowed_domain_list")
def test_unset_dns_domain_w_cert_deploy_enabled(test):
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "dns_domain": "search.yandex.net", "certificate_deploy": True}
    )

    result = test.api_client.open("/v1/projects/{}/dns_domain".format(project.id), method="DELETE")
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == "Certificate deploy depends on DNS domain, please disable it first."

    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin", "mock_certificator_allowed_domain_list")
def test_unset_dns_domain_w_custom_hostname_template(test):
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "dns_domain": "search.yandex.net", "host_shortname_template": "{index}"}
    )

    result = test.api_client.open("/v1/projects/{}/dns_domain".format(project.id), method="DELETE")
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == "Host shortname template is set for the project, please clean it up first."

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
@pytest.mark.parametrize("new_dns_domain", ["oblabla", "yp@yandex-team.net", "@1111andherestartsnormal.domain"])
def test_set_invalid_dns_domain(test, method, new_dns_domain):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "dns_domain": "some.valid.domain"}))
    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id), method=method, data={"dns_domain": new_dns_domain}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'].startswith("Request validation error: ")

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("mock_certificator_allowed_domain_list")
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_dns_domain_not_in_wl_w_certificate_deploy_enabled(test, method):
    project = test.mock_project(
        {"id": "some-id", "name": "Some name", "dns_domain": "search.yandex.net", "certificate_deploy": True}
    )
    result = test.api_client.open(
        "/v1/projects/{}/dns_domain".format(project.id), method=method, data={"dns_domain": "domain.not.allowed"}
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == "DNS domain domain.not.allowed is not in certficator white list."

    test.projects.assert_equal()
