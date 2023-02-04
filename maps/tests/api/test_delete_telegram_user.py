import pytest

pytestmark = [pytest.mark.asyncio]

URL = "/v2/delete_telegram_user/"


async def test_returns_valid_status(api):
    await api.post(
        URL,
        json=dict(
            user_id=123
        ),
        expected_status=200,
    )


async def test_updates_status_in_db(api, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    await api.post(
        URL,
        json=dict(
            user_id=123
        ),
        expected_status=200,
    )

    result = await factory.fetch_telegram_users(user_logins=['test'])

    assert result == [
        dict(
            active=False,
            user_login='test',
            user_id=123
        )
    ]
