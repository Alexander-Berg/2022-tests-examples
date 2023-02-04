"""Tests project owners modification API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, set_project_owners
from walle.clients import staff
from walle.idm import project_role_managers
from walle.projects import get_project_owners


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/owners", method=method, data={"owners": ["invalid"]})
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/owners", method=method, data={"owners": ["invalid"]})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_set_invalid(test, method):
    project = test.mock_project({"id": "some-id", "owners": ["some-login"]})
    result = test.api_client.open(
        "/v1/projects/" + project.id + "/owners", method=method, data={"owners": ["some invalid login"]}
    )
    assert result.status_code == http.client.BAD_REQUEST
    test.projects.assert_equal()


@pytest.fixture()
def request_ownership_mock(mp, batch_request_execute_mock):
    return mp.method(project_role_managers.OwnerManager.request_add_member, obj=project_role_managers.OwnerManager)


@pytest.fixture()
def revoke_ownerships_mock(mp, batch_request_execute_mock):
    return mp.method(project_role_managers.OwnerManager.request_remove_member, obj=project_role_managers.OwnerManager)


def check_request_ownership(request_ownership_mock, members):
    assert request_ownership_mock.call_count == len(members)
    requested_members = {call_args[0][3] for call_args in request_ownership_mock.call_args_list}
    assert requested_members == members
    request_ownership_mock.reset_mock()


def check_revoke_ownerships(revoke_ownerships_mock, members):
    assert revoke_ownerships_mock.call_count == len(members)
    revoked_members = {args[0][2] for args in revoke_ownerships_mock.call_args_list}
    assert revoked_members == members
    revoke_ownerships_mock.reset_mock()


@pytest.mark.usefixtures("enable_idm_push")
class TestModify:
    @pytest.fixture(autouse=True)
    def validate_owners(self, mp):
        self.check_owners_mock = mp.function(staff.check_owners, side_effect=lambda logins: logins)

    @pytest.fixture()
    def project(self, test):
        owners = [test.api_user, "owner1", "owner2", "@group"]
        project = test.mock_project({"id": "some-id", "owners": owners})
        return project

    def test_can_replace_owners_with_empty_list(self, test, project, revoke_ownerships_mock):
        existing_owners = get_project_owners(project)
        result = test.api_client.put("/v1/projects/" + project.id + "/owners", data={"owners": []})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with([])
        check_revoke_ownerships(revoke_ownerships_mock, set(existing_owners))
        test.projects.assert_equal()

    def test_cannot_call_without_owners(self, test, project):
        result = test.api_client.delete("/v1/projects/" + project.id + "/owners")
        assert result.status_code == http.client.BAD_REQUEST
        test.projects.assert_equal()

    def test_deduplicates_passed_owners(self, test, project, request_ownership_mock):
        result = test.api_client.patch("/v1/projects/" + project.id + "/owners", data={"owners": ["owner3", "owner3"]})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["owner3", "owner3"])
        check_request_ownership(request_ownership_mock, {"owner3"})
        test.projects.assert_equal()

    def test_requests_ownership(self, test, project, request_ownership_mock):
        result = test.api_client.post("/v1/projects/" + project.id + "/owners", data={"owners": ["owner4"]})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["owner4"])
        check_request_ownership(request_ownership_mock, {"owner4"})
        test.projects.assert_equal()

    def test_revokes_ownership(self, test, project, revoke_ownerships_mock):
        result = test.api_client.delete("/v1/projects/" + project.id + "/owners", data={"owners": ["owner2", "owner4"]})
        assert result.status_code == http.client.OK
        assert not self.check_owners_mock.called
        check_revoke_ownerships(revoke_ownerships_mock, {"owner2"})
        test.projects.assert_equal()

    def test_ignores_already_present_owners_on_add(self, test, project, request_ownership_mock):
        result = test.api_client.post("/v1/projects/" + project.id + "/owners", data={"owners": ["owner2"]})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["owner2"])
        assert not request_ownership_mock.called
        test.projects.assert_equal()

    def test_ignores_non_present_owners_on_remove(self, test, project, revoke_ownerships_mock):
        result = test.api_client.delete("/v1/projects/" + project.id + "/owners", data={"owners": ["owner4"]})
        assert result.status_code == http.client.OK
        assert not self.check_owners_mock.called
        assert not revoke_ownerships_mock.called
        test.projects.assert_equal()

    def test_requests_ownership_for_groups(self, test, project, request_ownership_mock):
        result = test.api_client.post("/v1/projects/" + project.id + "/owners", data={"owners": ["@group2"]})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["@group2"])
        check_request_ownership(request_ownership_mock, {"@group2"})
        test.projects.assert_equal()

    def test_deduplicates_groups(self, test, project, request_ownership_mock):
        result = test.api_client.patch(
            "/v1/projects/" + project.id + "/owners", data={"owners": ["@group1", "@group1"]}
        )
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["@group1", "@group1"])
        check_request_ownership(request_ownership_mock, {"@group1"})
        test.projects.assert_equal()

    def test_revokes_ownership_for_groups(self, test, project, revoke_ownerships_mock):
        result = test.api_client.delete("/v1/projects/" + project.id + "/owners", data={"owners": ["@group"]})
        assert result.status_code == http.client.OK
        assert not self.check_owners_mock.called
        check_revoke_ownerships(revoke_ownerships_mock, {"@group"})
        test.projects.assert_equal()

    def test_sets_to_single_owner(self, test, project, request_ownership_mock, revoke_ownerships_mock):
        existing_owners = get_project_owners(project)
        result = test.api_client.put("/v1/projects/" + project.id + "/owners", data={"owners": ["single-owner"]})
        assert result.status_code == http.client.OK
        self.check_owners_mock.assert_called_once_with(["single-owner"])
        check_revoke_ownerships(revoke_ownerships_mock, set(existing_owners))
        check_request_ownership(request_ownership_mock, {"single-owner"})
        test.projects.assert_equal()

    def test_authorization(self, test, mp, project):
        set_project_owners(project, ["single-user"])
        mp.function(staff.get_user_groups, return_value={"@group1", "@group2"})
        result = test.api_client.put("/v1/projects/" + project.id + "/owners", data={"owners": ["single-owner"]})
        assert result.status_code == http.client.FORBIDDEN
        test.projects.assert_equal()
