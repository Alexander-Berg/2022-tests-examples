# coding: utf-8


import pytest
from django.core.management import call_command
from django.db import connection

from idm.core.models import RoleNode

pytestmark = [pytest.mark.django_db]


def test_recover_deleted_closures(complex_system):
    node = RoleNode.objects.get(slug='rules')
    correct_descendants = {'rules', 'role', 'admin', 'auditor', 'invisic'}
    assert set(node.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants

    to_delete_slugs = ['rules', 'invisic']
    to_delete_nodes = RoleNode.objects.filter(slug__in=to_delete_slugs)
    with connection.cursor() as cursor:
        for item in to_delete_nodes:
            cursor.execute('DELETE FROM upravlyator_rolenodeclosure WHERE child_id=%s', [item.pk])

    assert set(node.get_descendants().values_list('slug', flat=True)) == {'role', 'admin', 'auditor'}

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)

    assert set(node.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants
    assert set(node.get_descendants(include_self=True, exact_depth=0).values_list('slug', flat=True)) == {'rules'}
    assert set(node.get_descendants(include_self=True, exact_depth=1).values_list('slug', flat=True)) == {'role'}
    assert set(node.get_descendants(include_self=True, exact_depth=2).values_list('slug', flat=True)) == {'admin',
                                                                                                          'auditor',
                                                                                                          'invisic'}


def test_recover_broken_names(complex_system):
    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    RoleNode.objects.filter(pk=node.pk).update(
        slug_path='/project/rulezz/rol/',
        value_path='/pro/',
        fullname=[]
    )

    assert not RoleNode.objects.filter(slug_path='/project/rules/role/').exists()

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)

    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    assert node.value_path == '/rules/'



