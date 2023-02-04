import pytest
from intranet.femida.src.staff.models import Department

from intranet.femida.src.staff.choices import DEPARTMENT_KINDS

from intranet.femida.tests import factories as f

pytestmark = pytest.mark.django_db


def test_department_direction_id_attribute_error():
    department = f.DepartmentFactory()
    with pytest.raises(AttributeError):
        print(department.direction_id)


def test_department_direction_self():
    tree = f.create_departments_tree(5, kind=DEPARTMENT_KINDS.direction)
    leaf = tree[-1]
    direction = leaf

    department = Department.objects.with_direction().get(id=leaf.id)
    assert department.direction_id == direction.id


def test_department_direction_closest():
    directions = f.create_departments_tree(5, kind=DEPARTMENT_KINDS.direction)
    tree = f.create_departments_tree(3, ancestors=[d.id for d in directions])
    leaf = tree[-1]
    direction = directions[-1]

    department = Department.objects.with_direction().get(id=leaf.id)
    assert department.direction_id == direction.id


def test_department_direction_3rd_no_direction():
    tree = f.create_departments_tree(5)
    leaf = tree[-1]
    direction = tree[2]

    department = Department.objects.with_direction().get(id=leaf.id)
    assert department.direction_id == direction.id


def test_department_direction_self_no_direction():
    tree = f.create_departments_tree(2)
    leaf = tree[-1]
    direction = leaf

    department = Department.objects.with_direction().get(id=leaf.id)
    assert department.direction_id == direction.id
