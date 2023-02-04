# coding: utf-8

import pytest
from django.core.management import call_command

from idm.core.constants.action import ACTION

pytestmark = [pytest.mark.django_db]


def test_idm_sync_fix_inconsistencies_push_actions_data(simple_system, django_assert_num_queries):
    simple_system.actions.create(data={'memberships': []}, action=ACTION.GROUP_MEMBERSHIP_INCONSISTENCIES_RESOLVED)
    simple_system.actions.create(action=ACTION.GROUP_MEMBERSHIP_INCONSISTENCIES_RESOLVED)
    action1 = simple_system.actions.create(data=[], action=ACTION.GROUP_MEMBERSHIP_INCONSISTENCIES_PUSH_FAILED)
    action2 = simple_system.actions.create(data=[], action=ACTION.GROUP_MEMBERSHIP_INCONSISTENCIES_RESOLVED)
    simple_system.actions.create(data=[], action=ACTION.GROUP_RESPONSIBLE_ADDED)

    # 1 select, 2 update, 3 for command
    with django_assert_num_queries(5):
        call_command('idm_sync_fix_inconsistencies_push_actions_data')

    action1.refresh_from_db()
    action2.refresh_from_db()
    assert action1.data == action2.data == {'memberships': []}
