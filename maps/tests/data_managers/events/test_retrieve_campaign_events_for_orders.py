import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_no_events(factory, events_dm, con):
    result = await events_dm.retrieve_campaigns_events_by_orders()

    assert result == []


async def test_returns_correct_event_data(factory, events_dm, con):
    campaign_id = (await factory.create_campaign(order_id=1, name="Test campaign"))[
        "id"
    ]

    event = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:00"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={"field": "value"},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(order_ids=[1])

    assert result == [
        {
            "id": event["id"],
            "timestamp": dt("2020-05-01 00:00:00"),
            "campaign_id": campaign_id,
            "campaign_name": "Test campaign",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {"field": "value"},
        }
    ]


async def test_returns_events_for_orders(factory, events_dm, con):
    campaign_1_id = (await factory.create_campaign(order_id=1))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=2))["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(order_ids=[1, 2])

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_2_id,
            "campaign_name": "campaign0",
            "billing_order_id": 2,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_events_for_manul_orders(factory, events_dm, con):
    campaign_1_id = (await factory.create_campaign(manul_order_id=2))["id"]
    campaign_2_id = (await factory.create_campaign(manul_order_id=3))["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(manul_order_ids=[2, 3])

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_2_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 3,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_events_for_both_types_of_orders(factory, events_dm, con):
    campaign_1_id = (await factory.create_campaign(order_id=1))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=3))["id"]
    campaign_3_id = (await factory.create_campaign(manul_order_id=2))["id"]
    campaign_4_id = (await factory.create_campaign(manul_order_id=4))["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_3_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )
    event4 = await factory.create_campaign_event(
        campaign_id=campaign_4_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1, 3], manul_order_ids=[2, 4]
    )

    assert result == [
        {
            "id": event4["id"],
            "timestamp": event4["timestamp"],
            "campaign_id": campaign_4_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 4,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_3_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_2_id,
            "campaign_name": "campaign0",
            "billing_order_id": 3,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_does_not_returns_events_for_other_orders(factory, events_dm, con):

    campaign_1_id = (await factory.create_campaign(order_id=1))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=3))["id"]
    campaign_3_id = (await factory.create_campaign(manul_order_id=2))["id"]
    campaign_4_id = (await factory.create_campaign(manul_order_id=4))["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_3_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )
    await factory.create_campaign_event(
        campaign_id=campaign_4_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1], manul_order_ids=[2]
    )

    assert result == [
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_3_id,
            "campaign_name": "campaign0",
            "billing_order_id": None,
            "manul_order_id": 2,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_1_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_limited_number_of_events(factory, events_dm, con):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for _ in range(20):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
        )

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(order_ids=[1], limit=3)

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_limited_number_of_events_starting_with_specified(
    factory, events_dm, con
):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for _ in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
        )

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )
    event4 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    for _ in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
        )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1],
        starting_event_id=event4["id"],  # should not be included into result
        limit=3,
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_events_from_time_range(factory, events_dm, con):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:30"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:31"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:33"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    for i in range(40, 50):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1],
        from_timestamp=dt("2020-05-01 00:00:30"),
        to_timestamp=dt("2020-05-01 00:00:33"),
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_events_from_time_range_no_from(factory, events_dm, con):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    event1 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:30"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:31"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:33"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    for i in range(40, 50):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1], to_timestamp=dt("2020-05-01 00:00:35")
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_events_from_time_range_no_to(factory, events_dm, con):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:40"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:41"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:43"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1], from_timestamp=dt("2020-05-01 00:00:30")
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_limited_number_of_events_from_time_range(
    factory, events_dm, con
):

    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:30"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:31"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:33"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    for i in range(40, 50):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1],
        from_timestamp=dt("2020-05-01 00:00:00"),
        to_timestamp=dt("2020-05-01 00:00:35"),
        limit=3,
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]


async def test_returns_limited_number_of_events_from_time_range_starting_with_specified(
    factory, events_dm, con
):
    campaign_id = (await factory.create_campaign(order_id=1))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:30"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
    )
    event2 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:31"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
    )
    event3 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:33"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )
    event4 = await factory.create_campaign_event(
        timestamp=dt("2020-05-01 00:00:33"),
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
    )

    for i in range(40, 50):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-05-01 00:00:{i}"),
        )

    result = await events_dm.retrieve_campaigns_events_by_orders(
        order_ids=[1],
        from_timestamp=dt("2020-05-01 00:00:00"),
        to_timestamp=dt("2020-05-01 00:00:45"),
        starting_event_id=event4["id"],  # should not be included in result
        limit=3,
    )

    assert result == [
        {
            "id": event3["id"],
            "timestamp": event3["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.BUDGET_DECREASED,
            "event_data": {},
        },
        {
            "id": event2["id"],
            "timestamp": event2["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
            "event_data": {},
        },
        {
            "id": event1["id"],
            "timestamp": event1["timestamp"],
            "campaign_id": campaign_id,
            "campaign_name": "campaign0",
            "billing_order_id": 1,
            "manul_order_id": None,
            "event_type": CampaignEventTypeEnum.STOPPED_MANUALLY,
            "event_data": {},
        },
    ]
