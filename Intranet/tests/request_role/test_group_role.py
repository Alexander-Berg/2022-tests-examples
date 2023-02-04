# coding: utf-8


import copy
from textwrap import dedent
from unittest.mock import patch

import constance
import pytest
import waffle.testutils
from constance.test import override_config
from django.core import mail
from django.db.models import Count
from django.utils import timezone

from idm.core.workflow import exceptions
from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Role, RoleNode, ApproveRequest, Transfer, RoleField
from idm.core.plugins.errors import PluginError, PluginFatalError
from idm.tests.utils import (add_perms_by_role, set_workflow, refresh, DEFAULT_WORKFLOW, assert_action_chain,
                             assert_contains, clear_mailbox, raw_make_role, remove_members, add_members, move_group,
                             accept, change_department, ignore_tasks, create_user, create_group_structure,
                             random_slug)
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group, GroupMembership, User, Organization

pytestmark = [pytest.mark.django_db]

REVIEW_ON_RELOCATE_POLICIES = ('review', 'ignore')
OVERRIDE_LARGE_GROUP_SIZE = 3

def frodo_forbid(self, method, data, **_):
    """Хелпер для тестов, запрещающий выдачу роли для frodo"""
    login = data.pop('login')
    if login == 'frodo':
        raise PluginError(1, 'Cannot add second role', {})
    return {
        'data': {
            'token': 'another'
        }
    }


def test_cannot_request_group_role_if_group_policy_forbids_that(simple_system, arda_users, department_structure):
    """Проверка того, что если group_policy у системы запрещает запрос групповых ролей, то они действительно не могут
    быть запрошены"""

    frodo = arda_users.frodo
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    group = Group.objects.get(slug='fellowship-of-the-ring')
    simple_system.group_policy = 'unavailable'
    simple_system.save()
    with pytest.raises(exceptions.GroupPolicyError):
        Role.objects.request_role(frodo, group, simple_system, '', data={'role': 'manager'}, fields_data={})
    simple_system.group_policy = 'weird_policy'
    simple_system.save()
    with pytest.raises(exceptions.GroupPolicyError):
        Role.objects.request_role(frodo, group, simple_system,
                                  comment='', data={'role': 'manager'}, fields_data={})


def test_simple_role_request(simple_system, arda_users, department_structure):
    """В простом случае групповая роль должна выдавать связанную пользовательскую роль"""
    frodo = arda_users.frodo
    group = department_structure.fellowship
    organization = Organization.objects.create(org_id=100000)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()
    assert group.members.count() == members_count
    group_role = Role.objects.request_role(
        requester=frodo,
        subject=group,
        system=simple_system,
        comment='',
        data={'role': 'manager'},
        fields_data=None,
        organization_id=organization.id,
    )
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    frodo_role = Role.objects.get(user=frodo)
    assert group_role.organization_id == organization.id
    assert frodo_role.organization_id == organization.id
    frodo_role.fetch_node()
    assert frodo_role.node.data == {'role': 'manager'}
    assert frodo_role.fields_data is None
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Группа "Братство кольца", в которой вы являетесь ответственным, '
                     'получила новую роль в системе "Simple система":', 'Роль: Менеджер'], message.body)


def test_user_role_is_created_only_when_group_role_is_granted(simple_system, arda_users, department_structure):
    """Если групповую роль нужно подтверждать, то пользовательские роли не должны быть выданы раньше, чем групповая"""
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    group = department_structure.fellowship

    set_workflow(simple_system, group_code='approvers = ["legolas"]')
    members_count = group.members.count()
    Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == 1
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.set_approved(legolas)
    assert Role.objects.count() == 1 + members_count
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 3
    message1, message2, message3 = mail.outbox
    assert message1.to == ['legolas@example.yandex.ru']
    assert message1.subject == 'Подтверждение роли. Simple система.'
    assert_contains([
        'Фродо Бэггинс',
        '"Братство кольца (105)"',
        '"Simple система"',
        'Роль: Менеджер'
    ], message1.body)
    assert message2.to == ['frodo@example.yandex.ru']
    assert message2.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert_contains([
        'Вы запросили роль в системе "Simple система" для группы "Братство кольца", '
        'в которой вы являетесь ответственным:',
        '"Simple система"',
        'Роль: Менеджер',
        'legolas',
    ], message2.body)
    assert message3.to == ['frodo@example.yandex.ru']
    assert message3.subject == 'Simple система. Новая роль'


def test_user_roles_are_not_created_if_group_role_is_declined(simple_system, arda_users, department_structure):
    """Если групповая роль была отклонена, то пользовательские роли не должны создаваться"""
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    group = department_structure.fellowship

    set_workflow(simple_system, group_code='approvers = ["legolas"]')
    role = Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == 1
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.set_declined(legolas)
    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'declined'
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 3
    message1, message2, message3 = mail.outbox
    assert message1.to == ['legolas@example.yandex.ru']
    assert message2.to == ['frodo@example.yandex.ru']
    assert message3.to == ['frodo@example.yandex.ru']
    assert message1.subject == 'Подтверждение роли. Simple система.'
    assert message2.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert message3.subject == 'Simple система. Заявка на роль отклонена'
    assert_contains([
        'Фродо Бэггинс',
        '"Братство кольца (105)"',
        '"Simple система"',
        'Роль: Менеджер',
        'Вы получили запрос, он ожидает вашего подтверждения. С деталями вы можете ознакомиться ниже. Чтобы подтвердить или отклонить запрос, пожалуйста, перейдите по ссылке',
        'https://example.com/queue/'
    ], message1.body)
    assert_contains([
        'Вы запросили роль в системе "Simple система" для группы "Братство кольца", '
        'в которой вы являетесь ответственным',
        '"Simple система"',
        'Роль: Менеджер',
        'legolas',
    ], message2.body)
    assert_contains([(
        'legolas отклонил запрос роли в системе "Simple система" на группу "Братство кольца", '
        'в которой вы являетесь ответственным:'),
        'Роль: Менеджер'
    ], message3.body)


def test_user_gets_same_user_role_and_group_role(simple_system, arda_users, department_structure):
    """Проверка, что пользователь может получить одну и ту же пользовательскую и групповую роль"""
    frodo = arda_users.get('frodo')
    group = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()
    Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == members_count + 1
    assert Role.objects.filter(state='granted').count() == members_count + 1
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == members_count + 2
    assert Role.objects.filter(state='granted').count() == members_count + 2
    # отзовём пользовательскую роль, групповая должна остаться
    role = Role.objects.get(user__username='frodo', parent=None)
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'deprived'
    assert Role.objects.get(user__username='frodo', parent__isnull=False).state == 'granted'
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 3
    assert {tuple(message.to) for message in mail.outbox} == {('frodo@example.yandex.ru',)}
    expected_subjects = ['Simple система. Новая роль', 'Simple система. Новая роль', 'Simple система. Роль отозвана']
    assert [message.subject for message in mail.outbox] == expected_subjects
    assert_contains([
        ('Группа "Братство кольца", в которой вы являетесь ответственным, '
         'получила новую роль в системе "Simple система":')
    ], mail.outbox[0].body)
    assert_contains([
        'Вы получили новую роль в системе "Simple система":',
    ], mail.outbox[1].body)
    assert_contains([
        'Вы отозвали вашу роль в системе "Simple система":'
    ], mail.outbox[2].body)


