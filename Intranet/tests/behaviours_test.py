from datetime import date, timedelta

import pytest
from django.contrib.auth.models import Permission

from staff.departments.models import DepartmentRoles
from staff.lib.testing import StaffFactory, DepartmentStaffFactory

from staff.preprofile.action_context import ActionContext
from staff.preprofile.controller_behaviours.employee_behaviour import EmployeeBehaviour
from staff.preprofile.models import FORM_TYPE, PREPROFILE_STATUS
from staff.preprofile.tests.utils import PreprofileFactory


@pytest.mark.django_db
def test_new_employee_cannot_be_adopted(company):
    requested_by = StaffFactory()
    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.NEW,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert not result['adopt']


@pytest.mark.django_db
def test_ready_employee_can_be_saved_by_recruiter(company):
    requested_by = StaffFactory()

    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=requested_by,
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert result['save']


@pytest.mark.django_db
def test_ready_employee_cannot_be_saved_not_by_recruiter(company):
    requested_by = StaffFactory()

    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert not result['save']


@pytest.mark.django_db
def test_ready_employee_can_be_adopted_if_join_date_in_past_and_user_is_adopter(company):
    requested_by = StaffFactory()
    requested_by.user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert result['adopt']


@pytest.mark.django_db
def test_ready_employee_cannot_be_adopted_if_join_date_in_past_and_user_is_not_adopter(company):
    requested_by = StaffFactory()
    requested_by.user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))
    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today() + timedelta(days=1),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert not result['adopt']


@pytest.mark.django_db
def test_ready_employee_cannot_be_adopted_if_join_date_in_future_and_user_is_adopter(company):
    requested_by = StaffFactory()
    model = PreprofileFactory(
        department=company.yandex,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert not result['adopt']


@pytest.mark.django_db
def test_ready_employee_can_be_adopted_if_join_date_in_past_and_user_is_special_chief(company):
    department = company.yandex

    requested_by = StaffFactory()
    requested_by.user.user_permissions.add(Permission.objects.get(codename='chief_that_can_adopt'))
    DepartmentStaffFactory(staff=requested_by, role_id=DepartmentRoles.CHIEF.value, department=department)

    model = PreprofileFactory(
        department=department,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert result['adopt']


@pytest.mark.django_db
def test_ready_employee_cannot_be_adopted_if_join_date_in_past_and_user_is_chief(company):
    department = company.yandex

    requested_by = StaffFactory()
    DepartmentStaffFactory(staff=requested_by, role_id=DepartmentRoles.CHIEF.value, department=department)

    model = PreprofileFactory(
        department=department,
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
        recruiter=StaffFactory(),
    )

    result = EmployeeBehaviour().actions_on_edit(ActionContext(model, requested_by))
    assert not result['adopt']
