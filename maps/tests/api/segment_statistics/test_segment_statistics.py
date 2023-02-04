import pytest

from maps_adv.geosmb.doorman.proto import errors_pb2, segments_pb2
from maps_adv.geosmb.doorman.server.tests.api.segment_statistics import stat_el

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]

url = "v1/segment_statistics/"


@pytest.mark.parametrize(
    "segment_type",
    (
        "REGULAR",
        "ACTIVE",
        "LOST",
        "NO_ORDERS",
        "UNPROCESSED_ORDERS",
        "MISSED_LAST_CALL",
        "SHORT_LAST_CALL",
    ),
)
async def test_returns_empty_slots_if_there_are_no_statistics(segment_type, api):
    segment_type = segments_pb2.SegmentType.Value(segment_type)

    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=123, segment_type=segment_type
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
            stat_el("2020-05-31 00:00:00", 0),
            stat_el("2020-06-30 00:00:00", 0),
            stat_el("2020-07-31 00:00:00", 0),
            stat_el("2020-08-31 00:00:00", 0),
            stat_el("2020-09-30 00:00:00", 0),
            stat_el("2020-10-31 00:00:00", 0),
            stat_el("2020-11-30 00:00:00", 0),
            stat_el("2020-12-31 00:00:00", 0),
            stat_el("2021-01-31 00:00:00", 0),
        ],
    )


async def test_errored_for_wrong_input(api):
    got = await api.post(
        url,
        proto=segments_pb2.SegmentStatisticsInput(
            biz_id=0, segment_type=segments_pb2.SegmentType.ACTIVE
        ),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.VALIDATION_ERROR,
        description="biz_id: ['Must be at least 1.']",
    )
