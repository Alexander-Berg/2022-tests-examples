from decimal import Decimal

import pytest

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.proto.prediction_pb2 import (
    DailyBudgetPredictionInput,
    DailyBudgetPredictionOutput,
)
from maps_adv.adv_store.v2.tests import dt
from maps_adv.statistics.dashboard.client import NoStatistics

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/predict-daily-budget/"


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
        ["2020-01-01 19:00:00", 5000000],  # 500
        ["2020-02-05 19:00:00", 8333400],  # 833.34
        ["2020-02-06 19:00:00", 10000000],  # 1000
        ["2020-02-10 19:00:00", 50000000],  # 5000
    ),
)
async def test_predicts_expected_for_unspecified_campaign(
    c_args, now, expected, api, freezer
):
    freezer.move_to(now)
    input_pb = DailyBudgetPredictionInput(
        start_datetime=dt(c_args[0], as_proto=True),
        end_datetime=dt(c_args[1], as_proto=True),
        timezone=c_args[2],
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=DailyBudgetPredictionOutput, expected_status=200
    )

    assert got == DailyBudgetPredictionOutput(daily_budget=Money(value=expected))


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
        ["2020-01-01 19:00:00", 4000000],  # 400
        ["2020-02-05 19:00:00", 6666700],  # 666.67
        ["2020-02-06 19:00:00", 8000000],  # 800
        ["2020-02-10 19:00:00", 40000000],  # 4000
    ),
)
async def test_predicts_expected_for_speicified_campaign_with_statistics(
    c_args, now, expected, charged_sum_mock, api, freezer
):
    freezer.move_to(now)
    charged_sum_mock.return_value = {100500: Decimal("1000")}
    input_pb = DailyBudgetPredictionInput(
        campaign_id=100500,
        start_datetime=dt(c_args[0], as_proto=True),
        end_datetime=dt(c_args[1], as_proto=True),
        timezone=c_args[2],
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=DailyBudgetPredictionOutput, expected_status=200
    )

    assert got == DailyBudgetPredictionOutput(daily_budget=Money(value=expected))


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
        ["2020-01-01 19:00:00", 5000000],  # 500
        ["2020-02-05 19:00:00", 8333400],  # 833.34
        ["2020-02-06 19:00:00", 10000000],  # 1000
    ),
)
async def test_predicts_expected_for_specified_campaign_without_statistics(
    c_args, now, expected, charged_sum_mock, api, freezer
):
    freezer.move_to(now)
    charged_sum_mock.side_effect = NoStatistics()
    input_pb = DailyBudgetPredictionInput(
        campaign_id=100500,
        start_datetime=dt(c_args[0], as_proto=True),
        end_datetime=dt(c_args[1], as_proto=True),
        timezone=c_args[2],
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=DailyBudgetPredictionOutput, expected_status=200
    )

    assert got == DailyBudgetPredictionOutput(daily_budget=Money(value=expected))


@pytest.mark.parametrize(
    "now, expected",
    (
        ["2020-01-01 19:00:00", 5000000],  # 500
        ["2020-02-01 19:00:00", 5000000],  # 500
        ["2020-02-05 19:00:00", 8333400],  # 833.34
        ["2020-02-06 19:00:00", 10000000],  # 1000
        ["2020-02-10 19:00:00", 50000000],  # 5000
    ),
)
async def test_predicts_for_moscow_if_timezone_unspecified(
    now, expected, charged_sum_mock, api, freezer
):
    freezer.move_to(now)
    input_pb = DailyBudgetPredictionInput(
        start_datetime=dt("2020-01-31 21:00:00", as_proto=True),
        end_datetime=dt("2020-02-10 20:59:59", as_proto=True),
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=DailyBudgetPredictionOutput, expected_status=200
    )

    assert got == DailyBudgetPredictionOutput(daily_budget=Money(value=expected))


@pytest.mark.parametrize(
    "charged", (Decimal("5000"), Decimal("5000.0001"), Decimal("10000"))
)
@pytest.mark.freeze_time("2020-01-01")
async def test_returns_error_if_budget_is_lte_already_charged(
    charged, charged_sum_mock, api
):
    charged_sum_mock.return_value = {100500: charged}
    input_pb = DailyBudgetPredictionInput(
        campaign_id=100500,
        start_datetime=dt("2020-02-01 00:00:00", as_proto=True),
        end_datetime=dt("2020-02-10 00:00:00", as_proto=True),
        timezone="UTC",
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(
        code=Error.CAMPAIGN_BUDGET_IS_TOO_LOW,
        description=f"Budget should be greater than {charged}",
    )


@pytest.mark.freeze_time("2020-02-20")
async def test_returns_error_if_campaign_ends_in_the_past(api):
    input_pb = DailyBudgetPredictionInput(
        start_datetime=dt("2020-02-01 00:00:00", as_proto=True),
        end_datetime=dt("2020-02-10 00:00:00", as_proto=True),
        timezone="UTC",
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.SHOWING_PERIOD_ENDS_IN_THE_PAST)


@pytest.mark.freeze_time("2020-01-01")
async def test_returns_error_if_campaign_showing_period_start_later_than_end(api):
    input_pb = DailyBudgetPredictionInput(
        start_datetime=dt("2020-02-10 00:00:00", as_proto=True),
        end_datetime=dt("2020-02-01 00:00:00", as_proto=True),
        timezone="UTC",
        budget=Money(value=5000 * 10000),
    )

    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.SHOWING_PERIOD_START_LATER_THAN_END)
