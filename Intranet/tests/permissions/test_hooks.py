# coding: utf-8

import json
import pytest

from django.conf import settings
from django.urls import reverse
from django_idm_api.exceptions import AccessDenied, UserNotFound

from idm.core.models import InternalRole, RoleNode, NodeResponsibility
from idm.tests.utils import create_user, add_perms_by_role, get_all_roles
from idm.users.models import User


pytestmark = [
    pytest.mark.django_db,
]


@pytest.fixture
def self_simple_system(simple_system):
    simple_system.plugin_type = 'idm.core.plugins.generic_self.Plugin'
    simple_system._plugin = None
    simple_system.save()
    return simple_system


def test_hooks_info(self_simple_system):
    """Проверяем дерево ролей
    """

    tree = self_simple_system.plugin.info()
    assert tree['code'] == 0
    assert set(tree['roles']['values'].keys()) == {'common', 'system'}
    assert (set(tree['roles']['values']['common']['roles']['values'].keys())
            == set(settings.IDM_COMMON_ROLES_PERMISSIONS.keys()))

    assert set(tree['roles']['name'].keys()) == {'ru', 'en'}
    assert set(tree['roles']['values']['common']['name'].keys()) == {'ru', 'en'}
    assert set(tree['roles']['values']['common']['help'].keys()) == {'ru', 'en'}
    assert set(tree['roles']['values']['common']['roles']['name'].keys()) == {'ru', 'en'}
    assert set(tree['roles']['values']['common']['roles']['values']['developer']['name'].keys()) == {'ru', 'en'}

    tree = self_simple_system.plugin.info()

    assert set(tree['roles']['values']['system']['roles']['values'].keys()) == {'simple'}

    system_tree = tree['roles']['values']['system']['roles']['values']['simple']
    assert set(system_tree['roles']['values'].keys()) == set(settings.IDM_SYSTEM_ROLES_PERMISSIONS.keys())

    assert set(system_tree['name'].keys()) == {'ru', 'en'}
    assert set(system_tree['roles']['values']['roles_manage']['name'].keys()) == {'ru', 'en'}
    assert 'aliases' in system_tree['roles']['values']['roles_manage']


def test_hooks_add_role(self_simple_system):
    """Проверяем выдачу роли
    """

    frodo = create_user('frodo')

    result = self_simple_system.plugin.add_role(
        'frodo',
        {'group': 'common', 'role': 'developer'},
        {},
        subject_type='user',
    )
    assert result == {'code': 0}

    assert frodo.has_internal_role('developer')

    result = self_simple_system.plugin.add_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'roles_manage'},
        {},
        subject_type='user',
    )
    assert result == {'code': 0, 'data': {'scope': '/'}}

    assert frodo.has_internal_role('developer')
    assert frodo.has_internal_role('roles_manage', self_simple_system)

    result = self_simple_system.plugin.add_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'roles_manage'},
        {'scope': '/admin/'},
        subject_type='user',
    )
    assert result == {'code': 0, 'data': {'scope': '/admin/'}}

    assert frodo.has_internal_role('developer')
    assert frodo.has_internal_role('roles_manage', self_simple_system)
    assert frodo.has_internal_role('roles_manage', self_simple_system, '/admin/')


def test_hooks_add_role_processes_tvm_apps(self_simple_system):
    """Проверяем, что параметр subject_type не игнорируется и валидируется
    """

    frodo = create_user('frodo')
    the_ring = create_user('the-ring', type='tvm_app')

    result = self_simple_system.plugin.add_role(
        'frodo',
        {'group': 'common', 'role': 'developer'},
        {},
        subject_type='user',
    )
    assert result == {'code': 0}
    result = self_simple_system.plugin.add_role(
        'the-ring',
        {'group': 'common', 'role': 'auditor'},
        {},
        subject_type='tvm_app',
    )
    assert result == {'code': 0}

    assert frodo.has_internal_role('developer')
    assert the_ring.has_internal_role('auditor')

    with pytest.raises(UserNotFound):
        self_simple_system.plugin.add_role(
            'frodo',
            {'group': 'common', 'role': 'developer'},
            {},
            subject_type='tvm_app',
        )
    with pytest.raises(UserNotFound):
        self_simple_system.plugin.add_role(
            'the-ring',
            {'group': 'common', 'role': 'auditor'},
            {},
            subject_type='user',
        )


