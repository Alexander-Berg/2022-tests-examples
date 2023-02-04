# coding: utf-8


import mock
import pytest
from django.core.management import call_command

from idm.core.constants.instrasearch import INTRASEARCH_METHOD
from idm.core.constants.rolenode import ROLENODE_STATE
from idm.core.models import RoleNode
from idm.tests.utils import raw_make_role

pytestmark = [
    pytest.mark.django_db,
]


def test_push_nodes(complex_system, arda_users):
    frodo = arda_users.frodo
    aragorn = arda_users.aragorn
    gandalf = arda_users.gandalf
    raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'})
    raw_make_role(aragorn, complex_system, {'project': 'rules', 'role': 'admin'})
    raw_make_role(gandalf, complex_system, {'project': 'subs', 'role': 'manager'})
    frodo_node = complex_system.nodes.get(slug='developer')
    aragorn_node = complex_system.nodes.get(slug='admin')
    frodo_node.need_isearch_push_method = INTRASEARCH_METHOD.REMOVE
    frodo_node.state = ROLENODE_STATE.DEPRIVING
    aragorn_node.need_isearch_push_method = INTRASEARCH_METHOD.ADD
    frodo_node.save(update_fields=['need_isearch_push_method', 'state'])
    aragorn_node.save(update_fields=['need_isearch_push_method'])

    with mock.patch.object(RoleNode, 'send_intrasearch_push') as mocked_push:
        call_command('idm_push_nodes_to_isearch')
    mocked_push.assert_called_once_with(INTRASEARCH_METHOD.ADD)

    frodo_node.state = ROLENODE_STATE.DEPRIVED
    frodo_node.save(update_fields=['state'])
    with mock.patch.object(RoleNode, 'send_intrasearch_push') as mocked_push:
        call_command('idm_push_nodes_to_isearch')
    assert mocked_push.call_count == 2
    mocked_push.assert_any_call(INTRASEARCH_METHOD.ADD)
    mocked_push.assert_any_call(INTRASEARCH_METHOD.REMOVE)

    frodo_node.is_key = True
    frodo_node.save(update_fields=['is_key'])
    with mock.patch.object(RoleNode, 'send_intrasearch_push') as mocked_push:
        call_command('idm_push_nodes_to_isearch')
    mocked_push.assert_called_once_with(INTRASEARCH_METHOD.ADD)
