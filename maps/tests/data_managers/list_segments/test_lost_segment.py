import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


@pytest.mark.parametrize("event_type", [e for e in OrderEvent] + [e for e in CallEvent])
async def test_segments_as_lost_clients_with_only_old_events(factory, dm, event_type):
    client_id = await factory.create_empty_client()
    await factory.create_event(
        client_id=client_id,
        event_type=event_type,
        event_timestamp=dt("2019-10-03 00:00:00"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 1


async def test_segments_as_lost_clients_with_only_rejected_recent_orders(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2020-01-01 00:00:00")
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 1


async def test_counts_several_clients_as_expected(factory, dm):
    await factory.create_empty_client(segments={SegmentType.LOST})
    await factory.create_empty_client(segments={SegmentType.LOST})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 2


async def test_matches_each_client_only_once(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_lost_segment(client_id)
    await factory.add_client_to_lost_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 1


async def test_skips_clients_without_orders(factory, dm):
    await factory.create_empty_client()

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 0


async def test_skips_clients_with_not_all_recent_orders_rejected(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )
    await factory.create_resolved_order_events_pair(
        client_id=client_id,
        event_type=OrderEvent.REJECTED,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 0


async def test_skips_clients_with_other_biz_id(factory, dm):
    client_id = await factory.create_empty_client(biz_id=999)
    await factory.add_client_to_lost_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    client_id_1 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_2 = await factory.create_empty_client()  # Was in segment and still there
    await factory.create_resolved_order_events_pair(
        client_id=client_id_1,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-06-01 00:00:00"),
    )
    await factory.create_resolved_order_events_pair(
        client_id=client_id_1,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-12-20 00:00:00"),
    )
    await factory.create_resolved_order_events_pair(
        client_id=client_id_2,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-06-01 00:00:00"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["previous_size"] == 2


async def test_skips_client_with_recent_unprocessed_order_for_previous(factory, dm):
    client_id = await factory.create_empty_client(created_at=dt("2019-09-10"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20 00:00:00"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.LOST]["previous_size"] == 0
