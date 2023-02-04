# coding: utf-8

from math import ceil
import mock
import pytest
from waffle.models import Switch

import constance

from django.core.management import call_command
from django.db.models import Q
from django.test.utils import override_settings

from idm.core.constants.action import ACTION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import GroupMembershipSystemRelation, Action, UserPassportLogin, Role
from idm.users.models import GroupMembership
from idm.tests.utils import raw_make_role


pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('batch_size', [1, 3, 5, 100])
@pytest.mark.parametrize('push_method', ['add_group_membership', 'remove_group_membership'])
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_group_membership_system_relation_activate(simple_system, department_structure, batch_size,
                                                   push_method, group_policy):
    """
    Системочленства в статусе activating пушатся в систему и переходят в activated
    Системочленства в статусе depriving пушатся в систему и переходят в статус deprived
    """
    params = {
        'add_group_membership': {
            'success_state': 'activated',
            'success_action': 'sysmembership_activate',
        },
        'remove_group_membership': {
            'success_state': 'deprived',
            'success_action': 'sysmembership_deprive'
        }
    }[push_method]
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.push_batch_size = batch_size
    simple_system.save()

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor

    for membership in shire.memberships.select_related('user').iterator():
        login = UserPassportLogin(
            user_id=membership.user_id,
            login='yndx-' + membership.user.username,
        )
        login.save()
        membership.passport_login = login
        membership.save()

    if push_method == 'add_group_membership':
        raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
        raw_make_role(valinor, simple_system, {'role': 'manager'}, None, state='need_request')
        raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')
        call_command('idm_sync_groupmembership_system_relations')
    elif push_method == 'remove_group_membership':
        group_ids = sorted([fellowship.id, shire.id, valinor.id])
        membership_ids = (
            GroupMembership.objects.filter(
                group_id__in=group_ids, state__in=GROUPMEMBERSHIP_STATE.ACTIVE_STATES
            ).values_list('id', flat=True)
        )
        GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(
            membership_ids, simple_system
        )
        GroupMembershipSystemRelation.objects.update(state='depriving')

    with mock.patch('idm.tests.base.SimplePlugin.' + push_method, return_value={'code': 0}) as push_group_membership:
        constance.config.GROUPMEMBERSHIP_SYSTEM_DEPRIVING_THRESHOLD = 1
        call_command('idm_push_groupmembership_system_relations')

        if push_method == 'remove_group_membership':
            assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {'depriving'}
            call_command('idm_push_groupmembership_system_relations', '--force')

    assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {params['success_state']}
    groupmembership_actions = Action.objects.exclude(sysmembership=None)
    assert groupmembership_actions.count() == GroupMembershipSystemRelation.objects.count()
    assert set(groupmembership_actions.values_list(
        'action', flat=True)) == {params['success_action']}
    n_batches = ceil(GroupMembershipSystemRelation.objects.count() / float(batch_size))
    assert push_group_membership.call_count == n_batches
    add_args = [element for call in push_group_membership.call_args_list for element in call[0][0]]
    membership_tuples = GroupMembershipSystemRelation.objects.values_list(
        'membership__user__username',
        'membership__group__external_id',
        'membership__passport_login__login'
    )
    memberships = [{
        'login': user,
        'group': group,
        'passport_login': '' if passport_login is None else passport_login
    } for user, group, passport_login in membership_tuples]

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        [sm.pop('passport_login') for sm in memberships]

    assert set(map(str, add_args)) == set(map(str, memberships))


