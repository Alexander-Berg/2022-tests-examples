import pytest

from maps_adv.geosmb.tuner.proto.telegram_pb2 import (
    FetchTelegramUsersInput,
    FetchTelegramUsersOutput
)

pytestmark = [pytest.mark.asyncio]

URL = "/v2/fetch_telegram_users/"


async def test_returns_telegram_users(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await api.post(
        URL,
        proto=FetchTelegramUsersInput(user_logins=['test']),
        decode_as=FetchTelegramUsersOutput,
        expected_status=200,
    )

    assert result == FetchTelegramUsersOutput(
        users=[
            dict(
                user_login='test',
                user_id=123,
            )
        ],
    )


async def test_returns_empty_array_if_no_data(api):
    result = await api.post(
        URL,
        proto=FetchTelegramUsersInput(user_logins=['test']),
        decode_as=FetchTelegramUsersOutput,
        expected_status=200,
    )

    assert result == FetchTelegramUsersOutput(
        users=[]
    )


async def test_does_not_return_other_logins(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)
    await factory.create_telegram_user(user_login='random', user_id=456)

    result = await api.post(
        URL,
        proto=FetchTelegramUsersInput(user_logins=['test']),
        decode_as=FetchTelegramUsersOutput,
        expected_status=200,
    )

    assert result == FetchTelegramUsersOutput(
        users=[
            dict(
                user_login='test',
                user_id=123,
            )
        ],
    )
