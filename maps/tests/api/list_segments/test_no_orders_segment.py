import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent
from maps_adv.geosmb.doorman.server.tests.api.list_segments.utils import (
    parse_pb_segment_sizes,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


url = "v1/list_segments/"


async def test_segments_as_no_order_clients_without_events(factory, api):
    await factory.create_empty_client()
    await factory.create_empty_client()

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[0] == 2


async def test_segments_as_no_order_clients_without_order_events(factory, api):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[0] == 1


@pytest.mark.parametrize("event_type", OrderEvent)
@pytest.mark.parametrize(
    "event_ts", (dt("2020-01-01 00:00:00"), dt("2018-01-01 00:00:00"))
)
async def test_skips_client_with_order_events(factory, api, event_type, event_ts):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id, event_type=event_type, event_timestamp=event_ts
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[0] == 0


async def test_skips_client_with_other_biz_id(factory, api):
    await factory.create_client(biz_id=999)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[0] == 0


async def test_returns_previous_segment_size(factory, api):
    # Was in segment, but left it
    client_id_1 = await factory.create_empty_client(created_at=dt("2019-09-10"))
    # Was in segment and still there
    await factory.create_empty_client(created_at=dt("2019-09-10"))

    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20"),
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[1] == 2


async def test_skips_clients_created_later_for_previous(factory, api):
    client_id_1 = await factory.create_empty_client(created_at=dt("2019-12-10"))
    await factory.create_empty_client(created_at=dt("2019-12-10"))

    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20"),
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.NO_ORDERS)[1] == 0
