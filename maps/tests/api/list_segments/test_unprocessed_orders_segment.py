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


@pytest.mark.parametrize(
    "event_ts",
    (dt("2021-01-01 00:00:00"), dt("2020-01-01 00:00:00"), dt("2019-01-01 00:00:01")),
)
async def test_segments_as_unprocessed_clients_with_not_resolved_order_events(
    factory, api, event_ts
):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id, event_type=OrderEvent.CREATED, event_timestamp=event_ts
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 1


async def test_does_not_segment_as_unprocessed_clients_by_call_events(factory, api):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(client_id, OrderEvent.ACCEPTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 0


async def test_segments_clients_regardless_resolved_order_events(factory, api):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(client_id, event_type=OrderEvent.CREATED)
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.ACCEPTED
    )
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.REJECTED
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 1


async def test_skips_clients_if_all_order_events_resolved(factory, api):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.ACCEPTED
    )
    await factory.create_resolved_order_events_pair(
        client_id, event_type=OrderEvent.REJECTED
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 0


async def test_counts_several_clients_as_expected(factory, api):
    await factory.create_empty_client(segments={SegmentType.UNPROCESSED_ORDERS})
    await factory.create_empty_client(segments={SegmentType.UNPROCESSED_ORDERS})

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 2


async def test_matches_each_client_only_once(factory, api):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_unprocessed_orders_segment(client_id)
    await factory.add_client_to_unprocessed_orders_segment(client_id)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 1


@pytest.mark.parametrize(
    "event_type",
    [
        # match only 'order_created'
        OrderEvent.ACCEPTED,
        OrderEvent.REJECTED,
        CallEvent.INITIATED,
    ],
)
async def test_skips_clients_with_irrelevant_events(factory, api, event_type):
    client_id = await factory.create_empty_client()
    await factory.create_event(client_id=client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 0


async def test_skips_clients_events_from_another_business(factory, api):
    client_id = await factory.create_empty_client(biz_id=999)
    await factory.create_order_event(client_id=client_id, event_type=OrderEvent.CREATED)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[0] == 0


async def test_returns_previous_segment_size(factory, api):
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

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.UNPROCESSED_ORDERS)[1] == 2
