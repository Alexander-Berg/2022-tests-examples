from pretend import stub

from plan.services.operations import validate

PersonStub = stub
RoleStub = stub
DepartmentStub = stub
MemberStub = stub
ServiceStub = stub


def test_validate_everyone_linked_from_only_department_all_ok():
    members = [
        {
            'person': PersonStub(id=1),
            'from_department': None,
        },
        {
            'person': PersonStub(id=1),
            'from_department': DepartmentStub(id=10),
        }
    ]
    data = stub(new_members_data=members)

    err_data = validate.validate_everyone_linked_from_only_department(
        data=data)

    assert err_data is None


def test_validate_everyone_linked_from_only_department_dpt_duplicate():
    members = [
        {
            'person': PersonStub(id=1),
            'from_department': DepartmentStub(id=10),
        },
        {
            'person': PersonStub(id=1),
            'from_department': DepartmentStub(id=10),
        }
    ]

    data = stub(new_members_data=members)
    err_data = validate.validate_everyone_linked_from_only_department(
        data=data)

    assert 'departments' in err_data
    assert err_data['departments'] == [10]


def test_validate_everyone_linked_from_only_department_one_person_two_dpts():
    members = [
        {
            'person': PersonStub(id=1),
            'from_department': DepartmentStub(id=20),
        },
        {
            'person': PersonStub(id=1),
            'from_department': DepartmentStub(id=10),
        }
    ]

    data = stub(new_members_data=members)
    err_data = validate.validate_everyone_linked_from_only_department(
        data=data)

    assert 'departments' in err_data
    assert err_data['departments'] == [10]


def test_validate_departments_already_linked_no_errors():
    data = stub(new_departments=[1, 2, 3], present_departments=[5, 6])

    err_data = validate.validate_departments_already_linked(data)

    assert err_data is None


def test_validate_departments_already_linked_errors():
    data = stub(new_departments=[1, 2, 3], present_departments=[1, 2, 6])

    err_data = validate.validate_departments_already_linked(data)

    assert err_data is not None
    assert 'departments' in err_data
    assert sorted(err_data['departments']) == [1, 2]
