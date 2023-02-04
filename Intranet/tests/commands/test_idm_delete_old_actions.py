# coding: utf-8


from django.utils import timezone

import pytest
from django.core.management import call_command

from idm.core.models import ActionTiny


pytestmark = [pytest.mark.django_db]


def test_idm_delete_old_actions():
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""
    ActionTiny.objects.create(start=timezone.now(), finish=timezone.now())
    ActionTiny.objects.create(start=timezone.now(), finish=timezone.now() - timezone.timedelta(weeks=99999))

    call_command('idm_delete_old_actions')
    assert ActionTiny.objects.count() == 1