def test_user_can_get_two_same_group_roles(simple_system, arda_users, department_structure):
    """Проверка, что пользователь, состоящий в двух группах, может получить две одинаковых роли,
    дочерних для каждой из групп"""
    frodo = arda_users.get('frodo')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    shire = Group.objects.get(slug='the-shire')
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    fellowship_count = fellowship.members.count()
    shire_count = shire.members.count()
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == fellowship_count + 1
    shire_role = Role.objects.request_role(frodo, shire, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == fellowship_count + 1 + shire_count + 1
    assert Role.objects.filter(state='granted').count() == fellowship_count + 1 + shire_count + 1
    assert Role.objects.filter(user__username='frodo', state='granted').count() == 2
    shire_role.deprive_or_decline(frodo)
    assert Role.objects.filter(user__username='frodo', state='granted').count() == 1
    assert Role.objects.filter(user__username='frodo', state='deprived').count() == 1
    assert Role.objects.filter(state='deprived').count() == shire_count + 1
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 5
    msg1, msg2, msg3, msg4, msg5 = mail.outbox
    assert_contains(['Группа "Братство кольца", в которой вы являетесь ответственным,',
                     'получила новую роль в системе "Simple система":'], msg1.body)
    assert_contains(['Группа "Шир", в которой вы являетесь ответственным,',
                     'получила новую роль в системе "Simple система":'], msg2.body)
    assert_contains(['Группа "Шир", в которой вы являетесь ответственным,',
                     'получила новую роль в системе "Simple система":'], msg3.body)
    assert_contains(['Фродо Бэггинс (frodo) отозвал роль в системе "Simple система" у группы "Шир", '
                     'в которой вы являетесь ответственным:'], msg4.body)
    assert_contains(['Фродо Бэггинс (frodo) отозвал роль в системе "Simple система" у группы "Шир", '
                     'в которой вы являетесь ответственным:'], msg5.body)
    assert msg1.to == ['frodo@example.yandex.ru']
    shire_mails = {responsible.email for responsible in shire.get_responsibles()}
    assert set(msg2.to + msg3.to) == shire_mails
    assert set(msg4.to + msg5.to) == shire_mails


def test_member_of_two_groups_gets_just_one_role(simple_system, arda_users, department_structure):
    """Проверка, что пользователь, состоящий в двух группах, одна из которых является дочерней для другой,
    получает только одну роль, если на родительскую группу выдана роль. В дереве групп такое происходить не
    должно, но если случилось, мы должны корректно обработать этот краевой случай."""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    associations = department_structure.associations
    associations.memberships.direct().update(state=GROUPMEMBERSHIP_STATE.INACTIVE)
    associations.add_members([legolas, frodo])
    assert associations.get_unique_memberships_count() == fellowship.get_unique_memberships_count()
    assert associations.get_descendant_members().count() == fellowship.get_descendant_members().count() + 2
    assert frodo in fellowship.members.all()
    assert frodo in associations.members.all()
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'manager'}, None)
    unique_count = associations.get_unique_memberships_count()
    assert Role.objects.count() == unique_count + 1
    assert Role.objects.filter(group=None, state='granted').count() == unique_count
    userrole = Role.objects.get(user=legolas)
    assert userrole.parent_id == role.id
    assert_action_chain(userrole, ['request', 'approve', 'first_add_role_push', 'grant'])
    request_action = userrole.actions.get(action='request')
    assert set(request_action.data.keys()) == {'comment'}


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_user_role_is_put_on_hold_on_leave(simple_system, arda_users, department_structure, without_hold, onhold_state):
    """Если пользователь вышел из группы, то его роль, связанная с групповой, должна отложиться.
    А не связанная с групповой - не должна отложиться"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    fellowship_count = fellowship.members.count()
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None, **kwargs)
    personal_role = Role.objects.request_role(frodo, gandalf, simple_system, '', {'role': 'manager'}, None)
    # число ролей - это число членов группы + одна групповая + одна личная
    assert Role.objects.filter(state='granted').count() == fellowship_count + 2
    moving_users = [arda_users.boromir, arda_users.gandalf]
    remove_members(fellowship, moving_users)
    add_members(valinor, moving_users)

    # теперь число ролей - это число членов группы-2, так как двое вышли, плюс одна групповая.
    # личная пока в granted, но при разрешении transfer-а может перейти в need_request.
    stats = dict(Role.objects.values('state').annotate(x=Count('state')).values_list('state', 'x').order_by('x'))
    assert stats == {
        'granted': fellowship_count - 2 + 1 + 1,
        onhold_state: 2,
    }
    hold = Role.objects.filter(user__in=moving_users, state=onhold_state)
    assert hold.count() == 2

    if onhold_state == 'onhold':
        assert_action_chain(hold[0], ['request', 'approve', 'first_add_role_push', 'grant', 'hold'])
    else:
        assert_action_chain(hold[0], [
            'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'remove',
        ])
    assert Transfer.objects.count() == 2
    assert Transfer.objects.filter(parent=None).count() == 2

    accept(Transfer.objects.all())
    stats = dict(Role.objects.values('state').annotate(x=Count('state')).values_list('state', 'x').order_by('x'))
    assert stats == {
        'need_request': 1,
        'granted': fellowship_count - 2 + 1,
        onhold_state: 2,
    }

    personal_role = refresh(personal_role)
    assert personal_role.state == 'need_request'
    assert_action_chain(personal_role, [
        'request', 'apply_workflow', 'approve', 'grant', 'ask_rerequest',
    ])


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_user_role_is_put_on_hold_on_leave_if_it_were_active(simple_system, arda_users, department_structure,
                                                             without_hold, onhold_state):
    """Если пользователь вышел из группы, то его роль, связанная с групповой, должна отозваться, но если она
     уже была в одном из неактивных состояний, то никаких ошибок возникнуть не должно"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, code=DEFAULT_WORKFLOW, group_code=DEFAULT_WORKFLOW)
    for state in ROLE_STATE.ALL_STATES:
        is_active = state in ROLE_STATE.ACTIVE_STATES
        # используем state в качестве логина, чтобы эти роли отличались друг от друга
        kwargs = {}
        if without_hold is not None:
            kwargs['without_hold'] = without_hold
        group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'},
                                               {'login': state},**kwargs)
        group_role.set_raw_state(state, is_active=is_active)
        user_role = Role.objects.request_role(gandalf, gandalf, simple_system, '', {'role': 'manager'},
                                              {'login': state})
        user_role.set_raw_state(state, is_active=is_active)
    remove_members(fellowship, [gandalf])
    add_members(valinor, [gandalf])
    indirect_roles = Role.objects.filter(user=gandalf, parent__isnull=False)
    direct_roles = Role.objects.filter(user=gandalf, parent__isnull=True)

    assert Transfer.objects.count() == 1
    accept(Transfer.objects.all())

    # Все indirect_roles изначально находятся в granted.
    # Если родительская групповая роль была активна, то персональная должна отложиться.
    # Во всех остальных случаях персональная остаётся в том же состоянии, что и была.
    # Особенность теста в том, что он проверяет и те состояния, которых не может существовать,
    # например, если родительская роль declined, то у неё не может быть granted дочерней роли
    expected_mapping = {
        state: onhold_state if state in ROLE_STATE.ACTIVE_RETURNABLE_STATES else 'granted'
        for state in ROLE_STATE.ALL_STATES
    }
    assert dict(indirect_roles.values_list('parent__state', 'state')) == expected_mapping
    # granted перешла в need_request, остальные остались как были
    stats = dict(direct_roles.values('state').annotate(x=Count('state')).values_list('state', 'x').order_by('x'))
    expected_stats = {}
    for state in ROLE_STATE.ALL_STATES:
        if state == 'granted':
            continue  # granted не будет в выборке
        elif state == 'need_request':
            count = 2
        else:
            count = 1
        expected_stats[state] = count
    assert stats == expected_stats


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_user_ref_role_is_put_on_hold_on_leave(simple_system, arda_users, department_structure, without_hold, onhold_state):
    """without_hold пропагируется на все связанные роли"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    fellowship_count = fellowship.members.count()
    add_perms_by_role('responsible', frodo, simple_system)

    set_workflow(simple_system, group_code=dedent("""
    approvers = []
    if role['role'] == 'manager':
        ref_roles = [{'system': 'simple', 'role_data': {'role': 'poweruser'}}]
    """))

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None, **kwargs)
    personal_role = Role.objects.request_role(frodo, gandalf, simple_system, '', {'role': 'manager'}, None)
    # число ролей - это число членов группы x 2 + две групповые (базовая и связанная) + личная
    # role = fellowship.roles.select_related('group', 'user', 'system', 'node').get()
    assert Role.objects.select_related('user', 'group', 'node', 'system').filter(state='granted').count() == fellowship_count*2 + 3
    moving_users = [arda_users.boromir, arda_users.gandalf]
    remove_members(fellowship, moving_users)
    add_members(valinor, moving_users)

    # теперь число ролей - это число членов группы-2, так как двое вышли, плюс одна групповая.
    # личная пока в granted, но при разрешении transfer-а может перейти в need_request.
    stats = dict(Role.objects.values('state').annotate(x=Count('state')).values_list('state', 'x').order_by('x'))
    assert stats == {
        'granted': fellowship_count*2 - 4 + 2 + 1,
        onhold_state: 4,
    }
    hold = Role.objects.filter(user__in=moving_users, state=onhold_state)
    assert hold.count() == 4

    if onhold_state == 'onhold':
        assert_action_chain(hold[0], ['request', 'approve', 'first_add_role_push', 'grant', 'hold'])
    else:
        assert_action_chain(hold[0], [
            'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'remove',
        ])
    assert Transfer.objects.count() == 2
    assert Transfer.objects.filter(parent=None).count() == 2

    accept(Transfer.objects.all())
    stats = dict(Role.objects.values('state').annotate(x=Count('state')).values_list('state', 'x').order_by('x'))
    assert stats == {
        'need_request': 1,
        'granted': fellowship_count*2 - 4 + 2,
        onhold_state: 4,
    }

    personal_role = refresh(personal_role)
    assert personal_role.state == 'need_request'
    assert_action_chain(personal_role, [
        'request', 'apply_workflow', 'approve', 'grant', 'ask_rerequest',
    ])


def test_user_role_is_added_on_join(simple_system, arda_users, department_structure):
    """Если пользователь вступил в группу, ему должна быть выдана групповая роль, даже если у него уже была такая же
    персональная"""
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    personal_role = Role.objects.request_role(gandalf, gandalf, simple_system, '', {'role': 'manager'}, None)
    remove_members(fellowship, [gandalf])
    add_members(valinor, [gandalf])
    fellowship_count = fellowship.members.count()
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.select_related('user', 'group').get()
    transfer.accept(bypass_checks=True)

    personal_role = refresh(personal_role)
    assert personal_role.state == 'need_request'
    assert_action_chain(personal_role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest',
    ])
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    # число ролей - это число членов группы + одна групповая, личная - в need_request
    assert Role.objects.filter(state='granted').count() == fellowship_count + 1
    joining_users = [arda_users.gandalf, arda_users.varda]
    add_members(fellowship, joining_users)
    # теперь число ролей - это число членов группы + одна групповая, плюс двое вступивших, личная всё ещё в need_request
    assert Role.objects.filter(state='granted').count() == fellowship_count + 1 + 2
    new_roles = Role.objects.filter(user__in=joining_users, parent=group_role)
    assert new_roles.count() == 2
    assert new_roles.filter(state='granted').count() == 2


def test_user_gets_only_active_group_roles_on_join(simple_system, arda_users, department_structure):
    """Если пользователь вступил в группу, ему должна выдаться персональная роль по групповой, но только если эта
    групповая роль активна"""
    # arrange
    frodo = arda_users.frodo
    sauron = arda_users.sauron
    fellowship = department_structure.fellowship
    set_workflow(simple_system, code=DEFAULT_WORKFLOW, group_code=DEFAULT_WORKFLOW)

    for state in ROLE_STATE.ALL_STATES:
        user_role = Role.objects.request_role(sauron, sauron, simple_system, '', {'role': 'manager'},
                                                  {'login': state})
        user_role.set_raw_state(state)

        group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'},
                                               {'login': state})
        group_role.set_raw_state(state)

    # act
    add_members(fellowship, [sauron])

    # assert
    user_roles = Role.objects.filter(user=sauron, parent__isnull=True)
    personal_roles = Role.objects.filter(user=sauron, parent__isnull=False)
    assert set(user_roles.values_list('state', flat=True)) == ROLE_STATE.ALL_STATES
    assert list(personal_roles.values_list('state', flat=True)) == (
        ['granted'] * len(ROLE_STATE.ACTIVE_RETURNABLE_STATES)
    )
    expected_parent_states = sorted(ROLE_STATE.ACTIVE_RETURNABLE_STATES)
    assert sorted(personal_roles.values_list('parent__state', flat=True)) == expected_parent_states


def test_preserve_roles_on_user_leavejoin_if_they_are_applicable(simple_system, arda_users, department_structure):
    """Если человек вышел из одной группы и одновременно вступил в другую, и обе они являются потомками третьей, на
    которую выдана роль, то мы должны сохранить персональные роли, выданные по групповой, выданной
    на эту третью группу"""

    # выдадим роль на средиземье
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    Role.objects.request_role(frodo, department_structure.earth, simple_system, '', {'role': 'admin'}, None)

    # переместим gandalf из fellowship в shire
    shire = department_structure.shire
    fellowship = department_structure.fellowship

    # сначала добавим в новую группу
    assert gandalf.roles.count() == 1
    gandalf_role = gandalf.roles.get()
    assert gandalf_role.state == 'granted'
    add_members(shire, [gandalf])
    assert gandalf.roles.count() == 1
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'

    # затем удалим из старой
    remove_members(fellowship, [gandalf])
    assert gandalf.roles.count() == 1
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'


@pytest.mark.parametrize('review_on_relocate_policy', REVIEW_ON_RELOCATE_POLICIES)
@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_group_is_moved(simple_system, arda_users, department_structure, review_on_relocate_policy,
                        without_hold, onhold_state):
    """Если группа перемещена, то нужно проставить этим групповым ролям статус "нужен перезапрос"
    """

    simple_system.review_on_relocate_policy = review_on_relocate_policy
    simple_system.save()
    if review_on_relocate_policy == 'review':
        expected_state = 'need_request'
    else:
        expected_state = 'granted'

    frodo = arda_users.frodo
    sam = arda_users.sam
    legolas = arda_users.legolas
    sauron = arda_users.sauron
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    middle_earth = department_structure.earth
    middle_earth_count = middle_earth.get_unique_memberships_count()
    associations = department_structure.associations
    associations_count = associations.get_unique_memberships_count()
    fellowship = department_structure.fellowship
    fellowship_count = fellowship.get_unique_memberships_count()

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold

    # выдадим общую групповую роль на middle_earth
    Role.objects.request_role(frodo, middle_earth, simple_system, '', {'role': 'manager'}, None, **kwargs)
    assert Role.objects.filter(state='granted').count() == middle_earth_count + 1

    # выдадим более частную групповую роль на associations
    Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'poweruser'}, None, **kwargs)
    assert Role.objects.filter(state='granted').count() == middle_earth_count + associations_count + 2

    # выдадим совсем частную роль на fellowship
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, None, **kwargs)
    expected_count = Role.objects.filter(state='granted').count()
    assert expected_count == middle_earth_count + associations_count + fellowship_count + 3

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type,
        external_id=505,
    )
    fellowships.save()

    # переместим группу fellowship-of-the-ring в группу fellowships
    move_group(fellowship, fellowships)
    fellowship = refresh(fellowship)

    assert Transfer.objects.count() == fellowship_count + 1
    assert Transfer.objects.filter(state='undecided').count() == Transfer.objects.count()
    accept(Transfer.objects.all())

    # ожидаемый результат манипуляций:
    # пользовательские роли, связанные с группой middle_earth, остаются в тех же состояниях, в каких находились,
    # групповые роли тоже
    # пользовательские роли, связанные с группой associations, отложены, групповая роль не отозвана
    # (так как группа associations никуда не делась)
    # пользовательские роли, связанные с группой fellowship, не отозваны
    # групповая роль группы fellowship привязана к новой группе moved_fellowship и переведена в статус need_request
    assert sam.roles.filter(parent__group=middle_earth).count() == 1
    middle_earth_role = sam.roles.select_related('parent').get(parent__group=middle_earth)
    assert middle_earth_role.state == 'granted'
    assert middle_earth_role.parent.state == 'granted'
    sam_associations_role = sam.roles.select_related('parent').get(parent__group=associations)
    assert sam_associations_role.state == onhold_state
    assert sam_associations_role.parent.state == 'granted'
    # sauron входит в associations напрямую, роль не отозвана
    sauron_associations_role = sauron.roles.get(parent__group=associations)
    assert sauron_associations_role.state == 'granted'
    assert sam.roles.filter(parent__group=fellowship).count() == 1
    sam_fellowship_role = sam.roles.select_related('parent').get(parent__group=fellowship)
    assert sam_fellowship_role.state == 'granted'
    assert sam_fellowship_role.parent.state == expected_state
    if expected_state == 'need_request':
        assert_action_chain(sam_fellowship_role.parent, ['request', 'apply_workflow', 'approve', 'grant', 'ask_rerequest'])
        ask_rerequest_action = sam_fellowship_role.parent.actions.get(action='ask_rerequest')
        expected_message = (
            'Необходимо перезапросить роль в связи с перемещением группы '
            'из "/Средиземье/Объединения/Братство кольца/" в "/Средиземье/Братства/Братство кольца/"'
        )
        assert ask_rerequest_action.comment == expected_message
    else:
        assert_action_chain(sam_fellowship_role.parent, ['request', 'apply_workflow', 'approve', 'grant', 'keep_granted'])
        keep_action = sam_fellowship_role.parent.actions.get(action='keep_granted')
        expected_message = (
            'Роль не перезапрошена, так как в системе отключен пересмотр при смене подразделения. '
            'Группа была перемещена из '
            '"/Средиземье/Объединения/Братство кольца/" в "/Средиземье/Братства/Братство кольца/"'
        )
        assert keep_action.comment == expected_message
    legolas_fellowship_role = legolas.roles.get(parent__group=fellowship)
    assert legolas_fellowship_role.state == 'granted'


@pytest.mark.parametrize('review_on_relocate_policy', REVIEW_ON_RELOCATE_POLICIES)
def test_parentless_role_needs_request_on_group_move(simple_system, arda_users, department_structure,
                                                     review_on_relocate_policy):
    """
    Если группа перемещена, то у входящих в неё пользователей parentless-роли должны перейти в need_request
    """

    simple_system.review_on_relocate_policy = review_on_relocate_policy
    simple_system.save()
    if review_on_relocate_policy == 'review':
        expected_state = 'need_request'
    else:
        expected_state = 'granted'

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'})

    middle_earth = department_structure.earth
    fellowship = department_structure.fellowship
    fellowship_count = fellowship.members.count()

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type,
        external_id=505,
    )
    fellowships.save()

    # переместим группу fellowship-of-the-ring в группу fellowships
    move_group(fellowship, fellowships)

    assert Transfer.objects.count() == 1 + fellowship_count
    assert Transfer.objects.filter(type='user_group').select_related('parent').order_by('?').first().parent.type == 'group'
    stats = dict(Transfer.objects.values('type').annotate(x=Count('type')).values_list('type', 'x'))
    assert stats == {
        'group': 1,
        'user_group': fellowship_count,
    }

    assert Transfer.objects.filter(state='undecided').count() == Transfer.objects.count()
    accept(Transfer.objects.all())

    # Роль должна перейти в need_request тогда и только тогда, когда включен перезапрос, политика системы подходящая
    # и transfer принят.
    role = refresh(role)
    assert role.state == expected_state
    if expected_state == 'need_request':
        assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest'])
        ask_rerequest_action = role.actions.get(action='ask_rerequest')
        expected_message = (
            'Необходимо перезапросить роль в связи с перемещением группы пользователя из '
            '"/Средиземье/Объединения/Братство кольца/" в "/Средиземье/Братства/Братство кольца/"'
        )
        assert ask_rerequest_action.comment == expected_message
        assert ask_rerequest_action.transfer.user_id == frodo.id
    else:
        assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'keep_granted'])
        keep_granted_action = role.actions.get(action='keep_granted')
        expected_message = (
            'Роль не перезапрошена, так как в системе отключен '
            'пересмотр при смене подразделения. Группа пользователя была перемещена из '
            '"/Средиземье/Объединения/Братство кольца/" в "/Средиземье/Братства/Братство кольца/"'
        )
        assert keep_granted_action.comment == expected_message


@pytest.mark.parametrize('review_on_relocate_policy', REVIEW_ON_RELOCATE_POLICIES)
def test_subgroup_roles_are_rerequested_on_group_move(simple_system, arda_users, department_structure,
                                                      review_on_relocate_policy):
    """Проверяем, что при перемещении группы перезапрашиваются также роли дочерних групп"""

    simple_system.review_on_relocate_policy = review_on_relocate_policy
    simple_system.save()
    if review_on_relocate_policy == 'review':
        expected_state = 'need_request'
    else:
        expected_state = 'granted'

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    middle_earth = department_structure.earth
    associations = department_structure.associations
    fellowship = department_structure.fellowship

    Role.objects.request_role(frodo, middle_earth, simple_system, '', {'role': 'manager'}, {})
    Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'poweruser'}, {})
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {})

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type
    )
    fellowships.save()

    # переместим группу associations вместе с подгруппой fellowship в группу fellowships
    move_group(associations, fellowships)
    associations = refresh(associations)
    fellowship = refresh(fellowship)
    accept(Transfer.objects.all())

    # ожидаемый результат манипуляций:
    # пользовательские роли, связанные с группой middle_earth, остаются в прежних состояниях, групповая роль тоже
    # пользовательские роли, связанные с группой associations, остаются в прежних состояниях
    # пользовательские роли, связанные с группой fellowship, остаются в прежних состояниях
    # групповая роль associations привязана к новой группе moved_associations, находится в статусе need_request
    # групповая роль fellowships привязана к той же группе, находится в статусе need_request
    assert legolas.roles.filter(parent__group=middle_earth).count() == 1
    middle_earth_role = legolas.roles.select_related('parent').get(parent__group=middle_earth)
    assert middle_earth_role.state == 'granted'
    assert middle_earth_role.parent.state == 'granted'
    associations_roles = legolas.roles.filter(parent__group=associations)
    assert associations_roles.count() == 1
    associations_role = associations_roles.select_related('parent').get()
    assert associations_role.state == 'granted'
    assert associations_role.parent.state == expected_state
    fellowship_roles = legolas.roles.filter(parent__group=fellowship)
    assert fellowship_roles.count() == 1
    fellowship_role = fellowship_roles.select_related('parent').get()
    assert fellowship_role.state == 'granted'
    assert fellowship_role.parent.state == expected_state


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_only_granted_roles_are_rerequested_on_group_move(simple_system, arda_users, department_structure,
                                                          without_hold, onhold_state):
    """Проверим, что при перемещении выдаются только активные роли нового местоположения, а в статус need_request
    переводятся только те роли старого местоположения, которые находились в статусе 'granted'."""
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    sam = arda_users.sam
    sauron = arda_users.sauron
    states_amount = len(ROLE_STATE.ALL_STATES)
    # чтобы запрашивать роли на вышестоящие группы, в которых frodo не является ответственным
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    middle_earth = department_structure.earth
    associations = department_structure.associations
    fellowship = department_structure.fellowship

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type,
        external_id=505,
    )
    fellowships.save()

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold

    for state in ROLE_STATE.ALL_STATES:
        is_active = state in ROLE_STATE.ACTIVE_STATES
        # используем state в качестве логина, чтобы эти роли отличались друг от друга
        for group, role_slug in [(middle_earth, 'manager'), (associations, 'poweruser'), (fellowship, 'superuser'),
                                 (fellowships, 'admin')]:
            group_role = Role.objects.request_role(frodo, group, simple_system, '', {'role': role_slug},
                                                   {'login': state}, **kwargs)
            group_role.set_raw_state(state, is_active=is_active)

    # переместим группу fellowship-of-the-ring в группу fellowships
    move_group(fellowship, fellowships)
    fellowship = refresh(fellowship)
    accept(Transfer.objects.all())

    # ожидаемый результат манипуляций:
    # пользовательские роли, связанные с группой middle_earth, остаются в прежних состояниях (т.е. granted),
    # групповые роли тоже остаются в прежних состояниях
    # пользовательские роли, связанные с группой associations, откладываются,
    # групповые роли остаются в прежних состояниях
    # групповые и пользовательские роли, связанные с группой fellowship, остаются в прежних состояниях
    # по активным ролям, связанным с группой fellowships (с s на конце) выдаются персональные роли
    # групповая роль fellowship привязана к новой группе moved_fellowship, находится в статусе need_request
    earth_roles = legolas.roles.filter(parent__group=middle_earth)
    sauron_associations_role = sauron.roles.filter(parent__group=associations)
    sam_associtiations_roles = sam.roles.filter(parent__group=associations)
    fellowship_roles = legolas.roles.filter(parent__group=fellowship)
    fellowships_roles = legolas.roles.filter(parent__group=fellowships)
    assert earth_roles.count() == states_amount
    assert dict(earth_roles.values_list('parent__state', 'state')) == {key: 'granted' for key in ROLE_STATE.ALL_STATES}
    assert set(middle_earth.roles.values_list('state', flat=True)) == ROLE_STATE.ALL_STATES
    # sauron состоит в associations напрямую, его роли по роли в associations не отозваны
    assert list(sauron_associations_role.values_list('state', flat=True)) == ['granted'] * states_amount
    # а вот sam нет, его роли, выданные по активным групповым ролям, отозваны, а по неактивным остались в том же
    # состоянии, в каком и были, то есть в granted
    expected_mapping = {
        key: onhold_state if key in ROLE_STATE.ACTIVE_RETURNABLE_STATES else 'granted'
        for key in ROLE_STATE.ALL_STATES
    }
    assert dict(sam_associtiations_roles.values_list('parent__state', 'state')) == expected_mapping
    assert set(associations.roles.values_list('state', flat=True)) == ROLE_STATE.ALL_STATES
    assert fellowship_roles.count() == len(ROLE_STATE.ALL_STATES)
    assert fellowship.roles.count() == len(ROLE_STATE.ALL_STATES)
    assert list(fellowship_roles.values_list('state', flat=True)) == ['granted'] * states_amount
    # групповая роль, находившаяся в состоянии granted, перешла в need_request, остальные остались в прежних состояниях
    expected_list = sorted(list(ROLE_STATE.ALL_STATES - {'granted'}) + ['need_request'])
    assert sorted(fellowship.roles.values_list('state', flat=True)) == expected_list
    # создались (и выдались) только персональные роли по активным групповым ролям новой родительской группы
    assert list(fellowships_roles.values_list('state', flat=True)) == ['granted'] * len(ROLE_STATE.ACTIVE_RETURNABLE_STATES)
    expected_parent_states = sorted(ROLE_STATE.ACTIVE_RETURNABLE_STATES)
    assert sorted(fellowships_roles.values_list('parent__state', flat=True)) == expected_parent_states


def test_user_role_is_deprived_if_it_cannot_be_granted(generic_system, arda_users, department_structure):
    """Если пользовательскую роль, созданную по групповой, невозможно выдать, то она должна отозваться"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(generic_system, group_code='approvers = []')
    group = Group.objects.get(slug='fellowship-of-the-ring')
    members_count = group.members.count()

    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = PluginFatalError(1, 'weird error', {'hello': 'world'})
        role = Role.objects.request_role(frodo, group, generic_system, comment='', data={'role': 'manager'},
                                         fields_data={})

    assert Role.objects.count() == 1 + members_count
    user_roles = Role.objects.filter(group=None)
    assert user_roles.count() == members_count
    assert user_roles.filter(state='failed').count() == members_count
    role = refresh(role)
    assert role.state == 'granted'
    userrole = user_roles.get(user=legolas)
    assert_action_chain(userrole, ['request', 'approve', 'first_add_role_push', 'fail'])

    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Generic система. Новая роль'


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_deprive_group_role(simple_system, arda_users, department_structure, without_hold, onhold_state):
    """Проверяем, что при удалении группы отзывается групповая роль"""
    frodo = arda_users.frodo
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {}, **kwargs)
    assert fellowship.roles.count() == 1

    fellowship.mark_depriving()
    fellowship = refresh(fellowship)
    fellowship.deprive()

    role = refresh(role)
    assert role.state == onhold_state
    if onhold_state == 'onhold':
        assert role.expire_at.date() == timezone.now().date() + timezone.timedelta(days=7)
    else:
        assert role.expire_at is None


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_onhold_ref_roles_depriving_on_group_role_depriving(simple_system, arda_users, department_structure,
                                                            without_hold, onhold_state):
    """Проверяем, что при отзыве групповой роли отзывается отложенная персональная"""
    frodo, sam = arda_users.frodo, arda_users.sam
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {}, **kwargs)

    frodo_role = frodo.roles.get()
    sam_role = sam.roles.get()

    remove_members(fellowship, [frodo])

    frodo_role.refresh_from_db()
    assert frodo_role.state == onhold_state
    sam_role.refresh_from_db()
    assert sam_role.state == 'granted'

    group_role.set_state('depriving')

    sam_role.refresh_from_db()
    assert sam_role.state == 'deprived'
    frodo_role.refresh_from_db()
    assert frodo_role.state == 'deprived'


