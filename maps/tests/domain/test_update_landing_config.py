import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_bunker_client(domain, dm, bunker_client):
    await domain.update_landing_config()

    bunker_client.get_node_content.assert_called_with("/landlord/config", "latest")


async def test_uses_dm(domain, dm):
    await domain.update_landing_config()

    dm.set_cached_landing_config.coro.assert_called_with({"key": "value"})
