import pytest
from mock import patch

import json
from itertools import groupby
from typing import Any, Dict

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse
from waffle.models import Switch

from staff.budget_position.models import BudgetPositionAssignment, BudgetPositionAssignmentStatus, ReplacementType
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import Department
from staff.departments.models.department import DepartmentRoles
from staff.departments.tests.factories import RelevanceDateFactory, VacancyFactory
from staff.departments.tree_lib import TreeBuilder
from staff.lib.models.mptt import filter_by_heirarchy
from staff.lib.testing import BudgetPositionFactory, DepartmentStaffFactory, GeographyDepartmentFactory

from staff.headcounts.forms import QUANTITY_FILTER
from staff.headcounts.models import CreditManagementApplication, CreditManagementApplicationRow
from staff.headcounts.permissions import Permissions as HeadcountsPermissions
from staff.headcounts.budget_position_assignment_entity_info import BudgetPositionAssignmentEntityInfo
from staff.headcounts.budget_position_assignment_filter_context import BudgetPositionAssignmentFilterContext
from staff.headcounts.views import budget_position_assignments


@pytest.fixture
def relevance_date():
    RelevanceDateFactory(
        model_name=BudgetPositionAssignment.__name__,
        updated_entities=0,
        failed_entities=0,
        skipped_entities=0,
    )


def person_with_perm(person, codename):
    person.user.user_permissions.add(Permission.objects.get(codename=codename))
    return person


@pytest.mark.django_db()
def test_department_ceilings_wo_position(company, rf, relevance_date):
    yandex_department = company.yandex
    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')
    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    result = json.loads(response.content)

    assert len(result['departments']) == 0
    assert result['positions_count'] == 0
    assert result['hc_sum'] == 0


