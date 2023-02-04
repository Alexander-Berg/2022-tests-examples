# -*- coding: utf-8 -*-


import collections

import pytest

from idm.core.constants.action import ACTION
from idm.core.models import Role, ApproveRequest, Action
from idm.utils.actions import start_stop_actions
from idm.tests.utils import (set_workflow, refresh, add_perms_by_role, remove_perms_by_role, set_roles_tree,
                             create_system, DEFAULT_WORKFLOW, assert_contains)
from idm.users.models import Group

pytestmark = [pytest.mark.django_db]


def assert_role_actions(actions_qs, expected_roles):
    """Проверка, что queryset actions содержит действия по ролям expected_roles
    и не содержит повторяющихся элементов"""
    if not isinstance(expected_roles, collections.Iterable):
        expected_roles = [expected_roles]
    actions = list(actions_qs.order_by('pk'))
    expected_actions = []
    for role in expected_roles:
        expected_actions += list(role.actions.all())
    expected_actions.sort(key=lambda item: item.pk)
    assert actions == expected_actions


def assert_all_actions(actions_qs):
    """Проверка, что actions_qs - все существующие роли"""
    actions = list(actions_qs.order_by('pk'))
    expected_actions = list(Action.objects.order_by('pk'))
    assert actions == expected_actions


def test_user_can_view_action_of_roles_he_has_approved(arda_users, pt1_system):
    """Проверим, что пользователю доступны действия роли, которые он когда-либо подтверждал"""

    set_workflow(pt1_system, 'approvers = [approver("legolas") | approver("gimli"), approver("gandalf")]')

    role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, pt1_system, None,
                                     {'project': 'proj1', 'role': 'manager'}, {'passport-login': 'frodo'})

    # действия по роли доступны владельцу роли
    actions = Action.objects.permitted_for(arda_users.frodo)
    assert_role_actions(actions, role)
    # действия по роли доступны подтверждающему
    actions = Action.objects.permitted_for(arda_users.legolas)
    assert_role_actions(actions, role)
    # не подтверждающему - недоступна
    actions = Role.objects.permitted_for(arda_users.nazgul)
    assert not actions

    # подтвердим роль
    legolas_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role,
                                                         approver__username='legolas')
    legolas_approve_request.set_approved(arda_users.legolas)

    # действия по роли доступны и после подтверждения
    actions = Action.objects.permitted_for(arda_users.legolas)
    assert_role_actions(actions, role)

    # отклоним роль
    gandalf_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role, approver__username='gandalf')
    gandalf_approve_request.set_declined(arda_users.gandalf)

    # действия по отклонённой роли доступны как подтвердившему,
    actions = Action.objects.permitted_for(arda_users.legolas)
    assert_role_actions(actions, role)
    # так и отклонившему
    actions = Action.objects.permitted_for(arda_users.legolas)
    assert_role_actions(actions, role)


def test_user_can_view_actions_of_roles_he_has_approved_even_if_they_were_rerequested(arda_users, pt1_system):
    """Проверим, что если роль была перезапрошена, и бывшего подтверждающего нет среди новых подтверждающих,
    то действия по этой роли всё равно ему доступны"""

    set_workflow(pt1_system, 'approvers = [approver("legolas"), approver("gimli")]')

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='frodo', state='created', is_fully_registered=True)
    role = Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                                     {'passport-login': 'frodo'})
    for request in ApproveRequest.objects.select_related_for_set_decided().select_related('approver'):
        request.set_approved(request.approver)

    set_workflow(pt1_system, 'approvers = [approver("saruman")]')
    role = refresh(role)
    role.set_state('need_request')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    # новому подтверждающему действия по роли видны
    actions = Action.objects.permitted_for(arda_users.saruman)
    assert_role_actions(actions, role)
    # равно как и старым подтверждающим
    actions = Action.objects.permitted_for(arda_users.gimli)
    assert_role_actions(actions, role)
    actions = Action.objects.permitted_for(arda_users.legolas)
    assert_role_actions(actions, role)
    # но посторонним она недоступна
    actions = Action.objects.permitted_for(arda_users.sauron)
    assert_role_actions(actions, [])


