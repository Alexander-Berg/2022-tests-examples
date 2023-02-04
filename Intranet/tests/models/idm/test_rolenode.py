# coding: utf-8


import copy
import itertools
from textwrap import dedent

import pytest
import waffle.testutils
from django.conf import settings
from django.db.models import Count
from django.utils import functional, timezone
from freezegun import freeze_time

from idm.core import canonical, node_pipeline
from idm.core.constants.action import ACTION
from idm.core.constants.instrasearch import INTRASEARCH_METHOD
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.constants.rolenode import ROLENODE_STATE
from idm.core.constants.node_relocation import RELOCATION_STATE
from idm.core.models import (Role, RoleAlias, RoleField, Action, RoleNode, NodeResponsibility, InternalRole,
                             InternalRoleUserObjectPermission, RoleNodeResponsibilityAction)
from idm.permissions.shortcuts import can_request_role
from idm.tests.utils import (raw_make_role, refresh, assert_action_chain, mock_tree, CountIncreasedContext,
                             ctt_data_is_consistent, sync_role_nodes, add_perms_by_role, days_from_now,
                             set_workflow, assert_contains, random_slug, create_system, set_roles_tree)
from idm.utils import reverse
from idm.utils.rolenode import deprive_nodes

pytestmark = pytest.mark.django_db


def assert_report(action_data, **kwargs):
    data = {
        'status': 0,
        'report': {
            'nodes_created': 0,
            'nodes_changed': 0,
            'nodes_deleted': 0,
            'fields_removed': 0,
            'fields_created': 0,
            'fields_changed': 0,
            'aliases_removed': 0,
            'aliases_created': 0,
            'responsibilities_created': 0,
            'responsibilities_changed': 0,
            'responsibilities_removed': 0,
        }
    }
    data['report'].update(**kwargs)
    assert action_data == data


def check_role_on_node_deprival(role, deprived):
    role = refresh(role)
    assert role.state == 'deprived' if deprived else 'granted'
    assert_action_chain(role, ['deprive', 'first_remove_role_push', 'remove'] if deprived else [])
    if deprived:
        deprive = role.actions.get(action='deprive')
        assert deprive.data == {'comment': 'Роль больше не поддерживается системой', 'force_deprive': True}


def test_failed_fetch(complex_system, arda_users):
    with mock_tree(complex_system, AttributeError):
        with CountIncreasedContext((Action, 2)) as new_data:
            sync_role_nodes(complex_system)

    actions = new_data.get_new_objects(Action)
    assert actions.count() == 2
    start = actions.get(action=ACTION.ROLE_TREE_STARTED_SYNC)
    assert start.system_id == complex_system.id
    finish = actions.get(action=ACTION.ROLE_TREE_SYNC_FAILED)
    assert finish.system_id == complex_system.id

    assert finish.data['status'] == 1
    assert_contains([
        "Traceback (most recent call last):",
        "AttributeError",
    ], finish.data['traceback'])


def test_rolenode_humanize(pt1_system, arda_users):
    role = raw_make_role(arda_users.frodo, pt1_system, {'project': 'proj1', 'role': 'admin'})
    assert role.humanize() == 'Проект: Проект 1, Роль: Админ'
    assert role.humanize(format='short') == 'Проект 1 / Админ'


def get_queue(system, force_update=True):
    system.root_role_node.rehash()
    fetcher = system.root_role_node.fetcher
    fetcher.prepare()
    data = fetcher.fetch(system)
    queue = system.root_role_node.get_queue(data, force_update=force_update)
    return queue


def strip_queue(queue):
    return [item for item in queue if item.type != 'hashupdate']


def test_simple_system_get_queue(simple_system, complex_system):
    """Проверяем метод сверки двух деревьев ролей на наличие новых и удаленных ролей"""

    queue = get_queue(simple_system)
    assert bool(queue) is False

    with mock_tree(simple_system, complex_system.plugin.get_info()):
        queue = get_queue(simple_system)
        assert len(queue) == 26
        expected = ['add'] * 3 + ['add', 'hashupdate'] * 2 + ['hashupdate'] * 2 + ['add'] * 2
        expected += ['add', 'hashupdate'] * 3 + ['hashupdate'] * 3 + ['remove'] * 5 + ['hashupdate']
        assert [queue_item.type for queue_item in queue] == expected

        sorted_queue = sorted(queue, key=lambda item: (item.type, item.counter))
        grouped_queue = itertools.groupby(sorted_queue, key=lambda item: item.type)
        adds, hashupdates, removes = [list(items) for _, items in grouped_queue]

        add = adds[0]
        assert add.node.level == 0
        assert add.child_data.name == 'Проект'

        expected = {'/role/poweruser/', '/role/manager/', '/role/superuser/', '/role/admin/', '/role/'}
        assert {remove.node.slug_path for remove in removes} == expected

    tree = {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'admin': 'Админ',
                'manager': 'Менеджер',
                'poweruser': 'Могучий Пользователь',
                'superuser': 'Супер Пользователь',
                'new_node': 'Новый узел'
            }
        },
        'fields': [
            {
                'slug': 'login',
                'name': 'Доп поле',
                'required': False,
            },
            {
                'slug': 'passport-login',
                'name': 'Паспортный логин',
                'required': False
            }
        ]
    }
    with mock_tree(simple_system, tree):
        queue = get_queue(simple_system)
        assert len(queue) == 4
        assert len(strip_queue(queue)) == 1
        item = queue[0]
        assert item.type == 'add'
        assert item.node.slug_path == '/role/'
        assert item.node.is_key
        assert item.node.name == 'Роль'
        assert item.child_data.slug == 'new_node'
        assert item.child_data.name == 'Новый узел'


def test_complex_system_get_queue(complex_system):
    queue = get_queue(complex_system)
    assert bool(queue) is False
    with mock_tree(complex_system, {'code': 0}):
        queue = get_queue(complex_system)
        assert len(queue) == 28
        assert [item.type for item in queue] == ['remove'] * 27 + ['hashupdate']

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['rules']
    tree['roles']['values']['subs']['roles']['values']['new_role'] = 'Новая роль'
    with mock_tree(complex_system, tree):
        queue = get_queue(complex_system)
        assert len(queue) == 14
        assert len(strip_queue(queue)) == 9
        assert [item.type for item in strip_queue(queue)] == ['add'] + ['remove'] * 8

        add = queue.items[0]
        # так как мы изменили подлистья "проекта", то он попал в очередь с флагом modify
        assert add.node.slug_path == '/project/subs/role/'
        assert add.node.name == 'роль'
        assert add.child_data.name == 'Новая роль'

        slugs = {remove.node.slug_path for remove in queue if remove.type == 'remove'}
        expected = {
            '/project/rules/',
            '/project/rules/role/',
            '/project/rules/role/admin/',
            '/project/rules/role/auditor/',
            '/project/rules/role/invisic/',
            '/project/rules/role/admin/',
            '/project/rules/role/auditor/',
            '/project/rules/role/invisic/',
        }
        assert slugs == expected


