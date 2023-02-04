import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import OrderEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]


async def test_returns_statistics(dm, factory):
    """Includes client with two or more events."""
    with freeze_time(dt("2020-05-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-05-05 12:00:00"),
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.REGULAR,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got == {
        dt("2020-01-31 00:00:00"): 0,
        dt("2020-02-29 00:00:00"): 0,
        dt("2020-03-31 00:00:00"): 0,
        dt("2020-04-30 00:00:00"): 0,
        dt("2020-05-31 00:00:00"): 1,
        dt("2020-06-30 00:00:00"): 1,
        dt("2020-07-31 00:00:00"): 1,
        dt("2020-08-31 00:00:00"): 0,
        dt("2020-09-30 00:00:00"): 0,
        dt("2020-10-31 00:00:00"): 0,
        dt("2020-11-30 00:00:00"): 0,
        dt("2020-12-31 00:00:00"): 0,
        dt("2021-01-31 00:00:00"): 0,
    }


async def test_not_includes_client_with_lt_3_events(dm, factory):
    with freeze_time(dt("2020-05-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-05-05 12:00:00"),
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.REGULAR,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert set(got.values()) == {0}


@pytest.mark.parametrize("resolution", (OrderEvent.ACCEPTED, OrderEvent.REJECTED, None))
async def test_not_depends_on_event_resolution(dm, factory, resolution):
    with freeze_time(dt("2020-01-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )
        if resolution:
            await factory.create_order_event(
                client_id=client_id,
                event_type=resolution,
                event_timestamp=dt("2020-01-05 12:00:00"),
            )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.REGULAR,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-01-31 00:00:00")] == 1


async def test_counts_clients_not_events(dm, factory):
    with freeze_time(dt("2020-01-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(6):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=SegmentType.REGULAR,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got[dt("2020-01-31 00:00:00")] == 1
