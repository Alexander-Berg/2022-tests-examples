# coding: utf-8


import pytest
from django.conf import settings
from django.urls import reverse
from textwrap import dedent

from idm.core.models import Role, InternalRole
from idm.tests.utils import create_user, add_perms_by_role, set_workflow, get_all_roles
from idm.utils import json
from idm.utils.rolenode import deprive_nodes

pytestmark = pytest.mark.django_db


def test_add_same_role_twice(client, simple_system, arda_users):
    """Проверка, что IDM как система штатно отвечает на попытку выдать одну и ту же роль дважды"""

    add_role_url = reverse('client-api:add-role')
    data = {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        }),
    }
    response = client.post(add_role_url, data, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
        'data': {
            'scope': '/'
        }
    }
    response = client.post(add_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        })
    }, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
        'data': {
            'scope': '/'
        }
    }
    assert InternalRole.objects.count() == 1


def test_remove_same_role_twice(client, simple_system, arda_users):
    """Проверка, что IDM как система штатно отвечает на попытку отозвать несуществующую роль"""

    add_role_url = reverse('client-api:add-role')
    remove_role_url = reverse('client-api:remove-role')
    response = client.post(add_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        })
    }, with_idm_credentials=True)
    response = client.post(remove_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        })
    }, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
    }
    response = client.post(remove_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        })
    }, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
    }
    assert InternalRole.objects.count() == 0


def test_add_same_role_twice_with_scope(client, simple_system, arda_users):
    """Проверка, что IDM как система штатно отвечает на попытку выдать одну и ту же роль дважды.
    Проверяем, что при передаче scope тоже всё хорошо"""

    add_role_url = reverse('client-api:add-role')
    response = client.post(add_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        }),
        'fields': json.dumps({
            'scope': '/manager/'
        })
    }, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
        'data': {
            'scope': '/manager/'
        }
    }
    response = client.post(add_role_url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': simple_system.slug,
            'role': 'responsible'
        }),
        'fields': json.dumps({
            'scope': '/manager/'
        })
    }, with_idm_credentials=True)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'code': 0,
        'data': {
            'scope': '/manager/'
        }
    }
    assert InternalRole.objects.count() == 1


def test_info(client, simple_system):
    info_url = reverse('client-api:info')
    response = client.json.get(info_url, with_idm_credentials=True)
    data = response.json()
    assert set(data.keys()) == {'code', 'roles'}
    assert data['code'] == 0
    assert set(data['roles']['values'].keys()) == {'common', 'system'}
    assert set(data['roles']['values']['common']['roles']['values'].keys()) == {item[0] for item in settings.IDM_COMMON_ROLES}


def test_add_common_role(client, arda_users):
    response = client.post(reverse('client-api:add-role'),
                           data={'login': 'frodo', 'role': json.dumps({'group': 'common', 'role': 'auditor'})},
                           with_idm_credentials=True)

    assert response.status_code == 200

    assert InternalRole.objects.count() == 1
    assert InternalRole.objects.get().role == 'auditor'


def test_remove_common_role(client):
    frodo = create_user('frodo')

    add_perms_by_role('auditor', frodo)

    response = client.post(reverse('client-api:remove-role'),
                           data={'login': 'frodo', 'role': json.dumps({'group': 'common', 'role': 'auditor'})},
                           with_idm_credentials=True)

    assert response.status_code == 200
    assert json.loads(response.content) == {'code': 0}

    assert InternalRole.objects.count() == 0


@pytest.mark.parametrize('scope', [None, '/', '/proj1/manager/'])
def test_add_system_role(client, pt1_system, scope):
    frodo = create_user('frodo')

    url = reverse('client-api:add-role')
    data = {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': 'test1',
            'role': 'roles_manage'
        }),
    }
    if scope is None:
        expected_scope = '/'
    else:
        expected_scope = scope
        data['fields'] = json.dumps({'scope': scope})
    response = client.post(url, data, with_idm_credentials=True)

    assert response.status_code == 200
    assert json.loads(response.content) == {'code': 0, 'data': {'scope': expected_scope}}

    assert InternalRole.objects.count() == 1

    role = InternalRole.objects.get()

    assert role.role == 'roles_manage'
    role.fetch_node()
    assert role.node.value_path == expected_scope

    # get_users_with_permissions отдаёт пользователя только если scope равен /
    if expected_scope == '/':
        expected_users = [frodo]
    else:
        expected_users = []
    assert list(pt1_system.get_users_with_permissions('core.request_role')) == expected_users


def test_remove_system_role(client, pt1_system):
    frodo = create_user('frodo')

    add_perms_by_role('roles_manage', frodo, pt1_system, '/proj1/manager/')

    url = reverse('client-api:remove-role')
    response = client.post(url, {
        'login': 'frodo',
        'role': json.dumps({
            'group': 'system',
            'system_on': 'test1',
            'role': 'roles_manage'
        }),
        'fields': json.dumps({
            'scope': '/proj1/manager/'
        })
    }, with_idm_credentials=True)

    assert response.status_code == 200

    assert InternalRole.objects.count() == 0


def test_get_info_returns_tree(client, pt1_system):
    data = json.loads(client.get(reverse('client-api:info'), with_idm_credentials=True).content)

    assert data['code'] == 0
    assert data['roles']['slug'] == 'group'
    sorted_common = sorted(r for r, _ in settings.IDM_COMMON_ROLES)
    assert sorted_common == sorted(data['roles']['values']['common']['roles']['values'].keys())
    assert data['roles']['values']['system']['roles']['slug'] == 'system_on'
    assert list(data['roles']['values']['system']['roles']['values'].keys()) == ['test1']


def test_get_all_roles_returns_all_slugs(client, pt1_system):
    """ Проверяем, что /get-all-roles/ возвращает не просто group-число,
    но и все предыдущие уровни.
    """
    art = create_user('art')

    info = json.loads(client.get(reverse('client-api:info'), with_idm_credentials=True).content)
    role_name = list(info['roles']['values']['system']['roles']['values']['test1']['roles']['values'].keys())[0]
    add_perms_by_role(role_name, art, pt1_system)

    role_description = '/group/system/system_on/test1/role/%s/' % role_name
    roles = get_all_roles(client, reverse('client-api:get-roles'))
    assert len(roles) == 1
    assert {'login': 'art', 'subject_type': 'user', 'path': role_description, 'fields': {'scope': '/'}} == roles[0]


def test_internal_roles_are_deleted(simple_system, idm, arda_users):
    workflow_with_refs = dedent("""
    approvers = []

    if role['role'] == 'manager':
        ref_roles = [dict(system='self',
                          role_data={'group': 'system', 'system_on': 'simple', 'role': 'roles_manage'},
                          role_fields={'scope': scope})]

    """)

    set_workflow(simple_system, workflow_with_refs)

    frodo = arda_users.frodo
    role_data = {'role': 'manager'}

    role = Role.objects.request_role(frodo, frodo, simple_system, '', role_data, None)
    assert InternalRole.objects.count() == 1

    node = role.node
    group_node = node.parent.parent
    group_node.mark_depriving(immediately=True)
    deprive_nodes(simple_system, from_api=True)

    assert InternalRole.objects.count() == 0
