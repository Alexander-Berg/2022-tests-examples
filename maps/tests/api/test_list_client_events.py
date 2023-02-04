import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto.common_pb2 import Source
from maps_adv.geosmb.doorman.proto.errors_pb2 import Error
from maps_adv.geosmb.doorman.proto.events_pb2 import (
    ClientHistory,
    HistoryCallEvent,
    RetrieveClientCalls,
)
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent as CallEventEnum

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "v1/list_client_events/"


@pytest.mark.parametrize(
    "event_params, expected",
    [
        # not finished call
        (
            dict(event_type=CallEventEnum.INITIATED),
            dict(type=HistoryCallEvent.INITIATED),
        ),
        # accepted call
        (
            dict(event_type=CallEventEnum.FINISHED, talk_duration=1),
            dict(
                type=HistoryCallEvent.ACCEPTED,
                accepted_details=HistoryCallEvent.AcceptedDetails(talk_duration=1),
            ),
        ),
        # missed call
        (
            dict(
                event_type=CallEventEnum.FINISHED, await_duration=333, talk_duration=0
            ),
            dict(
                type=HistoryCallEvent.MISSED,
                missed_details=HistoryCallEvent.MissedDetails(await_duration=333),
            ),
        ),
    ],
)
async def test_returns_matched_event(factory, api, event_params, expected):
    client_id = await factory.create_client()
    await factory.create_call_event(client_id=client_id, **event_params)

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=123),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert got == ClientHistory(
        calls=[
            HistoryCallEvent(
                timestamp=dt("2020-01-01 00:00:00", as_proto=True),
                source=Source.GEOADV_PHONE_CALL,
                **expected,
            )
        ],
        events_before=0,
        events_after=0,
    )


async def test_does_not_matches_init_event_if_call_has_finished_one(factory, api):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEventEnum.INITIATED,
        session_id=111,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEventEnum.FINISHED,
        session_id=111,
        event_timestamp=dt("2020-02-02 00:00:00"),
    )

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=123),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert got == ClientHistory(
        calls=[
            HistoryCallEvent(
                type=HistoryCallEvent.ACCEPTED,
                timestamp=dt("2020-02-02 00:00:00", as_proto=True),
                source=Source.GEOADV_PHONE_CALL,
                accepted_details=HistoryCallEvent.AcceptedDetails(talk_duration=30),
            )
        ],
        events_before=0,
        events_after=0,
    )


async def test_matches_any_init_event_without_session_id(factory, api):
    client_id = await factory.create_client()
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEventEnum.INITIATED,
        session_id=None,
        event_timestamp=dt("2020-01-01 00:00:00"),
    )
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEventEnum.FINISHED,
        session_id=None,
        event_timestamp=dt("2020-02-02 00:00:00"),
    )

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=123),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert got == ClientHistory(
        calls=[
            HistoryCallEvent(
                type=HistoryCallEvent.ACCEPTED,
                timestamp=dt("2020-02-02 00:00:00", as_proto=True),
                source=Source.GEOADV_PHONE_CALL,
                accepted_details=HistoryCallEvent.AcceptedDetails(talk_duration=30),
            ),
            HistoryCallEvent(
                type=HistoryCallEvent.INITIATED,
                timestamp=dt("2020-01-01 00:00:00", as_proto=True),
                source=Source.GEOADV_PHONE_CALL,
            ),
        ],
        events_before=0,
        events_after=0,
    )


