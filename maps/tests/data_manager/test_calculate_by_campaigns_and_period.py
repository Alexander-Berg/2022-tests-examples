from datetime import date
from decimal import Decimal
from unittest.mock import call, patch

import asyncio
import copy
import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.server.lib.data_manager import (
    CampaignsFromDifferentVersionOfStatistics,
    NothingFound,
)
from maps_adv.statistics.dashboard.server.tests import make_event, make_event_v2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]


async def test_raises_if_no_campaigns_passed(dm):
    with pytest.raises(NothingFound):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[], period_from=dt("2019-01-01"), period_to=dt("2019-01-31")
        )


async def test_raises_if_requested_campaigns_from_v1_and_v2_of_statistics(dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", {20})
    with pytest.raises(CampaignsFromDifferentVersionOfStatistics):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[10, 20],
            period_from=dt("2019-01-01"),
            period_to=dt("2019-01-31"),
        )


@pytest.mark.parametrize("campaigns_for_v2", [set(), {110, 120, 130}])
async def test_raises_if_no_campaigns_found(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    with pytest.raises(NothingFound):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[110, 120, 130],
            period_from=dt("2019-01-01"),
            period_to=dt("2019-01-31"),
        )


@pytest.mark.parametrize("campaigns_for_v2", [set(), {10, 20}])
async def test_raises_if_nothing_found(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    with pytest.raises(NothingFound):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[10, 20],
            period_from=dt("2019-02-01"),
            period_to=dt("2019-02-28"),
        )


@pytest.mark.parametrize(
    "campaign_id, expected",
    (
        [
            10,
            [
                {
                    "date": date(2019, 1, 1),
                    "call": 1,
                    "makeRoute": 1,
                    "openSite": 0,
                    "saveOffer": 0,
                    "search": 2,
                    "show": 7,
                    "tap": 4,
                    "ctr": 0.5714,
                    "clicks_to_routes": 0.25,
                    "charged_sum": Decimal("0.7"),
                    "show_unique": 3,
                },
                {
                    "call": 1,
                    "makeRoute": 1,
                    "openSite": 0,
                    "saveOffer": 0,
                    "search": 2,
                    "show": 7,
                    "tap": 4,
                    "ctr": 0.5714,
                    "clicks_to_routes": 0.25,
                    "charged_sum": Decimal("0.7"),
                    "show_unique": 3,
                },
            ],
        ],
        [
            20,
            [
                {
                    "date": date(2019, 1, 1),
                    "call": 0,
                    "makeRoute": 0,
                    "openSite": 2,
                    "saveOffer": 1,
                    "search": 0,
                    "show": 5,
                    "tap": 3,
                    "ctr": 0.6,
                    "clicks_to_routes": 0,
                    "charged_sum": Decimal("5"),
                    "show_unique": 5,
                },
                {
                    "call": 0,
                    "makeRoute": 0,
                    "openSite": 2,
                    "saveOffer": 1,
                    "search": 0,
                    "show": 5,
                    "tap": 3,
                    "ctr": 0.6,
                    "clicks_to_routes": 0,
                    "charged_sum": Decimal("5"),
                    "show_unique": 5,
                },
            ],
        ],
    ),
)
@pytest.mark.parametrize("campaigns_for_v2", [set(), {10, 20}])
async def test_returns_statistics_for_one_campaign(
    campaigns_for_v2, campaign_id, expected, dm, mocker
):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[campaign_id],
        period_from=dt("2019-01-01"),
        period_to=dt("2019-01-01"),
    )

    assert got == expected


@pytest.mark.parametrize("campaigns_for_v2", [set(), {10, 20}])
async def test_returns_statistics_for_many_campaigns(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[10, 20], period_from=dt("2019-01-01"), period_to=dt("2019-01-01")
    )

    assert got == [
        {
            "date": date(2019, 1, 1),
            "call": 1,
            "makeRoute": 1,
            "openSite": 2,
            "saveOffer": 1,
            "search": 2,
            "show": 12,
            "tap": 7,
            "ctr": 0.5833,
            "clicks_to_routes": 0.1428,
            "charged_sum": Decimal("5.7"),
            "show_unique": 6,
        },
        {
            "call": 1,
            "makeRoute": 1,
            "openSite": 2,
            "saveOffer": 1,
            "search": 2,
            "show": 12,
            "tap": 7,
            "ctr": 0.5833,
            "clicks_to_routes": 0.1428,
            "charged_sum": Decimal("5.7"),
            "show_unique": 6,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {10}])
async def test_returns_statistics_for_many_days(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[10], period_from=dt("2019-01-01"), period_to=dt("2019-01-31")
    )

    assert got == [
        {
            "date": date(2019, 1, 2),
            "call": 1,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 1,
            "show": 4,
            "tap": 2,
            "ctr": 0.5,
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
            "ctr": 0.5714,
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
            "ctr": 0.5454,
            "clicks_to_routes": 0.1666,
            "charged_sum": Decimal("1.1"),
            "show_unique": 4,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {30}])
