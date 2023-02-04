import pytest

from maps_adv.geosmb.booking_yang.client import BookingYangClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("booking-yang.server", *a)


@pytest.fixture
def mock_fetch_actual_orders(rmock):
    return lambda h: rmock("/v1/fetch_actual_orders/", "POST", h)


@pytest.fixture
def mock_clear_orders_gdpr(rmock):
    return lambda h: rmock("/internal/v1/clear_orders_for_gdpr/", "POST", h)


@pytest.fixture
def mock_search_orders_gdpr(rmock):
    return lambda h: rmock("/internal/v1/search_orders_for_gdpr/", "POST", h)


@pytest.fixture()
async def client(aiotvm):
    async with BookingYangClient(
        url="http://booking-yang.server", tvm=aiotvm, tvm_destination="booking_yang"
    ) as client:
        yield client
