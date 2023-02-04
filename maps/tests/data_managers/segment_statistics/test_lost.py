from datetime import datetime

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]


@pytest.fixture
def add_client(factory):
    async def _add_client(on_dt: datetime) -> int:
        with freeze_time(on_dt):
            client_id = await factory.create_empty_client()
            await factory.create_resolved_order_events_pair(
                client_id=client_id,
                event_type=OrderEvent.ACCEPTED,
                event_timestamp=on_dt,
            )
        return client_id

    return _add_client


async def test_returns_statistics(dm, add_client):
    await add_client(dt("2020-04-10 15:00:00"))
    await add_client(dt("2020-09-05 23:00:00"))

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got == {
        dt("2020-01-31 00:00:00"): 0,
        dt("2020-02-29 00:00:00"): 0,
        dt("2020-03-31 00:00:00"): 0,
        dt("2020-04-30 00:00:00"): 0,
        dt("2020-05-31 00:00:00"): 0,
        dt("2020-06-30 00:00:00"): 0,
        dt("2020-07-31 00:00:00"): 1,
        dt("2020-08-31 00:00:00"): 1,
        dt("2020-09-30 00:00:00"): 1,
        dt("2020-10-31 00:00:00"): 1,
        dt("2020-11-30 00:00:00"): 1,
        dt("2020-12-31 00:00:00"): 2,
        dt("2021-01-31 00:00:00"): 2,
    }


async def test_not_includes_client_with_recent_calls(dm, add_client, factory):
    client_id = await add_client(dt("2020-01-05 12:00:00"))
    await factory.create_event(
        client_id=client_id,
        event_type=CallEvent.INITIATED,
        event_timestamp=dt("2020-04-05 12:00:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 0


async def test_includes_client_with_old_call(dm, add_client, factory):
    with freeze_time(dt("2020-01-05 12:00:00")):
        client_id = await factory.create_empty_client()
        await factory.create_event(
            client_id=client_id,
            event_type=CallEvent.INITIATED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 1


async def test_does_not_include_client_without_events(dm, factory):
    await factory.create_empty_client()

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert set(got.values()) == {0}


async def test_slot_not_includes_client_with_unresolved_event(dm, factory, add_client):
    client_id = await add_client(dt("2020-01-05 15:00:00"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2021-01-19 19:31:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2021-01-31 00:00:00")] == 0


async def test_slots_in_past_includes_client_with_currently_unresolved_event(
    dm, factory, add_client
):
    client_id = await add_client(dt("2020-01-05 15:00:00"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2021-01-19 19:31:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-12-31 00:00:00")] == 1


async def test_slot_not_includes_client_with_accepted_event(dm, factory, add_client):
    client_id = await add_client(dt("2020-01-05 15:00:00"))
    await factory.create_resolved_order_events_pair(
        client_id=client_id,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2021-01-19 19:31:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2021-01-31 00:00:00")] == 0


async def test_slots_in_past_includes_client_with_currently_accepted_event(
    dm, factory, add_client
):
    client_id = await add_client(dt("2020-01-05 15:00:00"))
    await factory.create_resolved_order_events_pair(
        client_id=client_id,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=dt("2021-01-19 19:31:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-12-31 00:00:00")] == 1


async def test_slot_not_includes_client_from_future(dm, factory, add_client):
    await add_client(dt("2020-04-05 15:00:00"))

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-03-31 00:00:00")] == 0


async def test_counts_clients_not_events(dm, factory):
    with freeze_time(dt("2020-01-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_resolved_order_events_pair(
            client_id=client_id,
            event_type=OrderEvent.ACCEPTED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )
        await factory.create_event(
            client_id=client_id,
            event_type=CallEvent.INITIATED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.LOST,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2021-01-31 00:00:00")] == 1
