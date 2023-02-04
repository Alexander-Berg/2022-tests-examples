import pytest

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def rmock(aresponses):
    return lambda *a: aresponses.add("adv_store.server", *a)


@pytest.fixture
def mock_charger_cpm_api(rmock):
    return lambda h: rmock("/v2/campaigns/charger/cpm/", "GET", h)


@pytest.fixture
def mock_charger_cpa_api(rmock):
    return lambda h: rmock("/v2/campaigns/charger/cpa/", "GET", h)


@pytest.fixture
def mock_charger_fix_api(rmock):
    return lambda h: rmock("/v2/campaigns/charger/fix/", "GET", h)


@pytest.fixture
def mock_export(rmock):
    return lambda h: rmock("/v2/campaigns/export/", "GET", h)


@pytest.fixture
def mock_stop_campaigns_api(rmock):
    return lambda h: rmock("/v2/campaigns/stop/", "PUT", h)


@pytest.fixture
def mock_create_campaign_events_api(rmock):
    return lambda h: rmock("/v2/create-campaign-events/", "POST", h)


@pytest.fixture
def mock_list_campaigns_for_budget_analysis(rmock):
    return lambda h: rmock("/v2/campaigns/budget-analysis/", "GET", h)


@pytest.fixture
def mock_retrieve_campaign_data_for_monitorings(rmock):
    return lambda h: rmock("/v2/campaigns/monitoring-info/", "POST", h)


@pytest.fixture
def mock_set_paid_till_api(rmock):
    return lambda campaign_id, h: rmock(
        f"/v2/campaigns/{campaign_id}/paid-till/", "PUT", h
    )
