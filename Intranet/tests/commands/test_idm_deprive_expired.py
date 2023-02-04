# coding: utf-8


import datetime

import pytest
from django.conf import settings
from django.core import management
from django.utils import timezone
from idm.core.models import Role
from idm.tests.utils import (set_workflow, refresh, raw_make_role, assert_action_chain,
                             expire_role, remove_members, days_from_now)

pytestmark = [
    pytest.mark.django_db,
]


def test_expire_granted_role(simple_system, arda_users):
    """Для выданной роли проставляем expire_at, проверяем, что роль переходит в deprived"""

    frodo = arda_users.frodo

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted',
                         expire_at=timezone.now() - datetime.timedelta(days=1))

    management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'deprived'
    assert role.is_active is False
    assert_action_chain(role, ['expire', 'first_remove_role_push', 'remove'])


def test_expire_requested_role(simple_system, arda_users):
    """Для запрошенной роли проставляем expire_at, проверяем, что роль переходит в expired"""

    frodo = arda_users.frodo
    set_workflow(simple_system, "approvers = ['legolas']")

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    expire_role(role, settings.REQUESTED_ROLE_TTL)

    management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.actions.count() == 3
    assert 'expired' == role.state
    assert not role.is_active


def test_expire_granted_group_role(simple_system, arda_users, department_structure):
    """ Проверка, что выданная временная групповая роль отзывается"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    members_count = fellowship.members.count()
    set_workflow(simple_system, group_code="approvers = []; ttl_days=5")
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, {})
    role = refresh(role)
    last_action = role.actions.order_by('-pk')[0]
    assert role.state == 'granted'
    assert (role.expire_at - timezone.now()).days in (4, 5)
    assert Role.objects.count() == members_count + 1
    assert Role.objects.filter(state='granted').count() == members_count + 1
    expire_role(role, 1)

    management.call_command('idm_deprive_expired')
    role = refresh(role)
    new_actions = role.actions.filter(pk__gt=last_action.pk)
    expire_action, remove_action = new_actions.order_by('id')
    assert expire_action.role == role
    assert expire_action.action == 'expire'
    assert remove_action.role == role
    assert remove_action.action == 'remove'
    assert role.state == 'deprived'
    assert not role.is_active
    assert Role.objects.count() == members_count + 1
    assert Role.objects.filter(state='deprived').count() == members_count + 1


def test_expire_requested_group_role(simple_system, arda_users, department_structure):
    """ Проверка, что запрошенная, но не подтверждённая групповая роль отзывается"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code="approvers = ['sauron']")
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, {})
    role = refresh(role)
    assert role.state == 'requested'
    expire_role(role, settings.REQUESTED_ROLE_TTL)

    management.call_command('idm_deprive_expired')
    role = refresh(role)
    assert role.actions.count() == 3
    assert role.state == 'expired'
    assert not role.is_active
    assert_action_chain(role, ['request', 'apply_workflow', 'expire'])


def test_expire_requested_role_for_one_system(simple_system, pt1_system, arda_users):
    """ Проверка, что можно запустить manage.py команду только для одной системы"""

    frodo = arda_users.frodo
    role1 = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted',
                          expire_at=timezone.now() - datetime.timedelta(days=1))
    role2 = raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'admin'}, state='granted',
                          expire_at=timezone.now() - datetime.timedelta(days=1))

    management.call_command('idm_deprive_expired', system=simple_system)

    role1 = refresh(role1)
    assert role1.state == 'deprived'
    assert_action_chain(role1, ['expire', 'first_remove_role_push', 'remove'])
    assert not role1.is_active

    role2 = refresh(role2)
    assert role2.actions.count() == 0
    assert role2.state == 'granted'
    assert role2.is_active


def test_expire_granted_group_role_by_hand(simple_system, arda_users, department_structure):
    """Для выданной роли проставляем expire_at, проверяем, что роль переходит в deprived"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, group_code='approvers=[]')
    fellowship = department_structure.fellowship
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role.is_active = True
    expire_role(role, 1)
    assert Role.objects.count() == fellowship.members.count() + 1

    management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.actions.count() == 6
    actions = list(role.actions.values_list('action', flat=True).order_by('pk'))
    assert actions == ['request', 'apply_workflow', 'approve', 'grant', 'expire', 'remove']
    assert role.state == 'deprived'
    assert not role.is_active

    user_role = Role.objects.get(user=legolas)
    actions = list(user_role.actions.values_list('action', flat=True).order_by('pk'))
    assert actions == ['request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push', 'remove']


def test_ref_role_cannot_expire(simple_system, arda_users, department_structure):
    """Связанная роль не может быть отозвана через протухание"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, group_code='approvers=[]')
    fellowship = department_structure.fellowship
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == fellowship.members.count() + 1
    user_role = Role.objects.get(user=legolas)
    expire_role(user_role, 1)

    management.call_command('idm_deprive_expired')

    user_role = refresh(user_role)
    actions = list(user_role.actions.values_list('action', flat=True).order_by('pk'))
    assert actions == ['request', 'approve', 'first_add_role_push', 'grant']
    assert role.state == 'granted'
    assert role.is_active


def test_onhold_expire(simple_system, arda_users, department_structure):
    """Протухание роли onhold"""
    frodo = arda_users.get('frodo')
    boromir = arda_users.get('boromir')
    varda = arda_users.get('varda')
    manve = arda_users.get('manve')

    set_workflow(simple_system, group_code='approvers=[]')

    # Случай выхода из группы
    fellowship = department_structure.fellowship
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)

    boromir_role = Role.objects.get(user=boromir)
    assert boromir_role.state == 'granted'

    remove_members(fellowship, [boromir])
    boromir_role = refresh(boromir_role)
    assert boromir_role.state == 'onhold'

    # Случай удаления группы
    valinor = department_structure.valinor

    Role.objects.request_role(varda, valinor, simple_system, '', {'role': 'manager'}, None)

    manve_role = Role.objects.get(user=manve)
    assert manve_role.state == 'granted'

    valinor.mark_depriving()
    valinor = refresh(valinor)
    valinor.deprive()
    manve_role = refresh(manve_role)
    assert manve_role.state == 'onhold'

    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        management.call_command('idm_deprive_expired')

    # Случай выхода из группы
    boromir_role = refresh(boromir_role)
    assert boromir_role.state == 'deprived'
    assert boromir_role.is_active is False

    # Случай удаления группы
    manve_role = refresh(manve_role)
    assert manve_role.state == 'deprived'
    assert manve_role.is_active is False


def test_nowis(simple_system, arda_users):
    """Проверим, что можно позвать команду с nowis в будущем, и она отзовёт необходимые роли"""

    frodo = arda_users.frodo

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted',
                         expire_at=timezone.now() + datetime.timedelta(days=7))

    management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'granted'

    management.call_command('idm_deprive_expired', now_is=timezone.now() + datetime.timedelta(days=7))

    role = refresh(role)
    assert role.state == 'deprived'

    assert role.state == 'deprived'
    assert role.is_active is False
    assert_action_chain(role, ['expire', 'first_remove_role_push', 'remove'])
