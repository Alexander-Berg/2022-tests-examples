"""Tests project modify API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.constants import TESTING_ENV_NAME


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.fixture
def mocked_project(test):
    project = test.mock_project({"id": "orig-id", "name": "Some name"})
    return project


@pytest.yield_fixture()
def project_idm_push_called(enable_idm_push, project_idm_add_role_nodes_mock, project_idm_request_project_roles_mock):
    yield
    assert project_idm_add_role_nodes_mock.called
    assert project_idm_request_project_roles_mock


def call_clone_api(test, orig_id="orig-id", new_id="new-id", new_name="New name"):
    return test.api_client.open("/v1/projects/clone/" + orig_id, method="POST", data={"id": new_id, "name": new_name})


@pytest.mark.usefixtures("unauthenticated", "mocked_project")
def test_unauthenticated(test):
    result = call_clone_api(test)
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.usefixtures("unauthorized_project", "mocked_project")
def test_unauthorized(test):
    result = call_clone_api(test)
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.usefixtures("mocked_project", "project_idm_push_called")
def test_normal_clone(test):
    result = call_clone_api(test)
    assert result.status_code == http.client.CREATED

    test.mock_project({"id": "new-id", "name": "New name"}, save=False)
    test.projects.assert_equal()


@pytest.mark.usefixtures("mocked_project")
def test_original_doesnt_exist(test):
    result = call_clone_api(test, orig_id="nonexistent-id")
    assert result.status_code == http.client.NOT_FOUND
    test.projects.assert_equal()


@pytest.mark.usefixtures("mocked_project", "project_idm_add_role_nodes_mock", "project_idm_request_project_roles_mock")
def test_duplicated_name(test):
    call_clone_api(test, new_id="new-id")
    test.mock_project({"id": "new-id", "name": "New name"}, save=False)

    result = call_clone_api(test, new_id="new-id")
    assert result.status_code == http.client.CONFLICT

    test.projects.assert_equal()


@pytest.mark.usefixtures("mocked_project")
def test_clone_name_conflict(test):
    test.mock_project({"id": "new-id", "name": "New name"})

    result = call_clone_api(test, new_id="new-id")
    assert result.status_code == http.client.CONFLICT

    test.projects.assert_equal()


def test_clone_wrong_env_regular_user(test, mp):
    env_name = TESTING_ENV_NAME
    mp.config("environment.name", env_name)
    result = call_clone_api(test)
    assert result.status_code == http.client.FORBIDDEN
    assert result.json[
        "message"
    ] == "Authorization failure: This method is available only for Wall-E admins on {} environment.".format(env_name)

    test.projects.assert_equal()


@pytest.mark.usefixtures("mocked_project", "project_idm_push_called")
def test_clone_wrong_env_admin(test, mp):
    env_name = TESTING_ENV_NAME
    mp.config("authorization.admins", [test.api_user])
    mp.config("environment.name", env_name)
    result = call_clone_api(test)
    assert result.status_code == http.client.CREATED

    test.mock_project({"id": "new-id", "name": "New name"}, save=False)
    test.projects.assert_equal()