def test_grant_role_in_system_just_once(generic_system, arda_users, department_structure):
    """Если у пользователя уже есть роль в системе, а мы выдаём ему ещё одну автоматически – то информацию
    в систему мы должны отправлять только один раз"""

    frodo = arda_users.frodo
    set_workflow(generic_system, code='approvers=[]', group_code='approvers = []')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    clear_mailbox()
    with patch.object(generic_system.plugin.__class__, '_post_data', frodo_forbid):
        Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    frodo_roles = Role.objects.filter(user=frodo)
    role1, role2 = frodo_roles
    assert role1.state == 'granted'
    assert role2.state == 'granted'
    assert role1.system_specific == role2.system_specific
    assert role1.system_specific == {'token': 'whoa!'}
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'
    assert_contains([
        'Группа "Братство кольца", в которой вы являетесь ответственным, '
        'получила новую роль в системе "Generic система":',
        'Роль: Менеджер',
    ], message.body)


def test_grant_role_in_system_just_once_even_for_empty_fields_data(generic_system, arda_users, department_structure):
    """Если у пользователя уже есть роль в системе, а мы выдаём ему ещё одну автоматически – то информацию
    в систему мы должны отправлять только один раз. Данный тест проверяет случай, когда роли содержат эквивалентные,
    но пустые fields_data: {} и None"""

    frodo = arda_users.frodo
    set_workflow(generic_system, code='approvers=[]', group_code='approvers = []')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    clear_mailbox()
    with patch.object(generic_system.plugin.__class__, '_post_data', frodo_forbid):
        Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, {})  # NB: {}, а не None
    frodo_roles = Role.objects.filter(user=frodo)
    role1, role2 = frodo_roles
    assert role1.state == 'granted'
    assert role2.state == 'granted'
    assert role1.system_specific == role2.system_specific
    assert role1.system_specific == {'token': 'whoa!'}
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'
    assert_contains([
        'Группа "Братство кольца", в которой вы являетесь ответственным, '
        'получила новую роль в системе "Generic система":',
        'Роль: Менеджер',
    ], message.body)


