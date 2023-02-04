# coding: utf-8


import pytest
from django.core import mail

from idm.core.models import Role, UserPassportLogin
from idm.tests.utils import set_workflow, add_perms_by_role
from idm.permissions.utils import add_perms_by_role
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_smoke_rolerequests_by_id(client, simple_system, users_for_test):
    """
    POST /api/v1/rolerequests/
    """
    set_workflow(simple_system, 'approvers = ["art"]')
    (art, fantom, terran, admin) = users_for_test

    # unauthorized error
    response = client.json.post(reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests'), {
        'system': 'simple',
        'role_path': ['manager']
    })
    assert response.status_code == 401

    # authorized actual request
    add_perms_by_role('impersonator', fantom)
    client.login('fantom')
    response = client.json.post(reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests'), {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        '_requester': 'fantom'
    })
    assert response.status_code == 201
    assert Role.objects.count() == 1

    role = Role.objects.get()
    role.fetch_node()
    assert role.node.data == {'role': 'manager'}
    assert role.user_id == fantom.id
    assert role.system_id == simple_system.id


def test_role_manage_by_impersonator(client, simple_system, users_for_test):
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = ["terran"]')

    # логинимся как art и даём ему права представляться другими
    client.login('art')
    add_perms_by_role('impersonator', art, simple_system)

    # запрашиваем роль для fantom от имени суперпользователя
    response = client.json.post(reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests'), {
        'path': '/superuser/',
        'user': 'fantom',
        'system': 'simple',
        '_requester': 'admin',
    })
    assert response.status_code == 201
    assert response.json()['node']['data'] == {'role': 'superuser'}
    assert response.json()['user']['username'] == 'fantom'
    assert response.json()['system']['slug'] == 'simple'

    # получаем список запросов от имени terran
    response = client.json.get(reverse('api_dispatch_list', api_name='v1', resource_name='approverequests'), {
        'status': 'pending',
        '_requester': 'terran',
    })

    assert response.status_code == 200
    approve_request_id = response.json()['objects'][0]['id']

    # подтверждаем роль для fantom от имени terran
    response = client.json.post(
        reverse('api_dispatch_detail', api_name='v1', resource_name='approverequests', pk=approve_request_id),
        {
            'approve': True,
            '_requester': 'terran',
        },
    )

    assert response.status_code == 204

    # получаем список ролей fantom от его имени
    response = client.json.get(
        reverse('api_dispatch_list', api_name='v1', resource_name='roles'),
        data={
            'user': 'fantom',
            '_requester': 'fantom',
        },
    )

    assert response.status_code == 200
    assert response.json()['meta']['total_count'] == 1


def test_request_hidden_role(client, pt1_system, users_for_test):
    """
    POST /v1/rolerequests/
    """
    (art, fantom, terran, admin) = users_for_test
    set_workflow(pt1_system, 'approvers = []')
    assert len(mail.outbox) == 0

    client.login('fantom')
    response = client.json.post(
        reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests'),
        {
            'path': '/proj2/invisible_role/',
            'system': pt1_system.slug,
            'user': 'fantom',
            'fields_data': {'passport-login': 'abc'},
        },
    )
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'Вы не можете запрашивать скрытую роль',
    }
    assert len(mail.outbox) == 0
    assert Role.objects.count() == 0

    add_perms_by_role('roles_manage', fantom, system=pt1_system)
    response = client.json.post(
        reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests'),
        {
            'path': '/proj2/invisible_role/',
            'system': pt1_system.slug,
            'user': 'fantom',
            'fields_data': {'passport-login': 'abc'},
        },
    )
    assert response.status_code == 201
    assert response.json()['node']['data'] == {'project': 'proj2', 'role': 'invisible_role'}
    assert Role.objects.count() == 1
    assert len(mail.outbox) == 0


def test_post_rolerequest_with_passport_login_occupied_by_another_user(pt1_system, arda_users, client):
    frodo = arda_users.frodo
    UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)
    client.login(frodo)
    url = reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests')

    response = client.json.post(
        url,
        {
            'user': 'frodo',
            'path': '/proj1/admin/',
            'system': 'test1',
            'fields_data': {'passport-login': 'yndx-frodo'},
        }
    )
    assert response.status_code == 201

    response = client.json.post(
        url,
        {
            'user': 'sam',
            'path': '/proj1/admin/',
            'system': 'test1',
            'fields_data': {'passport-login': 'yndx-frodo'},
        }
    )
    assert response.status_code == 409
    data = response.json()
    expected = ('Role /proj1/admin/ for user sam on passport_login yndx-frodo can not be issued, '
                'passport_login belongs to user frodo')
    assert data['message'] == expected
