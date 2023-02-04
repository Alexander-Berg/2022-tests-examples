import asyncio

import pytest
from aiohttp.web import json_response

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def update_rmock(rmock):
    return lambda h: rmock("/tasks/normalizer/100/", "put", h)


async def test_shield_request_from_cancel(normalizer_client, update_rmock):
    is_server_receives_request = False

    async def _handler(request):
        nonlocal is_server_receives_request
        is_server_receives_request = True
        await asyncio.sleep(0.2)
        return json_response({})

    update_rmock(_handler)

    task = asyncio.create_task(
        normalizer_client._request(
            "PUT",
            "/tasks/normalizer/100/",
            200,
            {"task_id": 1, "status": "completed", "executor_id": "abc"},
        )
    )
    shield = asyncio.shield(task)

    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(shield, timeout=0.1)

    await task

    assert shield.cancelled()
    assert not task.cancelled()
    assert is_server_receives_request
