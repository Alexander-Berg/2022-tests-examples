import pytest
from django.core.management import call_command

from idm.core.models import Action, RoleNodeResponsibilityAction
from idm.tests.utils import refresh_from_db

pytestmark = [pytest.mark.django_db]


def test_migrate_actions():
    assert Action.objects.count() == 0
    assert RoleNodeResponsibilityAction.objects.count() == 0

    action_to_stable = Action.objects.create(action='role_node_created')
    action_to_migrate = Action.objects.create(action='role_node_responsibility_created')
    Action.objects.create(action='role_node_responsibility_created')

    call_command('idm_migrate_actions')

    action_migrated = RoleNodeResponsibilityAction.objects.get(pk=action_to_migrate.pk)
    assert action_migrated.action == action_to_migrate.action
    assert action_migrated.added == action_to_migrate.added

    with pytest.raises(Action.DoesNotExist):
        refresh_from_db(action_to_migrate)

    action_stabled = Action.objects.get()
    assert action_stabled.action == action_to_stable.action
    assert action_stabled.added == action_to_stable.added