def test_queue_when_changing_help_text(complex_system):
    """Проверка состояния очереди при изменении описания ролей"""
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['help'] = 'Управляющий'
    tree['roles']['values']['rules']['roles']['values']['auditor']['name'] = {
        'ru': 'Аудитор',
        'en': 'Auditor',
    }
    tree['roles']['values']['rules']['roles']['values']['auditor']['help'] = {
        'ru': 'Занимается аудитом',
        'en': 'Audits internal processes',
    }
    with mock_tree(complex_system, tree):
        queue = get_queue(complex_system)
    assert len(queue) == 10
    stripped = strip_queue(queue)
    assert len(stripped) == 2
    manager, auditor = stripped
    assert auditor.type == 'modify'
    assert manager.type == 'modify'
    assert auditor.node.name == 'Аудитор'
    assert auditor.node.slug_path == '/project/rules/role/auditor/'
    assert auditor.new_data.name == 'Аудитор'
    assert auditor.new_data.name_en == 'Auditor'
    assert auditor.new_data.description == 'Занимается аудитом'
    assert manager.node.name == 'Менеджер'
    assert manager.node.slug_path == '/project/subs/role/manager/'
    assert manager.new_data.name == 'Менеджер'
    assert manager.new_data.name_en == 'Менеджер'
    assert manager.new_data.description == 'Управляющий'
    assert manager.new_data.description_en == 'Управляющий'


def test_rolenode_rename(complex_system, arda_users):
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['help'] = 'Управляющий'
    tree['roles']['values']['rules']['roles']['values']['auditor']['name'] = {
        'ru': 'Аудитор',
        'en': 'Auditor',
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 4 + 1)) as new_data:
            sync_role_nodes(complex_system, requester=arda_users.frodo)
    actions = new_data.get_new_objects(Action)
    role_node_changes = actions.select_related('role_node').filter(action='role_node_changed')
    change2, change1 = role_node_changes
    assert change1.role_node.slug_path == '/project/rules/role/auditor/'
    assert change2.role_node.slug_path == '/project/subs/role/manager/'
    assert change1.data['diff'] == {
        'name_en': ['Аудитор', 'Auditor'],
    }
    assert change2.data['diff'] == {
        'description': ['Управленец', 'Управляющий'],
        'description_en': ['Управленец', 'Управляющий'],
    }
    auditor = change1.role_node
    manager = change2.role_node
    assert auditor.name_en == 'Auditor'
    assert manager.description == 'Управляющий'


def test_queue_when_changing_firewall(complex_system):
    """Проверка состояния очереди при изменении правил фаерволла"""
    tree = complex_system.plugin.get_info()
    tree['roles']['firewall-declaration'] = 'test-project-new-firewall'
    with mock_tree(complex_system, tree):
        queue = get_queue(complex_system)
        assert len(queue) == 3
        stripped = strip_queue(queue)
        assert len(stripped) == 1
        modify, = stripped
        assert modify.type == 'modify'
        assert modify.node.name == 'Проект'
        assert modify.node.slug_path == '/project/'
        expected = canonical.CanonicalAlias(
            type='firewall',
            name='test-project-new-firewall',
            name_en='test-project-new-firewall',
        )
        assert modify.new_data.aliases == {
            expected.as_key(): expected
        }


def test_add_nodes(complex_system, arda_users):
    """Тестируем добавление узлов в дерево"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = 'Разработчик'
    tree['roles']['values']['rules']['roles']['values']['editor'] = {
        'name': 'Редактор',
        'roles': {
            'slug': 'editor_type',
            'name': 'Тип редактора',
            'values': {
                'all': 'Редактор всего',
                'limited': {
                    'name': {
                        'ru': 'С ограниченными правами',
                        'en': 'With limited rights',
                    },
                    'help': {
                        'ru': 'Описание',
                        'en': 'Description',
                    }
                }
            }
        }
    }
    Action.objects.all().delete()
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)
    rolenode = RoleNode.objects.get(slug='limited')
    assert rolenode.name == 'С ограниченными правами'
    assert rolenode.name_en == 'With limited rights'
    assert rolenode.description == 'Описание'
    assert rolenode.description_en == 'Description'
    assert Action.objects.count() == 8
    assert Action.objects.filter(requester=frodo).count() == 7
    assert Action.objects.filter(user=frodo).count() == 0
    stats_qs = (Action.objects.values_list('action').
                annotate(x=Count('action')).
                values_list('action', 'x').
                order_by('-x'))
    assert dict(stats_qs) == {
        ACTION.ROLE_TREE_STARTED_SYNC: 1,
        ACTION.ROLE_TREE_SYNCED: 1,
        ACTION.ROLE_NODE_CREATED: 5,
        ACTION.MASS_ACTION: 1,
    }


def test_add_nodes_with_suggest_field(complex_system, arda_users):
    """Тестируем добавление узлов с валидацией suggest-полей"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['tester'] = {
        'name': 'Тестировщик',
        'fields': [{
            'slug': 'macro',
            'name': 'Network Macro',
            'required': True,
            'type': 'suggestfield',
        }]
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)  # с пустым options
    assert not RoleNode.objects.filter(slug='tester').exists()

    tree['roles']['values']['rules']['roles']['values']['tester']['fields'][0]['options'] = {'suggest': 'strange'}
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)  # с несуществующим suggest в options
    assert not RoleNode.objects.filter(slug='tester').exists()

    tree['roles']['values']['rules']['roles']['values']['tester']['fields'][0]['options'] = {'suggest': 'macros'}
    Action.objects.all().delete()
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)  # с нормальным suggest в options
    assert RoleNode.objects.filter(slug='tester').exists()
    assert Action.objects.count() == 5
    assert Action.objects.filter(requester=frodo).count() == 4
    assert Action.objects.filter(user=frodo).count() == 0
    stats_qs = (Action.objects.values_list('action').
                annotate(x=Count('action')).
                values_list('action', 'x').
                order_by('-x'))
    assert dict(stats_qs) == {
        ACTION.ROLE_TREE_STARTED_SYNC: 1,
        ACTION.ROLE_TREE_SYNCED: 1,
        ACTION.ROLE_NODE_CREATED: 1,
        ACTION.ROLE_NODE_FIELD_CREATED: 1,
        ACTION.MASS_ACTION: 1,
    }


