import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_created_user(dm, factory):
    result = await dm.update_telegram_user(
        user_id=123,
        user_login='test'
    )

    assert result == dict(
        user_id=123,
        user_login='test'
    )


async def test_returns_updated_user(dm, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await dm.update_telegram_user(
        user_id=123,
        user_login='random'
    )

    assert result == dict(
        user_id=123,
        user_login='random'
    )
