import http.client
from unittest import mock

import pytest

import walle.host_operations
from infra.walle.server.tests.lib.util import (
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_network_get_current_host_switch_port,
)
from tests.api.iam_permissions import mocks
from walle.clients import iam as iam_client, bot
from walle.hosts import HostState


def test_add_host(iam, mp, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    mp.function(walle.host_operations._exists_in_dns, return_value=True)
    monkeypatch_network_get_current_host_switch_port(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    mp.function(bot.missed_preordered_hosts, return_value={})

    project = walle_test.mock_project(
        {
            "id": "some-id",
            "yc_iam_folder_id": mocks.PROJECT_FOLDER_ID,
        }
    )
    request_data = {
        "inv": 0,
        "name": "sas1.yandex.net",
        "project": project.id,
    }
    response = walle_test.api_client.post("/v1/hosts", data=request_data, headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.CREATED
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.update, mocks.PROJECT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
    }
    assert mock_iam_get_user_login.mock_calls == [mock.call(mocks.MOCKED_IAM_TOKEN, mocks.MOCKED_USER_ID)]


@pytest.mark.usefixtures("cms_accept")
def test_reboot_host(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    project = walle_test.mock_project(
        {
            "id": "some-id",
            "yc_iam_folder_id": mocks.PROJECT_FOLDER_ID,
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "sas1.yandex.net",
            "project": project.id,
            "state": HostState.ASSIGNED,
        }
    )
    response = walle_test.api_client.post(f"/v1/hosts/{host.id}/reboot", data={}, headers=mocks.IAM_TOKEN_HEADERS)
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.update, mocks.PROJECT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
    }
    assert mock_iam_get_user_login.mock_calls == [mock.call(mocks.MOCKED_IAM_TOKEN, mocks.MOCKED_USER_ID)]
    assert response.status_code == http.client.OK


def test_get_host_list(iam, walle_test, mock_iam_as_authenticate, mock_iam_get_user_login):
    project = walle_test.mock_project(
        {
            "id": "some-id",
            "yc_iam_folder_id": mocks.PROJECT_FOLDER_ID,
        }
    )
    hosts_count = 3
    for index in range(hosts_count):
        walle_test.mock_host(
            {
                "inv": index,
                "name": f"sas{index}.yandex.net",
                "project": project.id,
                "ips": [],
            }
        )
    response = walle_test.api_client.get("/v1/hosts", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
    assert len(response.json["result"]) == hosts_count
    for project in response.json["result"]:
        # NOTE(rocco66): 'inv' is public field, 'ips' is not
        assert "inv" in project
        assert "ips" not in project


def test_get_one_host(iam, walle_test, mock_iam_as_authorize, mock_iam_get_user_login):
    project = walle_test.mock_project(
        {
            "id": "some-id",
            "yc_iam_folder_id": mocks.PROJECT_FOLDER_ID,
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "sas0.yandex.net",
            "project": project.id,
        }
    )
    response = walle_test.api_client.get(f"/v1/hosts/{host.inv}", headers=mocks.IAM_TOKEN_HEADERS)
    assert response.status_code == http.client.OK
    assert mocks.get_calls_args(mock_iam_as_authorize.mock_calls) == {
        (iam_client.ProjectsPermissions.get, mocks.PROJECT_FOLDER_ID, iam_client.FOLDER_RESOURCE_TYPE),
    }
