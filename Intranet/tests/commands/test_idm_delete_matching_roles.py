# coding: utf-8


import pytest
from django.core.management import call_command
from django.utils import timezone

from idm.core.models import Action
from idm.inconsistencies.models import Inconsistency, MatchingRole
from idm.tests.utils import raw_make_role

pytestmark = [
    pytest.mark.django_db,
]


def test_delete_matching_roles(simple_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'})
    action = Action.objects.create(id=-1)
    inconsistency = Inconsistency.objects.create(
        system=simple_system,
        state='active',
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        sync_key_id=action.id,
    )
    MatchingRole.objects.create(role_id=role.id, inconsistency_id=inconsistency.id)
    assert MatchingRole.objects.count() == 1
    call_command('idm_delete_matching_roles')
    assert MatchingRole.objects.count() == 1
    action.added = timezone.now() - timezone.timedelta(days=8)
    action.save()
    call_command('idm_delete_matching_roles')
    assert MatchingRole.objects.count() == 0
