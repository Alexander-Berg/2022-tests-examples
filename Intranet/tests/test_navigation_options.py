import pytest
from waffle.models import Switch

from staff.departments.models import DepartmentRoles
from staff.lib.testing import StaffFactory, DepartmentStaffFactory, DepartmentFactory

from staff.navigation.controller import is_management_options_available


@pytest.fixture
def _waffle_switch(db):
    Switch(name='enable_management_options_navigation', active=True).save()


@pytest.mark.django_db
def test_management_options_available_for_hr_analyst(_waffle_switch):
    person = StaffFactory()
    DepartmentStaffFactory(staff=person, role_id=DepartmentRoles.HR_ANALYST.value, department=DepartmentFactory())

    assert is_management_options_available(person)


@pytest.mark.skip
@pytest.mark.django_db
def test_management_options_available_for_director(_waffle_switch):
    person = StaffFactory()
    DepartmentStaffFactory(staff=person, role_id=DepartmentRoles.GENERAL_DIRECTOR.value, department=DepartmentFactory())

    assert is_management_options_available(person)


@pytest.mark.skip
@pytest.mark.django_db
def test_management_options_available_for_chiefs(_waffle_switch):
    person = StaffFactory()
    DepartmentStaffFactory(staff=person, role_id=DepartmentRoles.CHIEF.value, department=DepartmentFactory())

    assert is_management_options_available(person)


@pytest.mark.django_db
def test_management_options_available_for_su(_waffle_switch):
    person = StaffFactory()
    person.user.is_superuser = True
    person.user.save()

    assert is_management_options_available(person)


@pytest.mark.django_db
def test_management_options_not_available_for_regular_user(_waffle_switch):
    person = StaffFactory()

    assert not is_management_options_available(person)
