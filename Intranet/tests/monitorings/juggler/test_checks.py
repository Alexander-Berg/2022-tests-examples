import json
from unittest import mock

import constance
import pytest
from django.core.management import call_command
from django.db import connection
from django.db.models import Q
from django.test import override_settings
from django.utils import timezone
from metrics_framework.models import Metric
from requests.exceptions import ReadTimeout

from idm.core.constants.passport_login import PASSPORT_LOGIN_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Action, System, SystemRolePush, UserPassportLogin
from idm.core.models import Role
from idm.core.models import RoleNode
from idm.monitorings.juggler import JugglerStatus
from idm.monitorings.juggler.checks import active_roles_of_inactive_groups, NO_DATA_MESSAGE, \
    closure_inconsistencies_count, closure_inconsistent_paths, fired_users_limit_exceeded, gap_synchronization, \
    groups_closure_inconsistent_count, hanging_approving_roles, hanging_depriving_roles, not_blocked_ad_users, \
    not_pushed_system_roles, review_roles_threshold_exceeded, logins_to_subscribe, unsynchronized_systems, \
    users_without_depratment_groups, unsubscribed_logins, JugglerCheck, hanging_ref_roles
from idm.monitorings.metric import ActiveRolesOfInactiveGroupsMetric, FiredUsersLimitExceededMetric, \
    ReviewRolesThresholdExceededMetric
from idm.monitorings.tasks import CalculateUnistatMetrics
from idm.tests.commands.test_idm_sync_gaps import get_gaps
from idm.tests.utils import raw_make_role, create_system, assert_contains, random_slug, create_user
from idm.users.models import Group, User
from idm.users.sync.gaps import sync_gaps_for_all_users

pytestmark = pytest.mark.django_db


def test_register_with_tags():
    status, description = JugglerStatus.OK, random_slug()
    tags = [random_slug()]

    @JugglerCheck.register(tags=tags)
    def check():
        return status, description

    event = check.get_event()
    assert event.status == status
    assert event.description == description
    assert set(tags).issubset(event.tags)
    assert event.service == check.callback.__name__


def test_active_roles_if_inactive_groups_ok():
    ActiveRolesOfInactiveGroupsMetric.set({'key1': 0, 'key2': 0})
    assert active_roles_of_inactive_groups() == (JugglerStatus.OK, '')


def test_active_roles_if_inactive_groups_fail():
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской
    assert active_roles_of_inactive_groups() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    # Зажигаем мониторинг, если в кеше лежат данные о ролях
    data = {'key1': 'value1', 'key2': 'value2'}
    ActiveRolesOfInactiveGroupsMetric.set(data)
    assert active_roles_of_inactive_groups() == \
           (JugglerStatus.CRITICAL, f'Active roles of inactive groups: {json.dumps(data)}')


def test_closure_inconsistencies_count(complex_system):
    Metric.objects.create(
        slug='closure_inconsistencies_count',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )
    node = RoleNode.objects.get(slug='rules')
    correct_descendants = {'rules', 'role', 'admin', 'auditor', 'invisic'}
    assert set(node.get_descendants(include_self=True).values_list('slug', flat=True)) == correct_descendants

    assert closure_inconsistencies_count() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    call_command('start_metrics_tasks')
    assert closure_inconsistencies_count() == (JugglerStatus.OK, '')

    to_delete_slugs = ['rules', 'invisic']
    to_delete_nodes = RoleNode.objects.filter(slug__in=to_delete_slugs)
    with connection.cursor() as cursor:
        for item in to_delete_nodes:
            cursor.execute('DELETE FROM upravlyator_rolenodeclosure WHERE child_id=%s', [item.pk])

    assert set(node.get_descendants().values_list('slug', flat=True)) == {'role', 'admin', 'auditor'}

    call_command('start_metrics_tasks')
    assert closure_inconsistencies_count()[0] == JugglerStatus.CRITICAL

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)
    call_command('start_metrics_tasks')
    assert closure_inconsistencies_count() == (JugglerStatus.OK, '')


