# coding: utf-8


import datetime

import pytest
from django.core.management import call_command
from django.utils import timezone
from idm.core.models import Role
from idm.tests.utils import refresh

pytestmark = pytest.mark.django_db


def test_idm_restores_good_time(simple_system, arda_users):
    """Проверим, что запрос, выполняемый командой, действительно выставляет правильное время"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    request, apply_workflow, approve, first_add_role_push, grant = role.actions.order_by('pk')
    t1 = timezone.make_aware(datetime.datetime(1986, 4, 26, 1, 23, 45), timezone.utc)
    t2 = timezone.make_aware(datetime.datetime(1999, 4, 26, 1, 23, 45), timezone.utc)
    t3 = timezone.make_aware(datetime.datetime(2014, 3, 18, 1, 10, 10), timezone.utc)
    t4 = timezone.make_aware(datetime.datetime(2024, 3, 18, 1, 10, 10), timezone.utc)

    request.added = t1
    request.save()
    apply_workflow.added = t3
    apply_workflow.save()
    approve.added = t4
    approve.save()
    grant.added = t2
    grant.save()

    assert role.updated != t4
    call_command('idm_restore_updated')
    role = refresh(role)
    assert role.updated == t4
