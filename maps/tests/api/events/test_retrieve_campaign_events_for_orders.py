import pytest

from maps_adv.adv_store.api.proto.event_list_pb2 import (
    CampaignEventForOrderIdsInput,
    CampaignEventList,
    CampaignEventListItem,
    CampaignEventType,
    TimeRangeFilter,
)
from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

API_URL = "/events/by-order-ids/"


async def test_returns_nothing_if_nothing_exists(api):
    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1111, 9999], manul_order_ids=[2222, 4444, 8888]
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(events=[])


async def test_returns_event_for_orders(factory, api):
    campaign_1_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=2, name="Campaign 2"))["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:01"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:02"),
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:03"),
    )

    input_pb = CampaignEventForOrderIdsInput(order_ids=[1, 2])

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-01-01 00:00:03", as_proto=True),
                campaign_id=campaign_2_id,
                campaign_name="Campaign 2",
                billing_order_id=2,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:02", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:01", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_event_for_manul_orders(factory, api):
    campaign_1_id = (
        await factory.create_campaign(manul_order_id=2, name="Campaign 1")
    )["id"]
    campaign_2_id = (
        await factory.create_campaign(manul_order_id=3, name="Campaign 2")
    )["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:01"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:02"),
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:03"),
    )

    input_pb = CampaignEventForOrderIdsInput(manul_order_ids=[2, 3])

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-01-01 00:00:03", as_proto=True),
                campaign_id=campaign_2_id,
                campaign_name="Campaign 2",
                manul_order_id=3,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:02", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                manul_order_id=2,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:01", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                manul_order_id=2,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_events_for_both_types_of_orders(factory, api):
    campaign_1_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=3, name="Campaign 2"))["id"]
    campaign_3_id = (
        await factory.create_campaign(manul_order_id=2, name="Campaign 3")
    )["id"]
    campaign_4_id = (
        await factory.create_campaign(manul_order_id=4, name="Campaign 4")
    )["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:01"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:02"),
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_3_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:03"),
    )
    event4 = await factory.create_campaign_event(
        campaign_id=campaign_4_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:04"),
    )

    input_pb = CampaignEventForOrderIdsInput(order_ids=[1, 3], manul_order_ids=[2, 4])

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event4["id"],
                timestamp=dt("2020-01-01 00:00:04", as_proto=True),
                campaign_id=campaign_4_id,
                campaign_name="Campaign 4",
                manul_order_id=4,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-01-01 00:00:03", as_proto=True),
                campaign_id=campaign_3_id,
                campaign_name="Campaign 3",
                manul_order_id=2,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:02", as_proto=True),
                campaign_id=campaign_2_id,
                campaign_name="Campaign 2",
                billing_order_id=3,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:01", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_does_not_returns_events_for_other_orders(factory, api):

    campaign_1_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=3, name="Campaign 2"))["id"]
    campaign_3_id = (
        await factory.create_campaign(manul_order_id=2, name="Campaign 3")
    )["id"]
    campaign_4_id = (
        await factory.create_campaign(manul_order_id=4, name="Campaign 4")
    )["id"]

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_1_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:01"),
    )
    await factory.create_campaign_event(
        campaign_id=campaign_2_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:02"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_3_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:03"),
    )
    await factory.create_campaign_event(
        campaign_id=campaign_4_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:04"),
    )

    input_pb = CampaignEventForOrderIdsInput(order_ids=[1], manul_order_ids=[2])

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:03", as_proto=True),
                campaign_id=campaign_3_id,
                campaign_name="Campaign 3",
                manul_order_id=2,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:01", as_proto=True),
                campaign_id=campaign_1_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_limited_number_of_events(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-01-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:21"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:22"),
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:23"),
    )

    input_pb = CampaignEventForOrderIdsInput(order_ids=[1], limit=3)

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-01-01 00:00:23", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:22", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:21", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_limited_number_of_events_starting_with_specified(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

    for i in range(10):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-01-01 00:00:{i}"),
        )

    event1 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
        event_data={},
        timestamp=dt("2020-01-01 00:00:21"),
    )
    event2 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.STOPPED_BUDGET_REACHED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:22"),
    )
    event3 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:23"),
    )
    event4 = await factory.create_campaign_event(
        campaign_id=campaign_id,
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={},
        timestamp=dt("2020-01-01 00:00:24"),
    )

    for i in range(30, 40):
        await factory.create_campaign_event(
            campaign_id=campaign_id,
            event_type=CampaignEventTypeEnum.STOPPED_MANUALLY,
            event_data={},
            timestamp=dt(f"2020-01-01 00:00:{i}"),
        )

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        starting_event_id=event4["id"],  # result should not include this event
        limit=3,
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-01-01 00:00:23", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-01-01 00:00:22", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-01-01 00:00:21", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_events_from_time_range(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

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

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        time_range=TimeRangeFilter(
            from_timestamp=dt("2020-05-01 00:00:30", as_proto=True),
            to_timestamp=dt("2020-05-01 00:00:33", as_proto=True),
        ),
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-05-01 00:00:33", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-05-01 00:00:31", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-05-01 00:00:30", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_events_from_time_range_no_from(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

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

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        time_range=TimeRangeFilter(
            to_timestamp=dt("2020-05-01 00:00:33", as_proto=True)
        ),
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-05-01 00:00:33", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-05-01 00:00:31", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-05-01 00:00:30", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_events_from_time_range_no_to(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

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

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        time_range=TimeRangeFilter(
            from_timestamp=dt("2020-05-01 00:00:30", as_proto=True)
        ),
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-05-01 00:00:43", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-05-01 00:00:41", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-05-01 00:00:40", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_limited_number_of_events_from_time_range(factory, api):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

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

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        time_range=TimeRangeFilter(
            from_timestamp=dt("2020-05-01 00:00:30", as_proto=True),
            to_timestamp=dt("2020-05-01 00:00:33", as_proto=True),
        ),
        limit=3,
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-05-01 00:00:33", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-05-01 00:00:31", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-05-01 00:00:30", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )


async def test_returns_limited_number_of_events_from_time_range_starting_with_specified(
    factory, api
):

    campaign_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]

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
        timestamp=dt("2020-05-01 00:00:34"),
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

    input_pb = CampaignEventForOrderIdsInput(
        order_ids=[1],
        time_range=TimeRangeFilter(
            from_timestamp=dt("2020-05-01 00:00:30", as_proto=True),
            to_timestamp=dt("2020-05-01 00:00:35", as_proto=True),
        ),
        starting_event_id=event4["id"],  # result should not include this event
        limit=3,
    )

    result = await api.post(
        API_URL, proto=input_pb, decode_as=CampaignEventList, expected_status=200
    )

    assert result == CampaignEventList(
        events=[
            CampaignEventListItem(
                id=event3["id"],
                timestamp=dt("2020-05-01 00:00:33", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.BUDGET_DECREASED,
            ),
            CampaignEventListItem(
                id=event2["id"],
                timestamp=dt("2020-05-01 00:00:31", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_BUDGET_REACHED,
            ),
            CampaignEventListItem(
                id=event1["id"],
                timestamp=dt("2020-05-01 00:00:30", as_proto=True),
                campaign_id=campaign_id,
                campaign_name="Campaign 1",
                billing_order_id=1,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
        ]
    )