def test_closure_inconsistent_paths(complex_system):
    Metric.objects.create(
        slug='closure_inconsistent_paths',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )

    assert closure_inconsistent_paths() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    call_command('start_metrics_tasks')
    assert closure_inconsistent_paths() == (JugglerStatus.OK, '')

    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    RoleNode.objects.filter(pk=node.pk).update(
        slug_path='/project/rulezz/rol/',
        value_path='/pro/',
        fullname=[],
    )
    assert not RoleNode.objects.filter(slug_path='/project/rules/role/').exists()

    call_command('start_metrics_tasks')
    assert closure_inconsistent_paths()[0] == JugglerStatus.CRITICAL

    call_command('idm_oneoff_fix_system_tree', '--system', complex_system.slug)
    node = RoleNode.objects.get(slug_path='/project/rules/role/')
    assert node.value_path == '/rules/'
    call_command('start_metrics_tasks')
    assert closure_inconsistent_paths() == (JugglerStatus.OK, '')


def test_fired_users_limit_exceeded_ok():
    FiredUsersLimitExceededMetric.set(0)
    assert fired_users_limit_exceeded() == (JugglerStatus.OK, '')


def test_fired_users_limit_exceeded_fail():
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской
    assert fired_users_limit_exceeded() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    # Зажигаем мониторинг, если в кеше лежат данные о пользователях
    FiredUsersLimitExceededMetric.set(123)
    assert fired_users_limit_exceeded() == (JugglerStatus.CRITICAL, 'Not blocked fired users count: 123')


@mock.patch('idm.users.sync.gaps.get_gaps')
def test_gap_synchronization_ok(mocked_gaps, arda_users):
    mocked_gaps.return_value = get_gaps()

    for _ in range(10):
        sync_gaps_for_all_users()
        assert gap_synchronization() == (JugglerStatus.OK, '')


@mock.patch('idm.users.sync.gaps.get_gaps')
def test_gap_synchronization_fail(mocked_gap, arda_users):
    exception_message = "The server did not send any data in the allotted amount of time."
    mocked_gap.side_effect = ReadTimeout(exception_message)

    for _ in range(5):
        sync_gaps_for_all_users()

    failed_gap_sync_actions = Q(action='gap_synchronization_completed') & ~Q(error__exact='')

    assert Action.objects.count() == 10 and Action.objects.filter(failed_gap_sync_actions).count() == 5
    assert Action.objects.filter(error__contains=exception_message).count() == 5

    check_result = gap_synchronization()
    assert check_result[0] == JugglerStatus.CRITICAL
    assert 'The server did not send any data in the allotted amount of time.' in check_result[1]


def test_groups_closure_inconsistencies(department_structure):
    Metric.objects.create(
        slug='groups_closure_inconsistent_count',
        timedelta=timezone.timedelta(microseconds=1),
        max_timedelta=timezone.timedelta(minutes=15),
        is_exportable=False,
    )

    assert groups_closure_inconsistent_count() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    call_command('start_metrics_tasks')
    assert groups_closure_inconsistent_count() == (JugglerStatus.OK, '')

    group = Group.objects.get(slug='middle-earth')

    with connection.cursor() as cursor:
        cursor.execute('DELETE FROM users_groupclosure WHERE child_id=%s', [group.pk])

    call_command('start_metrics_tasks')
    assert groups_closure_inconsistent_count()[0] == JugglerStatus.CRITICAL

    call_command('idm_oneoff_fix_groups_tree')
    call_command('start_metrics_tasks')
    assert groups_closure_inconsistent_count() == (JugglerStatus.OK, '')


def _change_role_state(role, state, last_action, action_timedelta=None, delete_prev_actions=True):
    if not action_timedelta:
        action_timedelta = timezone.timedelta(seconds=constance.config.IDM_HANGING_DEPRIVING_OLD)
    role.set_raw_state(state)
    if delete_prev_actions:
        role.actions.all().delete()
    action = role.actions.create()
    action.added = timezone.now() - action_timedelta
    action.action = last_action
    action.save()


@pytest.mark.parametrize('status', [JugglerStatus.WARNING, JugglerStatus.CRITICAL])
@override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=1)
def test_hanging_roles(arda_users_with_roles, status: JugglerStatus):
    """Проверяем работу мониторинга зависших ролей"""
    roles = Role.objects.all()

    assert hanging_approving_roles() == (JugglerStatus.OK, '')
    assert hanging_depriving_roles() == (JugglerStatus.OK, '')

    # Не попадет в выборку, т.к. экшн содержит ошибку
    _change_role_state(roles[0], 'depriving', 'error')
    assert hanging_depriving_roles() == (JugglerStatus.OK, '')

    # 3 approved роли попадут в выборку
    if status == JugglerStatus.WARNING:
        action_timedelta = None
    else:
        action_timedelta = timezone.timedelta(minutes=constance.config.HANGING_ROLES_WARN_MINUTES + 1)
    current_timedelta = action_timedelta
    for role in roles[:3]:
        _change_role_state(role, 'approved', 'approve', action_timedelta=current_timedelta)
        # Достаточно одной сильно зависшей роли
        current_timedelta = None
    check_result = hanging_approving_roles()
    assert check_result[0] == status
    assert 'IDM has 3 approved hanging roles since' in check_result[1]

    # 2 depriving роли попадут в выборку
    current_timedelta = action_timedelta
    for role in roles[:2]:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=current_timedelta)
        current_timedelta = None
    check_result = hanging_depriving_roles()
    assert check_result[0] == status
    assert 'IDM has 2 depriving hanging roles since' in check_result[1]

    with override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=3):
        assert hanging_depriving_roles() == (JugglerStatus.OK, '')


