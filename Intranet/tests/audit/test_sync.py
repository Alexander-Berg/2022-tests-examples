# coding: utf-8


import mock
import pytest
from datetime import timedelta
from freezegun import freeze_time
import pytz

from django.core.management import call_command
from django.utils import timezone

from idm.core.constants.action import ACTION
from idm.core.models import Role, Action, RoleNode
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (mock_all_roles, refresh, CountIncreasedContext, raw_make_role, make_role,
                             assert_action_chain, sync_roles, mock_tree, assert_contains)


pytestmark = [pytest.mark.django_db, pytest.mark.robot]


def test_synchronization_forcibly_resolves_inconsistencies(simple_system, arda_users, responsible_gandalf):
    """Тестируем, что при принудительной синхронизации с системой роли не задваиваются,
    а также что все неразрешённые неконсистентности принудительно разрешаются"""

    frodo = arda_users.frodo
    gandalf = responsible_gandalf
    all_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{
                 'role': 'manager'
            }, {
                'passport-login': 'minor1'
            }]
        ]
    }]
    Action.objects.all().delete()

    with mock_all_roles(simple_system, all_roles):
        sync_roles(simple_system, arda_users.gandalf)

    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.user_id == frodo.id
    assert role.state == 'granted'

    assert Action.objects.count() == 8
    actions = list(Action.objects.order_by('added').values_list('action', flat=True))
    expected_actions = ['started_comparison_with_system', 'compared_with_system',  # check
                        'started_sync_with_system',  # resolve system
                        'import', 'approve', 'grant', 'resolve_inconsistency',  # role
                        'synced_with_system']  # end resolve system
    assert actions == expected_actions

    resolve_action = Action.objects.get(action='resolve_inconsistency')
    assert 'is_forcibly_resolved' in resolve_action.data

    assert gandalf.requested.count() == 4
    requester_actions = gandalf.requested.order_by('pk')
    for action in requester_actions:
        assert action.system_id == simple_system.id
    start_check, end_check, start_sync, end_sync = requester_actions
    assert start_check.action == 'started_comparison_with_system'
    assert end_check.action == 'compared_with_system'
    assert start_sync.action == 'started_sync_with_system'
    assert end_sync.action == 'synced_with_system'
    expected_end_sync_data = {
        'report_existed': 0,
        'report_created': 1,
        'report_errors': 0,
        'report_created_count': 1,
        'report_deprived_count': 0,
        'status': 0,
    }
    assert end_sync.data == expected_end_sync_data


def test_failed_synchronization(simple_system, arda_users, responsible_gandalf):
    assert Inconsistency.objects.count() == 0
    with CountIncreasedContext((Action, 2), (Inconsistency, 0)) as new_data:
        with mock_all_roles(simple_system, side_effect=ValueError):
            sync_roles(simple_system, responsible_gandalf)
    start_check, end_check = new_data.get_new_objects(Action).order_by('pk')
    assert start_check.action == 'started_comparison_with_system'
    assert start_check.system_id == simple_system.id
    assert end_check.action == 'compared_with_system'
    assert end_check.system_id == simple_system.id

    assert end_check.data['status'] == 1
    assert_contains([
        'Traceback (most recent call last):',
        'SynchronizationError',
        'check_roles returned False'
    ], end_check.data['traceback'])


def test_sync_forcibly_resolves_inconsistencies_if_user_is_not_active(simple_system, arda_users, responsible_gandalf):
    """Тестируем, что при принудительной синхронизации с системой в случае, если пользователь неактивен,
    а система его отдаёт, в системе отзывается роль, а неконсистентность разрешается с соответствующим комментарием
    и параметрами"""

    gandalf = responsible_gandalf
    sauron = arda_users.sauron
    Action.objects.all().delete()

    sauron.is_active = False
    sauron.save()

    all_roles = [{
        'login': 'sauron',
        'subject_type': 'user',
        'roles': [
            [{
                 'role': 'manager'
            }, {
                'passport-login': 'minor1'
            }]
        ]
    }]
    with mock_all_roles(simple_system, all_roles):
        sync_roles(simple_system, responsible_gandalf)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert inconsistency.state == 'resolved'
    assert Role.objects.count() == 0

    assert Action.objects.count() == 6
    actual_actions = list(Action.objects.order_by('pk').values_list('action', flat=True))
    assert actual_actions == ['started_comparison_with_system', 'compared_with_system', 'started_sync_with_system',
                              'remote_remove', 'resolve_inconsistency', 'synced_with_system']

    resolve_inactive_user_action = Action.objects.get(action='resolve_inconsistency')
    assert resolve_inactive_user_action.inconsistency_id is not None
    assert 'is_forcibly_resolved' in resolve_inactive_user_action.data
    assert 'resolved_because_user_is_not_active' in resolve_inactive_user_action.data
    assert 'user' in resolve_inactive_user_action.data
    assert resolve_inactive_user_action.data['user'] == 'sauron'
    expected_comment = 'Расхождение разрешено, так как пользователь уволен'
    assert resolve_inactive_user_action.data['comment'] == expected_comment

    assert gandalf.requested.count() == 4
    requester_actions = gandalf.requested.order_by('id')
    start_check, end_check, start_sync, end_sync = requester_actions
    assert start_check.action == 'started_comparison_with_system'
    assert end_check.action == 'compared_with_system'
    assert start_sync.action == 'started_sync_with_system'
    assert end_sync.action == 'synced_with_system'
    expected_end_sync_data = {
        'status': 0,
        'report_existed': 0,
        'report_created': 1,
        'report_errors': 0,
        'report_created_count': 0,
        'report_deprived_count': 1
    }
    assert end_sync.data == expected_end_sync_data


