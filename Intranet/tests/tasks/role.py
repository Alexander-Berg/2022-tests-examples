import random
from unittest.mock import patch, ANY

import pytest
from constance.test import override_config
from waffle.testutils import override_switch

from django.conf import settings
from idm.core.constants.action import ACTION
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.core.plugins.dumb import Plugin
from idm.core.plugins.errors import PluginError, PluginFatalError
from idm.core.tasks import RoleAdded
from idm.core.tasks.roles import CheckDeprivingRoles
from idm.tests.utils import disable_tasks, refresh, assert_action_chain, create_user, create_group, raw_make_role

pytestmark = pytest.mark.django_db


def prepare_approved_role(user, system):
    with disable_tasks():
        role = Role.objects.request_role(user, user, system, '', {'role': 'admin'}, None)
    refresh(role)
    assert role.state == ROLE_STATE.APPROVED
    return role


@pytest.mark.parametrize('expected_state,patched', [
    (ROLE_STATE.FAILED, patch.object(Plugin, 'add_role', side_effect=PluginError(500, "error"))),
    (ROLE_STATE.IDM_ERROR, patch.object(RoleAdded, 'add_role', side_effect=AssertionError)),
])
def test_system_plugin_exception(simple_system, arda_users, expected_state, patched):
    """Проверяем статус роли в зависисомти от того почему не получилось approved->granted"""

    role = prepare_approved_role(arda_users['frodo'], simple_system)
    with patched:
        RoleAdded.apply_async(kwargs={'role_id': role.id}, countdown=settings.IDM_PLUGIN_TASK_COUNTDOWN)

    role = refresh(role)
    assert role.state == expected_state


@pytest.mark.parametrize('retry_enabled,state,last_actions,patched', [
    (True, ROLE_STATE.APPROVED, [ACTION.FIRST_ADD_ROLE_PUSH, ACTION.FAIL],
     patch.object(Plugin, 'add_role', side_effect=PluginError(500, "error"))),
    (True, ROLE_STATE.FAILED, [ACTION.FIRST_ADD_ROLE_PUSH, ACTION.FAIL],
     patch.object(Plugin, 'add_role', side_effect=PluginFatalError(500, "error"))),
    (True, ROLE_STATE.APPROVED, [ACTION.IDM_ERROR],
     patch.object(RoleAdded, 'add_role', side_effect=AssertionError)),
    (False, ROLE_STATE.IDM_ERROR, [ACTION.IDM_ERROR],
     patch.object(RoleAdded, 'add_role', side_effect=AssertionError)),
])
def test_system_plugin_exception_retry(simple_system, arda_users, state, last_actions, patched, retry_enabled, settings):
    """проверяем что при включенной опции retry роль остается в состоянии,
    из которого она может попытаться повторно добавиться в системиу"""
    simple_system.retry_failed_roles = retry_enabled
    simple_system.save()

    role = prepare_approved_role(arda_users['frodo'], simple_system)

    with patched:
        RoleAdded.apply_async(kwargs={'role_id': role.id}, countdown=settings.IDM_PLUGIN_TASK_COUNTDOWN)

    role = refresh(role)
    assert role.state == state
    assert_action_chain(role, [ACTION.REQUEST, ACTION.APPLY_WORKFLOW, ACTION.APPROVE] + last_actions)


@pytest.mark.parametrize(
    'errors',
    ([], ['Среди переданных ролей нет ни одной персональной']),
    ids=('no_errors', 'errors_from_staff'),
)
@override_switch('idm.deprive_not_immediately', active=True)
def test_check_depriving_roles(simple_system, settings, errors):
    settings.IDM_DEPRIVING_AFTER_MIN = 0

    staff_group = create_group()
    temporary_members = [create_user(staff_id=random.randint(1, 10*6)) for _ in range(3)]

    permanent_member = create_user()
    temporary_members.append(permanent_member)
    staff_group.add_members(temporary_members)

    group_role = raw_make_role(staff_group, simple_system, {'role': 'manager'}, state=ROLE_STATE.APPROVED)
    group_role.set_state(ROLE_STATE.GRANTED)  # для выдачи персональных ролей под группой
    assert group_role.refs.count() == staff_group.members.count()

    temporary_members.remove(permanent_member)

    for user in temporary_members:
        role = user.roles.select_related('parent', 'system').get(parent__group=staff_group)
        role.deprive_or_decline(None, bypass_checks=True)
        assert role.state == ROLE_STATE.DEPRIVING_VALIDATION

    with patch('idm.core.tasks.roles.DepriveDeprivingRoles.delay') as deprive_task_mock, \
            patch('idm.core.tasks.roles.CheckDeprivingRoles.CHUNK_SIZE', new=1), \
            patch('idm.core.depriving.check_depriving_roles_by_group', return_value=errors) as check_staff_mock, \
            patch('idm.core.depriving.cache_total_roles_count') as cache_total_roles_mock, \
            patch('idm.core.depriving.cache_failed_roles_count') as cache_failed_roles_mock:
        CheckDeprivingRoles.delay()

    cache_total_roles_mock.assert_called_once_with(len(temporary_members))
    check_staff_mock.assert_called_once_with(
        ANY,
        data={'slug': staff_group.slug}
    )
    if errors:
        deprive_task_mock.assert_not_called()
        cache_failed_roles_mock.assert_called_once_with(len(temporary_members))
    else:
        assert deprive_task_mock.call_count == len(temporary_members)
        assert sorted(deprive_task_mock.call_args_list, key=lambda call: call.kwargs['roles_ids']) == [
            ((), {'depriver_id': None, 'roles_ids': [role_id], 'block': True})
            for role_id in group_role.refs.filter(
                state=ROLE_STATE.DEPRIVING_VALIDATION
            ).order_by('id').values_list('id', flat=True)
        ]
        cache_failed_roles_mock.assert_called_once_with(0)


