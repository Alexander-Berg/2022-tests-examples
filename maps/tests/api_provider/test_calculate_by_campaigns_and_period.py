from datetime import date
from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.proto import campaigns_stat_pb2
from maps_adv.statistics.dashboard.server.lib.api_provider import NoCampaignsPassed

pytestmark = [pytest.mark.asyncio]

_calculate_result = [
    {
        "date": date(2019, 1, 2),
        "call": 1,
        "makeRoute": 0,
        "openSite": 0,
        "saveOffer": 0,
        "search": 1,
        "show": 4,
        "tap": 2,
        "ctr": 0.599,
        "clicks_to_routes": 0,
        "charged_sum": Decimal("0.4"),
        "show_unique": 3,
    },
    {
        "date": date(2019, 1, 1),
        "call": 1,
        "makeRoute": 1,
        "openSite": 0,
        "saveOffer": 0,
        "search": 2,
        "show": 7,
        "tap": 4,
        "ctr": 0.5799,
        "clicks_to_routes": 0.25,
        "charged_sum": Decimal("0.7"),
        "show_unique": 3,
    },
    {
        "call": 2,
        "makeRoute": 1,
        "openSite": 0,
        "saveOffer": 0,
        "search": 3,
        "show": 11,
        "tap": 6,
        "ctr": 0.5499,
        "clicks_to_routes": 0.1666,
        "charged_sum": Decimal("1.1"),
        "show_unique": 4,
    },
]


@pytest.mark.mock_dm
async def test_returns_expected(dm, api_provider):
    dm.calculate_by_campaigns_and_period.coro.return_value = _calculate_result
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-31 23:59:59", as_proto=True),
    )

    raw_got = await api_provider.calculate_by_campaigns_and_period(
        pb_input.SerializeToString()
    )

    dm.calculate_by_campaigns_and_period.assert_called_with(
        campaign_ids=[10], period_from=date(2019, 1, 1), period_to=date(2019, 1, 31)
    )

    got = campaigns_stat_pb2.CampaignsStatOutput.FromString(raw_got)
    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-02",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=0,
                    openSite=0,
                    saveOffer=0,
                    search=1,
                    show=4,
                    tap=2,
                    ctr=0.599,
                    clicks_to_routes=0,
                    charged_sum="0.4",
                    show_unique=3,
                ),
            ),
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=1,
                    openSite=0,
                    saveOffer=0,
                    search=2,
                    show=7,
                    tap=4,
                    ctr=0.5799,
                    clicks_to_routes=0.25,
                    charged_sum="0.7",
                    show_unique=3,
                ),
            ),
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=2,
            makeRoute=1,
            openSite=0,
            saveOffer=0,
            search=3,
            show=11,
            tap=6,
            ctr=0.5499,
            clicks_to_routes=0.1666,
            charged_sum="1.1",
            show_unique=4,
        ),
    )


@pytest.mark.mock_dm
async def test_raises_for_empty_campaign_ids(dm, api_provider):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-31 23:59:59", as_proto=True),
    )

    with pytest.raises(NoCampaignsPassed):
        await api_provider.calculate_by_campaigns_and_period(
            pb_input.SerializeToString()
        )
