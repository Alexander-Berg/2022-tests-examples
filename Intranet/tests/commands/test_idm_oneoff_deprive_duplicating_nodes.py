# -*- coding: utf-8 -*-


from copy import deepcopy

import pytest
from django.core.management import call_command
from idm.core.models import Role, RoleNode
from idm.tests.utils import raw_make_role, refresh

pytestmark = [pytest.mark.django_db, pytest.mark.xfail]


def test_deprive_duplicating_nodes(simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted')
    manager_node = role1.node
    raw_make_role(legolas, simple_system, {'role': 'manager'}, state='granted')
    raw_make_role(frodo, simple_system, {'role': 'admin'}, state='need_request')
    raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='granted')

    for node in RoleNode.objects.filter(level=2):
        node.slug = 'manager'
        node.slug_path = '/role/manager/'
        node.save(update_fields=('slug', 'slug_path'))

    call_command('idm_oneoff_deprive_duplicating_nodes', system='simple')

    assert Role.objects.filter(is_active=True).count() == 4
    assert Role.objects.filter(node=manager_node).count() == 4
    assert RoleNode.objects.filter(state='active', level=2).count() == 1
    assert RoleNode.objects.filter(state='depriving', level=2).count() == 3


def test_deprive_duplicating_nodes_on_complex_system(complex_system, arda_users):
    frodo = arda_users.frodo

    role1 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    role2 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    role3 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    manager_node = role1.node
    parent = manager_node.parent
    grand = parent.parent
    clone_grand = deepcopy(grand)
    clone_grand.pk = None
    clone_grand.save()
    clone_parent = deepcopy(parent)
    clone_parent.parent = clone_grand
    clone_parent.pk = None
    clone_parent.save()
    clone_node = deepcopy(manager_node)
    clone_node.parent = clone_parent
    clone_node.pk = None
    clone_node.save()

    role2.node = clone_node
    role2.save(update_fields=('node',))
    role3.node = clone_node
    role3.save(update_fields=('node',))

    call_command('idm_oneoff_deprive_duplicating_nodes', system=complex_system.slug)

    assert Role.objects.filter(is_active=True).count() == 3
    assert Role.objects.filter(node=manager_node).count() == 0
    assert Role.objects.filter(node=clone_node).count() == 3

    clone_node = refresh(clone_node)
    assert clone_node.state == 'active'
    assert refresh(manager_node).state == 'depriving'
    assert refresh(clone_grand).state == 'active'
    assert refresh(grand).state == 'depriving'
    assert refresh(clone_parent).state == 'active'
    assert refresh(parent).state == 'depriving'


def test_zero_roles(complex_system):
    node = RoleNode.objects.get_node_by_data(complex_system, {'project': 'subs', 'role': 'manager'})
    parent = node.parent
    grand = parent.parent
    clone_grand = deepcopy(grand)
    clone_grand.pk = None
    clone_grand.save()

    clone_grand2 = deepcopy(grand)
    clone_grand2.pk = None
    clone_grand2.save()

    clone_parent = deepcopy(parent)
    clone_parent.parent = clone_grand
    clone_parent.pk = None
    clone_parent.save()

    clone_parent2 = deepcopy(parent)
    clone_parent2.parent = clone_grand2
    clone_parent2.pk = None
    clone_parent2.save()

    clone_node = deepcopy(node)
    clone_node.parent = clone_parent
    clone_node.pk = None
    clone_node.save()

    clone_node2 = deepcopy(node)
    clone_node2.parent = clone_parent2
    clone_node2.pk = None
    clone_node2.save()

    call_command('idm_oneoff_deprive_duplicating_nodes', system=complex_system.slug)

    states = []

    for node_ in (node, parent, grand, clone_node, clone_parent, clone_grand, clone_node2, clone_parent2, clone_grand2):
        node_.refresh_from_db()
        states.append(node_.state)

    expected = ['depriving', 'depriving', 'depriving', 'depriving', 'depriving', 'depriving', 'active', 'active', 'active']
    assert states == expected
