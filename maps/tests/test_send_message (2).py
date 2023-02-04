import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_response(aresponses):
    return lambda *args: aresponses.add("notify_me_test.server", "/api/send-message", "POST", *args)


async def test_sends_correct_request(client, mock_response):
    request_url = None

    async def handler(request):
        nonlocal request_url
        request_url = str(request.url)
        return json_response({"key": "value"})

    mock_response(handler)

    await client.send_message(telegram_uid="496329590", text="Client_test_message")

    assert request_url == "http://notify_me_test.server/api/send-message"


async def test_returns_message(client, mock_response):
    mock_response(json_response({"key": "value"}))

    got = await client.send_message(telegram_uid="496329590", text="Client_test_message")

    assert got == {"key": "value"}


async def test_raises_on_error(client, mock_response):
    mock_response(Response(status=400, body=b"Error description"))

    with pytest.raises(UnknownResponse):
        await client.send_message(telegram_uid="496329590", text="Client_test_message")
