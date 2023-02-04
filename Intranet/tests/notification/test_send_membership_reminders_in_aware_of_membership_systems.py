# coding: utf-8


from mock import patch
import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.tests.utils import (clear_mailbox, force_awaiting_memberships_role_grant, assert_contains, set_workflow,
                             DEFAULT_WORKFLOW, assert_sent_emails_count, add_members)
from idm.users import ranks
from idm.users.models import Group, GroupMembership, GroupResponsibility
from idm.users.ranks import HEAD
from idm.users.tasks import SendLoginRemindersToNewGroupMembers, sync_indirect_memberships

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS | {SYSTEM_GROUP_POLICY.UNAWARE})
def test_internal_user(pt1_system, arda_users, department_structure, group_policy):
    pt1_system.group_policy = group_policy
    pt1_system.save(update_fields=['group_policy'])

    fellowship = department_structure['fellowship']
    frodo = arda_users['frodo']
    sam = arda_users['sam']
    sam_membership = sam.memberships.get(group=fellowship)
    sam_membership.passport_login = sam.passport_logins.create(
        login='yndx-sam@yandex-team.ru',
        state='created',
        is_fully_registered=False,
    )
    sam_membership.save(update_fields=['passport_login'])

    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    fellowship.memberships.exclude(user__in=[frodo, sam]).delete()

    # Проверим, что при выдаче новой групповой роли отправится письмо участникам группы,
    # у которых не указан паспортный логин
    with patch('idm.sync.passport.exists', return_value=True):
        group_role = Role.objects.request_role(
            frodo,
            fellowship,
            pt1_system,
            '',
            {'project': 'proj1', 'role': 'admin'},
            None,
        )

    group_role.refresh_from_db()

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        force_awaiting_memberships_role_grant(group_role)
    else:
        assert group_role.state == ROLE_STATE.GRANTED

    if group_policy == SYSTEM_GROUP_POLICY.UNAWARE:
        assert group_role.refs.count() == 2
    else:
        assert group_role.refs.count() == 0

    expected_count = (
        2  # одно о выдаче групповой роли, второе о паспортном логине для frodo
        if group_policy in [SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS, SYSTEM_GROUP_POLICY.UNAWARE]
        else 1  # только письмо о выдаче групповой роли
    )

    assert len(mail.outbox) == expected_count
    if group_policy in [SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS, SYSTEM_GROUP_POLICY.UNAWARE]:
        message, _ = mail.outbox
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в группе требует указания паспортного логина'
        assert_contains(
                [
                    'Вы входите в группу %s'
                    % fellowship.name,
                    'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                    % (frodo.username, fellowship.external_id),
                ],
                message.body,
            )
    clear_mailbox()

    # Добавим пользователей в группу, проверим, что выполнение команды idm_send_passport_login_reminders_to_new_members
    # отправит письмо новому пользовтаелю, у которого нет логина и только ему (frodo не получит второго письма)
    meriadoc = arda_users['meriadoc']
    meriadoc_passport_login = meriadoc.passport_logins.create(
        login='yndx-meriadoc@yandex-team.ru',
        state='created',
        is_fully_registered=False,
    )
    fellowship.memberships.create(user=meriadoc, passport_login=meriadoc_passport_login, is_direct=True, state='active')

    peregrin = arda_users['peregrin']
    fellowship.memberships.create(user=peregrin, is_direct=True, state='active')

    with patch('idm.sync.passport.exists', return_value=True):
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
        SendLoginRemindersToNewGroupMembers()

    expected_count = (
        1  # Письмо для peregrin о необходимости привязки логина
        if group_policy in [SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS, SYSTEM_GROUP_POLICY.UNAWARE]
        else 0  # Нет писем
    )
    assert len(mail.outbox) == expected_count

    if group_policy in [SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS, SYSTEM_GROUP_POLICY.UNAWARE]:

        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
            snippets_list = [
                'В связи с вступлением в новые группы, требуется указать паспортный логин.',
                'Пожалуйста, привяжите паспортный логин для групп по ссылке ниже.',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (peregrin.username, fellowship.external_id),
            ]
        else:
            snippets_list = [
                'Вы входите в группу %s' % fellowship.name,
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (peregrin.username, fellowship.external_id),
            ]

        message = mail.outbox[0]
        assert message.to == ['peregrin@example.yandex.ru']
        assert message.subject == 'Участие в группе требует указания паспортного логина'
        assert_contains(snippets_list, message.body)

    # Проверим, что повторное письмо не отправим
    clear_mailbox()
    with patch('idm.sync.passport.exists', return_value=True):
        SendLoginRemindersToNewGroupMembers()
    assert len(mail.outbox) == 0


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_activate_membership(pt1_system, arda_users, department_structure, group_policy):
    # Проверим, что после перехода из неактивного состояния в активное сбрасывается флаг и письмо будет отправлено
    pt1_system.group_policy = group_policy
    pt1_system.save(update_fields=['group_policy'])
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    fellowship = department_structure['fellowship']
    associations = fellowship.parent

    frodo = arda_users['frodo']
    fellowship.memberships.exclude(user__in=[frodo]).delete()
    associations.memberships.exclude(user__in=[frodo]).delete()
    frodo.memberships.update(state='inactive', notified_about_passport_login=True)

    with patch('idm.sync.passport.exists', return_value=True):
        Role.objects.request_role(frodo, associations, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    clear_mailbox()

    frodo.memberships.filter(group=fellowship).update(state='active')
    with patch('idm.sync.passport.exists', return_value=True):
        sync_indirect_memberships()
        SendLoginRemindersToNewGroupMembers()

    expected_count = (
        1 # Письмо для frodo о необходимости привязки логина
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0 # Нет писем
    )
    assert len(mail.outbox) == expected_count

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        message = mail.outbox[0]

        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в группе требует указания паспортного логина'
        assert_contains(
            [
                'В связи с вступлением в новые группы, требуется указать паспортный логин.',
                'Пожалуйста, привяжите паспортный логин для групп по ссылке ниже.',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, associations.external_id),
            ],
            message.body,
        )


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_external_user(pt1_system, arda_users, department_structure, group_policy):
    pt1_system.group_policy = group_policy
    pt1_system.save(update_fields=['group_policy'])
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    frodo = arda_users['frodo']
    bilbo = arda_users['bilbo']
    varda = arda_users['varda']

    root = Group.objects.get_root('department')
    department, _ = Group.objects.get_or_create(slug='department', type='department', parent=root, external_id=10001)
    department.responsibilities.create(
        rank=ranks.HEAD,
        user=bilbo,
        is_active=True,
    )

    frodo.department_group = department
    frodo.affiliation = AFFILIATION.OTHER
    frodo.save()

    fellowship = department_structure['fellowship']
    fellowship.memberships.exclude(user=frodo).delete()
    valinor = department_structure['valinor']
    valinor.memberships.all().delete()
    clear_mailbox()

    # Проверим, что при выдаче новой групповой роли отправится письмо руквоводителю внешнего участника группы,
    # у которого не указан паспортный логин
    with patch('idm.sync.passport.exists', return_value=True):
        Role.objects.request_role(frodo, fellowship, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)

    expected_count = (
        2  # одно о выдаче групповой роли, второе о паспортном логине для frodo
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0  # нет писем, так как роль в `awaiting`
    )

    assert len(mail.outbox) == expected_count
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        message, _ = mail.outbox
        assert message.to == ['bilbo@example.yandex.ru']
        assert message.subject == 'Участие в группе пользователя Фродо Бэггинс требует указания паспортного логина'
        assert_contains(
            [
                'Пользователь Фродо Бэггинс входит в группу %s' % fellowship.name,
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, fellowship.external_id),
            ],
            message.body,
        )

    # Добавим пользователя в группу, на которую уже выдана роль,
    # проверим, что выполнение команды idm_send_passport_login_reminders_to_new_members
    # отправит письмо руководителю нового пользовтаеля
    Role.objects.request_role(varda, valinor, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    clear_mailbox()
    valinor.memberships.create(user=frodo, is_direct=True, state='active')

    with patch('idm.sync.passport.exists', return_value=True):
        SendLoginRemindersToNewGroupMembers()

    expected_count = (
        1  # Письмо для frodo о необходимости привязки логина
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0  # Нет писем
    )

    assert len(mail.outbox) == expected_count
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        message = mail.outbox[0]
        assert message.to == ['bilbo@example.yandex.ru']
        assert message.subject == 'Участие в группе пользователя Фродо Бэггинс требует указания паспортного логина'
        assert_contains(
            [
                'В связи с вступлением в новые группы пользователя Фродо Бэггинс, требуется указать паспортный логин.',
                'Пожалуйста, привяжите паспортный логин для Фродо Бэггинс для групп по ссылке ниже.',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, valinor.external_id),
            ],
            message.body,
        )


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_robot(pt1_system, arda_users, department_structure, robot_gollum, group_policy):
    pt1_system.group_policy = group_policy
    pt1_system.save(update_fields=['group_policy'])
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    frodo = arda_users['frodo']
    varda = arda_users['varda']
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()

    fellowship = department_structure['fellowship']
    fellowship.memberships.all().delete()
    valinor = department_structure['valinor']
    valinor.memberships.all().delete()

    fellowship.memberships.create(user=robot_gollum, is_direct=True, state='active')
    clear_mailbox()
    # Проверим, что при выдаче новой групповой роли отправится письмо ответственному за робота,
    # у которого не указан паспортный логин
    with patch('idm.sync.passport.exists', return_value=True):
        Role.objects.request_role(frodo, fellowship, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)

    expected_count = (
        2  # одно о выдаче групповой роли, второе о паспортном логине gollum для frodo
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0  # нет писем, так как роль в `awaiting`
    )

    assert len(mail.outbox) == expected_count
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        message, _ = mail.outbox
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в группе робота gollum требует указания паспортного логина'
        assert_contains(
            [
                'Робот gollum входит в группу %s. '
                'На эту группу выдана роль, которая требует привязки паспортного логина:' % fellowship.name,
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (robot_gollum.username, fellowship.external_id),
            ],
            message.body,
        )

    # Добавим пользователя в группу, на которую уже выдана роль,
    # проверим, что выполнение команды idm_send_passport_login_reminders_to_new_members
    # отправит письмо руководителю нового пользовтаеля
    Role.objects.request_role(varda, valinor, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    clear_mailbox()
    valinor.memberships.create(user=robot_gollum, is_direct=True, state='active')

    with patch('idm.sync.passport.exists', return_value=True):
        SendLoginRemindersToNewGroupMembers()

    expected_count = (
        1 # Письмо для frodo о необходимости привязки логина gollum
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0 # Нет писем
    )

    assert len(mail.outbox) == expected_count
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        message = mail.outbox[0]
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в группе робота gollum требует указания паспортного логина'
        assert_contains(
            [
                'В связи с вступлением в новые группы робота gollum, требуется указать паспортный логин.',
                'Пожалуйста, привяжите паспортный логин для gollum для групп по ссылке ниже.',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (robot_gollum.username, valinor.external_id),
            ],
            message.body,
        )


def test_multiple_groups(pt1_system, arda_users, department_structure):
    """Если пользователь вошел в несколько групп между выполнениями idm_sync_groups
    То ему придет одно письмо про все группы, где от него требуется паспортный логин"""

    pt1_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    pt1_system.save()
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)

    Role.objects.request_role(frodo, fellowship, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    Role.objects.request_role(frodo, shire, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)

    clear_mailbox()
    sauron = arda_users['sauron']
    for login in ['login1', 'login2']:
        sauron.passport_logins.create(login=login, state='created')
    fellowship.memberships.create(user=sauron, is_direct=True, state='active')
    shire.memberships.create(user=sauron, is_direct=True, state='active')

    with patch('idm.sync.passport.exists', return_value=True):
        SendLoginRemindersToNewGroupMembers()
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['sauron@example.yandex.ru']
    assert 'В связи с вступлением в новые группы, требуется указать паспортный логин.' in message.body
    assert 'f-group=105' in message.body
    assert 'f-group=104' in message.body


def _prepare_system(simple_system):
    field = [f for f in simple_system.nodes.get(slug='admin').get_fields() if f.slug == 'passport-login'][0]
    field.is_required = True
    field.save(update_fields=['is_required'])
    simple_system.group_policy = SYSTEM_GROUP_POLICY.UNAWARE  # unaware only
    simple_system.save(update_fields=['group_policy'])


@pytest.mark.parametrize('without_what', {'robots', 'external', 'inheritance'})
def test_not_notify_robots_in_groups(simple_system, arda_users, department_structure, robot_gollum, without_what):
    _prepare_system(simple_system)
    frodo = arda_users['frodo']
    fellowship = department_structure['fellowship']
    add_members(fellowship, [robot_gollum])
    fellowship.memberships.exclude(user__in=[frodo, robot_gollum]).delete()

    robot_gollum.add_responsibles([frodo])
    if without_what != 'robots':
        robot_gollum.is_robot = False
    if without_what == 'external':
        robot_gollum.affiliation = 'external'
        GroupResponsibility.objects.create(user=frodo, group=robot_gollum.department_group, is_active=True, rank=HEAD)
    if without_what == 'inheritance':
        simple_system.request_policy = 'anyone'
        simple_system.save()
        group = department_structure.lands
        add_members(group, [frodo])
    else:
        group = fellowship
    robot_gollum.save()

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    with patch('idm.sync.passport.exists', return_value=True):
        Role.objects.request_role(frodo, group, simple_system, '', {'role': 'admin'}, None,
                                  **{f'with_{without_what}': False})

    with assert_sent_emails_count(1):
        GroupMembership.objects.send_regular_passport_login_attach_reminders()

    passport_login = frodo.passport_logins.create(
        login='yndx-frodo@yandex-team.ru',
        state='created',
        is_fully_registered=False)

    for membership in frodo.memberships.filter(group=group).all():
        membership.passport_login = passport_login
        membership.save(update_fields=['passport_login'])

    with assert_sent_emails_count(0):
        GroupMembership.objects.send_regular_passport_login_attach_reminders()


def test_not_notify_robots_in_groups2(simple_system, arda_users, department_structure):
    _prepare_system(simple_system)
    frodo = arda_users['frodo']
    group = department_structure.fellowship

    assert frodo.roles.count() == 0
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    group.memberships.exclude(user__in=[frodo, arda_users['sam']]).delete()
    with patch('idm.sync.passport.exists', return_value=True):
        Role.objects.request_role(frodo, group, simple_system, '', {'role': 'admin'}, None)

    assert frodo.roles.count() == 1
    with assert_sent_emails_count(2):
        GroupMembership.objects.send_regular_passport_login_attach_reminders()
    frodo.roles.all().delete()
    with assert_sent_emails_count(1):
        GroupMembership.objects.send_regular_passport_login_attach_reminders()
