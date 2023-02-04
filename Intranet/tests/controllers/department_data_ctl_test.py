import pytest

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentStaffFactory

from staff.proposal.controllers.department import DepartmentDataCtl, order_field


@pytest.mark.django_db
def test_as_form_data(company):
    data_ctl = DepartmentDataCtl(
        urls=[company.dep11.url, company.dep2.url],
        person=company.persons['dep1-chief'],
    )

    form_data = data_ctl.as_form_data(company.dep11.url)
    assert 'action_id' in form_data
    assert 'url' in form_data
    assert 'administration' in form_data
    assert 'fake_id' in form_data
    assert 'hierarchy' in form_data
    assert 'name' in form_data
    assert 'sections' in form_data
    assert 'technical' in form_data
    assert 'delete' in form_data

    assert form_data['administration'] == {
        'budget_holder': None,
        'chief': 'dep11-chief',
        'deputies': [],
        'hr_partners': [],
    }
    assert form_data['hierarchy'] == {
        'changing_duties': None,
        'fake_parent': None,
        'parent': company.dep11.parent.url,
    }
    assert form_data['name'] == {
        'hr_type': True,
        'is_correction': False,
        'name': company.dep11.name,
        'name_en': company.dep11.name_en,
    }
    assert form_data['technical'] == {
        'category': company.dep11.category,
        'code': company.dep11.code,
        'department_type': company.dep11.kind.id,
        'hr_type': False,
        'order': company.dep11.position,
        'allowed_overdraft_percents': '',
    }


@pytest.mark.django_db
def test_as_meta(company):
    dep11 = company.dep11
    dep11_chief = company.persons['dep11-chief']

    # На время теста выдадим hr-партнёрство в проверяемом подразделении одному Аркадию
    temp_hr_partnership = DepartmentStaffFactory(
        department=dep11,
        staff=company.persons['dep2-hr-partner'],
        role_id=DepartmentRoles.HR_PARTNER.value,
    )
    data_ctl = DepartmentDataCtl(urls=[dep11.url], person=dep11_chief)

    meta_data = data_ctl.as_meta(dep11.url)

    assert meta_data['budget_holder'] is None
    assert meta_data['category'] == 'nontechnical'
    assert meta_data['chief'] == {
        'first_name': dep11_chief.first_name,
        'id': dep11_chief.id,
        'last_name': dep11_chief.last_name,
        'login': dep11_chief.login,
    }
    assert meta_data['code'] == dep11.code
    assert meta_data['department_type'] == dep11.kind.id
    assert meta_data['deputies'] == []
    assert meta_data['description'] == ''
    assert meta_data['hr_analysts'] == []
    assert meta_data['hr_partners'] == [
        {
            'login': 'dep2-hr-partner',
            'id': company.persons['dep2-hr-partner'].id,
            'first_name': company.persons['dep2-hr-partner'].first_name,
            'last_name': company.persons['dep2-hr-partner'].last_name,
        }
    ]
    assert meta_data['hr_type'] is None
    assert meta_data['id'] == dep11.id
    assert meta_data['name'] == dep11.name
    assert meta_data['order'] == dep11.position
    assert meta_data['parent'] == dep11.parent.url
    assert meta_data['parents'] == ['Яндекс', 'Главный бизнес-юнит', 'dep11']
    assert meta_data['type'] == dep11.kind.name
    assert meta_data['url'] == dep11.url
    assert meta_data['level'] == dep11.level
    assert meta_data['order_field'] == order_field(dep11.tree_id, dep11.lft)

    temp_hr_partnership.delete()