@pytest.mark.django_db()
def test_department_ceilings_marks_disabled_positions(company, rf, relevance_date):
    first_position = BudgetPositionFactory()
    second_position = BudgetPositionFactory()
    third_position = BudgetPositionFactory()

    application = CreditManagementApplication.objects.create(
        author=company.persons['yandex-chief'],
        comment='test',
        startrek_headcount_key='TEST-1',
    )
    CreditManagementApplicationRow.objects.create(
        credit_budget_position=first_position,
        repayment_budget_position=third_position,
        application=application,
    )

    yandex_department = company.yandex
    yandex_person = company.persons['yandex-person']
    BudgetPositionAssignmentFactory(
        department=yandex_department,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=yandex_person,
        budget_position=first_position,
        main_assignment=True,
        replacement_type=ReplacementType.BUSY.value,
    )

    nested_department = company.dep111
    dep111_person = company.persons['dep111-person']
    BudgetPositionAssignmentFactory(
        department=nested_department,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=dep111_person,
        budget_position=second_position,
        main_assignment=False,
        replacement_type=ReplacementType.HAS_REPLACEMENT.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    result = json.loads(response.content)
    assert result['departments'][0]['info']['positions'][0]['is_disabled']
    assert not result['departments'][1]['info']['positions'][0]['is_disabled']

    assert (
        result['departments'][0]['info']['positions'][0]['credit_application']['author']['login']
        == company.persons['yandex-chief'].login
    )
    assert result['departments'][0]['info']['positions'][0]['credit_application']['author']['name'] == (
        f"{company.persons['yandex-chief'].i_first_name} "
        f"{company.persons['yandex-chief'].i_last_name}"
    )
    assert result['departments'][0]['info']['positions'][0]['credit_application']['ticket'] == 'TEST-1'


@pytest.mark.django_db()
def test_department_ceilings(company, rf, relevance_date):
    yandex_department = company.yandex
    yandex_person = company.persons['yandex-person']
    hp1 = BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=yandex_person,
        budget_position=BudgetPositionFactory(headcount=1),
        main_assignment=True,
        replacement_type=ReplacementType.BUSY.value,
    )

    nested_department = company.dep111
    dep111_person = company.persons['dep111-person']
    hp2 = BudgetPositionAssignmentFactory(
        department=nested_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=dep111_person,
        budget_position=BudgetPositionFactory(headcount=0),
        main_assignment=False,
        replacement_type=ReplacementType.HAS_REPLACEMENT.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    result = json.loads(response.content)

    assert len(result['departments']) == 2
    assert result['positions_count'] == 2
    assert result['hc_sum'] == 1
    assert len(result['departments'][0]['info']['positions']) == 1
    assert result['departments'][0]['info']['positions'][0]['code'] == hp1.budget_position.code
    assert result['departments'][0]['info']['positions'][0]['current_person']
    assert result['departments'][0]['info']['positions'][0]['main_assignment']
    assert result['departments'][0]['info']['positions'][0]['replacement_type'] == ReplacementType.BUSY.value
    assert result['departments'][0]['info']['positions'][0]['geography'] == _get_geography_data(hp1.geography)
    assert result['departments'][0]['info']['positions'][0]['reward_id'] == hp1.reward_id
    assert result['departments'][0]['info']['positions'][0]['reward_category'] == hp1.reward.category

    assert len(result['departments'][1]['info']['positions']) == 1
    assert result['departments'][1]['info']['positions'][0]['code'] == hp2.budget_position.code
    assert result['departments'][1]['info']['positions'][0]['current_person']
    assert not result['departments'][1]['info']['positions'][0]['main_assignment']
    assert result['departments'][1]['info']['positions'][0]['replacement_type'] == ReplacementType.HAS_REPLACEMENT.value
    assert result['departments'][1]['info']['positions'][0]['geography'] == _get_geography_data(hp2.geography)
    assert result['departments'][1]['info']['positions'][0]['reward_id'] == hp2.reward_id
    assert result['departments'][1]['info']['positions'][0]['reward_category'] == hp2.reward.category


@pytest.mark.django_db()
def test_department_ceilings_filter_by_status(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        creates_new_position=False,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        creates_new_position=False,
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {
            'position_statuses': [
                BudgetPositionAssignmentStatus.OCCUPIED.value,
                BudgetPositionAssignmentStatus.OFFER.value,
            ],
        },
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result, result
    assert response.status_code == 200

    assert len(result['departments']) == 2
    assert result['positions_count'] == 2


@pytest.mark.django_db()
def test_department_ceilings_filter_by_replacement_type(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        replacement_type=ReplacementType.HAS_REPLACEMENT.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        replacement_type=ReplacementType.BUSY.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'replacement_type': [ReplacementType.HAS_REPLACEMENT.value]},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result, result
    assert response.status_code == 200

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1


@pytest.mark.django_db()
def test_department_ceilings_filter_by_code(company, rf, relevance_date):
    yandex_dep = company.yandex

    assignment = BudgetPositionAssignmentFactory(department=yandex_dep)
    BudgetPositionAssignmentFactory(department=yandex_dep)

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'code': assignment.budget_position.code},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result
    assert response.status_code == 200

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1
    assert result['departments'][0]['info']['positions'][0]['code'] == assignment.budget_position.code
    assert result['departments'][0]['info']['positions'][0]['geography'] == _get_geography_data(assignment.geography)


@pytest.mark.django_db()
def test_department_ceilings_filter_by_category_is_new(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        creates_new_position=False,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'category_is_new': True},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result
    assert response.status_code == 200

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1


@pytest.mark.django_db()
def test_department_ceilings_filter_by_category_main_assignment(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        main_assignment=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        main_assignment=False,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'main_assignment': True},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result
    assert response.status_code == 200

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1


@pytest.mark.django_db()
def test_department_ceilings_filter_by_category_is_replacement(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        creates_new_position=False,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'category_is_new': False},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result
    assert response.status_code == 200

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1


@pytest.mark.django_db()
def test_department_ceilings_filter_by_quantity(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        budget_position=BudgetPositionFactory(headcount=0),
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        budget_position=BudgetPositionFactory(headcount=1),
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'quantity': QUANTITY_FILTER.ZERO},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result
    assert 200 == response.status_code

    assert len(result['departments']) == 1
    assert result['positions_count'] == 1


@pytest.mark.django_db()
def test_department_ceilings_filter_works_together(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        budget_position=BudgetPositionFactory(headcount=0),
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=nested_dep,
        budget_position=BudgetPositionFactory(headcount=1),
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'quantity': QUANTITY_FILTER.ZERO, 'position_statuses': BudgetPositionAssignmentStatus.OFFER.value},
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_dep.url)

    result = json.loads(response.content)

    assert 'errors' not in result, result
    assert response.status_code == 200

    assert len(result['departments']) == 0


