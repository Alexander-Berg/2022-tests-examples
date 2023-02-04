# coding: utf-8


import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.tests.utils import raw_make_role

pytestmark = pytest.mark.django_db(transaction=True)

@pytest.mark.skip
def test_deduplicate_roles_with_active(simple_system, arda_users, rollback_uniqueness):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    for state in ROLE_STATE.ALL_STATES - {'granted'}:
        role.state = state
        role.is_active = state in ROLE_STATE.ACTIVE_STATES
        role.pk = None
        role.save()

    call_command('idm_oneoff_deduplicate_roles')

    remaining_roles = Role.objects.filter(state__in=ROLE_STATE.RETURNABLE_STATES)
    assert remaining_roles.count() == 1

    remaining_role = remaining_roles.get()
    assert remaining_role.state == 'granted'
    assert mail.outbox == []

@pytest.mark.skip
def test_deduplicate_roles_without_active(simple_system, arda_users, rollback_uniqueness):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='created')

    for state in ROLE_STATE.ALL_STATES - ROLE_STATE.ACTIVE_STATES - {'created'}:
        role.state = state
        role.is_active = state in ROLE_STATE.ACTIVE_STATES
        role.is_returnable = state in ROLE_STATE.RETURNABLE_STATES
        role.pk = None
        role.save()
    remaining_states = ROLE_STATE.RETURNABLE_STATES
    remaining_pks = Role.objects.filter(state__in=remaining_states).values_list('pk', flat=True)

    call_command('idm_oneoff_deduplicate_roles')

    remaining_roles = Role.objects.filter(state__in=remaining_states)
    assert remaining_roles.count() == 1

    remaining_role = remaining_roles.get()
    assert remaining_role.pk == max(remaining_pks)
    assert remaining_role.state in remaining_states
    assert mail.outbox == []
