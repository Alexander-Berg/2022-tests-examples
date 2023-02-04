import pytest

pytestmark = [pytest.mark.asyncio]

URL = "/v2/update_telegram_user/"


async def test_returns_created_user_if_none(api):
    result = await api.post(
        URL,
        json=dict(
            user_login='test',
            user_id=123
        ),
        expected_status=200,
    )

    assert result == dict(
        user_login='test',
        user_id=123
    )


async def test_returns_updated_user(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await api.post(
        URL,
        json=dict(
            user_login='random',
            user_id=123
        ),
        expected_status=200,
    )

    assert result == dict(
        user_login='random',
        user_id=123
    )


async def test_updates_data_in_db(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    await api.post(
        URL,
        json=dict(
            user_login='random',
            user_id=123
        ),
        expected_status=200,
    )

    result = await factory.fetch_telegram_users(user_logins=['random'])

    assert result == [
        dict(
            active=True,
            user_login='random',
            user_id=123
        )
    ]
