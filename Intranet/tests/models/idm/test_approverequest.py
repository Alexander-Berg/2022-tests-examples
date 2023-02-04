# coding: utf-8

from textwrap import dedent

import pytest
from django.core import mail

from idm.core.models import Approve, ApproveRequest, Role
from idm.core.workflow.shortcuts import workflow
from idm.core.workflow.plain.user import userify
from idm.tests.utils import set_workflow, clear_mailbox, make_absent, make_attending

pytestmark = [pytest.mark.django_db]


def get_data_after_applying_workflow(workflow_, superuser_node):
    """
        Функция принимает текстовый workflow, прогоняет его и возвращает данные.
        Используется вместе с фикстурами superuser_node, arda_users
    """
    return workflow(workflow_, requester=userify('gandalf'), subject=userify('galadriel'),
                    role_data={'role': 'manager'},
                    system=superuser_node.system,
                    node=superuser_node,
                    )


def test_save_priority_in_approverequest(arda_users, simple_system, department_structure):
    """
        Проверка сохранения приоритетов approver'ов  в базу
    """
    # priority_policy = no_deputies
    workflow_ = dedent("""
        approvers = [userify('gimli').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)]
        """)

    set_workflow(simple_system, workflow_, workflow_)

    frodo = arda_users.frodo

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'})
    # создана одна OR-группа.  Всего 5 аппруверов
    assert Approve.objects.count() == 1
    assert ApproveRequest.objects.count() == 5

    expected_result = {
        'frodo': 2,
        'galadriel': 1,
        'sam': 1,
        'gandalf': 1,
        'varda': 4,
    }

    for index, approverequest in enumerate(ApproveRequest.objects.select_related('approver')):
        username = approverequest.approver.username
        assert username in expected_result
        assert approverequest.priority == expected_result[username]


def test_recalculating_main_priority(arda_users, simple_system):
    """
        Проверяет случаи когда основной приоритет группы пересчитывается
    """
    workflow_ = dedent("""
    approvers = [approver('gimli', priority=1) | approver('gandalf', priority=1) | approver('sam', priority=2) |
                 approver('frodo', priority=2), any_from(['bilbo', 'galadriel'], priority=1),
                 any_from(['saruman', 'boromir', 'varda'])]
    """)

    set_workflow(simple_system, workflow_, workflow_)

    meriadoc = arda_users.meriadoc

    Role.objects.request_role(meriadoc, meriadoc, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 6

    for message, recipient in zip(
            sorted(mail.outbox, key=lambda message: message.to[0]),
            ['bilbo', 'galadriel', 'gandalf', 'gimli', 'meriadoc', 'saruman']
    ):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(recipient)

    clear_mailbox()

    assert Approve.objects.count() == 3
    assert ApproveRequest.objects.count() == 9

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 0  # все уже получали письма

    assert list(Approve.objects.values_list('main_priority', flat=True)) == [1, 1, 1]

    # gimli, gandalf и saruman теперь отсутствуют. Основные приоритеты для групп должны поменяться
    make_absent([arda_users.gimli, arda_users.gandalf, arda_users.bilbo, arda_users.saruman])

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 3  # galadriel получала уведомление раньше

    for message, recipient in zip(sorted(mail.outbox, key=lambda message: message.to[0]), ['boromir', 'frodo', 'sam']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(recipient)

    clear_mailbox()

    assert list(Approve.objects.values_list('main_priority', flat=True)) == [2, 1, 2]

    make_absent([arda_users.sam, arda_users.galadriel, arda_users.boromir])

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 1

    for message, recipient in zip(mail.outbox, ['varda']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(recipient)

    clear_mailbox()

    assert list(Approve.objects.values_list('main_priority', flat=True)) == [2, 1, 3]

    make_absent([arda_users.varda])
    make_attending([arda_users.frodo, arda_users.boromir, arda_users.bilbo, arda_users.gandalf])

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 0  # все уже получили письма

    clear_mailbox()

    assert list(Approve.objects.values_list('main_priority', flat=True)) == [1, 1, 2]


def test_not_sending_notification_after_recalculating_main_priority(arda_users, simple_system):
    """
        Проверяет что после пересчета основного приоритета уведомление не уходит аппруверам (ApproveRequest),
        у которых approve.approved != None
    """
    workflow_ = dedent("""
       approvers = [approver('gimli', priority=1) | approver('gandalf', priority=1) | approver('sam', priority=2) |
                    approver('frodo', priority=2), any_from(['bilbo', 'galadriel'], priority=1),
                    any_from(['saruman', 'boromir', 'varda'])]
       """)

    set_workflow(simple_system, workflow_, workflow_)

    meriadoc = arda_users.meriadoc

    Role.objects.request_role(meriadoc, meriadoc, simple_system, '', {'role': 'admin'})

    # очистить почту и присвоить всем флагам False, чтобы уведомление пришло
    clear_mailbox()
    ApproveRequest.objects.update(is_notification_sent=False)

    # подтверждение из первой группы
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.gimli)
    approve_request.set_approved(arda_users.gimli)

    ApproveRequest.objects.recalculate_main_priority()

    # никому из первой группы уведомление не пришло
    assert len(mail.outbox) == 3
    mail_suffix = '{}@example.yandex.ru'
    for message, expected_recipient in zip(
            sorted(mail.outbox, key=lambda message: message.to[0]),
            ['bilbo', 'galadriel', 'saruman']
    ):
        assert len(message.to) == 1 and message.to[0] == mail_suffix.format(expected_recipient)


def test_send_message_to_all_absenters(arda_users, simple_system):
    """
        Проверяет случай когда все в одной OR-группе отсутствуют. В этом случае основной приоритет группы становится
        минимальным приоритетом апрувера с notify != False
    """
    workflow_ = dedent("""
        approvers = [any_from(['saruman', 'boromir', 'varda'])]"""
    )

    set_workflow(simple_system, workflow_, workflow_)

    make_absent([arda_users.saruman, arda_users.boromir, arda_users.varda])

    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 2

    for message, recipient in zip(sorted(mail.outbox, key=lambda message: message.to[0]), ['frodo', 'saruman']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(recipient)

    # чтобы проверить уведомаления после пересчета приоритетов
    ApproveRequest.objects.all().update(is_notification_sent=False)

    clear_mailbox()

    assert Approve.objects.count() == 1
    assert ApproveRequest.objects.count() == 3

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 1  # уведомление пришло всем

    for message, recipient in zip(mail.outbox, ['saruman']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(recipient)

    assert list(Approve.objects.values_list('main_priority', flat=True)) == [ApproveRequest.objects.
                                                                             get(approver=arda_users.saruman).priority]
