import pytest

from maps_adv.geosmb.promoter.client import PromoterClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("promoter.server", *a)


@pytest.fixture
def mock_remove_leads_for_gdpr(rmock):
    return lambda h: rmock("/internal/v1/remove_leads_for_gdpr/", "POST", h)


@pytest.fixture
def mock_search_leads_for_gdpr(rmock):
    return lambda h: rmock("/internal/v1/search_leads_for_gdpr/", "POST", h)


@pytest.fixture()
async def client(aiotvm):
    async with PromoterClient(
        url="http://promoter.server", tvm=aiotvm, tvm_destination="promoter"
    ) as _client:
        yield _client
