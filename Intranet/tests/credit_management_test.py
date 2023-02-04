import pytest
from mock import Mock, patch

import json
from typing import Any, Dict

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import DepartmentRoles, Geography
from staff.lib.testing import BudgetPositionFactory, DepartmentStaffFactory, GeographyFactory
from staff.oebs.constants import PERSON_POSITION_STATUS

from staff.headcounts.forms import CreditManagementApplicationRowForm
from staff.headcounts.headcounts_credit_management import CreditRepayment
from staff.headcounts.headcounts_credit_management.use_cases import (
    RepositoryInterface,
    CancelUseCase,
)
from staff.headcounts.models import CreditManagementApplication
from staff.headcounts.tests.factories import (
    CreditManagementApplicationFactory,
    CreditManagementApplicationRowFactory,
    HeadcountPositionFactory,
)
from staff.headcounts.views.credit import application, application_list, credit_management_application


@pytest.mark.django_db
def test_correct_credit_repayment_form():
    # given
    credit_budget_position = BudgetPositionFactory(headcount=-1)
    BudgetPositionAssignmentFactory(
        budget_position=credit_budget_position,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
    )
    repayment_budget_position = BudgetPositionFactory(headcount=1)
    BudgetPositionAssignmentFactory(
        budget_position=repayment_budget_position,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )
    form = CreditManagementApplicationRowForm(data={
        'credit': credit_budget_position.code,
        'repayment': repayment_budget_position.code,
    })

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_credit_repayment_form_forbids_repayment_bp_with_wrong_status():
    # given
    budget_position = BudgetPositionFactory(headcount=1)
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    form = CreditManagementApplicationRowForm(data={'repayment': budget_position.code})

    # when
    result = form.is_valid()

    # then
    assert not result
    assert 'repayment' in form.errors['errors']
    assert form.errors['errors']['repayment'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
def test_credit_repayment_form_forbids_credit_bp_with_wrong_status():
    # given
    budget_position = BudgetPositionFactory(headcount=1)
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    form = CreditManagementApplicationRowForm(data={'credit': budget_position.code})

    # when
    result = form.is_valid()

    # then
    assert not result
    assert 'credit' in form.errors['errors']
    assert form.errors['errors']['credit'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
def test_credit_repayment_form_forbids_repayment_bp_with_crossing():
    # given
    budget_position = BudgetPositionFactory(headcount=1)
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
    )
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.MATERNITY.value,
    )
    form = CreditManagementApplicationRowForm(data={'repayment': budget_position.code})

    # when
    result = form.is_valid()

    # then
    assert not result
    assert 'repayment' in form.errors['errors']
    assert form.errors['errors']['repayment'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
def test_credit_repayment_form_forbids_credit_bp_with_crossing():
    # given
    budget_position = BudgetPositionFactory(headcount=1)
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
    )
    BudgetPositionAssignmentFactory(
        budget_position=budget_position,
        status=BudgetPositionAssignmentStatus.MATERNITY.value,
    )
    form = CreditManagementApplicationRowForm(data={'credit': budget_position.code})

    # when
    result = form.is_valid()

    # then
    assert not result
    assert 'credit' in form.errors['errors']
    assert form.errors['errors']['credit'][0]['code'] == 'invalid_choice'


@pytest.mark.django_db
@pytest.mark.parametrize('role', (DepartmentRoles.HR_ANALYST.value, DepartmentRoles.CHIEF.value))
def test_credit_management_view_gives_structure_on_get(rf, company, role):
    chief = company.persons['yandex-chief']
    DepartmentStaffFactory(
        staff=chief,
        role_id=role,
        department=company.yandex,
    )
    request = rf.get(reverse('headcounts-api:credit_management'))
    request.user = chief.user

    response = credit_management_application(request)
    assert response.status_code == 200


@pytest.mark.django_db
@pytest.mark.parametrize('role', (DepartmentRoles.HR_ANALYST.value, DepartmentRoles.CHIEF.value))
def test_credit_management_view_accepts_valid_form_on_post(post_rf, company, role):
    # given
    with patch('staff.headcounts.headcounts_credit_management.gateways.startrek.Startrek._ancestors_chain') as chain:
        chain.return_value = ''
        chief = company.persons['yandex-chief']
        if role == DepartmentRoles.CHIEF.value:
            chief.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
        DepartmentStaffFactory(staff=chief, role_id=role, department=company.yandex)
        credit_bp = BudgetPositionFactory(headcount=-1)
        BudgetPositionAssignmentFactory(
            budget_position=credit_bp,
            status=BudgetPositionAssignmentStatus.RESERVE.value,
            department=company.dep1,
        )
        repayment_bp = BudgetPositionFactory(headcount=1)
        BudgetPositionAssignmentFactory(
            budget_position=repayment_bp,
            status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
            department=company.dep2,
        )

        form = {
            'comment': 'some',
            'budget_positions': [{
                'credit': credit_bp.code,
                'repayment': repayment_bp.code,
            }],
        }

        request = post_rf('headcounts-api:credit_management', form, chief.user)
        current_count = CreditManagementApplication.objects.count()

        # when
        response = credit_management_application(request)

        # then
        assert response.status_code == 200
        assert CreditManagementApplication.objects.count() == current_count + 1
        application = CreditManagementApplication.objects.order_by('id').last()

        assert json.loads(response.content) == {'application_id': application.id}


