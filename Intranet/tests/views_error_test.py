import pytest
from mock import patch, MagicMock

import json
import random
from waffle.models import Switch

from staff.departments.tests.factories import VacancyFactory, HRProductFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.lib.testing import StaffFactory
from staff.oebs.constants import PERSON_POSITION_STATUS

from staff.budget_position import views
from staff.budget_position.const import FemidaRequestType
from staff.budget_position.tests import utils
from staff.budget_position.workflow_service import UnexpectedResponseOEBSError


@pytest.mark.django_db
def test_error_on_absent_bp(femida_post_request, company):
    # given
    Switch.objects.get_or_create(name='enable_bp_state_checks_in_registry', active=True)
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': 0xDEADBEAF,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.OFFER.value,
    }

    # when
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    # then
    assert resp.status_code == 400
    errors = json.loads(resp.content)
    assert errors['errors']['messages_ru']
    assert errors['errors']['messages_en']


@pytest.mark.django_db
def test_error_on_empty_bp_and_not_vacancy(femida_post_request, company):
    # given
    Switch.objects.get_or_create(name='enable_bp_state_checks_in_registry', active=True)
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': None,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.OFFER.value,
    }

    # when
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    # then
    assert resp.status_code == 400
    errors = json.loads(resp.content)
    assert errors['errors']['messages_ru']
    assert errors['errors']['messages_en']


@pytest.mark.django_db
def test_error_on_salary_not_a_number(femida_post_request, company):
    bp = utils.BudgetPositionFactory()
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        department=company.yandex,
        code=bp.code,
        hr_product=HRProductFactory(),
    )
    vacancy = VacancyFactory()

    person = StaffFactory()
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', {
        'budget_position_id': bp.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.INTERNAL_OFFER.value,
        'username': person.login,
        'salary': 'RUB'
    }))

    assert resp.status_code == 400
    errors = json.loads(resp.content)
    assert errors['errors']['salary']


@pytest.mark.django_db
def test_error_on_non_vacancy_creation_on_occupied_bp(femida_post_request, company):
    # given
    Switch.objects.get_or_create(name='enable_bp_state_checks_in_registry', active=True)
    bp = utils.BudgetPositionFactory()
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.OCCUPIED,
        department=company.yandex,
        code=bp.code,
        hr_product=HRProductFactory(),
    )
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': bp.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.OFFER.value,
    }

    # when
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    # then
    assert resp.status_code == 400
    errors = json.loads(resp.content)
    assert errors['errors']['messages_ru']
    assert errors['errors']['messages_en']


@pytest.mark.django_db
def test_attach_bp_to_vacancy_simple(femida_post_request, company):
    bp = utils.BudgetPositionFactory()
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.VACANCY_PLAN,
        department=company.yandex,
        code=bp.code,
        hr_product=HRProductFactory(),
    )
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': bp.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.VACANCY.value,
    }

    error_message = f'err-{random.random()}'
    service_mock = MagicMock()
    service_mock.try_create_workflow_from_femida.side_effect = UnexpectedResponseOEBSError(error_message)

    with patch('staff.budget_position.views.worfklows_views.WorkflowRegistryService', return_value=service_mock):
        resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    assert resp.status_code == 400, resp.content
    errors = json.loads(resp.content)
    assert errors['errors']['messages_ru']
    assert errors['errors']['messages_en']
    assert error_message in errors['errors']['messages_ru'][0]
    assert error_message in errors['errors']['messages_en'][0]