@pytest.mark.django_db()
def test_department_ceilings_filter_by_replaced_or_current_person(company, rf, relevance_date):
    yandex_dep = company.yandex
    nested_dep = company.dep111

    some_person = company.persons['dep1-person']
    previous_assignment = BudgetPositionAssignmentFactory(
        department=yandex_dep,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=some_person,
    )
    BudgetPositionAssignmentFactory(
        department=yandex_dep,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        previous_assignment=previous_assignment,
    )
    BudgetPositionAssignmentFactory(department=nested_dep, status=BudgetPositionAssignmentStatus.OFFER.value)

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'replaced_person': some_person.login},
    )
    request.user = viewer_person.user
    response = budget_position_assignments(request, yandex_dep.url)
    result = json.loads(response.content)
    positions = result['departments'][0]['info']['positions']

    assert len(positions) == 1
    assert positions[0]['previous_person']['login'] == some_person.login

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_dep.url}),
        {'current_person': some_person.login},
    )
    request.user = viewer_person.user
    response = budget_position_assignments(request, yandex_dep.url)
    result = json.loads(response.content)
    positions = result['departments'][0]['info']['positions']

    assert len(positions) == 1
    assert positions[0]['current_person']['login'] == some_person.login


@pytest.mark.django_db()
def test_department_ceilings_has_vacancies(company, rf, relevance_date):
    yandex_department = company.yandex
    yandex_person = company.persons['yandex-person']
    first_position = BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=yandex_person,
    )
    VacancyFactory(headcount_position_code=first_position.budget_position.code, is_active=True)

    nested_department = company.dep111
    dep111_person = company.persons['dep111-person']
    second_position = BudgetPositionAssignmentFactory(
        department=nested_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=dep111_person,
    )
    VacancyFactory(headcount_position_code=second_position.budget_position.code, is_active=True)

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    result = json.loads(response.content)

    assert len(result['departments']) == 2
    assert result['positions_count'] == 2

    assert len(result['departments'][0]['info']['positions']) == 1
    position = result['departments'][0]['info']['positions'][0]
    assert position['code'] == first_position.budget_position.code
    assert position['current_person']
    assert position['vacancy']['candidate_id'] is not None
    assert position['vacancy']['candidate_first_name'] is not None
    assert position['vacancy']['candidate_last_name'] is not None
    assert position['vacancy']['candidate_login'] is not None
    assert position['vacancy']['id'] is not None
    assert position['vacancy']['name']
    assert position['vacancy']['ticket']

    assert len(result['departments'][1]['info']['positions']) == 1
    position = result['departments'][1]['info']['positions'][0]
    assert position['code'] == second_position.budget_position.code
    assert position['current_person']
    assert position['vacancy']['candidate_id']
    assert position['vacancy']['candidate_first_name'] is not None
    assert position['vacancy']['candidate_last_name'] is not None
    assert position['vacancy']['candidate_login'] is not None
    assert position['vacancy']['id']
    assert position['vacancy']['name']
    assert position['vacancy']['ticket']


def _get_positions_by_search_text(text, company, rf):
    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': company.yandex.url}),
        {'search_text': text},
    )
    request.user = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts').user
    response = budget_position_assignments(request, company.yandex.url)
    result = json.loads(response.content)
    positions = sorted(result['departments'][0]['info']['positions'], key=lambda x: x['code'])
    return positions


