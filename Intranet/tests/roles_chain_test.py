import mock
import pytest

from staff.lib.models.roles_chain import (
    chiefs_chain_for_person,
    direct_hr_partners_for_department,
    get_grouped_chiefs_by_departments,
)

from staff.departments.models import DepartmentRoles, DepartmentStaff


def test_get_hrbp_roles_qs_fields():
    from staff.lib.models.roles_chain import _get_roles_qs_fields
    assert _get_roles_qs_fields(None) == {
        'staff__id',
        'department__id',
        'department__tree_id',
        'department__lft',
        'department__rght',
        'role_id',
    }
    assert _get_roles_qs_fields(['login']) == {
        'staff__login',
        'staff__id',
        'department__id',
        'department__tree_id',
        'department__lft',
        'department__rght',
        'role_id',
    }
    assert _get_roles_qs_fields(['login', 'id']) == {
        'staff__login',
        'staff__id',
        'department__id',
        'department__tree_id',
        'department__lft',
        'department__rght',
        'role_id',
    }


def test_get_hrbp_by_departments_qs():
    qs_mock_res = object()
    departments = object()
    filter_mock_res = object()
    fields = ['login']
    roles = ['H']

    with mock.patch('staff.lib.models.roles_chain.filter_by_heirarchy', return_value=filter_mock_res) as filter_mock:
        with mock.patch('staff.lib.models.roles_chain._get_roles_qs', return_value=qs_mock_res) as qs_mock:
            from staff.lib.models.roles_chain import get_roles_by_departments_qs
            actual = get_roles_by_departments_qs(
                department_list=departments,
                fields=fields,
                roles=[DepartmentRoles.HR_PARTNER.value],
            )
            assert actual == filter_mock_res
            qs_mock.assert_called_once_with(fields=fields, roles=roles)
            filter_mock.assert_called_once_with(
                query_set=qs_mock_res,
                mptt_objects=departments,
                by_children=False,
                filter_prefix='department__',
                include_self=True,
            )


def test_get_hrbp_by_departments():
    departments = [
        {'id': 1, 'tree_id': 1, 'lft': 1, 'rght': 6},
        {'id': 2, 'tree_id': 1, 'lft': 2, 'rght': 5},
        {'id': 3, 'tree_id': 1, 'lft': 3, 'rght': 4},
    ]

    mock_data = {
        1: [
            {'id': 10, 'login': 'l10'},
            {'id': 20, 'login': 'l20'},
        ],
        2: [
            {'id': 30, 'login': 'l30'},
        ],
    }

    result = [{'id': 10, 'login': 'l10'}, {'id': 20, 'login': 'l20'}, {'id': 30, 'login': 'l30'}]

    with mock.patch('staff.lib.models.roles_chain.get_grouped_hrbp_by_departments', return_value=mock_data) as func:
        from staff.lib.models.roles_chain import get_hrbp_by_departments

        assert list(get_hrbp_by_departments(department_list=departments, fields=['login'])) == result

        func.assert_called_once_with(department_list=departments, fields=['login'])


def test_get_grouped_hrbp_by_departments():
    departments = [
        {'id': 1, 'tree_id': 1, 'lft': 1, 'rght': 6},
        {'id': 2, 'tree_id': 1, 'lft': 2, 'rght': 5},
        {'id': 3, 'tree_id': 1, 'lft': 3, 'rght': 4},
    ]

    p10 = {'id': 10, 'login': 'l10'}
    p20 = {'id': 20, 'login': 'l20'}
    p30 = {'id': 30, 'login': 'l30'}

    mock_data = [
        {
            'staff__id': 10,
            'staff__login': 'l10',
            'department__id': 2,
            'department__tree_id': 1,
            'department__lft': 2,
            'department__rght': 5,
        },
        {
            'staff__id': 20,
            'staff__login': 'l20',
            'department__id': 2,
            'department__tree_id': 1,
            'department__lft': 2,
            'department__rght': 5,
        },
        {
            'staff__id': 30,
            'staff__login': 'l30',
            'department__id': 1,
            'department__tree_id': 1,
            'department__lft': 1,
            'department__rght': 6,
        },
    ]

    result = {
        1: [p30],
        2: [p10, p20],
        3: [p10, p20]
    }

    with mock.patch('staff.lib.models.roles_chain.get_roles_by_departments_qs', return_value=mock_data) as func:
        from staff.lib.models.roles_chain import get_grouped_hrbp_by_departments

        assert get_grouped_hrbp_by_departments(department_list=departments, fields=['login']) == result

        func.assert_called_once_with(department_list=departments, fields=['login'], roles=['H'])


