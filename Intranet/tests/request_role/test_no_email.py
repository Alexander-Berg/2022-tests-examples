# coding: utf-8


import pytest
from django.core import mail

from idm.core.models import Role, ApproveRequest
from idm.tests.utils import set_workflow, refresh

pytestmark = pytest.mark.django_db


def test_no_email_workflow(simple_system, arda_users):
    """Проверяем отсутствие рассылки писем при выдаче роли с параметром no_email"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    # 1 - при отсутствии подтверждающих
    set_workflow(simple_system, 'no_email=True; approvers = []')

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert frodo.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 0

    # 2 - при наличии подтверждающих
    set_workflow(simple_system, 'approvers = [approver("legolas")]; no_email=True')

    # аппурувер отклонит роль, писем нет
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert len(mail.outbox) == 1  # письмо аппруверу
    message = mail.outbox[0]
    assert message.subject == 'Подтверждение роли. Simple система.'
    assert message.to == ['legolas@example.yandex.ru']

    role = refresh(role)
    assert frodo.roles.count() == 2
    assert role.state == 'requested'

    assert ApproveRequest.objects.count() == 1
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.set_declined(legolas)

    role = refresh(role)
    assert frodo.roles.count() == 2
    assert role.state == 'declined'

    assert len(mail.outbox) == 1

    # аппурувер подтвердит - писем нет
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert len(mail.outbox) == 2  # второе письмо аппруверу
    message = mail.outbox[-1]
    assert message.subject == 'Подтверждение роли. Simple система.'
    assert message.to == ['legolas@example.yandex.ru']

    role = refresh(role)
    assert frodo.roles.count() == 3
    assert role.state == 'requested'

    assert ApproveRequest.objects.count() == 2
    ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role).set_approved(legolas)

    role = refresh(role)
    assert frodo.roles.count() == 3
    assert role.state == 'granted'

    assert len(mail.outbox) == 2  # только письма аппруверам о подтверждении роли
