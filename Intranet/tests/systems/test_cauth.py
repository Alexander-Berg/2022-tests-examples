# coding: utf-8


import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.workflow.exceptions import NoApproversDefined
from idm.core.models import Role, ApproveRequest
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (set_workflow, refresh,
                             assert_action_chain, assert_contains, clear_mailbox, add_perms_by_role,
                             mock_all_roles, get_recievers)

pytestmark = [pytest.mark.django_db]


workflow = """
from functools import reduce
import operator
decline_email_to = {'decline-too@yandex-team.ru'}
parent = node.get_ancestor(-2)  # родитель родителя
aliases = parent.get_aliases(type='notify-email')
responsibilities = parent.get_responsibilities()
defaults = {'lang': 'ru', 'requested': True, 'granted': True}
if len(aliases):
    email_cc = []
    for alias in aliases:
        kwargs = defaults.copy()
        emails = [mail.strip() for mail in alias.name.split(',') if '@' in mail.strip()]
        for email in emails:
            kwargs['email'] = email
            if alias.name in decline_email_to:
                kwargs['declined'] = True
                kwargs['deprived'] = True
            email_cc.append(recipient(**kwargs))
    approvers = any_from([responsibility.user for responsibility in responsibilities], notify=False)
else:
    approvers = []
    for responsibility in responsibilities:
        if responsibility.notify:
            approvers.append(approver(responsibility.user, notify=True))
        else:
            approvers.append(approver(responsibility.user))
    if approvers:
        approvers = reduce(operator.or_, approvers)
if not approvers:
    del approvers
else:
    approvers = [approvers]
"""


@pytest.fixture
def cauth_w_wf(cauth):
    set_workflow(cauth, workflow, group_code=workflow)
    return cauth


def test_user_request_for_aliased_server(cauth_w_wf, arda_users):
    """Проверим запрос роли для пользователя для случая, когда есть notify-email alias"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    gandalf = arda_users.gandalf
    varda = arda_users.varda

    # без ответственного роль запросить нельзя
    with pytest.raises(NoApproversDefined) as excinfo:
        Role.objects.request_role(frodo, frodo, cauth_w_wf, '', {'dst': 'server2', 'role': 'ssh'}, {'root': False})
    assert str(excinfo.value) == 'В workflow не определены подтверждающие (approvers)'
    assert len(mail.outbox) == 0

    add_perms_by_role('responsible', varda, cauth_w_wf)
    # теперь попробуем для сервера, у которого есть responsibilities
    role = Role.objects.request_role(varda, frodo, cauth_w_wf, '', {'dst': 'server1', 'role': 'ssh'}, {'root': False})
    role = refresh(role)
    assert role.state == 'requested'
    assert role.ref_roles == []
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Роль в системе "CAuth" требует подтверждения.'

    assert_contains([
        'varda запросил для вас роль в системе "CAuth":',
        'Назначение: server1, Роль: Ssh',
        'gandalf',
        'gimli и legolas',
    ], message.body)
    assert cc_message.to == ['vs-admin@yandex-team.ru']
    assert cc_message.subject == 'Роль в системе "CAuth" требует подтверждения.'

    clear_mailbox()
    requests = role.requests.get().approves.get().requests.all()
    assert requests.filter(notify=True).count() == 0
    assert requests.filter(notify=False).count() == 3
    assert requests.count() == 3
    request = requests.get(approver=legolas)
    request.set_approved(legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'CAuth. Новая роль'
    assert_contains((
        'Вы получили новую роль в системе "CAuth":',
        'Назначение: server1, Роль: Ssh',
        'Роль была запрошена пользователем varda',
    ), message.body)
    assert cc_message.subject == 'CAuth. Новая роль'
    assert cc_message.to == ['vs-admin@yandex-team.ru']


def test_user_request_aliased_server_decline(cauth_w_wf, arda_users):
    """Проверим отказ в выдаче роли, если письмо должно быть отослано только владельцу"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    assert len(mail.outbox) == 0

    role = Role.objects.request_role(frodo, frodo, cauth_w_wf, '', {'dst': 'server1', 'role': 'ssh'}, {'root': False})
    role = refresh(role)
    assert role.state == 'requested'
    assert role.ref_roles == []
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert_contains([
        'Вы запросили роль в системе "CAuth":',
        'Назначение: server1, Роль: Ssh',
        'gandalf',
        'gimli и legolas',
    ], message.body)
    assert cc_message.to == ['vs-admin@yandex-team.ru']
    assert cc_message.subject == 'Роль в системе "CAuth" требует подтверждения.'
    clear_mailbox()

    requests = role.requests.get().approves.get().requests.all()
    assert requests.count() == 3
    request = requests.get(approver=legolas)
    request.set_declined(legolas)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])
    assert len(mail.outbox) == 1
    message, = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'CAuth. Заявка на роль отклонена'
    assert_contains((
        'legolas отклонил запрос роли в системе "CAuth" для пользователя Фродо Бэггинс (frodo):',
        'Назначение: server1, Роль: Ssh',
    ), message.body)


