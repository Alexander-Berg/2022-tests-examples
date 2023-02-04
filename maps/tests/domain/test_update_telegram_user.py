import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_updated_data(domain, dm):
    dm.update_telegram_user.coro.return_value = dict(
        user_login='test',
        user_id=123,
    )

    got = await domain.update_telegram_user(
        user_login='test',
        user_id=123,
    )

    assert got == dict(
        user_login='test',
        user_id=123,
    )


async def test_requests_dm_correctly(domain, dm):
    await domain.update_telegram_user(
        user_login='test',
        user_id=123,
    )

    dm.update_telegram_user.assert_called_with(
        user_login='test',
        user_id=123,
    )
