# coding: utf-8


import pytest

from idm.core.models import Role, ApproveRequest, Approve
from idm.tests.utils import set_workflow, assert_action_chain

pytestmark = pytest.mark.django_db


def test_priority_recalculation(simple_system, arda_users):
    "Основной приоритет пересчитывается после отморозки"
    set_workflow(
        simple_system,
        'approvers = [approver("aragorn", priority=1)| approver("sauron", priority=1) | approver("legolas", priority=2)]'
    )

    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    approve = Approve.objects.get()
    assert approve.main_priority == 1

    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.aragorn).set_ignored(arda_users.aragorn)
    approve.refresh_from_db()
    assert approve.main_priority == 1

    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.sauron).set_ignored(arda_users.sauron)
    approve.refresh_from_db()
    assert approve.main_priority == 2


def test_decline_after_ignore(simple_system, arda_users, idm_robot):
    "Роль отклоняется после отморозки всех подтверждающих"
    set_workflow(simple_system, 'approvers = [approver("aragorn", priority=1) | approver("legolas", priority=2)]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert role.state == 'requested'

    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.aragorn).set_ignored(arda_users.aragorn)
    role.refresh_from_db()
    assert role.state == 'requested'
    assert role.actions.get(action='ignore').requester_id == arda_users.aragorn.id

    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.legolas).set_ignored(arda_users.legolas)
    role.refresh_from_db()
    assert role.state == 'declined'
    assert list(role.actions.filter(action='ignore').order_by('added').values_list('requester', flat=True)) == [
        arda_users.aragorn.id, arda_users.legolas.id]
    assert role.actions.get(action='decline').requester == None

    assert_action_chain(role, ['request', 'apply_workflow', 'ignore', 'ignore', 'decline'])
