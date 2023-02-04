"""Tests host name assigning."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    mock_fail_current_stage,
    mock_commit_stage_changes,
    mock_retry_parent_stage,
    check_stage_initialization,
    mock_retry_current_stage,
)
from walle.errors import ResourceAlreadyExistsError, InvalidHostConfiguration
from walle.fsm_stages.assign_hostname import (
    _STATUS_ASSIGN,
    _STATUS_WAIT,
    _MAX_RACE_ERRORS,
    _BOT_POLLING_PERIOD,
    _RENAME_TIMEOUT,
    _RENAME_RETRY_TIMEOUT,
)
from walle.fsm_stages.common import get_current_stage, _set_persistent_error
from walle.hosts import HostState
from walle.models import timestamp
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.ASSIGN_HOSTNAME), status=_STATUS_ASSIGN)


@patch("walle.clients.bot.rename_host")
def test_assign(rename_host, test):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_ASSIGN)}
    )

    handle_host(host)

    rename_host.assert_called_once_with(host.inv, host.name)

    mock_commit_stage_changes(host, status=_STATUS_WAIT, check_now=True)
    test.hosts.assert_equal()


@pytest.mark.parametrize("exceed_limits", (True, False))
@patch("walle.clients.bot.rename_host", side_effect=ResourceAlreadyExistsError("Mocked error"))
def test_assign_with_race(rename_host, test, exceed_limits):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_ASSIGN)}
    )

    if exceed_limits:
        get_current_stage(host).set_data("race_errors", _MAX_RACE_ERRORS - 1)
        host.save()

    handle_host(host)

    rename_host.assert_called_once_with(host.inv, host.name)

    if exceed_limits:
        reason = (
            "Too many errors occurred during processing 'assign-hostname' stage of 'ready' task."
            " Last error: Failed to assign 'default' name to the host in BOT."
            " Too many race conditions with other services: rename operation failed 10 times."
        )
        mock_fail_current_stage(host, reason=reason)
    else:
        get_current_stage(host).set_data("race_errors", 1)
        mock_retry_parent_stage(host)

    test.hosts.assert_equal()


def test_assign_with_error(test):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_ASSIGN)}
    )

    with patch("walle.clients.bot.rename_host", side_effect=InvalidHostConfiguration("Mocked error")) as rename_host:
        handle_host(host)

    rename_host.assert_called_once_with(host.inv, host.name)
    mock_fail_current_stage(host, reason="Failed to assign 'default' name to the host in BOT: Mocked error")
    test.hosts.assert_equal()


def test_wait_completed(test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_WAIT),
            "rename_time": 0,
        }
    )

    with patch("walle.clients.bot.get_host_info", return_value={"name": host.name}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)

    host.rename_time = timestamp()
    mock_complete_current_stage(host, inc_revision=1)
    test.hosts.assert_equal()


@pytest.mark.parametrize("persistent_error", [None, "Operation failed, retrying."])
def test_wait_in_process(test, persistent_error):
    stage = Stage(name=Stages.ASSIGN_HOSTNAME, status=_STATUS_WAIT)
    _set_persistent_error(stage, persistent_error)

    host = test.mock_host({"state": HostState.FREE, "task": mock_task(stage=stage)})

    with patch("walle.clients.bot.get_host_info", return_value={"name": "some.other.name"}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    mock_commit_stage_changes(
        host,
        status_message="Waiting for BOT to assign the host name.",
        check_after=_BOT_POLLING_PERIOD,
        error=persistent_error,
    )
    test.hosts.assert_equal()


def test_wait_unexisting_host(test):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_WAIT)}
    )

    with patch("walle.clients.bot.get_host_info", return_value=None) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    mock_fail_current_stage(host, reason="The host has suddenly vanished from BOT during name assigning process.")
    test.hosts.assert_equal()


def test_wait_timeout_show_error(test):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_WAIT)}
    )
    get_current_stage(host).status_time = timestamp() - _RENAME_TIMEOUT
    host.save()

    with patch("walle.clients.bot.get_host_info", return_value={"name": "some.other.name"}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    error_message = (
        "Rename operation takes too long to complete, "
        "it is probably failing ont BOT's side. "
        "Please, contact with bot@yandex-team.ru. "
        "Waiting more."
    )
    mock_commit_stage_changes(host, error=error_message, check_after=_BOT_POLLING_PERIOD)

    test.hosts.assert_equal()


def test_wait_timeout_retry(test):
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.ASSIGN_HOSTNAME, stage_status=_STATUS_WAIT)}
    )
    get_current_stage(host).status_time = timestamp() - _RENAME_RETRY_TIMEOUT
    host.save()

    with patch("walle.clients.bot.get_host_info", return_value={"name": "some.other.name"}) as get_host_info:
        handle_host(host)

    get_host_info.assert_called_once_with(host.inv)
    error_message = (
        "Rename operation takes too long to complete, "
        "it is probably failing ont BOT's side. "
        "Please, contact with bot@yandex-team.ru. "
        "Retrying."
    )
    mock_retry_current_stage(
        host, Stages.ASSIGN_HOSTNAME, expected_status=_STATUS_ASSIGN, persistent_error=error_message.format(host.name)
    )
    test.hosts.assert_equal()
