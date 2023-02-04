# coding: utf-8
"""
Тесты API открытого для тестировщиков на тестинге (triple-combo!)
"""


import pytest

from idm.core.models import Role, Action
from idm.tests.utils import refresh, set_workflow, add_perms_by_role, raw_make_role
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_get_roles(client, simple_system, users_for_test):
    """
    GET /testapi/roles/
    """

    (art, fantom, terran, admin) = users_for_test

    # role granted to art
    Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    set_workflow(simple_system, 'approvers = ["admin"]')

    # role in requested state
    Role.objects.request_role(art, art, simple_system, None, {'role': 'manager'}, None)

    # role in deprived state
    role_to_deprive = Role.objects.request_role(admin, admin, simple_system, None, {'role': 'superuser'}, None)
    refresh(role_to_deprive).deprive_or_decline(admin)

    client.login('art')
    data = client.json.get(reverse('api_dispatch_list', api_name='testapi', resource_name='roles')).json()

    assert data['meta']['total_count'] == 3
    assert {role_data['system']['slug'] for role_data in data['objects']} == {'simple'}

    assert data['objects'][0]['data'] == {'role': 'superuser'}
    assert data['objects'][0]['user']['username'] == 'admin'
    assert data['objects'][0]['state'] == 'deprived'
    assert data['objects'][0]['is_active'] is False

    assert data['objects'][1]['data'] == {'role': 'manager'}
    assert data['objects'][1]['user']['username'] == 'art'
    assert data['objects'][1]['state'] == 'requested'
    assert data['objects'][1]['is_active'] is False

    assert data['objects'][2]['data'] == {'role': 'superuser'}
    assert data['objects'][2]['user']['username'] == 'art'
    assert data['objects'][2]['state'] == 'granted'
    assert data['objects'][2]['is_active'] is True


def test_get_role(client, simple_system, users_for_test):
    """
    GET /testapi/roles/<role_id>/
    """
    (art, fantom, terran, admin) = users_for_test

    # role granted to art
    add_perms_by_role('responsible', fantom, simple_system)
    superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'}, None)

    client.login('art')
    data = client.json.get(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=superuser_role.id)
    ).json()

    assert data['data'] == {'role': 'superuser'}
    assert data['user']['username'] == 'art'
    assert data['state'] == 'granted'
    assert data['is_active'] is True
    assert data['system']['slug'] == 'simple'
    assert data['system_specific'] is None


def test_request_role(client, simple_system, users_for_test):
    """
    POST /testapi/roles/
    """
    (art, fantom, terran, admin) = users_for_test
    # get role history
    add_perms_by_role('responsible', art, simple_system)

    client.login('art')
    response = client.json.post(reverse('api_dispatch_list', api_name='testapi', resource_name='roles'), {
        'requester': 'art',
        'user': 'fantom',
        'data': {'role': 'superuser'},
        'system': 'simple'
    })

    assert response.status_code == 201
    assert Role.objects.count() == 1
    created_role = Role.objects.select_related('user', 'system').all()[0]

    assert created_role.actions.filter(action='request')[0].requester_id == art.id
    assert created_role.user_id == fantom.id
    assert created_role.system_id == simple_system.id
    created_role.fetch_node()
    assert created_role.node.data == {'role': 'superuser'}
    assert created_role.state == 'granted'


def test_update_role_allowed(client, simple_system, users_for_test, settings):
    """
    PUT /testapi/roles/<role_id>/
    Изменять можно только три поля: review_at, node и system_specific
    """

    settings.TIME_ZONE = 'UTC'

    (art, fantom, terran, admin) = users_for_test
    changing_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    assert refresh(changing_role).node.data == {'role': 'superuser'}

    # change role data
    client.login('art')
    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'node': '/role/manager/',
        }
    )

    assert response.status_code == 204
    assert refresh(changing_role).node.data == {'role': 'manager'}

    # change role review_at field
    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'review_at': '1970-02-01T00:00:00',
        }
    )

    assert response.status_code == 204
    changing_role.refresh_from_db()
    assert changing_role.review_at.year == 1970
    assert changing_role.review_at.month == 2

    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'system_specific': {'login': 'frodo'},
        }
    )
    assert response.status_code == 204
    changing_role.refresh_from_db()
    assert changing_role.system_specific == {'login': 'frodo'}


