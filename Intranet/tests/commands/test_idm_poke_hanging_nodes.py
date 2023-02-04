# coding: utf-8


import datetime

import pytest
from django.core.management import call_command
from django.utils import timezone
from idm.tests.utils import raw_make_role, refresh, assert_action_chain

pytestmark = pytest.mark.django_db


def test_deprive_hanging_roles(simple_system, arda_users):
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""

    frodo = arda_users['frodo']

    role1 = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted',
                         updated=timezone.now() - datetime.timedelta(days=1))
    role2 = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted',
                          updated=timezone.now() - datetime.timedelta(days=1))

    role1.node.state = 'deprived'
    role1.node.save(update_fields=['state'])
    assert_action_chain(role1, [])
    assert_action_chain(role2, [])

    call_command('idm_poke_hanging_nodes')

    role1, role2 = refresh(role1), refresh(role2)
    assert role1.state == 'deprived'
    assert_action_chain(role1, ['deprive', 'first_remove_role_push', 'remove'])
    assert role2.state == 'granted'
    assert_action_chain(role2, [])


def test_deprive_hanging_roles_with_specified_system(simple_system, complex_system, arda_users):
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""

    frodo = arda_users['frodo']

    role1 = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted',
                         updated=timezone.now() - datetime.timedelta(days=1))
    role2 = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted',
                          updated=timezone.now() - datetime.timedelta(days=1))
    role3 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted',
                             updated=timezone.now() - datetime.timedelta(days=1))
    role4 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'}, state='granted',
                          updated=timezone.now() - datetime.timedelta(days=1))

    for role in (role1, role3):
        role.node.state = 'deprived'
        role.node.save(update_fields=['state'])

    for role in (role1, role2, role3, role4):
        assert_action_chain(role, [])

    call_command('idm_poke_hanging_nodes', '--system', simple_system.slug)

    role1 = refresh(role1)
    role2 = refresh(role2)
    role3 = refresh(role3)
    role4 = refresh(role4)
    assert role1.state == 'deprived'
    assert_action_chain(role1, ['deprive', 'first_remove_role_push', 'remove'])

    for role in (role2, role3, role4):
        assert role.state == 'granted'
        assert_action_chain(role, [])