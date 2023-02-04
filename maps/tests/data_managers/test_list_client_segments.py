import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType
from maps_adv.geosmb.doorman.server.lib.exceptions import NoClientIdFieldsPassed

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


@pytest.mark.parametrize(
    "input_params",
    [
        dict(passport_uid=465),
        dict(email="kek@ya.ru"),
        dict(phone=88002000600),
        dict(passport_uid=465, phone=88002000600, email="kek@ya.ru"),
    ],
)
async def test_returns_list_of_segments_for_businesses_to_which_client_belongs_to(
    input_params, factory, dm
):
    client_id1 = await factory.create_client(
        biz_id=100,
        passport_uid=465,
        phone=88002000600,
        email="kek@ya.ru",
        segments={SegmentType.LOST},
        labels=["orange"],
    )
    client_id2 = await factory.create_client(
        biz_id=200,
        passport_uid=465,
        phone=88002000600,
        email="kek@ya.ru",
        # side effect - also ACTIVE
        segments={SegmentType.NO_ORDERS},
        labels=["lemon"],
    )

    got = await dm.list_client_segments(**input_params)

    assert got == [
        dict(
            client_id=client_id2,
            biz_id=200,
            segments=[SegmentType.ACTIVE, SegmentType.NO_ORDERS],
            labels=["lemon"],
        ),
        dict(
            client_id=client_id1,
            biz_id=100,
            segments=[SegmentType.LOST],
            labels=["orange"],
        ),
    ]


async def test_returns_all_business_to_which_client_belongs_to_by_any_id_match(
    factory, dm
):
    await factory.create_client(
        biz_id=100, passport_uid=465, phone=88003335566, email=None
    )
    await factory.create_client(
        biz_id=200, passport_uid=872, phone=None, email="kek@ya.ru"
    )
    await factory.create_client(
        biz_id=300, passport_uid=None, phone=88002000600, email="ne_kek@ya.ru"
    )

    got = await dm.list_client_segments(
        passport_uid=465, phone=88002000600, email="kek@ya.ru"
    )

    assert [g["biz_id"] for g in got] == [300, 200, 100]


async def test_filter_by_biz_id_if_passed(factory, dm):
    await factory.create_client(
        biz_id=100, passport_uid=465, phone=88003335566, email=None
    )
    await factory.create_client(
        biz_id=200, passport_uid=872, phone=None, email="kek@ya.ru"
    )
    await factory.create_client(
        biz_id=300, passport_uid=None, phone=88002000600, email="ne_kek@ya.ru"
    )

    got = await dm.list_client_segments(
        passport_uid=465, phone=88002000600, email="kek@ya.ru", biz_id=200
    )

    assert [g["biz_id"] for g in got] == [200]


async def test_segments_correctly_each_client_in_business(dm, factory):
    # no orders, active as side_effect
    await factory.create_client(
        client_id=1, phone=88003335566, segments={SegmentType.NO_ORDERS}, labels=[]
    )
    # lost
    await factory.create_client(
        client_id=2, passport_uid=873, segments={SegmentType.LOST}, labels=[]
    )
    # regular and active, unprocessed as side effect
    await factory.create_client(
        client_id=3,
        passport_uid=124,
        email="kek@ya.ru",
        segments={SegmentType.REGULAR, SegmentType.ACTIVE},
        labels=[],
    )
    # short_last_call
    await factory.create_client(
        client_id=4,
        passport_uid=567,
        email="kek@ya.ru",
        segments={SegmentType.SHORT_LAST_CALL},
        labels=[],
    )
    # missed_last_call
    await factory.create_client(
        client_id=5,
        passport_uid=890,
        email="kek@ya.ru",
        segments={SegmentType.MISSED_LAST_CALL},
        labels=[],
    )

    got = await dm.list_client_segments(
        passport_uid=873, phone=88003335566, email="kek@ya.ru"
    )

    assert got == [
        dict(
            client_id=5,
            biz_id=123,
            segments=[
                SegmentType.LOST,
                SegmentType.NO_ORDERS,
                SegmentType.MISSED_LAST_CALL,
            ],
            labels=[],
        ),
        dict(
            client_id=4,
            biz_id=123,
            segments=[
                SegmentType.LOST,
                SegmentType.NO_ORDERS,
                SegmentType.SHORT_LAST_CALL,
            ],
            labels=[],
        ),
        dict(
            client_id=3,
            biz_id=123,
            segments=[
                SegmentType.REGULAR,
                SegmentType.ACTIVE,
                SegmentType.UNPROCESSED_ORDERS,
            ],
            labels=[],
        ),
        dict(client_id=2, biz_id=123, segments=[SegmentType.LOST], labels=[]),
        dict(
            client_id=1,
            biz_id=123,
            segments=[SegmentType.ACTIVE, SegmentType.NO_ORDERS],
            labels=[],
        ),
    ]