def test_hooks_remove_role(self_simple_system):
    """Проверяем отзыв роли
    """

    frodo = create_user('frodo')

    add_perms_by_role('developer', frodo)
    add_perms_by_role('roles_manage', frodo, self_simple_system)
    add_perms_by_role('roles_manage', frodo, self_simple_system, '/admin/')

    assert frodo.has_internal_role('developer')
    assert frodo.has_internal_role('roles_manage', self_simple_system)
    assert frodo.has_internal_role('roles_manage', self_simple_system, '/admin/')

    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'common', 'role': 'developer'},
        {},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    assert not frodo.has_internal_role('developer')
    assert frodo.has_internal_role('roles_manage', self_simple_system)
    assert frodo.has_internal_role('roles_manage', self_simple_system, '/admin/')

    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'roles_manage'},
        {},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    assert not frodo.has_internal_role('roles_manage', self_simple_system)
    assert frodo.has_internal_role('roles_manage', self_simple_system, '/admin/')

    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'roles_manage'},
        {'scope': '/admin/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    assert not frodo.has_internal_role('roles_manage', self_simple_system, '/admin/')

    # второй раз удалем такую же роль
    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'roles_manage'},
        {'scope': '/admin/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    # удаляем несуществующие роли

    # плохая роль
    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'system', 'system_on': self_simple_system.slug, 'role': 'bar'},
        {'scope': '/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    # плохая система
    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'system', 'system_on': 'foo', 'role': 'roles_manage'},
        {'scope': '/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    # плохая группа ролей
    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'foo', 'system_on': self_simple_system.slug, 'foo': 'bar'},
        {'scope': '/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}

    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'foo': 'bar'},
        {'scope': '/'},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}


def test_hooks_remove_role_processes_tvm_apps(self_simple_system):
    """Проверяем, что параметр subject_type не игнорируется и валидируется
    """

    frodo = create_user('frodo')
    the_ring = create_user('the-ring', type='tvm_app')

    add_perms_by_role('developer', frodo)
    add_perms_by_role('helpdesk', frodo)
    add_perms_by_role('auditor', the_ring)

    assert frodo.has_internal_role('developer')
    assert the_ring.has_internal_role('auditor')

    self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'common', 'role': 'developer'},
        {},
        False,
        subject_type='tvm_app',
    )
    assert frodo.has_internal_role('developer')  # эксепшна нет, но мы тихо ничего не делаем
    self_simple_system.plugin.remove_role(
        'the-ring',
        {'group': 'common', 'role': 'developer'},
        {},
        False,
        subject_type='user',
    )
    assert the_ring.has_internal_role('auditor')  # эксепшна нет, но мы тихо ничего не делаем

    result = self_simple_system.plugin.remove_role(
        'frodo',
        {'group': 'common', 'role': 'developer'},
        {},
        False,
        subject_type='user',
    )
    assert result == {'code': 0}
    result = self_simple_system.plugin.remove_role(
        'the-ring',
        {'group': 'common', 'role': 'auditor'},
        {},
        False,
        subject_type='tvm_app',
    )
    assert result == {'code': 0}

    assert not frodo.has_internal_role('developer')
    assert not the_ring.has_internal_role('auditor')


def get_user_roles(roles, login, subject_type):
    result = []
    for user_role in roles:
        if user_role['login'] != login or user_role['subject_type'] != subject_type:
            continue
        role = {'role': user_role['path']}
        if 'fields' in user_role:
            role.update(user_role['fields'])
        result.append(role)
    return result


