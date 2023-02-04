import pytest
from aiohttp.web import json_response

from maps_adv.common.blackbox import BlackboxClientException

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_response(aresponses):
    return lambda *args: aresponses.add("blackbox.api", "/blackbox", "GET", *args)


@pytest.mark.parametrize("ssl_session_id", [None, "def"])
async def test_sends_correct_request(client, mock_response, ssl_session_id):
    request_url = None

    async def handler(request):
        nonlocal request_url
        request_url = str(request.url)
        return json_response({"status": {"value": "VALID"}})

    mock_response(handler)

    await client.sessionid(
        user_ip="1.2.3.4",
        session_id="abc",
        ssl_session_id=ssl_session_id,
        get_user_ticket="yes",
    )

    assert request_url == (
        "http://blackbox.api/blackbox?method=sessionid&userip=1.2.3.4&host=session.host"
        "&sessionid=abc&format=json&get_user_ticket=yes"
        + ("&sslsessionid=def" if ssl_session_id == "def" else "")
    )


@pytest.mark.parametrize("status", ["VALID", "NEED_RESET"])
async def test_returns_data_on_valid_status(client, mock_response, status):
    expected_data = {"status": {"value": status}, "user_ticket": "abc"}
    mock_response(json_response(expected_data))

    data = await client.sessionid(user_ip="1.2.3.4", session_id="abc")

    assert data == expected_data


@pytest.mark.parametrize("status", ["EXPIRED", "NOAUTH", "DISABLED", "INVALID"])
async def test_raises_on_bad_status(client, mock_response, status):
    mock_response(json_response({"status": {"value": status}, "user_ticket": "abc"}))

    with pytest.raises(BlackboxClientException):
        await client.sessionid(user_ip="1.2.3.4", session_id="abc")
