import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_requests_dm_correctly(domain, dm):
    await domain.delete_telegram_user(
        user_id=123,
    )

    dm.delete_telegram_user.assert_called_with(
        user_id=123,
    )
