import pytest

from django.contrib.auth.models import Permission

from staff.proposal.forms.department import DepartmentName, DepartmentEditForm

correct_name_en_data = {'name': 'Пордазделение', 'name_en': 'Department'}
wrong_name_en_data = {'name': 'Пордазделение', 'name_en': 'Департамент'}


@pytest.mark.parametrize(
    'test_data, is_valid',
    [(correct_name_en_data, True), (wrong_name_en_data, False)]
)
def test_validate_name_en(test_data, is_valid):
    form = DepartmentName(data=test_data)
    assert form.is_valid() == is_valid


@pytest.mark.django_db
def test_name_changes_required(company, mocked_mongo):
    department = company.dep12
    data = {
        'action_id': 'act1174834',
        'url': department.url,
        'fake_id': '',
        'sections': ['name'],
        'name': {
            'name': department.name,
            'name_en': department.name_en,
            'hr_type': 'true',
            'is_correction': 'false',
        }
    }

    form = DepartmentEditForm(data=data, base_initial={'changes_controlled_by_hr': True})
    assert form.is_valid() is False
    assert form.errors['errors']['name'] == [{'code': 'name_changes_required'}]


@pytest.mark.django_db
@pytest.mark.parametrize('is_correction, is_valid', [('false', False), ('true', True)])
def test_is_correction_required_for_executers(company, mocked_mongo, is_correction, is_valid):
    department = company.dep12
    author = company.persons['yandex-person']
    can_execute_permission = Permission.objects.get(codename='can_execute_department_proposals')
    author.user.user_permissions.add(can_execute_permission)

    data = {
        'action_id': 'act1174834',
        'url': department.url,
        'fake_id': '',
        'sections': ['name'],
        'name': {
            'name': department.name,
            'name_en': department.name_en[1:-1],
            'hr_type': 'true',
            'is_correction': is_correction,
        }
    }

    form = DepartmentEditForm(data=data, base_initial={'changes_controlled_by_hr': True})
    form.base_initial['author_user'] = author.user

    assert form.is_valid() is is_valid
    if not is_valid:
        assert form.errors['errors']['name'] == [{'code': 'is_correction_required'}]


@pytest.mark.django_db
def test_name_changes_required_for_non_executers(company, mocked_mongo):
    department = company.dep12
    author = company.persons['dep2-person']

    data = {
        'action_id': 'act1174834',
        'url': department.url,
        'fake_id': '',
        'sections': ['name'],
        'name': {
            'name': department.name,
            'name_en': department.name_en[1:-1],
            'hr_type': 'true',
            'is_correction': 'false',
        }
    }

    form = DepartmentEditForm(data=data, base_initial={'changes_controlled_by_hr': True})
    form.base_initial['author_user'] = author.user
    assert form.is_valid() is False
    assert form.errors['errors']['name'] == [{'code': 'name_changes_required'}]
