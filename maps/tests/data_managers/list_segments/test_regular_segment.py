import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2020-01-01 00:00:01")]


@pytest.mark.parametrize(
    "event_ts",
    (dt("2021-01-01 00:00:00"), dt("2020-01-01 00:00:00"), dt("2019-10-03 00:00:01")),
)
async def test_segments_as_regular_clients_with_3_or_more_recent_created_order_events(
    factory, dm, event_ts
):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id, event_type=OrderEvent.CREATED, event_timestamp=event_ts
        )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 1


@pytest.mark.parametrize(
    "irrelevant_event_type",
    (OrderEvent.ACCEPTED, OrderEvent.REJECTED, CallEvent.INITIATED),
)
async def test_segments_client_as_regular_regardless_irrelevant_events(
    factory, dm, irrelevant_event_type
):
    client_id = await factory.create_empty_client()
    await factory.create_event(
        client_id=client_id,
        event_type=irrelevant_event_type,
        event_timestamp=dt("2019-10-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-10-03 00:00:01"),
        )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 1


async def test_counts_several_clients_as_expected(factory, dm):
    await factory.create_empty_client(segments={SegmentType.REGULAR})
    await factory.create_empty_client(segments={SegmentType.REGULAR})

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 2


async def test_matches_each_client_only_once(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.add_client_to_regular_segment(client_id)
    await factory.add_client_to_regular_segment(client_id)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 1


@pytest.mark.parametrize(
    "event_type", [OrderEvent.ACCEPTED, OrderEvent.REJECTED, CallEvent.INITIATED]
)
async def test_skips_clients_with_irrelevant_events(factory, dm, event_type):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_event(client_id=client_id, event_type=event_type)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 0


async def test_skips_other_business_clients(factory, dm):
    client_id = await factory.create_empty_client(biz_id=999)
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id, event_type=OrderEvent.CREATED
        )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 0


async def test_skips_clients_with_old_events(factory, dm):
    client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2019-10-03 00:00:00"),
        )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 0


async def test_skips_clients_if_not_enough_relevant_events(factory, dm):
    client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.CREATED)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    client_id_1 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_2 = await factory.create_empty_client()  # Was in segment, but left it
    client_id_3 = await factory.create_empty_client()  # Was in segment and still there

    for ts in [
        dt("2019-09-10 00:00:00"),
        dt("2019-09-15 00:00:00"),
        dt("2019-09-20 00:00:00"),
    ]:
        await factory.create_order_event(
            client_id=client_id_1,
            event_type=OrderEvent.CREATED,
            event_timestamp=ts,
        )
    for ts in [
        dt("2019-09-10 00:00:00"),
        dt("2019-10-10 00:00:00"),
        dt("2019-10-20 00:00:00"),
    ]:
        await factory.create_order_event(
            client_id=client_id_2,
            event_type=OrderEvent.CREATED,
            event_timestamp=ts,
        )
    for ts in [
        dt("2019-11-10 00:00:00"),
        dt("2019-11-15 00:00:00"),
        dt("2019-11-20 00:00:00"),
    ]:
        await factory.create_order_event(
            client_id=client_id_3,
            event_type=OrderEvent.CREATED,
            event_timestamp=ts,
        )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.REGULAR]["previous_size"] == 3
