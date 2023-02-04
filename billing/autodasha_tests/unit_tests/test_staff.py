# -*- coding: utf-8 -*-

import pytest

from tests.autodasha_tests.common import staff_utils


@pytest.fixture(scope='session')
def staff_api():
    gr1_m = staff_utils.Person('gr1_m')
    gr1_d = staff_utils.Person('gr1_d')
    gr1_c = staff_utils.Person('gr1_c')

    gr2_m = staff_utils.Person('gr2_m')
    gr2_d = staff_utils.Person('gr2_d')
    gr2_c = staff_utils.Person('gr2_c')

    gr3_m = staff_utils.Person('gr3_m')
    gr3_d = staff_utils.Person('gr3_d')

    sg_m = staff_utils.Person('sg_m')

    d_m = staff_utils.Person('d_m')
    d_d = staff_utils.Person('d_d')
    d_c = staff_utils.Person('d_c')

    y_m = staff_utils.Person('y_m')
    y_d = staff_utils.Person('y_d')
    y_c = staff_utils.Person('y_c')

    group1 = staff_utils.Department('group1', [gr1_c], [gr1_d], [gr1_m])
    group2 = staff_utils.Department('group2', [gr2_c], [gr2_d], [gr2_m])
    subgroup = staff_utils.Department('subgroup', [], [], [sg_m])
    group3 = staff_utils.Department('group3', [], [gr3_d], [gr3_m], [subgroup])
    dept = staff_utils.Department('dept', [d_c], [d_d], [d_m], [group1, group2])
    yandex = staff_utils.Department('yandex', [y_c], [y_d], [y_m], [dept, group3])

    return staff_utils.StaffMock(yandex)


def test_login(staff_api):
    person = staff_api.get_person_info('gr1_m')
    assert person.login == 'gr1_m'


def test_email(staff_api):
    person = staff_api.get_person_info('gr2_m')
    assert person.email == 'gr2_m@y-t.ru'


def test_person_department_name(staff_api):
    person = staff_api.get_person_info('gr2_m')
    assert person.department_name == 'Department group2'


def test_department(staff_api):
    person = staff_api.get_person_info('gr2_m')
    assert person.department == 'group2'


def test_department_wo_chief(staff_api):
    person = staff_api.get_person_info('gr3_m')
    assert person.department == 'group3'


@pytest.mark.parametrize(['login', 'req_is_chief'], [
    ('gr2_m', False),
    ('gr2_d', False),
    ('d_c', True),
])
def test_is_chief(staff_api, login, req_is_chief):
    person = staff_api.get_person_info(login)
    assert person.is_chief is req_is_chief


@pytest.mark.parametrize(['login', 'req_is_deputy'], [
    ('gr2_m', False),
    ('gr2_d', True),
    ('d_c', False),
])
def test_is_deputy(staff_api, login, req_is_deputy):
    person = staff_api.get_person_info(login)
    assert person.is_deputy is req_is_deputy


def test_chief(staff_api):
    person = staff_api.get_person_info('gr2_m')
    assert person.chief == 'gr2_c'


@pytest.mark.parametrize(['login', 'req_dpt', 'req_chief'], [
    ('gr2_m', 'group2', 'gr2_c'),
    ('gr2_d', 'group2', 'gr2_c'),
    ('gr2_c', 'dept', 'd_c'),
    ('gr3_m', 'yandex', 'y_c'),
    ('gr3_d', 'yandex', 'y_c'),
    ('sg_m', 'yandex', 'y_c'),
])
def test_chief_department(staff_api, login, req_dpt, req_chief):
    person = staff_api.get_person_info(login)
    assert person.chief_w_department == (req_dpt, req_chief)


def test_member_hierarchy(staff_api):
    person = staff_api.get_person_info('gr2_m')

    assert list(person.chiefs_hierarchy) == [
        {'id': 'group2', 'chiefs': ['gr2_c'], 'deputies': ['gr2_d']},
        {'id': 'dept', 'chiefs': ['d_c'], 'deputies': ['d_d']},
        {'id': 'yandex', 'chiefs': ['y_c'], 'deputies': ['y_d']},
    ]


def test_deputy_hierarchy(staff_api):
    person = staff_api.get_person_info('gr2_d')

    assert list(person.chiefs_hierarchy) == [
        {'id': 'group2', 'chiefs': ['gr2_c'], 'deputies': ['gr2_d']},
        {'id': 'dept', 'chiefs': ['d_c'], 'deputies': ['d_d']},
        {'id': 'yandex', 'chiefs': ['y_c'], 'deputies': ['y_d']},
    ]


