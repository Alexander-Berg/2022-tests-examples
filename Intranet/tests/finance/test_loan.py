import json
import os

import attr
import pytest

from review.bi import logic as bi_logic
from review.finance.loan import (
    HIGH_GRADE_FOR_SHORT_LOAN,
    HIGH_GRADE_FOR_LONG_LOAN,
    LOW_GRADE_FOR_LONG_LOAN,
    LOW_GRADE_FOR_SHORT_LOAN,
    check_loan_req,
    check_mark,
    NEW_SCALES_FORMAT_STARTS_AT,
    SALARY_TYPE,
)
from review.oebs.loan_api import OebsLoan
from tests.helpers import waffle_switch


CUR_SCALE = NEW_SCALES_FORMAT_STARTS_AT
OLD_SCALE = NEW_SCALES_FORMAT_STARTS_AT - 1

HIGH_GRADE = HIGH_GRADE_FOR_LONG_LOAN + 1
VERY_HIGH_GRADE = HIGH_GRADE_FOR_LONG_LOAN + 3


def load_json(file_path):
    with open(os.path.join(os.path.dirname(__file__), file_path), 'r') as json_data:
        return json.load(json_data)


@pytest.fixture()
def login():
    return 'user'


@pytest.fixture()
def oebs_loan():
    data = load_json('./fixtures/oebs_loan.json')
    return OebsLoan(
        logins=[OebsLoan.Login(**data['logins'][0])],
        refinanceRate=2,
    )


@pytest.fixture()
def not_vested():
    return [{
        "type": "RSU",
        "amount": 1,
        "in_rub": "32",
        "in_usd": "1"
    }]


@pytest.fixture()
def person_income():
    # type: () -> bi_logic.PersonIncome
    return bi_logic.PersonIncome(
        grade_current=VERY_HIGH_GRADE,
        grade_last=HIGH_GRADE,
        mark_last=bi_logic.marks.Mark(
            mark='C',
            scale_id=OLD_SCALE,
        ),
        mark_current=bi_logic.marks.Mark(
            mark='outstanding',
            scale_id=CUR_SCALE,
        ),
    )


@pytest.fixture()
def data_request_errors():
    return {}


def get_requirements_dict(**params):
    res = check_loan_req(**params)
    return json.loads(json.dumps(res.as_dict()))


