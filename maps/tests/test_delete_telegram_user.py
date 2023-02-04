import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]


request_params = dict()


async def test_sends_correct_request(
    client,
    mock_delete_telegram_user,
):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.json()
        return Response(status=200)

    mock_delete_telegram_user(_handler)

    await client.delete_telegram_user(
        user_id=123
    )

    assert request_path == "/v2/delete_telegram_user/"
    assert request_body == dict(
        user_id=123
    )


async def test_raises_for_unknown_exceptions(client, mock_delete_telegram_user):
    mock_delete_telegram_user(lambda _: Response(status=409))

    with pytest.raises(UnknownResponse):
        await client.delete_telegram_user(user_id=123)