def test_get_grouped_hrbp_by_persons():
    persons = [
        {'id': 1, 'department': {'id': 1, 'tree_id': 1, 'lft': 1, 'rght': 1}},
        {'id': 2, 'department': {'id': 1, 'tree_id': 1, 'lft': 1, 'rght': 1}},
        {'id': 3, 'department': {'id': 2, 'tree_id': 2, 'lft': 2, 'rght': 2}},
    ]

    mock_data = {
        1: [{'id': 10, 'login': 'l10'}],
        2: [{'id': 20, 'login': 'l20'}, {'id': 30, 'login': 'l30'}]
    }

    result = {1: mock_data[1], 2: mock_data[1], 3: mock_data[2]}

    with mock.patch('staff.lib.models.roles_chain.get_grouped_hrbp_by_departments', return_value=mock_data) as func:
        from staff.lib.models.roles_chain import get_grouped_hrbp_by_persons

        assert get_grouped_hrbp_by_persons(person_list=persons, fields=['login']) == result

        func.assert_called_once_with(
            department_list=[
                {'id': 1, 'tree_id': 1, 'lft': 1, 'rght': 1},
                {'id': 2, 'tree_id': 2, 'lft': 2, 'rght': 2},
            ],
            fields=['login']
        )


@pytest.mark.django_db()
def test_get_grouped_chiefs_by_departments(company):
    dep_to_check = company.dep11
    DepartmentStaff.objects.filter(
        department=dep_to_check,
        role_id=DepartmentRoles.CHIEF.value,
    ).delete()
    dep_to_chief = get_grouped_chiefs_by_departments([dep_to_check], fields=['login'])
    chief = dep_to_chief[dep_to_check.id]
    assert chief and chief['staff__login'] == 'dep1-chief'


@pytest.mark.django_db()
def test_direct_hr_partners_for_department(company):
    dep_for_hrbp = company.dep1
    dep_to_check = company.dep11

    hrbp2 = company.persons['dep2-hr-partner']

    DepartmentStaff.objects.filter(staff=hrbp2, department=dep_to_check).update(department=dep_for_hrbp)

    partners = direct_hr_partners_for_department(dep_to_check, ['login'])

    assert len(partners) == 1
    assert partners[0]['login'] == 'dep1-hr-partner'


@pytest.mark.django_db()
def test_direct_hr_partners_for_department_returns_none_on_absent_hrbp(company):
    dep_to_check = company.dep111
    DepartmentStaff.objects.filter(role='H').delete()
    assert direct_hr_partners_for_department(dep_to_check, ['login']) is None


@pytest.mark.django_db()
def test_direct_chiefs_chain_for_chief(company):
    person_for_check = company.persons['dep12-chief']

    result = chiefs_chain_for_person(person_for_check, fields=['login'])

    assert [
        {'login': company.persons['dep1-chief'].login, 'id': company.persons['dep1-chief'].id},
        {'login': company.persons['yandex-chief'].login, 'id': company.persons['yandex-chief'].id},
    ] == result


@pytest.mark.django_db()
def test_direct_chiefs_chain_for_person(company):
    person_for_check = company.persons['dep12-person']

    result = chiefs_chain_for_person(person_for_check, fields=['login'])

    assert [
        {'login': company.persons['dep12-chief'].login, 'id': company.persons['dep12-chief'].id},
        {'login': company.persons['dep1-chief'].login, 'id': company.persons['dep1-chief'].id},
        {'login': company.persons['yandex-chief'].login, 'id': company.persons['yandex-chief'].id},
    ] == result


@pytest.mark.django_db()
def test_direct_chiefs_chain_for_person_with_general_director(company):
    person_for_check = company.persons['dep12-person']

    result = chiefs_chain_for_person(
        person=person_for_check,
        fields=['login'],
        roles=[DepartmentRoles.CHIEF.value, DepartmentRoles.GENERAL_DIRECTOR.value],
    )

    assert [
        {'login': company.persons['dep12-chief'].login, 'id': company.persons['dep12-chief'].id},
        {'login': company.persons['dep1-chief'].login, 'id': company.persons['dep1-chief'].id},
        {'login': company.persons['yandex-chief'].login, 'id': company.persons['yandex-chief'].id},
        {'login': company.persons['general'].login, 'id': company.persons['general'].id},
    ] == result


@pytest.mark.django_db()
def test_direct_chiefs_chain_for_top(company):
    person_for_check = company.persons['yandex-chief']

    result = chiefs_chain_for_person(person_for_check, fields=['login'])

    assert [] == result
