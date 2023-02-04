import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_response(aresponses):
    return lambda *args: aresponses.add("bunker.server", "/v1/cat", "GET", *args)


async def test_sends_correct_request(client, mock_response):
    request_url = None

    async def handler(request):
        nonlocal request_url
        request_url = str(request.url)
        return json_response({"key": "value"})

    mock_response(handler)

    await client.get_node_content(node="/a/b/c", version="latest")

    assert request_url == "http://bunker.server/v1/cat?node=/a/b/c&version=latest"


async def test_returns_node_content(client, mock_response):
    mock_response(json_response({"key": "value"}))

    got = await client.get_node_content("/a/b/c")

    assert got == {"key": "value"}


async def test_raises_on_error(client, mock_response):
    mock_response(Response(status=400, body=b"Error description"))

    with pytest.raises(UnknownResponse):
        await client.get_node_content("/a/b/c")