def test_decline_email_cc(cauth_w_wf, arda_users):
    """Проверим отказ в выдаче роли, если письмо должно быть отослано владельцу и на рассылку"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role = Role.objects.request_role(frodo, frodo, cauth_w_wf, '', {'dst': 'server5', 'role': 'ssh'}, {'root': False})
    clear_mailbox()
    request = ApproveRequest.objects.select_related_for_set_decided().select_related('approver').get()
    request.set_declined(request.approver)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'CAuth. Заявка на роль отклонена'
    assert_contains((
        'legolas отклонил запрос роли в системе "CAuth" для пользователя Фродо Бэггинс (frodo):',
        'Назначение: server5, Роль: Ssh'
    ), message.body)
    assert cc_message.to == ['decline-too@yandex-team.ru']
    assert cc_message.subject == 'CAuth. Заявка на роль отклонена'


def test_user_request_for_unaliased_server(cauth_w_wf, arda_users):
    """Проверим запрос роли для пользователя для случая, когда нет notify-email alias-а"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    gandalf = arda_users.gandalf
    varda = arda_users.varda
    add_perms_by_role('responsible', varda, cauth_w_wf)
    role = Role.objects.request_role(varda, frodo, cauth_w_wf, '', {'dst': 'server4', 'role': 'ssh'}, {'root': False})
    # у сервера три ответственных, но только два из них с notify=True, а также gandalf с приоритетом 1

    assert len(mail.outbox) == 4
    gandalf_mail, gimli_mail, legolas_mail, frodo_mail = mail.outbox
    assert gandalf_mail.to == ['gandalf@example.yandex.ru']
    assert gandalf_mail.subject == 'Подтверждение роли. CAuth.'
    assert gimli_mail.to == ['gimli@example.yandex.ru']
    assert gimli_mail.subject == 'Подтверждение роли. CAuth.'
    assert legolas_mail.to == ['legolas@example.yandex.ru']
    assert legolas_mail.subject == 'Подтверждение роли. CAuth.'
    assert_contains((
        'varda',
        'Фродо Бэггинс',
        'Назначение: server4, Роль: Ssh',
    ), legolas_mail.body)
    assert frodo_mail.to == ['frodo@example.yandex.ru']
    assert frodo_mail.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert_contains((
        'varda',
        'Фродо',
        'Назначение: server4, Роль: Ssh',
        'gandalf',
        'gimli и legolas',
    ), frodo_mail.body)


def test_reminders_for_aliased_server(cauth_w_wf, arda_users):
    """Проверим рассылку оповещений для случая, когда есть notify-email"""

    frodo = arda_users.frodo
    varda = arda_users.varda
    add_perms_by_role('responsible', varda, cauth_w_wf)
    role = Role.objects.request_role(varda, frodo, cauth_w_wf, '', {'dst': 'server1', 'role': 'ssh'}, {'root': False})
    assert len(mail.outbox) == 2
    clear_mailbox()

    call_command('idm_send_notifications')
    # дайджест придет gandalf с приоритетом 1
    assert len(mail.outbox) == 1
    gandalf_mail = mail.outbox[0]
    assert gandalf_mail.to == ['gandalf@example.yandex.ru']


