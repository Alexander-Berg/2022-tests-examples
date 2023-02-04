# coding: utf-8


from django.core import management
from django.utils import timezone
from freezegun import freeze_time
from mock import call, patch
import pytest

from idm.core.models import System, SystemRolePush
from idm.tests.utils import refresh

pytestmark = [pytest.mark.django_db]


def test_reset_system_creators(simple_system, arda_users):
    """Проверим, что можно позвать команду с nowis в будущем, и она отзовёт необходимые роли"""

    frodo = arda_users.frodo

    system1 = System.objects.create(slug='1', added=timezone.now() - timezone.timedelta(hours=30), creator=None)
    system2 = System.objects.create(slug='2', added=timezone.now() - timezone.timedelta(hours=30), creator=frodo)
    system3 = System.objects.create(slug='3', added=timezone.now() - timezone.timedelta(hours=20), creator=None)
    system4 = System.objects.create(slug='4', added=timezone.now() - timezone.timedelta(hours=20), creator=frodo)
    SystemRolePush.objects.create(system=system4)

    with patch('idm.core.management.commands.idm_reset_system_creators.log.info') as log:
        management.call_command('idm_reset_system_creators')

    system1 = refresh(system1)
    system2 = refresh(system2)
    system3 = refresh(system3)
    system4 = refresh(system4)

    assert system1.creator is None
    assert system2.creator is None
    assert system3.creator is None
    assert system4.creator_id == frodo.id

    assert log.call_args_list == [call('Resetting creator for system %s', system2.slug)]
