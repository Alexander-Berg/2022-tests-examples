import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_telegram_users(dm, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await dm.fetch_telegram_users(user_logins=['test'])

    assert result == [
        dict(
            user_id=123,
            user_login='test',
        )
    ]


async def test_returns_empty_list_if_no_telegram_users(dm):
    result = await dm.fetch_telegram_users(user_logins=['test'])

    assert result == []


async def test_does_not_return_other_telegram_users(dm, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)
    await factory.create_telegram_user(user_login='random', user_id=456)

    result = await dm.fetch_telegram_users(user_logins=['test'])

    assert result == [
        dict(
            user_id=123,
            user_login='test',
        )
    ]
