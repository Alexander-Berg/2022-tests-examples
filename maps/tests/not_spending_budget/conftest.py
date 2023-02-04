import pytest


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("somedomain.com", *a)


@pytest.fixture
def adv_store_create_campaign_events_rmock(rmock):
    return lambda h: rmock("/v2/create-campaign-events/", "POST", h)


@pytest.fixture
def adv_store_list_campaigns_for_budget_analysis_rmock(rmock):
    return lambda h: rmock("/v2/campaigns/budget-analysis/", "GET", h)
