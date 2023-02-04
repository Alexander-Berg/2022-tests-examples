import pytest

from maps_adv.geosmb.tuner.proto.telegram_pb2 import (
    CheckTelegramUserInput,
    CheckTelegramUserOutput,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v2/check_telegram_user/"


async def test_returns_is_authorized_check_true(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await api.post(
        URL,
        proto=CheckTelegramUserInput(user_login='test'),
        decode_as=CheckTelegramUserOutput,
        expected_status=200,
    )

    assert result == CheckTelegramUserOutput(
        is_authorized=True
    )


async def test_returns_not_authorized_check_false(api, factory):
    result = await api.post(
        URL,
        proto=CheckTelegramUserInput(user_login='test'),
        decode_as=CheckTelegramUserOutput,
        expected_status=200,
    )

    assert result == CheckTelegramUserOutput(
        is_authorized=False
    )


async def test_returns_other_user_auth_check_false(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await api.post(
        URL,
        proto=CheckTelegramUserInput(user_login='random'),
        decode_as=CheckTelegramUserOutput,
        expected_status=200,
    )

    assert result == CheckTelegramUserOutput(
        is_authorized=False
    )
