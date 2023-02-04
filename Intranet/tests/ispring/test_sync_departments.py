from unittest.mock import patch
from copy import deepcopy

from intranet.hrdb_ext.src.ispring.api.departments import DepartmentRepository
from intranet.hrdb_ext.src.ispring.mock.connector import MockedConnector
from intranet.hrdb_ext.src.ispring.sync.tree import Tree
from intranet.hrdb_ext.src.ispring.sync.departments import (
    RemoveUnknownDepartments,
    DepartmentCreateRequest,
    CreateDepartments,
    UpdateDepartments,
    DepartmentUpdateRequest,
)
from intranet.hrdb_ext.src.ispring.mock.resources import MockedDepartment


ISPRING_ROOT_DEPARTMENT = MockedDepartment(
    department_id='i-master',
    parent_id=None,
    name='MasterDep',
    code='-',
)

#
#                   i-master
#               i-level-0-dep-1
#       i-level-1-dep-1     i-level-1-dep-2
#   i-level-2-dep-1
#
DEFAULT_ISPRING_TREE = [
    ISPRING_ROOT_DEPARTMENT,

    MockedDepartment(
        department_id='i-level-0-dep-1',
        parent_id='i-master',
        name='Dep0',
        code='level-0-dep-1-code',
    ),
    MockedDepartment(
        department_id='i-level-1-dep-1',
        parent_id='i-level-0-dep-1',
        name='Dep11',
        code='level-1-dep-1-code',
    ),
    MockedDepartment(
        department_id='i-level-1-dep-2',
        parent_id='i-level-0-dep-1',
        name='Dep12',
        code='level-1-dep-2-code',
    ),
    MockedDepartment(
        department_id='i-level-2-dep-1',
        parent_id='i-level-1-dep-1',
        name='Dep21',
        code='level-2-dep-1-code',
    ),
]

#
#               i-level-0-dep-1
#       i-level-1-dep-1     i-level-1-dep-2
#   i-level-2-dep-1
#
DEFAULT_STAFF_TREE = [
    {'department_id': 100, 'code': 'level-0-dep-1-code', 'parent_id': None, 'name': 'Dep0'},
    {'department_id': 111, 'code': 'level-1-dep-1-code', 'parent_id': 100, 'name': 'Dep11'},
    {'department_id': 112, 'code': 'level-1-dep-2-code', 'parent_id': 100, 'name': 'Dep12'},
    {'department_id': 121, 'code': 'level-2-dep-1-code', 'parent_id': 111, 'name': 'Dep21'},
]


def init_ispring_tree(repo, departments):
    for department in departments:
        repo.connector.departments.by_id[department.department_id] = department
    return departments


def convert_staff_meta_to_staff_response(departments):
    return [
        {
            'department': {'id': meta['department_id'], 'name': {'full': {'ru': meta['name']}}},
            'url': meta['code'],
            'ancestors': [{'department': {'id': meta['parent_id']}}],
            'is_deleted': meta.get('is_deleted', False),
        }
        for meta in departments
    ]


@patch.object(DepartmentRepository, 'connector_cls', MockedConnector)
@patch('intranet.hrdb_ext.src.ispring.sync.tree.load_departments')
def test_create_departments_from_scratch(fake_load_departments):
    repo = DepartmentRepository(departments_count=0)

    # Master node should already be created (ispring root for all)
    assert len(repo.connector.departments.by_id) == 0
    init_ispring_tree(repo, [ISPRING_ROOT_DEPARTMENT])
    assert len(repo.connector.departments.by_id) == 1

    staff_departments = convert_staff_meta_to_staff_response(DEFAULT_STAFF_TREE)
    fake_load_departments.return_value = staff_departments
    task = CreateDepartments('i-master', 100, repository=repo)

    task.load()
    assert isinstance(task.ispring_tree, Tree)
    assert isinstance(task.staff_tree, Tree)
    assert len(task.ispring_tree.by_id) == 1
    assert len(task.staff_tree.by_id) == 4

    task.generate_requests()
    assert len(task.requests) == 4

    # Should be in the same order (from top to bottom of the tree)
    for department, request in zip(staff_departments, task.requests):
        assert request.name == department['department']['name']['full']['ru']
        assert request.code == department['url']

        parent_id = department['ancestors'][0]['department']['id']
        if parent_id is None:
            assert request.parent_exists
            assert request.parent_id == ISPRING_ROOT_DEPARTMENT.department_id
        else:
            assert not request.parent_exists

    assert len(task.new_departments) == 0
    assert len(repo.connector.departments.by_id) == 1

    task.run_requests()

    assert len(task.new_departments) == 4
    assert len(repo.connector.departments.by_id) == 5

    by_id = repo.connector.departments.by_id
    uids = task.new_departments
    assert by_id[uids['level-0-dep-1-code']].parent_id == 'i-master'
    assert by_id[uids['level-1-dep-1-code']].parent_id == uids['level-0-dep-1-code']
    assert by_id[uids['level-1-dep-2-code']].parent_id == uids['level-0-dep-1-code']
    assert by_id[uids['level-2-dep-1-code']].parent_id == uids['level-1-dep-1-code']


