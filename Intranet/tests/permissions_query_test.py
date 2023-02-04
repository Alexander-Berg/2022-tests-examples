from typing import AnyStr, Callable

import pytest

from django.contrib.auth.models import ContentType, Permission

from staff.departments.models import DepartmentRole, Department
from staff.lib.testing import PermissionFactory, DepartmentRoleFactory, DepartmentStaffFactory
from staff.lib.tests.pytest_fixtures import AttrDict

from staff.person.models import Staff, AFFILIATION


@pytest.fixture
def external_person(company):
    person = company.persons['outstaff-person']
    person.affiliation = AFFILIATION.EXTERNAL
    person.work_email = 'outstaff-person@yandex-team.ru'
    person.save()

    return person


@pytest.fixture()
@pytest.mark.django_db()
def role_with_perm():
    role = DepartmentRoleFactory(id='TEST')
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    role.permissions.add(permission)
    return role


@pytest.fixture()
@pytest.mark.django_db()
def role_with_perm_factory():
    def role_factory(permission_codename: str) -> DepartmentRole:
        role = DepartmentRoleFactory(id='TEST_ROLE_NAME')
        role.permissions.add(Permission.objects.get(codename=permission_codename))
        return role

    return role_factory


def test_departments_by_perm_query_can_return_ancestors_chain(company: AttrDict, role_with_perm: DepartmentRole):
    person: Staff = company.persons['dep111-person']
    DepartmentStaffFactory(role=role_with_perm, department=company.dep111, staff=person)

    result = list(
        Department.objects
        .filter(person.departments_by_perm_query('django_intranet_stuff.test_perm', False))
    )

    assert result == [
        company.yandex,
        company.dep1,
        company.dep11,
        company.dep111,
    ]


def test_departments_by_perm_uses_correct_roles(company, role_with_perm):
    # type: (AttrDict, DepartmentRole) -> None
    another_person = company.persons['dep11-person']  # type: Staff
    DepartmentStaffFactory(role=role_with_perm, department=company.dep11, staff=another_person)

    person = company.persons['dep111-person']  # type: Staff
    role2 = DepartmentRoleFactory(id='TEST2')
    DepartmentStaffFactory(role=role2, department=company.dep11, staff=person)

    result = list(
        Department.objects
        .filter(person.departments_by_perm_query('django_intranet_stuff.test_perm', True))
    )

    assert len(result) == 0


def test_departments_by_perm_query_returns_descendants(company, role_with_perm):
    # type: (AttrDict, DepartmentRole) -> None
    person = company.persons['dep111-person']  # type: Staff
    DepartmentStaffFactory(role=role_with_perm, department=company.dep11, staff=person)

    result = list(
        Department.objects
        .filter(person.departments_by_perm_query('django_intranet_stuff.test_perm', True))
    )

    assert result == [
        company.dep11,
        company.dep111,
    ]


def test_internal_has_access_to_any_department_profiles_by_default(company):
    # type: (AttrDict) -> None
    person = company.persons['dep111-person']  # type: Staff

    assert person.has_access_to_department_profiles(company.yandex.id)
    assert person.has_access_to_department_profiles(company.outstaff.id)
    assert person.has_access_to_department_profiles(company.yamoney.id)
    assert person.has_access_to_department_profiles(company.dep12.id)


def test_external_has_no_access_to_any_department_profiles_by_default(company, external_person):
    # type: (AttrDict, Staff) -> None

    assert not external_person.has_access_to_department_profiles(company.yandex.id)
    assert not external_person.has_access_to_department_profiles(company.outstaff.id)
    assert not external_person.has_access_to_department_profiles(company.yamoney.id)
    assert not external_person.has_access_to_department_profiles(company.dep12.id)


def test_external_with_departments_view_role_can_view_profiles_in_department(
    company: AttrDict,
    external_person: Staff,
    role_with_perm_factory: Callable[[str], DepartmentRole],
):

    role = role_with_perm_factory('can_view_departments')
    DepartmentStaffFactory(staff=external_person, department=company.dep11, role=role)

    assert not external_person.has_access_to_department_profiles(company.yandex.id)
    assert not external_person.has_access_to_department_profiles(company.dep1.id)
    assert external_person.has_access_to_department_profiles(company.dep11.id)
    assert external_person.has_access_to_department_profiles(company.dep111.id)


def test_external_with_profiles_view_role_can_view_profiles_in_department(
    company: AttrDict,
    external_person: Staff,
    role_with_perm_factory: Callable[[AnyStr], DepartmentRole],
):
    role = role_with_perm_factory('can_view_profiles')
    DepartmentStaffFactory(staff=external_person, department=company.dep11, role=role)

    assert not external_person.has_access_to_department_profiles(company.yandex.id)
    assert not external_person.has_access_to_department_profiles(company.dep1.id)
    assert external_person.has_access_to_department_profiles(company.dep11.id)
    assert external_person.has_access_to_department_profiles(company.dep111.id)