async def test_returns_empty_list_if_no_match_found(factory, dm):
    await factory.create_client(
        client_id=111, biz_id=100, passport_uid=234, phone=88003335566, email=None
    )
    await factory.create_client(
        client_id=222, biz_id=200, passport_uid=872, phone=None, email="ne_kek@ya.ru"
    )

    got = await dm.list_client_segments(
        passport_uid=465, phone=88002000600, email="kek@ya.ru"
    )

    assert got == []


async def test_returns_all_clients_segments_in_business(dm, factory):
    client_id = await factory.create_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert len(got) == 1
    assert got[0]["segments"] == [
        SegmentType.REGULAR,
        SegmentType.ACTIVE,
        SegmentType.UNPROCESSED_ORDERS,
    ]


async def test_segments_in_business_as_no_order_if_client_without_events(dm, factory):
    await factory.create_client()

    got = await dm.list_client_segments(passport_uid=456)

    assert got[0]["segments"] == [SegmentType.NO_ORDERS]


async def test_segments_in_business_as_no_order_if_client_without_order_events(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.NO_ORDERS in got[0]["segments"]


@pytest.mark.parametrize(
    "event_timestamp", [dt("2019-11-03 00:00:01"), dt("2018-11-03 00:00:01")]
)
@pytest.mark.parametrize("event_type", OrderEvent)
async def test_does_not_segment_as_no_order_if_client_has_any_order_event_in_business(
    event_type, event_timestamp, dm, factory
):
    client_id = await factory.create_client()
    await factory.create_order_event(
        client_id, event_type=event_type, event_timestamp=event_timestamp
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.NO_ORDERS not in got[0]["segments"]


async def test_segments_in_business_as_active_if_has_accepted_events_in_last_90_days(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.ACTIVE in got[0]["segments"]


async def test_segments_in_business_as_active_if_has_call_events_in_last_90_days(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.ACTIVE in got[0]["segments"]


async def test_segments_in_business_as_regular_if_3_or_more_created_order_events_in_last_90_days(  # noqa
    dm, factory
):
    client_id = await factory.create_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.REGULAR in got[0]["segments"]


async def test_does_not_segments_in_business_as_regular_for_call_events(dm, factory):
    client_id = await factory.create_client()
    for _ in range(3):
        await factory.create_call_event(
            client_id,
            event_type=CallEvent.INITIATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.REGULAR not in got[0]["segments"]


@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, OrderEvent.REJECTED])
async def test_segments_in_business_as_regular_regardless_of_order_resolution(
    event_type, dm, factory
):
    client_id = await factory.create_client()
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, event_type, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.REGULAR in got[0]["segments"]


async def test_does_not_segment_in_business_as_regular_if_lt_3_accepted_events_in_last_90_days(  # noqa
    dm, factory
):
    client_id = await factory.create_client()
    for _ in range(2):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.REGULAR not in got[0]["segments"]


@pytest.mark.parametrize("event_type", [OrderEvent.ACCEPTED, OrderEvent.REJECTED])
async def test_segments_in_business_as_lost_if_processed_events_older_then_90_days(
    event_type, dm, factory
):
    client_id = await factory.create_client()
    await factory.create_resolved_order_events_pair(
        client_id, event_type=event_type, event_timestamp=dt("2018-11-03 00:00:01")
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST in got[0]["segments"]


async def test_segments_in_business_as_lost_if_only_rejected_events_in_last_90_days(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2019-11-03 00:00:01")
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST in got[0]["segments"]


async def test_segments_in_business_as_lost_if_call_events_older_then_90_days(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST in got[0]["segments"]


async def test_segments_in_business_as_lost_if_unprocessed_events_older_then_90_days(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST in got[0]["segments"]


async def test_does_not_segment_as_lost_for_history_if_currently_active_in_business(
    dm, factory
):
    client_id = await factory.create_client()
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

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST not in got[0]["segments"]


@pytest.mark.parametrize("recent_event_type", [OrderEvent.CREATED, CallEvent.INITIATED])
async def test_does_not_segment_as_lost_if_client_has_recent_call_or_order_created_event(  # noqa
    recent_event_type, dm, factory
):
    client_id = await factory.create_client()
    await factory.create_resolved_order_events_pair(
        client_id, OrderEvent.REJECTED, event_timestamp=dt("2019-11-03 00:00:01")
    )
    await factory.create_event(
        client_id,
        event_type=recent_event_type,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.LOST not in got[0]["segments"]


async def test_segments_in_business_as_unprocessed_orders_if_order_event_not_completed(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_order_event(client_id=client_id, event_type=OrderEvent.CREATED)

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.UNPROCESSED_ORDERS in got[0]["segments"]


async def test_does_not_segment_in_business_as_unprocessed_orders_for_call_events(
    dm, factory
):
    client_id = await factory.create_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.UNPROCESSED_ORDERS not in got[0]["segments"]


@pytest.mark.parametrize("event_value", ["Success", "Error", "fake-value"])
@pytest.mark.parametrize("talk_duration", [1, 9])
async def test_segments_as_short_last_call_if_last_call_is_answered_and_lt_10_sec(
    dm, factory, event_value, talk_duration
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_value=event_value,
        talk_duration=talk_duration,
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.SHORT_LAST_CALL in got[0]["segments"]


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
    client_id = await factory.create_client()
    for event_params in call_events:
        await factory.create_call_event(client_id=client_id, **event_params)

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.SHORT_LAST_CALL not in got[0]["segments"]


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

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.MISSED_LAST_CALL in got[0]["segments"]


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

    got = await dm.list_client_segments(passport_uid=456)

    assert SegmentType.MISSED_LAST_CALL not in got[0]["segments"]


async def test_does_not_considers_other_client_events_in_business(dm, factory):
    side_client_id = await factory.create_client(passport_uid=678)
    await factory.create_client(passport_uid=456)
    for event_type in OrderEvent:
        await factory.create_order_event(
            client_id=side_client_id, event_type=event_type
        )

    got = await dm.list_client_segments(passport_uid=456)

    assert len(got) == 1
    assert got[0]["segments"] == [SegmentType.NO_ORDERS]


async def test_does_not_considers_other_client_call_events_in_business(dm, factory):
    # active, no orders
    side_client_id = await factory.create_client(passport_uid=678)
    await factory.create_call_event(
        side_client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2019-11-03 00:00:01"),
    )
    # lost
    client_id = await factory.create_client(passport_uid=456)
    await factory.create_resolved_order_events_pair(
        client_id,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )

    got = await dm.list_client_segments(passport_uid=456)

    assert len(got) == 1
    assert got[0]["segments"] == [SegmentType.LOST]


async def test_raises_error_if_no_id_fields_passed(dm):
    with pytest.raises(NoClientIdFieldsPassed):
        await dm.list_client_segments(passport_uid=None, phone=None, email=None)
