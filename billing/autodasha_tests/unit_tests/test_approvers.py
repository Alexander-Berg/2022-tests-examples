# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock

from balance import muzzle_util as ut

from autodasha.core.logic.approvers import ApproversManager, TopSummoneesException, SummoneesNotFoundException

from tests.autodasha_tests.common import staff_utils


def create_manager(staff, gap, dept_rows=None, appr_rows=None, solver='default'):
    settings = staff_utils.create_approvers_settings(dept_rows or [], appr_rows or [])

    config = ut.Struct(
        top_departments=['top'],
        common_approve_departments=['common1', 'common2'],
        approvers_settings=settings,
        absence_check_steps=2
    )

    staff_path = 'autodasha.core.api.staff.Staff.__init__'
    gap_path = 'autodasha.core.api.gap.Gap.__init__'
    with mock.patch(staff_path, lambda *args: None), mock.patch(gap_path, lambda *args: None):
        manager = ApproversManager(solver, config)

    manager._staff = staff
    manager._gap = gap

    return manager


@pytest.fixture()
def departments():
    common1_chief = staff_utils.Person('common1_chief')
    common1_deputy = staff_utils.Person('common1_deputy')

    common2_chief = staff_utils.Person('common2_chief')
    common2_deputy = staff_utils.Person('common2_deputy')

    top_chief = staff_utils.Person('top_chief')
    yandex_chief = staff_utils.Person('yandex_chief')

    common1 = staff_utils.Department('common1', [common1_chief], [common1_deputy], [], [])
    common2 = staff_utils.Department('common2', [common2_chief], [common2_deputy], [], [])
    top = staff_utils.Department('top', [top_chief], [], [], [common1, common2])
    yandex = staff_utils.Department('yandex', [yandex_chief], [], [], [top])

    return yandex, top, common1, common2


@pytest.fixture()
def yandex(departments):
    return departments[0]


@pytest.fixture()
def top_department(departments):
    return departments[1]


@pytest.fixture()
def common1(departments):
    return departments[2]


@pytest.fixture()
def common2(departments):
    return departments[3]


