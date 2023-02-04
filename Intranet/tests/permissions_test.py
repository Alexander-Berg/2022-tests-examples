import pytest

from django.contrib.auth.models import Permission

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import DepartmentRoles, Department
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentRoleFactory,
    DepartmentStaffFactory,
    StaffFactory,
    UserFactory,
)
from staff.lib.tests.pytest_fixtures import AttrDict
from staff.person.models import Staff

from staff.headcounts.permissions import Permissions


@pytest.fixture
def headcount_viewer_role() -> str:
    role_id = 'HEADCOUNT_VIEWER'
    permission = Permission.objects.get(codename='can_view_headcounts')
    role = DepartmentRoleFactory(id=role_id)
    role.permissions.add(permission)
    return role_id


@pytest.mark.django_db()
def test_that_hr_analyst_has_access_to_everything():
    s = StaffFactory()
    d = DepartmentFactory()
    DepartmentStaffFactory(department=d, staff=s, role_id=DepartmentRoles.HR_ANALYST.value)

    assert Permissions(s).has_access_to_department_url(None)


@pytest.mark.django_db()
def test_that_general_director_has_access_to_everything():
    s = StaffFactory()
    d = DepartmentFactory()
    DepartmentStaffFactory(department=d, staff=s, role_id=DepartmentRoles.GENERAL_DIRECTOR.value)

    assert Permissions(s).has_access_to_department_url(None)


@pytest.mark.django_db()
def test_that_robot_with_export_perm_has_access_to_everything():
    s = StaffFactory(is_robot=True)
    s.user.user_permissions.add(Permission.objects.get(codename='can_export_ceilings'))

    assert Permissions(s).has_access_to_department_url(None)


@pytest.mark.django_db()
def test_that_user_with_export_perm_has_no_access_to_everything():
    s = StaffFactory(is_robot=False)
    s.user.user_permissions.add(Permission.objects.get(codename='can_export_ceilings'))

    assert not Permissions(s).has_access_to_department_url(None)


@pytest.mark.django_db()
def test_that_staff_has_no_access_to_everything():
    s = StaffFactory()
    assert not Permissions(s).has_access_to_department_url(None)


@pytest.mark.django_db()
def test_that_hr_partner_has_access_to_his_departments():
    s = StaffFactory()
    s.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    parent_department = DepartmentFactory()
    d2 = DepartmentFactory(parent=parent_department, url='some')
    DepartmentStaffFactory(department=parent_department, staff=s, role_id=DepartmentRoles.HR_PARTNER.value)

    assert Permissions(s).has_access_to_department_url(d2.url)


@pytest.mark.django_db()
def test_that_hr_partner_has_no_access_to_his_departments_without_permission():
    s = StaffFactory()
    d = DepartmentFactory()
    d2 = DepartmentFactory(parent=d, url='some')
    DepartmentStaffFactory(department=d, staff=s, role_id=DepartmentRoles.HR_PARTNER.value)

    assert not Permissions(s).has_access_to_department_url(d2.url)


@pytest.mark.django_db()
def test_that_chief_has_access_to_his_departments():
    s = StaffFactory()
    s.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    d = DepartmentFactory()
    d2 = DepartmentFactory(parent=d, url='some')
    DepartmentStaffFactory(department=d, staff=s, role_id=DepartmentRoles.CHIEF.value)

    assert Permissions(s).has_access_to_department_url(d2.url)


@pytest.mark.django_db()
def test_that_chief_has_no_access_to_his_departments_without_permission():
    s = StaffFactory()
    d = DepartmentFactory()
    d2 = DepartmentFactory(parent=d, url='some')
    DepartmentStaffFactory(department=d, staff=s, role_id=DepartmentRoles.CHIEF.value)

    assert not Permissions(s).has_access_to_department_url(d2.url)


@pytest.mark.django_db()
def test_that_staff_has_no_access_to_departments():
    s = StaffFactory()
    d = DepartmentFactory(url='some')
    assert not Permissions(s).has_access_to_department_url(d.url)


@pytest.mark.django_db
def test_has_management_options_on_general_director_role() -> None:
    # given
    department = DepartmentFactory()
    observer = StaffFactory()
    DepartmentStaffFactory(department=department, staff=observer, role_id=DepartmentRoles.GENERAL_DIRECTOR.value)
    permissions = Permissions(observer)

    # then
    assert permissions.has_management_options() is True


@pytest.mark.django_db
def test_has_management_options_on_hr_analyst_role() -> None:
    # given
    observer = StaffFactory()
    DepartmentStaffFactory(department=DepartmentFactory(), staff=observer, role_id=DepartmentRoles.HR_ANALYST.value)
    permissions = Permissions(observer)

    # then
    assert permissions.has_management_options() is True


