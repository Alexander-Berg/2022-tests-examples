# coding: utf-8


import pytest

from waffle.testutils import override_switch

from django.core import mail
from django.core.management import call_command

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.tests.utils import set_workflow, clear_mailbox

pytestmark = pytest.mark.django_db


def test_idm_poke_role_in_inactive_systems(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code='approvers=[]')

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == fellowship.members.count() + 2  # + персональная роль frodo и групповая роль
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()

    simple_system.is_active = False
    simple_system.save(update_fields=['is_active'])
    clear_mailbox()

    # Без включенного флага роли не отзываются
    call_command('idm_poke_role_in_inactive_systems')
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()

    # Если передать флаг force, то роли отзовутся даже без флага
    call_command('idm_poke_role_in_inactive_systems', '--force')
    assert len(mail.outbox) == 0
    assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == Role.objects.count()

    Role.objects.update(state=ROLE_STATE.GRANTED, is_active=True)

    # При включенном флаге роли отзываются
    with override_switch('idm.regular_deprive_roles_in_inactive_systems', active=True):
        call_command('idm_poke_role_in_inactive_systems')
    assert len(mail.outbox) == 0
    assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == Role.objects.count()
