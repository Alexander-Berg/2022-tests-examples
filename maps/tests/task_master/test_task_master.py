import asyncio
import logging
from unittest.mock import Mock

import pytest
from aiohttp.web import Response

from maps_adv.warden.client.lib import TaskMaster
from maps_adv.warden.proto.tasks_pb2 import TaskDetails, UpdateTaskOutput

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_create_task(rmock):
    return lambda h: rmock("/tasks/", "POST", h)


@pytest.fixture
def mock_update_task(rmock):
    return lambda h: rmock("/tasks/", "PUT", h)


async def test_goes_through_full_pipeline_for_periodical_task(
    mock_create_task, mock_update_task, caplog, periodical_task
):
    caplog.clear()
    caplog.set_level(logging.WARNING)

    lock = asyncio.Lock()
    await lock.acquire()

    created_task_data = TaskDetails(
        task_id=1, status="accepted", time_limit=300, metadata='{"some": "json"}'
    )

    async def handler_update(_):
        lock.release()
        return Response(body=UpdateTaskOutput().SerializeToString(), status=204)

    mock_create_task(
        Response(body=created_task_data.SerializeToString(), status=201)
    )
    mock_update_task(handler_update)

    tm = TaskMaster(
        "http://warden.server",
        tasks=[periodical_task(Mock(side_effect=[asyncio.sleep(0)]))],
    )

    await tm.run()
    await asyncio.wait_for(lock.acquire(), timeout=1)
    await tm.stop()

    assert caplog.messages == []