class TestSummonees(object):
    def test_member_direct_deputy_with_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.summonees(person, check_absence=False) == {'deputy'}

    def test_member_direct_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [], [deputy], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.summonees(person, check_absence=False) == {'deputy'}

    def test_member_indirect_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        dept = staff_utils.Department('dept', [], [], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_chief_direct_chief_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        dept = staff_utils.Department('dept', [member], [], [])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_deputy_direct_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [member], [])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_dept_add(self, yandex, common1):
        member = staff_utils.Person('member')
        approver1 = staff_utils.Person('approver1')
        approver2 = staff_utils.Person('approver2')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver1, approver2])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver1', 'a', 0),
            staff_utils.DepartmentSettings('default', 'dept', 'approver2', 'a', 0),
        ])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver1', 'approver2'}

    def test_dept_add_wo_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        dept = staff_utils.Department('dept', [], [], [member, approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'a', 0)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_dept_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        approver1 = staff_utils.Person('approver1')
        approver2 = staff_utils.Person('approver2')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver1, approver2])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver1', 'r', 0),
            staff_utils.DepartmentSettings('default', 'dept', 'approver2', 'r', 0),
        ])
        assert manager.summonees(person, check_absence=False) == {'approver1', 'approver2'}

    def test_dept_replace_wo_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        dept = staff_utils.Department('dept', [], [], [member, approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_dept_hier_add(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'approver', 'a', 1)])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver'}

    def test_dept_hier_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'approver', 'r', 1)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_dept_hier_add_w_add(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        h_approver = staff_utils.Person('h_approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver, h_approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver', 'a', 0),
            staff_utils.DepartmentSettings('default', 'common1', 'h_approver', 'a', 1)
        ])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver', 'h_approver'}

    def test_dept_hier_add_w_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        h_approver = staff_utils.Person('h_approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver, h_approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver', 'r', 0),
            staff_utils.DepartmentSettings('default', 'common1', 'h_approver', 'a', 1)
        ])
        assert manager.summonees(person, check_absence=False) == {'approver', 'h_approver'}

    def test_dept_hier_replace_w_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        h_approver = staff_utils.Person('h_approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver, h_approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver', 'r', 1),
            staff_utils.DepartmentSettings('default', 'common1', 'h_approver', 'r', 1)
        ])
        assert manager.summonees(person, check_absence=False) == {'approver', 'h_approver'}

    def test_dept_hier_replace_w_add(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        h_approver = staff_utils.Person('h_approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver, h_approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'approver', 'a', 0),
            staff_utils.DepartmentSettings('default', 'common1', 'h_approver', 'r', 1)
        ])
        assert manager.summonees(person, check_absence=False) == {'approver', 'h_approver'}

    def test_dept_hier_add_w_hier_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        h_approver = staff_utils.Person('h_approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver, h_approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'common1', 'approver', 'r', 1),
            staff_utils.DepartmentSettings('default', 'common1', 'h_approver', 'a', 1)
        ])
        assert manager.summonees(person, check_absence=False) == {'approver', 'h_approver'}

    def test_person_add_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        approver1 = staff_utils.Person('approver1')
        approver2 = staff_utils.Person('approver2')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver1, approver2])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'chief', 'approver1', 0),
            staff_utils.PersonSettings('default', 'chief', 'approver2', 0),
        ])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver1', 'approver2'}

    def test_person_replace_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        approver1 = staff_utils.Person('approver1')
        approver2 = staff_utils.Person('approver2')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member, approver1, approver2])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'chief', 'approver1', 1),
            staff_utils.PersonSettings('default', 'chief', 'approver2', 0),
        ])
        assert manager.summonees(person, check_absence=False) == {'approver1', 'approver2'}

    def test_person_add_deputy_w_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        approver1 = staff_utils.Person('approver1')
        approver2 = staff_utils.Person('approver2')
        deputy = staff_utils.Person('deputy')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, approver1, approver2])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'deputy', 'approver1', 0),
            staff_utils.PersonSettings('default', 'deputy', 'approver2', 0),
        ])
        assert manager.summonees(person, check_absence=False) == {'approver1', 'approver2', 'deputy'}

    def test_person_add_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member])

        common1.childs = [dept]
        common1.members.append(approver)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'common1_chief', 'approver', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_dept_add_for_chief(self, yandex, common1):
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('chief')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'a', 0)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_dept_add_for_deputy(self, yandex, common1):
        deputy = staff_utils.Person('deputy')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [deputy], [approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('deputy')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'a', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver'}

    def test_dept_replace_for_chief(self, yandex, common1):
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('chief')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_dept_replace_for_deputy(self, yandex, common1):
        deputy = staff_utils.Person('deputy')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [deputy], [approver])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('deputy')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'approver', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_person_add_for_chief(self, yandex, common1):
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [approver])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('chief')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'approver', 0)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_person_add_for_deputy(self, yandex, common1):
        deputy = staff_utils.Person('deputy')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [deputy], [approver])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('deputy')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'approver', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief', 'approver'}

    def test_person_replace_for_chief(self, yandex, common1):
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [approver])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('chief')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'approver', 1)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_person_replace_for_deputy(self, yandex, common1):
        deputy = staff_utils.Person('deputy')
        approver = staff_utils.Person('approver')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [deputy], [approver])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('deputy')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'approver', 1)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_dept_add_self_direct(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'member', 'a', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_dept_replace_self_direct(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'member', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_dept_add_self_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdept = staff_utils.Department('subdept', [subchief], [], [member])

        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'member', 'a', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_dept_replace_self_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdept = staff_utils.Department('subdept', [subchief], [], [member])

        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'member', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_person_add_self_direct(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'member', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_person_replace_self_direct(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [member])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'member', 1)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_person_add_self_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdept = staff_utils.Department('subdept', [subchief], [], [member])

        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'member', 0)])
        assert manager.summonees(person, check_absence=False) == {'chief'}

    def test_person_replace_self_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdept = staff_utils.Department('subdept', [subchief], [], [member])

        chief = staff_utils.Person('chief')
        dept = staff_utils.Department('dept', [chief], [], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'chief', 'member', 1)])
        assert manager.summonees(person, check_absence=False) == {'common1_deputy'}

    def test_person_add_self_indirect_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdept = staff_utils.Department('subdept', [subchief], [], [member])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'deputy', 'member', 0)])
        assert manager.summonees(person, check_absence=False) == {'deputy'}

    def test_top(self, yandex, top_department):
        member = staff_utils.Person('member')
        dept = staff_utils.Department('dept', [], [], [member])
        top_department.childs.append(dept)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        with pytest.raises(TopSummoneesException):
            manager.summonees(person, check_absence=False)

    def test_top_dept_replace(self, yandex, top_department):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        dept = staff_utils.Department('dept', [], [], [member, approver])
        top_department.childs.append(dept)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'top', 'approver', 'r', 0)])
        assert manager.summonees(person, check_absence=False) == {'approver'}

    def test_top_person_replace(self, yandex, top_department):
        member = staff_utils.Person('member')
        approver = staff_utils.Person('approver')
        dept = staff_utils.Department('dept', [], [], [member, approver])
        top_department.childs.append(dept)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', 'top_chief', 'approver', 1)])
        with pytest.raises(TopSummoneesException):
            manager.summonees(person, check_absence=False)