def test_synchronization_returns_unknown_user(simple_system, arda_users, responsible_gandalf):
    """Тестируем, что при принудительной синхронизации с системой мы корректно обрабатываем случай,
    когда система отдаёт нам логин, о котором мы ничего не знаем"""

    gandalf = responsible_gandalf

    all_roles = [{
        'login': 'wtf',
        'subject_type': 'user',
        'roles': [
            [{
                 'role': 'manager'
            }, {
                'passport-login': 'yndx-wtf-manager'
            }]
        ]
    }]
    with CountIncreasedContext((Inconsistency, 1), (Action, 4), (Role, 0)) as new_data:
        with mock_all_roles(simple_system, all_roles):
            sync_roles(simple_system, gandalf)

    inconsistency = Inconsistency.objects.get()
    assert inconsistency.type == Inconsistency.TYPE_UNKNOWN_USER
    assert inconsistency.state == 'active'
    assert Role.objects.count() == 0

    actual_actions = list(new_data.get_new_objects(Action).order_by('pk').values_list('action', flat=True))
    expected_actions = ['started_comparison_with_system', 'compared_with_system',
                        'started_sync_with_system', 'synced_with_system']
    assert actual_actions == expected_actions

    assert gandalf.requested.count() == 4
    requester_actions = gandalf.requested.order_by('pk')
    start_check, end_check, start_sync, end_sync = requester_actions
    assert start_sync.action == 'started_sync_with_system'
    assert end_sync.action == 'synced_with_system'
    expected_end_sync_data = {
        'status': 0,
        'report_existed': 0,
        'report_created': 0,
        'report_errors': 1,
        'report_created_count': 0,
        'report_deprived_count': 0
    }
    assert end_sync.data == expected_end_sync_data


def test_get_initial_roles(simple_system, arda_users, responsible_gandalf):
    """Проверим работу первоначальной синхронизации"""

    frodo = arda_users.frodo
    gandalf = responsible_gandalf

    roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager', }, {'passport-login': 'art1', 'some_system_field': '22'}],
            [{'role': 'manager'}, {'passport-login': 'art2'}],
            {'role': 'admin'},
            [{'role': 'poweruser'}, {}]
        ]
    }]
    with mock_all_roles(simple_system, roles):
        sync_roles(simple_system, gandalf)

        # факты начала и окончания синхронизации должны быть записаны в Action Log.
        assert gandalf.requested.count() == 4
        assert frodo.roles.count() == 4

        # проверяем роль manager-art1
        role = frodo.roles.get(system_specific={'passport-login': 'art1', 'some_system_field': '22'})
        assert role.state == 'granted'
        assert role.fields_data == {'passport-login': 'art1'}
        assert role.actions.count() == 4
        assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency'])

        # в комментарии к запросу сказано, что это был авто-импорт
        expected_comment = ('Запрошена при принудительном разрешении некосистентности: '
                            'синхронизации с системой или первичном импорте')
        assert role.actions.get(action='import').comment == expected_comment
        assert role.actions.get(action='import').data.get('is_forcibly_resolved') is True

        # проверяем роль manager-art2
        role = frodo.roles.select_related('system__actual_workflow').get(system_specific={'passport-login': 'art2'})
        assert role.state == 'granted'
        assert role.system_specific == {'passport-login': 'art2'}
        assert role.fields_data == {'passport-login': 'art2'}

        role.set_state('depriving')

        # проверяем, что после повторной синхронизации, роли не будут проимпортированы заново
        sync_roles(simple_system, gandalf)

        role = refresh(role)
        assert role.state == 'deprived'

        assert frodo.roles.count() == 5
        other_roles = frodo.roles.exclude(pk=role.pk)
        assert other_roles.count() == 4
        assert other_roles.filter(state='granted').count() == 4


def test_sync_when_there_are_some_failed_roles(simple_system, arda_users):
    """Проверим, что неактивные роли исключаются из рассмотрения"""

    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'admin'}, state='failed')
    roles = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [
                {'role': 'admin'}
            ]
        }
    ]

    with mock_all_roles(simple_system, roles):
        sync_roles(simple_system, arda_users.legolas)

    assert frodo.roles.count() == 2
    assert set(frodo.roles.values_list('state', flat=True)) == {'failed', 'granted'}


