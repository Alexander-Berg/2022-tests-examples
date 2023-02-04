"""Tests project modification API: set/change/remove host_shortname_template."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none


@pytest.fixture
def test(request, monkeypatch_production_env):
    return TestCase.create(request)


def _url(project_id):
    return "/v1/projects/{}/host_shortname_template".format(project_id)


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        _url(project.id), method=method, data={"host_shortname_template": "{location}-{index}"}
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("unauthorized_project")
def test_unauthorized(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        _url(project.id), method=method, data={"host_shortname_template": "{location}-{index}"}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("previous_template", [None, "{index}"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_set_new_template(test, method, reason, previous_template):
    project = test.mock_project(
        drop_none(
            {
                "id": "some-id",
                "name": "Some name",
                "dns_domain": "search.yandex.net",
                "host_shortname_template": previous_template,
            }
        )
    )

    new_template = "{location}-{index}"
    result = test.api_client.open(
        _url(project.id), method=method, data=drop_none({"host_shortname_template": new_template, "reason": reason})
    )
    assert result.status_code == http.client.NO_CONTENT

    project.host_shortname_template = new_template
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_new_template_validates_new_template(test, method):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "dns_domain": "search.yandex.net"}))

    result = test.api_client.open(
        _url(project.id), method=method, data=drop_none({"host_shortname_template": "not-a-good-template"})
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == (
        "Request validation error: '{index}' is a required placeholder in host shortname template."
    )

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_new_template_only_allowed_if_dns_domain_set(test, method):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name"}))

    result = test.api_client.open(
        _url(project.id), method=method, data=drop_none({"host_shortname_template": "{index}"})
    )

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json['message'] == "Can not use custom host shortname template in project without dns settings."

    test.projects.assert_equal()


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("previous_template", [None, "{index}"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_unset_dns_domain(test, reason, previous_template):
    project = test.mock_project(
        drop_none(
            {
                "id": "some-id",
                "name": "Some name",
                "dns_domain": "search.yandex.net",
                "host_shortname_template": previous_template,
            }
        )
    )

    result = test.api_client.open(_url(project.id), method="DELETE", data=drop_none({"reason": reason}))
    assert result.status_code == http.client.NO_CONTENT

    del project.host_shortname_template
    test.projects.assert_equal()