def test_remove_node_with_fields_and_aliases(complex_system, arda_users):
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['editor'] = {
        'name': 'Редактор',
        'fields': [{
            'type': 'booleanfield',
            'slug': 'has_skill',
            'name': 'Has skills to translate text',
        }],
        'aliases': [{
            'name': 'translator',
        }],
        'responsibilities': [{
            'username': 'frodo',
            'notify': True,
        }]
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext(
                (NodeResponsibility, 1), (RoleAlias, 1), (RoleField, 1), (RoleNodeResponsibilityAction, 1), (Action, 6),
                (InternalRole, 1),
                (InternalRoleUserObjectPermission, 4)
        ) as manager:
            sync_role_nodes(complex_system)
    assert NodeResponsibility.objects.count() == 1
    assert RoleAlias.objects.filter(type='default').count() == 1
    assert RoleField.objects.filter(type='booleanfield').count() == 1
    expected = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_FIELD_CREATED,
        ACTION.ROLE_NODE_ALIAS_CREATED,
        ACTION.ROLE_NODE_CREATED,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.MASS_ACTION,
    }
    assert set(manager.get_new_objects(Action).values_list('action', flat=True)) == expected

    # помечаем узел для удаления и при следующей синхронизации удаляем его
    RoleNodeResponsibilityAction.objects.all().delete()
    with mock_tree(complex_system, complex_system.plugin.get_info()):
        sync_role_nodes(complex_system)
    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        with mock_tree(complex_system, complex_system.plugin.get_info()):
            with CountIncreasedContext((Action, 6), (RoleNodeResponsibilityAction, 1)) as manager:
                sync_role_nodes(complex_system)
    expected = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_ALIAS_REMOVED,
        ACTION.ROLE_NODE_FIELD_REMOVED,
        ACTION.ROLE_NODE_DELETED,
        ACTION.ROLE_NODE_ROLES_DEPRIVED,
        ACTION.ROLE_TREE_SYNCED,
    }
    assert set(manager.get_new_objects(Action).values_list('action', flat=True)) == expected

    assert InternalRole.objects.count() == 0
    assert InternalRoleUserObjectPermission.objects.count() == 0
    assert NodeResponsibility.objects.filter(is_active=True).count() == 0
    assert RoleField.objects.filter(is_active=True, type='booleanfield').count() == 0
    assert RoleAlias.objects.filter(is_active=True, type='default').count() == 0


@pytest.mark.parametrize('from_api', [False, True])
def test_deprive_node_deferredly(complex_system, arda_users, from_api):
    """Тестируем удаление узлов и выданных на них ролей через API и через механизм синхронизации"""
    frodo = arda_users.frodo
    role = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    assert role.state == 'granted'
    node = complex_system.nodes.get(slug='manager')
    assert node.state == 'active'
    assert node.is_active()
    assert not node.depriving_at

    now = timezone.now()
    deprival_time = (now + timezone.timedelta(days=settings.IDM_DEPRIVING_NODE_TTL)) if not from_api else now
    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['manager']
    del tree['fields']

    if from_api:
        with freeze_time(now):
            node.mark_depriving(immediately=True)
        with freeze_time(now + timezone.timedelta(milliseconds=1)):
            deprive_nodes(complex_system, from_api=True)
    else:
        with freeze_time(now):
            with mock_tree(complex_system, tree):
                sync_role_nodes(complex_system, from_api=from_api)

    node = refresh(node)
    assert node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert node.state == ('deprived' if from_api else 'depriving')
    assert not node.is_active()
    assert node.depriving_at == deprival_time
    if from_api:
        check_role_on_node_deprival(role, deprived=True)
    else:
        with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1, now=now):
            deprive_nodes(complex_system)
            node = refresh(node)
            assert node.state == 'deprived'
            assert not node.is_active()
            assert node.depriving_at == deprival_time
            check_role_on_node_deprival(role, deprived=True)


def test_deprive_and_restore_node(complex_system, arda_users):
    """Тестируем удаление и восстановление узлов и выданных на них ролей"""
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    aragorn = arda_users.aragorn
    frodo_role = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    gandalf_role = raw_make_role(gandalf, complex_system, {'project': 'subs', 'role': 'developer'})
    aragorn_role = raw_make_role(aragorn, complex_system, {'project': 'rules', 'role': 'admin'})
    check_role_on_node_deprival(frodo_role, deprived=False)
    check_role_on_node_deprival(gandalf_role, deprived=False)
    check_role_on_node_deprival(aragorn_role, deprived=False)
    frodo_node = complex_system.nodes.get(slug='manager')
    gandalf_node = complex_system.nodes.get(slug='developer')
    aragorn_node = complex_system.nodes.get(slug='admin')
    gandalf_node.unique_id = 'developer'
    gandalf_node.save(update_fields=['unique_id'])
    assert frodo_node.state == ROLENODE_STATE.ACTIVE
    assert gandalf_node.state == ROLENODE_STATE.ACTIVE
    assert aragorn_node.state == ROLENODE_STATE.ACTIVE
    assert not frodo_node.need_isearch_push_method
    assert not gandalf_node.need_isearch_push_method
    assert not aragorn_node.need_isearch_push_method
    assert not frodo_node.depriving_at
    assert not gandalf_node.depriving_at
    assert not aragorn_node.depriving_at
    with mock_tree(complex_system, complex_system.plugin.get_info()):
        sync_role_nodes(complex_system)

    now = timezone.now()
    deprival_time = now + timezone.timedelta(days=settings.IDM_DEPRIVING_NODE_TTL)
    bad_tree = complex_system.plugin.get_info()
    del bad_tree['roles']['values']['subs']['roles']['values']['manager']
    del bad_tree['fields']
    bad_tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'unique_id': 'developer',
    }
    worse_tree = copy.deepcopy(bad_tree)
    del worse_tree['roles']['values']['subs']['roles']['values']['developer']
    del worse_tree['roles']['values']['rules']['roles']['values']['admin']

    # Удалим узлы и запустим синхронизацию
    with freeze_time(now):
        with mock_tree(complex_system, worse_tree):
            sync_role_nodes(complex_system)
    frodo_node = refresh(frodo_node)
    gandalf_node = refresh(gandalf_node)
    aragorn_node = refresh(aragorn_node)
    assert frodo_node.state == ROLENODE_STATE.DEPRIVING
    assert gandalf_node.state == ROLENODE_STATE.DEPRIVING
    assert aragorn_node.state == ROLENODE_STATE.DEPRIVING
    assert frodo_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert gandalf_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert aragorn_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert frodo_node.depriving_at == deprival_time
    assert gandalf_node.depriving_at == deprival_time
    assert aragorn_node.depriving_at == deprival_time
    check_role_on_node_deprival(frodo_role, deprived=False)
    check_role_on_node_deprival(gandalf_role, deprived=False)
    check_role_on_node_deprival(aragorn_role, deprived=False)

    # Снова запустим синхронизацию, чтобы убедиться, что узлы с неистёкшим сроком удаления не удалятся
    with days_from_now(1, now=now):
        with mock_tree(complex_system, worse_tree):
            sync_role_nodes(complex_system)
    frodo_node = refresh(frodo_node)
    gandalf_node = refresh(gandalf_node)
    aragorn_node = refresh(aragorn_node)
    assert frodo_node.state == ROLENODE_STATE.DEPRIVING
    assert gandalf_node.state == ROLENODE_STATE.DEPRIVING
    assert aragorn_node.state == ROLENODE_STATE.DEPRIVING
    assert frodo_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert gandalf_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert aragorn_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert frodo_node.depriving_at == deprival_time
    assert gandalf_node.depriving_at == deprival_time
    assert aragorn_node.depriving_at == deprival_time
    check_role_on_node_deprival(frodo_role, deprived=False)
    check_role_on_node_deprival(gandalf_role, deprived=False)
    check_role_on_node_deprival(aragorn_role, deprived=False)

    # Восстановим узел
    with days_from_now(2, now=now):
        with mock_tree(complex_system, bad_tree):
            sync_role_nodes(complex_system)
    frodo_node = refresh(frodo_node)
    gandalf_node = complex_system.nodes.active().get(slug='developer')
    aragorn_node = refresh(aragorn_node)
    assert frodo_node.state == ROLENODE_STATE.DEPRIVING
    assert gandalf_node.state == ROLENODE_STATE.ACTIVE
    assert aragorn_node.state == ROLENODE_STATE.ACTIVE
    assert frodo_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert gandalf_node.need_isearch_push_method == INTRASEARCH_METHOD.ADD
    assert aragorn_node.need_isearch_push_method == INTRASEARCH_METHOD.ADD
    assert frodo_node.depriving_at == deprival_time
    assert not gandalf_node.depriving_at
    assert not aragorn_node.depriving_at
    check_role_on_node_deprival(frodo_role, deprived=False)
    check_role_on_node_deprival(gandalf_role, deprived=False)
    check_role_on_node_deprival(aragorn_role, deprived=False)

    # Снова запустим синхронизацию
    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1, now=now):
        with mock_tree(complex_system, bad_tree):
            sync_role_nodes(complex_system)
    frodo_node = refresh(frodo_node)
    gandalf_node = refresh(gandalf_node)
    aragorn_node = refresh(aragorn_node)
    assert frodo_node.state == ROLENODE_STATE.DEPRIVED
    assert gandalf_node.state == ROLENODE_STATE.ACTIVE
    assert aragorn_node.state == ROLENODE_STATE.ACTIVE
    assert frodo_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE
    assert gandalf_node.need_isearch_push_method == INTRASEARCH_METHOD.ADD
    assert aragorn_node.need_isearch_push_method == INTRASEARCH_METHOD.ADD
    assert frodo_node.depriving_at == deprival_time
    assert not gandalf_node.depriving_at
    assert not aragorn_node.depriving_at
    check_role_on_node_deprival(frodo_role, deprived=True)
    check_role_on_node_deprival(gandalf_role, deprived=False)
    check_role_on_node_deprival(aragorn_role, deprived=False)