def test_user_can_view_all_actions_if_he_has_appropriate_rights(arda_users, pt1_system):
    """Проверим, что если у пользователя есть необходимые права (глобальные),
    то он может видеть действия по роли, даже если не является подтверждающим роли"""

    set_workflow(pt1_system, 'approvers = [approver("legolas"), approver("gimli")]')

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                              {'passport-login': 'frodo'})
    # вообще пользователь не может видеть действия по чужим ролям
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, [])

    add_perms_by_role('viewer', sauron)

    actions = Action.objects.permitted_for(sauron)
    assert_all_actions(actions)

    remove_perms_by_role('viewer', sauron)

    # или вот если он суперпользователь, то ему можно всё
    add_perms_by_role('superuser', sauron)
    actions = Action.objects.permitted_for(sauron)
    assert_all_actions(actions)


def test_user_with_permission_can_view_actions_for_some_roles_inside_a_system(arda_users, pt1_system):
    """Проверим, что если у пользователя есть права (внутри системы), то он может видеть действия
    по части ролей"""

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    gandalf = arda_users.gandalf
    pt2_system = create_system('test2', name='Test2 система', plugin_type=pt1_system.plugin_type)

    role1 = Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                                      {'passport-login': 'frodo'})
    role2 = Role.objects.request_role(gandalf, gandalf, pt2_system, None, {'project': 'proj2', 'role': 'wizard'},
                                      {'passport-login': 'frodo-wizard'})
    assert role1.node.value_path == '/proj1/manager/'
    assert role2.node.value_path == '/proj2/wizard/'

    add_perms_by_role('users_view', sauron, system=pt1_system, scope='/proj3/')
    actions = Action.objects.permitted_for(sauron)
    assert not actions
    add_perms_by_role('users_view', sauron, system=pt1_system, scope='/proj1/')
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, role1)
    # но если иметь нужный scope в другой системе, то можно увидеть обе роли
    add_perms_by_role('users_view', sauron, system=pt2_system, scope='/proj2/')
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, {role1, role2})


def test_user_with_empty_scope_can_view_actions_for_all_roles_inside_a_system(arda_users, pt1_system, simple_system):
    """Проверим, что если у пользователя есть хотя бы одно право с пустым scope,
    то он может видеть действия по всем ролям в системе"""

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    role1 = Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                                      {'passport-login': 'frodo'})
    role2 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'manager'}, None)

    # пустой scope даёт возможность просматривать действия по всем ролям в одной системе
    add_perms_by_role('users_view', sauron, system=pt1_system)
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, role1)
    remove_perms_by_role('users_view', sauron, system=pt1_system)
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, [])

    # если выдать пустой scope в другой системе, то можно увидеть и роли в ней
    add_perms_by_role('users_view', sauron, system=pt1_system)
    add_perms_by_role('users_view', sauron, system=simple_system)
    actions = Action.objects.permitted_for(sauron)
    assert_role_actions(actions, {role1, role2})


