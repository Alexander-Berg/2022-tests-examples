# coding: utf-8


from datetime import timedelta

import pytest
from django.core.management import call_command
from django.utils import timezone
from django.conf import settings
from freezegun import freeze_time

from idm.core.constants.action import ACTION
from idm.core.models import RoleNode, Action, Role
from idm.tests.utils import (mock_tree, assert_action_chain, raw_make_role, refresh, days_from_now)
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import mock_tree, assert_action_chain, raw_make_role, refresh, mock_all_roles

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def reset_last_sync(complex_system):
    complex_system.metainfo.last_sync_nodes_start = None
    complex_system.metainfo.last_sync_nodes_finish = None
    complex_system.metainfo.save(update_fields=('last_sync_nodes_start', 'last_sync_nodes_finish'))


@pytest.fixture()
def subauto_system(complex_system):
    subs = RoleNode.objects.get(slug='subs')
    subs.is_auto_updated = True
    subs.save()
    Action.objects.all().delete()
    return complex_system


def test_auto_roles_update(subauto_system, patch_get_systems_in_sync_celery_queue):
    """Проверим, что если некоторые узлы помечены, как автообновляемые, то они и их подузлы автоматически обновляются.
    А если изменены какие-то другие узлы, то ничего не происходит"""

    tree = subauto_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['new_role'] = 'Новая Роль'
    tree['roles']['values']['rules']['roles']['values']['manager'] = 'Менеджер'
    # При синхронизации узлы удаляются с задержкой
    del tree['roles']['values']['subs']['roles']['values']['manager']

    with mock_tree(subauto_system, tree):
        call_command('idm_auto_update_roles')

    expected = {
        ACTION.ROLE_NODE_CREATED,
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_MARKED_DEPRIVING,
        ACTION.MASS_ACTION,
    }
    assert {action.action for action in Action.objects.all()} == expected
    add_action = Action.objects.select_related('role_node').get(action='role_node_created')
    assert Action.objects.filter(action='role_node_deleted').count() == 0
    assert add_action.user is None
    assert add_action.role_node.slug_path == '/project/subs/role/new_role/'

    Action.objects.all().delete()
    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        with mock_tree(subauto_system, tree):
            call_command('idm_auto_update_roles')

    expected = {
        ACTION.ROLE_NODE_DELETED,
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_ROLES_DEPRIVED,
    }
    assert {action.action for action in Action.objects.all()} == expected
    remove_action = Action.objects.select_related('role_node').get(action='role_node_deleted')
    assert remove_action.user is None
    assert remove_action.role_node.slug_path == '/project/subs/role/manager/'


@pytest.mark.thick_db
def test_check_respects_auto_update(arda_users, subauto_system):
    """Проверим, что мы уважаем помеченность галочками при сверке ролей"""

    tree = subauto_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['new_role'] = 'Новая Роль'
    tree['roles']['values']['rules']['roles']['values']['manager'] = 'Менеджер'
    del tree['roles']['values']['subs']['roles']['values']['manager']

    roles = [{
        'login': 'frodo',
        'roles': [
            # роль, для которой уже есть узел, но нужно синкануть дерево (и он внутри контура галочек автообновления)
            {'project': 'subs', 'role': 'new_role'},
            # роль, для которой есть узел, но он за пределами доступности галочек автообновления
            {'project': 'rules', 'role': 'manager'},
            # роль, для которой есть старый узел
            {'project': 'rules', 'role': 'admin'},
        ]
    }]

    with mock_tree(subauto_system, tree):
        with mock_all_roles(subauto_system, roles):
            call_command('idm_check_and_resolve')

    assert Inconsistency.objects.count() == 3
    assert Inconsistency.objects.active().count() == 1
    ic = Inconsistency.objects.active().get()
    assert ic.type == Inconsistency.TYPE_UNKNOWN_ROLE
    assert ic.remote_data == {'project': 'rules', 'role': 'manager'}
    assert Role.objects.count() == 2
    assert set(Role.objects.values_list('node__slug',flat=True)) == {'new_role', 'admin'}


