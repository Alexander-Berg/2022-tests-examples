from decimal import Decimal

from maps_adv.stat_tasks_starter.lib.not_spending_budget.analyzer import (
    get_campaigns_not_spending_budget,
)


def test_finds_campaigns_not_spending_budget():
    campaign_data = [
        {
            "campaign_id": 1,
            "daily_budget": Decimal(10),
            "days_left": 3,
            "budget": Decimal(100),
            "charged": Decimal(50),
        }
    ]

    result = get_campaigns_not_spending_budget(campaign_data)

    assert result == [1]


def test_does_not_return_campaigns_spending_budget():
    campaign_data = [
        {
            "campaign_id": 1,
            "daily_budget": Decimal(10),
            "days_left": 3,
            "budget": Decimal(100),
            "charged": Decimal(50),
        },
        {
            "campaign_id": 2,
            "daily_budget": Decimal(10),
            "days_left": 10,
            "budget": Decimal(100),
            "charged": Decimal(50),
        },
    ]

    result = get_campaigns_not_spending_budget(campaign_data)

    assert result == [1]


def test_does_not_return_campaigns_with_infinite_day_budget():
    campaign_data = [
        {
            "campaign_id": 1,
            "daily_budget": Decimal("Inf"),
            "days_left": 3,
            "budget": Decimal(100),
            "charged": Decimal(50),
        }
    ]

    result = get_campaigns_not_spending_budget(campaign_data)

    assert result == []
