import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_data_manager(events_domain, events_dm):

    await events_domain.retrieve_campaigns_events_by_orders(
        order_ids=[1, 2],
        manul_order_ids=[3, 4],
        time_range={
            "from_timestamp": dt("2020-05-01 00:00:00"),
            "to_timestamp": dt("2020-05-31 00:00:00"),
        },
        starting_event_id=1,
        limit=10,
    )

    events_dm.retrieve_campaigns_events_by_orders.assert_called_with(
        [1, 2], [3, 4], dt("2020-05-01 00:00:00"), dt("2020-05-31 00:00:00"), 1, 10
    )


async def test_returns_events(events_domain, events_dm):
    events_dm.retrieve_campaigns_events_by_orders.coro.return_value = [
        {
            "id": 1,
            "timestamp": dt("2020-05-01 00:00:00"),
            "campaign_id": 1,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {"field": "value"},
        },
        {
            "id": 2,
            "timestamp": dt("2020-05-01 00:00:00"),
            "campaign_id": 2,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {"field": "value"},
        },
    ]

    result = await events_domain.retrieve_campaigns_events_by_orders(
        order_ids=[1, 2],
        manul_order_ids=[3, 4],
        time_range={
            "from_timestamp": dt("2020-05-01 00:00:00"),
            "to_timestamp": dt("2020-05-31 00:00:00"),
        },
        starting_event_id=1,
        limit=10,
    )

    assert result == [
        {
            "id": 1,
            "timestamp": dt("2020-05-01 00:00:00"),
            "campaign_id": 1,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {"field": "value"},
        },
        {
            "id": 2,
            "timestamp": dt("2020-05-01 00:00:00"),
            "campaign_id": 2,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {"field": "value"},
        },
    ]
