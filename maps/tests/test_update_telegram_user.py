import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]


def make_response(**overrides) -> dict:
    res = dict(
        user_login='random',
        user_id=123
    )
    res.update(overrides)
    return res


async def test_sends_correct_request(
    client,
    mock_update_telegram_user,
):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.json()
        return json_response(make_response(), status=200)

    mock_update_telegram_user(_handler)

    await client.update_telegram_user(
        user_id=123,
        user_login="random"
    )

    assert request_path == "/v2/update_telegram_user/"
    assert request_body == dict(
        user_id=123,
        user_login="random"
    )


async def test_returns_updated_user(client, mock_update_telegram_user):
    mock_update_telegram_user(lambda _: json_response(make_response(), status=200))

    got = await client.update_telegram_user(
        user_id=123,
        user_login="random"
    )

    assert got == {
        "user_id": 123,
        "user_login": "random"
    }


async def test_raises_for_unknown_exceptions(client, mock_update_telegram_user):
    mock_update_telegram_user(lambda _: Response(status=409))

    with pytest.raises(UnknownResponse):
        await client.update_telegram_user(user_id=123, user_login='random')