def test_hooks_get_all_roles(simple_system, client):
    """Проверяем выдачу списка ролей
    """
    frodo = create_user('frodo')
    sam = create_user('sam')
    the_ring = create_user('the-ring', type='tvm_app')
    client.login('frodo')

    url = reverse('client-api:get-all-roles')
    response = client.json.get(url, with_idm_credentials=True)
    data = response.json()
    assert data['code'] == 1
    assert data['error'] == 'get-all-roles is not implemented'

    url = reverse('client-api:get-roles')
    roles = get_all_roles(client, url)
    assert {(role['login'], role['subject_type']) for role in roles} == set()

    add_perms_by_role('developer', frodo)
    add_perms_by_role('auditor', the_ring)
    add_perms_by_role('roles_manage', frodo, simple_system)
    add_perms_by_role('roles_manage', sam, simple_system, '/admin/')

    # и просто создаем internal роль, привязанную к системе, но без юзеров
    InternalRole.objects.create(
        role='some_role',
        node=RoleNode.objects.get_node_by_value_path(simple_system, '/manager/'),
    )

    roles = get_all_roles(client, url)

    assert {(role['login'], role['subject_type']) for role in roles} == {
        ('frodo', 'user'),
        ('sam', 'user'),
        ('the-ring', 'tvm_app'),
    }

    frodo_roles = get_user_roles(roles, 'frodo', 'user')
    assert sorted(frodo_roles, key=lambda x: x['role']) == sorted([
        {'role': '/group/common/role/developer/'},
        {'scope': '/', 'role': '/group/system/system_on/simple/role/roles_manage/'},
    ], key=lambda x: x['role'])

    sam_roles = get_user_roles(roles, 'sam', 'user')
    assert sorted(sam_roles) == sorted([
        {'scope': '/admin/', 'role': '/group/system/system_on/simple/role/roles_manage/'},
    ])

    ring_roles = get_user_roles(roles, 'the-ring', 'tvm_app')
    assert sorted(ring_roles, key=lambda x: x['role']) == [
        {'role': '/group/common/role/auditor/'},
    ]


def test_hooks_get_all_roles_excludes_responsibilities(complex_system_with_responsibilities, arda_users, client):
    """Проверяем, что права, выданные через ответственных отфильтровываются из выдачи get-all-roles"""

    url = reverse('client-api:get-roles')
    roles = get_all_roles(client, url)
    assert {(role['login'], role['subject_type']) for role in roles} == set()

    # но если пометить их неактивными, то выгрузка работает
    NodeResponsibility.objects.update(is_active=False)
    roles = get_all_roles(client, url)
    assert {(role['login'], role['subject_type']) for role in roles} == {('frodo', 'user'), ('sam', 'user')}


def test_cannot_request_role_on_node_with_responsibilities(complex_system_with_responsibilities, arda_users):
    """Проверим, что запросить роль на ноду с активными ответственными нельзя"""

    gimli = arda_users.gimli

    system = complex_system_with_responsibilities
    system.plugin_type = 'idm.core.plugins.generic_self.Plugin'
    system._plugin = None
    system.save()

    with pytest.raises(AccessDenied) as excinfo:
        system.plugin.add_role(
            'gimli',
            {'group': 'system', 'system_on': system.slug, 'role': 'roles_manage'},
            {'scope': '/subs/manager/'},
            subject_type='user',
        )
    assert str(excinfo.value) == 'Scope `/subs/manager/` in system `complex` is managed by responsibilities'

    # Если пометить ответственными неактивными, то всё получается
    NodeResponsibility.objects.update(is_active=False)

    response = system.plugin.add_role(
        'gimli',
        {'group': 'system', 'system_on': system.slug, 'role': 'roles_manage'},
        {'scope': '/subs/manager/'},
        subject_type='user',
    )
    assert response == {
        'code': 0,
        'data': {
            'scope': '/subs/manager/'
        }
    }
    assert gimli.has_internal_role('roles_manage', system) is False
    assert gimli.has_internal_role('roles_manage', system, '/subs/manager/') is True
    assert gimli.has_internal_role('roles_manage', system, '/rules/admin/') is False
