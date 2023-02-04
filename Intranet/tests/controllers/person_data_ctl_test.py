from datetime import datetime

import pytest

from staff.lib.testing import GroupFactory, GroupMembershipFactory

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.person.models import Occupation, StaffExtraFields

from staff.proposal.controllers.department import order_field
from staff.proposal.controllers.person import PersonDataCtl


@pytest.mark.django_db
def test_as_form_data(company):
    person1 = company.persons['dep1-chief']
    person2 = company.persons['dep12-person']

    valid_servicegroup = GroupFactory(
        url='svc_devoops',
        type=GROUP_TYPE_CHOICES.SERVICE,
        service_id=1,
        department=None,
    )
    invalid_servicegroup = GroupFactory(
        url='del_abcdef98765',
        intranet_status=0,
        type=GROUP_TYPE_CHOICES.SERVICE,
        service_id=2,
        department=None,
    )

    GroupMembershipFactory(staff=person1, group=valid_servicegroup)
    GroupMembershipFactory(staff=person2, group=valid_servicegroup)
    GroupMembershipFactory(staff=person2, group=invalid_servicegroup)
    occupation = Occupation.objects.create(
        name='Someone',
        description='someone',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    StaffExtraFields.objects.create(staff=person1, occupation=occupation)

    data_ctl = PersonDataCtl(logins=[person1.login, person2.login])

    person1_form_data = data_ctl.as_form_data(person1.login)

    assert person1_form_data == {
        'action_id': '',
        'comment': '',
        'department': {
            'changing_duties': None,
            'department': '',
            'fake_department': '',
            'from_maternity_leave': False,
            'service_groups': [valid_servicegroup.url],
            'vacancy_url': '',
            'with_budget': True,
        },
        'grade': {
            'new_grade': '0',
            'occupation': 'Someone',
            'force_recalculate_schemes': False,
        },
        'login': person1.login,
        'office': {'office': person1.office_id},
        'organization': {'organization': person1.organization_id},
        'position': {'new_position': '', 'position_legal': ''},
        'salary': {
            'new_currency': 'RUB', 'new_rate': '', 'new_salary': '', 'new_wage_system': 'fixed',
            'old_currency': 'RUB', 'old_rate': '', 'old_salary': '', 'old_wage_system': 'fixed',
        },
        'value_stream': {
            'value_stream': '',
        },
        'sections': [],
    }

    person2_form_data = data_ctl.as_form_data(person2.login)
    assert person2_form_data['department'] == {
        'changing_duties': None,
        'department': '',
        'fake_department': '',
        'from_maternity_leave': False,
        'service_groups': [valid_servicegroup.url],
        'vacancy_url': '',
        'with_budget': True,
    }


@pytest.mark.django_db
def test_as_meta(company):
    person = company.persons['dep1-chief']
    occupation = Occupation.objects.create(
        name='Someone',
        description='someone',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    StaffExtraFields.objects.create(staff=person, occupation=occupation)

    data_ctl = PersonDataCtl(logins=[person.login])

    person_meta_data = data_ctl.as_meta(person.login)

    assert person_meta_data == {
        'department_name': person.department.name,
        'department_url': person.department.url,
        'department_level': person.department.level,
        'department_order_field': order_field(person.department.tree_id, person.department.lft),
        'first_name': person.first_name,
        'id': person.id,
        'last_name': person.last_name,
        'login': person.login,
        'office_id': person.office_id,
        'office_name': person.office.name,
        'organization_name': company.organizations['yandex'].name,
        'position': person.position,
        'services': [],
        'occupation_name': 'Someone',
        'occupation_description': 'someone',
    }