def test_check_loan_req_no_reviews(oebs_loan, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=bi_logic.PersonIncome(),
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['long_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['last_reviews_results'] == []
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_one_enough_review(oebs_loan, data_request_errors):
    person_income = bi_logic.PersonIncome(
        grade_current=LOW_GRADE_FOR_LONG_LOAN,
        mark_current=bi_logic.marks.Mark(
            mark='great',
            scale_id=CUR_SCALE,
        ),
        grade_main=VERY_HIGH_GRADE,
    )
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        None,
    ]
    assert results['long_loan_missing_requirements'] == [{
        'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        None,
    ]
    assert results['last_reviews_results'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_one_not_enough_review(oebs_loan, data_request_errors):
    grade = LOW_GRADE_FOR_SHORT_LOAN - 1
    person_income = bi_logic.PersonIncome(
        grade_current=grade,
        mark_current=bi_logic.marks.Mark(
            mark='great',
            scale_id=CUR_SCALE,
        ),
        grade_main=VERY_HIGH_GRADE,
    )
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['long_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['last_reviews_results'] == [
        {'grade': grade, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_all_ok(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_short_period(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        period=36,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9
    assert results['available_sums'] == {
        'AVERAGE': {
            'max_loan_amount': '375696.00',
            'max_monthly_payment': '10436.00',
        },
        'INCOME_ONLY': {
            'max_loan_amount': '281718.00',
            'max_monthly_payment': '7825.50',
        },
    }


def test_check_loan_req_long_period(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        period=37,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9
    assert results['available_sums'] == {
        'AVERAGE': {
            'max_loan_amount': '368372.22',
            'max_monthly_payment': '10436.00',
        },
        'INCOME_ONLY': {
            'max_loan_amount': '276226.22',
            'max_monthly_payment': '7825.50',
        },
    }


def test_check_loan_req_short_period_matprofit(oebs_loan, person_income, data_request_errors):
    waffle_switch('need_pay_taxes_for_matprofit')
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        period=36,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9
    assert results['available_sums'] == {
        'AVERAGE': {
            'max_loan_amount': '375696.00',
            'max_monthly_payment': '10436.00',
        },
        'INCOME_ONLY': {
            'max_loan_amount': '281718.00',
            'max_monthly_payment': '7825.50',
        },
    }


def test_check_loan_req_long_period_matprofit(oebs_loan, person_income, data_request_errors):
    waffle_switch('need_pay_taxes_for_matprofit')
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        period=37,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9
    assert results['available_sums'] == {
        'AVERAGE': {
            'max_loan_amount': '339330.27',
            'max_monthly_payment': '10436.00',
        },
        'INCOME_ONLY': {
            'max_loan_amount': '254448.93',
            'max_monthly_payment': '7825.50',
        },
    }


def test_check_loan_req_all_ok(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


@pytest.mark.parametrize(
    'grade,show_marks',
    [
        [18, True],
        [19, False],
    ]
)
def test_check_loan_hide_marks_for_outside_observer(
    grade,
    show_marks,
    oebs_loan,
    person_income,
    data_request_errors,
):
    person_income.grade_main = grade
    expected_marks = [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ] if show_marks else None

    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        is_viewing_self=False,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == expected_marks
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


@pytest.mark.django_db
def test_check_loan_req_with_restricted_department(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
        person_department_urls=['yandex_personal_vertserv']
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is None
    assert results['requirements']['has_review_marks_long'] is None
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is True
    assert results['short_loan_missing_requirements'] is None
    assert results['long_loan_missing_requirements'] is None
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is True
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_not_resident(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].accountNotResident = 'Y'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is False
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_on_maternity_leave(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].maternityLeave = 'Y'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is False
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_temporary_job_contract(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].temporaryContract = 'Y'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is False
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_no_experience(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].expYears = 0
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is False
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 0, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_has_loan(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].loanFlag = 'Y'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is False
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_low_marks_short_low_grade(oebs_loan, person_income, data_request_errors):
    person_income.grade_last = LOW_GRADE_FOR_LONG_LOAN
    person_income.mark_last.mark = 'B'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'C', 'scale_id': None},
        None,
    ]
    assert results['long_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'C', 'scale_id': None},
        None,
    ]
    assert results['last_reviews_results'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'B', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_low_marks_long_low_grade(oebs_loan, person_income, data_request_errors):
    person_income.grade_last = HIGH_GRADE_FOR_SHORT_LOAN
    person_income.mark_last.mark = 'good'
    person_income.mark_last.scale_id = CUR_SCALE

    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [
        {'grade': HIGH_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        None,
    ]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE_FOR_SHORT_LOAN, 'mark': 'good', 'scale_id': CUR_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_low_marks_high_grade(oebs_loan, person_income, data_request_errors):
    person_income.grade_last = VERY_HIGH_GRADE
    person_income.mark_last.mark = 'B'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': VERY_HIGH_GRADE, 'mark': 'C', 'scale_id': None},
        None,
    ]
    assert results['long_loan_missing_requirements'] == [
        {'grade': VERY_HIGH_GRADE, 'mark': 'C', 'scale_id': None},
        None,
    ]
    assert results['last_reviews_results'] == [
        {'grade': VERY_HIGH_GRADE, 'mark': 'B', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_no_grants(oebs_loan, person_income, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=False,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is False

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_no_oebs_loan(person_income):
    error_text = 'OEBS error'
    results = get_requirements_dict(
        oebs_loan=None,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors={
            'oebs_loan': error_text
        }
    )

    assert results['requirements']['is_resident'] is None
    assert results['requirements']['is_not_on_maternity_leave'] is None
    assert results['requirements']['has_permanent_job_contract'] is None
    assert results['requirements']['has_been_working_one_year'] is None
    assert results['requirements']['does_not_have_loan'] is None
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is None
    assert results['is_long_loan_available'] is None
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] is None
    assert results['skip_grade_checks'] is False

    assert results['has_errors'] is True
    assert results['errors']['is_resident'] == error_text
    assert results['hold_amount'] is None
    assert results['exec_doc'] is None


def test_check_loan_req_no_person_income(oebs_loan):
    error_text = 'REVIEW error: wrong data format'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=None,
        has_not_vested=True,
        not_vested=None,
        data_request_errors={
            'person_income': error_text
        }
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is None
    assert results['requirements']['has_review_marks_long'] is None
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is None
    assert results['is_long_loan_available'] is None
    assert results['short_loan_missing_requirements'] is None
    assert results['long_loan_missing_requirements'] is None
    assert results['last_reviews_results'] is None
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False

    assert results['has_errors'] is True
    assert results['errors']['has_review_marks_short'] == error_text
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_no_options_history(oebs_loan, person_income):
    error_text = 'REVIEW error'
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=None,
        not_vested=None,
        data_request_errors={
            'finance': error_text
        }
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is None

    assert results['is_short_loan_available'] is True
    assert results['is_long_loan_available'] is None
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False

    assert results['has_errors'] is True
    assert results['errors']['has_not_vested_options'] == error_text
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9


def test_check_loan_req_oebs_hidden_info(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].hiddenInfo = True
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is None
    assert results['requirements']['is_not_on_maternity_leave'] is None
    assert results['requirements']['has_permanent_job_contract'] is None
    assert results['requirements']['has_been_working_one_year'] is None
    assert results['requirements']['does_not_have_loan'] is None
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is None
    assert results['is_long_loan_available'] is None
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] is None

    assert results['has_errors'] is True
    assert results['errors']['is_resident'] == 'OEBS error: hidden info'
    assert results['hold_amount'] is None
    assert results['exec_doc'] is None


def test_check_loan_req_oebs_wrong_data_format(oebs_loan, person_income, data_request_errors):
    oebs_loan.logins[0].expYears = None
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=person_income,
        has_not_vested=True,
        not_vested=None,
        data_request_errors=data_request_errors,
    )

    assert results['requirements']['is_resident'] is None
    assert results['requirements']['is_not_on_maternity_leave'] is None
    assert results['requirements']['has_permanent_job_contract'] is None
    assert results['requirements']['has_been_working_one_year'] is None
    assert results['requirements']['does_not_have_loan'] is None
    assert results['requirements']['has_review_marks_short'] is True
    assert results['requirements']['has_review_marks_long'] is True
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is None
    assert results['is_long_loan_available'] is None
    assert results['short_loan_missing_requirements'] == [None, None]
    assert results['long_loan_missing_requirements'] == [None, None]
    assert results['last_reviews_results'] == [
        {'grade': HIGH_GRADE, 'mark': 'C', 'scale_id': OLD_SCALE},
        {'grade': VERY_HIGH_GRADE, 'mark': 'outstanding', 'scale_id': CUR_SCALE},
    ]
    assert results['not_vested'] is None
    assert results['work_experience'] is None
    assert results['skip_grade_checks'] is False

    assert results['has_errors'] is True
    assert results['errors']['is_resident'] == 'OEBS error: wrong data format'
    assert results['hold_amount'] is None
    assert results['exec_doc'] is None


@pytest.mark.parametrize('grade, mark, low_grade, high_grade, required_grade, required_mark', [
    (0, '-', LOW_GRADE_FOR_SHORT_LOAN, HIGH_GRADE_FOR_SHORT_LOAN, LOW_GRADE_FOR_SHORT_LOAN, 'great'),
    (25, '-', LOW_GRADE_FOR_SHORT_LOAN, HIGH_GRADE_FOR_SHORT_LOAN, 25, 'good'),

    (
        LOW_GRADE_FOR_SHORT_LOAN - 1,
        'D',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        LOW_GRADE_FOR_SHORT_LOAN,
        'C',
    ),
    (
        LOW_GRADE_FOR_SHORT_LOAN,
        'D',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        None,
        None,
    ),
    (
        LOW_GRADE_FOR_LONG_LOAN,
        'B',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        LOW_GRADE_FOR_LONG_LOAN,
        'C',
    ),
    (
        HIGH_GRADE_FOR_SHORT_LOAN,
        'B',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        'C',
    ),
    (
        HIGH_GRADE_FOR_SHORT_LOAN,
        'C',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        None,
        None,
    ),
    (
        HIGH_GRADE_FOR_LONG_LOAN,
        'B',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_LONG_LOAN,
        'C',
    ),

    (
        LOW_GRADE_FOR_SHORT_LOAN - 1,
        'good',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        LOW_GRADE_FOR_SHORT_LOAN,
        'great',
    ),
    (
        LOW_GRADE_FOR_SHORT_LOAN,
        'great',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        None,
        None,
    ),
    (
        LOW_GRADE_FOR_LONG_LOAN,
        'good',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        LOW_GRADE_FOR_LONG_LOAN,
        'great',
    ),
    (
        HIGH_GRADE_FOR_SHORT_LOAN,
        'below',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        'good',
    ),
    (
        HIGH_GRADE_FOR_SHORT_LOAN,
        'good',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        None,
        None,
    ),
    (
        HIGH_GRADE_FOR_LONG_LOAN,
        'below',
        LOW_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_SHORT_LOAN,
        HIGH_GRADE_FOR_LONG_LOAN,
        'good',
    ),
])
def test_check_mark(grade, mark, low_grade, high_grade, required_grade, required_mark):
    assert check_mark(grade, mark, low_grade, high_grade) == (required_grade, required_mark)


def test_check_loan_req_with_not_vested(oebs_loan, not_vested, data_request_errors):
    results = get_requirements_dict(
        oebs_loan=oebs_loan,
        person_income=bi_logic.PersonIncome(),
        has_not_vested=True,
        not_vested=not_vested,
        data_request_errors=data_request_errors
    )

    assert results['requirements']['is_resident'] is True
    assert results['requirements']['is_not_on_maternity_leave'] is True
    assert results['requirements']['has_permanent_job_contract'] is True
    assert results['requirements']['has_been_working_one_year'] is True
    assert results['requirements']['does_not_have_loan'] is True
    assert results['requirements']['has_review_marks_short'] is False
    assert results['requirements']['has_review_marks_long'] is False
    assert results['requirements']['has_not_vested_options'] is True

    assert results['is_short_loan_available'] is False
    assert results['is_long_loan_available'] is False
    assert results['short_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_SHORT_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['long_loan_missing_requirements'] == [
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
        {'grade': LOW_GRADE_FOR_LONG_LOAN, 'mark': 'great', 'scale_id': CUR_SCALE},
    ]
    assert results['last_reviews_results'] == []
    assert results['not_vested'] == not_vested
    assert results['work_experience'] == {'years': 5, 'months': 4, 'days': 2}
    assert results['skip_grade_checks'] is False
    assert results['hold_amount'] == 10
    assert results['exec_doc'] == 9