def test_sync_resolves_personal_roles_prefers_our_side(generic_system, arda_users, department_structure):
    """Проверим, что при синхронизации персональные роли разрешаются всегда в нашу пользу"""
    fellowship = department_structure.fellowship
    with mock.patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 0,
            'data': {}
        }
        make_role(fellowship, generic_system, {'role': 'admin'})
    assert Role.objects.filter(state='granted').count() == fellowship.members.count() + 1
    roles = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [
                {'role': 'admin'}
            ]
        }
    ]
    with mock_all_roles(generic_system, roles):
        with mock.patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
            send_data.return_value = {
                'code': 0,
                'data': {}
            }
            sync_roles(generic_system, arda_users.legolas)

    # неконсистентности разрешаются в нашу пользу
    assert Role.objects.filter(state='granted').count() == fellowship.members.count() + 1
    assert len(send_data.call_args_list) == fellowship.members.count() - 1


def test_everysync(complex_system, arda_users, settings):
    """Проверим работу ежедневной команды и убедимся, что сначала выполняется синхронизация дерева,
    а затем разрешение неконсистентностей"""

    settings.IDM_MIN_NODES_SYNC_INTERVAL = 2*60
    now = timezone.now()

    complex_system.metainfo.last_sync_nodes_start = now
    complex_system.metainfo.last_sync_nodes_finish = None
    complex_system.metainfo.save(update_fields=('last_sync_nodes_start', 'last_sync_nodes_finish'))
    complex_system.sync_interval = timedelta(seconds=5 * 60)
    complex_system.save(update_fields=('sync_interval',))

    Action.objects.all().delete()
    rules = RoleNode.objects.get(slug='rules')
    rules.is_auto_updated = True
    rules.save(update_fields=['is_auto_updated'])

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['ringholder'] = {
        'name': {
            'ru': 'Носитель кольца',
            'en': 'Ring holder',
        }
    }

    roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'project': 'rules', 'role': 'ringholder'}, {}],
        ]
    }]

    with mock_tree(complex_system, tree):
        with mock_all_roles(complex_system, roles):
            call_command('idm_check_and_resolve', log_check_time=True)

    assert Role.objects.count() == 1
    role = Role.objects.select_related('node').get()
    assert role.node.slug == 'ringholder'
    assert role.node.slug_path == '/project/rules/role/ringholder/'
    assert role.node.name == 'Носитель кольца'
    assert role.node.name_en == 'Ring holder'

    assert_action_chain(role, ['import', 'apply_workflow', 'approve', 'grant', 'resolve_inconsistency'])
    actions = list(Action.objects.values_list('action', flat=True).order_by('pk'))
    expected = [
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_CREATED,
        ACTION.MASS_ACTION,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.STARTED_COMPARISON_WITH_SYSTEM,
        ACTION.COMPARED_WITH_SYSTEM,
        ACTION.STARTED_SYNC_WITH_SYSTEM,
        ACTION.IMPORT,
        ACTION.APPLY_WORKFLOW,
        ACTION.APPROVE,
        ACTION.GRANT,
        ACTION.RESOLVE_INCONSISTENCY,
        ACTION.SYNCED_WITH_SYSTEM,
    ]
    assert actions == expected


@pytest.mark.parametrize('can_be_broken', [True, False])
def test_everysync_with_too_many_inconsistencies(simple_system, arda_users, can_be_broken):
    """Проверим работу синхронизации и убедимся, что большое количество неконсистентностей не разрешается"""

    simple_system.can_be_broken = can_be_broken
    simple_system.save()

    for user in arda_users:
        raw_make_role(arda_users[user], simple_system, {'role': 'poweruser'})

    with mock_all_roles(simple_system, []):
        call_command('idm_check_and_resolve', log_check_time=True)

    assert refresh(simple_system).is_broken == can_be_broken
    assert Action.objects.filter(action='resolve_inconsistency').count() == 0


def test_everysync_timestamps(simple_system, arda_users):
    """Проверим, что успешное завершение каждого шага пишет в базу таймстемп"""

    for user in arda_users:
        raw_make_role(arda_users[user], simple_system, {'role': 'poweruser'})

    old_time = timezone.datetime(1991, 1, 1, tzinfo=pytz.UTC)
    sync_time = timezone.datetime(3020, 2, 29, tzinfo=pytz.UTC)

    for task in ['deprive_nodes', 'check_inconsistencies', 'report_inconsistencies', 'resolve_inconsistencies']:
        for moment in ['start', 'finish']:
            setattr(simple_system.metainfo, 'last_{}_{}'.format(task, moment), old_time)
    simple_system.metainfo.save()

    with freeze_time(sync_time):
        with mock_all_roles(simple_system, []):
            call_command('idm_check_and_resolve', log_check_time=True)

    simple_system.metainfo.refresh_from_db()

    for task in ['deprive_nodes', 'check_inconsistencies', 'report_inconsistencies']:
        for moment in ['start', 'finish']:
            assert getattr(simple_system.metainfo, 'last_{}_{}'.format(task, moment)) == sync_time

    assert simple_system.metainfo.last_resolve_inconsistencies_start == sync_time
    assert simple_system.metainfo.last_resolve_inconsistencies_finish == old_time  # упали из-за поломки системы
