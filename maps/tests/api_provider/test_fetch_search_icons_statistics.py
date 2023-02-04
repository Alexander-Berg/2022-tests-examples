import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.proto.campaigns_stat_pb2 import (
    CampaignsStatInput,
    IconCampaignsStatDetails,
    IconCampaignsStatOnDate,
    IconCampaignsStatOutput,
)
from maps_adv.statistics.dashboard.server.lib.data_manager import NoCampaignsPassed

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_expected_data_for_one_campaign(dm, api_provider):
    dm.fetch_search_icons_statistics.coro.return_value = [
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

    pb_input = CampaignsStatInput(
        campaign_ids=[1],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-12 23:59:59", as_proto=True),
    ).SerializeToString()

    raw_got = await api_provider.fetch_search_icons_statistics(pb_input)
    got = IconCampaignsStatOutput.FromString(raw_got)

    assert got == IconCampaignsStatOutput(
        total=IconCampaignsStatDetails(
            icon_shows=111,
            icon_clicks=1011,
            pin_shows=1011,
            pin_clicks=111,
            routes=1110,
        ),
        by_dates=[
            IconCampaignsStatOnDate(
                date="2019-12-02",
                details=IconCampaignsStatDetails(
                    icon_shows=100,
                    icon_clicks=10,
                    unique_icon_shows=100,
                    pin_shows=1,
                    pin_clicks=10,
                    routes=1000,
                ),
            ),
            IconCampaignsStatOnDate(
                date="2019-12-01",
                details=IconCampaignsStatDetails(
                    icon_shows=10,
                    icon_clicks=1,
                    unique_icon_shows=1000,
                    pin_shows=10,
                    pin_clicks=100,
                    routes=100,
                ),
            ),
        ],
    )


async def test_returns_expected_data_for_list_of_campaigns(dm, api_provider):
    dm.fetch_search_icons_statistics.coro.return_value = [
        dict(
            date="2019-12-02",
            icon_shows=100,
            icon_clicks=10,
            unique_icon_shows=None,
            pin_shows=1,
            pin_clicks=10,
            routes=1000,
        ),
        dict(
            date="2019-12-01",
            icon_shows=10,
            icon_clicks=1,
            unique_icon_shows=None,
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

    pb_input = CampaignsStatInput(
        campaign_ids=[1, 2, 3],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-12 23:59:59", as_proto=True),
    ).SerializeToString()

    raw_got = await api_provider.fetch_search_icons_statistics(pb_input)
    got = IconCampaignsStatOutput.FromString(raw_got)

    assert got == IconCampaignsStatOutput(
        total=IconCampaignsStatDetails(
            icon_shows=111,
            icon_clicks=1011,
            pin_shows=1011,
            pin_clicks=111,
            routes=1110,
        ),
        by_dates=[
            IconCampaignsStatOnDate(
                date="2019-12-02",
                details=IconCampaignsStatDetails(
                    icon_shows=100,
                    icon_clicks=10,
                    pin_shows=1,
                    pin_clicks=10,
                    routes=1000,
                ),
            ),
            IconCampaignsStatOnDate(
                date="2019-12-01",
                details=IconCampaignsStatDetails(
                    icon_shows=10,
                    icon_clicks=1,
                    pin_shows=10,
                    pin_clicks=100,
                    routes=100,
                ),
            ),
        ],
    )


async def test_dm_called_ok(dm, api_provider):
    dm.fetch_search_icons_statistics.coro.return_value = [
        dict(
            date="2019-12-01",
            icon_shows=10,
            icon_clicks=1,
            unique_icon_shows=None,
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
    pb_input = CampaignsStatInput(
        campaign_ids=[1],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-12 23:59:59", as_proto=True),
    ).SerializeToString()

    await api_provider.fetch_search_icons_statistics(pb_input)

    dm.fetch_search_icons_statistics.assert_called_with(
        campaign_ids=[1],
        period_from=dt("2019-12-01 00:00:00"),
        period_to=dt("2019-12-12 23:59:59"),
    )


async def test_raises_for_empty_campaign_ids(dm, api_provider):
    pb_input = CampaignsStatInput(
        campaign_ids=[],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-31 23:59:59", as_proto=True),
    ).SerializeToString()

    with pytest.raises(NoCampaignsPassed):
        await api_provider.fetch_search_icons_statistics(pb_input)