@pytest.mark.django_db()
def test_department_ceilings_filter_by_vacancy_name(company, rf, relevance_date):
    position1 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )
    position2 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )
    position3 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )

    VacancyFactory(headcount_position_code=position1.budget_position.code, name='Разработчик', is_active=True)
    VacancyFactory(
        headcount_position_code=position2.budget_position.code,
        name='Неактивный одинокий разработчик',
        is_active=False,
    )
    VacancyFactory(
        headcount_position_code=position3.budget_position.code,
        name='Неактивный разработчик',
        is_active=False,
    )
    VacancyFactory(headcount_position_code=position3.budget_position.code, name='Активный разработчик', is_active=True)

    positions = _get_positions_by_search_text('разраб', company, rf)

    assert len(positions) == 2
    assert positions[0]['vacancy']['name'] == 'Разработчик'
    assert positions[1]['vacancy']['name'] == 'Активный разработчик'


@pytest.mark.django_db()
def test_department_ceilings_filter_by_ticket(company, rf, relevance_date):
    position1 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,)
    position2 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )

    VacancyFactory(headcount_position_code=position1.budget_position.code, ticket='JOB-123', is_active=True)
    VacancyFactory(headcount_position_code=position2.budget_position.code, ticket='JOB-12', is_active=True)

    positions = _get_positions_by_search_text('job-12', company, rf)

    assert len(positions) == 2
    assert positions[0]['vacancy']['ticket'] == 'JOB-123'
    assert positions[1]['vacancy']['ticket'] == 'JOB-12'


@pytest.mark.django_db()
def test_department_ceilings_filter_by_offer_details(company, rf, relevance_date):
    position1 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )
    position2 = BudgetPositionAssignmentFactory(
        department=company.yandex,
        status=BudgetPositionAssignmentStatus.OFFER.value,
    )
    VacancyFactory(
        headcount_position_code=position1.budget_position.code,
        candidate_first_name='name',
        candidate_last_name='surname',
        is_active=True,
    )
    VacancyFactory(
        headcount_position_code=position2.budget_position.code,
        candidate_first_name='name',
        candidate_last_name='last',
        is_active=True,
    )

    positions = _get_positions_by_search_text('name surname', company, rf)

    assert len(positions) == 1
    assert positions[0]['vacancy']['candidate_first_name'] == 'name'
    assert positions[0]['vacancy']['candidate_last_name'] == 'surname'


@pytest.mark.django_db()
def test_reserve_position_with_positive_headcount_should_not_appear_in_positions_list(company, rf, relevance_date):
    yandex_department = company.yandex

    BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)

    assert response.status_code == 200

    content = json.loads(response.content)
    assert content['positions_count'] == 0


@pytest.mark.django_db()
def test_reserve_position_with_zero_headcount_should_not_appear_in_positions_list(company, rf, relevance_date):
    yandex_department = company.yandex

    BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
        budget_position=BudgetPositionFactory(headcount=0),
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)

    assert response.status_code == 200

    content = json.loads(response.content)
    assert content['positions_count'] == 0


@pytest.mark.parametrize('has_special_role, status, result', (
    (True, BudgetPositionAssignmentStatus.OCCUPIED.value, True),
    (False, BudgetPositionAssignmentStatus.OCCUPIED.value, False),
    (True, BudgetPositionAssignmentStatus.VACANCY_OPEN.value, False),
    (True, BudgetPositionAssignmentStatus.OCCUPIED.value, True),
))
@pytest.mark.django_db()
def test_maternity_leave_attribute(company, rf, relevance_date, has_special_role, status, result):
    Switch.objects.get_or_create(name='enable_maternity_leave', active=True)
    yandex_department = company.yandex
    yandex_person = company.persons['yandex-person']
    BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=status,
        person=yandex_person,
    )

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')
    if has_special_role:
        DepartmentStaffFactory(
            department=yandex_department,
            staff=viewer_person,
            role_id=DepartmentRoles.HR_ANALYST.value,
        )

    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)

    assert response.status_code == 200

    content = json.loads(response.content)
    assert content['positions_count'] == 1
    assert content['departments'][0]['info']['positions'][0]['is_maternity_leave_available'] is result


