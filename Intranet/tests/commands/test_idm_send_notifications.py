# coding: utf-8


from textwrap import dedent

import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import six
from mock import patch

from idm.core.models import Role, ApproveRequest, Approve
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import make_inconsistency, clear_mailbox, set_workflow, assert_contains, refresh, make_absent, \
    make_attending, raw_make_role

pytestmark = [pytest.mark.django_db]


def test_pending_requests_notification_for_real_requested_roles(simple_system, arda_users, settings):
    """Проверим, что в обычном случае подтверждающие получают дайджест  с учётом приоритетов"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("gandalf")]')

    assert len(mail.outbox) == 0
    # запросим роль
    Role.objects.request_role(frodo, frodo, simple_system, 'Комментарий к запросу роли', {'role': 'admin'}, {})

    # письма о запросе и просьба подтверждения
    assert len(mail.outbox) == 2
    mail1, mail2 = mail.outbox
    assert mail1.to == ['legolas@example.yandex.ru']
    assert mail2.to == ['frodo@example.yandex.ru']
    assert_contains(['Комментарий к запросу роли'], mail1.body)

    clear_mailbox()
    call_command('idm_send_notifications')

    assert len(mail.outbox) == 1
    message, = mail.outbox
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Подтверждение ролей.'
    assert_contains(['Простых запросов: 1'], message.body)

    clear_mailbox()

    # подтвердим роль и посмотрим что получится
    approve_req = ApproveRequest.objects.select_related_for_set_decided().get(approver__username='legolas')
    approve_req.set_approved(legolas)

    # придет письмо для frodo о выдаче роли
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']

    # ничего не отправится - нечего подтверждать
    clear_mailbox()
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 0


def test_approved_or_group_wont_receive_notifications(simple_system, arda_users):
    """Проверим, что если хотя бы один человек из OR-группы подтвердил, то никто из неё не получит оповещений"""

    frodo = arda_users.frodo
    gandalf = arda_users.legolas
    set_workflow(simple_system, 'approvers = [any_from(["legolas", "gandalf"]), "varda"]')

    assert len(mail.outbox) == 0
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 3
    mail1, mail2, mail3 = mail.outbox
    assert mail1.to == ['legolas@example.yandex.ru']
    assert mail2.to == ['varda@example.yandex.ru']
    assert mail3.to == ['frodo@example.yandex.ru']

    # подтвердим первую or-группу вторым подтверждающим:
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approver=gandalf)
    approve_request.set_approved(gandalf)
    clear_mailbox()

    # и отошлём уведомления. они должны уйти только пользователю из второй or-группы, т.е. варде
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['varda@example.yandex.ru']


@pytest.mark.robot
def test_pending_requests_types(simple_system, arda_users):
    """Проверка всех типов запросов"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["legolas"]')
    # Обычный запрос
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    # Перезапрос
    role2 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    approve_request = role2.requests.get().approves.get().requests.get()
    approve_request.set_approved(arda_users.legolas)
    role2 = Role.objects.select_related('node', 'system__actual_workflow').get(pk=role2.pk)
    role2.set_state('need_request')
    role2.rerequest(frodo)
    # Перезапрос в связи с регулярным пересмотром
    role3 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'poweruser'}, None)
    approve_request = role3.requests.get().approves.get().requests.get()
    approve_request.set_approved(arda_users.legolas)
    role3 = Role.objects.select_related('node', 'system__actual_workflow').get(pk=role3.pk)
    role3.set_state('review_request')
    # Перезапрос в связи с неконсистентностью
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=frodo,
        path='/role/superuser/',
    )
    inconsistency.resolve()
    expected = ['imported', 'requested', 'rerequested', 'review_request']
    assert list(Role.objects.values_list('state', flat=True).order_by('state')) == expected
    clear_mailbox()
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert_contains([
        'Вашего решения ожидают 4 запроса роли:',
        'Регулярных пересмотров: 1',
        'Запросов по расхождениям: 1',
        'Повторных запросов: 1',
        'Простых запросов: 1',
        'Пройдите по ссылке https://example.com/queue/, чтобы подтвердить или отклонить запросы на выдачу ролей.'
    ], message.body)


@pytest.mark.parametrize('notify', [True, False])
def test_reminders_explicitly_true_or_false(simple_system, arda_users, notify):
    """Проверяем напоминания при approver('username', notify=True/False).
    На рассылку напоминаний этот флаг не влияет, письма отсылаются всем с основным приоритетом"""

    frodo = arda_users.frodo
    workflow_code = ('approvers = [approver("legolas", notify=True) | "gandalf",'
                     'approver("varda", notify=%s) | "manve"]' % six.text_type(notify))

    make_absent([arda_users.legolas, arda_users.manve])
    set_workflow(simple_system, workflow_code)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    # запустить пересчет основного приоритета перед рассылкой уведомлений
    ApproveRequest.objects.recalculate_main_priority()
    clear_mailbox()
    call_command('idm_send_notifications')

    assert len(mail.outbox) == 2

    if notify:
        expected = {'gandalf', 'varda'}
    else:
        expected = {'gandalf', 'manve'}
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected


