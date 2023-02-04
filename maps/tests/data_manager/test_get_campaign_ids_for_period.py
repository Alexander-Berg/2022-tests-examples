import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.no_setup_ch]


@pytest.mark.usefixtures("fill_raw_table")
async def test_returns_campaign_ids(dm):
    got = await dm.get_campaign_ids_for_period(
        start_time=dt("2020-07-27 00:00:00"), end_time=dt("2020-07-27 00:15:00")
    )

    assert got == [111, 222, 333, 444, 555]


@pytest.mark.usefixtures("fill_raw_table")
async def test_does_not_return_campaign_ids_if_no_in_range(dm):
    got = await dm.get_campaign_ids_for_period(
        start_time=dt("2020-07-27 01:00:00"), end_time=dt("2020-07-27 01:15:00")
    )

    assert got == []
