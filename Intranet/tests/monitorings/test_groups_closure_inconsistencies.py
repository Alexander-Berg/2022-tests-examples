# coding: utf-8


import pytest

from django.core.management import call_command
from django.db import connection
from django.utils import timezone
from metrics_framework.models import Metric

from idm.users.models import Group

pytestmark = [pytest.mark.django_db]


def test_groups_count(department_structure, client):
    Metric.objects.create(
        slug='groups_closure_inconsistent_count',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )

    response = client.get('/monitorings/groups-closure-inconsistent-count/')
    assert response.status_code == 400
    assert response.content == b'No data'

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/groups-closure-inconsistent-count/')
    assert response.status_code == 200

    group = Group.objects.get(slug='middle-earth')

    with connection.cursor() as cursor:
        cursor.execute('DELETE FROM users_groupclosure WHERE child_id=%s', [group.pk])

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/groups-closure-inconsistent-count/')
    assert response.status_code == 400

    call_command('idm_oneoff_fix_groups_tree')
    call_command('start_metrics_tasks')
    response = client.get('/monitorings/groups-closure-inconsistent-count/')
    assert response.status_code == 200