def test_new_firewall_rule(complex_system):
    """Проверка создания правила фаерволла"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['firewall-declaration'] = 'manager-firewall'
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleAlias, 1)) as new_data:
            sync_role_nodes(complex_system)

    aliases = new_data.get_new_objects(RoleAlias)
    alias = aliases.select_related('node').get()
    assert alias.node.slug_path == '/project/subs/role/manager/'
    assert alias.name_en == 'manager-firewall'
    assert alias.type == RoleAlias.FIREWALL_ALIAS
    actions = new_data.get_new_objects(Action)
    assert {action.action for action in actions} == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_ALIAS_CREATED,
    }
    alias_created = actions.select_related('role_node').get(action='role_node_alias_created')
    assert alias_created.role_node.slug_path == '/project/subs/role/manager/'
    assert alias_created.role_alias_id == alias.id
    start_sync = actions.get(action=ACTION.ROLE_TREE_STARTED_SYNC)
    assert start_sync.system_id == complex_system.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_sync.system_id == complex_system.id
    assert_report(end_sync.data, aliases_created=1)


def test_remove_firewall_rule(complex_system):
    """Проверка удаления правила фаерволла"""

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['rules']['firewall-declaration']
    rules = RoleNode.objects.get(slug='rules')
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleAlias, 0)) as new_data:
            sync_role_nodes(complex_system)

    alias = rules.aliases.get()
    assert alias.is_active is False
    actions = new_data.get_new_objects(Action)
    assert {action.action for action in actions} == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_ALIAS_REMOVED,
    }
    alias_removed = actions.get(action='role_node_alias_removed')
    assert alias_removed.user is None
    assert alias_removed.role_alias_id == alias.id
    start_sync = actions.get(action=ACTION.ROLE_TREE_STARTED_SYNC)
    assert start_sync.system_id == complex_system.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_sync.system_id == complex_system.id
    assert_report(end_sync.data, aliases_removed=1)


def test_change_firewall_rule(complex_system):
    """Проверка изменения правила фаерволла"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['firewall-declaration'] = 'other-rule'
    rules = RoleNode.objects.get(slug='rules')
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 4), (RoleAlias, 1)) as new_data:
            sync_role_nodes(complex_system)

    aliases = rules.aliases.all()
    assert aliases.count() == 2
    (inactive,), (active,) = functional.partition(lambda alias_: alias_.is_active, aliases)
    assert inactive.is_active is False
    assert inactive.name == 'test-project-rules'
    assert active.is_active
    assert active.name == 'other-rule'

    actions = new_data.get_new_objects(Action)
    alias_removed = actions.get(action='role_node_alias_removed')
    assert alias_removed.user is None
    assert alias_removed.role_alias_id == inactive.id
    alias_created = actions.get(action='role_node_alias_created')
    assert alias_created.user_id is None
    assert alias_created.role_alias_id == active.id
    assert actions.filter(action=ACTION.ROLE_TREE_STARTED_SYNC).count() == 1
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, aliases_removed=1, aliases_created=1)


def test_remove_then_add_rule(complex_system):
    """Удалим, добавим, удалим, а потом снова добавим такое же правило фаерволла"""

    tree = complex_system.plugin.get_info()
    tree2 = copy.deepcopy(tree)
    del tree['roles']['values']['rules']['firewall-declaration']
    tree3 = copy.deepcopy(tree)
    tree4 = copy.deepcopy(tree2)
    rules = RoleNode.objects.get(slug='rules')
    # при добавлении ранее существовшего alias-а не создаем дубликат, а восстанаваливаем старый
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleAlias, 0)):
        sync_role_nodes(complex_system)
    with mock_tree(complex_system, tree2), CountIncreasedContext((Action, 3), (RoleAlias, 0)):
        sync_role_nodes(complex_system)
    with mock_tree(complex_system, tree3), CountIncreasedContext((Action, 3), (RoleAlias, 0)):
        sync_role_nodes(complex_system)
    with mock_tree(complex_system, tree4), CountIncreasedContext((Action, 3), (RoleAlias, 0)):
        sync_role_nodes(complex_system)


