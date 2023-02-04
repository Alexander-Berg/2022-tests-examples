from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_calls_dm_and_returns_data(campaigns_domain, campaigns_dm):
    campaigns_dm.list_campaigns_for_charger_cpa.coro.return_value = [
        {
            "campaign_id": 1,
            "campaign_type": CampaignTypeEnum.VIA_POINTS,
            "order_id": 10,
            "cost": Decimal("50"),
            "budget": Decimal("66000"),
            "daily_budget": Decimal("5000"),
            "timezone": "UTC",
        },
        {
            "campaign_id": 2,
            "campaign_type": CampaignTypeEnum.VIA_POINTS,
            "order_id": 20,
            "cost": Decimal("150"),
            "budget": Decimal("11000"),
            "daily_budget": Decimal("1000"),
            "timezone": "Moscow",
        },
    ]

    got = await campaigns_domain.list_campaigns_for_charger_cpa(
        dt("2020-09-10 00:00:00")
    )

    campaigns_dm.list_campaigns_for_charger_cpa.assert_called_with(
        dt("2020-09-10 00:00:00")
    )

    assert got == [
        {
            "campaign_id": 1,
            "order_id": 10,
            "cost": Decimal("50"),
            "budget": Decimal("66000"),
            "daily_budget": Decimal("5000"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_MAKE_ROUTE",
            ],
        },
        {
            "campaign_id": 2,
            "order_id": 20,
            "cost": Decimal("150"),
            "budget": Decimal("11000"),
            "daily_budget": Decimal("1000"),
            "timezone": "Moscow",
            "paid_events_names": [
                "ACTION_MAKE_ROUTE",
            ],
        },
    ]
