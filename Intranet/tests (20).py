# coding: utf-8

from datetime import timedelta

import pytest
from django.utils.timezone import now

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


REQUEST_LIST = {
    'count': 1,
    'page': 1,
    'limit': 40,
    'num_pages': 1,
    'results': [
        {
            'id': 1,
            'request': {
                'key': 'YP1',
                'subject': 'Мультиварки',
                'no_replacement': False,
            },
            'deadline_at': '2018-02-20T18:33:00+03:00',
            'updated_at': '2018-02-16T13:17:55.485000+03:00',
            'status': {'key': 'review', 'name': 'Выбор поставщика'},
            'reason': {'key': 'none', 'name': ''},
            'has_offer': True,
            'has_won': False,
        }
    ],
}


def test_requests_list(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.get('/api/requests/')
    assert_status(resp, 200)

    assert resp.json() == REQUEST_LIST


# ------------------------------------------------------------------------------


REQUEST_RETRIEVE = {
    'id': 1,
    'request': {
        'key': 'YP1',
        'subject': 'Мультиварки',
        'no_replacement': False,
    },
    'supplier': 1,
    'deadline_at': '2018-02-20T18:33:00+03:00',
    'created_at': '2017-12-06T18:54:27.077000+03:00',
    'updated_at': '2018-02-16T13:17:55.485000+03:00',
    'delivery_at': '2018-12-30',
    'terms': {'comment': 'Утром деньги, вечером стулья'},
    'status': {'key': 'review', 'name': 'Выбор поставщика'},
    'reason': {'key': 'none', 'name': ''},
    'has_offer': True,
    'has_won': False,
    'hide_products': False,
    'invoices': [],
}


def test_requests_retrieve(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.get('/api/requests/1')
    assert_status(resp, 200)

    assert resp.json() == REQUEST_RETRIEVE


# ------------------------------------------------------------------------------


def test_requests_update(clients):

    # Change status to BIDDING by moving deadline into the future
    models.Quote.objects.filter(id=1).update(
        deadline_at=now() + timedelta(days=1)
    )

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.patch('/api/requests/1', data={'terms': 'wow'})
    assert_status(resp, 200)

    terms = models.Quote.objects.values_list('terms', flat=True).get(id=1)

    assert terms == {'comment': 'wow'}
