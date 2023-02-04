# coding: utf-8


import pytest

from django.utils import timezone
from django.conf import settings

from idm.core.constants.action import ACTION
from idm.core.models import RoleNode, RoleNodeSet, Action
from idm.tests.utils import mock_tree, CountIncreasedContext, assert_stable_hash, sync_role_nodes, refresh, days_from_now

pytestmark = pytest.mark.django_db


def test_new_rolenodeset(complex_system):
    """Проверка добавления новой группы узлов"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['set'] = 'test1'
    rules = RoleNode.objects.get(slug='rules')
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleNodeSet, 1)) as new_data:
            sync_role_nodes(complex_system)

    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'test1'
    assert nodeset.system_id == complex_system.id
    assert nodeset.is_active
    assert nodeset.nodes.count() == 1
    assert nodeset.nodes.get() == rules
    assert nodeset.name == 'IDM'
    assert nodeset.name_en == 'IDM'

    actions = new_data.get_new_objects(Action)
    expected_actions = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.ROLE_NODE_CHANGED,
    }
    assert {action.action for action in actions} == expected_actions
    action = Action.objects.get(action='role_node_changed')
    assert action.role_node_id == rules.id
    assert action.system_id == complex_system.id
    assert action.data['diff'] == {
        'set': ['', 'test1']
    }

    # повторная синхронизация ничего не изменит. хеши совпадают
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['set'] = 'test1'
    with mock_tree(complex_system, tree):
        assert_stable_hash(complex_system)

    # предыдущие операции затирают структуру данных, поэтому нужно повторить манипуляции
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['set'] = 'test1'
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 2), (RoleNodeSet, 0)) as sync_data:
            sync_role_nodes(complex_system)
    expected = {'role_tree_started_sync', 'role_tree_synced'}
    assert {action.action for action in sync_data.get_new_objects(Action)} == expected


def test_new_node_with_new_nodeset(complex_system):
    """Проверим, что если добавляется новая нода с set, то создаётся nodeset"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'developer'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 1)) as new_data:
            sync_role_nodes(complex_system)

    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'developer'
    assert nodeset.system_id == complex_system.id
    assert nodeset.is_active
    assert nodeset.nodes.count() == 1
    assert nodeset.nodes.get().slug_path == '/project/rules/role/developer/'
    assert nodeset.name == 'Разработчик'
    assert nodeset.name_en == 'Разработчик'

    actions = new_data.get_new_objects(Action)
    expected_actions = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_CREATED,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.MASS_ACTION,
    }
    assert {action.action for action in actions} == expected_actions
    action = Action.objects.select_related('role_node').get(action='role_node_created')
    assert action.role_node.slug_path == '/project/rules/role/developer/'
    assert action.system_id == complex_system.id


def test_new_node_and_change_others(complex_system):
    """Проверим, что всё ок, если добавляется новая нода, а у некоторых проставляется set"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['set'] = 'test1'
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'test1'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 4 + 1), (RoleNodeSet, 1)) as new_data:
            sync_role_nodes(complex_system)

    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'test1'
    assert nodeset.system_id == complex_system.id
    assert nodeset.is_active
    assert nodeset.nodes.count() == 2
    # сначала изменения на верхнем уровне, потом изменения на нижнем уровне, без определённого порядка по типу.
    # сначала мы создадим новый set и привяжем к нему старый узел,
    # а потом добавим новый узел, у которого другое название и изменим название узла.
    # В целом это скорее недетерминированное поведение, но это ответственность системы:
    # все узлы с одинаковым set должны называться одинаково
    assert nodeset.name == 'Разработчик'
    assert nodeset.name_en == 'Разработчик'
    expected_actions = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_CREATED,
        ACTION.ROLE_NODE_CHANGED,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.MASS_ACTION,
    }
    assert {action.action for action in new_data.get_new_objects(Action)} == expected_actions


def test_deprive_node_deactivates_nodeset(complex_system):
    """Проверим, что при удалении узла, если это был последний узел, ссылавшийся на nodeset,
    то nodeset меняет флаг is_active"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'test1'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 1)):
            sync_role_nodes(complex_system)
    assert RoleNodeSet.objects.get().is_active

    tree = complex_system.plugin.get_info()
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleNodeSet, 0)) as new_data:
            sync_role_nodes(complex_system)
    assert RoleNodeSet.objects.get().is_active

    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        with mock_tree(complex_system, tree):
            with CountIncreasedContext((Action, 4), (RoleNodeSet, 0)) as new_data:
                sync_role_nodes(complex_system)
    assert not RoleNodeSet.objects.get().is_active