def positions_department_filter_qs(filter_context, url):
    yandex_dep = filter_context.departments_qs().get(url=url)

    return filter_by_heirarchy(
        Department.objects.all(),
        mptt_objects=[yandex_dep],
        by_children=True,
        include_self=True,
    )


def get_positions(departments, positions_filter_context):
    departments_ids = [dep['id'] for dep in departments]
    positions_qs = list(
        positions_filter_context
        .positions_qs()
        .filter(department_id__in=departments_ids)
        .order_by('department')
    )

    return {
        dep_id: list(positions)
        for dep_id, positions in groupby(positions_qs, lambda p: p['department_id'])
    }


@pytest.mark.django_db()
def test_department_filler_simple(company, rf):
    yandex_department = company.yandex
    nested_department = company.dep111

    BudgetPositionAssignmentFactory(
        department=yandex_department,
        budget_position=BudgetPositionFactory(headcount=1),
        creates_new_position=True,
    )

    BudgetPositionAssignmentFactory(
        department=nested_department,
        budget_position=BudgetPositionFactory(headcount=1),
        creates_new_position=True,
    )

    observer = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')
    permissions = HeadcountsPermissions(observer)
    filter_context = BudgetPositionAssignmentFilterContext(observer_permissions=permissions)
    qs = (
        positions_department_filter_qs(filter_context, yandex_department.url)
        .values(*BudgetPositionAssignmentFilterContext.dep_fields)
        .order_by('tree_id', 'level', 'lft')
    )
    departments = list(qs)
    positions = get_positions(departments, filter_context)

    result = TreeBuilder(BudgetPositionAssignmentEntityInfo(filter_context)).get_for_info_list(departments, positions)

    assert len(result[0]['info']['positions']) == 1
    assert len(result[5]['info']['positions']) == 1


@pytest.mark.django_db()
def test_department_ceilings_have_next_and_prev_headcount(company, rf, relevance_date):

    def get_positions(person):
        viewer_person = person_with_perm(company.persons[person], codename='can_view_headcounts')
        request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': company.dep111.url}))
        request.user = viewer_person.user
        response = budget_position_assignments(request, company.dep111.url)
        result = json.loads(response.content)
        return result['departments'][0]['info']['positions']

    first_assignment = BudgetPositionAssignmentFactory(department=company.dep111)

    headcounts = get_positions('dep111-chief')

    assert len(headcounts) == 1
    assert headcounts[0]['prev_position'] is None
    assert headcounts[0]['next_position'] is None

    second_assignment = BudgetPositionAssignmentFactory(
        department=company.dep111,
        budget_position=first_assignment.budget_position,
        previous_assignment=first_assignment
    )
    BudgetPositionAssignmentFactory(
        department=company.dep11,
        budget_position=first_assignment.budget_position,
        previous_assignment=second_assignment,
    )

    headcounts = sorted(get_positions('dep111-chief'), key=lambda x: x['category_is_new'])

    assert len(headcounts) == 2
    assert headcounts[0]['prev_position'] is None
    assert headcounts[1]['next_position'] is None

    headcounts = sorted(get_positions('dep11-chief'), key=lambda x: x['category_is_new'])

    assert len(headcounts) == 2
    assert headcounts[0]['prev_position'] is None


@pytest.mark.django_db()
def test_adds_department_filter_param_in_response(company, rf, relevance_date):
    yandex_department = company.yandex
    dep = company.dep1

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}),
        data={
            'department': dep.url,
        }
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    assert response.status_code == 200
    content = json.loads(response.content)

    assert 'department' in content
    assert content['department'] == {
        'id': dep.id,
        'url': dep.url,
        'name': dep.name
    }


@pytest.mark.django_db()
def test_adds_value_stream_filter_param_in_response(company, rf, relevance_date):
    yandex_department = company.yandex
    vs = company.vs_11

    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}),
        data={
            'value_stream': vs.url,
        }
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    assert response.status_code == 200
    content = json.loads(response.content)

    assert 'value_stream' in content
    assert content['value_stream'] == {
        'id': vs.id,
        'url': vs.url,
        'name': vs.name
    }


