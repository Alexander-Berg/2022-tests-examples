import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


@pytest.mark.parametrize(
    "event_ts",
    (dt("2021-01-01 00:00:00"), dt("2020-01-01 00:00:00"), dt("2019-10-03 00:00:01")),
)
async def test_segments_as_unprocessed_clients_with_not_resolved_order_events(
    factory, dm, event_ts
):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id, event_type=OrderEvent.CREATED, event_timestamp=event_ts
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 1


async def test_does_not_segment_as_unprocessed_clients_by_call_events(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(client_id, OrderEvent.ACCEPTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 0


async def test_segments_clients_regardless_resolved_order_events(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(client_id, event_type=OrderEvent.CREATED)
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.ACCEPTED
    )
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.REJECTED
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 1


async def test_skips_clients_if_all_order_events_resolved(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.ACCEPTED
    )
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.REJECTED
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 0


async def test_counts_several_clients_as_expected(factory, dm):
    await factory.create_empty_client(segments={SegmentType.UNPROCESSED_ORDERS})
    await factory.create_empty_client(segments={SegmentType.UNPROCESSED_ORDERS})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 2


async def test_matches_each_client_only_once(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_unprocessed_orders_segment(client_id)
    await factory.add_client_to_unprocessed_orders_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 1


@pytest.mark.parametrize(
    "event_type",
    [
        # match only 'order_created'
        OrderEvent.ACCEPTED,
        OrderEvent.REJECTED,
        CallEvent.INITIATED,
    ],
)
async def test_skips_clients_with_irrelevant_events(event_type, factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_event(client_id=client_id, event_type=event_type)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 0


async def test_skips_clients_events_from_another_business(factory, dm):
    client_id = await factory.create_empty_client(biz_id=999)
    await factory.create_order_event(client_id=client_id, event_type=OrderEvent.CREATED)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    # Was in segment, but left it
    client_id_1 = await factory.create_empty_client(created_at=dt("2019-09-10"))
    # Was in segment and still there
    client_id_2 = await factory.create_empty_client(created_at=dt("2019-09-10"))

    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-11-20"),
    )
    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-12-20"),
    )
    await factory.create_order_event(
        client_id=client_id_2,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-11-20"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.UNPROCESSED_ORDERS]["previous_size"] == 2