def test_unsetting_node_deactivates_nodeset(complex_system):
    """Проверим, что если убрать у узла свойство set, если это был последний узел, ссылавшийся на nodeset,
    то nodeset меняет флаг is_active"""

    expected_actions = {
        ACTION.ROLE_TREE_STARTED_SYNC,
        ACTION.ROLE_NODE_CHANGED,
        ACTION.ROLE_TREE_SYNCED,
        ACTION.MASS_ACTION,
    }

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'test1'
    }
    tree['roles']['values']['subs']['roles']['values']['manager'] = {
        'name': 'Менеджер',
        'set': 'test1'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 4 + 1), (RoleNodeSet, 1)) as new_data:
            sync_role_nodes(complex_system)

    assert {action.action for action in new_data.get_new_objects(Action)} == expected_actions
    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'test1'
    assert nodeset.nodes.count() == 2
    assert nodeset.is_active is True
    assert nodeset.system_id == complex_system.id

    # уберём одну ссылку на test1
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'test1'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 0)) as new_data:
            sync_role_nodes(complex_system)

    assert {action.action for action in new_data.get_new_objects(Action)} == expected_actions

    nodeset = refresh(nodeset)
    assert nodeset.nodes.count() == 1
    assert nodeset.is_active is True

    # теперь убираем последнюю ссылку
    with mock_tree(complex_system, complex_system.plugin.get_info()):
        with CountIncreasedContext((Action, 3), (RoleNodeSet, 0)) as new_data:
            sync_role_nodes(complex_system)

    nodeset = refresh(nodeset)
    assert RoleNodeSet.objects.count() == 1
    assert nodeset.is_active is False
    assert nodeset.nodes.count() == 0
    assert {action.action for action in new_data.get_new_objects(Action)} == expected_actions - {ACTION.MASS_ACTION}


def test_restore_nodeset(complex_system):
    """Проверка, что если сначала удалить узел с nodeset, а потом восстановить его, то
    RoleNodeSet восстановится, а не создастся новый"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'developer'
    }
    # создаём узел
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 1)):
            sync_role_nodes(complex_system)
    # помечаем для удаления и удаляем узел
    tree = complex_system.plugin.get_info()
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3), (RoleNodeSet, 0)):
            sync_role_nodes(complex_system)
    with days_from_now(settings.IDM_DEPRIVING_NODE_TTL + 1):
        with mock_tree(complex_system, tree):
            with CountIncreasedContext((Action, 4), (RoleNodeSet, 0)):
                sync_role_nodes(complex_system)
    # восстанавливаем узел с новым именем
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Программист',
        'set': 'developer'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 0)):
            sync_role_nodes(complex_system)

    assert RoleNodeSet.objects.count() == 1
    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'developer'
    assert nodeset.system_id == complex_system.id
    assert nodeset.is_active is True
    assert nodeset.nodes.count() == 2
    assert nodeset.name == 'Программист'
    assert nodeset.name_en == 'Программист'
    # один узел активен, а другой нет
    assert {node.state for node in nodeset.nodes.all()} == {'active', 'deprived'}


def test_rename_nodeset(complex_system):
    """Убедимся, что набор узлов переименовывается при переименовывании узла"""

    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'developer'
    }
    # создаём узел
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 1)):
            sync_role_nodes(complex_system)
    # переименовываем узел
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Программист',
        'set': 'developer'
    }
    with mock_tree(complex_system, tree):
        with CountIncreasedContext((Action, 3 + 1), (RoleNodeSet, 0)):
            sync_role_nodes(complex_system)

    assert RoleNodeSet.objects.count() == 1
    nodeset = RoleNodeSet.objects.get()
    assert nodeset.set_id == 'developer'
    assert nodeset.system_id == complex_system.id
    assert nodeset.is_active is True
    assert nodeset.nodes.count() == 1
    assert nodeset.name == 'Программист'
    assert nodeset.name_en == 'Программист'
