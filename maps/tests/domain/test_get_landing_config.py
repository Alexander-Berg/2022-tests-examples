import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import InvalidFetchToken

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_data(domain, dm):
    dm.fetch_cached_landing_config.coro.return_value = {"k": "v"}

    result = await domain.get_landing_config(token="fetch_data_token")

    assert result == {"k": "v"}


async def test_raises_if_token_is_invalid(domain):
    with pytest.raises(InvalidFetchToken):
        await domain.get_landing_config(token="BAD_TOKEN")
