import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.proto.campaigns_stat_pb2 import (
    CampaignsStatInput,
    IconCampaignsStatDetails,
    IconCampaignsStatOnDate,
    IconCampaignsStatOutput,
)
from maps_adv.statistics.dashboard.proto.error_pb2 import Error

pytestmark = [pytest.mark.asyncio]


url = "/statistics/campaigns/icons/"


@pytest.mark.usefixtures("fill_category_search_report_table")
async def test_returns_data_for_one_campaign(api):
    pb_input = CampaignsStatInput(
        campaign_ids=[1],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-12 23:59:59", as_proto=True),
    )

    got = await api.post(
        url, proto=pb_input, decode_as=IconCampaignsStatOutput, expected_status=200
    )

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
                date="2019-12-10",
                details=IconCampaignsStatDetails(
                    icon_shows=1,
                    icon_clicks=1000,
                    unique_icon_shows=1,
                    pin_shows=1000,
                    pin_clicks=1,
                    routes=10,
                ),
            ),
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


@pytest.mark.usefixtures("fill_category_search_report_table")
async def test_returns_data_for_list_of_campaigns(api):
    pb_input = CampaignsStatInput(
        campaign_ids=[1, 2, 100],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-12 23:59:59", as_proto=True),
    )

    got = await api.post(
        url, proto=pb_input, decode_as=IconCampaignsStatOutput, expected_status=200
    )

    assert got == IconCampaignsStatOutput(
        total=IconCampaignsStatDetails(
            icon_shows=2331,
            icon_clicks=1233,
            pin_shows=1035,
            pin_clicks=2331,
            routes=3312,
        ),
        by_dates=[
            IconCampaignsStatOnDate(
                date="2019-12-10",
                details=IconCampaignsStatDetails(
                    icon_shows=1,
                    icon_clicks=1000,
                    pin_shows=1000,
                    pin_clicks=1,
                    routes=10,
                ),
            ),
            IconCampaignsStatOnDate(
                date="2019-12-03",
                details=IconCampaignsStatDetails(
                    icon_shows=2000,
                    icon_clicks=200,
                    pin_shows=2,
                    pin_clicks=2000,
                    routes=2,
                ),
            ),
            IconCampaignsStatOnDate(
                date="2019-12-02",
                details=IconCampaignsStatDetails(
                    icon_shows=300,
                    icon_clicks=30,
                    pin_shows=3,
                    pin_clicks=30,
                    routes=3000,
                ),
            ),
            IconCampaignsStatOnDate(
                date="2019-12-01",
                details=IconCampaignsStatDetails(
                    icon_shows=30,
                    icon_clicks=3,
                    pin_shows=30,
                    pin_clicks=300,
                    routes=300,
                ),
            ),
        ],
    )


@pytest.mark.usefixtures("fill_category_search_report_table")
@pytest.mark.parametrize("campaigns", [[1], [1, 2, 3]])
async def test_returns_204_if_nothing_in_selected_dates(campaigns, api):
    pb_input = CampaignsStatInput(
        campaign_ids=campaigns,
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-02 23:59:59", as_proto=True),
    )
    await api.post(url, proto=pb_input, expected_status=204)


@pytest.mark.parametrize("campaigns", [[100], [100, 200, 300]])
async def test_returns_204_if_nothing_found(campaigns, api):
    pb_input = CampaignsStatInput(
        campaign_ids=campaigns,
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-02 23:59:59", as_proto=True),
    )

    await api.post(url, proto=pb_input, expected_status=204)


async def test_raises_if_no_campaigns_passed(api):
    pb_input = CampaignsStatInput(
        campaign_ids=[],
        period_from=dt("2019-12-01 00:00:00", as_proto=True),
        period_to=dt("2019-12-01 23:59:59", as_proto=True),
    )

    got = await api.post(url, proto=pb_input, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.NO_CAMPAIGNS_PASSED)
