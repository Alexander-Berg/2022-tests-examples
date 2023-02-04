import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2
from maps_adv.geosmb.doorman.server.lib.enums import OrderEvent
from maps_adv.geosmb.doorman.server.tests.api.segment_statistics import stat_el

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]

url = "v1/segment_statistics/"


async def test_returns_statistics(api, factory):
    """Includes client with three or more events."""
    with freeze_time(dt("2020-05-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(3):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-05-05 12:00:00"),
        )

    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=123, segment_type=segments_pb2.SegmentType.REGULAR
        ),
        decode_as=segments_pb2.SegmentStatisticsOutput,
        expected_status=200,
    )

    assert got == segments_pb2.SegmentStatisticsOutput(
        statistics=[
            stat_el("2020-01-31 00:00:00", 0),
            stat_el("2020-02-29 00:00:00", 0),
            stat_el("2020-03-31 00:00:00", 0),
            stat_el("2020-04-30 00:00:00", 0),
            stat_el("2020-05-31 00:00:00", 1),
            stat_el("2020-06-30 00:00:00", 1),
            stat_el("2020-07-31 00:00:00", 1),
            stat_el("2020-08-31 00:00:00", 0),
            stat_el("2020-09-30 00:00:00", 0),
            stat_el("2020-10-31 00:00:00", 0),
            stat_el("2020-11-30 00:00:00", 0),
            stat_el("2020-12-31 00:00:00", 0),
            stat_el("2021-01-31 00:00:00", 0),
        ],
    )


async def test_not_includes_client_with_lt_3_events(api, factory):
    with freeze_time(dt("2020-05-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(2):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-05-05 12:00:00"),
        )

    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=123, segment_type=segments_pb2.SegmentType.REGULAR
        ),
        decode_as=segments_pb2.SegmentStatisticsOutput,
        expected_status=200,
    )

    assert set([el.size for el in got.statistics]) == {0}


@pytest.mark.parametrize("resolution", (OrderEvent.ACCEPTED, OrderEvent.REJECTED, None))
async def test_not_depends_on_event_resolution(api, factory, resolution):
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

    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=123, segment_type=segments_pb2.SegmentType.REGULAR
        ),
        decode_as=segments_pb2.SegmentStatisticsOutput,
        expected_status=200,
    )

    el = next(
        filter(
            lambda el: el.timestamp == dt("2020-01-31 00:00:00", as_proto=True),
            got.statistics,
        )
    )
    assert el.size == 1


async def test_counts_clients_not_events(api, factory):
    with freeze_time(dt("2020-01-05 12:00:00")):
        client_id = await factory.create_empty_client()
    for _ in range(6):
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-01-05 12:00:00"),
        )

    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=123, segment_type=segments_pb2.SegmentType.REGULAR
        ),
        decode_as=segments_pb2.SegmentStatisticsOutput,
        expected_status=200,
    )

    el = next(
        filter(
            lambda el: el.timestamp == dt("2020-01-31 00:00:00", as_proto=True),
            got.statistics,
        )
    )
    assert el.size == 1