@pytest.mark.django_db
def test_credit_management_list_view_shows_applicatoins_for_author(get_rf, company_with_module_scope):
    company = company_with_module_scope
    person = company.persons['yandex-person']
    request = get_rf('headcounts-api:credit_management_list', params={'_limit': 1}, user=person.user)

    application1 = CreditManagementApplicationFactory(author=person)
    application2 = CreditManagementApplicationFactory(author=person)
    CreditManagementApplicationFactory()

    response = application_list(request)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'page': 1,
        'limit': 1,
        'result': [{
            'id': application2.id,
            'author': {
                'login': application2.author.login,
                'name': f'{application2.author.i_first_name} {application2.author.i_last_name}',
            },
            'created_at': application2.created_at.isoformat()[:19],
            'comment': application2.comment,
            'ticket': application2.startrek_headcount_key,
            'is_active': application2.is_active,
        }],
        'total': 2,
        'pages': 2,
    }

    request = get_rf('headcounts-api:credit_management_list', params={'_limit': 1, '_page': 2}, user=person.user)

    response = application_list(request)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'page': 2,
        'limit': 1,
        'result': [{
            'id': application1.id,
            'author': {
                'login': application1.author.login,
                'name': f'{application1.author.i_first_name} {application1.author.i_last_name}',
            },
            'created_at': application1.created_at.isoformat()[:19],
            'comment': application1.comment,
            'ticket': application1.startrek_headcount_key,
            'is_active': application1.is_active,
        }],
        'total': 2,
        'pages': 2,
    }


