import http.client
from unittest import mock

import pytest
import yc_as_client

from tests.api.common import project_settings
from tests.api.iam_permissions import mocks
from walle.clients import iam as iam_client


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_push_called",
)
def test_create_project(
    iam,
    walle_test,
    monkeypatch_abc_get_service_by_id,
    mock_iam_as_authorize,
    mock_iam_get_user_login,
    mock_yc_binded_automation_plot,
):
    automation_plot = walle_test.automation_plot.mock(mock_yc_binded_automation_plot)
    request_data = project_settings()
    request_data["yc_iam_folder_id"] = mocks.PROJECT_FOLDER_ID
    request_data["automation_plot_id"] = automation_plot.id
    response = walle_test.api_client.post("/v1/projects", data=request_data, headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.CREATED
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.update, mocks.PROJECT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
        (iam_client.ProjectsPermissions.create, iam_client.GIZMO_RESOURCE, iam_client.GIZMO_RESOURCE_TYPE),
        (iam_client.AutomationPlotsPermissions.get, mocks.AUTOMATION_PLOT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
    }
    assert mock_iam_get_user_login.mock_calls == [mock.call(mocks.MOCKED_IAM_TOKEN, mocks.MOCKED_USER_ID)]


@pytest.mark.parametrize(
    "exception",
    [
        yc_as_client.exceptions.UnauthenticatedException,
        yc_as_client.exceptions.PermissionDeniedException,
    ],
)
def test_no_access(iam, walle_test, exception, mock_iam_as_authenticate):
    def _raise(*args, **kwargs):
        mock_original_exception = mock.Mock()
        mock.details = mock.Mock(return_value={})
        raise exception(mock_original_exception)

    request_data = project_settings()
    request_data["yc_iam_folder_id"] = mocks.PROJECT_FOLDER_ID
    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authorize",
        return_value=_raise,
        spec=yc_as_client.YCAccessServiceClient.authorize,
    ):
        response = walle_test.api_client.post("/v1/projects", data=request_data, headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.FORBIDDEN


def test_no_token(iam, walle_test):
    request_data = project_settings()
    request_data["yc_iam_folder_id"] = mocks.PROJECT_FOLDER_ID
    response = walle_test.api_client.post("/v1/projects", data=request_data)
    assert response.status_code == http.client.FORBIDDEN


def test_no_folder(iam, walle_test):
    request_data = project_settings()
    response = walle_test.api_client.post("/v1/projects", data=request_data, headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.BAD_REQUEST


def test_get_projects(iam, walle_test, mock_iam_as_authenticate, mock_iam_get_user_login):
    walle_test.default_project.delete()  # NOTE(rocco66): no needs
    projects_count = 3
    for index in range(projects_count):
        walle_test.mock_project(
            {
                "id": f"orig-project-id-{index}",
                "yc_iam_folder_id": f"some-project-folder-id-{index}",
            }
        )
    response = walle_test.api_client.get("/v1/projects", headers=mocks.IAM_TOKEN_HEADERS)

    assert response.status_code == http.client.OK
    assert len(response.json["result"]) == projects_count
    for project in response.json["result"]:
        # NOTE(rocco66): 'name' is public field, 'yc_iam_folder_id' is not
        assert "name" in project
        assert "yc_iam_folder_id" not in project


@pytest.mark.usefixtures(
    "monkeypatch_bot_projects",
    "monkeypatch_check_deploy_conf",
    "monkeypatch_staff_get_user_groups",
    "project_idm_add_role_nodes_mock",
    "project_idm_request_project_roles_mock",
)
def test_check_iam_while_project_clone(
    iam,
    walle_test,
    monkeypatch_abc_get_service_by_id,
    mock_iam_as_authorize,
    mock_iam_get_user_login,
    mock_yc_binded_automation_plot,
):
    automation_plot = walle_test.automation_plot.mock(mock_yc_binded_automation_plot)
    orig_project = walle_test.mock_project(
        {
            "id": "orig-project-id",
            "yc_iam_folder_id": mocks.PROJECT_FOLDER_ID,
            "automation_plot_id": automation_plot.id,
        }
    )
    new_project_folder_id = "new-project-folder_id"
    request_data = {
        "id": "new-project-id",
        "name": "new_project_name",
        "yc_iam_folder_id": new_project_folder_id,
    }
    # TODO(rocco66): check owners
    response = walle_test.api_client.post(
        f"/v1/projects/clone/{orig_project.id}", data=request_data, headers=mocks.IAM_TOKEN_HEADERS
    )
    assert response.status_code == http.client.CREATED
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.update, new_project_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
        (iam_client.ProjectsPermissions.get, mocks.PROJECT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
        (iam_client.ProjectsPermissions.create, iam_client.GIZMO_RESOURCE, iam_client.GIZMO_RESOURCE_TYPE),
    }
    assert mock_iam_get_user_login.mock_calls == [mock.call(mocks.MOCKED_IAM_TOKEN, mocks.MOCKED_USER_ID)]
