"""Tests project deletion API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.constants import PRODUCTION_ENV_NAME, TESTING_ENV_NAME
from walle.idm import project_push
from walle.idm.role_storage import IDMRoleMembership


@pytest.fixture
def test(request, mp):
    mp.config("environment.name", PRODUCTION_ENV_NAME)
    test = TestCase.create(request)
    test.mock_projects()
    return test


def test_unauthenticated(test, unauthenticated):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


def test_unauthorized(test, unauthorized_project):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.fixture()
def project_idm_delete_nodes_mock(mp, batch_request_execute_mock):
    return mp.function(project_push.delete_project_role_tree_nodes)


@pytest.yield_fixture()
def project_idm_push_called(enable_idm_push, project_idm_delete_nodes_mock):
    yield
    assert project_idm_delete_nodes_mock.called


@pytest.mark.usefixtures("monkeypatch_locks", "project_idm_push_called")
def test_delete(test):
    project = test.mock_project({"id": "some-id"}, add=False)

    assert project.id in {o.path_components[3] for o in IDMRoleMembership.objects()}

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.NO_CONTENT

    assert project.id not in {o.path_components[3] for o in IDMRoleMembership.objects()}

    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "project_idm_push_called")
def test_delete_with_preorders(test):
    project = test.mock_project({"id": "some-id"})
    preorder = test.preorders.mock(
        {"id": 1, "owner": test.api_user, "project": project.id, "prepare": False, "processed": False}
    )

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "Unable to delete the project: it has 1 preorders that are in process."
    test.projects.assert_equal()
    test.preorders.assert_equal()

    preorder.processed = True
    preorder.save()

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.NO_CONTENT
    test.projects.remove(project)

    test.projects.assert_equal()
    test.preorders.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "project_idm_push_called")
def test_delete_with_hosts(test):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host({"project": project.id})

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "Unable to delete the project: it has 1 hosts."
    test.projects.assert_equal()
    test.hosts.assert_equal()

    host.delete()
    test.hosts.remove(host)

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.NO_CONTENT
    test.projects.remove(project)

    test.projects.assert_equal()
    test.hosts.assert_equal()


def test_delete_missing(test):
    result = test.api_client.delete("/v1/projects/some-id")
    assert result.status_code == http.client.NOT_FOUND
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "project_idm_push_called")
def test_delete_with_reason(test):
    project = test.mock_project({"id": "some-id"}, add=False)

    result = test.api_client.delete("/v1/projects/" + project.id, data={"reason": "some reason"})
    assert result.status_code == http.client.NO_CONTENT

    test.projects.assert_equal()


def test_delete_wrong_env_regular_user(test, mp):
    env_name = TESTING_ENV_NAME
    mp.config("environment.name", env_name)
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.FORBIDDEN
    assert result.json[
        "message"
    ] == "Authorization failure: This method is available only for Wall-E admins on {} environment.".format(env_name)

    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "project_idm_push_called")
def test_delete_wrong_env_admin(test, mp):
    env_name = TESTING_ENV_NAME
    mp.config("authorization.admins", [test.api_user])
    mp.config("environment.name", env_name)
    project = test.mock_project({"id": "some-id"}, add=False)

    result = test.api_client.delete("/v1/projects/" + project.id)
    assert result.status_code == http.client.NO_CONTENT

    test.projects.assert_equal()
