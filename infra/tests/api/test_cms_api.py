"""Tests CMS API and client."""

from unittest.mock import call

import pytest
import http.client
from requests import Response

from infra.walle.server.tests.lib.util import patch, patch_attr
from walle.authorization import blackbox
from walle.authorization.blackbox import AuthInfo
from walle.clients.cms import (
    CmsTaskType,
    CmsTaskAction,
    CmsTaskStatus,
    CmsApiError,
    CmsTaskRejectedError,
    CmsApiVersion,
    get_cms_client,
)
from walle.default_cms import NonDefaultCMSError
from walle.errors import ResourceNotFoundError


class UrlOpener:
    def __init__(self, client):
        self.__client = client

    def open(self, method, url, params=None, headers=None, data=None, timeout=None):
        result = self.__client.open(url, method=method, query_string=params, headers=headers, data=data)

        response = Response()
        response.status_code = result.status_code
        response.reason = result.status
        response.headers = result.headers
        response._content = result.data
        return response


@pytest.fixture(params=CmsApiVersion.ALL_CMS_API)
def cms_client(walle_test, request):
    walle_test.mock_project({"id": "project-mock"})
    client_version = get_cms_client(request.param)
    return client_version(
        "mock",
        "project-mock",
        "/cms/v1/project-mock",
        url_opener=UrlOpener(walle_test.api_client),
        query_params={"strict": True},
    )


@pytest.fixture
def robot_walle(monkeypatch):
    patch_attr(monkeypatch, blackbox, "authenticate", return_value=AuthInfo(issuer="robot-walle@", session_id=None))


@pytest.mark.parametrize("dry_run", (True, False))
def test_add(cms_client, dry_run, robot_walle):
    task, result = _mock_task(CmsTaskStatus.OK)

    with patch("walle.default_cms.add_task", return_value=result) as add_task:
        assert cms_client.add_task(dry_run=dry_run, **task) == result

    assert add_task.mock_calls == [call("project-mock", task, dry_run=dry_run)]


def test_add_no_cms_max_busy_hosts(cms_client, robot_walle):
    task = _mock_task()

    with patch("walle.default_cms.add_task", side_effect=NonDefaultCMSError("")) as add_task:
        with pytest.raises(CmsApiError):
            cms_client.add_task(dry_run=False, **task)

    assert add_task.mock_calls == [call("project-mock", task, dry_run=False)]


@pytest.mark.parametrize("dry_run", (True, False))
def test_add_no_hosts(cms_client, dry_run, robot_walle):
    """Validation error on empty hosts in POST /tasks"""
    task = _mock_task(hosts=[])

    with pytest.raises(CmsApiError) as error:
        cms_client.add_task(dry_run=dry_run, **task)

    message = str(error.value)
    assert "Request validation error" in message and "must contain at least 1 items" in message


def test_add_ready(cms_client, robot_walle):
    task, result = _mock_task(CmsTaskStatus.OK, hosts=["host-mock-1", "host-mock-2"])
    assert cms_client.add_task(**task) == result
    assert cms_client.get_task(task["id"]) == result


@pytest.mark.parametrize("dry_run", (True, False))
def test_add_rejected(cms_client, dry_run, robot_walle):
    task, result = _mock_task(CmsTaskStatus.REJECTED)

    with patch("walle.default_cms.add_task", return_value=result) as add_task:
        with pytest.raises(CmsTaskRejectedError):
            cms_client.add_task(dry_run=dry_run, **task)

    assert add_task.mock_calls == [call("project-mock", task, dry_run=dry_run)]


def test_get(cms_client):
    task = {"id": "task-id-mock", "hosts": ["host-mock"], "status": CmsTaskStatus.OK}

    with patch("walle.default_cms.get_task", return_value=task) as get_task:
        assert cms_client.get_task(task["id"]) == task

    assert get_task.mock_calls == [call("project-mock", task["id"])]


def test_get_missing(cms_client):
    with patch("walle.default_cms.get_task", side_effect=ResourceNotFoundError("Mocked not found error")) as get_task:
        assert cms_client.get_task("task-id-mock") is None

    assert get_task.mock_calls == [call("project-mock", "task-id-mock")]


def test_get_all(cms_client):
    task = {"id": "task-id-mock", "hosts": ["host-mock"], "status": CmsTaskStatus.OK}

    with patch("walle.default_cms.get_tasks", return_value=[task]) as get_tasks:
        assert cms_client.get_tasks() == [task]

    assert get_tasks.mock_calls == [call("project-mock")]


def test_delete(cms_client, robot_walle):
    with patch("walle.default_cms.delete_task", return_value=None) as delete_task:
        assert cms_client.delete_task("task-id-mock") is None

    assert delete_task.mock_calls == [call("project-mock", "task-id-mock")]


def test_delete_missing(cms_client, robot_walle):
    mock_error = ResourceNotFoundError("Mocked not found error")
    with patch("walle.default_cms.delete_task", side_effect=mock_error) as delete_task:
        assert cms_client.delete_task("task-id-mock") is None

    assert delete_task.mock_calls == [call("project-mock", "task-id-mock")]


def test_internal_error(cms_client):
    with patch("walle.default_cms.get_task", side_effect=Exception("Mocked error")) as get_task:
        with pytest.raises(CmsApiError) as error:
            assert cms_client.get_task("some-id")

    assert error.value.status_code == http.client.INTERNAL_SERVER_ERROR
    assert str(error.value).endswith("Mocked error")

    assert get_task.mock_calls == [call("project-mock", "some-id")]


@pytest.mark.parametrize("action", CmsTaskAction.ALL)
def test_all_versions_allow_all_operations(cms_client, robot_walle, action):
    task, result = _mock_task(CmsTaskStatus.OK, action=action)

    with patch("walle.default_cms.add_task", return_value=result) as add_task:
        cms_client.add_task(**task)

    if action in cms_client._supported_actions:
        assert add_task.mock_calls == [call("project-mock", task, dry_run=False)]
    else:
        # unsupported cms actions go here
        if cms_client.api_version == CmsApiVersion.V1_0 and action in (CmsTaskAction.DEACTIVATE, CmsTaskAction.PREPARE):
            # fake allow
            assert add_task.mock_calls == []
        else:
            downscaled_action = {
                CmsTaskAction.TEMPORARY_UNREACHABLE: CmsTaskAction.REBOOT,
            }[action]

            downscaled_task, result = _mock_task(CmsTaskStatus.OK, action=downscaled_action)
            assert add_task.mock_calls == [call("project-mock", downscaled_task, dry_run=False)]

    assert result["status"] == CmsTaskStatus.OK


def _mock_task(status=None, hosts=None, action=CmsTaskAction.REBOOT):
    common = {
        "id": "task-id-mock",
        "hosts": ["host-mock"] if hosts is None else hosts,
    }

    adding_task = dict(
        common,
        **{
            "type": CmsTaskType.MANUAL,
            "issuer": "user@",
            "action": action,
        }
    )

    if status is None:
        return adding_task

    result_task = dict(common, status=status)
    return adding_task, result_task