@patch.object(DepartmentRepository, 'connector_cls', MockedConnector)
@patch('intranet.hrdb_ext.src.ispring.sync.tree.load_departments')
def test_create_departments_in_filled_tree(fake_load_departments):
    repo = DepartmentRepository(departments_count=0)
    assert len(repo.connector.departments.by_id) == 0

    ispring_departments = init_ispring_tree(repo, DEFAULT_ISPRING_TREE)

    #
    #                               i-level-0-dep-1
    #           i-level-1-dep-1     i-level-1-dep-2     i-level-1-dep-3
    #       i-level-2-dep-1                                     i-level-2-dep-3
    #   i-level-3-dep-1
    #
    staff_departments = convert_staff_meta_to_staff_response(DEFAULT_STAFF_TREE + [
        {'department_id': 113, 'code': 'level-1-dep-3-code', 'parent_id': 100, 'name': 'Dep13'},
        {'department_id': 123, 'code': 'level-2-dep-3-code', 'parent_id': 113, 'name': 'Dep23'},
        {'department_id': 131, 'code': 'level-3-dep-1-code', 'parent_id': 121, 'name': 'Dep31'},
    ])

    fake_load_departments.return_value = staff_departments
    task = CreateDepartments('i-master', 100, repository=repo)

    task.load()
    assert isinstance(task.ispring_tree, Tree)
    assert isinstance(task.staff_tree, Tree)

    ispring_ids = [d.department_id for d in ispring_departments]
    assert sorted(task.ispring_tree.by_id.keys()) == sorted(ispring_ids)
    ispring_codes = [d.code for d in ispring_departments]
    assert sorted(task.ispring_tree.by_code.keys()) == sorted(ispring_codes)

    staff_ids = [d['department']['id'] for d in staff_departments]
    assert sorted(task.staff_tree.by_id.keys()) == sorted(staff_ids)
    staff_codes = [d['url'] for d in staff_departments]
    assert sorted(task.staff_tree.by_code.keys()) == sorted(staff_codes)

    task.generate_requests()
    assert len(task.requests) == 3

    assert sorted(task.requests, key=lambda x: x.code) == [
        DepartmentCreateRequest(
            department_id=None,
            parent_id='i-level-0-dep-1',
            name='Dep13',
            code='level-1-dep-3-code',
            parent_exists=True,
        ),
        DepartmentCreateRequest(
            department_id=None,
            parent_id='level-1-dep-3-code',
            name='Dep23',
            code='level-2-dep-3-code',
            parent_exists=False,
        ),
        DepartmentCreateRequest(
            department_id=None,
            parent_id='i-level-2-dep-1',
            name='Dep31',
            code='level-3-dep-1-code',
            parent_exists=True,
        )
    ]

    assert len(task.new_departments) == 0
    assert len(repo.connector.departments.by_id) == 5

    task.run_requests()

    assert len(task.new_departments) == 3
    assert len(repo.connector.departments.by_id) == 8

    for code, obj_id in task.new_departments.items():
        assert code is not None
        assert obj_id is not None
        assert code in staff_codes


