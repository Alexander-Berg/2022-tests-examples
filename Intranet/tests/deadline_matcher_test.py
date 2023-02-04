import pytest

from collections import defaultdict
from datetime import date
from itertools import permutations
from typing import List

from staff.proposal.hr_deadlines import DeadlineMatcher, SplitData, Deadline

from staff.departments.controllers.actions.person_action import PersonAction
from staff.departments.controllers.intern_transfer_detector import InternTransferDetector
from staff.departments.models.hr_deadline import DEADLINE_TYPE


DEADLINES = {
    DEADLINE_TYPE.FUTURE_CHANGE: Deadline(date(2019, 1, 15), date(2019, 1, 15), DEADLINE_TYPE.FUTURE_CHANGE),
    DEADLINE_TYPE.STRUCTURE_CHANGE: Deadline(date(2019, 1, 17), date(2019, 1, 17), DEADLINE_TYPE.STRUCTURE_CHANGE),
    DEADLINE_TYPE.SALARY_CHANGE: Deadline(date(2019, 1, 19), date(2019, 1, 19), DEADLINE_TYPE.SALARY_CHANGE),
    DEADLINE_TYPE.TABLE_TRANSFER: Deadline(date(2019, 1, 21), date(2019, 1, 21), DEADLINE_TYPE.TABLE_TRANSFER),
    DEADLINE_TYPE.DEAL_DATE: Deadline(date(2019, 1, 16), date(2019, 1, 16), DEADLINE_TYPE.DEAL_DATE),
}


def test_get_pieceworker_date():
    login = "paysys"
    login_map = defaultdict(lambda: "aaa")
    login_map[login] = "XXYA_JOBPRICE"
    split_data = SplitData(date(2019, 1, 2), date(2019, 2, 2), DEADLINES, {}, {}, login_map)
    target_date = date(2030, 4, 6)

    assert DeadlineMatcher().get_pieceworker_date("XXX", split_data, target_date) == target_date
    assert DeadlineMatcher().get_pieceworker_date(login, split_data, target_date) == split_data.deal_date


@pytest.mark.parametrize('action_data, default_target_date, expected_date', [
    [
        {'salary': PersonAction.SalaryChange(old_wage_system='1', new_wage_system='2')},
        None,
        DEADLINES[DEADLINE_TYPE.SALARY_CHANGE].month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', old_salary='2')},
        None,
        DEADLINES[DEADLINE_TYPE.SALARY_CHANGE].month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_currency='$', old_currency='rub')},
        None,
        DEADLINES[DEADLINE_TYPE.SALARY_CHANGE].month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', old_salary='1')},
        date(3030, 5, 4),
        date(3030, 5, 4),
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='0.5', old_salary='2', old_rate='1')},
        date(3030, 5, 4),
        date(3030, 5, 4),
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='1', old_salary='2', old_rate='1')},
        None,
        DEADLINES[DEADLINE_TYPE.SALARY_CHANGE].month,
    ],
])
def test_get_salary_date(action_data, default_target_date, expected_date):
    split_data = SplitData(date(2019, 1, 2), date(2019, 2, 2), DEADLINES, {}, {}, {})
    target = DeadlineMatcher()
    action_data['login'] = 'any'
    action = PersonAction(**action_data)

    def _get_if_matched(p1, p2, p3):
        if p1 != action.login or p2 != split_data or p3 != split_data.table_date:
            raise Exception("unexpected args")
        return default_target_date
    target.get_pieceworker_date = _get_if_matched

    assert target.get_salary_date(action, split_data) == expected_date


max_deadline_proposal_desired = ["desired", "deadline"]
max_deadline_desired_month = ["deadline", "desired_month"]


@pytest.mark.parametrize('action_data, is_intern_transfer, max_of', [
    [{'department': PersonAction.DepartmentChange(from_maternity_leave=True)}, False, max_deadline_proposal_desired],
    [{'organization': 1}, True, max_deadline_proposal_desired],
    [
        {'salary': PersonAction.SalaryChange(old_wage_system='1', new_wage_system='2')},
        False,
        max_deadline_desired_month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', old_salary='2')},
        False,
        max_deadline_desired_month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', old_salary='1')},
        False,
        max_deadline_desired_month,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='0.5', old_salary='2', old_rate='1')},
        False,
        max_deadline_proposal_desired,
    ],
    [
        {'salary': PersonAction.SalaryChange(new_salary='1', new_rate='1', old_salary='2', old_rate='1')},
        False,
        max_deadline_desired_month,
    ],
    [{'office': 1}, False, max_deadline_desired_month],
    [{'office': 3}, False, max_deadline_desired_month],
    [{'grade': PersonAction.GradeChange(new_grade='+2')}, False, max_deadline_desired_month],
    [{'login': 'pieceworker', 'grade': PersonAction.GradeChange(new_grade='+2')}, False, max_deadline_desired_month],
    [{'position': PersonAction.PositionChange(position_legal=1)}, False, max_deadline_desired_month],
    [{'position': PersonAction.PositionChange(new_position='asd')}, False, max_deadline_desired_month],
])
def test_correct_application_dates(action_data, is_intern_transfer: bool, max_of: List[str]):
    dates = [date(2020, 3, 4), date(2020, 4, 2), date(2020, 7, 6)]

    for permutation in permutations(dates):
        date_mapping = {
            "proposal": permutation[0],
            "desired": permutation[1],
            "deadline": permutation[2],
            "desired_month": date(permutation[1].year, permutation[1].month, 1)
        }
        _verify_correct_application_dates(
            action_data,
            is_intern_transfer,
            date_mapping["proposal"],
            date_mapping["desired"],
            date_mapping["deadline"],
            max(value for name, value in date_mapping.items() if name in max_of),
        )


def _verify_correct_application_dates(
    action_data,
    is_intern_transfer: bool,
    proposal_date: date,
    desired_date: date,
    deadline: date,
    expected_date: date,
):
    action_data['login'] = 'any'
    action = PersonAction(**action_data)
    change = {"test": "test"}
    split_data = SplitData(proposal_date, desired_date, DEADLINES, {}, {}, {})
    intern_transfer_detector = InternTransferDetector()

    def _get_if_matched(p1):
        if p1 != action:
            raise Exception("unexpected args")
        return is_intern_transfer
    intern_transfer_detector.is_intern_transfer_to_staff = _get_if_matched

    target = DeadlineMatcher(intern_transfer_detector)

    assert target.correct_application_dates(action, {deadline: change}, split_data) == {expected_date: change}
