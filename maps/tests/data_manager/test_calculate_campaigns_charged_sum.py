from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.server.lib.data_manager import NothingFound

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]


async def test_raises_if_no_campaigns_passed(dm):
    with pytest.raises(NothingFound):
        await dm.calculate_campaigns_charged_sum(
            campaign_ids=[], on_timestamp=1546603800
        )


async def test_raises_if_no_campaigns_found(dm):
    with pytest.raises(NothingFound):
        await dm.calculate_campaigns_charged_sum(
            campaign_ids=[110, 120, 130], on_timestamp=1546603800
        )


@pytest.mark.parametrize(
    "campaign_ids, expected",
    (
        [[10], [{"campaign_id": 10, "charged_sum": Decimal("1.1")}]],
        [[20], [{"campaign_id": 20, "charged_sum": Decimal("5")}]],
        [
            [10, 20],
            [
                {"campaign_id": 10, "charged_sum": Decimal("1.1")},
                {"campaign_id": 20, "charged_sum": Decimal("5")},
            ],
        ],
    ),
)
@pytest.mark.parametrize("campaigns_for_v2", [set(), {10}, {20}, {10, 20}])
async def test_returns_statistics_for_passed_campaigns(
    campaigns_for_v2, campaign_ids, expected, dm, mocker
):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    sort_key = itemgetter("campaign_id")

    got = await dm.calculate_campaigns_charged_sum(
        campaign_ids=campaign_ids, on_timestamp=1546603800
    )

    assert sorted(got, key=sort_key) == sorted(expected, key=sort_key)


@pytest.mark.parametrize("campaigns_for_v2", [set(), {30}])
async def test_round_cost_to_two_decimal_places(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)

    got = await dm.calculate_campaigns_charged_sum(
        campaign_ids=[30], on_timestamp=1546603800
    )

    assert got == [{"campaign_id": 30, "charged_sum": Decimal("0.37")}]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {30}])
@pytest.mark.parametrize(
    ["on_timestamp", "expected_sum"],
    [
        (None, "0.12"),
        (dt("2019-01-01 12:20:00").timestamp(), "0.37"),
        (dt("2019-01-01 12:20:00").timestamp() - 1, "0.24"),
    ],
)
async def test_limits_events_by_receive_timestamp(
    on_timestamp, expected_sum, campaigns_for_v2, dm, mocker, freezer
):
    freezer.move_to(dt("2019-01-01 12:00:00"))
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)

    got = await dm.calculate_campaigns_charged_sum(
        campaign_ids=[30], on_timestamp=on_timestamp
    )

    assert got == [{"campaign_id": 30, "charged_sum": Decimal(expected_sum)}]
