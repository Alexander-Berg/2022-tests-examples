# coding: utf-8


import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from freezegun import freeze_time

from idm.core.models import Role
from idm.core.constants.affiliation import AFFILIATION
from idm.notification.models import Notice
from idm.tests.utils import (make_role, clear_mailbox, assert_contains, set_workflow, DEFAULT_WORKFLOW,
                             refresh, raw_make_role, move_group, accept)
from idm.users import ranks
from idm.users.models import Group

pytestmark = [pytest.mark.django_db]


def test_send_reminder_about_roles_need_rerequest(simple_system, arda_users):
    """Проверяем команду напоминания пользователям о ролях, требующих перезапроса.
    """

    frodo = arda_users['frodo']
    legolas = arda_users['legolas']

    role1 = make_role(frodo, simple_system, {'role': 'admin'})
    role1.set_state('need_request')
    role2 = make_role(frodo, simple_system, {'role': 'manager'})
    role2.set_state('need_request')
    role3 = make_role(legolas, simple_system, {'role': 'manager'})
    role3.set_state('need_request')
    make_role(legolas, simple_system, {'role': 'superuser'})  # эта роль не должна попасть в сводку
    clear_mailbox()

    assert Notice.objects.count() == 4
    Notice.objects.all().delete()
    call_command('idm_send_roles_reminders')
    assert Notice.objects.count() == 2
    assert frodo.received_notices.count() == 1
    assert legolas.received_notices.count() == 1
    assert len(mail.outbox) == 2
    message1, message2 = mail.outbox
    assert message1.to == ['frodo@example.yandex.ru']
    assert message1.subject == 'Некоторые роли требуют повторного подтверждения'
    assert_contains([
        'В связи со сменой подразделения некоторые роли требуют повторного подтверждения:',
        'Система: Simple система. Роль: Менеджер',
        'Система: Simple система. Роль: Админ'
    ], message1.body)
    assert message2.to == ['legolas@example.yandex.ru']
    assert message2.subject == 'Некоторые роли требуют повторного подтверждения'
    assert_contains([
        'В связи со сменой подразделения некоторые роли требуют повторного подтверждения:',
        'Система: Simple система. Роль: Менеджер',
    ], message2.body)