def test_grant_role_in_system_just_once_vice_versa(generic_system, arda_users, department_structure):
    """Если у пользователя уже есть персональная роль в системе, а он запрашивает такую же, но персональную,
     то информацию в систему мы должны отправлять только один раз"""

    frodo = arda_users.frodo
    set_workflow(generic_system, code='approvers=[]', group_code='approvers = []')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    clear_mailbox()
    with patch.object(generic_system.plugin.__class__, '_post_data', frodo_forbid):
        Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
    frodo_roles = Role.objects.filter(user=frodo)
    role1, role2 = frodo_roles
    assert role1.state == 'granted'
    assert role2.state == 'granted'
    assert role1.system_specific == role2.system_specific
    assert role1.system_specific == {'token': 'whoa!'}
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'
    assert_contains([
        'Вы получили новую роль в системе "Generic система":',
        'Роль: Менеджер'
    ], message.body)


def test_grant_role_in_system_just_once_for_two_group_roles(generic_system, arda_users, department_structure):
    """Проверка, что если пользователь получил две одинаковых персональных роли по двум группам, то в систему
    по этому поводу мы сходим только один раз"""

    shire = Group.objects.get(slug='the-shire')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    frodo = arda_users.frodo
    set_workflow(generic_system, group_code=DEFAULT_WORKFLOW)

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    with patch.object(generic_system.plugin.__class__, '_post_data', frodo_forbid):
        Role.objects.request_role(frodo, shire, generic_system, '', {'role': 'manager'}, None)
    roles = Role.objects.filter(user=frodo)
    assert roles.count() == 2
    assert roles.filter(state='granted').count() == 2