@override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=1)
@pytest.mark.parametrize('status', [JugglerStatus.WARNING, JugglerStatus.CRITICAL])
def test_two_systems(arda_users_with_roles, users_with_roles, status: JugglerStatus):
    simple_system_roles = Role.objects.filter(system__slug='simple')[:3]
    test1_system_roles = Role.objects.filter(system__slug='test1')[:2]

    if status == JugglerStatus.WARNING:
        action_timedelta = None
    else:
        action_timedelta = timezone.timedelta(minutes=constance.config.HANGING_ROLES_WARN_MINUTES + 1)
    # Добавляем по 2 экшена для ролей в simple_system, чтобы проверить, что не считаем их по 2 раза
    for role in simple_system_roles:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta)
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta, delete_prev_actions=False)
    assert simple_system_roles[0].actions.count() > 1

    for role in test1_system_roles:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta)

    check_result = hanging_depriving_roles()
    assert check_result[0] == status
    assert 'IDM has 5 depriving hanging roles since' in check_result[1]


def test_not_blocked_ad_users_ok(arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    legolas = arda_users.legolas

    frodo.idm_found_out_dismissal = None
    frodo.ldap_active = True
    frodo.is_active = True
    frodo.save()

    bilbo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=1)
    bilbo.is_active = False
    bilbo.ldap_active = True
    bilbo.save()

    legolas.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    legolas.is_active = False
    legolas.ldap_active = False
    legolas.save()

    User.objects.exclude(pk__in=[bilbo.pk, frodo.pk, legolas.pk]).delete()

    assert not_blocked_ad_users() == (JugglerStatus.OK, '')


def test_not_blocked_ad_users_fail(arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    legolas = arda_users.legolas

    frodo.idm_found_out_dismissal = None
    frodo.ldap_active = True
    frodo.is_active = True
    frodo.save()

    bilbo.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=1)
    bilbo.is_active = False
    bilbo.ldap_active = None
    bilbo.save()

    legolas.idm_found_out_dismissal = timezone.now() - timezone.timedelta(hours=3)
    legolas.is_active = False
    legolas.ldap_active = True
    legolas.save()

    assert not_blocked_ad_users() == \
           (JugglerStatus.CRITICAL, 'IDM has 1 fired, but active in AD users: %s' % legolas.username)


def test_not_pushed_system_roles_ok(client, arda_users):
    assert not_pushed_system_roles() == (JugglerStatus.OK, '')


def test_not_pushed_system_roles_fail(client, arda_users):
    new_system = System.objects.create(slug='new', name='new', name_en='new')
    old_system = System.objects.create(slug='old', name='old', name_en='old')
    old_system.added = timezone.now() - timezone.timedelta(hours=1)
    old_system.save()

    SystemRolePush.objects.create(system=old_system)
    SystemRolePush.objects.create(system=old_system)
    SystemRolePush.objects.create(system=new_system)

    assert not_pushed_system_roles() == \
           (JugglerStatus.CRITICAL, 'IDM has 1 systems with not pushed responsibles or team members: old')


def test_review_roles_threshold_exceeded_ok(client):
    ReviewRolesThresholdExceededMetric.set({})
    assert review_roles_threshold_exceeded() == (JugglerStatus.OK, '')


def test_reviews_roles_threshold_exceeded_fail(client):
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской на пересмотр ролей
    assert review_roles_threshold_exceeded() == (JugglerStatus.WARNING, NO_DATA_MESSAGE)

    # Зажигаем мониторинг, если в кеше лежат данные
    ReviewRolesThresholdExceededMetric.set({'self': 5})
    assert review_roles_threshold_exceeded() == \
           (JugglerStatus.CRITICAL, 'Number of roles to review on last run: {"self": 5}')


