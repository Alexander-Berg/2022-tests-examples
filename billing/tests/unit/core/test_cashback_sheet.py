from datetime import datetime, timedelta, timezone

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.cashback_sheet import (
    CashbackSheetPeriod,
    resolve_sheet_period_for_datetime,
)

msk = timezone(timedelta(hours=3))


@pytest.mark.parametrize(
    'for_datetime,expected_start,expected_end',
    [
        (
            datetime(2021, 12, 1, tzinfo=timezone.utc),
            datetime(2021, 12, 1, tzinfo=timezone.utc),
            datetime(2022, 1, 1, tzinfo=timezone.utc),
        ),
        (
            datetime(2021, 11, 1, tzinfo=msk),  # it's still October in UTC
            datetime(2021, 10, 1, tzinfo=timezone.utc),
            datetime(2021, 11, 1, tzinfo=timezone.utc),
        ),
        (
            datetime(2022, 1, 1, 3, tzinfo=msk),
            datetime(2022, 1, 1, tzinfo=timezone.utc),
            datetime(2022, 2, 1, tzinfo=timezone.utc),
        ),
        (
            datetime(2021, 5, 31, 23, 59, 59, tzinfo=timezone.utc),
            datetime(2021, 5, 1, tzinfo=timezone.utc),
            datetime(2021, 6, 1, tzinfo=timezone.utc),
        ),
    ]
)
def test_resolve_sheet_period(for_datetime, expected_start, expected_end):
    expected = CashbackSheetPeriod(period_start=expected_start, period_end=expected_end)
    assert_that(
        resolve_sheet_period_for_datetime(for_datetime),
        equal_to(expected),
    )
