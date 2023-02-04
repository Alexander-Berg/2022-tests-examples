import pytest

from datetime import date
import random

from staff.departments.controllers.intern_transfer_detector import InternTransferDetector
from staff.departments.controllers.actions.person_action import PersonAction
from staff.departments.tests.factories import VacancyFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.femida.utils import FemidaUtils
from staff.lib.testing import StaffFactory

TEST_CASES = [
    (None, None, 1, False),
    ({'with_budget': True}, None, 1, False),
    ({'with_budget': False}, None, 1, False),
    ({'with_budget': False, 'vacancy_url': 'x'}, date.today(), 0, False),
    ({'with_budget': False, 'vacancy_url': 'xy'}, date.today(), 2, False),
    ({'with_budget': False, 'vacancy_url': 'x'}, date.today(), 1, True),
]


@pytest.mark.django_db
@pytest.mark.parametrize('department_section, date_completion_internship, headcount, result', TEST_CASES)
def test_is_intern_transfer_to_staff_dep_change(department_section, date_completion_internship, headcount, result):
    headcount_position_code = f'{random.randint(10000, 999000)}'
    HeadcountPositionFactory(code=headcount_position_code, headcount=headcount)
    _is_intern_transfer_to_staff(date_completion_internship, department_section, headcount_position_code, result)


@pytest.mark.django_db
@pytest.mark.parametrize('department_section, date_completion_internship, _, _2', TEST_CASES)
def test_is_intern_transfer_to_staff_no_headcount_position(department_section, date_completion_internship, _, _2):
    headcount_position_code = f'{random.randint(10000, 999000)}'
    _is_intern_transfer_to_staff(date_completion_internship, department_section, headcount_position_code, False)


def _is_intern_transfer_to_staff(date_completion_internship, department_section, headcount_position_code, result):
    ticket = f'key-{random.randint(10, 999)}'
    action = {'login': f'login{random.randint(10, 999)}'}
    femida_utils = FemidaUtils()
    if department_section:
        action['department'] = department_section

        if department_section.get('vacancy_url'):
            def _return_if(p1):
                assert p1 == department_section.get('vacancy_url')
                return ticket

            femida_utils.get_vacancy_issue_key_from_url = _return_if
    StaffFactory(login=action['login'], date_completion_internship=date_completion_internship)
    VacancyFactory(ticket=ticket, headcount_position_code=headcount_position_code)
    target = InternTransferDetector(femida_utils)
    assert target.is_intern_transfer_to_staff(action) == result
    assert target.is_intern_transfer_to_staff(PersonAction.from_dict(action)) == result
