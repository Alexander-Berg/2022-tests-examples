import pytest

from maps_adv.geosmb.clients.market import MarketIntClient

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def mock_filter_services(aresponses):
    return lambda h: aresponses.add("market.server", "/v1/filter_services", "POST", h)


@pytest.fixture
def mock_filter_service_categories(aresponses):
    return lambda h: aresponses.add(
        "market.server", "/v1/filter_service_categories", "POST", h
    )


@pytest.fixture
def mock_fetch_actual_bookings(aresponses):
    return lambda h: aresponses.add(
        "market.server", "/v1/fetch_actual_orders", "POST", h
    )


@pytest.fixture
async def market_client():
    async with MarketIntClient(url="http://market.server") as client:
        yield client
