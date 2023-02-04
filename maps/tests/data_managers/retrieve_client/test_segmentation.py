import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


async def test_returns_all_clients_segments(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["segments"] == [
        SegmentType.REGULAR,
        SegmentType.ACTIVE,
        SegmentType.UNPROCESSED_ORDERS,
    ]


async def test_segments_as_no_order_if_client_without_events(dm, factory):
    client_id = await factory.create_empty_client()

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["segments"] == [SegmentType.NO_ORDERS]


async def test_segments_as_no_order_if_client_without_order_events(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.NO_ORDERS in got["segments"]


@pytest.mark.parametrize(
    "event_timestamp", [dt("2019-11-03 00:00:01"), dt("2018-11-03 00:00:01")]
)
@pytest.mark.parametrize("event_type", OrderEvent)
async def test_does_not_segment_as_no_order_if_client_has_any_order_event(
    event_type, event_timestamp, dm, factory
):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id, event_type=event_type, event_timestamp=event_timestamp
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.NO_ORDERS not in got["segments"]


async def test_segments_as_active_if_has_accepted_events_in_last_90_days(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.ACTIVE in got["segments"]


async def test_segments_as_active_if_has_call_events_in_last_90_days(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(
        client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.ACTIVE in got["segments"]


async def test_segments_as_regular_if_3_or_more_created_order_events_in_last_90_days(
    dm, factory
):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.REGULAR in got["segments"]


async def test_does_not_segments_as_regular_for_call_events(dm, factory):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_call_event(
            client_id,
            event_type=CallEvent.INITIATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.REGULAR not in got["segments"]


@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, OrderEvent.REJECTED])
async def test_segments_as_regular_regardless_of_order_resolution(
    event_type, dm, factory
):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, event_type, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.REGULAR in got["segments"]


async def test_does_not_segment_as_regular_if_lt_3_created_order_events_in_last_90_days(
    dm, factory
):
    client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.REGULAR not in got["segments"]


@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, OrderEvent.REJECTED])
async def test_segments_as_lost_if_processed_events_older_then_90_days(
    event_type, dm, factory
):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, event_type, event_timestamp=dt("2018-11-03 00:00:01")
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST in got["segments"]


async def test_segments_as_lost_if_only_rejected_events_in_last_90_days(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2019-11-03 00:00:01")
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST in got["segments"]


async def test_segments_as_lost_if_unprocessed_events_older_then_90_days(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST in got["segments"]


async def test_segments_as_lost_if_call_events_older_then_90_days(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(
        client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST in got["segments"]


async def test_does_not_segment_as_lost_for_history_if_currently_active(dm, factory):
    client_id = await factory.create_empty_client()
    # current accepted event
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
    )
    # old event
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2018-11-03 00:00:01")
    )
    # current rejected event
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2019-11-03 00:00:01")
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST not in got["segments"]


@pytest.mark.parametrize("recent_event_type", [OrderEvent.CREATED, CallEvent.INITIATED])
async def test_does_not_segment_as_lost_if_client_has_recent_order_created_event(  # noqa
    recent_event_type, dm, factory
):
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2019-11-03 00:00:01")
    )
    await factory.create_event(
        client_id,
        event_type=recent_event_type,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.LOST not in got["segments"]


async def test_segments_as_unprocessed_orders_if_order_event_not_completed(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(client_id=client_id, event_type=OrderEvent.CREATED)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.UNPROCESSED_ORDERS in got["segments"]


async def test_does_not_segment_as_unprocessed_orders_for_call_events(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.UNPROCESSED_ORDERS not in got["segments"]


@pytest.mark.parametrize("event_value", ["Success", "Error", "fake-value"])
@pytest.mark.parametrize("talk_duration", [1, 9])
async def test_segments_as_short_last_call_if_last_call_is_answered_and_lt_10_sec(
    dm, factory, event_value, talk_duration
):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_value=event_value,
        talk_duration=talk_duration,
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.SHORT_LAST_CALL in got["segments"]


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
    dm, factory, call_events
):
    client_id = await factory.create_empty_client()
    for event_params in call_events:
        await factory.create_call_event(client_id=client_id, **event_params)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.SHORT_LAST_CALL not in got["segments"]


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

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.MISSED_LAST_CALL in got["segments"]


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

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert SegmentType.MISSED_LAST_CALL not in got["segments"]


async def test_does_not_considers_other_client_events(dm, factory):
    side_client_id = await factory.create_empty_client()
    client_id = await factory.create_empty_client()
    for event_type in OrderEvent:
        await factory.create_order_event(side_client_id, event_type=event_type)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["segments"] == [SegmentType.NO_ORDERS]


async def test_does_not_considers_other_client_call_events(dm, factory):
    # active, no orders
    side_client_id = await factory.create_empty_client()
    await factory.create_call_event(
        side_client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )
    # lost
    client_id = await factory.create_empty_client()
    await factory.create_resolved_order_events_pair(
        client_id,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["segments"] == [SegmentType.LOST]