def test_group_request_for_aliased_server(cauth_w_wf, arda_users, department_structure):
    """Проверим запрос роли для группы для случая, когда есть notify-email alias"""

    fellowship = department_structure.fellowship
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    gandalf = arda_users.gandalf

    # без ответственного роль запросить нельзя
    with pytest.raises(NoApproversDefined) as excinfo:
        Role.objects.request_role(frodo, fellowship, cauth_w_wf, '', {'dst': 'server3', 'role': 'ssh'}, {'root': False})
    assert str(excinfo.value) == 'В workflow не определены подтверждающие (approvers)'
    assert len(mail.outbox) == 0

    role = Role.objects.request_role(frodo, fellowship, cauth_w_wf,
                                     '', {'dst': 'server1', 'role': 'ssh'}, {'root': False})
    role = refresh(role)
    assert role.state == 'requested'
    assert role.ref_roles == []
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert_contains([
        'Вы запросили роль в системе "CAuth" для группы "Братство кольца", в которой вы являетесь ответственным:',
        'Назначение: server1, Роль: Ssh',
        'gandalf',
        'gimli и legolas',
    ], message.body)
    assert cc_message.to == ['vs-admin@yandex-team.ru']
    assert cc_message.subject == 'Роль в системе "CAuth" требует подтверждения.'
    clear_mailbox()

    request = ApproveRequest.objects.select_related_for_set_decided().get(approver=legolas)
    request.set_approved(legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    assert len(mail.outbox) == 2
    message, cc_message = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'CAuth. Новая роль'
    assert_contains((
        'Группа "Братство кольца", в которой вы являетесь ответственным, получила новую роль в системе "CAuth":',
        'Назначение: server1, Роль: Ssh',
        'Роль была запрошена пользователем Фродо Бэггинс (frodo)',
    ), message.body)
    assert cc_message.to == ['vs-admin@yandex-team.ru']
    assert cc_message.subject == 'CAuth. Новая роль'


def test_group_request_for_unaliased_server(cauth_w_wf, arda_users, department_structure):
    """Проверим запрос роли для группы для случая, когда нет notify-email"""

    fellowship = department_structure.fellowship
    varda = arda_users.varda
    add_perms_by_role('responsible', varda, cauth_w_wf)
    role = Role.objects.request_role(varda, fellowship, cauth_w_wf,
                                     '', {'dst': 'server4', 'role': 'ssh'}, {'root': False})
    # у gandalf приоритет 1
    assert len(mail.outbox) == 4
    gandalf_mail, gimli_mail, legolas_mail, frodo_mail = mail.outbox
    assert gandalf_mail.to == ['gandalf@example.yandex.ru']
    assert gandalf_mail.subject == 'Подтверждение роли. CAuth.'
    assert gimli_mail.to == ['gimli@example.yandex.ru']
    assert gimli_mail.subject == 'Подтверждение роли. CAuth.'
    assert legolas_mail.to == ['legolas@example.yandex.ru']
    assert legolas_mail.subject == 'Подтверждение роли. CAuth.'
    assert_contains((
        'varda',
        'legolas',
        'Назначение: server4, Роль: Ssh',
        'https://example.com/system/cauth/#role=%s' % role.id,
    ), legolas_mail.body)
    assert frodo_mail.to == ['frodo@example.yandex.ru']
    assert frodo_mail.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert_contains((
        'varda запросил роль в системе "CAuth" для группы "Братство кольца", в которой вы являетесь ответственным:',
        'Назначение: server4, Роль: Ssh',
        'gandalf',
        'gimli и legolas',
    ), frodo_mail.body)


def test_comma_separated_emails(cauth_w_wf, arda_users, department_structure):
    """Проверим запрос роли для сервера с alias, где notify-email это разделённый точкой список мейлов"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, cauth_w_wf, '', {'dst': 'server6', 'role': 'ssh'}, {'root': False})
    assert len(mail.outbox) == 3
    frodo_mail, _, maillist_mail = mail.outbox
    assert frodo_mail.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert get_recievers(mail.outbox) == {
        'good@valinor.middleearth',
        'evil@mordor.midleearth',
        'frodo@example.yandex.ru'
    }
    assert maillist_mail.subject == 'Роль в системе "CAuth" требует подтверждения.'
    assert_contains([
        'Фродо Бэггинс',
        'Система: "CAuth"',
        'Назначение: server6, Роль: Ssh',
    ], maillist_mail.body)


@pytest.mark.robot
def test_import_cauth_role(cauth_w_wf, arda_users):
    """Проверим импорт ролей через неконсистентность"""
    all_roles = [
        {
            'login': 'frodo',
            'roles': [
                [
                    {'dst': 'server1', 'role': 'ssh'}, {'root': True}
                ]
            ]
        }
    ]

    with mock_all_roles(cauth_w_wf, all_roles):
        Inconsistency.objects.check_roles()
        Inconsistency.objects.resolve(force=True)

    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.state == 'granted'
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency'])
    assert role.fields_data == {'root': True}
