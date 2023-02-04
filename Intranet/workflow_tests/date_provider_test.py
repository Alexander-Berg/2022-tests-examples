import pytest

from datetime import date
from dateutil.relativedelta import relativedelta
from random import randrange

from staff.budget_position.workflow_service.entities.workflows.date_provider import DateProvider


def test_today():
    assert DateProvider().today().strftime('%Y-%m-%d') == date.today().strftime('%Y-%m-%d')


TEST_DATES = (
    date(2021, 2, 2),
    date(2024, 2, 2),
    date(2020, 12, 31),
    date(2020, 1, 1),
)


@pytest.mark.parametrize('current', TEST_DATES)
def test_get_month_last_day(current):
    result = DateProvider().get_month_last_day(current)
    next_date = date(current.year, current.month, result) + relativedelta(days=1)

    assert next_date.day == 1


@pytest.mark.parametrize('current', TEST_DATES)
def test_add(current):
    for _ in range(50):
        days = randrange(100) - 50
        months = randrange(100) - 50

        result = DateProvider().add(current, days=days, months=months)
        assert result == current + relativedelta(days=days, months=months)