@pytest.mark.parametrize(
    "datetime_from, datetime_to, expected_timestamps",
    [
        (
            dt("2020-01-02 00:00:00", as_proto=True),
            dt("2020-01-03 00:00:00", as_proto=True),
            [
                dt("2020-01-03 00:00:00", as_proto=True),
                dt("2020-01-02 00:00:00", as_proto=True),
            ],
        ),
        (
            None,
            dt("2020-01-03 00:00:00", as_proto=True),
            [
                dt("2020-01-03 00:00:00", as_proto=True),
                dt("2020-01-02 00:00:00", as_proto=True),
                dt("2020-01-01 23:59:59", as_proto=True),
            ],
        ),
        (
            dt("2020-01-02 00:00:00", as_proto=True),
            None,
            [
                dt("2020-01-03 00:00:01", as_proto=True),
                dt("2020-01-03 00:00:00", as_proto=True),
                dt("2020-01-02 00:00:00", as_proto=True),
            ],
        ),
        (
            None,
            None,
            [
                dt("2020-01-03 00:00:01", as_proto=True),
                dt("2020-01-03 00:00:00", as_proto=True),
                dt("2020-01-02 00:00:00", as_proto=True),
                dt("2020-01-01 23:59:59", as_proto=True),
            ],
        ),
    ],
)
async def test_returns_events_matched_to_search_interval(
    factory, api, datetime_from, datetime_to, expected_timestamps
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
    )

    got = await api.post(
        url,
        proto=RetrieveClientCalls(
            client_id=client_id,
            biz_id=123,
            datetime_from=datetime_from,
            datetime_to=datetime_to,
        ),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert list(map(lambda e: e.timestamp, got.calls)) == expected_timestamps


@pytest.mark.parametrize(
    "datetime_from, datetime_to, expected_events_before, expected_events_after",
    [
        (
            dt("2020-01-02 00:00:00", as_proto=True),
            dt("2020-01-03 00:00:00", as_proto=True),
            1,
            2,
        ),
        # no events in interval
        (
            dt("2020-01-02 12:00:00", as_proto=True),
            dt("2020-01-02 13:00:00", as_proto=True),
            2,
            3,
        ),
        (None, dt("2020-01-03 00:00:00", as_proto=True), 0, 2),
        (dt("2020-01-02 00:00:00", as_proto=True), None, 1, 0),
        (None, None, 0, 0),
    ],
)
async def test_returns_count_of_events_not_matched_to_search_interval(
    factory,
    api,
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

    got = await api.post(
        url,
        proto=RetrieveClientCalls(
            client_id=client_id,
            biz_id=123,
            datetime_from=datetime_from,
            datetime_to=datetime_to,
        ),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert got.events_before == expected_events_before
    assert got.events_after == expected_events_after


@pytest.mark.real_db
async def test_returns_events_sorted_by_timestamp(factory, api):
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

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=123),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert list(map(lambda e: e.timestamp, got.calls)) == [
        dt("2020-01-03 00:00:00", as_proto=True),
        dt("2020-01-02 00:00:00", as_proto=True),
        dt("2020-01-01 00:00:00", as_proto=True),
    ]


async def test_returns_empty_list_if_no_events(factory, api):
    client_id = await factory.create_client()

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=123),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert got == ClientHistory(calls=[], events_before=0, events_after=0)


@pytest.mark.parametrize("client_id, biz_id", [(111, 999), (999, 123)])
async def test_returns_error_for_unknown_client(factory, api, client_id, biz_id):
    await factory.create_client()

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=client_id, biz_id=biz_id),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id={biz_id}, id={client_id}",
    )


async def test_returns_error_for_invalid_datetime_interval(factory, api):
    client_id = await factory.create_client()

    got = await api.post(
        url,
        proto=RetrieveClientCalls(
            client_id=client_id,
            biz_id=123,
            datetime_from=dt("2020-02-02 00:00:00", as_proto=True),
            datetime_to=dt("2020-01-01 00:00:00", as_proto=True),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="datetime_from=2020-02-02 00:00:00+00:00 must be "
        "less or equal than datetime_to=2020-01-01 00:00:00+00:00",
    )


async def test_does_not_match_events_of_different_clients(factory, api):
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
        client_id=3, event_timestamp=dt("2020-03-03 00:00:00")
    )

    got = await api.post(
        url,
        proto=RetrieveClientCalls(client_id=2, biz_id=222),
        decode_as=ClientHistory,
        expected_status=200,
    )

    assert list(map(lambda e: e.timestamp, got.calls)) == [
        dt("2020-02-02 00:00:00", as_proto=True)
    ]
