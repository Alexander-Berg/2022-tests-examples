# coding: utf-8

import mock
import pytest

from idm.core.constants.workflow import DEFAULT_PRIORITY
from idm.core.workflow.common.subject import subjectify
from idm.core.workflow.sandbox.manager.user import UserWrapper as IDMUserWrapper
from idm.core.workflow.sandbox.container.context import UserWorkflowContext
from idm.core.workflow.sandbox.container.executor import ContainerWorkflowExecutor
from idm.core.workflow.sandbox.container.wrappers import UserWrapper as ContainerUserWrapper
from idm.core.conflicts import Conflict
from idm.core.workflow.sandbox.manager.conflcit import ConflictWrapper as IDMConflictWrapper
from idm.core.workflow.sandbox.container.wrappers import ConflictWrapper as ContainerConflictWrapper

pytestmark = [pytest.mark.django_db]


def test_initial_properties(arda_users, department_structure):
    frodo = arda_users.frodo
    frodo_idm_wrapper = IDMUserWrapper(arda_users.frodo)
    frodo_as_dict = frodo_idm_wrapper.as_dict()
    assert frodo_as_dict == {
        'username': frodo.username,
        'rank': None,
        'priority': DEFAULT_PRIORITY,
        'initial_properties': {
            'type': 'dict',
            'value': {
                'email': frodo.email,
                'is_robot': frodo.is_robot,
                'is_tvm_app': frodo.is_tvm_app,
                'is_homeworker': frodo.is_homeworker,
            }
        }
    }

    frodo_container_wrapper = ContainerUserWrapper.from_dict(frodo_as_dict)
    for field in ('username', 'email', 'is_robot', 'is_tvm_app'):
        assert getattr(frodo_container_wrapper, field) == getattr(frodo, field)


def test_exceptions(arda_users):
    context = UserWorkflowContext()
    executor = ContainerWorkflowExecutor('assert False', context)
    with mock.patch('idm.core.workflow.sandbox.container.runner.runner') as runner:
        executor.run()

    assert runner.connection.send_data.call_args_list == [mock.call(
        2,
        {
            'type': 'WorkflowSyntaxError',
            'value': {
                'exception': '',
                'globals': {
                    'type': 'dict',
                    'value': {
                        'False': False,
                        'None': None,
                        'True': True}
                },
                'line': 'assert False',
                'locals': {
                    'type': 'dict',
                    'value': {
                        'False': False,
                        'None': None,
                        'True': True}
                },
                'lineno': 0
            }
        }
    )]


@pytest.mark.xfail
def test_serialize_conflict(simple_system, arda_users_with_roles):
    """
    https://st.yandex-team.ru/IDM-9755
    """
    role = arda_users_with_roles.frodo[0]
    subj = role.user
    node = role.node
    outer_wrapper = IDMConflictWrapper.wrap(Conflict(requested_path='rp',
                                                     conflicting_path='cp',
                                                     email='a@b',
                                                     conflicting_system=simple_system,
                                                     subj=subjectify(subj),
                                                     node=node))
    data1 = outer_wrapper.as_dict()
    assert data1['conflicting_path'] == 'cp'
    assert data1['requested_path'] == 'rp'
    assert data1['email'] == 'a@b'
    assert data1['subj']['value']['username'] == 'frodo'
    assert data1['conflicting_system']['value'] == {
        'slug': 'simple', 'initial_properties': {'type': 'dict', 'value': {'slug': 'simple'}}}
    assert data1['node']['value']['initial_properties'] == {
        'type': 'dict', 'value': {'slug': 'admin', 'slug_path': '/role/admin/',
                                  'value_path': '/admin/', 'parent_path': '/simple/'}}

    inner_wrapper = ContainerConflictWrapper.from_dict(data1)
    assert inner_wrapper.conflicting_path == 'cp'
    assert inner_wrapper.requested_path == 'rp'
    assert inner_wrapper.email == 'a@b'
    assert inner_wrapper.subj.username == subj.username
    assert inner_wrapper.conflicting_system.slug == simple_system.slug
    assert inner_wrapper.node.slug == node.slug

    data2 = inner_wrapper.as_dict()
    assert data1 == data2
    assert outer_wrapper == IDMConflictWrapper.from_dict(data2)


@pytest.mark.xfail
def test_serialize_group_conflict(simple_system, arda_users_with_roles):
    role = arda_users_with_roles.fellowship[0]
    subj = role.group
    node = role.node
    outer_wrapper = IDMConflictWrapper.wrap(Conflict(requested_path='rp',
                                                     conflicting_path='cp',
                                                     email='a@b',
                                                     conflicting_system=simple_system,
                                                     subj=subjectify(subj),
                                                     node=node))
    data1 = outer_wrapper.as_dict()
    data2 = ContainerConflictWrapper.from_dict(data1).as_dict()
    assert data1 == data2
    assert outer_wrapper == IDMConflictWrapper.from_dict(data2)
