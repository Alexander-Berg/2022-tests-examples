# coding: utf-8
import mock
import pytest
from pretend import stub
from datetime import date

from review.lib import datetimes
from review.shortcuts import const
from review.core.logic import assemble

from tests import helpers


FIRST_SALARY_HISTORY = [
    {
        "salarySum": 100,
        "currency": const.DEFAULT_CURRENCIES.RUB,
        "dateFrom": "2010-01-01",
        "dateTo": "2010-12-31",
    },
    {
        "salarySum": 200,
        "currency": const.DEFAULT_CURRENCIES.RUB,
        "dateFrom": "2011-01-01",
        "dateTo": const.MAX_HISTORY_DATE,
    },
]
SECOND_SALARY_HISTORY = [
    {
        "salarySum": 500,
        "currency": const.DEFAULT_CURRENCIES.RUB,
        "dateFrom": "2009-01-01",
        "dateTo": "2009-12-31",
    },
    {
        "salarySum": 600,
        "currency": const.DEFAULT_CURRENCIES.RUB,
        "dateFrom": "2010-01-01",
        "dateTo": const.MAX_HISTORY_DATE,
    },
]


@pytest.fixture
def test_data(finance_builder):
    finance_one = finance_builder(**{
        const.SALARY_HISTORY: FIRST_SALARY_HISTORY,
    })
    finance_two = finance_builder(**{
        const.SALARY_HISTORY: SECOND_SALARY_HISTORY,
    })
    return stub(
        person_one=finance_one.person,
        person_two=finance_two.person,
        finance_one=finance_one,
        finance_two=finance_two,
    )


def test_salary_value_different_persons_in_one_review(
    test_data,
    review_role_superreviewer,
    person_review_builder,
):
    review = review_role_superreviewer.review
    helpers.update_model(
        review,
        start_date='2010-02-01',
        salary_date=None,
    )
    pr_one = person_review_builder(review=review, person=test_data.person_one)
    pr_two = person_review_builder(review=review, person=test_data.person_two)

    person_reviews = assemble.get_person_reviews(
        subject=review_role_superreviewer.person,
        filters_chosen={
            const.FILTERS.IDS: [pr_one.id, pr_two.id]
        },
        fields_requested=[const.FIELDS.SALARY_VALUE],
    )
    person_reviews = {
        person_review.id: person_review
        for person_review in person_reviews
    }

    assert person_reviews[pr_one.id].salary_value == 100
    assert person_reviews[pr_two.id].salary_value == 600