def test_new_alias(complex_system):
    """Проверка создания синонима"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['aliases'] = [{
        'name': {
            'ru': 'босс',
            'en': 'boss'
        }
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleAlias, 1)) as new_data:
            sync_role_nodes(complex_system)

    aliases = new_data.get_new_objects(RoleAlias)
    alias = aliases.select_related('node').get()
    assert alias.node.slug_path == '/project/subs/role/manager/'
    assert alias.name == 'босс'
    assert alias.name_en == 'boss'
    assert alias.type == RoleAlias.DEFAULT_ALIAS
    actions = new_data.get_new_objects(Action)
    assert {action.action for action in actions} == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_ALIAS_CREATED,
    }
    alias_created = actions.select_related('role_node').get(action='role_node_alias_created')
    assert alias_created.role_node.slug_path == '/project/subs/role/manager/'
    assert alias_created.role_alias_id == alias.id
    start_sync = actions.get(action=ACTION.ROLE_TREE_STARTED_SYNC)
    assert start_sync.system_id == complex_system.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_sync.system_id == complex_system.id
    assert_report(end_sync.data, aliases_created=1)


def test_new_responsibility(complex_system, arda_users):
    """Проверка создания ответственных"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }, {
        'username': 'johnsnow',
        'notify': False,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((RoleNodeResponsibilityAction, 1), (Action, 2), (NodeResponsibility, 1)) as new_data:
            sync_role_nodes(complex_system)

    responsibilities = new_data.get_new_objects(NodeResponsibility)
    responsibility = responsibilities.select_related('node__parent__parent').get()
    assert responsibility.user_id == frodo.id
    assert responsibility.notify is True
    assert responsibility.is_active is True
    actions = new_data.get_new_objects(Action)
    assert {action.action for action in actions} == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
    }
    responsibility_actions = new_data.get_new_objects(RoleNodeResponsibilityAction)
    action = responsibility_actions.select_related('role_node').get(action='role_node_responsibility_created')
    assert action.role_node.slug_path == '/project/subs/role/manager/'
    assert action.node_responsibility_id == responsibility.id
    start_sync = actions.get(action=ACTION.ROLE_TREE_STARTED_SYNC)
    assert start_sync.system_id == complex_system.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_sync.system_id == complex_system.id
    assert_report(end_sync.data, responsibilities_created=1)

    # проверим, что у frodo появились необходимые права
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node)) is True
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system,
                                 responsibility.node.parent.parent)) is False


def test_change_responsibility(complex_system, arda_users):
    """Проверка изменения флага notify"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((RoleNodeResponsibilityAction, 1), (Action, 2), (NodeResponsibility, 1)):
            sync_role_nodes(complex_system)

    RoleNodeResponsibilityAction.objects.all().delete()
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': False,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((RoleNodeResponsibilityAction, 1), (Action, 2), (NodeResponsibility, 0)) as new_data:
            sync_role_nodes(complex_system)
    responsibility = NodeResponsibility.objects.get()
    assert responsibility.notify is False
    actions = new_data.get_new_objects(Action)
    responsibility_actions = new_data.get_new_objects(RoleNodeResponsibilityAction)
    action = responsibility_actions.select_related('role_node').get(action='role_node_responsibility_changed')
    assert action.role_node.slug_path == '/project/subs/role/manager/'
    assert action.node_responsibility_id == responsibility.id
    assert action.data == {
        'diff': {
            'notify': [True, False]
        }
    }
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, responsibilities_changed=1)


def test_delete_and_restore_responsibility(complex_system, arda_users):
    """Проверим удаление и восстановление ответственности за ноду"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeResponsibilityAction, 1), (NodeResponsibility, 1)):
            sync_role_nodes(complex_system)

    Action.objects.all().delete()
    tree = complex_system.plugin.get_info()
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeResponsibilityAction, 1), (NodeResponsibility, 0)) as new_data:
            sync_role_nodes(complex_system)
    responsibility = NodeResponsibility.objects.select_related('node__parent__parent').get()
    assert responsibility.is_active is False
    assert responsibility.notify is True
    actions = new_data.get_new_objects(Action)
    responsibilty_actions = new_data.get_new_objects(RoleNodeResponsibilityAction)
    action = responsibilty_actions.select_related('role_node').get(action='role_node_responsibility_removed')
    assert action.role_node.slug_path == '/project/subs/role/manager/'
    assert action.node_responsibility_id == responsibility.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, responsibilities_removed=1)
    # проверим, что у frodo пропали права
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent.parent)) is False

    # теперь восстановим ответственность, на всякий случай с другим флагом notify
    Action.objects.all().delete()
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': False,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeResponsibilityAction, 1), (NodeResponsibility, 0)):
            sync_role_nodes(complex_system)
    node = RoleNode.objects.get(slug='manager')
    responsibility = NodeResponsibility.objects.select_related('node').get(user=frodo, node=node, is_active=True)
    assert responsibility.is_active is True
    assert responsibility.notify is False
    assert responsibility.user_id == arda_users.frodo.id
    actions = new_data.get_new_objects(Action)
    responsibilty_actions = new_data.get_new_objects(RoleNodeResponsibilityAction)
    action = responsibilty_actions.select_related('role_node').get(action='role_node_responsibility_created')
    assert action.node_responsibility_id == responsibility.id
    assert action.role_node.slug_path == '/project/subs/role/manager/'
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, responsibilities_created=1)
    # проверим, что права появились вновь
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node)) is True
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent.parent)) is False