def test_double_add_role_prevention_logic_filters_out_inactive_roles(generic_system, arda_users, department_structure):
    """Проверка, что если пользователь получил две одинаковых персональных роли по двум группам, то в систему
    по этому поводу мы сходим только один раз, однако логика по исключению второй отправки учитывает только активные
    роли, за исключением depriving"""

    shire = department_structure.shire
    frodo = arda_users.frodo
    set_workflow(generic_system, group_code=DEFAULT_WORKFLOW)

    def forbid(*args, **kwargs):
        raise PluginFatalError(1, 'Cannot add role')

    for state in ROLE_STATE.ALL_STATES:
        Role.objects.all().delete()
        # создадим первую роль
        raw_make_role(frodo, generic_system, {'role': 'manager'}, None, state=state)
        # запросим такую же роль
        with patch.object(generic_system.plugin.__class__, '_post_data', forbid):
            shire_role = Role.objects.request_role(frodo, shire, generic_system, '', {'role': 'manager'}, None)
            assert Role.objects.filter(user=frodo).count() == 2
            role = shire_role.refs.get(user=frodo)
            if state in ROLE_STATE.ACTIVE_RETURNABLE_STATES:
                # если первая роль среди активных,
                # то повторный запрос не приводит к пушу, роль просто переводится в granted
                assert role.state == 'granted'
                assert_action_chain(role, ['request', 'approve', 'grant'])
                grant_action = role.actions.get(action='grant')
                expected_comment = 'Роль подтверждена и добавлена в систему.: Роль выдана без оповещения системы'
                assert grant_action.comment == expected_comment
                shire_role.deprive_or_decline(frodo)
                role = refresh(role)
                assert_action_chain(role, [
                    'request', 'approve', 'grant', 'deprive', 'remove',
                ])
                remove_action = role.actions.get(action='remove')
                assert remove_action.comment == 'Роль удалена из системы.: Роль удалена без оповещения системы'
            else:
                # если первая роль среди неактивных, то происходит попытка пуша,
                # при этом "система" в лице мока отвечает нам неверно, роль переходит в failed
                assert role.state == 'failed'
                assert_action_chain(role, ['request', 'approve', 'first_add_role_push', 'fail'])


def test_personal_roles_are_created_just_once_on_rerequest(simple_system, arda_users, department_structure):
    """Проверим, что при перезапросе групповой роли персональные роли второй раз не создаются"""

    shire = department_structure.shire
    frodo = arda_users.frodo
    count = shire.members.count()
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, shire, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.refs.count() == count
    role.set_state('need_request')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    assert role.refs.count() == count
    assert_action_chain(role.refs.order_by('?').first(), ['request', 'approve', 'first_add_role_push', 'grant'])


def test_personal_roles_are_created_just_once(simple_system, arda_users, department_structure):
    """Проверим, что при переходе в granted персональные роли создаются только один раз.
    Переход в granted может быть не мгновенным, роли могут застрять в approved. В тесте это
    эмулируется через отсылку задач в память"""

    shire = department_structure.shire
    count = shire.members.count()
    set_workflow(simple_system, code='approvers = ["legolas"]', group_code=DEFAULT_WORKFLOW)
    role_node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'manager'})
    with ignore_tasks():
        role = Role.objects.create_role(shire, simple_system, role_node, None, save=True)
        role.set_raw_state('granted')
        role2 = copy.deepcopy(role)
        role.request_refs()  # персольные роли остаются в approved
    role2.request_refs()
    role = refresh(role)
    assert role.refs.count() == count


def test_do_not_grant_role_for_inactive_users(simple_system, arda_users, department_structure):
    """Проверка, что уволенному пользователю роль не выдаётся"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    legolas.is_active = False
    legolas.save()
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    assert role.refs.filter(user__username='legolas').count() == 0
    assert role.refs.filter(user__username='gandalf').count() == 1


def test_adding_personal_roles_in_broken_system(simple_system, arda_users, department_structure):
    """Проверим, что добавление пользователя в группу не добавит ему ролей в сломанных системах,
    но и не сломает синхронизацию"""

    frodo = arda_users.frodo
    varda = arda_users.varda
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    fellowship = department_structure.fellowship
    fellowship_count = fellowship.members.count()
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, comment='',
                                           data={'role': 'manager'}, fields_data={})
    simple_system.is_broken = True
    simple_system.save()

    add_members(fellowship, [varda])
    # число ролей не изменилось, это число членов группы + 1 групповая роль
    assert Role.objects.filter(state='granted').count() == fellowship_count + 1
    new_roles = Role.objects.filter(user__username='varda', parent=group_role)
    assert new_roles.count() == 0


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_removing_personal_roles_in_broken_system(simple_system, arda_users, department_structure,
                                                  without_hold, onhold_state):
    """Проверим, что удаление пользователя из группы не удалит его роли в сломанных системах,
    но и не сломает синхронизацию"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    fellowship = department_structure.fellowship
    fellowship_count = fellowship.members.count()

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, comment='',
                                           data={'role': 'manager'}, fields_data={}, **kwargs)
    simple_system.is_broken = True
    simple_system.save()

    remove_members(fellowship, [arda_users.gandalf])
    if onhold_state == 'onhold':
        # число ролей - это число членов группы + 1 групповая роль минус Гендальф
        assert Role.objects.filter(state='granted').count() == fellowship_count
        assert Role.objects.filter(state=onhold_state).count() == 1
    else:
        # Отозвать роль в сломанной системе нельзя
        # число ролей - это число членов группы + 1 групповая роль
        assert Role.objects.filter(state='granted').count() == fellowship_count + 1
        assert Role.objects.filter(state=onhold_state).count() == 0
    gandalf_roles = Role.objects.filter(user=gandalf, parent=group_role)
    assert gandalf_roles.count() == 1
    assert gandalf_roles.get().state == ('granted' if onhold_state == 'deprived' else onhold_state)


