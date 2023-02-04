# coding: utf-8


import pytest
from django.core import mail

from idm.core.models import Role
from idm.tests.utils import refresh, set_workflow

pytestmark = [pytest.mark.django_db]


def test_silent_notice(simple_system, arda_users):
    """Проверяем отсутствие рассылки писем при выдаче роли с параметром silent (без подтверждающих)"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []; email_cc = ["sauron@example.yandex.ru"]')

    # передаём параметр — писем нет
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None, silent=True)

    assert frodo.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 0

    # не передаём параметр — письма есть
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None, silent=False)

    assert frodo.roles.count() == 2
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 2


def test_silent_notice_affects_granted_only(simple_system, arda_users):
    """Проверяем, что silent-флаг не влияет на письма про отзыв роли"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []; email_cc = ["sauron@example.yandex.ru"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None, silent=True)
    assert frodo.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 0

    role = refresh(role)
    role.deprive_or_decline(frodo)

    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'Simple система. Роль отозвана'
    assert message.to == ['frodo@example.yandex.ru']


def test_silent_for_request(simple_system, arda_users):
    """Проверяем, что владельцу не уходит письмо, если роль требует подтверждения"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["varda"]; email_cc = ["sauron@example.yandex.ru"]')
    Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'manager'}, None, silent=True)

    # ушло письмо подтвеждающей, но не владельцу роли
    assert len(mail.outbox) == 1
    message = mail.outbox.pop()
    assert message.to == ['varda@example.yandex.ru']
    assert message.subject == 'Подтверждение роли. Simple система.'
