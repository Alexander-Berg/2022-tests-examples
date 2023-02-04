import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_will_call_data_manager(campaigns_domain, campaigns_dm):
    await campaigns_domain.backup_campaigns_change_log()

    assert campaigns_dm.backup_campaigns_change_log.called
