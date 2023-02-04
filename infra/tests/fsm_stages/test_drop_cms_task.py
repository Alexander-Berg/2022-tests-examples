"""Tests CMS task cleanup"""

from unittest.mock import ANY, call

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    mock_commit_stage_changes,
)
from sepelib.core import constants
from walle.clients.cms import CmsError, CmsApiVersion, _BaseCmsClient
from walle.hosts import HostState
from walle.projects import DEFAULT_CMS_NAME
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture()
def cms_task_id():
    return "test-cms-task-id"


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.DROP_CMS_TASK))


def test_cms_cleanup(test, mp, cms_task_id):
    host = _mock_cms_host(test, cms_task_id)

    mock_delete_task = mp.method(_BaseCmsClient.delete_task, return_value=None, obj=_BaseCmsClient)

    handle_host(host)

    mock_complete_current_stage(host)

    mock_delete_task.assert_called_once_with(ANY, cms_task_id)
    test.hosts.assert_equal()


def test_cms_cleanup_for_N_cms(test, mp, cms_task_id):
    project = test.mock_project(
        {
            "id": "some-id",
            "cms_settings": [
                {"cms": DEFAULT_CMS_NAME, "cms_max_busy_hosts": 1},
                {"cms": "custom", "cms_api_version": CmsApiVersion.V1_4},
            ],
        }
    )
    host = _mock_host(test, project.id, cms_task_id)

    mock_delete_task = mp.method(_BaseCmsClient.delete_task, return_value=None, obj=_BaseCmsClient)

    handle_host(host)

    mock_complete_current_stage(host)

    mock_delete_task.assert_has_calls([call(ANY, cms_task_id), call(ANY, cms_task_id)])
    test.hosts.assert_equal()


def test_cms_request_error(test, mp, cms_task_id):
    host = _mock_cms_host(test, cms_task_id)

    cms_error = CmsError("CMS request failed")
    mock_delete_task = mp.method(_BaseCmsClient.delete_task, side_effect=cms_error, obj=_BaseCmsClient)

    handle_host(host)

    msg = "{}: Failed to remove task {} from CMS: {}".format(host.human_id(), cms_task_id, cms_error)
    mock_commit_stage_changes(host, error=msg, check_after=constants.MINUTE_SECONDS)

    mock_delete_task.assert_called_once_with(ANY, cms_task_id)
    test.hosts.assert_equal()


def _mock_cms_host(test, cms_task_id):
    # this will use default cms as per mocker defaults
    project = test.mock_project({"id": "some-id"})
    return _mock_host(test, project.id, cms_task_id)


def _mock_host(test, project_id, cms_task_id):
    return test.mock_host(
        {
            "project": project_id,
            "state": HostState.MAINTENANCE,
            "cms_task_id": cms_task_id,
            "task": mock_task(stage=Stages.DROP_CMS_TASK, stage_params={"cms_task_id": cms_task_id}),
        }
    )
