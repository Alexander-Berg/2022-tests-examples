import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2020-01-01 00:00:01")]


@pytest.mark.parametrize(
    "event_ts",
    (dt("2021-01-01 00:00:00"), dt("2020-01-01 00:00:00"), dt("2019-10-03 00:00:01")),
)
@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, CallEvent.INITIATED])
async def test_segments_as_active_clients_with_relevant_events(
    event_type, event_ts, factory, dm
):
    """Clients with recently accepted order events are matched to ACTIVE segment."""
    client_id = await factory.create_empty_client()
    await factory.create_event(
        client_id=client_id, event_type=event_type, event_timestamp=event_ts
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 1


async def test_counts_several_clients_as_expected(factory, dm):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 2


async def test_matches_each_client_only_once(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_active_segment(client_id)
    await factory.add_client_to_active_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 1


async def test_skips_clients_with_other_biz_id(factory, dm):
    client_id = await factory.create_empty_client(biz_id=999)
    await factory.add_client_to_active_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 0


async def test_skips_other_business_clients(factory, dm):
    await factory.create_empty_client(biz_id=999, segments={SegmentType.ACTIVE})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 0


@pytest.mark.parametrize(
    "event_bad_kwargs",
    [
        # match only accepted orders or calls
        dict(event_type=OrderEvent.CREATED),
        dict(event_type=OrderEvent.REJECTED),
        # ignore events older than 90 days
        dict(event_timestamp=dt("2019-10-03 00:00:00")),
    ],
)
async def test_skips_clients_with_irrelevant_events(factory, dm, event_bad_kwargs):
    """Only clients with recently accepted order events
    or recently made calls are matched to ACTIVE segment."""
    client_id = await factory.create_empty_client()
    event_kwargs = dict(
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-10-03 00:00:01"),
    )
    event_kwargs.update(**event_bad_kwargs)
    await factory.create_order_event(client_id=client_id, **event_kwargs)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    client_id_1 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_2 = await factory.create_empty_client()  # Was in segment and still there
    await factory.create_resolved_order_events_pair(
        client_id=client_id_1,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-09-20 00:00:00"),
    )
    await factory.create_resolved_order_events_pair(
        client_id=client_id_2,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-09-20 00:00:00"),
    )
    await factory.create_resolved_order_events_pair(
        client_id=client_id_2,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-12-20 00:00:00"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.ACTIVE]["previous_size"] == 2