def test_send_reminders_about_group_role_needing_rerequest(simple_system, arda_users, department_structure):
    """Проверяем, что для групповой роли, которой необходим перезапрос, отсылается уведомление ответственным группы"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {})
    Notice.objects.all().delete()
    clear_mailbox()

    middle_earth = department_structure.earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        name_en='Brotherships',
        type=middle_earth.type
    )
    fellowships.save()
    move_group(fellowship, fellowships)
    fellowship = refresh(fellowship)
    fellowship = refresh(fellowship)

    group_role = fellowship.roles.get()
    assert group_role.state == 'granted'
    assert len(mail.outbox) == 0

    assert fellowship.transfers.count() == 1 + fellowship.members.count()
    assert fellowship.transfers.filter(state='undecided').count() == fellowship.transfers.count()
    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 0

    accept(fellowship.transfers.all())

    assert len(mail.outbox) == 0

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1
    assert Notice.objects.count() == 1
    assert frodo.received_notices.count() == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Некоторые роли Братство кольца требуют подтверждения'
    assert_contains([
        'В связи с перемещением группы Братство кольца в структуре некоторые роли требуют повторного подтверждения',
        'Система: Simple система. Роль: Супер Пользователь',
    ], message.body)


def test_dont_send_reminders_if_system_is_broken(simple_system, arda_users, department_structure):
    """Проверяем, что письма про сломанные системы не отправляются"""
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    Notice.objects.all().delete()
    clear_mailbox()

    simple_system.is_broken = True
    simple_system.save()

    raw_make_role(fellowship, simple_system, {'role': 'superuser'}, state='need_request')
    raw_make_role(arda_users.legolas, simple_system, {'role': 'admin'}, state='need_request')

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0


def test_send_reminder_about_roles_need_rerequest_external_user(simple_system, flat_arda_users):
    frodo = flat_arda_users.frodo
    gandalf = flat_arda_users.gandalf

    root = Group.objects.get_root('department')
    department, _ = Group.objects.get_or_create(slug='department', type='department', parent=root)
    department.responsibilities.create(
        rank=ranks.HEAD,
        user=gandalf,
        is_active=True,
    )

    frodo.department_group = department
    frodo.affiliation = AFFILIATION.OTHER
    frodo.save()

    role1 = make_role(frodo, simple_system, {'role': 'admin'})
    role1.set_state('need_request')
    clear_mailbox()
    Notice.objects.all().delete()

    call_command('idm_send_roles_reminders')

    assert Notice.objects.count() == 1
    assert frodo.received_notices.count() == 0
    assert gandalf.received_notices.count() == 1
    assert len(mail.outbox) == 1
    message1 = mail.outbox[0]
    assert message1.to == ['gandalf@example.yandex.ru']
    assert message1.subject == 'Некоторые роли Фродо Бэггинс требуют повторного подтверждения'
    assert_contains([
        'В связи со сменой подразделения некоторые роли сотрудника Фродо Бэггинс требуют повторного подтверждения',
        'Система: Simple система. Роль: Админ'
    ], message1.body)


def test_send_reminder_about_roles_need_rerequest_robot(simple_system, flat_arda_users, robot_gollum):
    frodo = flat_arda_users.frodo
    gandalf = flat_arda_users.gandalf
    robot_gollum.add_responsibles([frodo, gandalf])
    robot_gollum.save()

    role1 = make_role(robot_gollum, simple_system, {'role': 'admin'})
    role1.set_state('need_request')
    clear_mailbox()
    Notice.objects.all().delete()

    call_command('idm_send_roles_reminders')

    assert Notice.objects.count() == 2
    assert robot_gollum.received_notices.count() == 0
    assert frodo.received_notices.count() == 1
    assert gandalf.received_notices.count() == 1
    assert len(mail.outbox) == 2

    for message in mail.outbox:
        assert message.to[0] in ['frodo@example.yandex.ru', 'gandalf@example.yandex.ru']
        assert message.subject == 'Некоторые роли gollum требуют повторного подтверждения'
        assert_contains([
            'В связи со сменой подразделения некоторые роли робота gollum требуют повторного подтверждения',
            'Система: Simple система. Роль: Админ'
        ], message.body)


@freeze_time("2000-01-01 12:00:00")
def test_reminders_about_rerequested_roles_internal_user(simple_system, arda_users):
    frodo = arda_users['frodo']
    sam = arda_users['sam']

    role1 = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=1)
    )

    role2 = raw_make_role(
        frodo,
        simple_system,
        {'role': 'manager'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=3)
    )

    role3 = raw_make_role(
        sam,
        simple_system,
        {'role': 'admin'},
        state='review_request',
        expire_at=timezone.now() + timezone.timedelta(days=7, minutes=30)
    )

    # эта роль не должан попасть в рассылку
    role4 = raw_make_role(
        sam,
        simple_system,
        {'role': 'manager'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=5)
    )

    clear_mailbox()
    Notice.objects.all().delete()
    call_command('idm_send_roles_reminders')

    assert Notice.objects.count() == 2
    assert frodo.received_notices.count() == 1
    assert sam.received_notices.count() == 1
    assert len(mail.outbox) == 2
    mail1, mail2 = mail.outbox
    assert mail1.to == ['frodo@example.yandex.ru']
    assert mail1.subject == 'Некоторые перезапрошенные роли не подтверждены и скоро будут отозваны'
    assert_contains(
        [
            'Некоторые перезапрошенные роли все ещё не подтверждены и скоро будут отозваны.',
            'Роль: %s' % role1.node.name,
            'Роль: %s' % role2.node.name,
        ],
        mail1.body
    )
    assert mail2.to == ['sam@example.yandex.ru']
    assert mail2.subject == 'Некоторые перезапрошенные роли не подтверждены и скоро будут отозваны'
    assert_contains(
        [
            'Некоторые перезапрошенные роли все ещё не подтверждены и скоро будут отозваны.',
            'Роль: %s' % role3.node.name,
        ],
        mail2.body
    )
    assert 'Роль: %s' % role4.node.name not in mail2.body


def test_reminders_about_rerequested_roles_external_user(simple_system, arda_users):
    frodo = arda_users['frodo']
    bilbo = arda_users['bilbo']

    root = Group.objects.get_root('department')
    department, _ = Group.objects.get_or_create(slug='department', type='department', parent=root)
    department.responsibilities.create(
        rank=ranks.HEAD,
        user=bilbo,
        is_active=True,
    )

    frodo.department_group = department
    frodo.affiliation = AFFILIATION.OTHER
    frodo.save()

    role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=1)
    )
    clear_mailbox()
    Notice.objects.all().delete()

    call_command('idm_send_roles_reminders')

    assert Notice.objects.count() == 1
    assert frodo.received_notices.count() == 0
    assert bilbo.received_notices.count() == 1
    assert len(mail.outbox) == 1
    mail1 = mail.outbox[0]
    assert mail1.to == ['bilbo@example.yandex.ru']
    assert mail1.subject == 'Некоторые перезапрошенные роли Фродо Бэггинс не подтверждены и скоро будут отозваны'
    assert_contains(
        [
            'Некоторые перезапрошенные роли сотрудника Фродо Бэггинс все ещё не подтверждены и скоро будут отозваны.',
            'Роль: %s' % role.node.name,
        ],
        mail1.body
    )


def test_reminders_about_rerequested_roles_robot(simple_system, arda_users, robot_gollum):
    frodo = arda_users['frodo']
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()

    role = raw_make_role(
        robot_gollum,
        simple_system,
        {'role': 'admin'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=1)
    )
    clear_mailbox()
    Notice.objects.all().delete()

    call_command('idm_send_roles_reminders')

    assert Notice.objects.count() == 1
    assert robot_gollum.received_notices.count() == 0
    assert frodo.received_notices.count() == 1
    assert len(mail.outbox) == 1
    mail1 = mail.outbox[0]
    assert mail1.to == ['frodo@example.yandex.ru']
    assert mail1.subject == 'Некоторые перезапрошенные роли gollum не подтверждены и скоро будут отозваны'
    assert_contains(
        [
            'Некоторые перезапрошенные роли робота gollum все ещё не подтверждены и скоро будут отозваны.',
            'Роль: %s' % role.node.name,
        ],
        mail1.body
    )


def test_reminders_about_rerequested_roles_group(simple_system, arda_users, department_structure):
    """Проверяем, что для групповой роли, которой необходим перезапрос, отсылается уведомление ответственным группы"""

    frodo = arda_users['frodo']
    fellowship = department_structure['fellowship']
    valinor = department_structure['valinor']
    role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state='rerequested',
        expire_at=timezone.now() + timezone.timedelta(days=1)
    )
    # Проверим, что для группы с ролью в статусе onhold письмо не отправим
    role2 = raw_make_role(
        valinor,
        simple_system,
        {'role': 'manager'},
        state='onhold',
        expire_at=timezone.now() + timezone.timedelta(days=1)
    )
    Notice.objects.all().delete()
    clear_mailbox()

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1
    assert Notice.objects.count() == 1
    assert frodo.received_notices.count() == 1
    mail1 = mail.outbox[0]
    assert mail1.to == ['frodo@example.yandex.ru']
    assert mail1.subject == 'Некоторые перезапрошенные роли Братство кольца не подтверждены и скоро будут отозваны'
    assert_contains(
        [
            'Некоторые роли, перезапрошенные на группу Братство кольца, все ещё не подтверждены и скоро будут отозваны.',
            'Роль: %s' % role.node.name,
        ],
        mail1.body
    )
