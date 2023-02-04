# coding: utf-8


import difflib

import pytest
from django.conf import settings
from django.core import mail

from idm.core.models import Workflow
from idm.tests.utils import set_workflow, assert_contains, add_perms_by_role, refresh, get_recievers

pytestmark = [pytest.mark.django_db]


def get_diff(wf_code1, wf_code2):
    from itertools import islice
    diff = difflib.unified_diff(
        wf_code1.splitlines() if wf_code1 else [],
        wf_code2.splitlines() if wf_code2 else [],
    )
    diff = islice(diff, 2, None)
    diff = (line.strip('\n') for line in diff)
    diff = '\n'.join(diff)
    return diff


def test_workflow_long_comment(simple_system, arda_users):
    """Проверка работы с длинными комментариями"""

    frodo = arda_users.get('frodo')
    add_perms_by_role('responsible', frodo, simple_system)
    long_comment = ' '.join(['bla'] * 255)

    workflow = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=frodo,
        workflow='153',
        comment = long_comment
    )
    workflow.approve(frodo)


def test_send_email_on_change_workflow(simple_system, arda_users):
    """Проверяем отправку письма об изменении workflow"""

    initial = 'approvers = [approver("art"), approver("fantom")]'
    frodo = arda_users.get('frodo')
    set_workflow(simple_system, initial, user=frodo)
    add_perms_by_role('responsible', frodo, simple_system)

    workflow = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=frodo,
        workflow='153',
    )
    workflow.approve(frodo)

    workflow2 = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=frodo,
        workflow='querty',
    )
    workflow2.approve(frodo)

    assert len(mail.outbox) == 4

    message = mail.outbox[0]
    assert get_recievers(mail.outbox) == set(settings.EMAILS_FOR_REPORTS)
    assert message.subject == 'Изменение workflow системы Simple система.'
    diff = get_diff(initial, workflow.workflow)
    assert_contains(
        [
            'Пользователь Фродо Бэггинс',
            'изменил workflow системы "Simple система":',
            diff,
            'Ваш IDM'
        ],
        message.body
    )

    message = mail.outbox[3]
    assert message.subject == 'Изменение workflow системы Simple система.'
    diff = get_diff(workflow.workflow, workflow2.workflow)
    assert_contains(
        [
            'Пользователь Фродо Бэггинс',
            'изменил workflow системы "Simple система":',
            diff,
            'Ваш IDM'
        ],
        message.body
    )


def test_send_email_on_group_workflow_change(simple_system, arda_users):
    """Отсылаем письмо при изменении группового workflow"""

    initial = 'approvers = [approver("legolas"), approver("saruman")]'
    set_workflow(simple_system, code=initial, group_code=initial)
    frodo = arda_users.get('frodo')
    add_perms_by_role('responsible', frodo, simple_system)

    workflow = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=frodo,
        workflow='qwerty',
        group_workflow='qwe'
    )
    workflow.approve(frodo)

    workflow2 = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=frodo,
        workflow='approvers=[]',
        group_workflow='approvers=["frodo"]'
    )
    workflow2.approve(frodo)

    assert len(mail.outbox) == 4

    message = mail.outbox[0]
    assert get_recievers(mail.outbox) == set(settings.EMAILS_FOR_REPORTS)
    assert message.subject == 'Изменение workflow системы Simple система.'
    diff = get_diff(initial, workflow.workflow)
    group_diff = get_diff(initial, workflow.group_workflow)
    assert_contains(
        [
            'Пользователь Фродо Бэггинс',
            'изменил workflow системы "Simple система":',
            diff,
            group_diff,
            'Ваш IDM'
        ],
        message.body
    )

    message = mail.outbox[3]
    assert message.subject == 'Изменение workflow системы Simple система.'
    diff = get_diff(workflow.workflow, workflow2.workflow)
    group_diff = get_diff(workflow.group_workflow, workflow2.group_workflow)
    assert_contains(
        [
            'Пользователь Фродо Бэггинс',
            'изменил workflow системы "Simple система":',
            diff,
            group_diff,
            'Ваш IDM'
        ],
        message.body
    )


def test_notification_email_on_change(simple_system, arda_users):
    """Проверяем, что если workflow подтверждает не тот человек, который создал,
    то создавшему отправляется нотификация"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    gandalf = arda_users.get('gandalf')
    initial = 'approvers = [approver("legolas"), approver("saruman")]'
    group_initial = 'approvers = [approver("legolas")]'
    set_workflow(simple_system, code=initial, group_code=group_initial, user=frodo)
    add_perms_by_role('responsible', gandalf, simple_system)
    add_perms_by_role('responsible', frodo, simple_system)

    workflow = Workflow.objects.create(
        system=simple_system,
        state='commited',
        user=legolas,
        workflow='qwerty',
        group_workflow='qwe'
    )
    workflow.approve(frodo)
    diff = get_diff(initial, workflow.workflow)
    group_diff = get_diff(group_initial, workflow.group_workflow)
    assert len(mail.outbox) == 3
    message1, message2, _ = mail.outbox
    assert message1.subject == 'Ваши изменения workflow системы Simple система применены.'
    assert message2.subject == 'Изменение workflow системы Simple система.'
    assert get_recievers(mail.outbox) == {'legolas@example.yandex.ru'} | set(settings.EMAILS_FOR_REPORTS)
    assert_contains(['Ваши правки workflow в системе Simple система одобрены и применены:'], message1.body)
    assert_contains(['Пользователь legolas изменил workflow системы "Simple система":'], message2.body)
    for message in (message1, message2):
        assert_contains([diff, group_diff], message.body)


def test_notification_on_commit(simple_system, arda_users):
    """Проверяем, что при отправке workflow на утверждение отсылаются письма"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    gandalf = arda_users.get('gandalf')
    add_perms_by_role('superuser', gandalf)
    add_perms_by_role('responsible', legolas, simple_system)
    add_perms_by_role('users_view', frodo, simple_system)
    initial = 'approvers = []'
    set_workflow(simple_system, initial, group_code=initial)

    wf = Workflow.objects.create(
        system=simple_system,
        workflow='approvers = ["frodo"]',
        group_workflow='approvers = ["frodo"]',
        state='edit',
        user=frodo,
        approver=None,
        approved=None
    )
    wf.commit(frodo)
    wf = refresh(wf)
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    expected_to = {'legolas@example.yandex.ru', 'gandalf@example.yandex.ru'} | set(settings.EMAILS_FOR_REPORTS)
    assert get_recievers(mail.outbox) == expected_to
    assert message.subject == 'Изменение workflow системы Simple система требует подтверждения.'
    expected_link = 'https://example.com/system/simple'

    diff = get_diff(initial, wf.workflow)
    group_diff = get_diff(initial, wf.group_workflow)

    assert_contains(['Фродо Бэггинс',
                     'отредактировал workflow системы Simple система:',
                     'Изменения пользовательского workflow',
                     'Изменения группового workflow',
                     'Пожалуйста, подтвердите изменения',
                     diff,
                     group_diff,
                     expected_link], message.body)
