# coding: utf-8


import pytest
from django.core.management import call_command
from django.db import connection

from idm.users.models import Group

pytestmark = [pytest.mark.django_db]


def test_recover_deleted_closures(department_structure):
    group = Group.objects.get(slug='department')
    correct_descendants = {
        'department',
        'arda',
        'middle-earth',
        'valinor',
        'associations',
        'lands',
        'the-shire','fellowship-of-the-ring',
    }
    assert set(group.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants

    groups_to_delete = Group.objects.filter(slug__in=['middle-earth', 'associations'])
    for item in groups_to_delete:
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM users_groupclosure WHERE child_id=%s', [item.pk])

    assert set(group.get_descendants(include_self=True).values_list('slug', flat=True)) == {
        'department',
        'arda',
        'valinor',
        'lands',
        'the-shire',
        'fellowship-of-the-ring',
    }

    call_command('idm_oneoff_fix_groups_tree')

    assert set(group.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants
