# coding: utf-8

import pytest

from procu.api import models
from procu.api.enums import ACCESS_SOURCE
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


ACCESS_LIST = {
    'results': [
        {
            'id': 1,
            'user': {
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
            'allow_quote_comments': True,
            'is_subscribed': False,
            'can_update': True,
        }
    ]
}


ACCESS_RETRIEVE = {
    'id': 1,
    'user': {
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
    'allow_quote_comments': True,
    'is_subscribed': False,
}


def test_enquiry_accesses_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/accesses')
    assert_status(resp, 200)

    assert resp.json() == ACCESS_LIST


def test_enquiry_accesses_create(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.post(
        '/api/enquiries/1/accesses', data={'users': ['robot-procu-test']}
    )
    assert_status(resp, 200)

    qs = models.EnquiryAccess.objects.filter(
        enquiry_id=1, user_id=3, sources__contains=[ACCESS_SOURCE.ACCESS]
    )

    assert qs.exists()


def test_enquiry_accesses_retrieve(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/accesses/1')
    assert_status(resp, 200)

    assert resp.json() == ACCESS_RETRIEVE


def test_enquiry_accesses_update(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'is_subscribed': True, 'allow_quote_comments': False}

    resp = client.patch('/api/enquiries/1/accesses/1', data=data)
    assert_status(resp, 200)

    obj = models.EnquiryAccess.objects.values(
        'is_subscribed', 'allow_quote_comments'
    ).get(id=1)

    assert data == obj


def test_enquiry_accesses_delete(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.delete('/api/enquiries/1/accesses/1')
    assert_status(resp, 204)

    qs = models.EnquiryAccess.objects.filter(id=1)
    assert not qs.exists()
