import pytest

from maps_adv.common.blackbox import BlackboxClientException

pytestmark = [pytest.mark.asyncio]

URL = "/call"


async def test_returns_audio_file_if_authorized(api):
    response = await api.get(
        URL,
        headers={
            "X-Forwarded-For-Y": "1.2.3.4",
            "Cookie": "Session_id=abc",
        },
        expected_status=200,
        as_response=True,
    )

    assert response.headers["Content-Type"] == "audio/mpeg"
    with open("data/sample.mp3", "rb") as audio_file:
        assert await response.read() == audio_file.read()


async def test_calls_blackbox_api(api, blackbox_client):
    await api.get(
        URL,
        headers={
            "X-Forwarded-For-Y": "1.2.3.4",
            "Cookie": "Session_id=abc; sessionid2=def",
        },
        expected_status=200,
    )

    blackbox_client.sessionid.assert_called_with(
        user_ip="1.2.3.4",
        session_id="abc",
        ssl_session_id="def",
        get_user_ticket="yes",
    )


async def test_returns_403_if_no_session_id(api):
    await api.get(
        URL,
        headers={"X-Forwarded-For-Y": "1.2.3.4"},
        expected_status=403,
    )


async def test_returns_403_if_not_authorized(api, blackbox_client):
    blackbox_client.sessionid.side_effect = BlackboxClientException

    await api.get(
        URL,
        headers={
            "X-Forwarded-For-Y": "1.2.3.4",
            "Cookie": "Session_id=abc; sessionid2=def",
        },
        expected_status=403,
    )
