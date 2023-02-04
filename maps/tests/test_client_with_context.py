from unittest import mock

import pytest

from maps_adv.warden.client.lib import UnknownTaskOrType
from maps_adv.warden.client.lib.client import ClientWithContextManager

pytestmark = [pytest.mark.asyncio]


async def test_update_task_with_completed_status_after_success_finished_context(
    mock_client
):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    async with ClientWithContextManager(mock_client):
        pass

    mock_client.update_task.coro.assert_called_once_with(
        task_id=1, status="completed", metadata=None
    )


async def test_will_fail_task_if_raises_in_acquired_context(mock_client):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    class SomeException(Exception):
        pass

    with pytest.raises(SomeException) as e:
        async with ClientWithContextManager(mock_client):
            raise SomeException("test exception")

    assert str(e.value) == "test exception"
    mock_client.update_task.coro.assert_called_once_with(
        task_id=1,
        status="failed",
        metadata={
            "exception_type": "SomeException",
            "exception_value": "test exception",
        },
    )


async def test_no_raises_exception_in_finished_context_if_task_was_completed_in_context(
    mock_client
):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    async with ClientWithContextManager(mock_client) as c:
        await c.completed()


async def test_will_create_task_through_client(mock_client):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    async with ClientWithContextManager(mock_client):
        pass

    mock_client.create_task.coro.assert_called_once_with()


async def test_will_update_task_to_completed(mock_client):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    client = ClientWithContextManager(mock_client)

    await client.__aenter__()
    await client.completed()

    mock_client.update_task.coro.assert_called_once_with(
        task_id=1, status="completed", metadata=None
    )


async def test_will_update_task_to_failed(mock_client):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    client = ClientWithContextManager(mock_client)

    await client.__aenter__()
    await client.failed(metadata={"test": "meta"})

    mock_client.update_task.coro.assert_called_once_with(
        task_id=1, status="failed", metadata={"test": "meta"}
    )


@pytest.mark.parametrize(
    ["method_name", "params"],
    [("completed", {}), ("failed", {}), ("update_status", {"status": "status"})],
)
async def test_raises_when_trying_to_update_non_created_task(
    method_name, params, mock_client
):
    with pytest.raises(UnknownTaskOrType):
        client = ClientWithContextManager(mock_client)
        await getattr(client, method_name)(**params)


async def test_will_update_task_to_all_requested_statuses(mock_client):
    mock_client.create_task.coro.return_value = {"task_id": 1, "time_limit": 10}

    async with ClientWithContextManager(mock_client) as client:
        await client.update_status("status1", {})
        await client.update_status("status2", None)

    calls = [
        mock.call(task_id=1, status="status1", metadata={}),
        mock.call(task_id=1, status="status2", metadata=None),
        mock.call(task_id=1, status="completed", metadata=None),
    ]
    mock_client.update_task.coro.assert_has_calls(calls, any_order=False)
    assert mock_client.update_task.coro.call_count == len(calls)
