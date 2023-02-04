from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_adv_store_client
async def test_no_events_if_no_campaigns(adv_store_client, domain, fill_ch):
    adv_store_client.list_campaigns_for_budget_analysis.coro.return_value = []

    await domain.check_not_spending_budget()

    assert adv_store_client.create_campaign_not_spending_budget_events.call_count == 0


@pytest.mark.mock_adv_store_client
async def test_creates_events_for_campaigns_not_spending_budget(
    adv_store_client, domain, fill_ch
):
    adv_store_client.list_campaigns_for_budget_analysis.coro.return_value = [
        dict(
            campaign_id=10,
            budget=Decimal("802"),
            daily_budget=Decimal("200"),
            days_left=4,
        ),
        dict(
            campaign_id=20,
            budget=Decimal("1505.0001"),
            daily_budget=Decimal("300"),
            days_left=5,
        ),
        dict(
            campaign_id=30,
            budget=Decimal("300.38"),
            daily_budget=Decimal("300"),
            days_left=1,
        ),
        dict(
            campaign_id=2020,
            budget=Decimal("500.1"),
            daily_budget=Decimal("100"),
            days_left=5,
        ),
    ]

    await domain.check_not_spending_budget()

    create_event = adv_store_client.create_campaign_not_spending_budget_events
    assert create_event.call_args[0][0] == [10, 20, 30, 2020]
    assert create_event.call_count == 1


@pytest.mark.mock_adv_store_client
async def test_does_not_creates_events_for_campaigns_spending_budget(
    adv_store_client, domain
):
    adv_store_client.list_campaigns_for_budget_analysis.coro.return_value = [
        dict(
            campaign_id=10,
            budget=Decimal("300"),
            daily_budget=Decimal("100"),
            days_left=3,
        ),
        dict(
            campaign_id=20,
            budget=Decimal("505"),
            daily_budget=Decimal("100"),
            days_left=5,
        ),
        dict(
            campaign_id=2020,
            budget=Decimal("500"),
            daily_budget=Decimal("100"),
            days_left=5,
        ),
    ]

    await domain.check_not_spending_budget()

    create_event = adv_store_client.create_campaign_not_spending_budget_events
    assert create_event.call_count == 0