@pytest.mark.parametrize('threshold', [None, 1, 3])
def test_logins_to_subscribe(arda_users, simple_system, threshold):
    """Проверяем работу мониторинга зависших ролей"""
    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-frodo'},
        system_specific={'passport-login': 'yndx-frodo'},
    )
    raw_make_role(
        arda_users.legolas, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-legolas'},
        system_specific={'passport-login': 'yndx-legolas'},
    )

    CalculateUnistatMetrics.delay()

    with override_settings(IDM_SID67_THRESHOLD=threshold or 2500):
        check_result = logins_to_subscribe()
    if threshold in (None, 3):
        assert check_result == (JugglerStatus.OK, '')
    else:
        assert check_result == (JugglerStatus.CRITICAL, 'IDM has 2 passport logins to subscribe')


@pytest.mark.parametrize('subscribed', [True, False])
@mock.patch('idm.sync.passport.set_strongpwd', return_value=True)
def test_subscribed_logins_actually_not_subscribed(fake_subscriber, arda_users, generic_system,
                                                   simple_system, monkeypatch, subscribed):
    class PassportMock(object):
        def userinfo(self, *args, **kwargs):
            return {
                'uid': '123456',
                'fields': {'suid': '1' if subscribed else None},
            }

    monkeypatch.setattr('idm.sync.passport.exists', lambda *args, **kwargs: True)
    monkeypatch.setattr('blackbox.Blackbox.userinfo', PassportMock().userinfo)

    frodo = arda_users['frodo']

    Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'manager'}, {'passport-login': 'yndx-frodo'}
    )
    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-frodo'},
        system_specific={'passport-login': 'yndx-frodo'},
    )

    login = UserPassportLogin.objects.get()

    login.subscribe()
    login.refresh_from_db()

    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    call_command('idm_check_passport_logins_subscribed')
    check_result = unsubscribed_logins()

    if subscribed:

        assert check_result == (JugglerStatus.OK, '')
    else:
        assert check_result == (
            JugglerStatus.CRITICAL,
            'IDM has 1 passport logins in subscribed state, but actually not subscribed: yndx-frodo',
        )
        assert fake_subscriber.call_count == 1
        call_command('idm_check_passport_logins_subscribed', fix_all=True)
        assert fake_subscriber.call_count == 2
        call_command('idm_check_passport_logins_subscribed', fix='yndx-frodo,yndx-smth')
        assert fake_subscriber.call_count == 3
        call_command('idm_check_passport_logins_subscribed', fix='frodo,smth')
        assert fake_subscriber.call_count == 3


def _refresh_timestamps(system, hours_offset=0):
    now = timezone.now()
    for field in system.metainfo._meta.fields:
        if field.name.startswith('last_'):
            setattr(system.metainfo, field.name, now - timezone.timedelta(hours=hours_offset))
    system.metainfo.save()


