import pytest

from maps_adv.geosmb.clients.bvm import BvmClient

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def mock_bvm_fetch_biz_id(aresponses):
    return lambda h: aresponses.add("bvm.server", "/v1/fetch_biz_id", "POST", h)


@pytest.fixture
def mock_bvm_fetch_no_create_biz_id(aresponses):
    return lambda h: aresponses.add(
        "bvm.server", "/v1/fetch_no_create_biz_id", "POST", h
    )


@pytest.fixture
def mock_bvm_fetch_permalinks_by_biz_id(aresponses):
    return lambda h: aresponses.add("bvm.server", "/v1/fetch_permalinks", "POST", h)


@pytest.fixture
def mock_bvm_fetch_permalinks_by_biz_ids(aresponses):
    return lambda h: aresponses.add(
        "bvm.server", "/v1/fetch_approximate_permalinks_for_biz_ids", "POST", h
    )


@pytest.fixture
async def bvm_client():
    async with BvmClient(url="http://bvm.server") as client:
        yield client
