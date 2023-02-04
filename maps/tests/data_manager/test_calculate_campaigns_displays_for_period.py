import pytest

from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]


async def test_raises_if_no_campaigns_passed(dm):
    got = await dm.calculate_campaigns_events_for_period(
        events_query=[],
        period_from=dt("2020-12-01 00:00:00"),
        period_to=dt("2020-12-01 01:00:00"),
    )
    assert got == {}


async def test_returns_zeroes_if_no_campaign_displays_found(dm):

    got = await dm.calculate_campaigns_events_for_period(
        events_query=[
            (10, CampaignTypeEnum.PIN_ON_ROUTE),
            (101, CampaignTypeEnum.PIN_ON_ROUTE),
        ],
        period_from=dt("2020-12-01 00:00:00"),
        period_to=dt("2020-12-01 01:00:00"),
    )

    assert got == {10: 0, 101: 0}


@pytest.mark.parametrize(
    "campaign_ids, expected",
    (
        [[10], {10: 7}],
        [[20], {20: 5}],
        [
            [10, 20],
            {10: 7, 20: 5},
        ],
    ),
)
async def test_returns_statistics_for_passed_campaigns(campaign_ids, expected, dm):
    got = await dm.calculate_campaigns_events_for_period(
        list(map(lambda id: (id, CampaignTypeEnum.PIN_ON_ROUTE), campaign_ids)),
        period_from=dt("2019-01-01 00:00:00"),
        period_to=dt("2019-01-01 23:59:59"),
    )

    assert got == expected


@pytest.mark.parametrize(
    ["period_from", "period_to", "displays"],
    [
        (None, None, 2),
        (None, dt("2019-01-01 12:20:00"), 3),
        (dt("2019-01-01 12:01:00"), None, 1),
    ],
)
async def test_limits_events_by_receive_timestamp(
    period_from, period_to, displays, dm, freezer
):
    freezer.move_to(dt("2019-01-01 12:10:00"))

    got = await dm.calculate_campaigns_events_for_period(
        events_query=[
            (30, CampaignTypeEnum.PIN_ON_ROUTE),
        ],
        period_from=period_from,
        period_to=period_to,
    )

    assert got == {30: displays}