def test_transfer_is_expired_if_same_was_decided(simple_system, arda_users, department_structure):
    """Проверим, что если есть два перемещения одного и того же объекта, то при разрешении любого из них все остальные
    undecided перемещения переходят в expired"""

    frodo = arda_users.frodo
    varda = arda_users.varda
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    fellowship_count = fellowship.members.count()
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'})
    add_members(fellowship, [varda])
    remove_members(valinor, [varda])

    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.get()
    assert transfer.user_id == varda.id
    assert transfer.state == 'undecided'

    assert Role.objects.filter(state='granted').count() == fellowship_count + 2
    new_roles = Role.objects.filter(user__username='varda', parent=group_role)
    assert new_roles.count() == 1

    move_group(fellowship, valinor.parent)
    assert Transfer.objects.count() == fellowship.members.count() + 2
    varda_transfers = Transfer.objects.filter(user=varda)
    assert varda_transfers.count() == 2
    assert varda_transfers.filter(state='undecided').count() == 2
    assert dict(varda_transfers.annotate(x=Count('type')).values_list('type', 'x')) == {
        'user': 1,
        'user_group': 1,
    }

    transfer = varda_transfers.select_related('user', 'group').order_by('?').first()
    accept([transfer])
    other = varda_transfers.exclude(pk=transfer.pk).get()
    assert other.state == 'expired'


def test_user_group_transfers_are_created_only_once(simple_system, arda_users, department_structure):
    """
    Дочерние перемещения создаются только один раз
    """

    varda = arda_users.varda

    middle_earth = department_structure.earth
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    fellowship_count = fellowship.members.count()

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type,
        external_id=505,
    )
    fellowships.save()

    # переместим группу fellowship-of-the-ring в группу fellowships
    move_group(fellowship, fellowships)

    assert Transfer.objects.count() == 1 + fellowship_count
    stats = dict(Transfer.objects.values('type').annotate(x=Count('type')).values_list('type', 'x'))
    assert stats == {
        'group': 1,
        'user_group': fellowship_count,
    }

    # переместим пользователя внутрь перемещённой группы и убедимся, что для него не создалось user_group перемещения
    # но создалось user перемещение
    change_department(varda, valinor, fellowship)
    assert Transfer.objects.count() == 1 + fellowship_count + 1

    assert Transfer.objects.filter(state='undecided').count() == 1 + fellowship_count + 1
    stats = dict(Transfer.objects.values('type').annotate(x=Count('type')).values_list('type', 'x'))
    assert stats == {
        'group': 1,
        'user_group': fellowship_count,
        'user': 1,
    }


@pytest.mark.parametrize('without_hold,onhold_state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_sync_puts_on_hold_only_granted_roles(simple_system, arda_users, department_structure, without_hold, onhold_state):
    """Проверим, что мы пытаемся перевести в onhold только granted роли"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    boromir = arda_users.boromir
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    fellowship_count = fellowship.members.count()
    add_perms_by_role('responsible', frodo, simple_system)

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None, **kwargs)
    user_role = group_role.refs.get(user=gandalf)
    user_role2 = group_role.refs.get(user=boromir)
    user_role3 = group_role.refs.get(user=frodo)

    user_role3.set_raw_state(ROLE_STATE.DEPRIVING_VALIDATION)

    remove_members(fellowship, [gandalf, frodo])
    add_members(valinor, [gandalf])

    user_role = refresh(user_role)
    assert user_role.state == onhold_state

    if without_hold:
        assert_action_chain(user_role, [
            'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push', 'remove',
        ])
        # Роль в статусе depriving_validation, поэтому к ней экшены не добавляются
        assert_action_chain(user_role3, [
            'request', 'approve', 'first_add_role_push', 'grant',
        ])
    else:
        with ignore_tasks():
            # отозвали роль, но система не ответила, и роль осталась в depriving
            user_role.deprive_or_decline(gandalf)

        assert_action_chain(user_role, ['request', 'approve', 'first_add_role_push', 'grant', 'hold', 'deprive'])
        user_role = refresh(user_role)
        assert user_role.state == 'depriving'

        remove_members(fellowship, [boromir])
        add_members(valinor, [boromir])

        # синхронизация случилась, granted роль перешла в onhold:
        user_role2 = refresh(user_role2)
        assert user_role2.state == onhold_state

        # дополнительных попыток отозвать роль не было. доотзывом ролей занимается допинывалка depriving ролей,
        # а не синхронизация
        assert_action_chain(user_role, ['request', 'approve', 'first_add_role_push', 'grant', 'hold', 'deprive'])


def test_sync_excludes_approved_roles_from_consideration(simple_system, arda_users, department_structure):
    """Проверим, что если необходимая роль есть в статусе approved, синхронизация её не добавляет второй раз.
    https://st.yandex-team.ru/IDM-4948
    """

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    boromir = arda_users.boromir
    valinor = department_structure.valinor
    add_perms_by_role('responsible', frodo, simple_system)

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    group_role = Role.objects.request_role(frodo, valinor, simple_system, '', {'role': 'manager'}, None)
    assert group_role.refs.filter(user=gandalf).count() == 0

    # игнорим таски, чтобы роль осталась в approved
    with ignore_tasks():
        add_members(valinor, [gandalf])

    assert group_role.refs.filter(user=gandalf).count() == 1
    user_role = group_role.refs.get(user=gandalf)
    assert user_role.state == 'approved'

    # теперь добавляем кого-нибудь, просто чтобы синхронизация сработала
    add_members(valinor, [boromir])

    # и убеждаемся, что роль gandalf-а всё ещё ровно одна
    assert group_role.refs.filter(user=gandalf).count() == 1
    user_role.refresh_from_db()
    assert_action_chain(user_role, ['request', 'approve'])


@pytest.mark.parametrize('without_hold', [None, False, True])
def test_sync_deprives_roles_of_inactive_users(simple_system, arda_users, department_structure, without_hold):
    """Проверим, что роли уволенных пользователей сразу отзываются, а не улетают в onhold"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf

    boromir = arda_users.boromir
    boromir.is_active = False
    boromir.save()

    fellowship = department_structure.fellowship
    add_perms_by_role('responsible', frodo, simple_system)

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None, **kwargs)
    assert group_role.refs.filter(user=boromir).count() == 0
    role = group_role.refs.get(user=gandalf)
    assert role.state == 'granted'

    gandalf.is_active = False
    gandalf.save()

    remove_members(fellowship, [gandalf])

    role = refresh(role)
    assert role.state == 'deprived'


@pytest.mark.parametrize('without_hold,state', [(None, 'onhold'), (False, 'onhold'), (True, 'deprived')])
def test_group_cascade_hold(simple_system, arda_users, department_structure, without_hold, state):
    frodo = arda_users.frodo
    lands = department_structure.lands

    simple_system.request_policy = 'anyone'

    set_workflow(simple_system, group_code=dedent('''
        approvers = []
        if role == {'role': 'admin'}:
            ref_roles = [{
                'system': 'simple',
                'role_data': {'role': 'manager'},
            }]

    '''))

    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    Role.objects.request_role(frodo, lands, simple_system, '', {'role': 'admin'}, None, **kwargs)
    # выдались роли (1 главная и одна ref) на группы, и на всех members (их 5)
    assert [role.state for role in Role.objects.all()] == ['granted'] * (1 + 1 + 5 + 5)

    lands.mark_depriving()
    lands.deprive()
    # все роли перевелись в onhold
    assert [role.state for role in Role.objects.all()] == [state] * (1 + 1 + 5 + 5)