def test_user_can_view_actions_for_roles_in_groups_he_is_responsible_for(arda_users, simple_system,
                                                                         department_structure):
    """Проверим, что пользователь может видеть действия по групповым ролям, выданным на группы
    по иерархии ниже тех, в которых он ответственный"""

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    varda = arda_users.varda
    gimli = arda_users.gimli

    earth = department_structure.earth
    associations = department_structure.associations
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    shire = department_structure.shire

    add_perms_by_role('responsible', sauron, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    role1 = Role.objects.request_role(sauron, earth, simple_system, None, {'role': 'admin'}, None)
    role2 = Role.objects.request_role(sauron, associations, simple_system, None, {'role': 'manager'}, None)
    role3 = Role.objects.request_role(sauron, fellowship, simple_system, None, {'role': 'poweruser'}, None)
    role4 = Role.objects.request_role(sauron, valinor, simple_system, None, {'role': 'superuser'}, None)

    # varda ответственная в группах associations и valinor, должна видеть действия по ролям, выданным на группы
    # "средиземье", "объединения", "братство кольца" и "валинор", а также роли своих подчинённых, которые
    # выданы на основе этих групповых
    actions = Action.objects.permitted_for(varda)
    expected_roles = (
        {role1, role2, role3, role4} |
        set(role1.refs.filter(user__in=fellowship.members.all())) |
        set(role1.refs.filter(user__in=associations.members.all())) |
        set(role2.refs.all()) |
        set(role3.refs.all()) |
        set(role4.refs.all())
    )
    assert set(Role.objects.permitted_for(varda)) == expected_roles
    assert_role_actions(actions, expected_roles)

    # manve увидит только действия по ролям, выданным на группу valinor
    actions = Action.objects.permitted_for(arda_users.manve)
    expected_roles = {role4} | set(role4.refs.all())
    assert set(Role.objects.permitted_for(arda_users.manve)) == expected_roles
    assert_role_actions(actions, expected_roles)

    # gimli увидит только действия по ролям, выданным на братство кольца и по групповым ролям,
    # выданным на вышестоящие по иерерархии группы, а также действия по своим персональным ролям
    # и групповым ролям, по которым они выданы
    actions = Action.objects.permitted_for(gimli)
    personal_role1 = role1.refs.get(user=gimli)
    personal_role2 = role2.refs.get(user=gimli)
    personal_role3 = role3.refs.get(user=gimli)
    expected_roles = {role1, role2, role3, personal_role1, personal_role2, personal_role3}
    assert set(Role.objects.permitted_for(gimli)) == expected_roles
    assert_role_actions(actions, expected_roles)

    # frodo ответственный в группах associations, fellowship и shire, должен увидеть действия по ролям в них,
    # но не в группе Валинор. Однако он член группы Валинор, поэтому должен увидеть действия по своей роли в ней,
    # и действия по самой групповой роли. Кроме того, он должен увидеть действия по роли, выданной на middle earth,
    # потому что является участником групп, дочерних по отношению к ней
    actions = Action.objects.permitted_for(frodo)
    personal_roles = {role.refs.get(user=frodo) for role in (role1, role2, role3, role4)}
    subordinates = set(fellowship.members.all()) | set(shire.members.all()) | set(associations.members.all())
    subordinates_roles1 = role1.refs.filter(user__in=subordinates)
    subordinates_roles2 = role2.refs.filter(user__in=subordinates)
    subordinates_roles3 = role3.refs.all()
    subordinates_roles4 = role4.refs.filter(user__in=subordinates)
    expected_roles = (
        {role1, role2, role3, role4} | personal_roles |
        set(subordinates_roles1) | set(subordinates_roles2) | set(subordinates_roles3) | set(subordinates_roles4)
    )
    assert set(Role.objects.permitted_for(frodo)) == expected_roles
    assert_role_actions(actions, expected_roles)


def test_user_can_view_actions_for_roles_of_groups_he_is_member_of(arda_users, simple_system, department_structure):
    """Проверим, что пользователь видит действия по групповым ролям, выданным на группы, родительские для тех,
    в которых он состоит"""

    sauron = arda_users.sauron
    aragorn = arda_users.aragorn
    legolas = arda_users.legolas
    add_perms_by_role('responsible', sauron, simple_system)

    earth = Group.objects.get(slug='middle-earth')
    associations = Group.objects.get(slug='associations')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    valinor = Group.objects.get(slug='valinor')
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role1 = Role.objects.request_role(sauron, earth, simple_system, None, {'role': 'admin'}, None)
    role2 = Role.objects.request_role(sauron, associations, simple_system, None, {'role': 'manager'}, None)
    role3 = Role.objects.request_role(sauron, fellowship, simple_system, None, {'role': 'poweruser'}, None)
    role4 = Role.objects.request_role(sauron, valinor, simple_system, None, {'role': 'superuser'}, None)

    # aragorn входит только в группу "братство кольца" и не является ответственным в ней
    # поэтому он видит действия по всем групповым ролям выше по иерархии + по своим ролям, выданным по ним,
    # но не видит, например, дейстий по роли, выданной на группу valinor
    actions = Action.objects.permitted_for(aragorn)
    expected_roles = {role1, role2, role3}
    personal_roles = {group_role.refs.get(user=aragorn) for group_role in expected_roles}
    expected_roles |= personal_roles
    assert_role_actions(actions, expected_roles)

    # legolas входит во все группы, на которые выданы роли, поэтому видит действия по ним всем
    actions = Action.objects.permitted_for(legolas)
    expected_roles = {role1, role2, role3, role4}
    personal_roles = {group_role.refs.get(user=legolas) for group_role in expected_roles}
    expected_roles |= personal_roles
    assert_role_actions(actions, expected_roles)


def test_traceback_in_action_data_if_sync_failed():
    with start_stop_actions(ACTION.TRANSFERS_STARTED_DECISION, ACTION.TRANSFERS_DECIDED) as manager:
        manager.on_failure({'data': {'status': 1}})
        raise Exception('error')

    assert_contains([
        'Traceback (most recent call last):',
        'in test_traceback_in_action_data_if_sync_failed',
        'Exception: error'
    ], Action.objects.latest('pk').data.get('traceback'))
