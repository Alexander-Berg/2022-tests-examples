from mock import Mock
import pytest

from django.contrib.auth.models import Permission

from staff.lib.testing import (
    DepartmentRoleFactory,
    DepartmentStaffFactory,
    UserFactory,
    StaffFactory,
)

from staff.gap.workflows.utils import find_workflow
from staff.gap.controllers.templates import TemplatesCtl
from staff.gap.tests.constants import TEMPLATES_MONGO_COLLECTION


@pytest.fixture
def external_gap_case(gap_test, company):
    AbsenceWorkflow = find_workflow('absence')
    inner_gap = AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(gap_test.get_base_gap(AbsenceWorkflow))
    inner_person = gap_test.test_person

    external_person = company.persons['ext1-person']
    gap_test.test_person = external_person
    gap_test.DEFAULT_MODIFIER_ID = external_person.id
    external_gap = AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(gap_test.get_base_gap(AbsenceWorkflow))

    def create_permission():
        role = DepartmentRoleFactory(id='TEST_ROLE_NAME')
        role.permissions.add(Permission.objects.get(codename='can_view_gaps'))
        DepartmentStaffFactory(staff=external_person, department=inner_person.department, role=role)

    def open_for_self():
        external_person.user.user_permissions.add(Permission.objects.get(codename='can_view_gap_as_external'))

    return dict(
        inner_gap=inner_gap,
        inner_person=inner_person,
        external_person=external_person,
        external_gap=external_gap,
        create_permission=create_permission,
        open_for_self=open_for_self,
    )


@pytest.fixture
def ya_user():
    user = UserFactory()
    user.get_profile = Mock(return_value=StaffFactory())
    return user


@pytest.yield_fixture
def templates_ctl():
    ctl = TemplatesCtl()
    assert ctl.MONGO_COLLECTION == TEMPLATES_MONGO_COLLECTION

    yield ctl

    ctl.recreate_collection()
