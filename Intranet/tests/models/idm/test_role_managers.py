# -*- coding: utf-8 -*-


import pytest

from idm.core.models import Role, ApproveRequest
from idm.tests.utils import (set_workflow, refresh, add_perms_by_role, remove_perms_by_role, create_system,
                             DEFAULT_WORKFLOW, create_user)
from idm.users.models import Group

pytestmark = [pytest.mark.django_db]


def assert_unsorted_list(testing_list, expected_set):
    """Проверка, что несортированный список содержит те же элементы, что и expected_set,
    и не содержит повторяющихся элементов"""
    assert set(testing_list) == expected_set
    assert len(testing_list) == len(expected_set)


def test_user_can_view_roles_he_has_approved(arda_users, pt1_system):
    """Проверим, что пользователю доступны роли, которые он когда-либо подтверждал"""

    set_workflow(pt1_system, 'approvers = [approver("legolas") | approver("gimli"), approver("gandalf")]')

    role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, pt1_system, None,
                                     {'project': 'proj1', 'role': 'manager'}, {'passport-login': 'frodo'})

    # роль доступна владельцу
    assert list(Role.objects.permitted_for(arda_users.frodo)) == [role]
    # роль доступна подтверждающему
    roles = Role.objects.permitted_for(arda_users.legolas)
    assert list(roles) == [role]
    # не подтверждающему - недоступна
    roles = Role.objects.permitted_for(arda_users.nazgul)
    assert list(roles) == []

    # подтвердим роль
    legolas_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role, approver__username='legolas')
    legolas_approve_request.set_approved(arda_users.legolas)

    # роль доступна и после подтверждения
    roles = Role.objects.permitted_for(arda_users.legolas)
    assert list(roles) == [role]

    # отклоним роль
    gandalf_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role, approver__username='gandalf')
    gandalf_approve_request.set_declined(arda_users.gandalf)

    # отклонённая роль доступна как подтвердившему,
    roles = Role.objects.permitted_for(arda_users.legolas)
    assert list(roles) == [role]
    # так и отклонившему
    roles = Role.objects.permitted_for(arda_users.gandalf)
    assert list(roles) == [role]


def test_user_can_view_roles_he_has_approved_even_if_they_were_rerequested(arda_users, pt1_system):
    """Проверим, что если роль была перезапрошена, и бывшего подтверждающего нет среди новых подтверждающих,
    то роль всё равно ему доступна"""

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

    # новому подтверждающему роль видна
    roles = Role.objects.permitted_for(arda_users.saruman)
    assert list(roles) == [role]
    # равно как и старым подтверждающим
    roles = Role.objects.permitted_for(arda_users.gimli)
    assert list(roles) == [role]
    roles = Role.objects.permitted_for(arda_users.legolas)
    assert list(roles) == [role]
    # но посторонним она недоступна
    roles = Role.objects.permitted_for(arda_users.sauron)
    assert list(roles) == []


def test_user_can_view_roles_if_it_has_appropriate_rights(arda_users, pt1_system):
    """Проверим, что если у пользователя есть необходимые права (глобальные),
    то он может видеть роли, даже если не является подтверждающим роли"""

    set_workflow(pt1_system, 'approvers = [approver("legolas"), approver("gimli")]')

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    role = Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                                     {'passport-login': 'frodo'})
    # вообще пользователь не может видеть чужие роли
    assert list(Role.objects.permitted_for(sauron)) == []

    # но если у него есть соответствующее право, то может
    add_perms_by_role('viewer', sauron)
    assert list(Role.objects.permitted_for(sauron)) == [role]

    remove_perms_by_role('viewer', sauron)
    assert list(Role.objects.permitted_for(sauron)) == []

    # или вот если он суперпользователь, то ему можно всё
    add_perms_by_role('superuser', sauron)
    assert list(Role.objects.permitted_for(sauron)) == [role]


