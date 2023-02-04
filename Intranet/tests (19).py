# coding: utf-8

import pytest
from django.core import mail

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


COMMENT_LIST = {
    'results': [
        {
            'id': 2,
            'quote': 1,
            'updated_at': '2017-12-07T15:41:10.071000+03:00',
            'author': {'id': 1, 'full_name': 'Робот Закупок'},
            'message': 'pong',
            'message_html': None,
            'attachments': [],
            'is_suspicious': False,
            'is_from_email': False,
            'can_edit': False,
        },
        {
            'id': 1,
            'quote': 1,
            'updated_at': '2017-12-07T14:41:10.071000+03:00',
            'author': {'id': 2, 'full_name': 'Test User-001'},
            'message': 'ping',
            'message_html': None,
            'attachments': [],
            'is_suspicious': False,
            'is_from_email': False,
            'can_edit': True,
        },
    ]
}


def test_quote_comments_list(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.get('/api/requests/1/comments')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_LIST


# ------------------------------------------------------------------------------


def test_quote_comments_create(clients):

    mail.outbox = []

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    data = {'message': 'foobar', 'attachments': [7]}

    resp = client.post('/api/requests/1/comments', data=data)
    assert_status(resp, 201)

    comment_id = resp.json()['id']

    msg = models.QuoteComment.objects.get(id=comment_id)

    assert msg.message == 'foobar'

    attachments = list(msg.attachments.values_list('id', flat=True))
    assert attachments == [7]


# ------------------------------------------------------------------------------


COMMENT_RETRIEVE = {
    'id': 1,
    'author': {'id': 2, 'full_name': 'Test User-001'},
    'updated_at': '2017-12-07T14:41:10.071000+03:00',
    'message': 'ping',
    'attachments': [],
}


def test_quote_comments_retrieve(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.get('/api/requests/1/comments/1')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_RETRIEVE


# ------------------------------------------------------------------------------


def test_quote_comments_update(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    msg = '21345'
    data = {'message': msg}

    resp = client.patch('/api/requests/1/comments/1', data=data)
    assert_status(resp, 200)

    obj = models.QuoteComment.objects.values('message').get(id=1)

    assert obj['message'] == msg


# ------------------------------------------------------------------------------


def test_quote_comments_delete(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru')

    resp = client.delete('/api/requests/1/comments/1')
    assert_status(resp, 204)

    qs = models.QuoteComment.objects.filter(id=1)
    assert not qs.exists()


# ------------------------------------------------------------------------------


@pytest.mark.parametrize('method', ('DELETE', 'PATCH'))
def test_quote_comments_forbidden(clients, method):

    # Temporary adding another contact to the supplier
    models.User.objects.filter(email='s002@procu.ru').update(supplier_id=1)

    client = clients['external']
    prepare_user(client, username='s002@procu.ru')

    # Just to make sure that the comment is accessible...
    resp = client.get('/api/requests/1/comments/1')
    assert_status(resp, 200)

    # ...but we cannot apply the method
    # since the comment was created by another user
    resp = client.generic(method, '/api/requests/1/comments/1')
    assert_status(resp, 403)
