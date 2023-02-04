# coding: utf-8
import pytest

from review.oebs.sync import store
from review.oebs.sync import fake
from review.oebs import logic
from review.shortcuts import const

CURRENT_LOANS_EXAMPLE = [
    {
        "contractDate": '2018-01-01',
        "contractNum": '100500',
        "loanType": "20",
        "loanSum": 1000000,
        "currency": "RUB",
        "balance": 500000,
    }
]


def test_finance_non_list_data(person):
    store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert finance[const.CURRENT_SALARY]


def test_finance_list_data(person):
    store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert finance[const.SALARY_HISTORY]


def test_empty_list_correctly_overrides_data(person, mocker):
    data = fake.generate_data(const.OEBS_DATA_TYPES, [person.login])

    data[person.login][const.CURRENT_LOANS] = CURRENT_LOANS_EXAMPLE
    with mocker.patch(
        'review.oebs.sync.fetch.get_oebs_data',
        return_value=data,
    ):
        store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert finance[const.CURRENT_LOANS] == CURRENT_LOANS_EXAMPLE

    data = fake.generate_data(const.OEBS_DATA_TYPES, [person.login])
    data[person.login][const.CURRENT_LOANS] = []
    with mocker.patch(
        'review.oebs.sync.fetch.get_oebs_data',
        return_value=data,
    ):
        store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert not finance[const.CURRENT_LOANS]


def test_error_not_overrides_data(person, mocker):
    data = fake.generate_data(const.OEBS_DATA_TYPES, [person.login])

    data[person.login][const.CURRENT_LOANS] = CURRENT_LOANS_EXAMPLE
    with mocker.patch(
        'review.oebs.sync.fetch.get_oebs_data',
        return_value=data,
    ):
        store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert finance[const.CURRENT_LOANS] == CURRENT_LOANS_EXAMPLE

    data = fake.generate_data(const.OEBS_DATA_TYPES, [person.login])
    data[person.login][const.CURRENT_LOANS] = const.OEBS_DATA_FETCH_ERROR
    with mocker.patch(
        'review.oebs.sync.fetch.get_oebs_data',
        return_value=data,
    ):
        store.sync_finance_data()

    finance = logic.get_person_finance_data(person.login)
    assert finance[const.CURRENT_LOANS] == CURRENT_LOANS_EXAMPLE


@pytest.mark.parametrize('field_name', set(const.FINANCE_DB_FIELDS) - {const.SALARY_HISTORY, const.GRADE_HISTORY})
def test_oebs_no_data(person, finance_builder, field_name):
    finance_builder(**{'person': person, field_name: None})

    finance = logic.get_person_finance_data(person.login)
    assert finance[field_name] is None
