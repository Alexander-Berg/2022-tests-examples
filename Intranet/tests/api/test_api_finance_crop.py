# coding: utf-8

from datetime import date

from review.lib import datetimes
from review.shortcuts import const

from tests import helpers


SALARY_HISTORY = [
    {
        "salarySum": 100,
        "currency": 'RUB',
        "basis": "MONTHLY",
        "dateFrom": '2010-01-01',
        "dateTo": '2010-05-31',
    },
    {
        "salarySum": 200,
        "currency": 'RUB',
        "basis": "MONTHLY",
        "dateFrom": '2010-06-01',
        "dateTo": '4712-12-31',
    },
]


def test_crop_by_last_month(client, finance_builder):
    finance = finance_builder(
        salary_history=SALARY_HISTORY,
        generate_fields=const.OEBS_DATA_TYPES,
    )
    person = finance.person

    result = helpers.get_json(
        client=client,
        login=person.login,
        path='/v1/finance/',
        request={
            'persons': [person.login],
            'fields': [const.SALARY_HISTORY],
        },
    )['result']
    salary_history = result[person.login][const.SALARY_HISTORY]

    today = datetimes.today()
    last_day_of_previous_month = datetimes.shifted(
        date(
            year=today.year,
            month=today.month,
            day=1,
        ),
        days=-1,
    )
    helpers.assert_is_substructure(
        [
            {
                "salarySum": 100,
                "dateFrom": '2010-01-01',
                "dateTo": '2010-05-31',
            },
            {
                "salarySum": 200,
                "dateFrom": '2010-06-01',
                "dateTo": last_day_of_previous_month.isoformat(),
            },
        ],
        salary_history,
    )


def test_crop_by_active_review(
    client,
    finance_builder,
    review_builder,
    person_review_builder,
):
    finance = finance_builder(
        salary_history=SALARY_HISTORY,
        generate_fields=const.OEBS_DATA_TYPES,
    )
    person = finance.person
    start_date = datetimes.shifted(datetimes.today(), months=-1)
    review = review_builder(
        start_date=start_date,
        status=const.REVIEW_STATUS.FINISHED,
    )
    person_review_builder(
        review=review,
        person=person,
        status=const.PERSON_REVIEW_STATUS.EVALUATION,
    )

    result = helpers.get_json(
        client=client,
        login=person.login,
        path='/v1/finance/',
        request={
            'persons': [person.login],
            'fields': [const.SALARY_HISTORY],
        },
    )['result']
    salary_history = result[person.login][const.SALARY_HISTORY]

    day_before_review_start = datetimes.shifted(start_date, days=-1)
    helpers.assert_is_substructure(
        [
            {
                "salarySum": 100,
                "dateFrom": '2010-01-01',
                "dateTo": '2010-05-31',
            },
            {
                "salarySum": 200,
                "dateFrom": '2010-06-01',
                "dateTo": day_before_review_start.isoformat(),
            },
        ],
        salary_history,
    )
