from unittest.mock import Mock, MagicMock

import pytest
import http.client

from walle.clients import idm, staff
from walle.idm import project_role_managers
from walle.util.misc import drop_none

ROLE = "user"
ROLE_ID = 33333
GROUP_ID = 55555
ROLE_FIXED_USER = "fixed-user"


@pytest.fixture
def role_mock(monkeypatch):
    class RoleManagerMock(project_role_managers.ProjectRoleManagerBase):
        role_name = ROLE
        storage_strategy = Mock()
        member_processing_strategy = Mock()
        audit_log_strategy = MagicMock()  # audit log entries act as context managers (__exit__ is required)

    monkeypatch.setitem(project_role_managers.ProjectRole._alias_to_manager_class, ROLE, RoleManagerMock)
    return RoleManagerMock


@pytest.fixture
def mock_batch_request(monkeypatch):
    batch = MagicMock()
    monkeypatch.setattr(idm, "BatchRequest", batch)
    return batch


class TestListRoleMembers:
    def test_bad_request_on_wrong_role(self, walle_test, role_mock):
        r = self._request(walle_test.api_client, "nonexistent_role")
        assert r.status_code == http.client.BAD_REQUEST

    def test_storage_strategy_is_used(self, walle_test, role_mock):
        members = ["user", "@group"]
        role_mock.storage_strategy().list_members.return_value = members

        r = self._request(walle_test.api_client, ROLE)
        assert r.status_code == http.client.OK
        assert r.json == {"members": members}

    @staticmethod
    def _request(client, role, project="mocked-default-project"):
        return client.get("/v1/projects/{}/role/{}/members".format(project, role))


class TestAddRoleMember:
    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    def test_bad_request_on_wrong_role(self, walle_test, role_mock, method):
        r = self._request(walle_test.api_client, method, "nonexistent_role", "user1")
        assert r.status_code == http.client.BAD_REQUEST

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    def test_empty_member_param_is_accepted(self, walle_test, mp, role_mock, mock_batch_request, method):
        role_mock.member_processing_strategy().process_member.side_effect = lambda m: ROLE_FIXED_USER
        role_mock.storage_strategy().list_members.return_value = []

        r = self._request(walle_test.api_client, method, "user", None)
        assert r.status_code == http.client.NO_CONTENT

    @pytest.mark.parametrize("method", ["POST", "PATCH"])
    @pytest.mark.parametrize("member", ["user", "@group"])
    def test_role_is_requested(self, walle_test, role_mock, mock_batch_request, method, member):
        role_mock.member_processing_strategy().process_member.side_effect = lambda m: m
        role_mock.member_processing_strategy().process_requester.side_effect = lambda m: m
        role_mock.storage_strategy().list_members.return_value = []

        r = self._request(walle_test.api_client, method, ROLE, member)
        assert r.status_code == http.client.NO_CONTENT
        assert role_mock.audit_log_strategy().on_request_add_member.called

        batch_expected_args = {
            'path': ['scopes', 'project', 'project', 'mocked-default-project', 'role', 'user'],
            '_requester': walle_test.api_user,
        }
        batch_expected_args["group" if staff.is_group(member) else "user"] = member
        mock_batch_request().__enter__().request_role.assert_called_once_with(**batch_expected_args)

    @staticmethod
    def _request(client, method, role, member, project="mocked-default-project"):
        return client.open(
            "/v1/projects/{}/role/{}/members".format(project, role), method=method, data=drop_none({"member": member})
        )


class TestRemoveRoleMember:
    @pytest.fixture()
    def mock_idm_get_role(self, mp):
        return mp.function(idm.get_role, return_value=Mock(id=ROLE_ID))

    def test_bad_request_on_wrong_role(self, walle_test, role_mock):
        r = self._request(walle_test.api_client, "nonexistent_role", "user1")
        assert r.status_code == http.client.BAD_REQUEST

    def test_empty_member_param_is_accepted(self, walle_test, mp, role_mock, mock_batch_request, mock_idm_get_role):
        role_mock.member_processing_strategy().process_member.side_effect = lambda m: ROLE_FIXED_USER
        role_mock.storage_strategy().list_members.return_value = [ROLE_FIXED_USER]

        r = self._request(walle_test.api_client, "user", None)
        assert r.status_code == http.client.NO_CONTENT

    @pytest.mark.parametrize("member", ["user", "@group"])
    def test_role_is_requested_to_revoke(
        self, walle_test, mp, role_mock, mock_batch_request, mock_idm_get_role, member
    ):
        role_mock.member_processing_strategy().process_member.side_effect = lambda m: m
        role_mock.storage_strategy().list_members.return_value = [member]
        mp.function(staff.get_group_id, return_value=GROUP_ID)

        r = self._request(walle_test.api_client, ROLE, member)
        assert r.status_code == http.client.NO_CONTENT

        assert role_mock.audit_log_strategy().on_request_remove_member.called
        mock_batch_request().__enter__().revoke_role.assert_called_once_with(ROLE_ID)

    @staticmethod
    def _request(client, role, member, project="mocked-default-project"):
        return client.delete(
            "/v1/projects/{}/role/{}/members".format(project, role), data=drop_none({"member": member})
        )