def test_delete_responsibility_without_internalrole(complex_system, arda_users):
    """Проверим, что если не существует внутренней роли, связанной с узлом, на который выдана ответственность,
    ответственность можно удалить"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }]
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeResponsibilityAction, 1), (NodeResponsibility, 1),
                                   (InternalRole, 1),
                                   (InternalRoleUserObjectPermission, 4)):
            sync_role_nodes(complex_system)

    Action.objects.all().delete()
    # удалим InternalRole, а затем проверим, что синхронизация прошла успешно
    assert InternalRole.objects.count() == 1
    InternalRole.objects.all().delete()

    tree = complex_system.plugin.get_info()
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeResponsibilityAction, 1), (NodeResponsibility, 0)):
            sync_role_nodes(complex_system)
    responsibility = NodeResponsibility.objects.select_related('node__parent__parent').get()
    assert responsibility.is_active is False
    end_sync = Action.objects.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_sync.data['status'] == 0
    # проверим, что у frodo пропали права
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent)) is False
    assert bool(can_request_role(frodo, arda_users.varda, complex_system, responsibility.node.parent.parent)) is False


def test_responsibility_of_inactive_user(complex_system, arda_users):
    """Проверим кейс, когда кого-то увольняют во время синхронизации"""

    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.save()
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }, {
        'username': 'legolas',
        'notify': True,
    }]
    with mock_tree(complex_system, tree):
        # with mock.patch('idm.core.fetchers.RoleNodeFetcher.filter_active_users') as filter_users:
        #     filter_users.side_effect = lambda usernames: usernames
        sync_role_nodes(complex_system)
    assert NodeResponsibility.objects.count() == 1
    responsibility = NodeResponsibility.objects.get()
    assert responsibility.user_id == arda_users.legolas.id
    assert responsibility.notify is True


def test_intrasearch_push_on_visibility_change(complex_system, arda_users):
    """Проверяем что переменная need_intrasearch_push_method выставляется при изменении visibility"""

    import mock
    from idm.utils import http
    from django.conf import settings

    class MockResponseOk:
        def json(self):
            return dict(status='ok')

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['visibility'] = False
    role_node = complex_system.nodes.get(slug='manager')
    settings.IDM_SEND_INTRASEARCH_PUSHES = True
    with mock_tree(complex_system, tree):
        with mock.patch.object(http, 'delete') as mock_foo:
            # mock http request to intrasearch for request failure
            mock_foo.side_effect = http.RequestException

            sync_role_nodes(complex_system)
            # and expect `need` flag is still on
            role_node.refresh_from_db()
            assert role_node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE


def test_new_field(complex_system):
    """Проверим добавление нового поля"""

    tree = complex_system.plugin.get_info()
    tree['fields'] = tree['fields'] + [{
        'slug': 'new_field',
        'name': {
            'ru': 'Новое поле',
            'en': 'New field',
        }
    }]
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleField, 1)) as new_data:
        sync_role_nodes(complex_system)
    actions = new_data.get_new_objects(Action)
    fields = new_data.get_new_objects(RoleField)
    field = fields[0]
    assert field.is_required is False
    assert field.name == 'Новое поле'
    assert field.name_en == 'New field'
    assert field.is_active is True
    field.fetch_node()
    assert field.node.slug_path == '/project/'
    assert field.slug == 'new_field'
    assert field.type == FIELD_TYPE.CHARFIELD
    action = actions.get(action='role_node_field_created')
    assert action.role_node_id == field.node_id
    assert action.role_field_id == field.id
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, fields_created=1)


def test_delete_field(complex_system):
    tree = complex_system.plugin.get_info()
    tree['fields'] = tree['fields'][:-1]
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleField, 0)) as new_data:
        sync_role_nodes(complex_system)
    actions = new_data.get_new_objects(Action)
    action = actions.select_related('role_field', 'role_node').get(action='role_node_field_removed')
    field = action.role_field
    node = action.role_node
    assert node.slug_path == '/project/'
    assert field.is_active is False
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, fields_removed=1)


def test_rename_field(complex_system):
    tree = complex_system.plugin.get_info()
    tree['fields'][0]['name'] = 'Другое поле'
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleField, 0)) as new_data:
        sync_role_nodes(complex_system)
    actions = new_data.get_new_objects(Action)
    action = actions.select_related('role_node', 'role_field').get(action='role_node_field_changed')
    node = action.role_node
    assert node.slug_path == '/project/'
    field = action.role_field
    assert field.is_active
    assert field.name == 'Другое поле'
    assert action.data['field_data'] == {
        'is_required': True,
        'name': 'Другое поле',
        'name_en': 'Другое поле',
        'type': 'charfield',
        'slug': 'field_1',
        'options': None,
        'dependencies': None,
    }
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, fields_changed=1)


def test_change_field_slug(complex_system):
    """Проверим, что изменение slug-а эквивалентно удалению поля и добавлению нового"""
    tree = complex_system.plugin.get_info()
    tree['fields'][0]['slug'] = 'another'
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 4), (RoleField, 1)) as new_data:
        sync_role_nodes(complex_system)
    actions = new_data.get_new_objects(Action)
    assert {action.action for action in actions} == {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_FIELD_CREATED,
        ACTION.ROLE_NODE_FIELD_REMOVED,
    }
    action = actions.select_related('role_field').get(action='role_node_field_created')
    assert action.role_field.slug == 'another'
    assert action.role_field.is_active is True
    action = actions.select_related('role_field').get(action='role_node_field_removed')
    assert action.role_field.slug == 'field_1'
    assert action.role_field.is_active is False
    end_sync = actions.get(action=ACTION.ROLE_TREE_SYNCED)
    assert_report(end_sync.data, fields_removed=1, fields_created=1)


def test_add_is_exclusive(complex_system, arda_users):
    node = complex_system.nodes.get(slug='auditor')
    assert not node.is_exclusive

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['auditor']['is_exclusive'] = True

    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3)):
        sync_role_nodes(complex_system)

    node.refresh_from_db()
    assert node.is_exclusive

    del tree['roles']['values']['rules']['roles']['values']['auditor']['is_exclusive']

    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3)):
        sync_role_nodes(complex_system)

    node.refresh_from_db()
    assert not node.is_exclusive


def test_add_option_for_choice_field(complex_system, arda_users):
    """Проверим добавление опции к полю"""
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'year',
        'name': {
            'ru': 'Год для возвращения',
            'en': 'Year to return back to',
        },
        'required': True,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'choices': [
                {
                    'value': '2011',
                    'name': {
                        'en': 'Two thousand eleven',
                        'ru': 'Две тысячи одиннадцатый'
                    },
                }
            ]
        }
    }]
    # 3 Action-а: старт синхронизации, добавление поля, завершение синхронизации
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleField, 1)):
        sync_role_nodes(complex_system)
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'year',
        'name': {
            'ru': 'Год для возвращения',
            'en': 'Year to return back to',
        },
        'required': True,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'choices': [
                {
                    'value': '2011',
                    'name': {
                        'en': 'Two thousand eleven',
                        'ru': 'Две тысячи одиннадцатый'
                    },
                },
                {
                    'value': '2007',
                    'name': 'Two thousand and seven',
                }
            ]
        }
    }]
    # 3: старт синхронизации, изменение поля, завершение синхронизации
    with mock_tree(complex_system, tree), CountIncreasedContext((Action, 3), (RoleField, 0)) as new_data:
        sync_role_nodes(complex_system)
    field = RoleField.objects.get(slug='year')
    assert len(field.options['choices']) == 2
    assert field.options['choices'] == [{
        'value': '2011',
        'name': {
            'en': 'Two thousand eleven',
            'ru': 'Две тысячи одиннадцатый'
        },
    }, {
        'value': '2007',
        'name': {
            'en': 'Two thousand and seven',
            'ru': 'Two thousand and seven'
        },
    }]
    actions = new_data.get_new_objects(Action)
    change_action = actions.get(action='role_node_field_changed')
    expected_data = {
        'name': 'Год для возвращения',
        'name_en': 'Year to return back to',
        'slug': 'year',
        'is_required': True,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'choices': [
                {
                    'value': '2011',
                    'name': {
                        'en': 'Two thousand eleven',
                        'ru': 'Две тысячи одиннадцатый'
                    }
                },
                {
                    'value': '2007',
                    'name': {
                        'en': 'Two thousand and seven',
                        'ru': 'Two thousand and seven',
                    }
                }
            ]
        },
        'dependencies': None,
    }
    assert change_action.data['field_data'] == expected_data
    expected_diff = {
        'options': [
            {
                'widget': 'radio',
                'choices': [
                    {
                        'value': '2011',
                        'name': {
                            'en': 'Two thousand eleven',
                            'ru': 'Две тысячи одиннадцатый'
                        }
                    }
                ]
            },
            {
                'widget': 'radio',
                'choices': [
                    {
                        'value': '2011',
                        'name': {
                            'en': 'Two thousand eleven',
                            'ru': 'Две тысячи одиннадцатый'
                        }
                    },
                    {
                        'value': '2007',
                        'name': {
                            'en': 'Two thousand and seven',
                            'ru': 'Two thousand and seven',
                        }
                    }
                ]
            }
        ]
    }
    assert change_action.data['diff'] == expected_diff


def test_change_field_type():
    old_field_type = FIELD_TYPE.CHARFIELD
    new_field_type = FIELD_TYPE.SUGGEST
    suggest_name = 'metrika_counter'
    field_slug = random_slug()
    field_name = random_slug()
    role_slug = random_slug()
    role_tree = {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Role',
            'values': {
                role_slug: {
                    'name': random_slug(),
                    'fields': [{
                        'slug': field_slug,
                        'name': field_name,
                        'type': old_field_type,
                    }]
                }
            }
        }
    }

    system = create_system(role_tree=role_tree)
    role_node: RoleNode = RoleNode.objects.get_node_by_data(system, {'role': role_slug})
    assert role_node.is_active
    role_field: RoleField = role_node.fields.get()
    assert role_field.is_active is True
    assert role_field.slug == field_slug
    assert role_field.name == field_name
    assert role_field.type == old_field_type

    role_tree['roles']['values'][role_slug]['fields'] = [{
        'slug': field_slug,
        'name': field_name,
        'type': new_field_type,
        'options': {'suggest': suggest_name},
    }]
    set_roles_tree(system, role_tree)
    role_node.refresh_from_db()
    assert role_node.is_active
    old_role_field: RoleField = role_node.fields.filter(is_active=False).get()
    assert old_role_field == role_field
    assert old_role_field.is_active is False
    assert old_role_field.type == old_field_type
    new_role_field: RoleField = role_node.fields.filter(is_active=True).get()
    assert new_role_field.is_active is True
    assert new_role_field.type == new_field_type
    assert new_role_field.name == field_name
    assert new_role_field.slug == field_slug


def test_short_name(complex_system, pt1_system):
    """Проверим метод по выдаче короткого имени"""
    node = RoleNode.objects.get(slug='manager', system=complex_system)
    assert node.get_short_name() == 'Подписки / Менеджер'
    node = RoleNode.objects.get(slug='manager', parent__parent__slug='subproj1', system=pt1_system)
    assert node.get_short_name() == 'Проект 3 / Подпроект 1 / Менеджер'


def test_ctt_hook(complex_system):
    test_node = RoleNode.objects.get(slug_path='/project/subs/role/developer/')
    assert ctt_data_is_consistent(test_node)


def test_node_movement(arda_users, complex_system):
    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'unique_id': 'developer',
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    role = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'}, state='granted')
    node_from = role.node
    assert node_from.state == 'active'

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['developer']
    tree['roles']['values']['rules']['roles']['values']['dev'] = {
        'name': 'Программист',
        'unique_id': 'developer',
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    # Проверим корректность простановки хеша
    complex_system = refresh(complex_system)
    old_hashes = dict(complex_system.nodes.active().values_list('slug_path', 'hash'))
    complex_system.fetch_root_role_node()
    complex_system.root_role_node.rehash()
    new_hashes = dict(complex_system.nodes.active().values_list('slug_path', 'hash'))
    assert old_hashes == new_hashes

    node_to = RoleNode.objects.get_node_by_value_path(complex_system, '/rules/dev/')
    node_from = refresh(node_from)
    assert node_from.state == 'deprived'
    assert node_from.roles.count() == 0
    assert node_from.moved_to == node_to
    assert node_to.moved_from == node_from
    assert node_to.state == 'active'
    assert node_to.roles.count() == 1
    role = refresh(role)
    assert role.node == node_to
    assert role.state == 'granted'

    sync_key = node_from.actions.select_related('parent').get(action='role_node_deleted').parent
    end_action = sync_key.children.get(action=ACTION.ROLE_TREE_SYNCED)
    assert end_action.data['report']['nodes_deleted'] == 1


@pytest.mark.parametrize('with_approvers', [True, False])
def test_node_movement_reruns_workflow(idm_robot, arda_users, simple_system, complex_system,
                                       with_approvers, ad_system):
    set_workflow(complex_system, dedent('''
        if role['role'] == 'dev':
            no_email = False
            approvers = [approver('legolas')]
            email_cc = ['sauron@yandex-team.ru']
            ad_groups = ["OU=group1"]
            ref_roles = [{'system': '%(system)s', 'role_data': {'role': 'manager'}, 'role_fields': {'login': scope}}]
        else:
            approvers = [%(user)s]
            no_email = True
        ''' % {
        'system': simple_system.slug,
        'user': '"legolas"' if with_approvers else ''
    }))

    frodo = arda_users.frodo

    tree = ad_system.plugin.get_info()
    with mock_tree(ad_system, tree):
        sync_role_nodes(ad_system, requester=frodo)

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'unique_id': 'developer',
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    RoleField.objects.filter(node__system=complex_system).delete()
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    node_from = role.node
    assert node_from.state == 'active'
    assert node_from.slug_path == '/project/subs/role/developer/'
    assert role.no_email is True
    assert role.refs.count() == 0
    assert role.ref_roles == []

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['developer']
    tree['roles']['values']['rules']['roles']['values']['dev'] = {
        'name': 'Программист',
        'unique_id': 'developer',
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    role = refresh(role)
    assert role.node.slug_path == '/project/rules/role/dev/'
    assert role.no_email is False
    assert role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'sauron@yandex-team.ru', 'pass_to_personal': False}]
    }
    assert role.ad_groups == []
    expected_ref_roles = [
        {
            'system': 'simple',
            'role_data': {'role': 'manager'},
            'role_fields': {'login': '/rules/'}
        },
        {
            'system': 'ad_system',
            'role_data': {
                'type': 'roles_in_groups',
                'ad_group': 'OU=group1',
                'group_roles': 'member',
            },
        },
    ]
    assert role.ref_roles == expected_ref_roles
    assert role.actions.filter(action='rerun_workflow').count() == 1
    action = role.actions.get(action='rerun_workflow')
    assert action.user_id == frodo.id
    assert action.requester_id == idm_robot.id
    assert action.error == ''
    assert action.system_id == complex_system.id
    assert action.data == {
        'diff': {
            'email_cc': [
                {},
                {
                    'granted': [{'email': 'sauron@yandex-team.ru', 'lang': 'ru', 'pass_to_personal': False}]
                }
            ],
            'no_email': [
                True, False
            ],
            'ref_roles': [
                [],
                expected_ref_roles,
            ]
        }
    }

    if with_approvers:
        assert role.state == 'requested'
        assert role.refs.count() == 0
    else:
        assert role.state == 'granted'
        assert role.refs.count() == 2
        ref = list(role.refs.filter())[1]
        assert ref.state == 'granted'
        assert ref.fields_data == {'login': '/rules/'}


def test_node_moved_to_not_leaf(arda_users, complex_system):
    """Проверим случай, когда нелистовой узел становится листовым"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'unique_id': 'developer',
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    role = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'}, state='granted')

    tree = complex_system.plugin.get_info()
    del tree['roles']['values']['subs']['roles']['values']['developer']
    tree['roles']['values']['cert'] = {
        'name': 'Сертификатор',
        'unique_id': 'developer',
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'developer': 'Разработчик'
            }
        }
    }
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system, requester=frodo)

    role = refresh(role)
    assert role.node.slug_path == '/project/subs/role/developer/'
    assert role.state == 'deprived'
    assert_action_chain(role, ['rerun_workflow', 'deprive', 'first_remove_role_push', 'remove'])
    deprive_action = role.actions.get(action='deprive')
    assert deprive_action.comment == 'Роль больше не поддерживается системой'


