from decimal import Decimal

import pytest
from staff.headcounts.models import AllowedHeadcountOverdraft

from staff.departments.models import Department

from staff.departments.controllers.department import DepartmentCtl
from staff.lib.testing import DepartmentFactory, StaffFactory


@pytest.mark.django_db
def test_department_ctl_applies_overdraft_changes():
    author = StaffFactory()
    dep = DepartmentFactory(
        name='dep_name',
        name_en='dep_name_en',
        parent=None,
    )

    ctl = DepartmentCtl(dep, author.user)
    ctl.allowed_overdraft_percents = '10.3'
    ctl.save()

    dep = Department.objects.get(id=dep.id)
    assert dep.allowedheadcountoverdraft.percents_with_child_departments == Decimal('10.3')


@pytest.mark.django_db
def test_department_ctl_applies_overdraft_reset():
    author = StaffFactory()
    dep = DepartmentFactory(
        name='dep_name',
        name_en='dep_name_en',
        parent=None,
    )

    dep.allowedheadcountoverdraft = AllowedHeadcountOverdraft.objects.create(
        department_id=dep.id,
        percents_with_child_departments=Decimal('10.3'),
    )

    dep.save()

    ctl = DepartmentCtl(dep, author.user)
    ctl.allowed_overdraft_percents = None
    ctl.save()

    dep = Department.objects.get(id=dep.id)
    assert dep.allowedheadcountoverdraft.percents_with_child_departments is None