@pytest.mark.django_db
def test_credit_management_application_view(get_rf, company_with_module_scope):
    company = company_with_module_scope
    person = company.persons['yandex-person']
    request = get_rf(
        'headcounts-api:credit_management_application',
        reverse_args=[12345],
        user=person.user,
    )

    response = application(request, 12345)
    assert response.status_code == 403

    application1 = CreditManagementApplicationFactory(author=person)
    row = CreditManagementApplicationRowFactory(application=application1)
    credit_position = HeadcountPositionFactory(
        department=company['dep11'],
        code=row.credit_budget_position.code,
        status=PERSON_POSITION_STATUS.RESERVE,
        valuestream=company.vs_root,
    )
    repayment_position = HeadcountPositionFactory(
        department=company['dep12'],
        code=row.repayment_budget_position.code,
        status=PERSON_POSITION_STATUS.VACANCY_PLAN,
        valuestream=company.vs_root,
    )
    GeographyFactory(oebs_code=credit_position.geo)
    GeographyFactory(oebs_code=repayment_position.geo)

    response = application(request, application1.id)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'items': [{
            'credit': {
                'id': credit_position.id,
                'code': credit_position.code,
                'name': credit_position.name,
                'department_id': credit_position.department.id,
                'department': {
                    'id': credit_position.department.id,
                    'name': credit_position.department.name,
                    'url': credit_position.department.url,
                },
                'valuestream_id': credit_position.valuestream.id,
                'value_stream': {
                    'id': credit_position.valuestream.id,
                    'name': credit_position.valuestream.name,
                    'url': credit_position.valuestream.url,
                },
                'reward_id': credit_position.reward_id,
                'reward_category': None,
                'geography': _get_geography_data(credit_position.geo),
                'headcount': credit_position.headcount,
                'current_login': None,
                'status': credit_position.status,
                'is_crossing': False,
                'main_assignment': True,
                'replacement_type': credit_position.replacement_type,
                'previous_login': None,
                'index': credit_position.index,
                'prev_index': credit_position.prev_index,
                'next_index': credit_position.next_index,
                'current_person': None,
                'previous_person': None,
                'users': [{'status': credit_position.status, 'current_user': None, 'previous_user': None}],
                'is_disabled': True,
                'is_maternity_leave_available': False,
                'credit_application': {
                    'author': {
                        'login': application1.author.login,
                        'name': f'{application1.author.i_first_name} {application1.author.i_last_name}',
                    },
                    'ticket': application1.startrek_headcount_key,
                },
                'category': 'NEW',
                'prev_position': None,
                'next_position': None,
                'link_new': False,
                'link_replacement': False,
                'link_changehc': False,
            },
            'repayment': {
                'id': repayment_position.id,
                'code': repayment_position.code,
                'name': repayment_position.name,
                'department_id': repayment_position.department.id,
                'department': {
                    'id': repayment_position.department.id,
                    'name': repayment_position.department.name,
                    'url': repayment_position.department.url,
                },
                'valuestream_id': repayment_position.valuestream.id,
                'value_stream': {
                    'id': repayment_position.valuestream.id,
                    'name': repayment_position.valuestream.name,
                    'url': repayment_position.valuestream.url,
                },
                'reward_id': repayment_position.reward_id,
                'reward_category': None,
                'geography': _get_geography_data(repayment_position.geo),
                'headcount': repayment_position.headcount,
                'current_login': None,
                'status': repayment_position.status,
                'is_crossing': False,
                'main_assignment': True,
                'replacement_type': repayment_position.replacement_type,
                'previous_login': None,
                'index': repayment_position.index,
                'prev_index': repayment_position.prev_index,
                'next_index': repayment_position.next_index,
                'current_person': None,
                'previous_person': None,
                'users': [{'status': repayment_position.status, 'current_user': None, 'previous_user': None}],
                'is_disabled': True,
                'is_maternity_leave_available': False,
                'credit_application': {
                    'author': {
                        'login': application1.author.login,
                        'name': f'{application1.author.i_first_name} {application1.author.i_last_name}',
                    },
                    'ticket': application1.startrek_headcount_key,
                },
                'category': 'NEW',
                'prev_position': None,
                'next_position': None,
                'link_new': True,
                'link_replacement': False,
                'link_changehc': True,
            },
        }],
        'author': {
            'login': application1.author.login,
            'name': f'{application1.author.i_first_name} {application1.author.i_last_name}',
        },
        'comment': application1.comment,
        'ticket': application1.startrek_headcount_key,
        'is_active': application1.is_active,
        'departments_chains': {
            str(company['dep11'].id): [
                {
                    'id': company['yandex'].id,
                    'name': company['yandex'].i_name,
                    'url': company['yandex'].url,
                },
                {
                    'id': company['dep1'].id,
                    'name': company['dep1'].i_name,
                    'url': company['dep1'].url,
                },
                {
                    'id': company['dep11'].id,
                    'name': company['dep11'].i_name,
                    'url': company['dep11'].url,
                },
            ],
            str(company['dep12'].id): [
                {
                    'id': company['yandex'].id,
                    'name': company['yandex'].i_name,
                    'url': company['yandex'].url,
                },
                {
                    'id': company['dep1'].id,
                    'name': company['dep1'].i_name,
                    'url': company['dep1'].url,
                },
                {
                    'id': company['dep12'].id,
                    'name': company['dep12'].i_name,
                    'url': company['dep12'].url,
                },
            ],
        },
    }


@pytest.mark.django_db
def test_cancel_use_case(post_rf, company):
    # given
    credit_repayment = CreditRepayment(
        rows=[],
        comment='',
        author_login=company.persons['dep1-hr-analyst'].login,
        id=100500,
        ticket='THEADCOUNT-1',
        is_active=True,
        closed_at=None,
    )
    repository_mock = Mock(spec=RepositoryInterface)
    repository_mock.get_active_by_ticket = Mock(return_value=credit_repayment)
    cancel_use_case = CancelUseCase(repository_mock)

    # when
    cancel_use_case.cancel(credit_repayment.id)

    # then
    repository_mock.deactivate_application.assert_called_once_with(credit_repayment.id)


def _get_geography_data(code: str) -> Dict[str, Any]:
    geography = Geography.objects.get(oebs_code=code)
    return {
        'id': geography.department_instance.id,
        'url': geography.department_instance.url,
        'name': geography.department_instance.name,
    }


@pytest.mark.django_db
def test_credit_repayment_form_forbids_use_bp_in_two_active_applications():
    # given
    credit_budget_position = BudgetPositionFactory(headcount=-1)
    BudgetPositionAssignmentFactory(
        budget_position=credit_budget_position,
        status=BudgetPositionAssignmentStatus.RESERVE.value,
    )
    CreditManagementApplicationRowFactory(credit_budget_position=credit_budget_position)

    repayment = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value)

    form = CreditManagementApplicationRowForm(data={
        'credit': credit_budget_position.code,
        'repayment': repayment.budget_position.code,
    })

    # when
    result = form.is_valid()

    # then
    assert not result
    assert 'credit' in form.errors['errors']
    assert form.errors['errors']['credit'][0]['code'] == 'invalid_choice'