def test_node_revives_after_delete_from_api(client, simple_system, arda_users):
    """
    Проверим, что если узел удалили через api, но он не исчез из ручки info,
    он будет восстановлен при следующей синхронизации
    """
    info = simple_system.plugin.get_info()
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=simple_system)
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='rolenodes',
                  slug_path='simple/role/manager')

    assert simple_system.nodes.active().filter(slug='manager').count() == 1

    client.json.delete(url)

    assert simple_system.nodes.active().filter(slug='manager').count() == 0

    with mock_tree(simple_system, info):
        simple_system.synchronize(force_update=True)

    assert simple_system.nodes.active().filter(slug='manager').count() == 1


@pytest.mark.parametrize('add_child', [True, False])
def test_node_pipeline_broken_closure_tree(complex_system, add_child):
    """
    Ломаем closure_tree для поддерева дерева системы, делаем родительскую ноду superdirty,
    прогоняем node_pipeline, проверяем восстановление дерева. В случае add_child делаем
    superdirty еще одну дочернюю ноду.
    """

    def get_descendants_list(node):
        descendants = list(node.children.all())
        for child in node.children.all():
            descendants.extend(get_descendants_list(child))
        return descendants

    node = RoleNode.objects.get(slug_path='/')

    # Ломаем closure_tree
    for closure_child in node.get_descendants():
        closure_child.rolenodeclosure_parents.filter(parent=node).delete()
    assert node.get_descendants().count() == 0

    node.relocation_state = RELOCATION_STATE.SUPERDIRTY
    node.save(update_fields=['relocation_state'])
    if add_child:
        child = node.children.first()
        child.relocation_state = RELOCATION_STATE.SUPERDIRTY
        child.save(update_fields=['relocation_state'])
    pipeline = node_pipeline.NodePipeline(complex_system)

    pipeline.run()
    assert set(get_descendants_list(node)) == set(node.get_descendants())


