"""Tests host deploy log API."""

from unittest import mock

import http.client

import walle.constants as walle_constants
from walle.hosts import HostState


@mock.patch("walle.views.api.host_api.info._get_deploy_log")
def test_get_deploy_log__host_free(get_deploy_log_mock, walle_test, monkeypatch):
    """Use project's provisioner if host is in free state."""
    project = walle_test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "walle_test-config",
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "host-1",
            "project": project.id,
            "state": HostState.FREE,
            "provisioner": walle_constants.PROVISIONER_EINE,
            "config": "eine-config",
            "platform": {"system": "system_mock", "board": "board_mock"},
        }
    )
    host.save()

    result = walle_test.api_client.get("/v1/hosts/0/deploy-log")

    assert result.status_code == http.client.OK
    get_deploy_log_mock.assert_called_once_with(walle_constants.PROVISIONER_LUI, None, host.inv, host.name, None)


@mock.patch("walle.views.api.host_api.info._get_deploy_log")
def test_get_deploy_log__host_no_provisioner(get_deploy_log_mock, walle_test, monkeypatch):
    project = walle_test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config",
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "host-1",
            "project": project.id,
            "state": HostState.ASSIGNED,
            "platform": {"system": "system_mock", "board": "board_mock"},
        }
    )
    del host.provisioner
    del host.config
    host.save()

    result = walle_test.api_client.get("/v1/hosts/0/deploy-log")

    assert result.status_code == http.client.OK
    get_deploy_log_mock.assert_called_once_with(walle_constants.PROVISIONER_LUI, None, host.inv, host.name, None)


@mock.patch("walle.views.api.host_api.info._get_deploy_log")
def test_get_deploy_log__host_with_provisioner(get_deploy_log_mock, walle_test, monkeypatch):
    project = walle_test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config",
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "host-1",
            "project": project.id,
            "state": HostState.ASSIGNED,
            "provisioner": walle_constants.PROVISIONER_EINE,
            "config": "test-config-222",
            "platform": {"system": "system_mock", "board": "board_mock"},
        }
    )

    result = walle_test.api_client.get("/v1/hosts/0/deploy-log")

    assert result.status_code == http.client.OK
    get_deploy_log_mock.assert_called_once_with(walle_constants.PROVISIONER_EINE, None, host.inv, host.name, None)


@mock.patch("walle.views.api.host_api.info._get_deploy_log")
def test_get_deploy_log__provisioner_in_params(get_deploy_log_mock, walle_test, monkeypatch):
    project = walle_test.mock_project(
        {
            "id": "test-project",
            "provisioner": walle_constants.PROVISIONER_LUI,
            "deploy_config": "test-config",
        }
    )
    host = walle_test.mock_host(
        {
            "inv": 0,
            "name": "host-1",
            "project": project.id,
            "state": HostState.ASSIGNED,
            "provisioner": walle_constants.PROVISIONER_EINE,
            "config": "test-config-222",
            "platform": {"system": "system_mock", "board": "board_mock"},
        }
    )

    result = walle_test.api_client.get(
        "/v1/hosts/0/deploy-log", query_string={"provisioner": walle_constants.PROVISIONER_LUI}
    )

    assert result.status_code == http.client.OK
    get_deploy_log_mock.assert_called_once_with(walle_constants.PROVISIONER_LUI, None, host.inv, host.name, None)
