# coding: utf-8


import pytest

from django.core.management import call_command
from django.db import connection
from django.utils import timezone
from metrics_framework.models import Metric

from idm.core.models import RoleNode

pytestmark = [pytest.mark.django_db]


def test_count(complex_system, client):
    Metric.objects.create(
        slug='closure_inconsistencies_count',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )
    node = RoleNode.objects.get(slug='rules')
    correct_descendants = {'rules', 'role', 'admin', 'auditor', 'invisic'}
    assert set(node.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants

    response = client.get('/monitorings/closure-inconsistencies-count/')
    assert response.status_code == 400
    assert response.content == b'No data'

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistencies-count/')
    assert response.status_code == 200

    to_delete_slugs = ['rules', 'invisic']
    to_delete_nodes = RoleNode.objects.filter(slug__in=to_delete_slugs)
    with connection.cursor() as cursor:
        for item in to_delete_nodes:
            cursor.execute('DELETE FROM upravlyator_rolenodeclosure WHERE child_id=%s', [item.pk])

    assert set(node.get_descendants().values_list('slug', flat=True)) == {'role', 'admin', 'auditor'}

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistencies-count/')
    assert response.status_code == 400

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)
    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistencies-count/')
    assert response.status_code == 200


def test_paths(complex_system, client):
    Metric.objects.create(
        slug='closure_inconsistent_paths',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )

    response = client.get('/monitorings/closure-inconsistent-paths/')
    assert response.status_code == 400
    assert response.content == b'No data'

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistent-paths/')
    assert response.status_code == 200

    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    RoleNode.objects.filter(pk=node.pk).update(
        slug_path='/project/rulezz/rol/',
        value_path='/pro/',
        fullname=[],
    )
    assert not RoleNode.objects.filter(slug_path='/project/rules/role/').exists()

    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistent-paths/')
    assert response.status_code == 400

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)
    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    assert node.value_path == '/rules/'
    call_command('start_metrics_tasks')
    response = client.get('/monitorings/closure-inconsistent-paths/')
    assert response.status_code == 200
