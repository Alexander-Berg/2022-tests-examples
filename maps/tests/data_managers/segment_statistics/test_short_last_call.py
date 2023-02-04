from datetime import datetime

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]


@pytest.fixture
def add_client(factory):
    async def _add_client(
        on_dt: datetime, event: CallEvent = CallEvent.FINISHED, **kwargs
    ) -> int:
        with freeze_time(on_dt):
            client_id = await factory.create_empty_client()
            await factory.create_call_event(
                client_id=client_id,
                event_type=event,
                event_timestamp=on_dt,
                **kwargs,
            )
        return client_id

    return _add_client


async def test_returns_statistics(dm, add_client):
    await add_client(dt("2020-04-10 15:00:00"), talk_duration=1)
    await add_client(dt("2020-09-05 23:00:00"), talk_duration=9)

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.SHORT_LAST_CALL,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got == {
        dt("2020-01-31 00:00:00"): 0,
        dt("2020-02-29 00:00:00"): 0,
        dt("2020-03-31 00:00:00"): 0,
        dt("2020-04-30 00:00:00"): 1,
        dt("2020-05-31 00:00:00"): 1,
        dt("2020-06-30 00:00:00"): 1,
        dt("2020-07-31 00:00:00"): 1,
        dt("2020-08-31 00:00:00"): 1,
        dt("2020-09-30 00:00:00"): 2,
        dt("2020-10-31 00:00:00"): 2,
        dt("2020-11-30 00:00:00"): 2,
        dt("2020-12-31 00:00:00"): 2,
        dt("2021-01-31 00:00:00"): 2,
    }


async def test_not_includes_client_with_long_last_call(dm, add_client, factory):
    client_id = await add_client(dt("2020-04-10 15:00:00"), talk_duration=1)
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_timestamp=dt("2020-04-10 15:10:00"),
        talk_duration=10,
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.SHORT_LAST_CALL,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 0


async def test_includes_client_with_long_old_call(dm, add_client, factory):
    client_id = await add_client(dt("2020-04-10 15:00:00"), talk_duration=1)
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.FINISHED,
        event_timestamp=dt("2020-04-10 14:59:00"),
        talk_duration=10,
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.SHORT_LAST_CALL,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 1


async def test_not_includes_client_with_last_unfinished_call(dm, add_client, factory):
    client_id = await add_client(dt("2020-04-10 15:00:00"), talk_duration=1)
    await factory.create_call_event(
        client_id=client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2020-04-10 15:01:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.SHORT_LAST_CALL,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 0


async def test_counts_clients_not_events(dm, factory):
    with freeze_time(dt("2020-04-10 15:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_call_event(
            client_id=client_id,
            event_type=CallEvent.FINISHED,
            event_timestamp=dt("2020-04-10 15:00:00"),
            talk_duration=1,
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.SHORT_LAST_CALL,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 1
