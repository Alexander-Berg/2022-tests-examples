# coding: utf-8


import pytest
import mock
from math import ceil
from collections import Counter

from django.core.management import call_command

from idm.core.models import GroupMembershipSystemRelation, UserPassportLogin
from idm.users.models import User
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.inconsistencies.models import GroupMembershipInconsistency
from idm.tests.utils import mock_group_memberships
from idm.users.models import GroupMembership

pytestmark = [pytest.mark.django_db, pytest.mark.robot]


def update_system_memberships(system, membership_tuples):
    # Эмуляция ожидаемого поведения системы при пуше членств.
    # Получаем текущие членства системы, возварщаем обновленные
    memberships = [{'login':  user, 'group': group, 'passport_login': passport_login}
                   for user, group, passport_login in membership_tuples]
    with mock.patch.object(system.plugin.__class__, 'add_group_membership') as add_group_membership:
        with mock.patch.object(system.plugin.__class__, 'remove_group_membership') as remove_group_membership:
            with mock_group_memberships(system, memberships):
                call_command('idm_process_groupmembership_inconsistencies', system=system)
    add_args = [element for call in add_group_membership.call_args_list for element in call[0][0]]
    remove_args = [element for call in remove_group_membership.call_args_list for element in call[0][0]]
    add_elements = {(element['login'], element['group']): element['passport_login'] for element in add_args}
    remove_set = {(element['login'], element['group']) for element in remove_args}
    initial_elements = {(element[0], element[1]): element[2] for element in membership_tuples}
    initial_elements.update(add_elements)
    for element in remove_set:
        del initial_elements[element]
    return [(login, group_id, passport_login) for (login, group_id), passport_login in list(initial_elements.items())]


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_check_our_side_inconsistencies(simple_system, department_structure, group_policy):
    # Проверим создание неконсистентностей для всех системочленств в статусе activated и hold
    # В статусе depriving не создаем неконсистентностей
    simple_system.group_policy = group_policy
    simple_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.filter(membership__user__username='frodo').update(state='activated')
    GroupMembershipSystemRelation.objects.filter(membership__user__username='sam').update(state='depriving')
    GroupMembershipSystemRelation.objects.filter(membership__user__username='bilbo').update(state='hold')
    with mock_group_memberships(simple_system, []):
        call_command('idm_process_groupmembership_inconsistencies', check_only=True)
    assert 'started_memberships_sync_with_system' in simple_system.actions.values_list('action', flat=True)
    assert 'finished_memberships_sync_with_system' in simple_system.actions.values_list('action', flat=True)
    assert GroupMembershipInconsistency.objects.count() == GroupMembershipSystemRelation.objects.filter(
        membership__user__username__in=['frodo', 'bilbo']
    ).count()
    assert set(GroupMembershipInconsistency.objects.values_list('type', flat=True)) == {'we_have_system_dont'}

    # Неразрешенные неконсистентности уходят в obsolete. Система должна знать о depriving системочленствах.
    # Проверим, что при этом не создаются неконсистентности на стороне системы
    frodo_bilbo_memberships = (
        GroupMembershipSystemRelation.objects.filter(membership__user__username__in=['frodo', 'bilbo'])
    )
    frodo_bilbo_memberships.update(state='deprived')

    sam_groups = (
        GroupMembershipSystemRelation.objects
        .filter(membership__user__username='sam')
        .values_list('membership__group__external_id', flat=True)
    )
    memberships = [{'login': 'sam', 'group': group_id, 'passport_login': ''} for group_id in sam_groups]
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        [sm.pop('passport_login') for sm in memberships]

    with mock_group_memberships(simple_system, memberships):
        call_command('idm_process_groupmembership_inconsistencies', check_only=True)

    assert GroupMembershipInconsistency.objects.count() == frodo_bilbo_memberships.count()
    assert set(GroupMembershipInconsistency.objects.values_list('type', flat=True)) == {'we_have_system_dont'}
    assert set(GroupMembershipInconsistency.objects.values_list('state', flat=True)) == {'obsolete'}


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_check_system_side_inconsistencies(simple_system, department_structure, group_policy):
    # Для призвольного ответа системы вне зависимости от существования пользователя группы
    # создаются неконсистентности, в поля username и group_id прокидываются значения из ответа системы
    simple_system.group_policy = group_policy
    simple_system.save()
    memberships = [{'login': 'lenin', 'group': 666, 'passport_login': ''},
                   {'login': 'celsius', 'group': 273, 'passport_login': ''}]
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        [sm.pop('passport_login') for sm in memberships]
    with mock_group_memberships(simple_system, memberships):
        call_command('idm_process_groupmembership_inconsistencies', check_only=True)

    assert GroupMembershipInconsistency.objects.count() == 2
    assert set(GroupMembershipInconsistency.objects.values_list('state', 'type')) == {
        ('active', 'system_has_we_dont')
    }


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_check_system_side_inconsistencies_deprived(simple_system, department_structure, group_policy):
    # Создастся неконсистентность если у нас есть системочленство в статусе deprived
    simple_system.group_policy = group_policy
    simple_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='deprived')
    frodo_groups = GroupMembershipSystemRelation.objects.filter(
        membership__user__username='frodo').values_list('membership__group__external_id', flat=True)
    memberships = [{'login': 'frodo', 'group': group_id, 'passport_login': ''} for group_id in frodo_groups]
    if simple_system.group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        [sm.pop('passport_login') for sm in memberships]
    with mock_group_memberships(simple_system, memberships):
        call_command('idm_process_groupmembership_inconsistencies', check_only=True)
    assert GroupMembershipInconsistency.objects.count() == GroupMembershipSystemRelation.objects.filter(
        membership__user__username='frodo',
    ).count()
    assert set(GroupMembershipInconsistency.objects.values_list('state', 'type')) == {
        ('active', 'system_has_we_dont')
    }


