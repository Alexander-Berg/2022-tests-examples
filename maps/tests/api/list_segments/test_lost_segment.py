import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType
from maps_adv.geosmb.doorman.server.tests.api.list_segments.utils import (
    parse_pb_segment_sizes,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "v1/list_segments/"


@pytest.mark.parametrize("event_type", [e for e in OrderEvent] + [e for e in CallEvent])
async def test_segments_as_lost_clients_with_only_old_events(factory, api, event_type):
    client_id = await factory.create_empty_client()
    await factory.create_event(
        client_id=client_id,
        event_type=event_type,
        event_timestamp=dt("2019-10-03 00:00:00"),
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 1


async def test_segments_as_lost_clients_with_only_rejected_recent_orders(factory, api):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2020-01-01 00:00:00")
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 1


async def test_counts_several_clients_as_expected(factory, api):
    await factory.create_empty_client(segments={SegmentType.LOST})
    await factory.create_empty_client(segments={SegmentType.LOST})

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 2


async def test_matches_each_client_only_once(factory, api):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_lost_segment(client_id)
    await factory.add_client_to_lost_segment(client_id)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 1


async def test_skips_clients_without_orders(factory, api):
    await factory.create_empty_client()

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 0


async def test_skips_clients_with_not_all_recent_orders_rejected(factory, api):
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

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 0


async def test_skips_clients_with_other_biz_id(factory, api):
    client_id = await factory.create_empty_client(biz_id=999)
    await factory.add_client_to_lost_segment(client_id)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[0] == 0


async def test_returns_previous_segment_size(factory, api):
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

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[1] == 2


async def test_skips_client_with_recent_unprocessed_order_for_previous(factory, api):
    client_id = await factory.create_empty_client(created_at=dt("2019-09-10"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20 00:00:00"),
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.LOST)[1] == 0
