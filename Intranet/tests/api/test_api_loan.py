# coding: utf-8
import mock
import pytest

from review.core import const
from review.oebs import loan_api

from tests import helpers
from tests.helpers import waffle_switch

OEBS_MOCK = loan_api.OebsLoan(logins=[loan_api.OebsLoan.Login(
    NDFL=1,
    avgIncome=2,
    salarySum=3,
    holdAmount=4,
    matProfitNDFL=5,
)])


def test_loan_info(client, bi_income_builder, bi_vesting_builder, test_person):
    avg_salary, salary = 100500, 15
    bi_income_builder(
        person=test_person,
        ext_data={
            'AVG_SAL': avg_salary,
            'SALARY': salary,
        },
    )
    bi_vesting_builder(person=test_person)

    with mock.patch('review.finance.logic.loan_api.get_person_loan', return_value=OEBS_MOCK):
        resp_json = helpers.get_json(
            client,
            '/v1/loan/info/',
            login=test_person.login,
        )

    assert resp_json['refinanceRate'] == OEBS_MOCK.refinanceRate
    assert len(resp_json['logins']) == 1
    assert resp_json['logins'][0]['NDFL'] == OEBS_MOCK.logins[0].NDFL
    assert resp_json['logins'][0]['avgIncome'] == avg_salary
    assert resp_json['logins'][0]['salarySum'] == salary
    assert resp_json['logins'][0]['holdAmount'] == OEBS_MOCK.logins[0].holdAmount
    assert resp_json['logins'][0]['matProfitNDFL'] == OEBS_MOCK.logins[0].matProfitNDFL


def test_get_loan_available_exception_on_get_person_loan(
    client,
    bi_income_builder,
    test_person,
):
    bi_income_builder(person=test_person)
    with mock.patch('review.finance.logic.loan_api.get_person_loan', side_effect=Exception()):
        resp_json = helpers.get_json(
            client,
            '/v1/loan/requirements/?person={}'.format(test_person.login),
            login=test_person.login,
        )
        assert resp_json


def test_loan_info_exception_on_get_person_loan(
    client,
    bi_income_builder,
    bi_vesting_builder,
    test_person,
):
    bi_income_builder(person=test_person)
    bi_vesting_builder(person=test_person)
    with mock.patch('review.finance.logic.loan_api.get_person_loan', side_effect=Exception()):
        resp_json = helpers.get_json(
            client,
            '/v1/loan/info/',
            login=test_person.login,
            expect_status=400,
        )
        assert resp_json['errors']['*']['msg'][0] == 'Fail to fetch data from oebs'


def test_loan_info_no_income(
    client,
    test_person,
):
    with mock.patch('review.finance.logic.loan_api.get_person_loan', return_value=OEBS_MOCK):
        resp_json = helpers.get_json(
            client,
            '/v1/loan/info/',
            login=test_person.login,
            expect_status=404,
        )
        expected_err = 'Object BiPersonIncome {} was not found'.format(test_person.login)
        assert resp_json['errors']['*']['msg'][0] == expected_err


@pytest.mark.parametrize(
    'ext_data, expected_has_not_vested, expected_not_vested_data',
    [
        ({'EFF01_VESTCASHPAY': 100, 'EFF01_DEFPAY': 0}, True, [('vesting_cash_payment', 100)]),
        ({'EFF01_VESTCASHPAY': 0, 'EFF01_DEFPAY': 100}, True, [('deferred_payment', 100)]),
        (
            {'EFF01_DEFPAY': 100, 'EFF01_VESTCASHPAY': 200},
            True,
            [('deferred_payment', 100), ('vesting_cash_payment', 200)]
        ),
        ({'EFF01_VESTCASHPAY': 0, 'EFF01_DEFPAY': 0}, False, []),
        ({'EFF1_VESTCASHPAY': 100, 'EFF01_VESTCASHPAY': 0, 'EFF01_DEFPAY': 0}, False, []),
        ({'EFF1_DEFPAY': 100, 'EFF01_VESTCASHPAY': 0, 'EFF01_DEFPAY': 0}, False, []),
    ]
)
def test_deferred_paymenents_counts_as_not_vested(
    ext_data,
    expected_has_not_vested,
    expected_not_vested_data,
    client,
    bi_income_builder,
    bi_vesting_builder,
    test_person,
):
    waffle_switch('use_deferred_payments_as_not_vested_options_for_loan')
    bi_income_builder(person=test_person, ext_data=ext_data)
    bi_vesting_builder(person=test_person)
    resp = helpers.get_json(
        client,
        '/v1/loan/requirements/?person={}'.format(test_person.login),
        login=test_person.login,
    )
    assert resp['requirements']['has_not_vested_options'] == expected_has_not_vested

    expected_not_vested = []
    for type, amount in expected_not_vested_data:
        expected_not_vested.append({'amount': amount, 'in_rub': None, 'in_usd': None, 'type': type})
    assert resp['not_vested'] == expected_not_vested


@pytest.mark.parametrize(
    'person_grade,can_see_not_vested',
    [
        (18, True),
        (19, False),
    ]
)
def test_deferred_payments_as_not_vested_for_loan_viewer(
    person_grade,
    can_see_not_vested,
    client,
    bi_income_builder,
    bi_vesting_builder,
    person_builder,
    global_role_builder,
):
    waffle_switch('use_deferred_payments_as_not_vested_options_for_loan')
    person = person_builder()
    observer = person_builder()
    global_role_builder(person=observer, type=const.ROLE.GLOBAL.LOAN_VIEWER)

    bi_income_builder(person=person, ext_data={'GRADE_MAIN': str(person_grade), 'EFF01_DEFPAY': 1})
    bi_vesting_builder(person=person)

    resp = helpers.get_json(
        client,
        '/v1/loan/requirements/?person={}'.format(person.login),
        login=observer.login,
    )

    if can_see_not_vested:
        assert len(resp['not_vested']) > 0
    else:
        assert resp['not_vested'] is None
