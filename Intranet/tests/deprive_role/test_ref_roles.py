# coding: utf-8


from textwrap import dedent

import pytest
from idm.core import exceptions
from idm.core.workflow.exceptions import Forbidden
from idm.core.models import Role, RoleNode
from idm.tests.utils import refresh, set_workflow, assert_action_chain, patch_role

pytestmark = pytest.mark.django_db


@pytest.fixture
def role_with_ref(arda_users, simple_system_w_refs):
    frodo = arda_users.get('frodo')
    role_node = RoleNode.objects.get_node_by_data(simple_system_w_refs, {'role': 'manager'})
    role = Role.objects.create_role(frodo, simple_system_w_refs, role_node, None, save=True)
    patch_role(role, ref_roles=[
        {
            'system': simple_system_w_refs.slug,
            'role_data': {
                'role': 'manager'
            }
        }
    ])
    role.set_state('requested')
    role.set_state('approved')
    assert Role.objects.count() == 2
    role = refresh(role)
    assert role.is_active
    return role


def test_deprive_refs_if_parent_is_deprived(arda_users, role_with_ref):
    """Связанные роли отзываются, если отозвана родительская роль"""

    frodo = arda_users.get('frodo')
    assert Role.objects.count() == 2
    role_with_ref.deprive_or_decline(frodo)
    role_with_ref = refresh(role_with_ref)
    assert role_with_ref.state == 'deprived'
    assert role_with_ref.refs.get().state == 'deprived'


def test_deprive_refs_if_parent_is_deprived_for_any_ref_state(arda_users, role_with_ref):
    """Связанные роли отзываются, если отозвана родительская роль, даже если """

    frodo = arda_users.get('frodo')
    assert Role.objects.count() == 2
    role_with_ref.deprive_or_decline(frodo)
    role_with_ref = refresh(role_with_ref)
    assert role_with_ref.state == 'deprived'
    assert role_with_ref.refs.get().state == 'deprived'


def test_ref_role_cannot_be_deprived(arda_users, role_with_ref):
    """Связанная роль не может быть отозвана"""

    frodo = arda_users.get('frodo')
    ref = role_with_ref.refs.get()
    with pytest.raises(Forbidden):
        ref.deprive_or_decline(frodo)


def test_ref_role_cannot_be_rerequested(arda_users, role_with_ref):
    """Связанная роль не может быть перезапрошена"""

    frodo = arda_users.get('frodo')
    ref = role_with_ref.refs.select_related('system__actual_workflow', 'node').get()
    with pytest.raises(exceptions.RoleStateSwitchError):
        ref.set_state('need_request')

    role_with_ref.deprive_or_decline(frodo)
    ref = refresh(ref)
    assert ref.state == 'deprived'
    with pytest.raises(exceptions.RoleStateSwitchError):
        ref.set_state('need_request')


def test_deprive_requested_ref(arda_users, simple_system):
    """Проверим, что при отзыве роли, у которой есть связанные в состоянии requested, они переходят в decline"""

    frodo = arda_users.frodo
    workflow = dedent("""
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {'role': 'manager'},
            }]
        else:
            approvers = [approver('legolas')]
        """ % simple_system.slug)
    set_workflow(simple_system, workflow, workflow)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert role.refs.count() == 1
    ref = role.refs.get()
    assert ref.state == 'requested'
    role = refresh(role)
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])
    ref = role.refs.get()
    assert ref.state == 'declined'
    assert ref.is_active is False
    assert_action_chain(ref, ['request', 'apply_workflow', 'decline'])
    decline_action = ref.actions.get(action='decline')
    assert decline_action.comment == 'Reference role is deprived'
