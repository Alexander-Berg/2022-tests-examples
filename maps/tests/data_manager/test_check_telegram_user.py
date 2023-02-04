import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_if_telegram_user_exist(dm, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await dm.check_telegram_user(user_login='test')

    assert result is True


async def test_returns_false_if_other_user(dm, factory):
    await factory.create_telegram_user(user_login='test', user_id=123)

    result = await dm.check_telegram_user(user_login='random')

    assert result is False


async def test_returns_false_if_permission_do_not_exist(dm):
    result = await dm.check_telegram_user(user_login='test')

    assert result is False
