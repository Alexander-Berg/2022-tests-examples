# coding: utf-8


import pytest
from django.core import mail

from idm.core.workflow.exceptions import BrokenSystemError, Forbidden
from idm.core.models import Role, ApproveRequest
from idm.tests.utils import set_workflow, refresh, raw_make_role

pytestmark = pytest.mark.django_db


def test_role_actions_when_system_is_broken(simple_system, arda_users):
    """Тестирует различные действия с ролями сломанной системы. С ними ничего происходить не должно"""

    frodo = arda_users.frodo
    simple_system.is_broken = True
    simple_system.save()

    # попытка запросить роль тихо сфейлится
    with pytest.raises(BrokenSystemError) as info:
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert str(info.value) == 'Система "Simple система" сломана. Роль не может быть запрошена.'

    assert frodo.roles.count() == 0
    assert len(mail.outbox) == 0

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    assert frodo.roles.count() == 1

    # попытка отозвать роль тихо сфейлится
    with pytest.raises(Forbidden):
        role.deprive_or_decline(frodo)

    assert frodo.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active is True
    assert role.actions.count() == 0

    # попробуем изменить состояние роли непосредственно - получим exception
    with pytest.raises(BrokenSystemError):
        role.set_state('review_request')

    assert frodo.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active is True
    assert role.actions.count() == 0


def test_approve_when_system_broken(client, simple_system, arda_users):
    """Тестирует подтверждение ранее запрошенной роли"""
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    # запросим роль
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    assert role.state == 'requested'
    assert role.is_active is False
    role_request = role.get_last_request()
    assert role_request.approves.count() == 1
    assert role_request.is_done is False

    # сломаем систему и попробуем подтвердить роль - должно сфейлиться
    simple_system.is_broken = True
    simple_system.save()

    client.login('legolas')
    approvereq = ApproveRequest.objects.select_related_for_set_decided().get()

    with pytest.raises(BrokenSystemError):
        approvereq.set_approved(arda_users.legolas)

    role = refresh(role)
    assert role.state == 'requested'
    assert role.is_active is False
    role_request = refresh(role_request)
    assert role_request.approves.count() == 1
    assert role_request.is_done is False