def test_not_auto_updated_nodes_are_not_auto_updated(complex_system, patch_get_systems_in_sync_celery_queue):
    """Проверим, что если узлы не помечены флагом автообновления, то ничего не происходит"""

    Action.objects.all().delete()

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['new_role'] = 'Новая Роль'
    tree['roles']['values']['rules']['roles']['values']['manager'] = 'Менеджер'
    del tree['roles']['values']['subs']['roles']['values']['manager']

    with mock_tree(complex_system, tree):
        call_command('idm_auto_update_roles')

    assert Action.objects.count() == 0


def test_role_deprive_on_role_node_deprive(subauto_system, arda_users, patch_get_systems_in_sync_celery_queue):
    """Если из дерева пропадает узел, на который выдана роль, то роль должна отозваться"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    manager_role = raw_make_role(legolas, subauto_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    invisible_role = raw_make_role(frodo, subauto_system, {'project': 'rules', 'role': 'invisic'}, state='granted')
    Action.objects.all().delete()

    tree = subauto_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['developer']
    del tree['roles']['values']['subs']['roles']['values']['manager']

    # Помечаем для удаления
    with mock_tree(subauto_system, tree):
        call_command('idm_auto_update_roles')
    assert Role.objects.filter(is_active=True).count() == 2
    manager_role = refresh(manager_role)
    invisible_role = refresh(invisible_role)
    assert manager_role.state == 'granted'
    assert_action_chain(manager_role, [])
    assert invisible_role.state == 'granted'

    # Удаляем
    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        with mock_tree(subauto_system, tree):
            call_command('idm_auto_update_roles')
    assert Role.objects.filter(is_active=True).count() == 1
    manager_role = refresh(manager_role)
    invisible_role = refresh(invisible_role)
    assert manager_role.state == 'deprived'
    assert_action_chain(manager_role, ['deprive', 'first_remove_role_push', 'remove'])
    assert invisible_role.state == 'granted'


def test_role_does_not_deprive_if_role_node_deprived_is_not_auto_updated(complex_system, arda_users,
                                                                         patch_get_systems_in_sync_celery_queue):
    """Если из дерева пропадает узел, на который выдана роль, но этот узел не вложен в автообновляемый, то ничего не
    должно произойти"""

    legolas = arda_users.legolas
    manager_role = raw_make_role(legolas, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    Action.objects.all().delete()

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['developer']
    del tree['roles']['values']['subs']['roles']['values']['manager']

    with mock_tree(complex_system, tree):
        call_command('idm_auto_update_roles')

    assert Role.objects.filter(is_active=True).count() == 1
    manager_role = refresh(manager_role)
    assert manager_role.state == 'granted'


def test_deprived_roles_are_still_humanized(complex_system, patch_get_systems_in_sync_celery_queue):
    """Если из дерева удалён узел, он и его подузлы всё равно нормально показывают своё человеческое представление"""

    complex_system.root_role_node.is_auto_updated = True
    complex_system.root_role_node.save(update_fields=['is_auto_updated'])

    developer = RoleNode.objects.get(slug='developer')
    manager = RoleNode.objects.get(slug='manager')
    subs = RoleNode.objects.get(slug='subs')
    assert developer.humanize() == 'Проект: Подписки, Роль: Разработчик'
    assert manager.humanize() == 'Проект: Подписки, Роль: Менеджер'

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']

    with mock_tree(complex_system, tree):
        call_command('idm_auto_update_roles')

    subs = refresh(subs)
    developer = refresh(developer)
    manager = refresh(manager)
    assert subs.humanize() == 'Проект: Подписки'
    assert developer.humanize() == 'Проект: Подписки, Роль: Разработчик'
    assert manager.humanize() == 'Проект: Подписки, Роль: Менеджер'


def test_deprive_auto_updated_node(subauto_system, patch_get_systems_in_sync_celery_queue):
    """Можно удалить автообновляемый узел со всеми --чертями-- флагами автообновления"""

    tree = subauto_system.plugin.get_info()
    del tree['roles']['values']['subs']

    with mock_tree(subauto_system, tree):
        call_command('idm_auto_update_roles')

    subs = RoleNode.objects.get(slug='subs')
    assert subs.state == 'depriving'


def test_rename_node_affects_subnodes(subauto_system, patch_get_systems_in_sync_celery_queue):
    """Если переименован один из узлов, дочерние узлы тоже меняют свой humanize()"""

    developer = RoleNode.objects.get(slug='developer')
    manager = RoleNode.objects.get(slug='manager')
    assert developer.humanize() == 'Проект: Подписки, Роль: Разработчик'
    assert manager.humanize() == 'Проект: Подписки, Роль: Менеджер'

    tree = subauto_system.plugin.get_info()
    tree['roles']['values']['subs']['name'] = 'Обновления'

    with mock_tree(subauto_system, tree):
        call_command('idm_auto_update_roles')

    subs = RoleNode.objects.get(slug='subs')
    developer = refresh(developer)
    manager = refresh(manager)
    assert subs.humanize() == 'Проект: Обновления'
    assert developer.humanize() == 'Проект: Обновления, Роль: Разработчик'
    assert manager.humanize() == 'Проект: Обновления, Роль: Менеджер'


def test_auto_roles_update_two_branches(complex_system, patch_get_systems_in_sync_celery_queue):
    """Проверяет автоматическое обновление для случая, когда одна ветка отмечена для автоматического обновления,
    а изменения в другой ветке"""
    Action.objects.all().delete()

    rules = RoleNode.objects.get(slug='rules')
    rules.is_auto_updated = True
    rules.save(update_fields=['is_auto_updated'])

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['new_role'] = 'new role'

    with mock_tree(complex_system, tree):
        call_command('idm_auto_update_roles')

    # ничего не должно произойти
    assert Action.objects.count() == 2
    assert set(Action.objects.values_list('action', flat=True)) == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
    }


def test_auto_update_interval(complex_system, settings, patch_get_systems_in_sync_celery_queue):
    """Проверим работу скедулера"""

    settings.IDM_MIN_NODES_SYNC_INTERVAL = 2*60

    Action.objects.all().delete()
    rules = RoleNode.objects.get(slug='rules')
    rules.is_auto_updated = True
    rules.save(update_fields=['is_auto_updated'])

    now = timezone.now()
    complex_system.metainfo.last_sync_nodes_finish = now
    complex_system.metainfo.save()
    # интервал обновления - пять минут
    complex_system.sync_interval = timedelta(seconds=5*60)
    complex_system.save()

    # первоначальная синхронизация запустится в любом случае
    with mock_tree(complex_system, complex_system.plugin.get_info()):
        call_command('idm_auto_update_roles')
    assert Action.objects.count() == 2
    Action.objects.all().delete()
    prev_sync = refresh(complex_system.metainfo).last_sync_nodes_finish

    # теперь переведём время на две минуты вперёд: синхронизация не должна запуститься
    with freeze_time(now + timedelta(seconds=2*60)):
        with mock_tree(complex_system, complex_system.plugin.get_info()):
            call_command('idm_auto_update_roles')

    assert Action.objects.count() == 0
    complex_system = refresh(complex_system)
    assert complex_system.metainfo.last_sync_nodes_finish == prev_sync

    # переведём время на шесть минут вперёд: синхронизация должна запуститься
    with mock_tree(complex_system, complex_system.plugin.get_info()):
        with freeze_time(now + timedelta(seconds=6*60)):
            call_command('idm_auto_update_roles')

    complex_system = refresh(complex_system)
    assert complex_system.metainfo.last_sync_nodes_finish > prev_sync

    assert Action.objects.count() == 2
    assert set(Action.objects.values_list('action', flat=True)) == {'role_tree_started_sync', 'role_tree_synced'}


def test_not_auto_updated_nodes_if_system_already_in_queue(subauto_system, monkeypatch):
    """Проверим, что если система уже в очереди на обновление, то синхронизация не запускается."""

    Action.objects.all().delete()
    monkeypatch.setattr(
        'idm.core.querysets.system.SystemQuerySet.get_systems_in_sync_celery_queue',
        lambda x: {subauto_system.slug},
    )
    tree = subauto_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['manager']

    with mock_tree(subauto_system, tree):
        call_command('idm_auto_update_roles')

    assert Action.objects.count() == 0