def test_hold_min_expire_date(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    simple_system.request_policy = 'anyone'
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    onhold_ttl = constance.config.IDM_ONHOLD_TTL_DAYS

    fellowship_role = Role.objects.request_role(
        frodo, fellowship, simple_system, '', {'role': 'manager'}, None, ttl_days=onhold_ttl - 1)
    valinor_role = Role.objects.request_role(
        frodo, valinor, simple_system, '', {'role': 'manager'}, None, ttl_days=onhold_ttl + 1)

    # если expire_at был меньше, чем задержка onhold, то он не увеличится
    old_expire_at = fellowship_role.expire_at
    fellowship.mark_depriving()
    fellowship.deprive()
    fellowship_role = refresh(fellowship_role)
    assert fellowship_role.state == 'onhold'
    assert fellowship_role.expire_at == old_expire_at

    # если expire_at был меньше, чем задержка onhold, то он уменьшится
    old_expire_at = valinor_role.expire_at
    valinor.mark_depriving()
    valinor.deprive()
    valinor_role = refresh(valinor_role)
    assert valinor_role.state == 'onhold'
    assert valinor_role.expire_at < old_expire_at


@pytest.mark.parametrize('without_hold,onhold_state', [(False, 'onhold'), (True, 'deprived')])
@pytest.mark.parametrize('state_expected', [
    ('imported', '<ONHOLD_OR_DEPRIVED>'),
    ('granted', '<ONHOLD_OR_DEPRIVED>'),
    ('rerequested', '<ONHOLD_OR_DEPRIVED>'),
    ('need_request', '<ONHOLD_OR_DEPRIVED>'),
    ('review_request', '<ONHOLD_OR_DEPRIVED>'),
    ('created', 'deprived'),
    ('requested', 'declined'),
    ('approved', 'deprived'),
    ('sent', 'deprived'),
])
def test_hold_or_decline_or_deprive(simple_system, arda_users, department_structure,
                                    without_hold, state_expected, onhold_state):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    state, expected = state_expected
    kwargs = {}
    if without_hold is not None:
        kwargs['without_hold'] = without_hold
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None, **kwargs)
    role.set_raw_state(state)
    if state in {'created', 'requested', 'approved', 'sent'}:
        role.is_active = False
        role.save(update_fields=('is_active',))
    fellowship.mark_depriving()
    fellowship.deprive()
    role = refresh(role)
    assert role.state == (expected if expected != '<ONHOLD_OR_DEPRIVED>' else onhold_state)


def test_role_request_with_passport_login(pt1_system, arda_users, department_structure):
    """У персональных ролей паспортный логин может приеезжать из членства"""
    frodo = arda_users.frodo
    for login in ['login1', 'login2']:
        frodo.passport_logins.create(login=login, state='created')
    login = frodo.passport_logins.first()  # пофиг какой

    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()

    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    assert group_role.state == 'granted'
    frodo_role = Role.objects.get(user=frodo)
    assert frodo_role.state == 'awaiting'  # У членства не выбран логин.
    assert not frodo_role.passport_logins.exists()

    GroupMembership.objects.filter(user=frodo, group=group).update(passport_login=login)
    Role.objects.poke_awaiting_roles()
    frodo_role = refresh(frodo_role)
    assert frodo_role.state == 'awaiting'  # Теперь логин определён и прицеплен к роли, но не дореган
    login = refresh(login)
    assert frodo_role.passport_logins.get() == login
    assert not login.is_fully_registered

    login.is_fully_registered = True
    login.save()
    Role.objects.poke_awaiting_roles()
    frodo_role = refresh(frodo_role)
    group_role = refresh(group_role)
    assert group_role.state == 'granted'
    assert frodo_role.state == 'granted'  # Теперь всё ок
    assert frodo_role.node.data == {'project': 'proj1', 'role': 'admin'}
    assert frodo_role.fields_data == {'passport-login': 'login1'}  # Теперь тут есть логин
    assert frodo_role.passport_logins.get() == login
    assert group_role.fields_data is None  # А здесь нет


@pytest.mark.parametrize('failure_reason', ['multiple_logins', 'passport_failure'])
def test_aware_of_memberships_with_passport_logins(pt1_system, arda_users, department_structure, failure_reason):
    pt1_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    pt1_system.save()
    frodo = arda_users.frodo
    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    if failure_reason == 'multiple_logins':
       for login in ['login1', 'login2']:
           frodo.passport_logins.create(login=login, state='created')
       group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    else:
        # "В паспорте есть всевозможные логины, в которые входит 'frodo'"
        with patch('idm.sync.passport.exists', side_effect=lambda login: 'frodo' in login):
            group_role = Role.objects.request_role(
                frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)

    for membership in group.memberships.select_related('passport_login', 'user').iterator():
        if membership.user.username != 'frodo':
            # Всем сгенерировало логины.
            assert membership.passport_login.login == 'yndx-' + membership.user.username
        else:
            # У Фродо что-то пошло не так
            assert membership.passport_login == None
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 2
    message = mail.outbox[0]
    assert message.subject == 'Участие в группе требует указания паспортного логина'
    assert message.to[0] == 'frodo@example.yandex.ru'


def test_aware_of_memberships_already_attached(pt1_system, arda_users, department_structure):
    # Если паспортный логин уже привязан к членству, не трогаем его.
    pt1_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    pt1_system.save()
    frodo = arda_users.frodo
    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    for login in ['login1', 'login2']:
        frodo.passport_logins.create(login=login, state='created')
    login = frodo.passport_logins.first()
    frodo_membership = GroupMembership.objects.get(user=frodo, group=group)
    frodo_membership.passport_login = login
    frodo_membership.save()
    clear_mailbox()
    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    frodo_membership.refresh_from_db()
    assert frodo_membership.passport_login_id == login.id
    # В коннекте отключена рассылка писем
    assert len(mail.outbox) == 1


def test_role_request_with_optional_passport_login(pt1_system, arda_users, department_structure):
    """Если поле не обязательно и логин не привязан, то роль выдастся без него"""
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    for login in ['frodo-login1', 'frodo-login2']:
        frodo.passport_logins.create(login=login, state='created')
    for login in ['gandalf-login1', 'gandalf-login2']:
        gandalf.passport_logins.create(login=login, state='created')
    galdalf_login = gandalf.passport_logins.first()

    group = department_structure.fellowship
    RoleField.objects.filter(type=FIELD_TYPE.PASSPORT_LOGIN).update(is_required=False)
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()

    # Гендальф - парень умный и предусмотрительный
    GroupMembership.objects.filter(user=gandalf, group=group).update(passport_login=galdalf_login)

    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    assert group_role.state == 'granted'
    frodo_role = Role.objects.get(user=frodo)
    gandalf_role = Role.objects.get(user=gandalf)
    assert frodo_role.state == 'granted'  # Фродо уже успел получить свою роль
    assert gandalf_role.state == 'awaiting'  # А вот Гендальфу сначала надо дорегать логин

    galdalf_login.is_fully_registered = True
    galdalf_login.save()
    Role.objects.poke_awaiting_roles()
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'

    assert frodo_role.fields_data is None  # Тут логина нет
    assert group_role.fields_data is None  # И даже в групповой
    assert not frodo_role.passport_logins.exists()  # Фродо тоже не при делах
    assert gandalf_role.fields_data == {'passport-login': 'gandalf-login1'}  # Ай да Гендальф!
    assert gandalf_role.passport_logins.get() == galdalf_login


@pytest.mark.parametrize('passport_login', [None, 'yndx-frodo'])
@pytest.mark.parametrize('is_registered', [True, False])
def test_grant_personal_role_with_no_or_single_passport_login(pt1_system, arda_users, department_structure,
                                                              passport_login, is_registered):
    frodo = arda_users.frodo
    assert not frodo.passport_logins.exists()
    if passport_login:
        frodo.passport_logins.create(login=passport_login, state='created', is_fully_registered=is_registered)
    fellowship = department_structure['fellowship']
    membership = frodo.memberships.get(group=fellowship)
    assert not membership.passport_login
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    fellowship_role = Role.objects.request_role(
        frodo, fellowship, pt1_system, '',{'project': 'proj1', 'role': 'admin'}, None,
    )

    assert fellowship_role.state == 'granted'
    membership.refresh_from_db()
    frodo.refresh_from_db()
    frodo_role = Role.objects.get(user=frodo, parent__group=fellowship)
    assert frodo.passport_logins.count() == 1
    frodo_passport_login = frodo_role.passport_logins.get()
    assert frodo_passport_login.login == 'yndx-{username}'.format(username=frodo.username)
    membership = frodo.memberships.get(group=fellowship)
    assert membership.passport_login_id == frodo_passport_login.id
    state = 'granted' if (is_registered and passport_login) else 'awaiting'
    assert frodo_role.state == state


@pytest.mark.parametrize('is_registered', [True, False])
def test_indirect_membership_with_passport_login(pt1_system, arda_users, department_structure, is_registered):
    frodo = arda_users.frodo
    assert not frodo.passport_logins.exists()
    login = frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=is_registered)
    fellowship = department_structure['fellowship']
    associations = department_structure['associations']
    membership = frodo.memberships.get(group=fellowship)
    assert not membership.passport_login
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    group_role = Role.objects.request_role(
        frodo, associations, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None,
    )

    assert group_role.state == 'granted'
    membership.refresh_from_db()
    frodo.refresh_from_db()
    assert frodo.roles.count() == 1
    frodo_role = frodo.roles.select_related('parent__group').get()
    assert frodo_role.parent.group == associations
    assert frodo_role.passport_logins.get() == login

    assert Role.objects.count() == associations.memberships.count() + 1
    for role in Role.objects.all():
        if role == group_role:
            assert role.state == 'granted'
        elif role == frodo_role:
            assert role.state == ('granted' if is_registered else 'awaiting')
        else:
            assert role.state == 'awaiting'

    roles_before = list(Role.objects.values_list('pk', 'state'))
    Role.objects.request_or_deprive_personal_roles()
    roles_after = list(Role.objects.values_list('pk', 'state'))
    assert roles_before == roles_after


