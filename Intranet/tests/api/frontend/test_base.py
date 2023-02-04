# coding: utf-8


from mock import patch

from django.http import Http404

from idm.api.exceptions import ResourceAlreadyExists
from idm.api.frontend.system import SystemResource
from idm.utils import reverse
from idm.tests.utils import assert_sorted, assert_contains


import pytest
# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_api_error_handling(client, simple_system, users_for_test):
    client.login('art')

    with patch.object(SystemResource, 'dehydrate') as dehydrate:
        dehydrate.side_effect = ResourceAlreadyExists
        response = client.json.get(reverse('api_dispatch_list', api_name='frontend', resource_name='systems'))
        assert response.status_code == ResourceAlreadyExists.response_class.status_code
        assert response.json() == {
            'error_code': 'ALREADY_EXISTS',
            'message': 'Resource already exists',
        }

        dehydrate.side_effect = Http404('could not be found')
        response = client.json.get(reverse('api_dispatch_list', api_name='frontend', resource_name='systems'))
        assert response.status_code == 404
        assert response.json() == {
            'message': 'could not be found',
            'error_code': 'NOT_FOUND',
        }

        dehydrate.side_effect = RuntimeError('Some error')
        response = client.json.get(reverse('api_dispatch_list', api_name='frontend', resource_name='systems'))
        assert response.status_code == 500
        data = response.json()

        assert (data['message'], data['error_code']) == ('Unhandled exception - RuntimeError: Some error', 'UNKNOWN_ERROR')
        assert_contains(['Traceback (most recent call last):', 'RuntimeError'], data['traceback'])


def test_404_handling(client, simple_system, users_for_test):
    client.login('art')

    fake_url = '/api/frontend/hiall'
    response = client.json.get(fake_url)

    assert response.status_code == 404
    assert response.json() == {
        'message': 'resource not found',
        'error_code': 'NOT_FOUND',
    }


def test_info_headers(client, users_for_test):
    client.login('admin')

    response = client.json.get(reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/users/all'))
    assert response['X-IDM-READONLY'] == 'None'
    assert response['X-IDM-VERSION'] == 'dev'


def test_sorting(client, users_with_roles, groups_with_roles):
    client.login('admin')

    def get_sorted_response(order_by):
        return client.json.get(
            reverse('api_dispatch_list', api_name='frontend', resource_name='roles'),
            {'order_by': order_by, 'format': 'json'},
        )

    response = get_sorted_response(['level'])
    assert response.status_code == 400

    response = get_sorted_response(['updated'])
    assert response.status_code == 200
    data = response.json()
    assert_sorted(data['objects'], lambda item: item['updated'])

    response = get_sorted_response(['-added'])
    assert response.status_code == 200
    data = response.json()
    assert_sorted(data['objects'], lambda item: item['added'], descending=True)

    response = get_sorted_response(['state', 'id'])
    assert response.status_code == 200
    data = response.json()
    assert_sorted(data['objects'], lambda item: (item['state'], item['id']))

    response = get_sorted_response(['subject'])
    assert response.status_code == 200
    data = response.json()
    assert_sorted(
        items=data['objects'],
        keyfunc=lambda item: (
            (item.get('user') or {}).get('username') or '~',  # groups must be placed at the end
            (item.get('group') or {}).get('name') or float('inf'),
        ),
    )

    response = get_sorted_response(['-role'])
    assert response.status_code == 200
    data = response.json()
    assert_sorted(data['objects'], keyfunc=lambda item: (item['node']['id'], item['id']), descending=True)


def test_paginator(client, arda_users_with_roles, settings):
    settings.IDM_PAGINATOR_ESTIMATION_THRESHOLD = -1

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='roles')

    response = client.json.get(url, {'limit': 2})
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['count_is_estimated'] is True


def test_paginator_w_cyrillic(client, arda_users, settings):
    settings.IDM_PAGINATOR_ESTIMATION_THRESHOLD = -1

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')

    response = client.json.get(url, {'system__contains': 'Прост', 'limit': 0})
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['count_is_estimated'] is True