class TestDirectApprovers(object):

    @pytest.mark.parametrize(['login', 'approvers'], [
        ('member', {'chief', 'deputy'}),
        ('deputy', {'chief'}),
        ('chief', set()),
    ], ids=['member', 'deputy', 'chief'])
    def test_base(self, yandex, common1, login, approvers):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info(login)

        manager = create_manager(staff, None)
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        } | approvers
        assert manager.direct_approvers(person) == req_approvers

    def test_indirect(self, yandex, common1):
        member = staff_utils.Person('member')
        dept = staff_utils.Department('dept', [], [], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        }
        assert manager.direct_approvers(person) == req_approvers

    def test_with_childs(self, yandex, common1):
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('common1_deputy')

        manager = create_manager(staff, None)
        req_approvers = {
            'common1_chief',
            'top_chief',
            'yandex_chief'
        }
        assert manager.direct_approvers(person) == req_approvers

    @pytest.mark.parametrize(['login', 'mod_type', 'approvers'], [
        ('member', 'a', {'chief', 'deputy', 'add'}),
        ('member', 'r', {'chief', 'deputy', 'add'}),
        ('deputy', 'a', {'chief', 'add'}),
        ('deputy', 'r', {'chief', 'add'}),
        ('chief', 'a', set()),
        ('chief', 'r', set()),
        ('add', 'a', {'chief'}),
        ('add', 'r', {'chief'}),
    ], ids=lambda v: '*' if isinstance(v, set) else v)
    def test_dept_modify(self, yandex, common1, login, mod_type, approvers):
        add = staff_utils.Person('add')
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info(login)

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'add', mod_type, 0)])
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        } | approvers
        assert manager.direct_approvers(person) == req_approvers

    @pytest.mark.parametrize(['login', 'mod_login', 'mod_type', 'approvers'], [
        ('member', 'chief', 0, {'chief', 'deputy', 'add'}),
        ('member', 'chief', 1, {'chief', 'deputy', 'add'}),
        ('member', 'deputy', 0, {'chief', 'deputy', 'add'}),
        ('member', 'deputy', 1, {'chief', 'deputy', 'add'}),
        ('deputy', 'chief', 0, {'chief', 'add'}),
        ('deputy', 'chief', 1, {'chief', 'add'}),
        ('deputy', 'deputy', 0, {'chief', 'add'}),
        ('deputy', 'deputy', 1, {'chief', 'add'}),
        ('chief', 'deputy', 0, set()),
        ('chief', 'deputy', 1, set()),
        ('chief', 'chief', 0, set()),
        ('chief', 'chief', 1, set()),
        ('add', 'deputy', 0, {'chief', 'deputy'}),
        ('add', 'deputy', 1, {'chief', 'deputy'}),
        ('add', 'chief', 0, {'chief'}),
        ('add', 'chief', 1, {'chief'}),
    ], ids=lambda v: '*' if isinstance(v, set) else unicode(v))
    def test_person_modify(self, yandex, common1, login, mod_login, mod_type, approvers):
        add = staff_utils.Person('add')
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info(login)

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', mod_login, 'add', mod_type)])
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        } | approvers
        assert manager.direct_approvers(person) == req_approvers

    @pytest.mark.parametrize(['mod_type'], ['a', 'r'])
    def test_self_dept_modify_indirect(self, yandex, common1, mod_type):
        add = staff_utils.Person('add')

        subchief = staff_utils.Person('subchief')
        subdeputy = staff_utils.Person('subdeputy')
        subdept = staff_utils.Department('subdept', [subchief], [subdeputy], [add])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('add')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'add', mod_type, 0)])
        req_approvers = {
            'chief',
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        }
        assert manager.direct_approvers(person) == req_approvers

    @pytest.mark.parametrize(['mod_login', 'mod_type', 'approvers'], [
        ('chief', 0, {'chief'}),
        ('chief', 1, {'chief'}),
        ('deputy', 0, {'chief', 'deputy'}),
        ('deputy', 1, {'chief', 'deputy'}),
    ], ids=lambda v: '*' if isinstance(v, set) else unicode(v))
    def test_self_person_modify_indirect(self, yandex, common1, mod_login, mod_type, approvers):
        add = staff_utils.Person('add')

        subchief = staff_utils.Person('subchief')
        subdeputy = staff_utils.Person('subdeputy')
        subdept = staff_utils.Department('subdept', [subchief], [subdeputy], [add])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])

        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('add')

        manager = create_manager(staff, None, [], [staff_utils.PersonSettings('default', mod_login, 'add', mod_type)])
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        } | approvers
        assert manager.direct_approvers(person) == req_approvers

    def test_self_with_add_person_deputy(self, yandex, common1):
        add = staff_utils.Person('add')
        deputy_add = staff_utils.Person('deputy_add')
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add, deputy_add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('add')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'chief', 'add', 0),
            staff_utils.PersonSettings('default', 'deputy', 'deputy_add', 0),
        ])
        req_approvers = {
            'chief',
            'common1_chief',
            'common1_deputy',
            'top_chief',
            'yandex_chief'
        }
        assert manager.direct_approvers(person) == req_approvers


