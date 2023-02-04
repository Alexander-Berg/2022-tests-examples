import http.client
from unittest import mock

from tests.api.iam_permissions import mocks
from walle.clients import iam as iam_client


def test_create_automation_plot(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    request_data = {
        "id": "plot-id",
        "name": "plot-name",
        "yc_iam_folder_id": mocks.AUTOMATION_PLOT_FOLDER_ID,
    }
    response = walle_test.api_client.post("/v1/automation-plot/", data=request_data, headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.CREATED
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (
            iam_client.AutomationPlotsPermissions.update,
            mocks.AUTOMATION_PLOT_FOLDER_ID,
            iam_client.FOLDER_RESOURCE_TYPE,
        ),
    }
    assert mock_iam_get_user_login.mock_calls == [mock.call(mocks.MOCKED_IAM_TOKEN, mocks.MOCKED_USER_ID)]


def test_get_automation_plot(iam, walle_test, mock_iam_as_authenticate, mock_iam_get_user_login):
    plot_count = 3
    for index in range(plot_count):
        walle_test.automation_plot.mock(
            {
                "id": f"orig-plot-id-{index}",
                "name": f"plot-name-{index}",
                "yc_iam_folder_id": f"some-project-folder-id-{index}",
            }
        )
    response = walle_test.api_client.get("/v1/automation-plot/", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
    assert len(response.json["result"]) == plot_count


def test_delete_automation_plot(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    plot = walle_test.automation_plot.mock({"yc_iam_folder_id": mocks.AUTOMATION_PLOT_FOLDER_ID})
    response = walle_test.api_client.delete(f"/v1/automation-plot/{plot.id}", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.NO_CONTENT
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (
            iam_client.AutomationPlotsPermissions.delete,
            mocks.AUTOMATION_PLOT_FOLDER_ID,
            iam_client.FOLDER_RESOURCE_TYPE,
        ),
    }
