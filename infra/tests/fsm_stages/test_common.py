"""Tests stage common functions."""

from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import TestCase, mock_task, mock_switch_stage, mock_task_completion
from sepelib.core.exceptions import Error
from sepelib.yandex.startrek import Relationship
from walle.clients import startrek
from walle.fsm_stages import common
from walle.fsm_stages.common import (
    handle_current_stage,
    _StageConfig,
    register_stage,
    retry_parent_stage,
    complete_current_stage,
    cancel_host_stages,
    push_host_ticket,
    commit_stage_changes,
    get_current_stage,
    retry_current_stage,
)
from walle.stages import Stage, StageTerminals


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def monkeypatch_stage_config(mp, handler=None, error_handler=None):
    return mp.function(
        common._get_stage_config, side_effect=lambda name: _StageConfig(handler, None, None, error_handler)
    )


@pytest.fixture
def mock_stage_config(mp):
    monkeypatch_stage_config(mp)


def test_handle_current_stage_with_default_handler(mp, test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    handler = Mock(side_effect=complete_current_stage)
    monkeypatch_stage_config(mp, handler=handler, error_handler=Mock())

    handle_current_stage(host.copy())

    mock_switch_stage(host, current_stage, stages[1].stages[2], check_after=0)

    handler.assert_called_once_with(host)
    test.hosts.assert_equal()


def test_handle_current_stage_with_error_handler_skip_default_handler(mp, test):
    stages = _mock_stages(with_parents=True)
    host = test.mock_host({"task": mock_task(stage=stages[1].stages[1], stages=stages, error="mock-task-error")})

    handler = Mock(side_effect=complete_current_stage)
    error_handler = Mock(return_value=False)
    monkeypatch_stage_config(mp, handler=handler, error_handler=error_handler)

    handle_current_stage(host.copy())

    assert not handler.called
    error_handler.assert_called_once_with(host)
    test.hosts.assert_equal()


def test_handle_current_stage_with_error_handler_and_default_handler(mp, test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages, error="mock-task-error")})

    handler = Mock(side_effect=complete_current_stage)
    error_handler = Mock(return_value=True)
    monkeypatch_stage_config(mp, handler=handler, error_handler=error_handler)

    handle_current_stage(host.copy())

    mock_switch_stage(host, current_stage, stages[1].stages[2], check_after=0)
    del host.task.error

    handler.assert_called_once_with(host)
    error_handler.assert_called_once_with(host)
    test.hosts.assert_equal()


