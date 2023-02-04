import asyncio
import logging
from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.warden.client.lib.exceptions import Conflict

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_client]


async def test_called_with_expected_context_state(client_factory, periodical_task):
    result = None

    async def handler(context):
        nonlocal result
        result = dict(metadata=context.metadata, params=context.params)

    task = periodical_task(handler, params={"key": "value"})

    await task(client_factory)

    assert result == {"metadata": {}, "params": {"key": "value"}}


@pytest.mark.parametrize(
    ["exception", "expected_log"],
    [
        (Exception("raised test exception"), "raised test exception"),
        (Conflict(), 'Conflict creation new task type of "task_1"'),
    ],
)
async def test_logs_func_exceptions(
    exception, expected_log, client_factory, periodical_task, caplog
):
    caplog.clear()
    caplog.set_level(logging.INFO)

    handler = Mock(side_effect=[exception])

    await periodical_task(handler)(client_factory)

    handler.assert_called()
    assert caplog.messages == [expected_log]


async def test_stops_handler_if_timeout(client_factory, periodical_task, caplog):
    caplog.clear()
    client_factory.client("task1").create_task.coro.return_value = {
        "task_id": 1,
        "status": "accepted",
        "time_limit": 0.1,
    }

    task = periodical_task(Mock(side_effect=[asyncio.sleep(0.1)]))

    await task(client_factory)

    assert caplog.messages == ['Task "task_1" failed by timeout']


async def test_requests_warden_for_task(client_factory, mock_client, periodical_task):
    task = periodical_task(Mock(side_effect=[asyncio.sleep(0.1)]))

    await task(client_factory)

    assert mock_client.create_task.call_count == 1


async def test_will_notify_warden_on_completion(
    client_factory, mock_client, periodical_task
):
    task = periodical_task(Mock(side_effect=[asyncio.sleep(0.1)]))

    await task(client_factory)

    mock_client.update_task.assert_called_with(
        task_id=1, status="completed", metadata=None
    )


async def test_will_notify_warden_on_fail(client_factory, mock_client, periodical_task):
    task = periodical_task(Mock(side_effect=Exception("error text")))

    await task(client_factory)

    mock_client.update_task.assert_called_with(
        task_id=1,
        status="failed",
        metadata={"exception_type": "Exception", "exception_value": "error text"},
    )


async def test_retries_on_any_create_task_exception(
    client_factory, periodical_task, loop
):
    handler = coro_mock()
    handler.side_effect = Exception("error text")

    task = loop.create_task(periodical_task(handler).run(client_factory))

    await asyncio.sleep(0.1)

    assert handler.call_count > 1

    task.cancel()


async def test_retries_on_task_creation_error(
    loop, mock_client, periodical_task, client_factory
):
    mock_client.create_task.side_effect = Exception()

    task = loop.create_task(periodical_task(Mock()).run(client_factory))

    await asyncio.sleep(0.1)

    assert mock_client.create_task.call_count > 1

    task.cancel()


async def test_rerun_on_task_update_error(
    loop, mock_client, periodical_task, client_factory
):
    mock_client.update_task.side_effect = Exception()
    handler = coro_mock()

    task = loop.create_task(periodical_task(handler).run(client_factory))

    await asyncio.sleep(0.1)

    assert handler.call_count > 1

    task.cancel()
