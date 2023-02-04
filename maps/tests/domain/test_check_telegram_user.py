import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_requests_dm_correctly(domain, dm):
    await domain.check_telegram_user(
        user_login="test"
    )

    dm.check_telegram_user.assert_called_with(
        user_login="test"
    )
