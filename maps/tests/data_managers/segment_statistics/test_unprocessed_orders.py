from datetime import datetime

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import OrderEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]


@pytest.fixture
def add_client(factory):
    async def _add_client(on_dt: datetime) -> int:
        with freeze_time(on_dt):
            client_id = await factory.create_empty_client()
            await factory.create_order_event(
                client_id=client_id,
                event_type=OrderEvent.CREATED,
                event_timestamp=on_dt,
            )
        return client_id

    return _add_client


async def test_returns_statistics(dm, add_client):
    await add_client(dt("2020-04-10 15:00:00"))
    await add_client(dt("2020-09-05 23:00:00"))

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.UNPROCESSED_ORDERS,
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


@pytest.mark.parametrize("resolution", (OrderEvent.ACCEPTED, OrderEvent.REJECTED))
async def test_slot_includes_clients_with_resolved_events_if_they_has_unresolved(
    dm, factory, add_client, resolution
):
    client_id = await add_client(dt("2020-04-05 00:00:00"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2020-04-05 00:00:00"),
    )
    await factory.create_order_event(
        client_id=client_id,
        event_type=resolution,
        event_timestamp=dt("2020-04-05 00:00:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.UNPROCESSED_ORDERS,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 1


async def test_slot_not_includes_event_from_future(dm, factory):
    with freeze_time(dt("2020-03-15 00:00:00")):
        client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2020-04-05 00:00:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.UNPROCESSED_ORDERS,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-03-31 00:00:00")] == 0


@pytest.mark.parametrize("resolution", (OrderEvent.ACCEPTED, OrderEvent.REJECTED))
async def test_slot_not_includes_client_with_resolved_events(
    dm, factory, add_client, resolution
):
    client_id = await add_client(dt("2020-04-30 00:00:00"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=resolution,
        event_timestamp=dt("2020-04-30 00:00:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.UNPROCESSED_ORDERS,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert set(got.values()) == {0}


async def test_counts_clients_not_events(dm, add_client, factory):
    client_id = await add_client(dt("2020-04-05 00:00:00"))
    await factory.create_order_event(
        client_id=client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2020-04-05 00:00:00"),
    )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.UNPROCESSED_ORDERS,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-04-30 00:00:00")] == 1
