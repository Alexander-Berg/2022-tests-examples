# coding: utf-8
import json
from textwrap import dedent

import pytest
import requests
from django.core import mail
from django.core.management import call_command
from idm.users.constants.user import USER_TYPES

from idm.sync.staff import users

from idm.tests.sync.test_staff import user_dict, staff_response
from mock import patch

from idm.core.models import Role, ApproveRequest
from idm.tests.utils import (set_workflow, refresh, assert_contains, clear_mailbox, add_perms_by_role,
                             expire_role, DEFAULT_WORKFLOW, create_fake_response)
from idm.users.models import Group

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def test_request_role_with_email_cc(simple_system, arda_users):
    """Добавим роль с указанным email_cc через workflow"""

    set_workflow(simple_system, 'email_cc = ["sauron@example.yandex.ru"]; approvers = []')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'
    assert role.email_cc == {
        'granted': [{
            'lang': 'ru',
            'email': 'sauron@example.yandex.ru',
            'pass_to_personal': False
        }]
    }

    assert len(mail.outbox) == 2
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']
    assert mail.outbox[1].to == ['sauron@example.yandex.ru']


def test_request_group_role_with_email_cc(simple_system, arda_users, department_structure):
    """Добавим групповую роль с указанным email_cc через workflow"""

    set_workflow(simple_system, group_code='email_cc = ["sauron@example.yandex.ru"]; approvers = []')
    frodo = arda_users.get('frodo')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'
    assert role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'sauron@example.yandex.ru', 'pass_to_personal': False}]
    }

    assert len(mail.outbox) == 2
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains((
        'Группа "Братство кольца", в которой вы являетесь ответственным, получила новую роль в системе "Simple система"',
    ), message.body)

    cc_message = mail.outbox[1]
    assert cc_message.to == ['sauron@example.yandex.ru']
    assert_contains((
        'Группа "Братство кольца" получила новую роль в системе "Simple система"',
    ), cc_message.body)


def test_cc_reminders(simple_system, arda_users):
    """Проверим, как работает конструкция recipient(mail, reminders=True)"""

    set_workflow(simple_system, dedent('''
    email_cc=["sauron@example.yandex.ru", recipient("legolas@example.yandex.ru", reminders=True, granted=False, lang='en')]
    approvers=['gandalf']
    '''))

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    assert role.state == 'requested'
    assert role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'sauron@example.yandex.ru', 'pass_to_personal': False}],
        'reminders': [{'lang': 'en', 'email': 'legolas@example.yandex.ru', 'pass_to_personal': False}]
    }
    assert len(mail.outbox) == 2
    # письма о необходимости подтверждения аппруверу и запросившему
    approver_message, requester_message = mail.outbox
    assert approver_message.to == ['gandalf@example.yandex.ru']
    assert approver_message.cc == []
    assert approver_message.subject == 'Подтверждение роли. Simple система.'
    assert requester_message.to == ['frodo@example.yandex.ru']
    assert requester_message.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert requester_message.cc == []
    clear_mailbox()

    # при рассылке напоминаний они уходят sauron-у, а legolas-у нет. и одно письмо уходит подтверждающему gandalf
    clear_mailbox()
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 2
    approver_notification, cc_notification = mail.outbox
    assert approver_notification.to == ['gandalf@example.yandex.ru']
    assert approver_notification.subject == 'Подтверждение ролей.'
    assert approver_notification.cc == []
    assert cc_notification.to == ['legolas@example.yandex.ru']
    assert cc_notification.subject == 'New role request: please approve or reject.'  # на английском, как и просили
    assert cc_notification.cc == []  # отдельное письмо, а не cc

    assert_contains((
        '1 role request is awaiting your decision:',
        'New requests: 1',
        'Your IDM',
    ), cc_notification.body)

    clear_mailbox()
    # теперь подтвердим роль, должно уйти сообщение о подтверждении
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.fetch_approver()
    approve_request.set_approved(approve_request.approver)

    assert len(mail.outbox) == 2
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'

    cc_message = mail.outbox[1]
    # legolas не должен быть включён в cc, так как granted отключено вручную, а вот sauron здесь должен быть
    assert cc_message.to == ['sauron@example.yandex.ru']


def test_cc_requested(simple_system, arda_users):
    """Проверим работу recipient(mail, requested=True)"""

    set_workflow(simple_system, dedent('''
    approvers=["sauron"]
    email_cc=[recipient("legolas@example.yandex.ru", requested=True)]'''))

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, 'Wanna be admin!', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'requested'
    assert role.email_cc == {
        'requested': [{'lang': 'ru', 'email': 'legolas@example.yandex.ru', 'pass_to_personal': False}],
    }
    assert len(mail.outbox) == 3
    approver_mail, subject_mail, cc_mail = mail.outbox
    assert approver_mail.to == ['sauron@example.yandex.ru']
    assert subject_mail.to == ['frodo@example.yandex.ru']
    assert subject_mail.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert cc_mail.to == ['legolas@example.yandex.ru']
    assert cc_mail.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert_contains([
        'Фродо Бэггинс',
        '"Simple система"',
        'Wanna be admin!',
    ], cc_mail.body)


