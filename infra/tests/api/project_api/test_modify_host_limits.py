"""Tests project host limits modification API."""

import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.projects import Project


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    request.addfinalizer(test.projects.assert_equal)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/host_limits", method=method, data={})
    assert result.status_code == httplib.UNAUTHORIZED


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open(
        "/v1/projects/" + project.id + "/host_limits", method=method, data={"max_healing_cancellations": []}
    )
    assert result.status_code == httplib.FORBIDDEN


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_set(test, method):
    new_limits = {
        "max_healing_cancellations": [{"period": "1s", "limit": 1}, {"period": "10m", "limit": 10}],
    }

    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/host_limits", method=method, data=new_limits)
    assert result.status_code == httplib.OK

    assert result.json == new_limits
    assert Project.objects(id=project.id).only("host_limits").get().host_limits == new_limits

    project.host_limits.update(new_limits)
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unset(test, method):
    new_limits = {
        "max_healing_cancellations": [],
    }
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/host_limits", method=method, data=new_limits)
    assert result.status_code == httplib.OK

    assert result.json == {}
    assert Project.objects(id=project.id).only("host_limits").get().host_limits == {}

    for limit_name in new_limits:
        del project.host_limits[limit_name]
    test.projects.assert_equal()