@pytest.mark.parametrize('batch_size', [1, 3, 5, 100])
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_resolve_inconsistencies(simple_system, department_structure, batch_size, group_policy):
    # Создадим неконсистентности разных типов, посмотрим, как они пушатся и переходят в статус resolved.
    simple_system.group_policy = group_policy
    simple_system.push_batch_size = batch_size
    simple_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='activated')
    membership_tuples = GroupMembershipSystemRelation.objects.values_list(
        'membership__user__username',
        'membership__group__external_id',
    )
    memberships = [{'login':  user, 'group': group, 'passport_login': ''} for user, group in membership_tuples]
    wrong_memberships = [{'login': 'ne' + user, 'group': group, 'passport_login': ''} for user, group in membership_tuples]

    with mock.patch.object(simple_system.plugin.__class__, 'add_group_membership') as add_group_membership:
        with mock.patch.object(simple_system.plugin.__class__, 'remove_group_membership') as remove_group_membership:
            with mock_group_memberships(simple_system, wrong_memberships):
                call_command('idm_process_groupmembership_inconsistencies')
    n_batches = ceil(GroupMembershipSystemRelation.objects.count() / float(batch_size))
    assert add_group_membership.call_count == n_batches
    assert remove_group_membership.call_count == n_batches

    action_counter = Counter(simple_system.actions.values_list('action', flat=True))
    assert action_counter['group_membership_inconsistencies_resolved'] == 2 * n_batches
    assert action_counter['started_memberships_sync_with_system'] == 1
    assert action_counter['finished_memberships_sync_with_system'] == 1

    add_args = [element for call in add_group_membership.call_args_list for element in call[0][0]]
    remove_args = [element for call in remove_group_membership.call_args_list for element in call[0][0]]
    assert set(map(str, add_args)) == set(map(str, memberships))
    assert set(map(str, remove_args)) == set(map(str, wrong_memberships))
    assert set(GroupMembershipInconsistency.objects.values_list('state', flat=True)) == {'resolved'}


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_check_multiple_systems(simple_system, complex_system, department_structure, group_policy):
    # Возмем все системы. Сделаем им одинаковые членства на нашей стороне, а ответы у них разные
    # Проверим, что неконсистентности корректно создаются и не конфликтуют между системами.
    simple_system.group_policy = group_policy
    simple_system.save()
    complex_system.group_policy = group_policy
    complex_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, complex_system)
    GroupMembershipSystemRelation.objects.update(state='activated')

    # В выдаче системы будут дубликаты, мы должны быть к этому устойчивы
    membership_tuples = GroupMembershipSystemRelation.objects.values_list(
        'membership__user__username',
        'membership__group__external_id',
    )
    frodo_memberships = [{'login':  user, 'group': group, 'passport_login': ''}
                         for user, group in membership_tuples.filter(
        membership__user__username='frodo'
    )]
    shire_memberships = [{'login':  user, 'group': group, 'passport_login': ''}
                         for user, group in membership_tuples.filter(
        membership__group__external_id=104
    )]
    wrong_memberships = [
        {'login': 'batman', 'group': 0, 'passport_login': ''},
        {'login': 'grunt', 'group': 1, 'passport_login': ''}
    ]
    with mock_group_memberships(simple_system, frodo_memberships + wrong_memberships):
        with mock_group_memberships(complex_system, shire_memberships + wrong_memberships):
            call_command('idm_process_groupmembership_inconsistencies', check_only=True)
    expecting_our_side_simple = set(membership_tuples.exclude(membership__user__username='frodo'))
    expecting_our_side_complex = set(membership_tuples.exclude(membership__group__external_id=104))
    expecting_system_side = {('batman', 0), ('grunt', 1)}
    assert set(GroupMembershipInconsistency.objects.filter(
        system=simple_system, state='active', type='we_have_system_dont'
    ).values_list('username', 'group_id')) == expecting_our_side_simple
    assert set(GroupMembershipInconsistency.objects.filter(
        system=complex_system, state='active', type='we_have_system_dont'
    ).values_list('username', 'group_id')) == expecting_our_side_complex
    assert set(GroupMembershipInconsistency.objects.filter(
        system=simple_system, state='active', type='system_has_we_dont'
    ).values_list('username', 'group_id')) == expecting_system_side
    assert set(GroupMembershipInconsistency.objects.filter(
        system=complex_system, state='active', type='system_has_we_dont'
    ).values_list('username', 'group_id')) == expecting_system_side


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_passport_login_change(simple_system, department_structure, group_policy):
    def assert_actual_passport_logins(membership_tuples):
        if not group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
            return
        assert len(membership_tuples) == GroupMembershipSystemRelation.objects.filter(state='activated').count()
        for user, group, passport_login in membership_tuples:
            membership = GroupMembershipSystemRelation.objects.select_related('membership__passport_login').get(
                membership__user__username=user,
                membership__group__external_id=group,
            )
            if passport_login:
                assert membership.membership.passport_login.login == passport_login
            else:
                assert membership.membership.passport_login is None

    # При изменении паспортного логина должны создаться неконсистентности на нашей стороне и на стороне системы
    simple_system.group_policy = group_policy
    simple_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='activated')

    # Добавим системочленства в систему при помощи таски по синхронизации
    membership_tuples = update_system_memberships(simple_system, [])
    assert_actual_passport_logins(membership_tuples)

    # При разрешении мы пропушиваем обновление членства с новым логином.
    for user in User.objects.iterator():
        login = UserPassportLogin(
            user=user,
            login='yndx-' + user.username,
        )
        login.save()
    for membership in shire.memberships.iterator():
        membership.passport_login = UserPassportLogin.objects.get(user_id=membership.user_id)
        membership.save()
    membership_tuples = update_system_memberships(simple_system, membership_tuples)
    assert_actual_passport_logins(membership_tuples)
    sync_key = simple_system.actions.filter(action='started_memberships_sync_with_system').order_by('pk').last()
    expected_count = (
        shire.memberships.count()
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
        else 0
    )
    assert GroupMembershipInconsistency.objects.filter(
        sync_key=sync_key, state='resolved').count() == expected_count
    assert GroupMembershipInconsistency.objects.filter(
        sync_key=sync_key, state='paired').count() == expected_count

    for membership in shire.memberships.iterator():
        membership.passport_login = None
        membership.save()

    for membership in fellowship.memberships.iterator():
        membership.passport_login = UserPassportLogin.objects.get(user_id=membership.user_id)
        membership.save()
    GroupMembershipSystemRelation.objects.filter(membership__user__username='sam').update(state='deprived')

    membership_tuples = update_system_memberships(simple_system, membership_tuples)
    assert_actual_passport_logins(membership_tuples)
