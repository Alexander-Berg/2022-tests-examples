import pytest

from intranet.femida.src.staff.choices import DEPARTMENT_ROLES, DEPARTMENT_KINDS
from intranet.femida.src.staff.helpers import get_hr_partners, get_department_chiefs_chain

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def _create_hr_partner(department, user=None, is_direct=False):
    user = user or f.UserFactory()
    return f.DepartmentUserFactory(
        department=department,
        user=user,
        role=DEPARTMENT_ROLES.hr_partner,
        is_direct=is_direct,
    )


def test_get_hr_partners_straight():
    department = f.DepartmentFactory()
    direct_hr_partners = f.UserFactory.create_batch(5)

    for user in direct_hr_partners:
        _create_hr_partner(department, user, True)

    for i in range(5):
        _create_hr_partner(department)

    hr_partners = get_hr_partners(department)
    sorted_direct_hr_partner_ids = sorted([u.id for u in direct_hr_partners])
    sorted_hr_partner_ids = sorted([u.id for u in hr_partners])

    assert sorted_direct_hr_partner_ids == sorted_hr_partner_ids


def test_get_hr_partners_in_tree():
    departments = f.create_departments_tree(5)
    ancestors, department = departments[:-1], departments[-1]
    direct_hr_partners = f.UserFactory.create_batch(2)

    for user in direct_hr_partners:
        _create_hr_partner(ancestors[1], user, True)

    # Непосредственные hr-партнеры еще выше по дереву
    for i in range(5):
        _create_hr_partner(ancestors[0], is_direct=True)

    for department in departments:
        _create_hr_partner(department)

    hr_partners = get_hr_partners(department)
    sorted_direct_hr_partner_ids = sorted([u.id for u in direct_hr_partners])
    sorted_hr_partner_ids = sorted([u.id for u in hr_partners])

    assert sorted_direct_hr_partner_ids == sorted_hr_partner_ids


def test_get_hr_partners_without_partners():
    department = f.DepartmentFactory()
    hr_partners = get_hr_partners(department)
    assert not hr_partners


def test_get_department_chiefs_chain():
    # список из 5 подразделений в одной ветке от младшего к старшему
    departments = f.create_departments_tree(5)[::-1]
    expected = [f.create_department_chief(d).user for d in departments]
    result = get_department_chiefs_chain(departments[0])
    assert expected == result


def test_get_department_chiefs_chain_direction_only():
    # список из 5 подразделений в одной ветке от младшего к старшему
    departments = f.create_departments_tree(5)[::-1]
    direction_idx = 2
    direction = departments[direction_idx]
    direction.kind = DEPARTMENT_KINDS.direction
    direction.save()
    expected = [f.create_department_chief(d).user for d in departments][:direction_idx + 1]
    result = get_department_chiefs_chain(departments[0], from_current_direction_only=True)
    assert expected == result
