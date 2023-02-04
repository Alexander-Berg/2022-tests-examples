"""Tests bot project_id assigning."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    mock_commit_stage_changes,
    mock_fail_current_stage,
    check_stage_initialization,
    mock_retry_current_stage,
)
from walle.errors import InvalidHostConfiguration
from walle.fsm_stages.assign_bot_project import (
    _STATUS_SWITCHING,
    _STATUS_WAITING,
    _BOT_POLLING_PERIOD,
    _SWITCH_TIMEOUT,
    _SWITCH_RETRY_TIMEOUT,
)
from walle.fsm_stages.common import get_current_stage
from walle.hosts import HostState
from walle.models import timestamp
from walle.stages import Stages, Stage

_BOT_PROJECT_ID = 1000009


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.ASSIGN_BOT_PROJECT), status=_STATUS_SWITCHING)


@patch("walle.clients.bot.assign_project_id")
def test_assign(rename_host, test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_SWITCHING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )

    handle_host(host)

    rename_host.assert_called_once_with(host.inv, _BOT_PROJECT_ID)

    mock_commit_stage_changes(host, status=_STATUS_WAITING, check_now=True)
    test.hosts.assert_equal()


def test_assign_with_error(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_SWITCHING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )

    with patch(
        "walle.clients.bot.assign_project_id", side_effect=InvalidHostConfiguration("Mocked error")
    ) as rename_host:
        handle_host(host)

    rename_host.assert_called_once_with(host.inv, _BOT_PROJECT_ID)
    mock_fail_current_stage(host, reason="Failed to set BOT/OEBS project #1000009 in BOT: Mocked error")
    test.hosts.assert_equal()


def test_wait_completed(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_WAITING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
            "rename_time": 0,
        }
    )

    with patch("walle.clients.bot.get_host_info", return_value={"bot_project_id": _BOT_PROJECT_ID}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


def test_wait_in_process(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_WAITING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )

    with patch("walle.clients.bot.get_host_info", return_value={"bot_project_id": 9000001}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    mock_commit_stage_changes(
        host,
        status_message="Waiting for BOT to assign the new BOT/OEBS project to the host.",
        check_after=_BOT_POLLING_PERIOD,
    )
    test.hosts.assert_equal()


def test_wait_unexisting_host(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_WAITING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )

    with patch("walle.clients.bot.get_host_info", return_value=None) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)

    reason = "The host has suddenly vanished from BOT during BOT/OEBS project change process."
    mock_fail_current_stage(host, reason=reason)

    test.hosts.assert_equal()


def test_wait_timeout_error_message(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_WAITING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )
    get_current_stage(host).status_time = timestamp() - _SWITCH_TIMEOUT
    host.save()

    with patch("walle.clients.bot.get_host_info", return_value={"bot_project_id": 9000001}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    error_message = (
        "Assigning BOT/OEBS project to host takes too long. "
        "It's probably a failure on the BOT's side. "
        "Please, contact with bot@yandex-team.ru. "
        "Waiting more."
    )
    mock_commit_stage_changes(host, error=error_message, check_after=_BOT_POLLING_PERIOD)
    test.hosts.assert_equal()


def test_wait_timeout_retry(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.ASSIGN_BOT_PROJECT,
                stage_status=_STATUS_WAITING,
                stage_params={"bot_project_id": _BOT_PROJECT_ID},
            ),
        }
    )
    get_current_stage(host).status_time = timestamp() - _SWITCH_RETRY_TIMEOUT
    host.save()

    with patch("walle.clients.bot.get_host_info", return_value={"bot_project_id": 9000001}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)

    error_message = (
        "Assigning BOT/OEBS project to host takes too long. "
        "It's probably a failure on the BOT's side. "
        "Please, contact with bot@yandex-team.ru. "
        "Retrying."
    )

    mock_retry_current_stage(
        host, expected_name=Stages.ASSIGN_BOT_PROJECT, expected_status=_STATUS_SWITCHING, persistent_error=error_message
    )
    test.hosts.assert_equal()
