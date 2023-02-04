import pytest

from aiohttp.web import Response

pytestmark = [pytest.mark.asyncio]


async def test_returns_with_context(client, mock_request):
    mock_request(Response(body=b"", status=200))
    async with client as client:
        got = await client.request()
    assert got == b""


async def test_returns_without_context(client, mock_request):
    mock_request(Response(body=b"", status=200))
    got = await client.request()
    assert got == b""
