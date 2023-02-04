from datetime import datetime

import pytest

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import GroupFactory

from staff.proposal.forms.person import PersonDepartmentForm


@pytest.fixture()
def yandex_services():
    now = datetime.now()
    service_root = GroupFactory(
        name='__services__', url='__services__',
        service_id=None, department=None,
        parent=None,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )
    service1 = GroupFactory(
        name='service1', url='svc_service1',
        service_id=1, department=None,
        parent=service_root,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )
    service2 = GroupFactory(
        name='service2', url='svc_service2',
        service_id=2, department=None,
        parent=service_root,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )
    return {
        service_root.url: service_root,
        service1.url: service1,
        service2.url: service2,
    }


VACANCY_URL_VALID_VARIANTS = [
    ('', ''),
    ('https://st.test.yandex-team.ru/TJOB-123', 'https://st.test.yandex-team.ru/TJOB-123'),
    ('TJOB-123', 'https://st.test.yandex-team.ru/TJOB-123'),
    ('https://femida.test.yandex-team.ru/vacancies/abcdefgh', 'https://femida.test.yandex-team.ru/vacancies/abcdefgh'),
]

VACANCY_URL_INVALID_VARIANTS = [
    'NOTJOB-321',
    'https://st.test.yandex-team.ru/ANOTHER-123',
    'randomvalue',
]


@pytest.mark.django_db
@pytest.mark.parametrize('url_data,url_cleaned_data', VACANCY_URL_VALID_VARIANTS)
def test_valid_vacancy_url_validation(url_data, url_cleaned_data, company, yandex_services):
    data = {
        'with_budget': True,
        'vacancy_url': url_data,
        'from_maternity_leave': False,
        'department': company.dep1.url,
        'fake_department': '',
        'service_groups': list(yandex_services.keys()),
        'changing_duties': False,
    }
    form = PersonDepartmentForm(data=data)

    assert form.is_valid()
    assert form.cleaned_data['vacancy_url'] == url_cleaned_data


@pytest.mark.django_db
@pytest.mark.parametrize('url_data', VACANCY_URL_INVALID_VARIANTS)
def test_invalid_vacancy_url_validation(url_data, company, yandex_services):
    data = {
        'with_budget': True,
        'vacancy_url': url_data,
        'from_maternity_leave': False,
        'department': company.dep1.url,
        'fake_department': '',
        'service_groups': list(yandex_services.keys()),
        'changing_duties': False,
    }
    form = PersonDepartmentForm(data=data)

    assert not form.is_valid()


@pytest.mark.django_db
@pytest.mark.parametrize('empty_field', ['department', 'fake_department'])
def test_only_one_department_field_can_be_filled(company, yandex_services, empty_field):
    data = {
        'with_budget': True,
        'department': company.dep1.url,
        'fake_department': 'asd',
        'service_groups': list(yandex_services.keys()),
        'changing_duties': False,
    }
    data.pop(empty_field)
    form = PersonDepartmentForm(data=data)

    assert form.is_valid()


@pytest.mark.django_db
def test_no_department_fails(yandex_services):
    data = {
        'with_budget': True,
        'service_groups': list(yandex_services.keys()),
        'changing_duties': False,
    }
    form = PersonDepartmentForm(data=data)

    assert not form.is_valid()


@pytest.mark.django_db
def test_both_department_fields_cant_be_filled(company, yandex_services):
    data = {
        'with_budget': True,
        'department': company.dep1.url,
        'fake_department': 'asd',
        'service_groups': list(yandex_services.keys()),
        'changing_duties': False,
    }
    form = PersonDepartmentForm(data=data)

    assert not form.is_valid()