@override_switch('idm.deprive_not_immediately', active=True)
def test_check_depriving_roles__exception_during_staff_check(simple_system, settings):
    settings.IDM_DEPRIVING_AFTER_MIN = 0

    staff_group = create_group()
    temporary_members = [create_user(staff_id=random.randint(1, 10*6)) for _ in range(3)]

    permanent_member = create_user()
    temporary_members.append(permanent_member)
    staff_group.add_members(temporary_members)

    group_role = raw_make_role(staff_group, simple_system, {'role': 'manager'}, state=ROLE_STATE.APPROVED)
    group_role.set_state(ROLE_STATE.GRANTED)  # для выдачи персональных ролей под группой
    assert group_role.refs.count() == staff_group.members.count()

    temporary_members.remove(permanent_member)

    for user in temporary_members:
        role = user.roles.select_related('parent', 'system').get(parent__group=staff_group)
        role.deprive_or_decline(None, bypass_checks=True)
        assert role.state == ROLE_STATE.DEPRIVING_VALIDATION

    with patch('idm.core.tasks.roles.DepriveDeprivingRoles.delay') as deprive_task_mock, \
            patch('idm.core.tasks.roles.CheckDeprivingRoles.CHUNK_SIZE', new=1), \
            patch('idm.core.depriving.check_depriving_roles_by_group', side_effect=Exception) as check_staff_mock, \
            patch('idm.core.depriving.cache_total_roles_count') as cache_total_roles_mock, \
            patch('idm.core.depriving.cache_failed_roles_count') as cache_failed_roles_mock:
        CheckDeprivingRoles.delay()

    cache_total_roles_mock.assert_called_once_with(len(temporary_members))
    check_staff_mock.assert_called_once_with(
        ANY,
        data={'slug': staff_group.slug}
    )
    deprive_task_mock.assert_not_called()
    cache_failed_roles_mock.assert_called_once_with(len(temporary_members))


@override_switch('idm.deprive_not_immediately', active=True)
def test_check_depriving_roles__exceed_total_depriving_roles_threshold(simple_system, settings):
    settings.IDM_DEPRIVING_AFTER_MIN = 0

    staff_group = create_group()
    temporary_members = [create_user(staff_id=random.randint(1, 10*6)) for _ in range(3)]

    permanent_member = create_user()
    temporary_members.append(permanent_member)
    staff_group.add_members(temporary_members)

    group_role = raw_make_role(staff_group, simple_system, {'role': 'manager'}, state=ROLE_STATE.APPROVED)
    group_role.set_state(ROLE_STATE.GRANTED)  # для выдачи персональных ролей под группой
    assert group_role.refs.count() == staff_group.members.count()

    temporary_members.remove(permanent_member)

    for user in temporary_members:
        role = user.roles.select_related('parent', 'system').get(parent__group=staff_group)
        role.deprive_or_decline(None, bypass_checks=True)
        assert role.state == ROLE_STATE.DEPRIVING_VALIDATION

    with patch('idm.core.tasks.roles.DepriveDeprivingRoles.delay') as deprive_task_mock, \
            patch('idm.core.tasks.roles.CheckDeprivingRoles.CHUNK_SIZE', new=1), \
            patch('idm.core.depriving.check_depriving_roles_by_group') as check_staff_mock, \
            patch('idm.core.depriving.cache_total_roles_count') as cache_total_roles_mock, \
            patch('idm.core.depriving.cache_failed_roles_count') as cache_failed_roles_mock, \
            override_config(DEPRIVING_ROLES_WARNING_LIMIT=1):
        CheckDeprivingRoles.delay()

    cache_total_roles_mock.assert_called_once_with(len(temporary_members))
    check_staff_mock.assert_not_called()
    deprive_task_mock.assert_not_called()
    cache_failed_roles_mock.assert_not_called()
