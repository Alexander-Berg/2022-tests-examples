import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_dm_data(domain, dm):
    dm.fetch_telegram_users.coro.return_value = [
        dict(
            user_login='test',
            user_id=123
        )
    ]

    got = await domain.fetch_telegram_users(user_logins=['test'])

    assert got == dict(
        users=[
            dict(
                user_login='test',
                user_id=123
            )
        ]
    )
