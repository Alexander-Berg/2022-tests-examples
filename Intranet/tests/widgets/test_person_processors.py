# coding: utf-8
from __future__ import unicode_literals

import json
from datetime import date, datetime
from decimal import Decimal

import arrow
import pytest
from cia_stuff.oebs import utils as oebs_utils
from dateutil.relativedelta import relativedelta

from cab.utils import datetimes, std
from cab.widgets.persons import const, processors

from tests.data import review_data


BONUS_HISTORY_EXAMPLE = [
        {
            "effectiveDate": "2015-01-01",
            "currency": "RUB",
            "summ": 15000
        },
        {
            "effectiveDate": "2015-06-01",
            "currency": "RUB",
            "summ": 50000
        },
]

SALARY_HISTORY_EXAMPLE = [
    {
        "dateTo": "2015-01-31",
        "dateFrom": "2014-01-01",
        "salarySum": 30000,
        "currency": "RUB"
    },
    {
        "dateTo": "4712-12-31",
        "dateFrom": "2015-02-01",
        "salarySum": 50000,
        "currency": "RUB"
    },
]

BI_EXPECTED_FORECAST = {
    u'currency': u'RUB',
    u'date': date(2020, 5, 1),
    u'dateTo': date(2021, 4, 30),
    u'value': None,
}

BI_EXPECTED_ACTUAL = {
    u'currency': u'RUB',
    u'date': date(2019, 5, 1),
    u'dateTo': date(2020, 4, 30),
    u'value': None,
}


def test_calculate_bonus_part_normal():
    bonus_history = oebs_utils.BonusHistory(BONUS_HISTORY_EXAMPLE)
    salary_history = oebs_utils.SalaryHistory(SALARY_HISTORY_EXAMPLE)

    bonus_part = processors._calculate_bonus_part(
        bonus_history,
        salary_history,
        from_date=datetimes.parse_date('2014-12-01'),
        to_date=datetimes.parse_date('2015-12-01'),
    )
    assert bonus_part == Decimal('1.5')


def test_calculate_bonus_part_for_newbie():
    bonuses_small = BONUS_HISTORY_EXAMPLE[1:]
    salary_small = SALARY_HISTORY_EXAMPLE[1:]

    bonus_history = oebs_utils.BonusHistory(bonuses_small)
    salary_history = oebs_utils.SalaryHistory(salary_small)

    bonus_part = processors._calculate_bonus_part(
        bonus_history,
        salary_history,
        from_date=datetimes.parse_date('2014-12-01'),
        to_date=datetimes.parse_date('2015-12-01'),
    )
    assert std.rounded_decimal(bonus_part, places=1) == Decimal('1.2')


@pytest.mark.xfail
def test_process_salary_last_year():
    today = arrow.now()
    date_deltas = [1000, 315, 134]
    date_ranges = [365 - date_deltas[1], date_deltas[1] - date_deltas[2], date_deltas[2]]
    dates_from = [today.replace(days=-delta).date().isoformat() for delta in date_deltas]
    dates_to = [today.replace(days=-(delta + 1)).date().isoformat() for delta in date_deltas]
    salaries = [140000, 150000, 155000]
    salary_history = [
        {u'salarySum': salaries[0], u'dateTo': dates_to[1], u'dateFrom': dates_from[0], u'currency': u'RUB'},
        {u'salarySum': salaries[1], u'dateTo': dates_to[2], u'dateFrom': dates_from[1], u'currency': u'RUB'},
        {u'salarySum': salaries[2], u'dateTo': u'4712-12-31', u'dateFrom': dates_from[2], u'currency': u'RUB'}
    ]
    result = processors.process_salary_last_year({'salary_history': salary_history})
    expected_result = sum([
        int(salary * days / const.AVG_DAYS_IN_MONTH)
        for salary, days in zip(salaries, date_ranges)
    ])
    assert sum(date_ranges) == 365
    assert -2 <= result['value'] - expected_result <= 2