@patch.object(DepartmentRepository, 'connector_cls', MockedConnector)
@patch('intranet.hrdb_ext.src.ispring.sync.tree.load_departments')
def test_update_department(fake_load_departments):
    repo = DepartmentRepository(departments_count=0)
    assert len(repo.connector.departments.by_id) == 0

    init_ispring_tree(repo, DEFAULT_ISPRING_TREE)

    #
    #               i-level-0-dep-1
    #       [*]i-level-2-dep-1     i-level-1-dep-2
    #                                    [*]i-level-1-dep-1
    #
    #   - i-level-1-dep-1 унесли из под корня (с уровня 1) на уровень 3
    #   - i-level-2-dep-1 подняли с уровня 3 на уровень 2
    #   - поменяли названия у i-level-2-dep-1 и i-level-1-dep-2
    #
    staff_departments = convert_staff_meta_to_staff_response([
        {'department_id': 100, 'code': 'level-0-dep-1-code', 'parent_id': None, 'name': 'Dep0'},
        {'department_id': 121, 'code': 'level-2-dep-1-code', 'parent_id': 100, 'name': 'Dep21_NEW'},
        {'department_id': 112, 'code': 'level-1-dep-2-code', 'parent_id': 100, 'name': 'Dep12_NEW'},
        {'department_id': 111, 'code': 'level-1-dep-1-code', 'parent_id': 112, 'name': 'Dep11'},
    ])

    fake_load_departments.return_value = staff_departments
    task = UpdateDepartments('i-master', 100, repository=repo)

    task.load()
    assert isinstance(task.ispring_tree, Tree)
    assert isinstance(task.staff_tree, Tree)
    assert len(task.ispring_tree.by_id) == 5
    assert len(task.staff_tree.by_id) == 4

    task.generate_requests()
    assert task.requests == [  # order not crucial, but fixed (by bfs)
        DepartmentUpdateRequest(
            department_id='i-level-2-dep-1',
            parent_id='i-level-0-dep-1',
            name='Dep21_NEW',
        ),
        DepartmentUpdateRequest(
            department_id='i-level-1-dep-2',
            name='Dep12_NEW',
        ),
        DepartmentUpdateRequest(
            department_id='i-level-1-dep-1',
            parent_id='i-level-1-dep-2',
        ),
    ]


@patch.object(DepartmentRepository, 'connector_cls', MockedConnector)
@patch('intranet.hrdb_ext.src.ispring.sync.tree.load_departments')
def test_delete_departments(fake_load_departments):
    repo = DepartmentRepository(departments_count=0)
    assert len(repo.connector.departments.by_id) == 0

    init_ispring_tree(repo, DEFAULT_ISPRING_TREE)

    #
    #                       i-level-0-dep-1
    #           EXCLUDED:i-level-1-dep-1     i-level-1-dep-2
    #   DELETED:i-level-2-dep-1
    #
    #   * Excluding i-level-1-dep-1 from staff response.
    #       That means, that it is not our department and we should not delete it.
    #   * Removing i-level-2-dep-1 (by marking it as is_deleted=True).
    #       That means, that staff knows about that department, and it is deleted there.
    staff_tree = []
    for department in DEFAULT_STAFF_TREE:
        if department['code'] == 'level-1-dep-1-code':
            continue
        copy = deepcopy(department)
        if copy['code'] == 'level-2-dep-1-code':
            copy['is_deleted'] = True
        staff_tree.append(copy)

    staff_departments = convert_staff_meta_to_staff_response(staff_tree)

    fake_load_departments.return_value = staff_departments
    task = RemoveUnknownDepartments('i-master', 100, repository=repo)

    task.load()
    assert isinstance(task.ispring_tree, Tree)
    assert isinstance(task.staff_tree, Tree)
    assert len(task.ispring_tree.by_id) == 5
    assert len(task.staff_tree.by_id) == 3

    task.generate_requests()
    assert task.requests == [
        'i-level-2-dep-1',
    ]

    assert len(repo.connector.departments.by_id) == 5
    task.run_requests()
    assert len(repo.connector.departments.by_id) == 4
    assert sorted(repo.connector.departments.by_id.keys()) == sorted([
        'i-master',
        'i-level-0-dep-1',
        'i-level-1-dep-1',
        'i-level-1-dep-2',
    ])
