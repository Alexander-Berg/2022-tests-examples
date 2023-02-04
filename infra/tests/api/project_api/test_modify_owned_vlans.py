"""Tests project owned VLANs modification API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_function


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/owned_vlans", method=method, data={"vlans": []})
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
@pytest.mark.parametrize("vlans", [[1], [1, 2]])
def test_unauthorized(test, mp, method, vlans):
    from walle.views.api.project_api import common

    monkeypatch_function(mp, func=common.get_owned_vlans, module=common, return_value=[])

    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/owned_vlans", method=method, data={"vlans": vlans})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_modify_by_admin(test, authorized_admin):
    project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2, 3]})

    result = test.api_client.patch("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [3, 4, 4, 5]})
    assert result.status_code == http.client.OK
    project.owned_vlans.extend((4, 5))
    test.projects.assert_equal()

    result = test.api_client.post("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [5, 6]})
    assert result.status_code == http.client.OK
    project.owned_vlans.append(6)
    test.projects.assert_equal()

    result = test.api_client.delete("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [3, 4]})
    assert result.status_code == http.client.OK
    project.owned_vlans.remove(3)
    project.owned_vlans.remove(4)
    test.projects.assert_equal()

    result = test.api_client.delete("/v1/projects/" + project.id + "/owned_vlans")
    assert result.status_code == http.client.OK
    project.owned_vlans = []
    test.projects.assert_equal()

    result = test.api_client.put("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [7]})
    assert result.status_code == http.client.OK
    project.owned_vlans = [7]
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_modify_by_project_owners(test, mp):
    from walle.views.api.project_api import common

    monkeypatch_function(mp, func=common.get_owned_vlans, module=common, return_value=[1, 2, 3, 4, 5])
    project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2, 3, 7], "owners": [test.api_user]})

    result = test.api_client.patch("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [3, 4, 4, 5]})
    assert result.status_code == http.client.OK
    project.owned_vlans.extend((4, 5))
    test.projects.assert_equal()

    result = test.api_client.post("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [5, 6]})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()

    result = test.api_client.delete("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [3, 4]})
    assert result.status_code == http.client.OK
    project.owned_vlans.remove(3)
    project.owned_vlans.remove(4)
    test.projects.assert_equal()

    result = test.api_client.delete("/v1/projects/" + project.id + "/owned_vlans", data={"vlans": [7]})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()
