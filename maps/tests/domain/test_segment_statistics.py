import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time("2020-01-01 13:45:00"),
]


async def test_returns_empty_statistics_if_there_are_nothing(dm, domain):
    dm.segment_statistics.coro.return_value = {}

    got = await domain.segment_statistics(biz_id=123, segment_type=SegmentType.ACTIVE)

    assert got == {}


async def test_returns_calculated_statistics(dm, domain):
    dm.segment_statistics.coro.return_value = {
        dt("2020-01-01 00:00:00"): 100500,
        dt("2020-02-01 00:00:00"): 200500,
        dt("2020-03-01 00:00:00"): 300500,
        dt("2020-04-01 00:00:00"): 400500,
        dt("2020-05-01 00:00:00"): 500500,
    }

    got = await domain.segment_statistics(biz_id=123, segment_type=SegmentType.ACTIVE)

    assert got == {
        dt("2020-01-01 00:00:00"): 100500,
        dt("2020-02-01 00:00:00"): 200500,
        dt("2020-03-01 00:00:00"): 300500,
        dt("2020-04-01 00:00:00"): 400500,
        dt("2020-05-01 00:00:00"): 500500,
    }


async def test_calls_dm_for_current_datetime(dm, domain):
    dm.segment_statistics.coro.return_value = {}

    await domain.segment_statistics(biz_id=123, segment_type=SegmentType.ACTIVE)

    dm.segment_statistics.assert_called_with(
        biz_id=123,
        segment_type=SegmentType.ACTIVE,
        on_datetime=dt("2020-01-01 13:45:00"),
    )