@pytest.mark.django_db()
def test_adds_current_person_filter_param_in_response(company, rf, relevance_date):
    yandex_department = company.yandex

    current_person = company.persons['dep111-person']
    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}),
        data={
            'current_person': current_person.login,
        }
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    assert response.status_code == 200
    content = json.loads(response.content)

    assert 'current_person' in content
    assert content['current_person'] == {
        'login': current_person.login,
        'name': current_person.get_full_name(),
    }


@pytest.mark.django_db()
def test_adds_replaced_person_filter_param_in_response(company, rf, relevance_date):
    yandex_department = company.yandex

    replaced_person = company.persons['dep1-person']
    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')

    request = rf.get(
        reverse('departments-api:ceilings-url', kwargs={'url': yandex_department.url}),
        data={
            'replaced_person': replaced_person.login,
        }
    )
    request.user = viewer_person.user

    response = budget_position_assignments(request, yandex_department.url)
    assert response.status_code == 200
    content = json.loads(response.content)

    assert 'replaced_person' in content
    assert content['replaced_person'] == {
        'login': replaced_person.login,
        'name': replaced_person.get_full_name(),
    }


@pytest.mark.django_db()
def test_vs_ceilings_skip_provided_offset(company, rf, relevance_date):
    viewer = company.persons['vs-chief']
    # TODO: use proper VS-HEAD role
    viewer.user.is_superuser = True
    viewer.save()

    viewer_person = person_with_perm(viewer, codename='can_view_headcounts')

    department = company.vs_1

    vs_1_positions = [
        BudgetPositionAssignmentFactory(value_stream=company.vs_1),
        BudgetPositionAssignmentFactory(value_stream=company.vs_1),
        BudgetPositionAssignmentFactory(value_stream=company.vs_1),
    ]

    vs_11_positions = [
        BudgetPositionAssignmentFactory(value_stream=company.vs_11),
        BudgetPositionAssignmentFactory(value_stream=company.vs_11),
        BudgetPositionAssignmentFactory(value_stream=company.vs_11),
    ]

    vs_112_positions = [
        BudgetPositionAssignmentFactory(value_stream=company.vs_112),
        BudgetPositionAssignmentFactory(value_stream=company.vs_112),
        BudgetPositionAssignmentFactory(value_stream=company.vs_112),
    ]

    max_count = 4
    with patch('staff.departments.tree_lib.pager.Pager.MAX_ROWS_COUNT', max_count):
        request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': department.url}), {'skip': 0})
        request.user = viewer_person.user
        response = budget_position_assignments(request, department.url)
        assert response.status_code == 200

        result = json.loads(response.content)

        assert result['departments'][0]['id'] == company.vs_1.id
        assert result['departments'][1]['id'] == company.vs_11.id
        assert len(result['departments']) == 2

        headcounts_vs1 = result['departments'][0]['info']['positions']
        headcounts_vs11 = result['departments'][1]['info']['positions']
        assert len(headcounts_vs1) == len(vs_1_positions)
        assert len(headcounts_vs11) == max_count - len(headcounts_vs1)

        assert headcounts_vs1[0]['id'] == vs_1_positions[0].id
        assert headcounts_vs1[1]['id'] == vs_1_positions[1].id
        assert headcounts_vs1[2]['id'] == vs_1_positions[2].id
        assert headcounts_vs11[0]['id'] == vs_11_positions[0].id

        assert result['next_skip'] == max_count

        request = rf.get(reverse(
            'departments-api:ceilings-url',
            kwargs={'url': department.url}), {'skip': result['next_skip']}
        )
        request.user = viewer_person.user
        response = budget_position_assignments(request, department.url)
        assert response.status_code == 200

        result = json.loads(response.content)
        assert len(result['departments']) == 2

        headcounts_vs11 = result['departments'][0]['info']['positions']
        headcounts_vs112 = result['departments'][1]['info']['positions']
        assert len(headcounts_vs11) == len(vs_11_positions) - 1
        assert len(headcounts_vs112) == max_count - len(headcounts_vs11)
        assert headcounts_vs11[0]['id'] == vs_11_positions[1].id
        assert headcounts_vs11[1]['id'] == vs_11_positions[2].id
        assert headcounts_vs112[0]['id'] == vs_112_positions[0].id
        assert headcounts_vs112[1]['id'] == vs_112_positions[1].id

        skip_to = 7
        request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': department.url}), {'skip': skip_to})
        request.user = viewer_person.user
        response = budget_position_assignments(request, department.url)

        result = json.loads(response.content)

        assert result['departments'][0]['id'] == company.vs_112.id
        assert len(result['departments']) == 1

        headcounts_vs112 = result['departments'][0]['info']['positions']
        assert len(headcounts_vs112) == len(vs_1_positions) + len(vs_11_positions) + len(vs_112_positions) - skip_to

        assert headcounts_vs112[0]['id'] == vs_112_positions[1].id
        assert headcounts_vs112[1]['id'] == vs_112_positions[2].id

        assert 'next_skip' not in result


