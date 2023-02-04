import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]


async def test_returns_aggregate_processed_events(dm):
    got = await dm.get_aggregated_processed_events_by_campaign(
        start_time=dt("2019-01-01 00:00:00"), end_time=dt("2019-01-02 00:00:00")
    )

    assert got == {
        10: {"action_make_route": 1, "billboard_show": 7},
        20: {"action_make_route": 0, "billboard_show": 5},
        30: {"action_make_route": 0, "billboard_show": 3},
    }


async def test_returns_aggregate_normalized_events(dm):
    got = await dm.get_aggregated_normalized_events_by_campaign(
        start_time=dt("2019-01-01 00:00:00"), end_time=dt("2019-01-02 00:00:00")
    )

    assert got == {
        10: {"action_make_route": 1, "billboard_show": 7},
        20: {"action_make_route": 0, "billboard_show": 5},
        30: {"action_make_route": 0, "billboard_show": 3},
    }


async def test_returns_aggregate_mapkit_events(dm):
    got = await dm.get_aggregated_normalized_events_by_campaign(
        start_time=dt("2019-01-01 00:00:00"), end_time=dt("2019-01-02 00:00:00")
    )

    assert got == {
        10: {"action_make_route": 1, "billboard_show": 7},
        20: {"action_make_route": 0, "billboard_show": 5},
        30: {"action_make_route": 0, "billboard_show": 3},
    }
