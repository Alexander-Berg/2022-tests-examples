# coding: utf-8
from __future__ import absolute_import

import pytest

from django.utils import timezone

from idm.core.models import System, SystemRolePush

pytestmark = [pytest.mark.django_db]


def test_users_without_department_group_ok(client, arda_users):
    response = client.get('/monitorings/not-pushed-system-roles/')
    assert response.status_code == 200


def test_users_without_department_group_fail(client, arda_users):
    new_system = System.objects.create(slug='new', name='new', name_en='new')
    old_system = System.objects.create(slug='old', name='old', name_en='old')
    old_system.added = timezone.now() - timezone.timedelta(hours=1)
    old_system.save()

    SystemRolePush.objects.create(system=old_system)
    SystemRolePush.objects.create(system=old_system)
    SystemRolePush.objects.create(system=new_system)

    response = client.get('/monitorings/not-pushed-system-roles/')
    assert response.status_code == 400
    assert response.content == b'IDM has 1 systems with not pushed responsibles or team members: old'
