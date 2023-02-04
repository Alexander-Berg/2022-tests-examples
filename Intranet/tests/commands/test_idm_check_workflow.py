# -*- coding: utf-8 -*-


import pytest
from django.core import mail

from idm.core.workflow.exceptions import WorkflowError, ApproverNotFoundError
from idm.tests.utils import set_workflow, assert_contains, get_recievers, raw_make_role

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def test_check_workflow_raises_error(simple_system, complex_system, arda_users, department_structure, settings):
    """
    Проверяем, что если роль невозможно подтвердить из-за того, что одна из or-групп полностью состоит из уволенных
    сотрудников, то поднимается WorkflowError
    """

    role_simple = raw_make_role(arda_users.legolas, simple_system, {'role': 'admin'}, state='requested')

    arda_users.legolas.is_active = False
    arda_users.legolas.save()

    set_workflow(simple_system, 'approvers = ["legolas"]')
    simple_system.emails = 'simple@example.com'
    simple_system.save()

    with pytest.raises(WorkflowError):
        role_simple.apply_workflow(arda_users.legolas)


def test_check_workflow_sends_notification(simple_system, complex_system, arda_users, department_structure, settings):
    """Проверим письма об уволенных в воркфлоу"""

    role_complex = raw_make_role(arda_users.frodo, complex_system, {'role': 'admin', 'project': 'rules'},
                                 state='requested')

    arda_users.frodo.is_active = False
    arda_users.frodo.save()

    set_workflow(complex_system, 'approvers = [approver("frodo") | "gandalf"]')
    complex_system.emails = 'complex@example.com'
    complex_system.save()

    role_complex.apply_workflow(arda_users.frodo)

    assert len(mail.outbox) == 3
    current_mail = mail.outbox[0]
    assert get_recievers(mail.outbox) == {
        'complex@example.com',
        'security-alerts@yandex-team.ru',
        'idm-notification@yandex-team.ru'
    }
    assert_contains([
        'В workflow системы "Complex система" обнаружены уволенные сотрудники:',
        'frodo',
    ], current_mail.body)


def test_check_workflow_sends_notification_on_nonexistent_user(simple_system, complex_system, arda_users,
                                                               department_structure):
    """Проверим письмо о нахождении в workflow несуществующего сотрудника"""

    role_complex = raw_make_role(arda_users.frodo, complex_system, {'role': 'admin', 'project': 'rules'},
                                 state='requested')

    arda_users.frodo.is_active = False
    arda_users.frodo.save()

    set_workflow(complex_system, 'approvers = ["fake_frodo"]')
    complex_system.emails = 'complex@example.com'
    complex_system.save()

    with pytest.raises(ApproverNotFoundError):
        role_complex.apply_workflow(arda_users.frodo)

    assert len(mail.outbox) == 3
    current_mail = mail.outbox[0]
    assert get_recievers(mail.outbox) == {
        'complex@example.com',
        'security-alerts@yandex-team.ru',
        'idm-notification@yandex-team.ru'
    }
    assert_contains([
        'В workflow системы "Complex система" обнаружены несуществующие сотрудники:',
        'fake_frodo',
    ], current_mail.body)