def test_user_with_permission_can_view_some_roles_inside_a_system(arda_users, pt1_system):
    """Проверим, что если у пользователя есть права (внутри системы), то он может видеть часть ролей"""

    set_workflow(pt1_system, 'approvers = [approver("legolas"), approver("gimli")]')

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

    # но если у него есть соответствующее право, то может
    add_perms_by_role('users_view', sauron, system=pt1_system, scope='/proj3/')
    roles = Role.objects.permitted_for(sauron)
    assert list(roles) == []
    add_perms_by_role('users_view', sauron, system=pt1_system, scope='/proj1/')
    roles = Role.objects.permitted_for(sauron)
    assert list(roles) == [role1]
    # но если иметь нужный scope в другой системе, то можно увидеть обе роли
    add_perms_by_role('users_view', sauron, system=pt2_system, scope='/proj2/')
    roles = Role.objects.permitted_for(sauron)
    assert set(roles) == {role1, role2}


def test_user_with_empty_scope_can_view_all_roles_inside_a_system(arda_users, pt1_system, simple_system):
    """Проверим, что если у пользователя есть хотя бы одно право с пустым scope,
    то он может видеть все роли в системе"""

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    role1 = Role.objects.request_role(frodo, frodo, pt1_system, None, {'project': 'proj1', 'role': 'manager'},
                                      {'passport-login': 'frodo'})
    role2 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'manager'}, None)

    # пустой scope даёт возможность просматривать все роли в одной системе
    add_perms_by_role('users_view', sauron, system=pt1_system)
    roles = Role.objects.permitted_for(sauron)
    assert list(roles) == [role1]
    remove_perms_by_role('users_view', sauron, system=pt1_system)
    assert list(Role.objects.permitted_for(sauron)) == []

    # если выдать пустой scope в другой системе, то можно увидеть и роли в ней
    add_perms_by_role('users_view', sauron, system=simple_system)
    roles = Role.objects.permitted_for(sauron)
    assert list(roles) == [role2]


def test_user_can_view_roles_in_groups_he_is_responsible(arda_users, simple_system, department_structure):
    """Проверим, что пользователь может видеть групповые роли, выданные на группы, по иерархии ниже тех, в
    которых он ответственный"""

    frodo = arda_users.frodo
    sauron = arda_users.sauron
    varda = arda_users.varda
    gimli = arda_users.gimli
    add_perms_by_role('responsible', sauron, simple_system)

    earth = department_structure.earth
    associations = department_structure.associations
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    shire = department_structure.shire

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    role1 = Role.objects.request_role(sauron, earth, simple_system, None, {'role': 'admin'}, None)
    role2 = Role.objects.request_role(sauron, associations, simple_system, None, {'role': 'manager'}, None)
    role3 = Role.objects.request_role(sauron, fellowship, simple_system, None, {'role': 'poweruser'}, None)
    role4 = Role.objects.request_role(sauron, valinor, simple_system, None, {'role': 'superuser'}, None)

    # varda ответственная в группах associations и valinor, должна видеть роли, выданные на группы
    # "объединения", "братство кольца" и "валинор", а также родительскую группу earth
    roles = Role.objects.permitted_for(varda)
    group_roles = roles.filter(group__isnull=False)
    assert set(group_roles) == {role1, role2, role3, role4}
    expected_roles = (
        {role1, role2, role3, role4} |
        set(role1.refs.filter(user__in=fellowship.members.all())) |
        set(role1.refs.filter(user__in=associations.members.all())) |
        set(role2.refs.all()) |
        set(role3.refs.all()) |
        set(role4.refs.all())
    )
    assert_unsorted_list(roles, expected_roles)

    # manve увидит только роли, выданные на группу valinor
    roles = Role.objects.permitted_for(arda_users.manve)
    group_roles = roles.filter(group__isnull=False)
    assert set(group_roles) == {role4}
    expected_roles = {role4} | set(role4.refs.all())
    assert_unsorted_list(roles, expected_roles)

    # gimli увидит только роли, выданные на братство кольца + групповые роли, выданные на вышестоящие группы
    # и свои персональные роли, выданные по ним
    roles = Role.objects.permitted_for(gimli)
    group_roles = roles.filter(group__isnull=False)
    assert set(group_roles) == {role1, role2, role3}
    personal_role1 = role1.refs.get(user=gimli)
    personal_role2 = role2.refs.get(user=gimli)
    personal_role3 = role3.refs.get(user=gimli)
    expected_roles = {role1, role2, role3, personal_role1, personal_role2, personal_role3}
    assert_unsorted_list(roles, expected_roles)

    # frodo ответственный в группах associations, fellowship и shire, должен увидеть роли в них,
    # но не в группе Валинор.
    # Однако он член группы Валинор, поэтому должен увидеть свою роль в ней, и саму групповую роль.
    # Кроме того, он должен увидеть роль, выданную на middle earth, потому что является участником групп, дочерних
    # по отношению к ней
    roles = Role.objects.permitted_for(frodo)
    group_roles = roles.filter(group__isnull=False)
    assert set(group_roles) == {role1, role2, role3, role4}
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
    assert_unsorted_list(roles, expected_roles)


