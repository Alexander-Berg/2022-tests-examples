import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_data_manager(moderation_domain, moderation_dm):
    await moderation_domain.list_campaigns()

    assert moderation_dm.list_campaigns.called


async def test_returns_data_from_data_manager(moderation_domain, moderation_dm):
    expected_result = [{"id": 1, "key": "value"}, {"id": 2, "key": "value"}]
    moderation_dm.list_campaigns.coro.return_value = expected_result

    result = await moderation_domain.list_campaigns()

    assert result == expected_result