def test_update_role_disallowed(client, simple_system, pt1_system, users_for_test):
    """
    PUT /testapi/roles/<role_id>/
    Изменять можно только три поля: review_at, node и system_specific
    """

    (art, fantom, terran, admin) = users_for_test
    changing_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
    client.login('art')

    # change role system - not allowed
    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'system': 'test1',
        }
    )

    assert response.status_code == 204
    assert refresh(changing_role).system == simple_system

    # change role user - not allowed
    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'user': 'admin',
        }
    )

    assert response.status_code == 204
    assert refresh(changing_role).user_id == art.id

    # change role expire_at field - not allowed
    response = client.json.put(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=changing_role.id), {
            'expire_at': '1970-02-01T00:00:00',
        }
    )

    assert response.status_code == 204
    assert refresh(changing_role).expire_at is None


def test_delete_role(client, simple_system, users_for_test):
    """
    DELETE /testapi/roles/<role_id>/
    """
    (art, fantom, terran, admin) = users_for_test

    # delete simple role
    superuser_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
    assert Role.objects.count() == 1
    # request, apply_workflow, approve, first_add_role_push, grant actions, change_workflow and mass_action
    assert Action.objects.count() == 7

    client.login('art')
    response = client.delete(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='roles', pk=superuser_role.id)
    )

    assert response.status_code == 204
    assert Role.objects.count() == 0
    assert Action.objects.count() == 2

    # delete user-approved role
    set_workflow(simple_system, 'approvers = ["admin"]')

    manager_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'manager'}, None)
    approve_request = manager_role.get_open_request().approves.get().requests.get()
    approve_request.set_approved(admin)

    assert Role.objects.count() == 1
    # request, apply_workflow, approve, first_add_role_push, grant actions, change_workflow x2 and mass_action
    assert Action.objects.count() == 8

    response = client.delete(reverse('api_dispatch_detail', api_name='testapi', resource_name='roles',
                                     pk=manager_role.id))

    assert response.status_code == 204
    assert Role.objects.count() == 0
    assert Action.objects.count() == 3  # change_workflow x2 and mass_action


def test_expire_role(client, simple_system, users_for_test):
    """
    POST /testapi/roles/<role_id>/expire/
    """
    (art, fantom, terran, admin) = users_for_test

    # role granted to art
    add_perms_by_role('responsible', fantom, simple_system)
    superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'}, None)

    # expire role
    client.login('art')
    response = client.post(
        reverse('api_move_role', api_name='testapi', resource_name='roles', pk=superuser_role.id, action='expire'),
    )

    superuser_role = refresh(superuser_role)

    assert response.status_code == 204
    assert superuser_role.state == 'deprived'
    assert superuser_role.actions.filter(action='expire').exists() is True


def test_wrong_move_for_role(client, simple_system, arda_users):
    """
    POST /testapi/roles/<role_id>/expire/
    """

    frodo = arda_users.frodo
    add_perms_by_role('responsible', frodo, simple_system)
    role1 = raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='approved')

    client.login('frodo')
    response = client.json.post(
        reverse('api_move_role', api_name='testapi', resource_name='roles', pk=role1.id, action='expire'),
    )

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': (
            'Undefined state switch for role %d: approved -> depriving. '
            'Available states: awaiting, failed, granted, idm_error, sent.' % role1.pk
        )
    }

    role2 = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    client.json.post(
        reverse('api_move_role', api_name='testapi', resource_name='roles', pk=role2.id, action='expire'),
    )
    response = client.json.post(
        reverse('api_move_role', api_name='testapi', resource_name='roles', pk=role2.id, action='expire'),
    )

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': ('Undefined state switch for role %d: deprived -> depriving. '
                    'Available states: requested.' % role2.pk)
    }
