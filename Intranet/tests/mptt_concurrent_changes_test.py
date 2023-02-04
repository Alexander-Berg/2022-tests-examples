import pytest

from staff.departments.models import Department

from staff.lib.models.base import IntranetMpttModel
from staff.lib.testing import DepartmentFactory


def assert_tree_is_correct(dep: Department, prev_val: int = 0):
    assert dep.lft == prev_val + 1, dep.url

    prev_val = dep.lft
    for children in dep.get_children():
        assert_tree_is_correct(children, prev_val)
        prev_val = children.rght

    assert dep.rght == prev_val + 1, dep.url


@pytest.mark.django_db
def test_mptt_on_changing_new_model_instance_inside_one_transaction():
    # given
    assert issubclass(Department, IntranetMpttModel)  # department tree is good choice for our experiments

    root = DepartmentFactory(url='root')
    child1 = DepartmentFactory(parent=root, url='child1')
    child2 = DepartmentFactory(parent=root, url='child2')
    child3 = DepartmentFactory(parent=root, url='child3')
    deep = DepartmentFactory(parent=child1, url='deep')

    #       root
    # 	  /  |  \
    # 	 /	 |   \
    # child1 child2 child3
    #    |
    #  deep

    # when
    dep = Department.objects.get(id=deep.id)
    new_parent = Department.objects.get(id=child2.id)
    dep.parent = new_parent

    new_parent_instance2 = Department.objects.get(id=child2.id)
    new_parent_instance2.parent = child3
    new_parent_instance2.save()

    dep.save()

    #       root
    # 	  /    \
    # 	 /	    \
    # child1   child3
    #            |
    #          child2
    #            |
    #           deep

    # then
    assert_tree_is_correct(Department.objects.get(id=root.id))


@pytest.mark.django_db
def test_mptt_on_changing_old_parent_model_instance_inside_one_transaction():
    # given
    assert issubclass(Department, IntranetMpttModel)  # department tree is good choice for our experiments

    root = DepartmentFactory(url='root')
    child1 = DepartmentFactory(parent=root, url='child1')
    child2 = DepartmentFactory(parent=root, url='child2')
    child3 = DepartmentFactory(parent=root, url='child3')
    deep = DepartmentFactory(parent=child1, url='deep')

    #       root
    # 	  /  |  \
    # 	 /	 |   \
    # child1 child2 child3
    #    |
    #  deep

    # when
    dep = Department.objects.get(id=deep.id)
    new_parent = Department.objects.get(id=child2.id)
    dep.parent = new_parent

    old_parent_instance = Department.objects.get(id=child1.id)
    old_parent_instance.parent = child3
    old_parent_instance.save()

    dep.save()

    #  root
    #   |  \
    #   |   \
    # child2 child3
    #  |       |
    # deep   child1

    # then
    assert_tree_is_correct(Department.objects.get(id=root.id))
