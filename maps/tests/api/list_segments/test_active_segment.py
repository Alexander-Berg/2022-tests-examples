import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType
from maps_adv.geosmb.doorman.server.tests.api.list_segments.utils import (
    parse_pb_segment_sizes,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2020-01-01 00:00:01")]

url = "v1/list_segments/"


@pytest.mark.parametrize(
    "event_ts",
    (dt("2021-01-01 00:00:00"), dt("2020-01-01 00:00:00"), dt("2019-10-03 00:00:01")),
)
@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, CallEvent.INITIATED])
async def test_segments_as_active_clients_with_relevant_events(
    event_type, factory, api, event_ts
):
    client_id = await factory.create_empty_client()
    await factory.create_event(
        client_id=client_id, event_type=event_type, event_timestamp=event_ts
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[0] == 1


async def test_counts_several_clients_as_expected(factory, api):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[0] == 2


async def test_matches_each_client_only_once(factory, api):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_active_segment(client_id)
    await factory.add_client_to_active_segment(client_id)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[0] == 1


async def test_skips_other_business_clients(factory, api):
    await factory.create_empty_client(biz_id=999, segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[0] == 0


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
async def test_skips_clients_with_irrelevant_events(factory, api, event_bad_kwargs):
    """Only clients with recently accepted order events
    are matched to ACTIVE segment."""
    client_id = await factory.create_empty_client()
    event_kwargs = dict(
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2019-10-03 00:00:01"),
    )
    event_kwargs.update(**event_bad_kwargs)
    await factory.create_order_event(client_id=client_id, **event_kwargs)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[0] == 0


async def test_returns_previous_segment_size(factory, api):
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

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, segments_pb2.SegmentType.ACTIVE)[1] == 2
