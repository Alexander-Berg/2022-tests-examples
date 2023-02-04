# coding: utf-8


import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.models import Role
from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.tests.utils import clear_mailbox, assert_contains, set_workflow, DEFAULT_WORKFLOW
from idm.users import ranks
from idm.users.models import Group, GroupMembership

pytestmark = [
    pytest.mark.django_db,
]


def prepare_data_for_test(user, pt1_system, department, passport_logins, arda_users):
    assert user.passport_logins.exists() is False
    for login in passport_logins:
        user.passport_logins.create(login=login, state='created', is_fully_registered=False)

    membership = user.memberships.get(group=department)
    assert membership.passport_login is None
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    Role.objects.request_role(arda_users.frodo, department, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    user_role = Role.objects.get(user=user, parent__group=department)
    assert user_role.state == 'awaiting'
    clear_mailbox()
    call_command('idm_send_membership_reminders')
    return user_role


@pytest.mark.parametrize('passport_logins', [['yndx-frodo'], ['yndx-frodo', 'yndx-frodo-other-login']])
def test_reminders_about_attach_passport_login_to_membership_internal_user(pt1_system, arda_users, department_structure,
                                                                           passport_logins):
    frodo = arda_users['frodo']
    fellowship = department_structure['fellowship']
    frodo_role = prepare_data_for_test(frodo, pt1_system, fellowship, passport_logins, arda_users)
    passport_logins_count = len(passport_logins)
    if passport_logins_count < 2:
        # Если паспортных логинов не больше одного, то привязываем его к членству в группе, письмо не отправим
        assert len(mail.outbox) == 0
    else:
        assert frodo_role.passport_logins.exists() is False
        assert len(mail.outbox) == 1
        message = mail.outbox[0]
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в некоторых группах требует указания паспортного логина'
        assert_contains(
            [
                'Некоторые роли, выданные по групповым, требуют наличия паспортного логина.',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, fellowship.external_id),
            ],
            message.body,
        )


@pytest.mark.parametrize('passport_logins', [['yndx-frodo'], ['yndx-frodo', 'yndx-frodo-other-login']])
def test_reminders_about_attach_passport_login_to_membership_external_user(pt1_system, arda_users, department_structure,
                                                                           passport_logins):
    frodo = arda_users['frodo']
    bilbo = arda_users['bilbo']

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
    frodo_role = prepare_data_for_test(frodo, pt1_system, fellowship, passport_logins, arda_users)
    passport_logins_count = len(passport_logins)
    if passport_logins_count < 2:
        # Если паспортных логинов не больше одного, то привязываем его к членству в группе, письмо не отправим
        assert len(mail.outbox) == 0
    else:
        assert frodo_role.passport_logins.exists() is False
        assert len(mail.outbox) == 1
        message = mail.outbox[0]
        assert message.to == ['bilbo@example.yandex.ru']
        assert message.subject == 'Участие в некоторых группах Фродо Бэггинс требует указания паспортного логина'
        assert_contains(
            [
                'Некоторые роли Фродо Бэггинс, выданные по групповым, требуют наличия паспортного логина',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, fellowship.external_id),
            ],
            message.body,
        )


@pytest.mark.parametrize('passport_logins', [['yndx-gollum'], ['yndx-gollum', 'yndx-gollum-other-login']])
def test_reminders_about_attach_passport_login_to_membership_robot(pt1_system, arda_users, department_structure,
                                                                   passport_logins, robot_gollum):
    frodo = arda_users['frodo']
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()
    fellowship = department_structure['fellowship']
    GroupMembership.objects.create(
        user=robot_gollum, group=fellowship, state=GROUPMEMBERSHIP_STATE.ACTIVE, is_direct=True,
    )

    gollum_role = prepare_data_for_test(robot_gollum, pt1_system, fellowship, passport_logins, arda_users)
    passport_logins_count = len(passport_logins)
    if passport_logins_count < 2:
        # Если паспортных логинов не больше одного, то привязываем его к членству в группе, письмо не отправим
        assert len(mail.outbox) == 0
    else:
        assert gollum_role.passport_logins.exists() is False
        assert len(mail.outbox) == 1
        message = mail.outbox[0]
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Участие в некоторых группах gollum требует указания паспортного логина'
        assert_contains(
            [
                'Некоторые роли робота gollum, выданные по групповым, требуют наличия паспортного логина',
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (robot_gollum.username, fellowship.external_id),
            ],
            message.body,
        )