def test_cc_declined(simple_system, arda_users):
    """Проверим работу со статусом declined"""

    set_workflow(simple_system, dedent('''
    approvers=['varda']
    email_cc=[recipient("legolas@example.yandex.ru", declined=True)]'''))

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'requested'
    assert role.email_cc == {
        'declined': [{'lang': 'ru', 'email': 'legolas@example.yandex.ru', 'pass_to_personal': False}],
    }
    # письма владельцу и подтверждающему
    assert len(mail.outbox) == 2
    clear_mailbox()

    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.set_declined(arda_users.varda, 'Just no.')

    role = refresh(role)
    assert role.state == 'declined'
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.subject == 'Simple система. Заявка на роль отклонена'
    assert message.to == ['frodo@example.yandex.ru']
    assert_contains((
        'varda отклонил запрос роли в системе "Simple система" для пользователя Фродо Бэггинс (frodo):',
        'Роль: Админ',
        'Just no.',
    ), message.body)
    assert cc_message.subject == 'Simple система. Заявка на роль отклонена'
    assert cc_message.to == ['legolas@example.yandex.ru']
    assert_contains((
        'varda отклонил запрос роли сотруднику Фродо Бэггинс (frodo) в системе "Simple система":',
        'Роль: Админ',
        'Комментарий: Just no.',
    ), cc_message.body)


def test_cc_expired(simple_system, arda_users):
    """Проверим работу со статусом expired"""

    set_workflow(simple_system, dedent('''
    approvers=['varda']
    email_cc=[recipient("legolas@example.yandex.ru", expired=True)]'''))

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    add_perms_by_role('responsible', legolas, simple_system)

    role = Role.objects.request_role(legolas, frodo, simple_system, '', {'role': 'admin'}, None)
    expire_role(role, 1)
    clear_mailbox()

    simple_system.deprive_expired_roles()
    assert len(mail.outbox) == 2
    requester_mail, cc_mail = mail.outbox
    assert requester_mail.to == ['legolas@example.yandex.ru']
    assert requester_mail.subject == 'Запрошенная вами роль в системе "Simple система" не получила одобрения в срок'
    assert_contains((
        'Запрошенная вами для Фродо Бэггинс',
        'роль в системе "Simple система" не получила подтверждения в срок:',
        'Роль: Админ',
        'Вы можете запросить роль повторно:',
    ), requester_mail.body)

    assert cc_mail.to == ['legolas@example.yandex.ru']
    assert cc_mail.subject == 'Simple система. Роль не получила одобрения в срок'
    assert_contains((
        'Запрошенная для Фродо Бэггинс',
        'роль в системе "Simple система" не получила подтверждения в срок:',
        'Роль: Админ',
        'Вы можете запросить роль повторно:'
    ), cc_mail.body)


def test_cc_group_role_expired(simple_system, arda_users, department_structure):
    """Проверим работу со статусом expired для групповой роли"""

    set_workflow(simple_system, group_code=dedent('''
    approvers=['varda']
    email_cc=[recipient("legolas@example.yandex.ru", expired=True)]'''))

    fellowship = department_structure.fellowship
    legolas = arda_users.legolas
    add_perms_by_role('responsible', legolas, simple_system)

    role = Role.objects.request_role(legolas, fellowship, simple_system, '', {'role': 'admin'}, None)
    expire_role(role, 1)
    clear_mailbox()

    simple_system.deprive_expired_roles()
    assert len(mail.outbox) == 2
    requester_mail, cc_mail = mail.outbox
    assert requester_mail.to == ['legolas@example.yandex.ru']
    assert requester_mail.subject == 'Запрошенная вами роль в системе "Simple система" не получила одобрения в срок'
    assert_contains((
        'Запрошенная вами для группы "Братство кольца"',
        'роль в системе "Simple система" не получила '
        'подтверждения в срок:',
        'Роль: Админ',
        'Вы можете запросить роль повторно:'
    ), requester_mail.body)

    assert cc_mail.to == ['legolas@example.yandex.ru']
    assert cc_mail.subject == 'Simple система. Роль не получила одобрения в срок'
    assert_contains((
        'Запрошенная для группы "Братство кольца"',
        'роль в системе "Simple система" не получила подтверждения в срок:',
        'Роль: Админ',
        'Вы можете запросить роль повторно:'
    ), cc_mail.body)


def test_group_role_pass_cc_to_personal(simple_system, arda_users, department_structure):
    """Проверим что при pass_to_personal=True поле email_cc передается в персональные роли, создаваемые по групповой"""

    set_workflow(simple_system, group_code=dedent("""
        email_cc=[recipient("legolas@example.yandex.ru", granted=True, pass_to_personal=True)]
        approvers=[]
        """))

    frodo = arda_users.get('frodo')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    members_count = fellowship.members.count()

    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'
    assert role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'legolas@example.yandex.ru', 'pass_to_personal': True}]
    }

    assert Role.objects.count() == members_count + 1

    frodo_personal_role = Role.objects.get(user__username='frodo')

    assert frodo_personal_role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'legolas@example.yandex.ru', 'pass_to_personal': True}]
    }

    assert len(mail.outbox) == members_count + 2

    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Фродо Бэггинс получил новую роль в системе "Simple система"'], message.body)

    message = mail.outbox[members_count + 1]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Группа "Братство кольца" получила новую роль в системе "Simple система"'], message.body)


def test_pass_cc_to_personal_user_fired(simple_system, arda_users, department_structure):
    """Проверим что при pass_to_personal=True при увольнении по email_cc отправится письмо от отзыве роли"""

    set_workflow(simple_system, group_code=dedent("""
            email_cc=[recipient("legolas@example.yandex.ru", deprived=True, pass_to_personal=True)]
            approvers=[]
            """))

    frodo = arda_users.frodo
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    clear_mailbox()

    frodo.is_active = False
    data = [user_dict(user) for user in arda_users.values() if user.type == USER_TYPES.USER]
    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(data)))
        users.import_users()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    frodo.refresh_from_db()
    assert frodo.is_active is False

    assert len(mail.outbox) == 1

    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Simple система. Роль отозвана'
    assert_contains(['robot-idm отозвал роль сотрудника Фродо Бэггинс (frodo) в системе "Simple система"'], message.body)