class TestDepartmentApprovers(object):
    def test_in_common(self, yandex, common1, common2):
        add1 = staff_utils.Person('add1')
        chief = staff_utils.Person('chief')
        dept1 = staff_utils.Department('dept1', [chief], [], [add1])

        member = staff_utils.Person('member')
        add2 = staff_utils.Person('add2')
        add3 = staff_utils.Person('add3')
        deputy = staff_utils.Person('deputy')
        dept2 = staff_utils.Department('dept2', [], [deputy], [member, add2, add3])

        add4 = staff_utils.Person('add4')
        dept3 = staff_utils.Department('dept3', [], [], [add4])

        other_chief = staff_utils.Person('other_chief')
        other_deputy = staff_utils.Person('other_deputy')
        other_dept = staff_utils.Department('other_dept', [other_chief], [other_deputy], [])

        common1.childs = [dept1, dept2, dept3]
        common2.childs = [other_dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept1', 'add1', 'r', 0),
            staff_utils.DepartmentSettings('default', 'dept3', 'add4', 'a', 0),
            staff_utils.DepartmentSettings('default', 'top', 'add2', 'r', 0)
        ], [
            staff_utils.PersonSettings('default', 'deputy', 'add3', 1)
        ])
        req_approvers = {
            'common1_chief',
            'common1_deputy',
            'deputy',
            'chief',
            'top_chief',
            'yandex_chief',
            'add1',
            'add2',
            'add3',
            'add4'
        }
        assert manager.department_approvers(person) == req_approvers

    def test_wo_common(self, yandex, top_department, common1):
        add1 = staff_utils.Person('add1')
        add2 = staff_utils.Person('add2')
        add3 = staff_utils.Person('add3')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept1', [chief], [deputy], [add1, add2, add3])

        member = staff_utils.Person('member')

        common1.childs = [dept]
        top_department.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [
            staff_utils.DepartmentSettings('default', 'dept', 'add', 'r', 0),
            staff_utils.DepartmentSettings('default', 'dept', 'add2', 'a', 0),
        ], [
            staff_utils.PersonSettings('default', 'deputy', 'add3', 0)
        ])
        req_approvers = {
            'top_chief',
            'yandex_chief'
        }
        assert manager.department_approvers(person) == req_approvers


