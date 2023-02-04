import pytest

from django.contrib.auth.models import ContentType
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentRoleFactory,
    DepartmentStaffFactory,
    GroupFactory,
    GroupMembershipFactory,
    PermissionFactory,
    StaffFactory,
)

from staff.departments.models import Department


@pytest.mark.django_db
def test_has_perm_returns_true_for_any_department_on_superuser():
    PermissionFactory(content_type=ContentType.objects.get_for_model(Department), codename='test_perm')
    person = StaffFactory()
    person.user.is_superuser = True

    assert person.user.has_perm('django_intranet_stuff.test_perm', DepartmentFactory())


@pytest.mark.django_db
def test_has_perm_returns_true_for_child_departments():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()

    root_department = DepartmentFactory()
    child_department = DepartmentFactory(parent=root_department)

    role = DepartmentRoleFactory(id='TEST')
    DepartmentStaffFactory(role=role, department=root_department, staff=person)
    role.permissions.add(permission)
    assert person.user.has_perm('django_intranet_stuff.test_perm', child_department)


@pytest.mark.django_db
def test_has_perm_returns_false_for_parent_departments():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()

    root_department = DepartmentFactory()
    child_department = DepartmentFactory(parent=root_department)

    role = DepartmentRoleFactory(id='TEST')
    DepartmentStaffFactory(role=role, department=child_department, staff=person)
    role.permissions.add(permission)
    assert not person.user.has_perm('django_intranet_stuff.test_perm', root_department)


@pytest.mark.django_db
def test_has_perm_returns_false_on_wrong_perm():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()

    root_department = DepartmentFactory()
    child_department = DepartmentFactory()

    role = DepartmentRoleFactory(id='TEST')
    DepartmentStaffFactory(role=role, department=child_department, staff=person)
    role.permissions.add(permission)
    assert not person.user.has_perm('django_intranet_stuff.test_perm_wrong', root_department)


@pytest.mark.django_db
def test_has_perm_returns_true_when_user_has_perm():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()
    person.user.user_permissions.add(permission)
    assert person.user.has_perm('django_intranet_stuff.test_perm')


@pytest.mark.django_db
def test_has_perm_returns_false_when_user_has_no_perm():
    PermissionFactory(content_type=ContentType.objects.get_for_model(Department), codename='test_perm')
    person = StaffFactory()
    assert not person.user.has_perm('django_intranet_stuff.test_perm')


@pytest.mark.django_db
def test_has_perm_returns_true_when_user_has_perm_through_django_groups():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()

    from django.contrib.auth.models import Group
    group = Group.objects.create(name='123')
    group.permissions.add(permission)
    person.user.groups.add(group)

    assert person.user.has_perm('django_intranet_stuff.test_perm')


@pytest.mark.django_db
def test_has_perm_returns_true_when_user_has_perm_through_staff_groups():
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory()

    group = GroupFactory()
    group.permissions.add(permission)
    GroupMembershipFactory(staff=person, group=group)

    assert person.user.has_perm('django_intranet_stuff.test_perm')
