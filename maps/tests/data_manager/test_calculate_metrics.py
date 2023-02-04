import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.no_setup_ch]


@pytest.mark.usefixtures("fill_raw_table")
async def test_returns_metrics(dm):
    got = await dm.calculate_metrics(
        start_time=dt("2020-07-27 00:00:00"), end_time=dt("2020-07-27 00:15:00")
    )

    assert got == {"clicks": 3, "shows": 11, "users": 9}


@pytest.mark.usefixtures("fill_raw_table")
async def test_does_not_return_metrics_if_no_in_range(dm):
    got = await dm.calculate_metrics(
        start_time=dt("2020-07-27 01:00:00"), end_time=dt("2020-07-27 01:15:00")
    )

    assert got == {"clicks": 0, "shows": 0, "users": 0}


@pytest.mark.usefixtures("fill_raw_table")
async def test_returns_metrics_for_campaigns(dm):
    got = await dm.calculate_metrics(
        start_time=dt("2020-07-27 00:00:00"),
        end_time=dt("2020-07-27 00:15:00"),
        campaign_ids=[111, 222],
    )

    assert got == {"clicks": 1, "shows": 3, "users": 3}
