from decimal import Decimal

import pytest

from maps_adv.adv_store.v2.lib.domains.campaigns import (
    BudgetIsTooLow,
    ShowingPeriodEndsInThePast,
    ShowingPeriodStartLaterThanEnd,
)
from maps_adv.adv_store.v2.tests import dt
from maps_adv.statistics.dashboard.client import NoStatistics

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "c_args",
    [
        ["2020-02-01 00:00:00", "2020-02-10 23:59:59", "UTC"],
        ["2020-01-31 21:00:00", "2020-02-10 20:59:59", "Europe/Moscow"],
        ["2020-02-01 07:00:00", "2020-02-11 06:59:59", "America/Denver"],
    ],
)
@pytest.mark.parametrize(
    "now, expected",
    (
        ["2020-01-01 19:00:00", Decimal("500")],
        ["2020-02-05 19:00:00", Decimal("833.34")],
        ["2020-02-06 19:00:00", Decimal("1000")],
        ["2020-02-10 19:00:00", Decimal("5000")],
    ),
)
async def test_predicts_expected_for_unspecified_campaign(
    c_args, now, expected, campaigns_domain, freezer
):
    freezer.move_to(now)
    got = await campaigns_domain.predict_daily_budget(
        start_datetime=dt(c_args[0]),
        end_datetime=dt(c_args[1]),
        timezone=c_args[2],
        budget=Decimal("5000"),
    )

    assert got == expected


@pytest.mark.parametrize(
    "c_args",
    [
        ["2020-02-01 00:00:00", "2020-02-10 23:59:59", "UTC"],
        ["2020-01-31 21:00:00", "2020-02-10 20:59:59", "Europe/Moscow"],
        ["2020-02-01 07:00:00", "2020-02-11 06:59:59", "America/Denver"],
    ],
)
@pytest.mark.parametrize(
    "now, expected",
    (
        ["2020-01-01 19:00:00", Decimal("400")],
        ["2020-02-01 19:00:00", Decimal("400")],
        ["2020-02-05 19:00:00", Decimal("666.67")],
        ["2020-02-06 19:00:00", Decimal("800")],
        ["2020-02-10 19:00:00", Decimal("4000")],
    ),
)
async def test_predicts_expected_for_specified_campaign_with_statistics(
    c_args, now, expected, charged_sum_mock, campaigns_domain, freezer
):
    freezer.move_to(now)
    charged_sum_mock.return_value = {100500: Decimal("1000")}

    got = await campaigns_domain.predict_daily_budget(
        campaign_id=100500,
        start_datetime=dt(c_args[0]),
        end_datetime=dt(c_args[1]),
        timezone=c_args[2],
        budget=Decimal("5000"),
    )

    assert got == expected


@pytest.mark.parametrize(
    "c_args",
    [
        ["2020-02-01 00:00:00", "2020-02-10 23:59:59", "UTC"],
        ["2020-01-31 21:00:00", "2020-02-10 20:59:59", "Europe/Moscow"],
        ["2020-02-01 07:00:00", "2020-02-11 06:59:59", "America/Denver"],
    ],
)
@pytest.mark.parametrize(
    "now, expected",
    (
        ["2020-01-01 19:00:00", Decimal("500")],
        ["2020-02-01 19:00:00", Decimal("500")],
        ["2020-02-05 19:00:00", Decimal("833.34")],
        ["2020-02-06 19:00:00", Decimal("1000")],
    ),
)
async def test_predicts_expected_for_specified_campaign_without_statistics(
    c_args, now, expected, charged_sum_mock, campaigns_domain, freezer
):
    freezer.move_to(now)
    charged_sum_mock.side_effect = NoStatistics()

    got = await campaigns_domain.predict_daily_budget(
        start_datetime=dt(c_args[0]),
        end_datetime=dt(c_args[1]),
        timezone=c_args[2],
        budget=Decimal("5000"),
    )

    assert got == expected


@pytest.mark.parametrize(
    "charged", (Decimal("5000"), Decimal("5000.0001"), Decimal("10000"))
)
@pytest.mark.freeze_time("2020-01-01")
async def test_raises_if_budget_is_lte_already_charged(
    charged, charged_sum_mock, campaigns_domain
):
    charged_sum_mock.return_value = {100500: charged}

    with pytest.raises(
        BudgetIsTooLow, match=f"Budget should be greater than {charged}"
    ):
        await campaigns_domain.predict_daily_budget(
            campaign_id=100500,
            start_datetime=dt("2020-02-01 16:00:00"),
            end_datetime=dt("2020-02-10 10:00:00"),
            timezone="UTC",
            budget=Decimal("5000"),
        )


@pytest.mark.freeze_time("2020-02-20")
async def test_raises_if_campaign_ends_in_the_past(campaigns_domain):
    with pytest.raises(ShowingPeriodEndsInThePast):
        await campaigns_domain.predict_daily_budget(
            start_datetime=dt("2020-02-01 16:00:00"),
            end_datetime=dt("2020-02-10 10:00:00"),
            timezone="UTC",
            budget=Decimal("5000"),
        )


@pytest.mark.freeze_time("2020-01-10")
async def test_raises_if_campaign_showing_period_start_later_than_end(campaigns_domain):
    with pytest.raises(ShowingPeriodStartLaterThanEnd):
        await campaigns_domain.predict_daily_budget(
            start_datetime=dt("2020-02-10 16:00:00"),
            end_datetime=dt("2020-02-01 10:00:00"),
            timezone="UTC",
            budget=Decimal("5000"),
        )
