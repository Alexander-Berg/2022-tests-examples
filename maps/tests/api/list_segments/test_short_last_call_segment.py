import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, SegmentType
from maps_adv.geosmb.doorman.server.tests.api.list_segments.utils import (
    parse_pb_segment_sizes,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2020-01-01 00:00:01")]

url = "v1/list_segments/"


@pytest.mark.parametrize("event_value", ["Success", "Error", "fake-value"])
@pytest.mark.parametrize("talk_duration", [1, 9])
async def test_segments_as_short_last_call_if_last_call_is_answered_and_lt_10_sec(
    factory, api, event_value, talk_duration
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_value=event_value,
        talk_duration=talk_duration,
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[0] == 1


@pytest.mark.parametrize(
    "call_events",
    (
        [
            # not FINISHED
            dict(
                event_type=CallEvent.INITIATED, event_value="Success", talk_duration=9
            ),
        ],
        # talk duration >= 10s
        [
            dict(
                event_type=CallEvent.FINISHED, event_value="Success", talk_duration=10
            ),
        ],
        # talk duration = 0
        [
            dict(
                event_type=CallEvent.FINISHED, event_value="NoAnswer", talk_duration=0
            ),
        ],
        # last call event is not matched
        [
            dict(
                event_type=CallEvent.FINISHED,
                event_value="Success",
                talk_duration=10,
                event_timestamp=dt("2020-02-02 00:00:00"),
            ),
            dict(
                event_type=CallEvent.FINISHED,
                event_value="Success",
                talk_duration=9,
                event_timestamp=dt("2020-01-01 00:00:00"),
            ),
        ],
    ),
)
async def test_does_not_segment_as_short_last_call_if_events_does_not_match_conditions(
    api, factory, call_events
):
    client_id = await factory.create_client()
    for event_params in call_events:
        await factory.create_call_event(client_id=client_id, **event_params)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[0] == 0


async def test_counts_several_clients_as_expected(factory, api):
    await factory.create_empty_client(segments={SegmentType.SHORT_LAST_CALL})
    await factory.create_empty_client(segments={SegmentType.SHORT_LAST_CALL})

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[0] == 2


async def test_matches_each_client_only_once(factory, api):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_short_last_call_segment(client_id)
    await factory.add_client_to_short_last_call_segment(client_id)

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[0] == 1


async def test_skips_other_business_clients(factory, api):
    await factory.create_empty_client(
        biz_id=999, segments={SegmentType.SHORT_LAST_CALL}
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[0] == 0


async def test_returns_previous_segment_size(factory, api):
    client_id_1 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_2 = await factory.create_empty_client()  # Was in segment and still there
    client_id_3 = await factory.create_empty_client()  # Was in segment and still there
    await factory.create_call_event(
        client_id=client_id_1,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=3,
    )
    await factory.create_call_event(
        client_id=client_id_1,
        event_type=CallEvent.FINISHED,
        event_value="Success",
        event_timestamp=dt("2019-12-20 00:00:00"),
        talk_duration=22,
    )
    await factory.create_call_event(
        client_id=client_id_2,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=3,
    )
    await factory.create_call_event(
        client_id=client_id_3,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=3,
    )
    await factory.create_call_event(
        client_id=client_id_3,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-12-20 00:00:00"),
        talk_duration=3,
    )

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_sizes(got, SegmentTypePb.SHORT_LAST_CALL)[1] == 3