def test_send_reminders_notify_everyone(simple_system, arda_users, settings):
    """Проверяем, что при отмеченной опции notify_everyone напоминания о запросах уходят всем,
    в том числе и отсутствующим"""

    frodo = arda_users.frodo
    workflow_code = 'approvers = [approver("legolas") | "gandalf", approver("varda") | "manve"]'
    set_workflow(simple_system, workflow_code)
    make_absent([arda_users.legolas, arda_users.manve])
    # отсутствует один подтверждающий из каждой пары. письма уходят неотсутствующим
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    clear_mailbox()
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 2
    expected = {'gandalf', 'varda'}
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected
    Role.objects.all().delete()

    make_attending([arda_users.legolas, arda_users.manve])

    # отсутствуют оба подтверждающих первой пары и один второй пары
    make_absent([arda_users.legolas, arda_users.gandalf, arda_users.varda])
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    clear_mailbox()
    call_command('idm_send_notifications')
    expected = {'legolas', 'manve'}
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected
    Role.objects.all().delete()

    make_attending([arda_users.legolas, arda_users.gandalf, arda_users.varda])

    # включим режим notify_everyone
    workflow_code = 'notify_everyone=True; %s' % workflow_code
    set_workflow(simple_system, workflow_code)
    # теперь опять отсутствует один подтверждающий из каждой пары. письма уходят всем четверым.
    make_absent([arda_users.legolas, arda_users.manve])
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    clear_mailbox()

    call_command('idm_send_notifications')
    assert len(mail.outbox) == 4
    expected = {'legolas', 'gandalf', 'varda', 'manve'}
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected
    assert {message.subject for message in mail.outbox} == {'Подтверждение ролей.'}


def test_send_digest_with_priorities(arda_users, simple_system):
    """
        Проверяет что дайджесты шлются людям с основным приоритетом
    """
    workflow_ = dedent("""
            approvers = [any_from(['gimli', 'sam'], priority=1) | approver('sauron', priority=2) |
            approver('bilbo', priority=10, notify=True),
            approver('galadriel', priority=2) | approver('varda', priority=4) |
            any_from(['saruman', 'gandalf', 'varda'], priority=1, notify=False),
            approver('boromir', priority=1, notify=False) | any_from(['meriadoc', 'legolas']) |
            any_from(['galadriel', 'sauron'], notify=True)]
            """)

    set_workflow(simple_system, workflow_, workflow_)

    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'}, {})
    # запустить пересчет основного приоритета перед рассылкой уведомлений
    ApproveRequest.objects.recalculate_main_priority()
    clear_mailbox()
    call_command('idm_send_notifications')

    expected = {'gimli', 'sam', 'galadriel', 'meriadoc', 'legolas'}
    assert len(mail.outbox) == len(expected)
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected

    # люди с основным приоритетом отсутсвуют, чтобы дайджесты пришли другим
    make_absent([arda_users.gimli, arda_users.sam, arda_users.meriadoc, arda_users.legolas])

    ApproveRequest.objects.recalculate_main_priority()

    clear_mailbox()

    call_command('idm_send_notifications')

    expected = {'sauron', 'galadriel', 'meriadoc', 'legolas'}

    assert len(mail.outbox) == len(expected)
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected


@pytest.mark.parametrize('is_rerequested', [False, True])
def test_send_digest_with_priorities_(arda_users, simple_system, settings, is_rerequested):
    """
         Проверяет что дайджесты шлются людям с основным приоритетом.
         Дайджесты шлются несмотря на отсутствия и флаги notify
    """
    workflow_ = dedent("""
                approvers = [any_from(['gimli', 'sam'], priority=1) | approver('sauron', priority=2) |
                approver('bilbo', priority=10, notify=True),
                approver('galadriel', priority=2) | approver('varda', priority=4) |
                any_from(['saruman', 'gandalf', 'varda'], priority=1, notify=False),
                approver('boromir', priority=1, notify=False) | any_from(['meriadoc', 'legolas']) |
                any_from(['galadriel', 'sauron'], notify=True)]
                """)

    set_workflow(simple_system, workflow_, workflow_)

    if is_rerequested:
        role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'})
        role.set_state('need_request')
        role.rerequest(arda_users.frodo)
        role = refresh(role)
        assert role.state == 'rerequested'
    else:
        Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'}, {})
    # запустить пересчет основного приоритета перед рассылкой уведомлений
    ApproveRequest.objects.recalculate_main_priority()
    clear_mailbox()
    call_command('idm_send_notifications')

    # если приоритет у апруверов одинаковый, то они сортируются по id
    expected = {'gimli', 'sam', 'galadriel', 'meriadoc', 'legolas'}
    assert len(mail.outbox) == len(expected)
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected

    make_absent([arda_users.gimli, arda_users.meriadoc])

    ApproveRequest.objects.recalculate_main_priority()
    clear_mailbox()

    call_command('idm_send_notifications')
    expected = {'gimli', 'sam', 'galadriel', 'meriadoc', 'legolas'}

    assert len(mail.outbox) == len(expected)
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected


def test_send_digest_someone_found_false(arda_users, simple_system):
    workflow = dedent("""
            approvers = [
                approver('sam', priority=1, notify=False) | approver('bilbo', priority=1) | approver('varda', notify=True),
                any_from(['gimli', 'sauron'], notify=False)
            ]
    """)
    set_workflow(simple_system, workflow)
    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'}, {})
    # поставить несуществующий приоритет

    Approve.objects.update(main_priority=111)
    clear_mailbox()
    call_command('idm_send_notifications')

    expected = {'sam', 'bilbo', 'gimli'}
    assert len(mail.outbox) == len(expected)
    assert {message.to[0].split('@', 1)[0] for message in mail.outbox} == expected
