import pytest

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def rmock(aresponses):
    return lambda *a: aresponses.add("billing_proxy.server", *a)


@pytest.fixture
def mock_fetch_orders_balance_api(rmock):
    return lambda h: rmock("/orders/stats/", "POST", h)


@pytest.fixture
def mock_fetch_orders_discounts_api(rmock):
    return lambda h: rmock("/orders/discounts/", "POST", h)


@pytest.fixture
def mock_submit_orders_charges_api(rmock):
    return lambda h: rmock("/orders/charge/", "POST", h)


@pytest.fixture
def mock_fetch_order_api(rmock):
    return lambda h: rmock("/orders/1/", "GET", h)


@pytest.fixture
def mock_fetch_active_orders_api(rmock):
    return lambda h: rmock("/orders/active/", "POST", h)


@pytest.fixture
def mock_calculate_cpm(rmock):
    return lambda h: rmock("/products/1/cpm/", "POST", h)


@pytest.fixture
def mock_fetch_orders_debits_api(rmock):
    return lambda h: rmock("/orders/debits/", "POST", h)
