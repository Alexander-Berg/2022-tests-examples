from datetime import datetime, timezone

import pytest
from aiohttp.web import json_response

from maps_adv.geosmb.booking_yang.server.lib.clients import YangClient

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def client():
    async with YangClient(
        url="https://yang.server", token="keken", pool_id="123"
    ) as _client:
        yield _client


@pytest.fixture
def mock_create_task_suites(aresponses):
    return lambda *a: aresponses.add("yang.server", "/api/v1/task-suites", "POST", *a)


RESPONSE = {
    "id": "63614047-38c3-4ad4-8a86-99c5c651a9b8",
    "pool_id": "1",
    "tasks": [],
    "some other params": "any values for this params",
    "created": "2016-04-18T12:43:04.988",
}


async def test_sends_correct_request(client, mock_create_task_suites):
    request_url = None
    request_body = None
    request_headers = None

    async def _handler(request):
        nonlocal request_url, request_body, request_headers
        request_url = str(request.url)
        request_body = await request.json()
        request_headers = request.headers
        return json_response(status=201, data=RESPONSE)

    mock_create_task_suites(_handler)

    await client.create_task_suite({"any": "valid dict"})

    assert request_url == "http://yang.server/api/v1/task-suites"
    assert request_headers["Authorization"] == "OAuth keken"
    assert request_body == {
        "pool_id": "123",
        "overlap": 1,
        "tasks": [{"input_values": {"any": "valid dict"}}],
    }


async def test_returns_task_id_and_created_date(client, mock_create_task_suites):
    mock_create_task_suites(json_response(status=201, data=RESPONSE))

    got = await client.create_task_suite({"any": "valid dict"})

    assert got == dict(
        id="63614047-38c3-4ad4-8a86-99c5c651a9b8",
        created_at=datetime(2016, 4, 18, 12, 43, 4, 988000, tzinfo=timezone.utc),
    )
