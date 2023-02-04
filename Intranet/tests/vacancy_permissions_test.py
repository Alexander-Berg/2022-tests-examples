import pytest
from django.contrib.contenttypes.models import ContentType

from staff.headcounts.permissions import Permissions

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentRoleFactory,
    DepartmentStaffFactory,
    PermissionFactory,
    StaffFactory,
)

from staff.departments.models import Vacancy
from staff.departments.tests.factories import VacancyFactory
from staff.departments.vacancy_permissions import VacancyPermissions


@pytest.mark.django_db
def test_vacancy_filter_qs_can_view_headcounts_on_department_access():
    observer = StaffFactory()
    department = DepartmentFactory()
    app_label, codename = Permissions.permission_fullname.split('.')
    permission = PermissionFactory(codename=codename, content_type=ContentType.objects.create(app_label=app_label))
    role = DepartmentRoleFactory()
    role.permissions.add(permission)
    DepartmentStaffFactory(
        department=department,
        staff=observer,
        role=role,
    )

    budget_position_assignment = BudgetPositionAssignmentFactory(department=department)
    vacancy = VacancyFactory(headcount_position_code=budget_position_assignment.budget_position.code)

    target = VacancyPermissions(observer)

    result = Vacancy.objects.filter(target.vacancy_filter_qs()).first()

    assert result is not None, 'No vacancy found despite permission'
    assert result.id == vacancy.id, 'Wrong vacancy found'