def test_role_request_with_robot(fellowship, check_group_role_request_with_additional_field):
    """Выдача связанных пользовательских ролей с роботом"""
    user = fellowship.members[0]
    user.is_robot = True
    user.save()
    check_group_role_request_with_additional_field(user, 'with_robots')


def test_role_request_with_inheritance(arda_users, department_structure, simple_system):
    """Выдача связанных пользовательских ролей с наследованием"""
    simple_system.request_policy = 'anyone'
    simple_system.save()
    set_workflow(simple_system, 'approvers=[]', 'approvers=[]')
    frodo = arda_users.frodo
    Role.objects.request_role(
        arda_users.frodo,
        department_structure.lands,
        simple_system,
        '',
        {'role': 'manager'},
        None,
        with_inheritance=False,
    )
    assert not frodo.roles.exists()


def test_role_request_external_robot(arda_users, request_role_with_additional_fields):
    """Большинство роботов внешние. При наличии галки with_robots роль должна им быть выдана."""
    frodo = arda_users.frodo
    frodo.is_robot = True
    frodo.affiliation = AFFILIATION.EXTERNAL
    frodo.save()
    request_role_with_additional_fields(with_robots=True, with_external=False)
    assert frodo.roles.exists()

    # Удалим все персональные и запросим через request_applicable_personal_roles
    Role.objects.exclude(parent=None).delete()
    Role.objects.request_applicable_personal_roles()
    assert frodo.roles.exists()

    assert Role.objects.get_inapplicable_external_roles().count() == 0


def test_role_request_with_external(fellowship, check_group_role_request_with_additional_field):
    """Выдача связанных пользовательских ролей с внешними"""
    user = fellowship.members[0]
    user.affiliation = AFFILIATION.EXTERNAL
    user.save()
    check_group_role_request_with_additional_field(user, 'with_external')


@waffle.testutils.override_switch('idm.enable_ignore_failed_roles_on_poke', active=True)
def test_personal_role_rerequest(arda_users, simple_system, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    role0 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role1 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    Role.objects.request_or_deprive_personal_roles()

    # перезапрос, исключающий FAILED роли
    for role in frodo.roles.all():
        role.set_raw_state(ROLE_STATE.FAILED)
    Role.objects.request_or_deprive_personal_roles(retry_failed=False)
    assert frodo.roles.filter(state=ROLE_STATE.FAILED).count() == 2

    # перезапрос, не исключающий FAILED роли
    for role in frodo.roles.all():
        role.set_raw_state(ROLE_STATE.FAILED)
    Role.objects.request_or_deprive_personal_roles(retry_failed=True)
    assert frodo.roles.filter(state=ROLE_STATE.FAILED).count() == 0
    assert frodo.roles.filter(state=ROLE_STATE.GRANTED).count() == 2

    # перезапрос удаленной роли
    assert frodo.roles.count() == 2
    frodo.roles.first().delete()
    assert frodo.roles.count() == 1
    Role.objects.request_or_deprive_personal_roles()
    assert frodo.roles.count() == 2
    assert frodo.roles.filter(state=ROLE_STATE.GRANTED).count() == 2


@pytest.fixture
def check_group_role_request_with_additional_field(fellowship, request_role_with_additional_fields):
    def _check_group_role_request_with_additional_field(user, additional_field):
        members = list(User.objects.filter(
            memberships__group=fellowship,
            memberships__state__in=GROUPMEMBERSHIP_STATE.ACTIVE_STATES,
        ))
        request_role_with_additional_fields(**{additional_field: False})
        assert Role.objects.filter(group=None).count() == len(members) - 1
        assert all(not getattr(role, additional_field) for role in Role.objects.filter(group=None))

        with patch('idm.core.models.Role.request_group_roles') as request_group_roles:
            request_role_with_additional_fields(role='admin', **{additional_field: False})
            Role.objects.request_applicable_personal_roles()
            assert user.id not in request_group_roles.call_args[1]['user_ids']

    return _check_group_role_request_with_additional_field


@pytest.fixture
def request_role_with_additional_fields(arda_users, fellowship, simple_system, default_workflow):
    def _request_role_with_additional_fields(role='manager', **kwargs):
        Role.objects.request_role(
            arda_users.frodo,
            fellowship,
            simple_system,
            '',
            {'role': role},
            None,
            **kwargs
        )

    return _request_role_with_additional_fields


@pytest.mark.parametrize('with_external', [True, False])
@pytest.mark.parametrize('with_robots', [True, False])
@pytest.mark.parametrize('with_inheritance', [True, False])
@pytest.mark.parametrize('without_hold', [True, False])
def test_ref_role_request_with_flags(arda_users, department_structure, simple_system,
                                     with_external, with_robots, with_inheritance, without_hold):
    """Выдача связанных пользовательских ролей с наследованием"""
    simple_system.request_policy = 'anyone'
    simple_system.save()
    set_workflow(simple_system, code=dedent("""
        approvers = []
    """), group_code=dedent("""
        approvers = []
        if role['role'] == 'manager':
            ref_roles = [{'system': 'simple', 'role_data': {'role': 'poweruser'}}]
        """))
    Role.objects.request_role(
        arda_users.frodo,
        department_structure.lands,
        simple_system,
        '',
        {'role': 'manager'},
        None,
        with_external=with_external,
        with_robots=with_robots,
        with_inheritance=with_inheritance,
        without_hold=without_hold,
    )
    assert Role.objects.filter(state='granted').count() == Role.objects.count()
    assert Role.objects.filter(with_external=with_external).count() == Role.objects.count()
    assert Role.objects.filter(with_robots=with_robots).count() == Role.objects.count()
    assert Role.objects.filter(with_inheritance=with_inheritance).count() == Role.objects.count()
    assert Role.objects.filter(without_hold=without_hold).count() == Role.objects.count()
    if with_inheritance:
        assert Role.objects.count() == 2 * (1 + department_structure.lands.memberships.count())
    else:
        assert Role.objects.count() == 2


def test_move_between_groups(simple_system, department_structure, arda_users):
    """
    TestpalmID: 3456788-101
    """
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    manager_node = simple_system.nodes.get(slug='manager')
    admin_node = simple_system.nodes.get(slug='admin')

    assert not gandalf.roles.exists()

    # Выдадим роль manager только на fellowship, а роль admin и на fellowship, и на valinor
    raw_make_role(fellowship, simple_system, manager_node.data)
    raw_make_role(fellowship, simple_system, admin_node.data)
    raw_make_role(valinor, simple_system, admin_node.data)
    Role.objects.request_applicable_personal_roles()

    assert set(gandalf.roles.values_list('node__slug_path', 'state')) == {
        (manager_node.slug_path, ROLE_STATE.GRANTED),
        (admin_node.slug_path, ROLE_STATE.GRANTED),
    }

    remove_members(fellowship, [gandalf])
    add_members(valinor, [gandalf])

    assert set(gandalf.roles.values_list('node__slug_path', 'state')) == {
        (manager_node.slug_path, ROLE_STATE.ONHOLD),
        (admin_node.slug_path, ROLE_STATE.ONHOLD),
        (admin_node.slug_path, ROLE_STATE.GRANTED),
    }


@pytest.mark.parametrize(
    'members_count',
    (OVERRIDE_LARGE_GROUP_SIZE, OVERRIDE_LARGE_GROUP_SIZE - 1),
    ids=('large_group', 'small_group'),
)
def test_request_role_on_large_group(public_simple_system, members_count):
    supervision_users = set(constance.config.SUPERVISION_ROLE_USERS.split(','))
    for username in supervision_users:
        create_user(username)

    set_workflow(public_simple_system, group_code=DEFAULT_WORKFLOW)
    requester = create_user()

    member_users = []
    with override_config(SUPERVISION_ROLE_GROUP_SIZE=OVERRIDE_LARGE_GROUP_SIZE):
        for _ in range(members_count):
            member_users.append(create_user())

        group_slug = f'group-{random_slug()}'
        create_group_structure(
            {
                'slug': group_slug,
                'members': [user.username for user in member_users],
            },
            root=Group.objects.get_root(GROUP_TYPES.SERVICE)
        )

        group = Group.objects.get(slug=group_slug)

        role = Role.objects.request_role(
            requester=requester,
            subject=group,
            system=public_simple_system,
            comment='',
            data={'role': 'manager'},
        )

        if members_count >= constance.config.SUPERVISION_ROLE_GROUP_SIZE:
            assert role.state == ROLE_STATE.REQUESTED
            assert set(
                ApproveRequest.objects
                .select_related('approver')
                .filter(approve__role_request__role=role)
                .values_list('approver__username', flat=True)
            ).issuperset(supervision_users)
        else:
            assert role.state == ROLE_STATE.GRANTED
            assert not set(
                ApproveRequest.objects
                    .select_related('approver')
                    .filter(approve__role_request__role=role)
                    .values_list('approver__username', flat=True)
            ).intersection(supervision_users)