@pytest.mark.django_db
def test_has_management_options_on_su() -> None:
    # given
    observer = StaffFactory(user=UserFactory(is_superuser=True))
    permissions = Permissions(observer)

    # then
    assert permissions.has_management_options() is True


@pytest.mark.django_db
def test_has_no_management_options_on_regular_user() -> None:
    # given
    observer = StaffFactory()
    permissions = Permissions(observer)

    # then
    assert permissions.has_management_options() is False


@pytest.mark.django_db
def test_filter_by_observer_works_with_role_with_permission(company: AttrDict, headcount_viewer_role: str) -> None:
    # given
    observer = StaffFactory()
    DepartmentStaffFactory(department=company.dep1, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)

    # when
    result = set(permissions.filter_by_observer(Department.objects.all()).values_list('id', flat=True))

    # then
    assert result == set(company.dep1.get_descendants(include_self=True).values_list('id', flat=True))


@pytest.mark.django_db
def test_filter_by_observer_works_with_chief_with_permission(company: AttrDict) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    permissions = Permissions(observer)

    # when
    result = set(permissions.filter_by_observer(Department.objects.all()).values_list('id', flat=True))

    # then
    assert result == set(company.dep1.get_descendants(include_self=True).values_list('id', flat=True))


@pytest.mark.django_db
def test_filter_by_observer_works_with_all_roles_together(company: AttrDict, headcount_viewer_role: str) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    DepartmentStaffFactory(department=company.dep2, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)

    # when
    result = set(permissions.filter_by_observer(Department.objects.all()).values_list('id', flat=True))

    # then
    visible_by_chief_role = set(company.dep1.get_descendants(include_self=True).values_list('id', flat=True))
    visible_by_headcount_viewer_role = set(company.dep2.get_descendants(include_self=True).values_list('id', flat=True))
    assert result == visible_by_chief_role.union(visible_by_headcount_viewer_role)


@pytest.mark.django_db
def test_filter_by_observer_gives_nothing_on_regular_chief(company: AttrDict) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    permissions = Permissions(observer)

    # when
    result = set(permissions.filter_by_observer(Department.objects.all()).values_list('id', flat=True))

    # then
    assert result == set()


@pytest.mark.django_db
def test_filter_by_observer_gives_nothing_on_regular_user(company: AttrDict) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-person'].id)
    permissions = Permissions(observer)

    # when
    result = set(permissions.filter_by_observer(Department.objects.all()).values_list('id', flat=True))

    # then
    assert result == set()


@pytest.mark.django_db
def test_departments_with_headcount_permission_works_with_role(company: AttrDict, headcount_viewer_role: str) -> None:
    # given
    observer = StaffFactory()
    DepartmentStaffFactory(department=company.dep1, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)

    # when
    result = set(permissions.departments_with_headcount_permission().values_list('id', flat=True))

    # then
    assert result == {company.dep1.id}


@pytest.mark.django_db
def test_departments_with_headcount_permission_works_with_value_stream_roles(
    company: AttrDict,
    headcount_viewer_role: str,
) -> None:
    # given
    observer = StaffFactory()
    DepartmentStaffFactory(department=company.vs_2, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)

    # when
    result = set(permissions.departments_with_headcount_permission().values_list('id', flat=True))

    # then
    assert result == {company.vs_2.id}


@pytest.mark.django_db
def test_departments_with_headcount_permission_gives_nothing_on_regular_chief(company: AttrDict) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    permissions = Permissions(observer)

    # when
    result = set(permissions.departments_with_headcount_permission().values_list('id', flat=True))

    # then
    assert result == set()


@pytest.mark.django_db
def test_departments_with_headcount_permission_work_with_chief_with_permission(company: AttrDict) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    permissions = Permissions(observer)

    # when
    result = set(permissions.departments_with_headcount_permission().values_list('id', flat=True))

    # then
    assert result == {company.dep1.id}


@pytest.mark.django_db
def test_departments_with_headcount_permission_works_with_roles_and_chiefs_together(
    company: AttrDict,
    headcount_viewer_role: str,
) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    DepartmentStaffFactory(department=company.vs_2, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)

    # when
    result = set(permissions.departments_with_headcount_permission().values_list('id', flat=True))

    # then
    assert result == {company.dep1.id, company.vs_2.id}


@pytest.mark.django_db
def test_can_create_applications_when_access_through_vs(company: AttrDict, headcount_viewer_role: str) -> None:
    # given
    observer = Staff.objects.get(id=company.persons['dep1-chief'].id)
    DepartmentStaffFactory(department=company.vs_2, staff=observer, role_id=headcount_viewer_role)
    permissions = Permissions(observer)
    bpa = BudgetPositionAssignmentFactory(department=company.dep2, value_stream=company.vs_21)

    # when
    result = permissions.can_create_applications([bpa.budget_position_id])

    # then
    assert result
