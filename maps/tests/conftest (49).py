import pytest

from maps_adv.geosmb.tuner.client import TunerClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def mock_fetch_settings(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v1/fetch_settings/", "POST", h)


@pytest.fixture
async def mock_update_settings(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v1/update_settings/", "POST", h)


@pytest.fixture
async def mock_fetch_settings_v2(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v2/fetch_settings/", "POST", h)


@pytest.fixture
async def mock_update_settings_v2(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v2/update_settings/", "POST", h)


@pytest.fixture
async def mock_update_telegram_user(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v2/update_telegram_user/", "POST", h)


@pytest.fixture
async def mock_delete_telegram_user(aresponses):
    return lambda h: aresponses.add("tuner.server", "/v2/delete_telegram_user/", "POST", h)


@pytest.fixture
async def client(aiotvm):
    async with await TunerClient(
        url="http://tuner.server", tvm=aiotvm, tvm_destination="tuner"
    ) as client:
        yield client
