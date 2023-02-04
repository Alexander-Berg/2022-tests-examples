import pytest
from pretend import stub

from plan.services.operations import prepare

PersonStub = stub
RoleStub = stub
DepartmentStub = stub


new_members_data = [
    {
        'person': PersonStub(id=10),
        'role': RoleStub(id=1, code=None),
        'from_department': DepartmentStub(id=1),
    },
    {
        'person': PersonStub(id=20),
        'role': RoleStub(id=1, code=None),
        'from_department': DepartmentStub(id=1),
    },
]


def test_service_add_many_prepare_new_members():
    new_members = prepare.prepare_new_members(
        data=stub(new_members_data=new_members_data),
        prepared=None
    )

    assert len(new_members) == 2

    first = new_members[0]
    assert first.person.id == 10
    assert first.role.id == 1
    assert first.from_department.id == 1


@pytest.mark.django_db
def test_service_add_many_prepare_present_members(service_member):
    present_members = prepare.prepare_present_members(
        data=stub(service=service_member.service),
        prepared=None
    )

    assert len(present_members) == 1
    assert present_members[0] == service_member


def test_service_add_many_prepare_new_departments():
    new_departments = prepare.prepare_new_departments(
        data=stub(new_members_data=new_members_data),
        prepared=None
    )

    assert len(new_departments) == 1
    assert (1, 1) in new_departments


# prepare_present_departments is not ready
