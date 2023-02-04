import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.server.lib.data_manager import (
    NoCampaignsPassed,
    NothingFound,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.real_db()
@pytest.mark.usefixtures("fill_category_search_report_table")
async def test_returns_expected_data_for_one_campaign(dm):
    got = await dm.fetch_search_icons_statistics(
        campaign_ids=[1],
        period_from=dt("2019-12-01 00:00:00"),
        period_to=dt("2019-12-12 23:59:59"),
    )

    assert got == [
        dict(
            date="2019-12-10",
            icon_shows=1,
            icon_clicks=1000,
            unique_icon_shows=1,
            pin_shows=1000,
            pin_clicks=1,
            routes=10,
        ),
        dict(
            date="2019-12-02",
            icon_shows=100,
            icon_clicks=10,
            unique_icon_shows=100,
            pin_shows=1,
            pin_clicks=10,
            routes=1000,
        ),
        dict(
            date="2019-12-01",
            icon_shows=10,
            icon_clicks=1,
            unique_icon_shows=1000,
            pin_shows=10,
            pin_clicks=100,
            routes=100,
        ),
        dict(
            date=None,
            icon_shows=111,
            icon_clicks=1011,
            unique_icon_shows=None,
            pin_shows=1011,
            pin_clicks=111,
            routes=1110,
        ),
    ]


@pytest.mark.usefixtures("fill_category_search_report_table")
async def test_returns_expected_data_for_list_of_campaigns(dm):
    got = await dm.fetch_search_icons_statistics(
        campaign_ids=[1, 2, 100],
        period_from=dt("2019-12-01 00:00:00"),
        period_to=dt("2019-12-12 23:59:59"),
    )

    assert got == [
        dict(
            date="2019-12-10",
            icon_shows=1,
            icon_clicks=1000,
            unique_icon_shows=None,
            pin_shows=1000,
            pin_clicks=1,
            routes=10,
        ),
        dict(
            date="2019-12-03",
            icon_shows=2000,
            icon_clicks=200,
            unique_icon_shows=None,
            pin_shows=2,
            pin_clicks=2000,
            routes=2,
        ),
        dict(
            date="2019-12-02",
            icon_shows=300,
            icon_clicks=30,
            unique_icon_shows=None,
            pin_shows=3,
            pin_clicks=30,
            routes=3000,
        ),
        dict(
            date="2019-12-01",
            icon_shows=30,
            icon_clicks=3,
            unique_icon_shows=None,
            pin_shows=30,
            pin_clicks=300,
            routes=300,
        ),
        dict(
            date=None,
            icon_shows=2331,
            icon_clicks=1233,
            unique_icon_shows=None,
            pin_shows=1035,
            pin_clicks=2331,
            routes=3312,
        ),
    ]


@pytest.mark.parametrize("campaigns", [[1], [1, 2, 3]])
async def test_raises_if_nothing_found_on_selected_dates(campaigns, dm):
    with pytest.raises(NothingFound):
        await dm.fetch_search_icons_statistics(
            campaign_ids=campaigns,
            period_from=dt("2019-01-01 00:00:00"),
            period_to=dt("2019-01-12 23:59:59"),
        )


@pytest.mark.parametrize("campaigns", [[100], [100, 200, 300]])
async def test_raises_if_no_stats_for_campaign(campaigns, dm):
    with pytest.raises(NothingFound):
        await dm.fetch_search_icons_statistics(
            campaign_ids=[1000],
            period_from=dt("2019-12-01 00:00:00"),
            period_to=dt("2019-12-12 23:59:59"),
        )


async def test_raises_if_no_campaigns_passed(dm):
    with pytest.raises(NoCampaignsPassed):
        await dm.fetch_search_icons_statistics(
            campaign_ids=[],
            period_from=dt("2019-12-01 00:00:00"),
            period_to=dt("2019-12-12 23:59:59"),
        )
