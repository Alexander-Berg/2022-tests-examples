from unittest.mock import Mock

import pytest
import http.client

from tests.api.common import toggle_reboot_via_ssh
from walle.clients import idm
from walle.constants import ROBOT_WALLE_OWNER
from walle.idm.project_role_managers import get_project_idm_role_prefix


@pytest.fixture()
def request_role_mock(mp, batch_request_execute_mock):
    return mp.method(idm.BatchRequest.request_role, obj=idm.BatchRequest)


@pytest.fixture()
def revoke_role_mock(mp, batch_request_execute_mock):
    return mp.method(idm.BatchRequest.revoke_role, obj=idm.BatchRequest)


def mock_project(walle_test, reboot_via_ssh, **kwargs):
    project = walle_test.mock_project(dict(id="test", **kwargs))
    toggle_reboot_via_ssh(project, enable=reboot_via_ssh)
    return project


class TestToggleSshReboot:
    def test_requests_role_if_ssh_was_disabled(self, walle_test, request_role_mock):
        project = mock_project(walle_test, reboot_via_ssh=False)
        result = walle_test.api_client.put("/v1/projects/" + project.id + "/rebooting_via_ssh", data={})
        assert result.status_code == http.client.NO_CONTENT
        assert request_role_mock.call_count == 1
        assert request_role_mock.call_args[1] == {
            "path": get_project_idm_role_prefix(project.id) + ["role", "ssh_rebooter"],
            "_requester": ROBOT_WALLE_OWNER,
            "user": ROBOT_WALLE_OWNER,
        }
        walle_test.projects.assert_equal()

    @pytest.mark.usefixtures("monkeypatch_locks")
    def test_doesnt_request_role_if_ssh_was_enabled(self, walle_test, request_role_mock):
        project = mock_project(walle_test, reboot_via_ssh=True)
        result = walle_test.api_client.put("/v1/projects/" + project.id + "/rebooting_via_ssh", data={})
        assert result.status_code == http.client.NO_CONTENT
        assert not request_role_mock.called
        walle_test.projects.assert_equal()

    @pytest.mark.usefixtures("monkeypatch_locks")
    def test_revokes_role_if_ssh_was_enabled(self, walle_test, mp, revoke_role_mock):
        project = mock_project(walle_test, reboot_via_ssh=True)

        role_id = 777
        mp.function(idm.get_role, return_value=Mock(id=role_id))
        result = walle_test.api_client.delete("/v1/projects/" + project.id + "/rebooting_via_ssh", data={})
        assert result.status_code == http.client.NO_CONTENT
        assert revoke_role_mock.call_args[0][1] == role_id
        walle_test.projects.assert_equal()

    def test_doesnt_revoke_role_if_ssh_was_disabled(self, walle_test, revoke_role_mock):
        project = mock_project(walle_test, reboot_via_ssh=False)
        result = walle_test.api_client.delete("/v1/projects/" + project.id + "/rebooting_via_ssh", data={})
        assert result.status_code == http.client.NO_CONTENT
        assert not revoke_role_mock.called
        walle_test.projects.assert_equal()
