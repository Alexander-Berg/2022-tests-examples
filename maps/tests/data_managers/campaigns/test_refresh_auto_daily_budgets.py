from datetime import date, datetime
from decimal import Decimal
from typing import Optional, Tuple

import pytest

from maps_adv.common.helpers import Any, dt
from maps_adv.statistics.dashboard.client import NoStatistics

pytestmark = [pytest.mark.asyncio, pytest.mark.wip]


def build_campaign_kwargs_and_assertion_sql(
    billing_type: str,
    auto_daily_budget: bool,
    start_datetime: Optional[str] = "2020-01-01 00:00:00",
    end_datetime: Optional[str] = "2020-02-10 23:59:59",
    timezone: Optional[str] = "UTC",
) -> Tuple[dict, str]:
    kwargs = {
        billing_type: {
            "cost": Decimal("10"),
            "budget": Decimal("10000"),
            "daily_budget": Decimal("5000"),
            "auto_daily_budget": auto_daily_budget,
        },
        "start_datetime": dt(start_datetime),
        "end_datetime": dt(end_datetime),
        "timezone": timezone,
    }
    sql = f"""
        SELECT
            billing_{billing_type}.daily_budget
        FROM billing_{billing_type}
        JOIN campaign_billing
        ON billing_{billing_type}.id = campaign_billing.{billing_type}_id
        JOIN campaign
            ON campaign_billing.id = campaign.billing_id
        WHERE campaign.id = $1
    """
    return kwargs, sql


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
@pytest.mark.parametrize(
    "at_datetime, c_args, charged, expected_daily_budget",
    [
        # 10 days
        (
            dt("2020-02-01 00:00:00"),
            ["2020-01-01 00:00:00", "2020-02-10 23:59:59", "UTC"],
            Decimal("5000"),
            Decimal("500"),
        ),
        (
            dt("2020-01-31 21:00:00"),
            ["2019-12-31 21:00:00", "2020-02-10 20:59:59", "Europe/Moscow"],
            Decimal("5000"),
            Decimal("500"),
        ),
        (
            dt("2020-02-01 07:00:00"),  # winter time
            ["2020-01-01 07:00:00", "2020-02-11 06:59:59", "America/Denver"],
            Decimal("5000"),
            Decimal("500"),
        ),
        # 20 days
        (
            dt("2020-02-01 00:00:00"),
            ["2020-01-01 00:00:00", "2020-02-20 23:59:59", "UTC"],
            Decimal("1000"),
            Decimal("450"),
        ),
        (
            dt("2020-01-31 21:00:00"),
            ["2019-12-31 21:00:00", "2020-02-20 20:59:59", "Europe/Moscow"],
            Decimal("1000"),
            Decimal("450"),
        ),
        (
            dt("2020-02-01 07:00:00"),  # winter time
            ["2020-01-01 07:00:00", "2020-02-21 06:59:59", "America/Denver"],
            Decimal("1000"),
            Decimal("450"),
        ),
    ],
)
async def test_daily_budget_will_be_refreshed(
    at_datetime,
    billing_type,
    c_args,
    charged,
    expected_daily_budget,
    charged_sum_mock,
    factory,
    con,
    campaigns_dm,
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True, *c_args)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: charged}

    await campaigns_dm.refresh_auto_daily_budgets(at_datetime)

    assert await con.fetchval(sql, campaign_id) == expected_daily_budget


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_rounds_up_to_two_decimal_places(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: Decimal("345.6789")}

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(sql, campaign_id) == Decimal("965.44")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
@pytest.mark.parametrize(
    "charged", [Decimal("10000"), Decimal("10000.0001"), Decimal("20000")]
)
async def test_does_not_update_if_charged_gte_budget(
    billing_type, charged, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: charged}

    await campaigns_dm.refresh_auto_daily_budgets(date(2020, 2, 1))

    assert await con.fetchval(sql, campaign_id) == Decimal("5000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_does_not_update_if_auto_daily_budget_is_not_set(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, False)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: Decimal("5000")}

    await campaigns_dm.refresh_auto_daily_budgets(date(2020, 2, 1))

    assert await con.fetchval(sql, campaign_id) == Decimal("5000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
@pytest.mark.parametrize(
    "at_datetime, c_args",
    [
        (
            dt("2020-02-01 00:00:00"),
            ["2020-01-01 00:00:00", "2020-01-31 23:59:59", "UTC"],
        ),
        (
            dt("2020-01-31 21:00:00"),
            ["2019-12-31 21:00:00", "2020-01-31 20:59:59", "Europe/Moscow"],
        ),
        (
            dt("2020-02-01 07:00:00"),
            ["2020-01-01 07:00:00", "2020-02-01 06:59:59", "America/Denver"],
        ),
    ],
)
async def test_does_not_updates_finished_campaign(
    at_datetime, billing_type, c_args, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True, *c_args)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: Decimal("1000")}

    await campaigns_dm.refresh_auto_daily_budgets(at_datetime)

    assert await con.fetchval(sql, campaign_id) == Decimal("5000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_updates_not_started_campaign(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(
        billing_type, True, "2020-02-10 00:00:00", "2020-02-19 00:00:00"
    )
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id + 1: Decimal("5000")}

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(sql, campaign_id) == Decimal("1000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_updates_campaign_without_statistics(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id + 1: Decimal("5000")}

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(sql, campaign_id) == Decimal("1000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_updates_campaign_if_statistics_returns_nothing(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.side_effect = NoStatistics()

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(sql, campaign_id) == Decimal("1000")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
@pytest.mark.parametrize("billing_type_with_auto", ("cpm", "cpa"))
async def test_does_not_affect_campaigns_without_auto_daily_budget(
    billing_type, billing_type_with_auto, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, False)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    kwargs_with_auto, sql_with_auto = build_campaign_kwargs_and_assertion_sql(
        billing_type, True
    )
    campaign_id_with_auto = (await factory.create_campaign(**kwargs_with_auto))["id"]
    charged_sum_mock.return_value = {
        campaign_id: Decimal("1000"),
        campaign_id_with_auto: Decimal("5000"),
    }

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(sql, campaign_id) == Decimal("5000")
    assert await con.fetchval(sql_with_auto, campaign_id_with_auto) == Decimal("500")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
@pytest.mark.parametrize("billing_type_with_auto", ("cpm", "cpa"))
async def test_does_not_affect_finished_campaigns(
    billing_type, billing_type_with_auto, charged_sum_mock, factory, con, campaigns_dm
):
    finished_kwargs, finished_sql = build_campaign_kwargs_and_assertion_sql(
        billing_type, True, "2020-01-01 00:00:00", "2020-01-31 21:59:59", "UTC"
    )
    finished_campaign_id = (await factory.create_campaign(**finished_kwargs))["id"]
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(billing_type, True)
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {
        finished_campaign_id: Decimal("1000"),
        campaign_id: Decimal("5000"),
    }

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 00:00:00"))

    assert await con.fetchval(finished_sql, finished_campaign_id) == Decimal("5000")
    assert await con.fetchval(sql, campaign_id) == Decimal("500")


@pytest.mark.parametrize("billing_type", ("cpm", "cpa"))
async def test_adds_change_log_record_for_changing_campaigns_with_auto_daily_budget(
    billing_type, charged_sum_mock, factory, con, campaigns_dm
):
    kwargs, sql = build_campaign_kwargs_and_assertion_sql(
        billing_type,
        True,
        "2020-01-01 07:00:00",
        "2020-02-21 06:59:59",
        "America/Denver",
    )
    campaign_id = (await factory.create_campaign(**kwargs))["id"]
    charged_sum_mock.return_value = {campaign_id: Decimal("1000")}

    await campaigns_dm.refresh_auto_daily_budgets(dt("2020-02-01 07:00:00"))

    result = await factory.list_campaign_change_log(campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 0,
            "status": "DRAFT",
            "system_metadata": {"action": "campaign.refresh_auto_daily_budget"},
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
