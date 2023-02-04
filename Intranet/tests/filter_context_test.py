import pytest

from django.contrib.auth.models import Permission

from staff.lib.testing import DepartmentRoleFactory, DepartmentStaffFactory
from staff.person.models import AFFILIATION

from staff.person_filter.filter_context import FilterContext


@pytest.fixture
def external_person(company):
    person = company.persons['outstaff-person']
    person.affiliation = AFFILIATION.EXTERNAL
    person.work_email = 'outstaff-person@yandex-team.ru'
    person.save()

    return person


@pytest.fixture
def role_with_perm(company):
    role = DepartmentRoleFactory(id='TEST_ROLE_NAME')
    role.permissions.add(Permission.objects.get(codename='can_view_departments'))
    return role


def test_filter_context_returns_descendants_and_ascendants_chain(company, external_person, role_with_perm):
    DepartmentStaffFactory(department=company.dep11, staff=external_person, role=role_with_perm)

    filter_context = FilterContext(
        user=external_person.user,
        observer_tz=external_person.tz,
        permission='django_intranet_stuff.can_view_departments',
    )
    qs = filter_context.get_base_dep_qs(fields=['url'])

    permitted_to_see_departments = [
        company.yandex.url,
        company.dep1.url,
        company.dep11.url,
        company.dep111.url,
    ]

    assert qs.filter(url__in=permitted_to_see_departments).count() == len(permitted_to_see_departments)
    assert qs.exclude(url__in=permitted_to_see_departments).count() == 0


def test_filter_context_returns_persons_only_from_descendants_departments(company, external_person, role_with_perm):
    DepartmentStaffFactory(department=company.dep11, staff=external_person, role=role_with_perm)

    filter_context = FilterContext(
        user=external_person.user,
        observer_tz=external_person.tz,
        permission='django_intranet_stuff.can_view_departments',
    )
    qs = filter_context.get_base_person_qs()

    permitted_to_see_departments_persons = [
        company.persons['dep11-person'].id,
        company.persons['dep11-chief'].id,
        company.persons['dep111-person'].id,
        company.persons['dep111-chief'].id,
        company.persons['dep2-hr-partner'].id,  # Тоже живёт в dep11
    ]

    assert qs.filter(id__in=permitted_to_see_departments_persons).count() == len(permitted_to_see_departments_persons)
    assert qs.exclude(id__in=permitted_to_see_departments_persons).count() == 0