@pytest.mark.parametrize('batch_size', [1, 3, 5, 100])
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_one_object_not_fails_batch(simple_system, department_structure, batch_size, group_policy):
    """
    Если плагин возвращает ошибку, смотрим в multi-status и оставляем сфейленные членства в activating
    """
    def mocked_plugin_method(self, data, **kwargs):
        code = 0
        failed_memberships = []
        return_data = {}
        for membership in data:

            if membership['login'] == 'aragorn':
                error = 'User does not exist: aragorn'
                membership.update(error=error)
                failed_memberships.append(membership)

            if membership['group'] == shire.external_id:
                error = 'Group does not exist: {}'.format(shire.external_id)
                membership.update(error=error)
                failed_memberships.append(membership)

        if failed_memberships:
            code = 207
            return_data.update(multi_status=failed_memberships)

        return_data.update(code=code)

        return return_data

    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.push_batch_size = batch_size
    simple_system.save()

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor

    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
    raw_make_role(valinor, simple_system, {'role': 'manager'}, None, state='need_request')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')

    call_command('idm_sync_groupmembership_system_relations')
    with mock.patch.object(simple_system.plugin.__class__, 'add_group_membership', mocked_plugin_method):
        call_command('idm_push_groupmembership_system_relations')

    expected_activated_count = GroupMembershipSystemRelation.objects.count() - shire.members.count()
    expected_activating_count = GroupMembershipSystemRelation.objects.count() - expected_activated_count
    assert GroupMembershipSystemRelation.objects.activating().count() == expected_activating_count
    assert GroupMembershipSystemRelation.objects.activated().count() == expected_activated_count

    groupmembership_actions = Action.objects.exclude(sysmembership=None)
    assert groupmembership_actions.filter(action='sysmembership_activate').count() == expected_activated_count

    actication_failed_actions = groupmembership_actions.filter(action='sysmembership_activation_failed')
    assert actication_failed_actions.count() == expected_activating_count
    assert set(actication_failed_actions.values_list(
        'error', flat=True
    )) == {'Group does not exist: {}'.format(shire.external_id)}

    Role.objects.all().delete()
    GroupMembershipSystemRelation.objects.all().delete()

    raw_make_role(fellowship, simple_system, {'role': 'admin'}, None, state='granted')

    call_command('idm_sync_groupmembership_system_relations')
    with mock.patch.object(simple_system.plugin.__class__, 'add_group_membership', mocked_plugin_method):
        call_command('idm_push_groupmembership_system_relations')

    expected_activated_count = GroupMembershipSystemRelation.objects.count() - 1  # aragorn
    expected_activating_count = GroupMembershipSystemRelation.objects.count() - expected_activated_count
    assert GroupMembershipSystemRelation.objects.activating().count() == expected_activating_count
    assert GroupMembershipSystemRelation.objects.activated().count() == expected_activated_count

    groupmembership_actions = Action.objects.exclude(sysmembership=None)
    assert groupmembership_actions.filter(action='sysmembership_activate').count() == expected_activated_count

    actication_failed_actions = groupmembership_actions.filter(action='sysmembership_activation_failed')
    assert actication_failed_actions.count() == expected_activating_count
    assert actication_failed_actions.get().error == 'User does not exist: aragorn'


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
@pytest.mark.parametrize('batch_size', [1, 3, 5, 100])
def test_multiple_processes(simple_system, department_structure, batch_size, group_policy):
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.push_batch_size = batch_size
    simple_system.save()

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = (
        GroupMembership.objects
        .filter(group_id__in=group_ids, state__in=GROUPMEMBERSHIP_STATE.ACTIVE_STATES)
        .values_list('id', flat=True)
    )
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    q_fellowship = Q(membership__group__slug='fellowship-of-the-ring')
    q_shire = Q(membership__group__slug='the-shire')
    q_valinor = Q(membership__group__slug='valinor')
    q_frodo = Q(membership__user__username='frodo')
    GroupMembershipSystemRelation.objects.filter(q_fellowship).update(state='activating')
    GroupMembershipSystemRelation.objects.filter(q_shire).update(state='activated')
    GroupMembershipSystemRelation.objects.filter(q_valinor).update(state='deprived')
    GroupMembershipSystemRelation.objects.filter(q_frodo).update(state='depriving')
    call_command('idm_push_groupmembership_system_relations')
    assert set(GroupMembershipSystemRelation.objects.exclude(q_frodo).filter(q_fellowship | q_shire).values_list(
        'state', flat=True)) == {'activated'}
    assert set(GroupMembershipSystemRelation.objects.filter(q_valinor | q_frodo).values_list(
        'state', flat=True)) == {'deprived'}
    assert set(GroupMembershipSystemRelation.objects.values_list(
        'state', flat=True)) == {'activated', 'deprived'}

    for sysmembership in GroupMembershipSystemRelation.objects.filter((q_shire | q_valinor) & ~q_frodo).iterator():
        assert not Action.objects.filter(sysmembership=sysmembership).exists()

    for sysmembership in GroupMembershipSystemRelation.objects.filter(q_fellowship | q_frodo).iterator():
        assert Action.objects.filter(sysmembership=sysmembership).exists()


@pytest.mark.single
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_update_group_membership_system_relations(simple_system, department_structure, group_policy):
    simple_system.group_policy = group_policy
    simple_system.save()

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(
        group_id__in=group_ids,
        state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='activated', need_update=True)
    membership_tuples = GroupMembershipSystemRelation.objects.values_list(
        'membership__user__username',
        'membership__group__external_id',
    )
    memberships = [{'login': user, 'group': group, 'passport_login': ''} for user, group in membership_tuples]

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        [sm.pop('passport_login') for sm in memberships]

    with mock.patch(
            'idm.tests.base.SimplePlugin.add_group_membership', return_value={'code': 0},
    ) as add_group_membership:
        call_command('idm_push_groupmembership_system_relations')
    add_args = [element for call in add_group_membership.call_args_list for element in call[0][0]]
    assert set(map(str, add_args)) == set(map(str, memberships))
    groupmembership_actions = Action.objects.exclude(sysmembership=None)
    assert groupmembership_actions.count() == GroupMembershipSystemRelation.objects.count()
    assert set(groupmembership_actions.values_list(
        'action', flat=True)) == {ACTION.SYSMEMBERSHIP_LOGIN_UPDATED}
