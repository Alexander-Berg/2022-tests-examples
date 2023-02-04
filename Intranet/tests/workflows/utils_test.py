import pytest
from mock import patch

from datetime import date, timedelta

from django.conf import settings

from staff.gap.holidays import OutModes
from staff.gap.workflows.utils import (
    get_min_mandatory_vacation_date_to,
    get_date_n_business_days_before, gap_year_changed,
)


def fake_get_holidays_by_year(*args, **kwargs):
    return [
        date(2021, 6, 12),
        date(2021, 11, 4),
        date(2021, 11, 5),
    ]


def fake_get_weekdays_by_year(*args, **kwargs):
    return [
        date(2021, 11, 1),
        date(2021, 11, 2),
        date(2021, 11, 3),
        date(2021, 11, 8),
        date(2021, 11, 9),
        date(2021, 11, 10),
        date(2021, 11, 11),
        date(2021, 11, 12),
        date(2021, 11, 13),
        date(2021, 11, 14),
        date(2021, 11, 15),
    ]


def test_get_min_mandatory_vacation_date_to():
    geo_id = 225

    with patch('staff.gap.workflows.utils.get_holidays_by_year', fake_get_holidays_by_year):
        first_date = date(2021, 7, 1)
        first_result = get_min_mandatory_vacation_date_to(
            date_from=first_date,
            geo_id=geo_id,
        )
        assert first_result == first_date + timedelta(settings.MANDATORY_VACATION_DURATION)

        second_date = date(2021, 6, 1)
        second_result = get_min_mandatory_vacation_date_to(
            date_from=second_date,
            geo_id=geo_id,
        )
        assert second_result == second_date + timedelta(days=settings.MANDATORY_VACATION_DURATION + 1)

        third_date = date(2021, 10, 22)
        third_result = get_min_mandatory_vacation_date_to(
            date_from=third_date,
            geo_id=geo_id,
        )
        assert third_result == third_date + timedelta(settings.MANDATORY_VACATION_DURATION + 2)


def test_get_date_n_business_days_before():
    geo_id = 225
    days = 10

    with patch('staff.gap.workflows.utils.get_holidays_by_year', fake_get_weekdays_by_year):
        weekdays = fake_get_weekdays_by_year(out_mode=OutModes.weekdays)
        start_date = weekdays[days]
        result = get_date_n_business_days_before(
            start_date=start_date,
            geo_id=geo_id,
            n=days,
        )
        assert result == weekdays[-days - 1]


@pytest.mark.parametrize(
    'gap_data, expected_result',
    (
        ({'date_from': {'old': date(2020, 1, 1), 'new': date(2020, 1, 1)}}, False),
        ({'date_from': {'old': date(2020, 1, 1), 'new': date(2021, 1, 1)}}, True),
        (
            {
                'date_from': {'old': date(2020, 1, 1), 'new': date(2020, 1, 1)},
                'date_to': {'old': date(2020, 1, 1), 'new': date(2021, 1, 1)},
            },
            False,
        ),
    ),
)
def test_gap_year_changed(gap_data: dict, expected_result: bool) -> None:
    assert gap_year_changed(gap_data) == expected_result