def test_move_node_and_create_with_same_slug(simple_system):
    def create_pool_roles_info(pool_unique_id: str) -> dict:
        return {
            'unique_id': pool_unique_id,
            'roles': {
                'slug': 'pool_roles',
                'unique_id': f'{pool_unique_id}_pool_roles',
                'values': {
                    'user': {
                        'unique_id': f'{pool_unique_id}_user',
                    },
                    'admin': {
                        'unique_id': f'{pool_unique_id}_admin',
                    },
                },
            },
        }

    before = {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'pool_1': create_pool_roles_info('pool_1_unique_id')
            },
        },
    }

    after = {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'renamed_pool_1': create_pool_roles_info('pool_1_unique_id'),  # меняется слаг
                'pool_1': create_pool_roles_info('new_pool_1_unique_id'),
                # создается с тем же слагом и другим unique_id
            },
        },
    }

    simple_system.root_role_node.get_descendants().delete()

    with mock_tree(simple_system, before):
        simple_system.synchronize(force_update=True)

    expected_slug_paths = {
        '/',
        '/role/',
        '/role/pool_1/',
        '/role/pool_1/pool_roles/',
        '/role/pool_1/pool_roles/admin/', '/role/pool_1/pool_roles/user/',
    }
    slug_paths = set(simple_system.nodes.active().values_list('slug_path', flat=True))
    assert slug_paths == expected_slug_paths

    with mock_tree(simple_system, after):
        simple_system.synchronize(force_update=True)

    expected_slug_paths = {
        '/',
        '/role/',
        '/role/pool_1/', '/role/renamed_pool_1/',
        '/role/pool_1/pool_roles/', '/role/renamed_pool_1/pool_roles/',
        '/role/pool_1/pool_roles/admin/', '/role/pool_1/pool_roles/user/', '/role/renamed_pool_1/pool_roles/admin/',
        '/role/renamed_pool_1/pool_roles/user/'
    }
    slug_paths = set(simple_system.nodes.active().values_list('slug_path', flat=True))

    assert slug_paths == expected_slug_paths


@pytest.mark.parametrize('with_aliases', [True, False])
@waffle.testutils.override_switch('rolenode_full_text_search', active=True)
def test_queryset_text_search(with_aliases: bool):
    alpha, beta, gamma, delta, omega, teta, eta, yota, psi = [random_slug() for _ in range(9)]
    system = create_system()
    ref_params = {'parent': system.root_role_node, 'system': system}
    node_a, node_b, node_c = nodes = [
        RoleNode(slug=f'{omega}-{alpha}', name=f'{random_slug()}-{teta}', **ref_params),
        RoleNode(slug=f'{omega}-{beta}', name=f'{random_slug()}-{alpha}', name_en=f'{delta}-{gamma}', **ref_params),
        RoleNode(slug=f'{omega}-{gamma}', name_en=f'{random_slug()}-{eta}', **ref_params),
    ]
    for node in nodes:
        node.save()
    node_a.aliases.create(name=f'{yota}-{gamma}', name_en=psi)  # по gamma не найдется, в алиасах поиски по префиксу
    node_c.aliases.create(name=f'{yota}-{gamma}', name_en=alpha)

    for query, results in {
            alpha: ({node_a, node_b}, {node_c}),
            beta: ({node_b}, set()),
            gamma: ({node_b, node_c}, set()),
            delta: ({node_b}, set()),
            omega: ({node_a, node_b, node_c}, set()),
            teta: ({node_a}, set()),
            eta: ({node_c}, set()),
            yota: (set(), {node_a, node_c}),
            psi: (set(), {node_a}),
            f'{omega}-{beta}': ({node_b}, set()),
            f'{yota}-{gamma}': (set(), {node_a, node_c}),
    }.items():
        always, with_aliases_only = results
        assert set(
            RoleNode.objects
               .filter(parent=system.root_role_node)
               .text_search(query, with_aliases=with_aliases)
        ) == (with_aliases and (always | with_aliases_only) or always)