def test_user_can_view_roles_of_groups_he_is_member_of(arda_users, simple_system, department_structure):
    """Проверим, что пользователь видит групповые роли, выданные на группы, родительские для тех, где он
    состоит"""

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
    # поэтому он видит все групповые роли выше по иерархии + свои роли, выданные по ним,
    # но не видит, например, роли, выданные на группу valinor
    roles = Role.objects.permitted_for(aragorn)
    expected_roles = {role1, role2, role3}
    personal_roles = {group_role.refs.get(user=aragorn) for group_role in expected_roles}
    expected_roles |= personal_roles
    assert_unsorted_list(roles, expected_roles)

    # legolas входит во все группы, на которые выданы роли, поэтому видит их все
    roles = Role.objects.permitted_for(legolas)
    expected_roles = {role1, role2, role3, role4}
    personal_roles = {group_role.refs.get(user=legolas) for group_role in expected_roles}
    expected_roles |= personal_roles
    assert_unsorted_list(roles, expected_roles)


def test_get_approvers(simple_system, arda_users):
    admin = create_user('admin', superuser=True)
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["legolas"]')

    all_approvers, additional = Role.objects.simulate_role_request(
        frodo, frodo, simple_system, {'role': 'manager'}, {}
    )
    assert all_approvers == [[legolas]]

    all_approvers, additional = Role.objects.simulate_role_request(
        legolas, legolas, simple_system, {'role': 'manager'}, {}
    )
    assert all_approvers == []

    all_approvers, additional = Role.objects.simulate_role_request(
        admin, legolas, simple_system, {'role': 'manager'}, {}
    )
    assert all_approvers == []

    set_workflow(simple_system, 'approvers = [approver("legolas") | "frodo"]')
    all_approvers, additional = Role.objects.simulate_role_request(
        frodo, frodo, simple_system, {'role': 'manager'}, {}
    )
    assert all_approvers == []


def test_request_role_not_requiring_approval(arda_users, simple_system):
    admin = create_user('admin', superuser=True)
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["legolas"]')
    assert ApproveRequest.objects.count() == 0

    role = Role.objects.request_role(admin, legolas, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == 1

    role = refresh(role)
    assert role.state == 'granted'

    approverequest = ApproveRequest.objects.get()
    assert approverequest.approver_id == legolas.id
    assert approverequest.approved is True
    assert approverequest.decision == 'approve'


def test_nested_values(arda_users, simple_system):
    frodo = arda_users.legolas
    set_workflow(simple_system, 'approvers = []')
    assert ApproveRequest.objects.count() == 0

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    parent_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role.parent = parent_role
    role.save(update_fields=['parent'])
    assert Role.objects.count() == 2

    expected = [
        {
            'user': {
                'username': frodo.username,
            },
            'node': {
                'slug': role.node.slug,
            },
            'parent': {
                'node': {
                    'slug_path': parent_role.node.slug_path
                }
            },
            'system': role.node.system.id,
            'is_public': role.is_public,
        },
        {
            'user': {
                'username': frodo.username,
            },
            'node': {
                'slug': parent_role.node.slug,
            },
            'parent': None,
            'system': parent_role.node.system.id,
            'is_public': parent_role.is_public,
        }
    ]
    assert list(
        Role.objects
        .nested_values(
            'user__username', 'user', 'node__slug', 'node', 'system', 'is_public', 'parent__node__slug_path',
            'parent__node', 'parent',
        )
        .order_by('pk')
    ) == expected
