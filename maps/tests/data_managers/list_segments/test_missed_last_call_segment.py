import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2020-01-01 00:00:01")]


@pytest.mark.parametrize("event_value", ["Success", "Error", "fake-value"])
async def test_segments_as_missed_last_call_if_last_call_is_not_answered(
    factory, dm, event_value
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_value=event_value,
        talk_duration=0,
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["current_size"] == 1


@pytest.mark.parametrize(
    "call_events",
    (
        # not FINISHED
        [
            dict(event_type=CallEvent.INITIATED, talk_duration=0),
        ],
        # has talk duration
        [
            dict(event_type=CallEvent.FINISHED, talk_duration=10),
        ],
        # last call event is not matched
        [
            dict(
                event_type=CallEvent.FINISHED,
                event_value="Unknown",
                talk_duration=10,
                event_timestamp=dt("2020-02-02 00:00:00"),
            ),
            dict(
                event_type=CallEvent.FINISHED,
                event_value="Unknown",
                talk_duration=0,
                event_timestamp=dt("2020-01-01 00:00:00"),
            ),
        ],
    ),
)
async def test_does_not_segment_as_missed_last_call_if_events_does_not_match_conditions(
    dm, factory, call_events
):
    client_id = await factory.create_client()
    for event_params in call_events:
        await factory.create_call_event(client_id=client_id, **event_params)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["current_size"] == 0


async def test_counts_several_clients_as_expected(factory, dm):
    await factory.create_empty_client(segments={SegmentType.MISSED_LAST_CALL})
    await factory.create_empty_client(segments={SegmentType.MISSED_LAST_CALL})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["current_size"] == 2


async def test_matches_each_client_only_once(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_missed_last_call_segment(client_id)
    await factory.add_client_to_missed_last_call_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["current_size"] == 1


async def test_skips_other_business_clients(factory, dm):
    await factory.create_empty_client(
        biz_id=999, segments={SegmentType.MISSED_LAST_CALL}
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    client_id_1 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_2 = await factory.create_empty_client()  # Was in segment and still there
    client_id_3 = await factory.create_empty_client()  # Was in segment and still there
    await factory.create_call_event(
        client_id=client_id_1,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=0,
    )
    await factory.create_call_event(
        client_id=client_id_1,
        event_type=CallEvent.FINISHED,
        event_value="Success",
        event_timestamp=dt("2019-12-20 00:00:00"),
        talk_duration=0,
    )
    await factory.create_call_event(
        client_id=client_id_2,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=0,
    )
    await factory.create_call_event(
        client_id=client_id_3,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-09-20 00:00:00"),
        talk_duration=0,
    )
    await factory.create_call_event(
        client_id=client_id_3,
        event_type=CallEvent.FINISHED,
        event_value="NoAnswer",
        event_timestamp=dt("2019-12-20 00:00:00"),
        talk_duration=0,
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.MISSED_LAST_CALL]["previous_size"] == 3
