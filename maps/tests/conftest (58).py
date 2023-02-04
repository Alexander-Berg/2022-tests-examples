import pytest

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def rmock(aresponses):
    return lambda *a: aresponses.add("dashboard.server", *a)


@pytest.fixture
def mock_charged_sum_api(rmock):
    return lambda h: rmock("/statistics/campaigns/charged_sum/", "POST", h)


@pytest.fixture
def mock_displays_api(rmock):
    return lambda h: rmock("/statistics/campaigns/events/", "POST", h)
