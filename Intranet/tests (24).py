# coding: utf-8

from unittest.mock import patch

import pytest

from procu.api import models
from procu.utils.test import assert_status, mock_find_tickets, prepare_user

pytestmark = [pytest.mark.django_db]


WARNING_LIST = {
    'results': [
        {
            'id': 2,
            'message': 'Оч неоч :(',
            'author': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'full_name': 'Тестовый Робот Закупок',
                'first_name': 'Тестовый Робот',
                'last_name': 'Закупок',
                'is_staff': True,
                'is_deleted': False,
                'is_clickable': True,
            },
            'created_at': '2018-06-28T16:19:44.059000+03:00',
            'updated_at': '2018-06-29T01:06:17.153000+03:00',
            'can_edit': False,
            'is_deleted': False,
        },
        {
            'id': 1,
            'message': 'Ну правда, так себе поставщик',
            'author': {
                'id': 1,
                'username': 'robot-procu',
                'email': 'robot-procu@yandex-team.ru',
                'full_name': 'Робот Закупок',
                'first_name': 'Робот',
                'last_name': 'Закупок',
                'is_staff': True,
                'is_deleted': False,
                'is_clickable': True,
            },
            'created_at': '2018-05-28T16:19:44.059000+03:00',
            'updated_at': '2018-05-29T01:06:17.153000+03:00',
            'can_edit': True,
            'is_deleted': False,
        },
    ]
}


def test_supplier_warnings_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/suppliers/1/warnings')
    assert_status(resp, 200)

    assert resp.json() == WARNING_LIST


# ------------------------------------------------------------------------------


def test_supplier_warnings_create(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'message': 'Не советую'}

    resp = client.post('/api/suppliers/1/warnings', data=data)
    assert_status(resp, 201)

    warning_id = resp.json()['id']

    obj = models.SupplierWarning.objects.values('message').get(
        id=warning_id, author_id=1
    )

    assert obj == data


# ------------------------------------------------------------------------------


WARNING_RETRIEVE = {
    'id': 1,
    'message': 'Ну правда, так себе поставщик',
    'author': {
        'id': 1,
        'username': 'robot-procu',
        'email': 'robot-procu@yandex-team.ru',
        'full_name': 'Робот Закупок',
        'first_name': 'Робот',
        'last_name': 'Закупок',
        'is_staff': True,
        'is_deleted': False,
        'is_clickable': True,
    },
    'created_at': '2018-05-28T16:19:44.059000+03:00',
    'updated_at': '2018-05-29T01:06:17.153000+03:00',
}


def test_supplier_warnings_retrieve(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/suppliers/1/warnings/1')
    assert_status(resp, 200)

    assert resp.json() == WARNING_RETRIEVE


# ------------------------------------------------------------------------------


def test_supplier_warnings_update(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'message': 'foobar'}

    resp = client.patch('/api/suppliers/1/warnings/1', data=data)
    assert_status(resp, 200)

    obj = models.SupplierWarning.objects.values('message').get(id=1)

    assert obj == data


# ------------------------------------------------------------------------------


def test_supplier_warnings_destroy(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    qs = models.SupplierWarning.objects.filter(id=1)

    resp = client.delete(f'/api/suppliers/1/warnings/1')
    assert_status(resp, 204)

    assert not qs.exists()


# ------------------------------------------------------------------------------


@pytest.mark.parametrize('method', ('DELETE', 'PATCH'))
def test_supplier_warnings_forbidden(clients, method):

    client = clients['internal']
    prepare_user(client, username='robot-procu-test', roles=['admin'])

    # Just to make sure that the warning is accessible...
    resp = client.get('/api/suppliers/1/warnings/1')
    assert_status(resp, 200)

    # ...but we cannot apply the method
    # since the warning was created by another user
    resp = client.generic(method, '/api/suppliers/1/warnings/1')
    assert_status(resp, 403)