async def test_round_cost_to_two_decimal_places(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[30], period_from=dt("2019-01-01"), period_to=dt("2019-01-01")
    )

    assert got == [
        {
            "date": date(2019, 1, 1),
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 3,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0.37"),
            "show_unique": 1,
        },
        {
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 3,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0.37"),
            "show_unique": 1,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {40}])
async def test_clicks_to_routes_is_0_if_there_are_no_taps(
    campaigns_for_v2, ch, dm, mocker
):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    event = make_event(dt("2019-01-01 12:20:00"), 40, "di0", "action.makeRoute")
    ch.execute("INSERT INTO stat.accepted_sample VALUES", [event])
    event_v2 = make_event_v2(dt("2019-01-01 12:20:00"), 40, "di0", "ACTION_MAKE_ROUTE")
    ch.execute("INSERT INTO stat.processed_events_distributed VALUES", [event_v2])

    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[40], period_from=dt("2019-01-01"), period_to=dt("2019-01-01")
    )

    assert got == [
        {
            "date": date(2019, 1, 1),
            "call": 0,
            "makeRoute": 1,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 0,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0"),
            "show_unique": 0,
        },
        {
            "call": 0,
            "makeRoute": 1,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 0,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0"),
            "show_unique": 0,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {40}])
async def test_ctr_is_0_if_there_are_no_shows(campaigns_for_v2, ch, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    event = make_event(dt("2019-01-01 12:20:00"), 40, "di0", "pin.tap")
    ch.execute("INSERT INTO stat.accepted_sample VALUES", [event])
    event_v2 = make_event_v2(dt("2019-01-01 12:20:00"), 40, "di0", "BILLBOARD_TAP")
    ch.execute("INSERT INTO stat.processed_events_distributed VALUES", [event_v2])

    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[40], period_from=dt("2019-01-01"), period_to=dt("2019-01-01")
    )

    assert got == [
        {
            "date": date(2019, 1, 1),
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 0,
            "tap": 1,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0"),
            "show_unique": 0,
        },
        {
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 0,
            "tap": 1,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0"),
            "show_unique": 0,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {40}])
async def test_unique_is_not_grater_then_show(campaigns_for_v2, dm, ch, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    events = [
        (dt("2019-01-01 12:20:00"), 40, f"di{i}", "pin.show", Decimal("1"))
        for i in range(100000)
    ]
    ch.execute(
        "INSERT INTO stat.accepted_sample VALUES", [make_event(*e) for e in events]
    )
    events_v2 = [
        (dt("2019-01-01 12:20:00"), 40, f"di{i}", "BILLBOARD_SHOW", Decimal("1"))
        for i in range(100000)
    ]
    ch.execute(
        "INSERT INTO stat.processed_events_distributed VALUES",
        [make_event_v2(*e) for e in events_v2],
    )

    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[40], period_from=dt("2019-01-01"), period_to=dt("2019-01-01")
    )

    assert got == [
        {
            "date": date(2019, 1, 1),
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 100000,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("100000"),
            "show_unique": 100000,
        },
        {
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 100000,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("100000"),
            "show_unique": 100000,
        },
    ]


@pytest.mark.parametrize("campaigns_for_v2", [set(), {40}])
async def test_round_charged_sum_to_two_decimal_places(campaigns_for_v2, dm, mocker):
    mocker.patch.object(dm, "_campaigns_only_for_v2", campaigns_for_v2)
    got = await dm.calculate_by_campaigns_and_period(
        campaign_ids=[40], period_from=dt("2019-01-04"), period_to=dt("2019-01-04")
    )

    assert got == [
        {
            "date": date(2019, 1, 4),
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 2,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0.56"),
            "show_unique": 1,
        },
        {
            "call": 0,
            "makeRoute": 0,
            "openSite": 0,
            "saveOffer": 0,
            "search": 0,
            "show": 2,
            "tap": 0,
            "ctr": 0.0,
            "clicks_to_routes": 0,
            "charged_sum": Decimal("0.56"),
            "show_unique": 1,
        },
    ]


@patch("aioch.Client")
async def test_chooses_different_ch_hosts(ch_client_mock, dm, ch_config):
    ch_config = copy.copy(ch_config)
    host = iter(ch_config.pop("hosts"))

    instance = ch_client_mock.return_value
    instance.execute.side_effect = asyncio.coroutine(lambda a, b: [])

    host0 = next(host)
    with pytest.raises(NothingFound):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[10, 20],
            period_from=dt("2019-01-01"),
            period_to=dt("2019-01-01"),
        )
    host1 = next(host)
    with pytest.raises(NothingFound):
        await dm.calculate_by_campaigns_and_period(
            campaign_ids=[10, 20],
            period_from=dt("2019-01-01"),
            period_to=dt("2019-01-01"),
        )
    assert host0 != host1
    assert ch_client_mock.call_args_list == [
        call(**host0, **ch_config),
        call(**host1, **ch_config),
    ]
