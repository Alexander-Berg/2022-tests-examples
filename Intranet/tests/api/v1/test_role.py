# -*- coding: utf-8 -*-
"""
Тесты API v1
"""


import pytest

from idm.permissions.utils import add_perms_by_role
from idm.tests.utils import create_user, make_role, raw_make_role
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def get_role_url(role_id):
    return reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role_id)


def test_smoke_role_by_id(client, simple_system):
    art = create_user('art')
    add_perms_by_role('impersonator', art)
    role = make_role(art, simple_system, {'role': 'manager'})
    client.login('art')
    url = reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role.pk)

    data = client.json.get(
        url, data={'_requester': 'art'},
    ).json()

    assert data['id'] == role.id
    assert data['is_active'] is True
    assert data['user']['username'] == 'art'


def test_public_roles(client, arda_users, simple_system):
    """Проверим, что скрытые роли отдаются в списке ролей"""

    roles_url = reverse('api_dispatch_list', api_name='v1', resource_name='roles')
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    client.login('frodo')
    data = client.json.get(roles_url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['id'] == role.id

    role.node.is_public = False
    role.node.save(update_fields=['is_public'])
    data = client.json.get(roles_url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['id'] == role.id


@pytest.mark.parametrize('pass_as', ['data', 'query'])
def test_delete_with_requester(client, simple_system, arda_users, pass_as):
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    client.login('gandalf')
    add_perms_by_role('impersonator', gandalf)
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    deprive_url = get_role_url(role.id)

    kwargs = {
        pass_as: {'_requester': 'frodo'},
    }
    response = client.json.delete(deprive_url, **kwargs)
    assert response.status_code == 204
    deprive_action = role.actions.get(action='deprive')
    assert deprive_action.user_id == frodo.id
    assert deprive_action.impersonator_id == gandalf.id


@pytest.mark.skip('Починить переопределенный механизм листинга ролей')
@pytest.mark.parametrize('fields', (set(), {'user', 'state', 'is_active'}))
def test_list__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_list', api_name='v1', resource_name='roles'),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) > 0
    for node_data in result['objects']:
        if fields:
            assert set(node_data.keys()) == fields
        else:
            # при пустом значении возвращаем все поля
            assert node_data != {}


@pytest.mark.skip('Починить переопределенный механизм листинга ролей')
def test_list__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')
    unknown_field = 'unknown_field'

    response = client.json.get(
        reverse('api_dispatch_list', api_name='v1', resource_name='roles'),
        data={'fields': f'state,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


@pytest.mark.parametrize('fields', (set(), {'user', 'state', 'is_active'}))
def test_get__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role.pk),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    node_data = response.json()
    if fields:
        assert set(node_data.keys()) == fields
    else:
        # при пустом значении возвращаем все поля
        assert node_data != {}


def test_get__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    unknown_field = 'unknown_field'
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role.pk),
        data={'fields': f'state,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'
