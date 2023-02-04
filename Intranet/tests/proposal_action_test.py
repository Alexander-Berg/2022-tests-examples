from collections import defaultdict
from datetime import date

import pytest

from staff.proposal.hr_deadlines import DeadlineMatcher, split, SplitData, Deadline

from staff.departments.controllers.actions.person_action import PersonAction
from staff.departments.models import DEADLINE_TYPE


DEADLINES = {
    DEADLINE_TYPE.FUTURE_CHANGE: Deadline(date(2019, 1, 15), date(2019, 1, 15), DEADLINE_TYPE.FUTURE_CHANGE),
    DEADLINE_TYPE.STRUCTURE_CHANGE: Deadline(date(2019, 1, 17), date(2019, 1, 17), DEADLINE_TYPE.FUTURE_CHANGE),
    DEADLINE_TYPE.SALARY_CHANGE: Deadline(date(2019, 1, 19), date(2019, 1, 19), DEADLINE_TYPE.SALARY_CHANGE),
    DEADLINE_TYPE.TABLE_TRANSFER: Deadline(date(2019, 1, 21), date(2019, 1, 21), DEADLINE_TYPE.TABLE_TRANSFER),
    DEADLINE_TYPE.DEAL_DATE: Deadline(date(2019, 1, 16), date(2019, 1, 16), DEADLINE_TYPE.DEAL_DATE),
}


@pytest.fixture
def split_data():
    login_to_paysys = defaultdict(lambda: 'fixed')
    login_to_paysys['pieceworker'] = 'XXYA_JOBPRICE'
    return SplitData(
        proposal_date=date(2019, 1, 2),
        desired_date=date(2019, 1, 6),
        office_to_city={
            1: 13,
            2: 13,
            3: 21,
        },
        login_to_cur_city=defaultdict(lambda: 13),  # everyone lives in city 13 by default
        login_to_paysys=login_to_paysys,
        deadlines=DEADLINES,
    )


@pytest.mark.parametrize('person_action, expecting_deadline', [
    [{'organization': 1}, DEADLINE_TYPE.FUTURE_CHANGE],
    [{'office': 1}, DEADLINE_TYPE.FUTURE_CHANGE],
    [{'office': 3}, DEADLINE_TYPE.FUTURE_CHANGE],
    [{'grade': PersonAction.GradeChange(new_grade='+2')}, DEADLINE_TYPE.SALARY_CHANGE],
    [{'login': 'pieceworker', 'grade': PersonAction.GradeChange(new_grade='+2')}, DEADLINE_TYPE.SALARY_CHANGE],
    [
        {'login': 'pieceworker', 'department': PersonAction.DepartmentChange(department='yandex')},
        DEADLINE_TYPE.DEAL_DATE,
    ],
    [{'salary': PersonAction.SalaryChange(old_wage_system='1', new_wage_system='2')}, DEADLINE_TYPE.SALARY_CHANGE],
    [{'salary': PersonAction.SalaryChange(new_salary='1', old_salary='2')}, DEADLINE_TYPE.SALARY_CHANGE],
    [{'salary': PersonAction.SalaryChange(new_currency='$', old_currency='rub')}, DEADLINE_TYPE.SALARY_CHANGE],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='0.5', old_salary='2', old_rate='1')},
        DEADLINE_TYPE.TABLE_TRANSFER,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='1', old_salary='2', old_rate='1')},
        DEADLINE_TYPE.SALARY_CHANGE,
    ],
    [{'department': PersonAction.DepartmentChange(department='yandex')}, DEADLINE_TYPE.STRUCTURE_CHANGE],
    [{'position': PersonAction.PositionChange(position_legal=1)}, DEADLINE_TYPE.STRUCTURE_CHANGE],
    [{'position': PersonAction.PositionChange(new_position='asd')}, DEADLINE_TYPE.STRUCTURE_CHANGE],
])
def test_split_actions_date_choosing(split_data, person_action, expecting_deadline):
    if 'login' not in person_action:
        person_action['login'] = 'any'
    action = PersonAction(**person_action)

    from staff.departments.controllers.intern_transfer_detector import InternTransferDetector
    intern_transfer_detector = InternTransferDetector()
    intern_transfer_detector.is_intern_transfer_to_staff = lambda _: False
    res = list(split(action, split_data, DeadlineMatcher(intern_transfer_detector)))

    assert len(res) == 1, res
    assert res[0].oebs_application_date == DEADLINES[expecting_deadline].month, (person_action, expecting_deadline)


def test_split_into_several(split_data):
    action = PersonAction(
        login='asd',
        organization=1,
        grade=PersonAction.GradeChange(new_grade='+2'),
    )

    res = list(split(action, split_data))

    assert len(res) == 2

    assert any(it.organization == action.organization and not it.has_grade_change() for it in res)
    assert any(it.organization is None and it.grade == action.grade for it in res)
