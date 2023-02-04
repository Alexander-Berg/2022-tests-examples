import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


async def test_segments_as_no_order_clients_without_events(factory, dm):
    await factory.create_empty_client()
    await factory.create_empty_client()

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["current_size"] == 2


async def test_segments_as_no_order_clients_without_order_events(factory, dm):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["current_size"] == 1


@pytest.mark.parametrize("event_type", OrderEvent)
@pytest.mark.parametrize(
    "event_ts", (dt("2020-01-01 00:00:00"), dt("2018-01-01 00:00:00"))
)
async def test_skips_client_with_order_events(factory, dm, event_type, event_ts):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id=client_id, event_type=event_type, event_timestamp=event_ts
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["current_size"] == 0


async def test_skips_client_with_other_biz_id(factory, dm):
    await factory.create_client(biz_id=999)

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["current_size"] == 0


async def test_returns_previous_segment_size(factory, dm):
    # Was in segment, but left it
    client_id_1 = await factory.create_empty_client(created_at=dt("2019-09-10"))
    # Was in segment and still there
    await factory.create_empty_client(created_at=dt("2019-09-10"))

    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["previous_size"] == 2


async def test_skips_clients_created_later_for_previous(factory, dm):
    client_id_1 = await factory.create_empty_client(created_at=dt("2019-12-10"))
    await factory.create_empty_client(created_at=dt("2019-12-10"))

    await factory.create_order_event(
        client_id=client_id_1,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2019-12-20"),
    )

    _, got, _ = await dm.list_segments(123)

    assert got[SegmentType.NO_ORDERS]["previous_size"] == 0
