import pytest

from staff.departments.models import DepartmentRoles, DepartmentStaff
from staff.departments.tests.factories import VacancyFactory
from staff.lib.testing import DepartmentFactory, DepartmentStaffFactory, StaffFactory

from staff.preprofile.crown_depriving import (
    DeparmentsWithRoles,
    RolesInDepartment,
    _remove_person_crowns,
    _roles_in_departments,
    deprive_gold_and_silver_crown_through_proposal,
)


def test_remove_person_crowns():
    # given
    test_person_id = 123
    departments_with_roles: DeparmentsWithRoles = {
        'some_dep_url': RolesInDepartment(chief_id=test_person_id, deputies_id=set()),
        'some_dep_url2': RolesInDepartment(chief_id=3, deputies_id={2, test_person_id, 1}),
        'some_dep_url3': RolesInDepartment(chief_id=None, deputies_id={test_person_id}),
    }

    # when
    result = _remove_person_crowns(test_person_id, departments_with_roles)

    # then
    assert result == {
        'some_dep_url': RolesInDepartment(chief_id=None, deputies_id=set()),
        'some_dep_url2': RolesInDepartment(chief_id=3, deputies_id={2, 1}),
        'some_dep_url3': RolesInDepartment(chief_id=None, deputies_id=set()),
    }


@pytest.mark.django_db
def test_roles_in_department():
    # given
    deprived_person = StaffFactory()
    second_person = StaffFactory()
    third_person = StaffFactory()
    fourth_person = StaffFactory()

    first_department = DepartmentFactory()
    second_department = DepartmentFactory()
    third_department = DepartmentFactory()

    DepartmentStaffFactory(role_id=DepartmentRoles.CHIEF.value, staff=deprived_person, department=first_department)

    DepartmentStaffFactory(role_id=DepartmentRoles.CHIEF.value, staff=fourth_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=second_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=deprived_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=third_person, department=second_department)

    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=deprived_person, department=third_department)

    # when
    roles_in_department = _roles_in_departments(deprived_person)

    # then
    assert roles_in_department == {
        first_department.url: RolesInDepartment(chief_id=deprived_person.id, deputies_id=set()),
        second_department.url: RolesInDepartment(
            chief_id=fourth_person.id,
            deputies_id={second_person.id, deprived_person.id, third_person.id},
        ),
        third_department.url: RolesInDepartment(chief_id=None, deputies_id={deprived_person.id}),
    }


@pytest.mark.django_db
def test_crown_depriving(mocked_mongo, deadlines):
    # given
    vacancy = VacancyFactory(offer_id=15222, salary_ticket='SALARY-122')
    deprived_person = StaffFactory()
    second_person = StaffFactory()
    third_person = StaffFactory()
    fourth_person = StaffFactory()

    first_department = DepartmentFactory()
    second_department = DepartmentFactory()
    third_department = DepartmentFactory()

    DepartmentStaffFactory(role_id=DepartmentRoles.CHIEF.value, staff=deprived_person, department=first_department)

    DepartmentStaffFactory(role_id=DepartmentRoles.CHIEF.value, staff=fourth_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=second_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=deprived_person, department=second_department)
    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=third_person, department=second_department)

    DepartmentStaffFactory(role_id=DepartmentRoles.DEPUTY.value, staff=deprived_person, department=third_department)

    # when
    deprive_gold_and_silver_crown_through_proposal(third_person, deprived_person, vacancy.offer_id)

    # then
    assert not DepartmentStaff.objects.filter(department=first_department).exists()
    second_department_roles = set(
        DepartmentStaff.objects
        .filter(department=second_department)
        .values_list('staff__login', flat=True)
    )
    assert second_department_roles == {fourth_person.login, second_person.login, third_person.login}
    assert not DepartmentStaff.objects.filter(department=third_department).exists()