def test_handle_current_stage_with_error_handler_which_raises_exception(mp, test):
    stages = _mock_stages(with_parents=True)
    host = test.mock_host({"task": mock_task(stage=stages[1].stages[1], stages=stages, error="mock-task-error")})

    handler = Mock(side_effect=complete_current_stage)
    error_handler = Mock(side_effect=ValueError("mock"))
    monkeypatch_stage_config(mp, handler=handler, error_handler=error_handler)

    with pytest.raises(Exception):
        handle_current_stage(host.copy())

    assert not handler.called
    error_handler.assert_called_once_with(host)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_stage(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    complete_current_stage(host.copy())
    mock_switch_stage(host, current_stage, stages[1].stages[2], check_after=0)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_stage_with_persistent_error(test):
    error_message = "Error mock."
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    common._set_persistent_error(stages[1], error=error_message)

    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    complete_current_stage(host.copy())
    mock_switch_stage(host, current_stage, stages[1].stages[2], check_after=0, error=error_message)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_stage_without_parents(test):
    stages = _mock_stages()
    current_stage = stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    complete_current_stage(host.copy())
    mock_switch_stage(host, current_stage, stages[2], check_after=0)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_last_stage_without_parents(test):
    stages = _mock_stages()
    host = test.mock_host({"task": mock_task(stage=stages[2], stages=stages)})

    complete_current_stage(host.copy())
    mock_task_completion(host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_parent(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[2]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    complete_current_stage(host.copy())
    mock_switch_stage(host, current_stage, stages[2].stages[0], check_after=0)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_last_parent(test):
    stages = _mock_stages(with_parents=True)
    host = test.mock_host({"task": mock_task(stage=stages[2].stages[0], stages=stages)})

    complete_current_stage(host.copy())
    mock_task_completion(host)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_current_stage(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    current_stage.set_temp_data("temp_data", "mock temp data")
    current_stage.set_data("data", "mock data")

    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_current_stage(host.copy(), check_after=999)

    mock_switch_stage(host, current_stage, current_stage, check_after=999)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_current_stage_and_set_persistent_error(test):
    error_message = "Error mock."
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]

    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_current_stage(host.copy(), persistent_error=error_message, check_after=999)

    common._set_persistent_error(current_stage, error=error_message)
    mock_switch_stage(host, current_stage, current_stage, check_after=999, error=error_message)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_current_stage_with_previous_persistent_error(test):
    error_message = "Error mock."
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    common._set_persistent_error(current_stage, error=error_message)

    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_current_stage(host.copy(), check_after=999)

    mock_switch_stage(host, current_stage, current_stage, check_after=999, error=error_message)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_current_stage_with_persistent_error_for_parent_stage(test):
    error_message = "Error mock."
    stages = _mock_stages(with_parents=True)
    common._set_persistent_error(stages[1], error=error_message)

    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_current_stage(host.copy(), check_after=999)

    mock_switch_stage(host, current_stage, current_stage, check_after=999, error=error_message)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_parent_stage(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_parent_stage(host.copy(), check_after=999)
    mock_switch_stage(host, current_stage, stages[1], check_after=999, initial_status=common.PARENT_STAGE_RETRY_STATUS)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_parent_stage_with_error(test):
    error_message = "Error mock."
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    retry_parent_stage(host.copy(), error=error_message, check_after=999)

    common._set_persistent_error(stages[1], error=error_message)
    mock_switch_stage(
        host,
        current_stage,
        stages[1],
        check_after=999,
        error=error_message,
        initial_status=common.PARENT_STAGE_RETRY_STATUS,
    )

    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_retry_parent_stage_without_parents(test):
    stages = _mock_stages()
    current_stage = stages[1]
    host = test.mock_host({"task": mock_task(stage=current_stage, stages=stages)})

    with pytest.raises(Error):
        retry_parent_stage(host.copy(), check_after=999)

    test.hosts.assert_equal()


@pytest.mark.parametrize("custom_terminator", [True, False])
@pytest.mark.usefixtures("mock_stage_config")
def test_custom_stage_terminator(test, mp, custom_terminator):
    mock_skip = Mock(spec=common.skip_stage)
    mock_complete = Mock(spec=common.complete_stage)

    mp.setattr(common, "_STAGES", {})
    mp.setattr(
        common,
        "_TERMINATORS",
        {
            StageTerminals.SUCCESS: mock_complete,
            StageTerminals.SKIP: mock_skip,
        },
    )

    stages = _mock_stages(with_parents=True)
    stage = stages[1].stages[1]

    if custom_terminator:
        stage.terminators = {StageTerminals.SUCCESS: StageTerminals.SKIP}

    host = test.mock_host({"task": mock_task(stage=stage, stages=stages)})
    complete_current_stage(host)

    if custom_terminator:
        assert mock_complete.mock_calls == []
        mock_skip.assert_called_once_with(host, stage)
    else:
        assert mock_skip.mock_calls == []
        mock_complete.assert_called_once_with(host, stage)


# cancel stages
def test_cancel_task(test, mp):
    cancelled = []

    def handler(host):
        assert False

    def cancellation_handler(host, stage):
        cancelled.append(stage.name)
        raise Exception("Mocked error that must be ignored.")

    mp.setattr(common, "_STAGES", {})
    register_stage("prev-parent", handler)
    register_stage("first", handler, cancellation_handler=cancellation_handler)
    register_stage("second", handler)
    register_stage("third", handler, cancellation_handler=cancellation_handler)
    register_stage("next-parent", handler)
    register_stage("next-parent-child", handler)

    stages = _mock_stages(with_parents=True)
    host = test.mock_host({"task": mock_task(stage=stages[1], stages=stages)})

    cancel_host_stages(host.copy(), suppress_internal_errors=False)

    assert cancelled == ["third", "first"]
    test.hosts.assert_equal()


def test_push_host_ticket_adds_ticket_to_host(test):
    ticket_key = "ITDC-00000"
    stages = _mock_stages(with_parents=True)
    host = test.mock_host({"task": mock_task(stage=stages[2].stages[0], stages=stages, error="mock-error")})

    host_copy = host.copy()
    push_host_ticket(host_copy, ticket_key)
    commit_stage_changes(host_copy, extra_fields=["ticket"])

    host.ticket = ticket_key

    host.task.revision += 1
    get_current_stage(host).set_data("tickets", ["ITDC-00000"])
    test.hosts.assert_equal()


def test_push_host_ticket_remembers_host_ticket_and_links_them(mp, test):
    old_ticket_key = "ITDC-00000"
    new_ticket_key = "ITDC-00001"
    mock_link_tickets = mp.function(startrek.link_tickets, return_value=[old_ticket_key])

    stages = _mock_stages(with_parents=True)
    current_stage = stages[2].stages[0]
    host = test.mock_host(
        {
            "ticket": old_ticket_key,
            "task": mock_task(stage=current_stage, stages=stages, error="mock-error"),
        }
    )

    host_copy = host.copy()
    push_host_ticket(host_copy, new_ticket_key)
    commit_stage_changes(host_copy, extra_fields=["ticket"])

    current_stage.set_data("tickets", [old_ticket_key, new_ticket_key])
    current_stage.set_data("host_ticket", old_ticket_key)
    host.ticket = new_ticket_key
    host.task.revision += 1

    mock_link_tickets.assert_called_once_with(new_ticket_key, [old_ticket_key], silent=True)
    test.hosts.assert_equal()


def test_push_host_ticket_remembers_stage_tickets_and_links_them(mp, test):
    old_stage_tickets = ["ITDC-00000", "ITDC-00001"]
    new_ticket_key = "ITDC-00002"
    mock_link_tickets = mp.function(startrek.link_tickets, return_value=old_stage_tickets)

    stages = _mock_stages(with_parents=True)
    current_stage = stages[2].stages[0]
    current_stage.set_data("tickets", old_stage_tickets)

    host = test.mock_host(
        {
            "ticket": old_stage_tickets[-1],
            "task": mock_task(stage=current_stage, stages=stages, error="mock-error"),
        }
    )

    host_copy = host.copy()
    push_host_ticket(host_copy, new_ticket_key)
    commit_stage_changes(host_copy, extra_fields=["ticket"])

    current_stage.set_data("tickets", old_stage_tickets + [new_ticket_key])
    host.ticket = new_ticket_key
    host.task.revision += 1

    mock_link_tickets.assert_called_once_with(new_ticket_key, old_stage_tickets, silent=True)
    test.hosts.assert_equal()


def test_push_host_ticket_already_linked(mp, test):
    old_stage_tickets = ["ITDC-00000", "ITDC-00001"]
    new_ticket_key = "ITDC-00002"

    client_mock = Mock()
    client_mock.get_relationships.return_value = [{"object": {"key": old_stage_tickets[0]}}]
    mp.function(startrek.get_client, return_value=client_mock)

    stages = _mock_stages(with_parents=True)
    current_stage = stages[2].stages[0]
    current_stage.set_data("tickets", old_stage_tickets)

    host = test.mock_host(
        {
            "ticket": old_stage_tickets[-1],
            "task": mock_task(stage=current_stage, stages=stages, error="mock-error"),
        }
    )

    host_copy = host.copy()
    push_host_ticket(host_copy, new_ticket_key)
    commit_stage_changes(host_copy, extra_fields=["ticket"])

    current_stage.set_data("tickets", old_stage_tickets + [new_ticket_key])
    host.ticket = new_ticket_key
    host.task.revision += 1

    client_mock.add_relationship.assert_called_once_with(
        new_ticket_key, relationship=Relationship.RELATES, other_issue_id=old_stage_tickets[1]
    )
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_current_stage_returns_saved_host_ticket(test):
    old_host_ticket = "ITDC-00000"
    current_host_ticket = "ITDC-00001"

    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[0]
    current_stage.set_data("tickets", [old_host_ticket, current_host_ticket])
    current_stage.set_data("host_ticket", old_host_ticket)

    host = test.mock_host(
        {
            "ticket": current_host_ticket,
            "task": mock_task(stage=current_stage, stages=stages),
        }
    )

    complete_current_stage(host.copy())

    host.ticket = old_host_ticket
    mock_switch_stage(host, current_stage, stages[1].stages[1], check_after=0)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_current_stage_cleans_stage_tickets_from_host(test):
    old_stage_ticket = "ITDC-00000"
    current_stage_ticket = "ITDC-00001"

    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[1]
    current_stage.set_data("tickets", [old_stage_ticket, current_stage_ticket])

    host = test.mock_host(
        {
            "ticket": current_stage_ticket,
            "task": mock_task(stage=current_stage, stages=stages),
        }
    )

    complete_current_stage(host.copy())

    del host.ticket
    mock_switch_stage(host, current_stage, stages[1].stages[2], check_after=0)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_current_stage_restores_tickets_for_child_stages(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[2]
    current_stage.set_data("tickets", ["ITDC-00000", "ITDC-00001"])

    host = test.mock_host(
        {
            "ticket": "ITDC-00001",
            "task": mock_task(stage=current_stage, stages=stages),
        }
    )

    complete_current_stage(host.copy())

    del host.ticket
    mock_switch_stage(host, current_stage, stages[2].stages[0], check_after=0)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_current_stage_keeps_host_ticket_if_no_stage_tickets_were_created(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[1].stages[0]

    host = test.mock_host(
        {
            "ticket": "ITDC-00000",
            "task": mock_task(stage=current_stage, stages=stages),
        }
    )

    complete_current_stage(host.copy())

    mock_switch_stage(host, current_stage, stages[1].stages[1], check_after=0)
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_stage_config")
def test_complete_task_cleans_host_ticket(test):
    stages = _mock_stages(with_parents=True)
    current_stage = stages[2].stages[0]

    host = test.mock_host(
        {
            "ticket": "ITDC-00000",
            "task": mock_task(stage=current_stage, stages=stages),
        }
    )

    complete_current_stage(host.copy())

    del host.ticket
    mock_task_completion(host)
    test.hosts.assert_equal()


# more fixtures
def _mock_stages(with_parents=False):
    stages = [Stage(name="first"), Stage(name="second"), Stage(name="third")]
    if with_parents:
        stages = [
            Stage(name="prev-parent"),
            Stage(name="parent", stages=stages),
            Stage(name="next-parent", stages=[Stage(name="next-parent-child")]),
        ]
    return stages
