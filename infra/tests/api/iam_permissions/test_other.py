import http.client
from unittest import mock

import pytest
import yc_as_client

from infra.walle.server.tests.lib.util import monkeypatch_config
from tests.api.iam_permissions import mocks


def test_get_csrf_token(mp, iam, walle_test, mock_iam_as_authenticate, mock_iam_get_user_login):
    monkeypatch_config(mp, "authorization.csrf_key", "0000")
    headers = mocks.IAM_TOKEN_HEADERS.copy()
    headers["Cookie"] = "yc_session=some"
    response = walle_test.api_client.get("/v1/csrf-token", headers=headers)
    assert response.status_code == http.client.OK
    assert response.json["csrf_token"]


FOLDERS_WITH_ACCESS = ["folder1", "folder2"]
FOLDERS_WITHOUT_ACCESS = ["folder3"]


@pytest.yield_fixture
def mock_custom_iam_as_authorize():
    def authorize_mock(*args, **kwargs):
        def future_wrapper():
            if kwargs["resource_path"].id in FOLDERS_WITH_ACCESS:
                return yc_as_client.entities.UserAccountSubject(mocks.MOCKED_USER_ID)
            else:
                mock_original_exception = mock.Mock()
                mock.details = mock.Mock(return_value={})
                raise yc_as_client.exceptions.PermissionDeniedException(mock_original_exception)

        return future_wrapper

    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authorize",
        new=authorize_mock,
        spec=yc_as_client.YCAccessServiceClient.authorize,
    ) as mocked_authorize:
        yield mocked_authorize


def test_get_user_info(
    iam, walle_test, mock_custom_iam_as_authorize, mock_iam_as_authenticate, mock_iam_get_user_login
):
    projects_with_access = set()
    for folder_id in FOLDERS_WITH_ACCESS + FOLDERS_WITHOUT_ACCESS:
        project_id = f"orig-project-{folder_id}"
        walle_test.mock_project(
            {
                "id": project_id,
                "yc_iam_folder_id": folder_id,
            }
        )
        if folder_id in FOLDERS_WITH_ACCESS:
            projects_with_access.add(project_id)
    response = walle_test.api_client.get("/v1/user", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
    assert set(response.json["projects"]) == projects_with_access


def test_sa_authentification(
    iam, walle_test, mock_iam_sa_as_authorize, mock_iam_sa_as_authenticate, mock_iam_get_sa_name
):
    response = walle_test.api_client.get("/v1/hosts", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
