# coding: utf-8


import pytest
from django.core.management import call_command
from idm.tests.utils import raw_make_role, refresh

pytestmark = pytest.mark.django_db


def test_update_stats(simple_system, pt1_system, arda_users):

    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'manager'})

    assert simple_system.active_roles_count == -1
    assert simple_system.active_nodes_count == -1
    assert pt1_system.active_roles_count == -1
    assert pt1_system.active_nodes_count == -1

    # второй запуск ничего не меняет
    for i in range(2):
        call_command('idm_update_stats')
        simple_system = refresh(simple_system)
        pt1_system = refresh(pt1_system)

        assert simple_system.active_nodes_count == 6
        assert simple_system.active_roles_count == 2
        assert pt1_system.active_nodes_count == 18
        assert pt1_system.active_roles_count == 0
