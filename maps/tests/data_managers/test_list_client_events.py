import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, CallHistoryEvent, Source
from maps_adv.geosmb.doorman.server.lib.exceptions import BadClientData, UnknownClient

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "event_params, expected",
    [
        # not finished call
        (
            dict(
                event_type=CallEvent.INITIATED,
            ),
            dict(
                type=CallHistoryEvent.INITIATED,
            ),
        ),
        # accepted call
        (
            dict(
                event_type=CallEvent.FINISHED,
                talk_duration=1,
            ),
            dict(type=CallHistoryEvent.ACCEPTED, accepted_details={"talk_duration": 1}),
        ),
        # missed call
        (
            dict(event_type=CallEvent.FINISHED, await_duration=333, talk_duration=0),
            dict(type=CallHistoryEvent.MISSED, missed_details={"await_duration": 333}),
        ),
    ],
)
async def test_returns_matched_event(factory, dm, event_params, expected):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        **event_params,
    )

    got = await dm.list_client_events(
        client_id=client_id, biz_id=123, datetime_from=None, datetime_to=None
    )

    assert got == dict(
        calls=[
            dict(
                source=Source.GEOADV_PHONE_CALL,
                timestamp=dt("2020-01-01 00:00:00"),
                **expected,
            )
        ],
        events_before=0,
        events_after=0,
    )


async def test_does_not_matches_init_event_if_call_has_finished_one(factory, dm):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.INITIATED,
        session_id=111,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        session_id=111,
        event_timestamp=dt("2020-02-02 00:00:00"),
    )

    got = await dm.list_client_events(
        client_id=client_id, biz_id=123, datetime_from=None, datetime_to=None
    )

    assert got == dict(
        calls=[
            dict(
                type=CallHistoryEvent.ACCEPTED,
                source=Source.GEOADV_PHONE_CALL,
                timestamp=dt("2020-02-02 00:00:00"),
                accepted_details={"talk_duration": 30},
            )
        ],
        events_before=0,
        events_after=0,
    )


async def test_matches_any_init_event_without_session_id(factory, dm):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.INITIATED,
        session_id=None,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        session_id=None,
        event_timestamp=dt("2020-02-02 00:00:00"),
    )

    got = await dm.list_client_events(
        client_id=client_id, biz_id=123, datetime_from=None, datetime_to=None
    )

    assert got == dict(
        calls=[
            dict(
                type=CallHistoryEvent.ACCEPTED,
                source=Source.GEOADV_PHONE_CALL,
                timestamp=dt("2020-02-02 00:00:00"),
                accepted_details={"talk_duration": 30},
            ),
            dict(
                type=CallHistoryEvent.INITIATED,
                source=Source.GEOADV_PHONE_CALL,
                timestamp=dt("2020-01-01 00:00:00"),
            ),
        ],
        events_before=0,
        events_after=0,
    )


@pytest.mark.parametrize(
    "datetime_from, datetime_to, expected_timestamps",
    [
        (
            dt("2020-01-02 00:00:00"),
            dt("2020-01-03 00:00:00"),
            [dt("2020-01-03 00:00:00"), dt("2020-01-02 00:00:00")],
        ),
        (
            None,
            dt("2020-01-03 00:00:00"),
            [
                dt("2020-01-03 00:00:00"),
                dt("2020-01-02 00:00:00"),
                dt("2020-01-01 23:59:59"),
            ],
        ),
        (
            dt("2020-01-02 00:00:00"),
            None,
            [
                dt("2020-01-03 00:00:01"),
                dt("2020-01-03 00:00:00"),
                dt("2020-01-02 00:00:00"),
            ],
        ),
        (
            None,
            None,
            [
                dt("2020-01-03 00:00:01"),
                dt("2020-01-03 00:00:00"),
                dt("2020-01-02 00:00:00"),
                dt("2020-01-01 23:59:59"),
            ],
        ),
    ],
)
async def test_returns_events_matched_to_search_interval(
    factory, dm, datetime_from, datetime_to, expected_timestamps
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_timestamp=dt("2020-01-01 23:59:59"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_timestamp=dt("2020-01-02 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_timestamp=dt("2020-01-03 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_timestamp=dt("2020-01-03 00:00:01"),
    )

    got = await dm.list_client_events(
        client_id=client_id,
        biz_id=123,
        datetime_from=datetime_from,
        datetime_to=datetime_to,
    )

    assert list(map(lambda e: e["timestamp"], got["calls"])) == expected_timestamps


@pytest.mark.parametrize(
    "datetime_from, datetime_to, expected_events_before, expected_events_after",
    [
        (dt("2020-01-02 00:00:00"), dt("2020-01-03 00:00:00"), 1, 2),
        # no events in interval
        (dt("2020-01-02 12:00:00"), dt("2020-01-02 13:00:00"), 2, 3),
        (None, dt("2020-01-03 00:00:00"), 0, 2),
        (dt("2020-01-02 00:00:00"), None, 1, 0),
        (None, None, 0, 0),
    ],
)
async def test_returns_count_of_events_not_matched_to_search_interval(
    factory,
    dm,
    datetime_from,
    datetime_to,
    expected_events_before,
    expected_events_after,
):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-01 23:59:59")
    )
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-02 00:00:00")
    )
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-03 00:00:00")
    )
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-03 00:00:01")
    ),
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-04 00:00:01")
    )

    got = await dm.list_client_events(
        client_id=client_id,
        biz_id=123,
        datetime_from=datetime_from,
        datetime_to=datetime_to,
    )

    assert got["events_before"] == expected_events_before
    assert got["events_after"] == expected_events_after


@pytest.mark.real_db
async def test_returns_events_sorted_by_timestamp(factory, dm):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-02 00:00:00")
    )
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-01 00:00:00")
    )
    await factory.create_call_event(
        client_id=client_id, event_timestamp=dt("2020-01-03 00:00:00")
    )

    got = await dm.list_client_events(
        client_id=client_id, biz_id=123, datetime_from=None, datetime_to=None
    )

    assert list(map(lambda e: e["timestamp"], got["calls"])) == [
        dt("2020-01-03 00:00:00"),
        dt("2020-01-02 00:00:00"),
        dt("2020-01-01 00:00:00"),
    ]


async def test_returns_empty_list_if_no_events(factory, dm):
    client_id = await factory.create_client()

    got = await dm.list_client_events(
        client_id=client_id, biz_id=123, datetime_from=None, datetime_to=None
    )

    assert got == dict(calls=[], events_before=0, events_after=0)


@pytest.mark.parametrize("client_id, biz_id", [(111, 999), (999, 123)])
async def test_raises_for_unknown_client(factory, dm, client_id, biz_id):
    await factory.create_client(client_id=client_id, biz_id=biz_id)

    with pytest.raises(UnknownClient):
        await dm.list_client_events(
            client_id=111, biz_id=123, datetime_from=None, datetime_to=None
        )


async def test_raises_for_invalid_datetime_interval(factory, dm):
    client_id = await factory.create_client()

    with pytest.raises(BadClientData) as exc:
        await dm.list_client_events(
            client_id=client_id,
            biz_id=123,
            datetime_from=dt("2020-02-02 00:00:00"),
            datetime_to=dt("2020-01-01 00:00:00"),
        )

    assert exc.value.args == (
        "datetime_from=2020-02-02 00:00:00+00:00 must be "
        "less or equal than datetime_to=2020-01-01 00:00:00+00:00",
    )


async def test_does_not_match_events_of_different_clients(factory, dm):
    await factory.create_client(client_id=1, biz_id=111)
    await factory.create_client(client_id=2, biz_id=222)
    await factory.create_client(client_id=3, biz_id=333)

    await factory.create_call_event(
        client_id=1, event_timestamp=dt("2020-01-01 00:00:00")
    )
    await factory.create_call_event(
        client_id=2, event_timestamp=dt("2020-02-02 00:00:00")
    )
    await factory.create_call_event(
        client_id=3,
        event_timestamp=dt("2020-03-03 00:00:00"),
        event_type=CallEvent.FINISHED,
        event_value=CallHistoryEvent.ACCEPTED.name,
    )

    got = await dm.list_client_events(
        client_id=2, biz_id=222, datetime_from=None, datetime_to=None
    )

    assert list(map(lambda e: e["timestamp"], got["calls"])) == [
        dt("2020-02-02 00:00:00")
    ]