def test_chief_hierarchy(staff_api):
    person = staff_api.get_person_info('d_c')

    assert list(person.chiefs_hierarchy) == [
        {'id': 'dept', 'chiefs': ['d_c'], 'deputies': ['d_d']},
        {'id': 'yandex', 'chiefs': ['y_c'], 'deputies': ['y_d']},
    ]


def test_wo_chief_hierarchy(staff_api):
    person = staff_api.get_person_info('sg_m')

    assert list(person.chiefs_hierarchy) == [
        {'id': 'subgroup', 'chiefs': [], 'deputies': []},
        {'id': 'group3', 'chiefs': [], 'deputies': ['gr3_d']},
        {'id': 'yandex', 'chiefs': ['y_c'], 'deputies': ['y_d']},
    ]


def test_department_url(staff_api):
    dept = staff_api.get_department_info('yandex')
    assert dept.url == 'yandex'


def test_department_name(staff_api):
    dept = staff_api.get_department_info('yandex')
    assert dept.name == 'Department yandex'


def test_department_members(staff_api):
    dept = staff_api.get_department_info('dept')
    assert set(dept.members) == {'d_m', 'd_d', 'd_c'}


def test_department_chiefs(staff_api):
    dept = staff_api.get_department_info('dept')
    assert dept.chiefs == ['d_c']


def test_department_deputies(staff_api):
    dept = staff_api.get_department_info('dept')
    assert dept.deputies == ['d_d']


def test_department_heads(staff_api):
    dept = staff_api.get_department_info('dept')
    assert set(dept.heads) == {'d_d', 'd_c'}


def test_department_hierarchy_members(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {'d_m', 'd_d', 'd_c', 'gr1_m', 'gr1_d', 'gr1_c', 'gr2_m', 'gr2_d', 'gr2_c'}
    assert set(dept.hierarchy_members) == req_res


def test_department_hierarchy_chiefs(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {'d_c', 'gr1_c', 'gr2_c'}
    assert set(dept.hierarchy_chiefs) == req_res


def test_department_hierarchy_deputies(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {'d_d', 'gr1_d', 'gr2_d'}
    assert set(dept.hierarchy_deputies) == req_res


def test_department_hierarchy_heads(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {'d_d', 'd_c', 'gr1_d', 'gr1_c', 'gr2_d', 'gr2_c'}
    assert set(dept.hierarchy_heads) == req_res


def test_department_hierarchy_members_grouped(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {
        ('dept', frozenset(['d_m', 'd_d', 'd_c'])),
        ('group1', frozenset(['gr1_m', 'gr1_d', 'gr1_c'])),
        ('group2', frozenset(['gr2_m', 'gr2_d', 'gr2_c'])),
    }
    res = {(d, frozenset(ps)) for d, ps in dept.hierarchy_members_grouped}
    assert res == req_res


def test_department_hierarchy_chiefs_grouped(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {
        ('dept', frozenset(['d_c'])),
        ('group1', frozenset(['gr1_c'])),
        ('group2', frozenset(['gr2_c'])),
    }
    res = {(d, frozenset(ps)) for d, ps in dept.hierarchy_chiefs_grouped}
    assert res == req_res


def test_department_hierarchy_deputies_grouped(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {
        ('dept', frozenset(['d_d'])),
        ('group1', frozenset(['gr1_d'])),
        ('group2', frozenset(['gr2_d'])),
    }
    res = {(d, frozenset(ps)) for d, ps in dept.hierarchy_deputies_grouped}
    assert res == req_res


def test_department_hierarchy_heads_grouped(staff_api):
    dept = staff_api.get_department_info('dept')
    req_res = {
        ('dept', frozenset(['d_d', 'd_c'])),
        ('group1', frozenset(['gr1_d', 'gr1_c'])),
        ('group2', frozenset(['gr2_d', 'gr2_c'])),
    }
    res = {(d, frozenset(ps)) for d, ps in dept.hierarchy_heads_grouped}
    assert res == req_res


def test_department_hierarchy_chiefs_grouped_empty(staff_api):
    dept = staff_api.get_department_info('yandex')
    req_res = {
        ('yandex', frozenset(['y_c'])),
        ('group3', frozenset()),
        ('subgroup', frozenset()),
        ('dept', frozenset(['d_c'])),
        ('group1', frozenset(['gr1_c'])),
        ('group2', frozenset(['gr2_c'])),
    }
    res = {(d, frozenset(ps)) for d, ps in dept.hierarchy_chiefs_grouped}
    assert res == req_res