@pytest.mark.django_db()
def test_department_ceilings_department_permissions_no_url(company, rf, relevance_date):
    position1 = BudgetPositionAssignmentFactory(department=company.dep111)
    BudgetPositionAssignmentFactory(department=company.dep11)
    viewer_person = person_with_perm(company.persons['dep111-chief'], codename='can_view_headcounts')
    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': company.dep111.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request)

    assert response.status_code == 200
    result = json.loads(response.content)
    headcounts = result['departments'][0]['info']['positions']
    assert len(headcounts) == 1
    assert headcounts[0]['code'] == position1.budget_position.code


@pytest.mark.django_db()
def test_department_ceilings_department_permissions_wrong_url(company, rf, relevance_date):
    BudgetPositionAssignmentFactory(department=company.dep111)
    BudgetPositionAssignmentFactory(department=company.dep11)
    viewer_person = person_with_perm(company.persons['dep111-chief'], codename='can_view_headcounts')
    request = rf.get(reverse('departments-api:ceilings-url', kwargs={'url': company.dep111.url}))
    request.user = viewer_person.user

    response = budget_position_assignments(request, company.dep11.url)

    assert response.status_code == 403


@pytest.mark.django_db()
def test_ceilings_filter_by_geography(company, rf, relevance_date):
    yandex_department = company.yandex
    yandex_person = company.persons['yandex-person']
    root_geography_dep = GeographyDepartmentFactory()
    local_geography_dep = GeographyDepartmentFactory(parent=root_geography_dep)

    BudgetPositionAssignmentFactory(
        department=yandex_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=yandex_person,
        budget_position=BudgetPositionFactory(headcount=1),
        main_assignment=True,
        geography=local_geography_dep,
        replacement_type=ReplacementType.BUSY.value,
    )

    other_department = company.dep111
    dep111_person = company.persons['dep111-person']

    BudgetPositionAssignmentFactory(
        department=other_department,
        creates_new_position=True,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        person=dep111_person,
        budget_position=BudgetPositionFactory(headcount=0),
        main_assignment=False,
        geography=root_geography_dep,
        replacement_type=ReplacementType.HAS_REPLACEMENT.value,
    )

    request_root = rf.get(reverse('departments-api:ceilings'), {'geography': root_geography_dep.url})
    request_local = rf.get(reverse('departments-api:ceilings'), {'geography': local_geography_dep.url})
    viewer_person = person_with_perm(company.persons['yandex-chief'], codename='can_view_headcounts')
    request_local.user = viewer_person.user
    request_root.user = viewer_person.user

    result_root = json.loads(budget_position_assignments(request_root).content)
    result_local = json.loads(budget_position_assignments(request_local).content)

    assert len(result_root['departments']) == 2
    assert result_root['positions_count'] == 2
    assert len(result_local['departments']) == 1
    assert result_local['positions_count'] == 1


def _get_geography_data(geography: Department) -> Dict[str, Any]:
    return {
        'id': geography.id,
        'url': geography.url,
        'name': geography.name,
    }
