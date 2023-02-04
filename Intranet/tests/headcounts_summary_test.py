import json
from decimal import Decimal

import pytest
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentRoleFactory, DepartmentFactory, StaffFactory, DepartmentStaffFactory
from staff.oebs.constants import PERSON_POSITION_STATUS

from staff.headcounts.headcounts_summary_model import HeadcountsSummaryModel
from staff.headcounts.models import AllowedHeadcountOverdraft
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.headcounts.views import headcounts_summary, headcounts_summary_for_department


@pytest.mark.django_db()
def test_headcounts_summary_provides_right_order_when_person_has_two_consecutive_roles():
    root_dep = DepartmentFactory(url='root_dep')
    child_dep = DepartmentFactory(url='child_dep', parent=root_dep)
    child_of_child_dep = DepartmentFactory(url='child_of_child_dep', parent=child_dep)

    person = StaffFactory()
    person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    DepartmentStaffFactory(staff=person, department=root_dep, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(staff=person, department=child_dep, role_id=DepartmentRoles.CHIEF.value)

    model = HeadcountsSummaryModel(person, True)
    result = model.departments_summary_for_person(person, valuestream_mode=False)

    assert len(result.departments) == 2
    assert result.departments[0].id == root_dep.id
    assert len(result.departments[0].child_departments) == 0

    assert result.departments[1].id == child_dep.id
    assert len(result.departments[1].child_departments) == 1
    assert result.departments[1].child_departments[0].id == child_of_child_dep.id


@pytest.mark.django_db()
def test_headcounts_summary_provides_all_departments_with_roles(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['yandex-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    nested_dep = DepartmentFactory(parent=company.dep111, name='NesTed')
    DepartmentStaffFactory(department=nested_dep, staff=viewer_person, role_id=DepartmentRoles.CHIEF.value)

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': viewer_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, viewer_person.login)

    assert response.status_code == 200

    result = json.loads(response.content)
    assert 'departments' in result
    assert len(result['departments']) == 2

    parent_department = result['departments'][0]
    assert parent_department['chief']['login'] == 'yandex-chief'
    assert parent_department['department']['url'] == 'yandex'

    assert len(parent_department['child_departments']) == 2

    first_child_department = parent_department['child_departments'][0]
    assert first_child_department['chief']['login'] == 'dep1-chief'
    assert first_child_department['department']['url'] == 'yandex_dep1'

    second_child_department = parent_department['child_departments'][1]
    assert second_child_department['chief']['login'] == 'dep2-chief'
    assert second_child_department['department']['url'] == 'yandex_dep2'

    second_parent_department = result['departments'][1]
    assert len(second_parent_department['child_departments']) == 0


@pytest.mark.django_db()
def test_headcounts_summary_returns_all_results_for_superuser_even_on_missing_roles(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['dep2-person']
    viewer_person.user.is_superuser = True
    viewer_person.user.save()

    request = rf.get(reverse(
        'departments-api:headcounts-summary',
        kwargs={'login': company.persons['yandex-chief'].login}
    ))
    request.user = viewer_person.user

    response = headcounts_summary(request, company.persons['yandex-chief'].login)

    assert response.status_code == 200

    result = json.loads(response.content)
    assert 'departments' in result
    assert len(result['departments']) == 1


@pytest.mark.django_db()
def test_headcounts_summary_returns_empty_results_even_on_observable_missing_roles(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['yandex-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    request = rf.get(reverse(
        'departments-api:headcounts-summary',
        kwargs={'login': company.persons['dep1-person'].login}
    ))
    request.user = viewer_person.user

    response = headcounts_summary(request, company.persons['dep1-person'].login)

    assert response.status_code == 200

    result = json.loads(response.content)
    assert 'departments' in result
    assert len(result['departments']) == 0


@pytest.mark.django_db()
def test_headcounts_summary_allows_to_see_child_cheefs(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['yandex-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    observable_person = company.persons['dep1-chief']

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': observable_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, observable_person.login)

    assert response.status_code == 200

    result = json.loads(response.content)
    assert 'departments' in result
    assert len(result['departments']) == 1

    parent_departments = result['departments']

    assert len(parent_departments[0]['child_departments']) == 2


@pytest.mark.django_db()
def test_headcounts_summary_disallows_to_see_nonchild_chiefs(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['dep1-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    observable_person = company.persons['dep2-chief']

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': observable_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, observable_person.login)

    assert response.status_code == 403


@pytest.mark.django_db()
def test_headcounts_summary_returns_403_on_absent_roles(company_with_module_scope, rf):
    company = company_with_module_scope
    boss = StaffFactory(login='big-boss')

    viewer_person = company.persons['yandex-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': boss.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, boss.login)

    assert response.status_code == 403


@pytest.mark.django_db()
def test_departments_summary_returns_parent_departments_without_chief():
    observable = StaffFactory()
    observable.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    first_parent_department = DepartmentFactory(name='pdep1')
    second_parent_department = DepartmentFactory(name='pdep2')

    DepartmentStaffFactory(
        department=first_parent_department,
        staff=observable,
        role_id=DepartmentRoles.CHIEF.value,
    )
    DepartmentStaffFactory(
        department=second_parent_department,
        staff=observable,
        role_id=DepartmentRoles.HR_PARTNER.value,
    )

    model = HeadcountsSummaryModel(observable, True)
    result = model.departments_summary_for_person(observable, valuestream_mode=False)
    assert len(result.departments) == 2


@pytest.mark.django_db()
def test_departments_summary_returns_child_departments_without_chief():
    observable = StaffFactory()
    observable.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    root_department = DepartmentFactory(name='root')
    first_child = DepartmentFactory(name='first_child', parent=root_department)

    first_child_of_first_child = DepartmentFactory(name='first_child_of_first_child', parent=first_child)
    second_child_of_first_child = DepartmentFactory(name='second_child_of_first_child', parent=first_child)
    leaf = DepartmentFactory(name='leaf', parent=first_child_of_first_child)

    second_child = DepartmentFactory(name='second_child', parent=root_department)

    DepartmentStaffFactory(department=first_child, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=second_child, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=leaf, staff=observable, role_id=DepartmentRoles.CHIEF.value)

    model = HeadcountsSummaryModel(observable, True)
    result = model.departments_summary_for_person(observable, valuestream_mode=False)

    assert len(result.departments) == 3

    assert result.departments[0].id == first_child.id
    assert result.departments[0].child_departments[0].id == first_child_of_first_child.id
    assert result.departments[0].child_departments[1].id == second_child_of_first_child.id

    assert result.departments[1].id == leaf.id
    assert len(result.departments[1].child_departments) == 0

    assert result.departments[2].id == second_child.id
    assert len(result.departments[2].child_departments) == 0


@pytest.mark.django_db()
def test_departments_summary_returns_departments_according_to_observer_roles():
    observable = StaffFactory()
    observer = StaffFactory()
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    root_department = DepartmentFactory(name='root')
    child_department = DepartmentFactory(name='child', parent=root_department)

    some_separate_department = DepartmentFactory()
    DepartmentStaffFactory(department=child_department, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=some_separate_department, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=root_department, staff=observer, role_id=DepartmentRoles.CHIEF.value)

    model = HeadcountsSummaryModel(observer, True)
    result = model.departments_summary_for_person(observable, valuestream_mode=False)
    assert len(result.departments) == 1
    assert result.departments[0].id == child_department.id


@pytest.mark.django_db()
def test_departments_summary_returns_all_departments_for_hr_analyst(company_with_module_scope):
    observable = StaffFactory()
    observer = StaffFactory()

    root_department = DepartmentFactory(name='root')
    child_department = DepartmentFactory(name='child', parent=root_department)

    some_separate_department = DepartmentFactory()
    DepartmentStaffFactory(department=child_department, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=some_separate_department, staff=observable, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(department=root_department, staff=observer, role_id=DepartmentRoles.HR_ANALYST.value)

    model = HeadcountsSummaryModel(observer, True)
    result = model.departments_summary_for_person(observable, valuestream_mode=False)
    assert len(result.departments) == 2
    departments_ids = {department.id for department in result.departments}
    assert departments_ids == {child_department.id, some_separate_department.id}


@pytest.mark.django_db()
def test_departments_summary_returns_deleted_departments_with_positions(company_with_module_scope, rf):
    company = company_with_module_scope
    department = company.dep1
    department.intranet_status = 0
    department.save()

    HeadcountPositionFactory(department=department, category_is_new=True, status=PERSON_POSITION_STATUS.RESERVE)

    viewer_person = company.persons['yandex-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    observable_person = company.persons['dep1-chief']

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': observable_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, observable_person.login)

    assert response.status_code == 200

    result = json.loads(response.content)
    departments = result['departments']
    assert len(departments) == 1
    assert departments[0]['department']['url'] == department.url


@pytest.mark.django_db()
def test_headcounts_summary_provides_right_chief_for_veged_departments():
    second_child_dep = DepartmentFactory(url='yandex_search_interface')
    child_dep = DepartmentFactory(url='yandex_infra_tech_tools')
    root_dep = DepartmentFactory(url='yandex_search_tech_sq_interfaceandtools')

    second_child_dep.parent = root_dep
    second_child_dep.save()  # to make root department id higher than children

    child_dep.parent = root_dep
    child_dep.save()

    person = StaffFactory(login='veged')
    person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    DepartmentStaffFactory(staff=person, department=root_dep, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(staff=person, department=second_child_dep, role_id=DepartmentRoles.CHIEF.value)

    model = HeadcountsSummaryModel(person, True)
    result = model.departments_summary_for_person(person, valuestream_mode=False)
    assert result.departments[0].department.url == root_dep.url
    assert result.departments[1].department.url == second_child_dep.url


@pytest.mark.django_db()
def test_headcounts_summary_disallows_to_see_itself_on_absent_rights(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['dep1-person']
    observable_person = company.persons['dep2-chief']

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': observable_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, observable_person.login)
    assert response.status_code == 403


@pytest.mark.django_db()
def test_headcounts_summary_allows_to_see_itself_on_having_role_with_right(company_with_module_scope, rf):
    company = company_with_module_scope
    viewer_person = company.persons['dep2-person']
    role = DepartmentRoleFactory(id='HEADCOUNT_VIEWER')
    role.permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    DepartmentStaffFactory(
        staff=viewer_person,
        department=company.dep1,
        role=role,
    )

    request = rf.get(reverse('departments-api:headcounts-summary', kwargs={'login': viewer_person.login}))
    request.user = viewer_person.user

    response = headcounts_summary(request, viewer_person.login)
    assert response.status_code == 200


@pytest.mark.django_db()
def test_headcounts_summary_returns_department_summary_for_role_with_permisson(company_with_module_scope, rf):
    company = company_with_module_scope
    role = DepartmentRoleFactory(id='HEADCOUNT_VIEWER')
    role.permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    viewer_person = company.persons['dep2-person']
    DepartmentStaffFactory(staff=viewer_person, department=company.dep111, role=role)

    request = rf.get(
        reverse(
            'departments-api:headcounts-summary-by-departments',
            kwargs={'url': company.dep111.url}
        ),
        {'show_nested': True},
    )
    request.user = viewer_person.user

    response = headcounts_summary_for_department(request, company.dep111.url)

    assert response.status_code == 200
    result = json.loads(response.content)
    assert result['department']['department']['url'] == 'yandex_dep1_dep11_dep111'


@pytest.mark.django_db()
def test_headcounts_summary_returns_overdraft(company_with_module_scope, rf):
    company = company_with_module_scope
    role = DepartmentRoleFactory(id='HEADCOUNT_VIEWER')
    role.permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    viewer_person = company.persons['dep2-person']
    DepartmentStaffFactory(staff=viewer_person, department=company.dep111, role=role)

    request = rf.get(
        reverse(
            'departments-api:headcounts-summary-by-departments',
            kwargs={'url': company.dep111.url},
        ),
        {'show_nested': True},
    )
    request.user = viewer_person.user

    AllowedHeadcountOverdraft.objects.create(
        department=company.dep111,
        percents_with_child_departments=Decimal(round(99.5, 4)),
    )
    HeadcountPositionFactory(department=company.dep111, category_is_new=True, status=PERSON_POSITION_STATUS.RESERVE)
    response = headcounts_summary_for_department(request, company.dep111.url)

    assert response.status_code == 200
    result = json.loads(response.content)
    assert result['department']['department']['url'] == 'yandex_dep1_dep11_dep111'
    assert result['department']['overdraft_percents_with_child'] == '99.50'


@pytest.mark.django_db()
def test_headcounts_summary_doesnt_return_department_summary_without_role_for_department(company_with_module_scope, rf):
    company = company_with_module_scope
    company.reload()
    role = DepartmentRoleFactory(id='HEADCOUNT_VIEWER')
    role.permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    viewer_person = company.persons['dep2-person']
    viewer_person.user.refresh_from_db()

    request = rf.get(
        reverse(
            'departments-api:headcounts-summary-by-departments',
            kwargs={'url': company.dep11.url}
        ),
        {'show_nested': True},
    )
    request.user = viewer_person.user

    response = headcounts_summary_for_department(request, company.dep11.url)

    assert response.status_code == 403