class TestIsApprover(object):

    def test_member(self, yandex, common1):
        member = staff_utils.Person('member')
        common1.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None)
        assert manager.is_approver(person) is False

    def test_chief(self, yandex):
        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('common1_chief')

        manager = create_manager(staff, None)
        assert manager.is_approver(person) is True

    def test_deputy(self, yandex):
        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('common1_deputy')

        manager = create_manager(staff, None)
        assert manager.is_approver(person) is True

    def test_dept_add(self, yandex, common1):
        member = staff_utils.Person('member')
        common1.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'member', 'a', 0)])
        assert manager.is_approver(person) is True

    def test_dept_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        common1.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'member', 'r', 0)])
        assert manager.is_approver(person) is True

    def test_dept_add_hierarchy(self, yandex, common1):
        member = staff_utils.Person('member')
        common1.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'member', 'a', 1)])
        assert manager.is_approver(person) is True

    def test_dept_replace_hierarchy(self, yandex, common1):
        member = staff_utils.Person('member')
        common1.members.append(member)

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'common1', 'member', 'r', 1)])
        assert manager.is_approver(person) is True

    def test_dept_add_wo_heads(self, yandex, common1):
        member = staff_utils.Person('member')
        dept = staff_utils.Department('dept', [], [], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        manager = create_manager(staff, None, [staff_utils.DepartmentSettings('default', 'dept', 'member', 'a', 0)])
        assert manager.is_approver(person) is True

    def test_person_add(self, yandex, common1):
        member1 = staff_utils.Person('member1')
        member2 = staff_utils.Person('member2')
        common1.members.append(member1)
        common1.members.append(member2)

        staff = staff_utils.StaffMock(yandex)
        person1 = staff.get_person_info('member1')
        person2 = staff.get_person_info('member2')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'common1_chief', 'member1', 0),
            staff_utils.PersonSettings('default', 'common1_chief', 'member2', 0),
        ])
        assert manager.is_approver(person1) is True
        assert manager.is_approver(person2) is True

    def test_person_replace(self, yandex, common1):
        member1 = staff_utils.Person('member1')
        member2 = staff_utils.Person('member2')
        common1.members.append(member1)
        common1.members.append(member2)

        staff = staff_utils.StaffMock(yandex)
        person1 = staff.get_person_info('member1')
        person2 = staff.get_person_info('member2')

        manager = create_manager(staff, None, [], [
            staff_utils.PersonSettings('default', 'common1_chief', 'member1', 1),
            staff_utils.PersonSettings('default', 'common1_chief', 'member2', 1),
        ])
        assert manager.is_approver(person1) is True
        assert manager.is_approver(person2) is True


