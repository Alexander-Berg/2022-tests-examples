import http.client

from tests.api.iam_permissions import mocks
from walle.clients import iam as iam_client

EMPTY_PROJECT_ID = "empty-id"
EMPTY_PROJECT_FOLDER_ID = f"{EMPTY_PROJECT_ID}-folder"
OTHER_PROJECT_ID = "other-id"
OTHER_PROJECT_FOLDER_ID = f"{OTHER_PROJECT_ID}-folder"


def _make_test_data(walle_test):
    empty_project = walle_test.mock_project(
        {
            "id": EMPTY_PROJECT_ID,
            "yc_iam_folder_id": EMPTY_PROJECT_FOLDER_ID,
        }
    )
    other_project = walle_test.mock_project(
        {
            "id": OTHER_PROJECT_ID,
            "yc_iam_folder_id": OTHER_PROJECT_FOLDER_ID,
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "sas0.yandex.net",
            "project": other_project.id,
        }
    )
    return empty_project, other_project, host


def test_get_full_audit_log(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    empty_project, other_project, _host = _make_test_data(walle_test)
    response = walle_test.api_client.get("/v1/audit-log", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.get, empty_project.yc_iam_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
        (iam_client.ProjectsPermissions.get, other_project.yc_iam_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
    }


def test_get_audit_log_with_project_filter(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    empty_project, other_project, host = _make_test_data(walle_test)
    response = walle_test.api_client.get(
        "/v1/audit-log",
        query_string={"project": ",".join([empty_project.id, other_project.id])},
        headers=mocks.IAM_TOKEN_HEADERS,
    )
    assert response.status_code == http.client.OK
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.get, empty_project.yc_iam_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
        (iam_client.ProjectsPermissions.get, other_project.yc_iam_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
    }


def test_get_audit_log_with_host_filter(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    _empty_project, other_project, host = _make_test_data(walle_test)
    response = walle_test.api_client.get(
        "/v1/audit-log", query_string={"host_name": host.name}, headers=mocks.IAM_TOKEN_HEADERS
    )
    assert response.status_code == http.client.OK
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.get, other_project.yc_iam_folder_id, iam_client.FOLDER_RESOURCE_TYPE),
    }