def test_salary_value_different_persons_in_two_reviews(
    test_data,
    review_role_builder,
    person_review_builder,
):
    pr_one = person_review_builder(person=test_data.person_one)
    pr_two = person_review_builder(person=test_data.person_two)
    review_one = pr_one.review
    review_two = pr_two.review
    helpers.update_model(
        review_one,
        start_date='2012-01-01',
    )
    helpers.update_model(
        review_two,
        start_date='2012-01-01',
    )
    superreviewer = review_role_builder(
        review=review_one,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    review_role_builder(
        person=superreviewer.person,
        review=review_two,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )

    person_reviews = assemble.get_person_reviews(
        subject=superreviewer.person,
        filters_chosen={
            const.FILTERS.IDS: [pr_one.id, pr_two.id]
        },
        fields_requested=[const.FIELDS.SALARY_VALUE],
    )
    person_reviews = {
        person_review.id: person_review
        for person_review in person_reviews
    }

    assert person_reviews[pr_one.id].salary_value == 200
    assert person_reviews[pr_two.id].salary_value == 600


def test_salary_value_same_person_in_two_reviews(
    test_data,
    person_review_builder,
):
    pr_one = person_review_builder(
        person=test_data.person_one,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
    )
    pr_two = person_review_builder(
        person=test_data.person_one,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
    )
    review_one = pr_one.review
    review_two = pr_two.review

    helpers.update_model(
        review_one,
        start_date='2010-02-01',
        salary_date='2010-03-04',
    )
    helpers.update_model(
        review_two,
        start_date='2011-02-01',
        salary_date='2011-03-04',
    )

    person_reviews = assemble.get_person_reviews(
        subject=test_data.person_one,
        filters_chosen={
            const.FILTERS.IDS: [pr_one.id, pr_two.id]
        },
        fields_requested=[const.FIELDS.SALARY_VALUE],
    )
    person_reviews = {
        person_review.id: person_review
        for person_review in person_reviews
    }

    assert person_reviews[pr_one.id].salary_value == 100
    assert person_reviews[pr_two.id].salary_value == 200


def test_salary_counts_by_salary_date(
        person_builder,
        review_builder,
        person_review_builder,
        finance_builder,
        review_role_builder):
    # salarySum:             300                 700                    900
    # FinanceEvents:    ┌────────────┐┌──────────────────────────┐┌──────────────>
    # Review: ----------------[----x--------------x-----|-----x-----x----]------->
    # Review dates:         start  1              2   salary  3     4  finish
    person = person_builder()
    observer = person_builder()
    today = date(2030, 6, 18)
    review = review_builder(**{
        'start_date': datetimes.shifted(today, months=-2),
        'salary_date': datetimes.shifted(today, months=+1),
        'finish_date': datetimes.shifted(today, months=+2),
    })
    salary_history = [
        {
            "salarySum": 300,
            "currency": 'RUB',
            "basis": "MONTHLY",
            "dateFrom": datetimes.shifted(review.start_date, days=-5).isoformat(),
            "dateTo": datetimes.shifted(review.start_date, days=5).isoformat(),
        },
        {
            "salarySum": 700,
            "currency": 'RUB',
            "basis": "MONTHLY",
            "dateFrom": datetimes.shifted(review.start_date, days=5).isoformat(),
            "dateTo": datetimes.shifted(review.salary_date, days=5).isoformat(),
        },
        {
            "salarySum": 900,
            "currency": 'RUB',
            "basis": "MONTHLY",
            "dateFrom": datetimes.shifted(review.salary_date, days=5).isoformat(),
            "dateTo": '4712-12-31',
        },
    ]
    finance_builder(person=person, salary_history=salary_history)

    person_review = person_review_builder(person=person, review=review)

    review_role_builder(
        person=observer,
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )

    # 1
    with mock.patch('review.lib.datetimes.today', return_value=datetimes.shifted(review.start_date, days=2)):
        person_reviews = assemble.get_person_reviews(
            subject=observer,
            filters_chosen={const.FILTERS.IDS: [person_review.id]},
            fields_requested=[const.FIELDS.SALARY_VALUE],
        )
        person_review = next(iter(person_reviews))
        assert person_review.salary_value == 300

    # 2
    with mock.patch('review.lib.datetimes.today', return_value=datetimes.shifted(review.salary_date, days=-2)):
        person_reviews = assemble.get_person_reviews(
            subject=observer,
            filters_chosen={const.FILTERS.IDS: [person_review.id]},
            fields_requested=[const.FIELDS.SALARY_VALUE],
        )
        person_review = next(iter(person_reviews))
        assert person_review.salary_value == 700

    # 3
    with mock.patch('review.lib.datetimes.today', return_value=datetimes.shifted(review.salary_date, days=2)):
        person_reviews = assemble.get_person_reviews(
            subject=observer,
            filters_chosen={const.FILTERS.IDS: [person_review.id]},
            fields_requested=[const.FIELDS.SALARY_VALUE],
        )
        person_review = next(iter(person_reviews))
        assert person_review.salary_value == 700

    # 4
    with mock.patch('review.lib.datetimes.today', return_value=datetimes.shifted(review.finish_date, days=-2)):
        person_reviews = assemble.get_person_reviews(
            subject=observer,
            filters_chosen={const.FILTERS.IDS: [person_review.id]},
            fields_requested=[const.FIELDS.SALARY_VALUE],
        )
        person_review = next(iter(person_reviews))
        assert person_review.salary_value == 700