class TestAbsentSummonees(object):
    def test_abs_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap)
        assert manager.summonees(person, check_absence=True) == {'deputy'}

    def test_abs_chief_w_deputy_steps(self, yandex, common1):
        member = staff_utils.Person('member')
        subchief = staff_utils.Person('subchief')
        subdeputy = staff_utils.Person('subdeputy')
        subdept = staff_utils.Department('subdept', [subchief], [subdeputy], [member])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([
            staff_utils.PersonGap('subchief'),
            staff_utils.PersonGap('subdeputy'),
            staff_utils.PersonGap('chief'),
            staff_utils.PersonGap('deputy'),
        ])

        manager = create_manager(staff, gap)
        assert manager.summonees(person, check_absence=True) == {'common1_deputy'}

    def test_empty_dept_steps(self, yandex, common1):
        member = staff_utils.Person('member')

        subsubdept = staff_utils.Department('subsubdept', [], [], [member])

        subchief = staff_utils.Person('subchief')
        subdeputy = staff_utils.Person('subdeputy')
        subdept = staff_utils.Department('subdept', [subchief], [subdeputy], [], [subsubdept])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([
            staff_utils.PersonGap('subsubchief'),
            staff_utils.PersonGap('subsubdeputy'),
            staff_utils.PersonGap('subchief'),
            staff_utils.PersonGap('subdeputy'),
            staff_utils.PersonGap('chief'),
            staff_utils.PersonGap('deputy'),
        ])

        manager = create_manager(staff, gap)
        assert manager.summonees(person, check_absence=True) == {'common1_deputy'}

    def test_max_steps(self, yandex, common1):
        member = staff_utils.Person('member')

        subsubchief = staff_utils.Person('subsubchief')
        subsubdeputy = staff_utils.Person('subsubdeputy')
        subsubdept = staff_utils.Department('subsubdept', [subsubchief], [subsubdeputy], [member])

        subchief = staff_utils.Person('subchief')
        subdeputy = staff_utils.Person('subdeputy')
        subdept = staff_utils.Department('subdept', [subchief], [subdeputy], [], [subsubdept])

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [], [subdept])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([
            staff_utils.PersonGap('subsubchief'),
            staff_utils.PersonGap('subsubdeputy'),
            staff_utils.PersonGap('subchief'),
            staff_utils.PersonGap('subdeputy'),
            staff_utils.PersonGap('chief'),
            staff_utils.PersonGap('deputy'),
        ])

        manager = create_manager(staff, gap)
        with pytest.raises(SummoneesNotFoundException):
            manager.summonees(person, check_absence=True)

    def test_top_depts(self, yandex, common1):
        member = staff_utils.Person('member')

        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([
            staff_utils.PersonGap('chief'),
            staff_utils.PersonGap('deputy'),
            staff_utils.PersonGap('common1_chief'),
            staff_utils.PersonGap('common1_deputy'),
        ])

        manager = create_manager(staff, gap)
        with pytest.raises(TopSummoneesException):
            manager.summonees(person, check_absence=True)

    def test_abs_chief_dept_add(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief'), staff_utils.PersonGap('deputy')])

        manager = create_manager(staff, gap, [staff_utils.DepartmentSettings('default', 'dept', 'add', 'a', 0)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_chief_dept_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [staff_utils.DepartmentSettings('default', 'dept', 'add', 'r', 0)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_chief_person_add_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('deputy'), staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'chief', 'add', 0)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_chief_person_replace_chief(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('deputy'), staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'chief', 'add', 1)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_chief_person_replace_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('deputy')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'chief', 'add', 1)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_add_dept_add(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('add')])

        manager = create_manager(staff, gap, [staff_utils.DepartmentSettings('default', 'dept', 'add', 'a', 0)])
        assert manager.summonees(person, check_absence=True) == {'deputy'}

    def test_abs_add_dept_replace(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('add')])

        manager = create_manager(staff, gap, [staff_utils.DepartmentSettings('default', 'dept', 'add', 'r', 0)])
        assert manager.summonees(person, check_absence=True) == {'deputy'}

    def test_abs_chief_person_add_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'deputy', 'add', 0)])
        assert manager.summonees(person, check_absence=True) == {'deputy', 'add'}

    def test_abs_chief_person_replace_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [member, add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'deputy', 'add', 1)])
        assert manager.summonees(person, check_absence=True) == {'add'}

    def test_abs_chief_self_deputy(self, yandex, common1):
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('deputy')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'deputy', 'add', 0)])
        assert manager.summonees(person, check_absence=True) == {'common1_deputy'}

    def test_abs_chief_self_add_deputy(self, yandex, common1):
        add = staff_utils.Person('add')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [add])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('add')

        gap = staff_utils.GapMock([staff_utils.PersonGap('chief')])

        manager = create_manager(staff, gap, [], [staff_utils.PersonSettings('default', 'deputy', 'add', 0)])
        assert manager.summonees(person, check_absence=True) == {'deputy'}

    def test_abs_chief_w_add_person_add_deputy(self, yandex, common1):
        member = staff_utils.Person('member')
        add1 = staff_utils.Person('add1')
        add2 = staff_utils.Person('add2')
        chief = staff_utils.Person('chief')
        deputy = staff_utils.Person('deputy')
        dept = staff_utils.Department('dept', [chief], [deputy], [add1, add2, member])
        common1.childs = [dept]

        staff = staff_utils.StaffMock(yandex)
        person = staff.get_person_info('member')

        gap = staff_utils.GapMock([
            staff_utils.PersonGap('chief'),
            staff_utils.PersonGap('add1')
        ])

        manager = create_manager(staff, gap, [], [
            staff_utils.PersonSettings('default', 'chief', 'add1', 0),
            staff_utils.PersonSettings('default', 'deputy', 'add2', 0),
        ])
        assert manager.summonees(person, check_absence=True) == {'deputy', 'add2'}