def test_process_review():
    expecting = [
        {
            'mark': 'A',
            'start_date': '2010-01-01',
            'goldstar': 'option_and_bonus',
        },
        {
            'mark': 'C',
            'start_date': '2012-01-01',
            'goldstar': 'no_goldstar',
        },
    ]
    review_answer = [
        {
            'status': 'announced',
            'mark': expecting[0]['mark'],
            'goldstar': expecting[0]['goldstar'],
            'person': {
                'login': 'user',
                'first_name': 'Nameless',
                'last_name': 'One',
                'id': 666,
            },
            'review': {
                'start_date': expecting[0]['start_date'],
                'finish_date': '2010-02-01',
                'id': 1,
                'type': 'normal',
            }
        },
        {
            'status': 'announced',
            'mark': 'B',
            'goldstar': 'no_goldstar',
            'person': {
                'login': 'user',
                'first_name': 'Nameless',
                'last_name': 'One',
                'id': 666,
            },
            'review': {
                'start_date': '2011-01-01',
                'finish_date': '2011-02-01',
                'id': 2,
                'type': 'middle',
            }
        },
        {
            'status': 'announced',
            'mark': expecting[1]['mark'],
            'goldstar': '_DISABLED_',
            'person': {
                'login': 'user',
                'first_name': 'Nameless',
                'last_name': 'One',
                'id': 666,
            },
            'review': {
                'start_date': expecting[1]['start_date'],
                'finish_date': '2012-02-01',
                'id': 3,
                'type': 'normal',
            }
        },
    ]
    processed = processors.process_reviews(review_answer)
    assert processed == expecting


@pytest.mark.parametrize('processor_func', (
    processors.process_salary,
    processors.process_salary_forecast,
    processors.process_options_vesting_forecast_cost,
    processors.process_incomes,
    processors.process_income_forecast,
    processors.process_year_bonus_payment,
    processors.process_bonus_forecast,
    processors.process_signup,
    processors.process_piecerate,
))
def test_bi_processors_no_exception(processor_func):
    data = json.loads(review_data.BI_INCOME_EXAMPLE)
    result = processor_func(data)
    assert result


def test_process_salary():
    data = json.loads(review_data.BI_INCOME_EXAMPLE)
    expected = {
        u'currency': u'RUB',
        u'date': datetime.now().date().replace(day=1) - relativedelta(days=1),
        u'value': 105200,
    }
    result = processors.process_salary(data)
    assert result == expected


@pytest.mark.parametrize('processor_func, value, expected', (
    (
        processors.process_salary_forecast,
        1262400,
        BI_EXPECTED_FORECAST,
    ),
    (
        processors.process_options_vesting_forecast_cost,
        69338268.12,
        BI_EXPECTED_FORECAST,
    ),
    (
        processors.process_incomes,
        69239784.8,
        BI_EXPECTED_ACTUAL,
    ),
    (
        processors.process_income_forecast,
        70600668.12,
        BI_EXPECTED_FORECAST,
    ),
    (
        processors.process_year_bonus_payment,
        152520,
        BI_EXPECTED_ACTUAL,
    ),
    (
        processors.process_bonus_forecast,
        305040,
        BI_EXPECTED_FORECAST,
    ),
    (
        processors.process_signup,
        450000,
        BI_EXPECTED_FORECAST,
    ),
    (
        processors.process_piecerate,
        95500,
        BI_EXPECTED_ACTUAL,
    ),
))
def test_bi_processors_results(processor_func, value, expected):
    data = json.loads(review_data.BI_INCOME_EXAMPLE)
    expected = dict(expected)
    expected['value'] = value
    result = processor_func(data)
    assert result == expected


@pytest.mark.parametrize('is_main, actual_fte, expected_fte', (
    (True, 1, 1),
    (True, 0.5, 0.5),
    (False, 0.5, 0),
))
def test_process_fte(is_main, actual_fte, expected_fte):
    data = json.loads(review_data.BI_ASSIGNMENT_EXAMPLE)
    data[0]['rate'] = actual_fte
    data[0]['main'] = is_main

    result = processors.process_fte(data)
    assert result == expected_fte