def test_unsynchronized_systems():
    """Проверяем работу мониторинга давно не выполнявшихся посистемных тасок"""

    system_1 = create_system('system_1', sync_role_tree=False)
    system_2 = create_system('system_2', sync_role_tree=False)

    assert unsynchronized_systems() == (JugglerStatus.OK, '')  # таймстемпов нет, но системы новые

    System.objects.update(added=timezone.now() - timezone.timedelta(days=10))
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL  # если для старой системы таймстемпа не было – это тоже плохо
    assert_contains((
        '"check_inconsistencies" task has not been recently run for systems system_1,system_2',
        '"deprive_nodes" task has not been recently run for systems system_1,system_2',
        '"recalc_pipeline" task has not been recently run for systems system_1,system_2',
        '"report_inconsistencies" task has not been recently run for systems system_1,system_2',
        '"resolve_inconsistencies" task has not been recently run for systems system_1,system_2'
    ), check_result[1])

    _refresh_timestamps(system_1)
    _refresh_timestamps(system_2)

    assert unsynchronized_systems() == (JugglerStatus.OK, '')

    system_1.metainfo.last_sync_nodes_finish = timezone.now() - timezone.timedelta(days=4)
    system_1.metainfo.save()
    assert unsynchronized_systems() == (JugglerStatus.OK, '')  # для системы без автоапдейта мониторинг не загорается

    system_1.root_role_node.is_auto_updated = True
    system_1.root_role_node.save()
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL
    assert '"sync_nodes" task has not been recently run for systems system_1' in check_result[1]
    RoleNode.objects.all().delete()

    system_1.metainfo.last_activate_memberships_finish = timezone.now() - timezone.timedelta(hours=5)
    system_1.metainfo.last_deprive_memberships_finish = timezone.now() - timezone.timedelta(days=10)
    system_1.metainfo.save()
    # мониторинг на членства не загорается для систем, не поддерживающих их
    assert unsynchronized_systems() == (JugglerStatus.OK, '')

    system_1.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS
    system_1.save(update_fields=['group_policy'])
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL
    assert_contains((
        '"activate_memberships" task has not been recently run for systems system_1',
        '"deprive_memberships" task has not been recently run for systems system_1',
    ), check_result[1])

    system_1.metainfo.monitor_deprive_memberships = False
    system_1.metainfo.save()
    # monitor_{task}=False позволяет выкинуть какую-то таску какой-то системы из мониторинга
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL
    assert '"activate_memberships" task has not been recently run for systems system_1' in check_result[1]

    system_2.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS
    system_2.is_broken = True
    system_2.save(update_fields=['group_policy', 'is_broken'])
    _refresh_timestamps(system_2, 5*24)  # всё должно протухнуть
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL
    # система сломана, поэтому мониторинги на загораются
    assert '"activate_memberships" task has not been recently run for systems system_1' in check_result[1]

    system_2.is_broken = False
    system_2.save(update_fields=['is_broken'])
    check_result = unsynchronized_systems()
    assert check_result[0] == JugglerStatus.CRITICAL
    assert_contains((
        '"activate_memberships" task has not been recently run for systems system_1,system_2',
        '"check_inconsistencies" task has not been recently run for systems system_2',
        '"check_memberships" task has not been recently run for systems system_2',
        '"deprive_memberships" task has not been recently run for systems system_2',
        '"deprive_nodes" task has not been recently run for systems system_2',
        '"recalc_pipeline" task has not been recently run for systems system_2',
        '"report_inconsistencies" task has not been recently run for systems system_2',
        '"resolve_inconsistencies" task has not been recently run for systems system_2',
        '"resolve_memberships" task has not been recently run for systems system_2',
        '"update_memberships" task has not been recently run for systems system_2'
    ), check_result[1])


def test_users_without_department_group_ok(arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    User.objects.exclude(pk__in=[bilbo.pk, frodo.pk]).delete()

    assert frodo.memberships.filter(state='active', group__type='department').exists()
    bilbo.memberships.filter(group__type='department').update(
        state='inactive', date_leaved=timezone.now() - timezone.timedelta(hours=1)
    )

    assert users_without_depratment_groups() == (JugglerStatus.OK, '')


def test_users_without_department_group_fail(arda_users):
    frodo = arda_users.frodo
    User.objects.exclude(pk=frodo.pk).delete()
    frodo.date_joined = timezone.now() - timezone.timedelta(hours=4)
    frodo.save()
    frodo.memberships.filter(group__type='department').update(
        state='inactive', date_leaved=timezone.now() - timezone.timedelta(hours=4)
    )
    assert users_without_depratment_groups() == \
           (JugglerStatus.CRITICAL, 'IDM has 1 users without department group: %s' % frodo.username)


def test_hanging_ref_roles():
    ref_system = create_system()
    ref_node = ref_system.nodes.last()
    ref_roles_definition = [
        {},
        {'system': random_slug(), 'role_data': {}},
        {'system': ref_system.slug, 'role_data': {'role': random_slug()}},
        {'system': ref_system.slug, 'role_data': ref_node.data},
    ]
    system = create_system()
    user = create_user()
    parent_role = raw_make_role(
        subject=user,
        system=system,
        data=system.nodes.last().data,
        ref_roles=ref_roles_definition
    )
    assert parent_role.refs.count() == 0
    assert hanging_ref_roles() == (
        JugglerStatus.CRITICAL,
        'Ref roles problems: ' + json.dumps({
            'corrupted_ref_roles_value': 1,
            'refs_on_inactive_systems': 1,
            'refs_on_unknown_nodes': 1,
            'non_requested_refs': 1
        }),
    )

    parent_role.ref_roles = [ref_roles_definition.pop()]
    parent_role.save(update_fields=('ref_roles',))
    parent_role.request_refs()
    assert parent_role.refs.count() == 1
    assert hanging_ref_roles() == (JugglerStatus.OK, '')
