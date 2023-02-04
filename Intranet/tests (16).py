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
            'message': 'pong',
            'message_html': None,
            'attachments': [],
            'is_suspicious': False,
            'is_from_email': False,
            'can_edit': True,
        },
        {
            'id': 1,
            'quote': 1,
            'updated_at': '2017-12-07T14:41:10.071000+03:00',
            'author': {
                'id': 2,
                'username': 's001@procu.ru',
                'email': 's001@procu.ru',
                'full_name': 'Test User-001',
                'first_name': 'Test',
                'last_name': 'User-001',
                'is_staff': False,
                'is_deleted': False,
                'is_clickable': True,
            },
            'message': 'ping',
            'message_html': None,
            'attachments': [],
            'is_suspicious': False,
            'is_from_email': False,
            'can_edit': False,
        },
    ]
}


def test_quote_comments_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/quotes/1/comments')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_LIST


# ------------------------------------------------------------------------------


def test_quote_comments_create(clients):

    mail.outbox = []

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {'message': 'foobar', 'attachments': [6]}

    resp = client.post('/api/enquiries/1/quotes/1/comments', data=data)
    assert_status(resp, 201)

    comment_id = resp.json()['id']

    msg = models.QuoteComment.objects.get(id=comment_id)

    assert msg.message == 'foobar'

    attachments = list(msg.attachments.values_list('id', flat=True))
    assert attachments == [6]


# ------------------------------------------------------------------------------


COMMENT_RETRIEVE = {
    'id': 2,
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
    'updated_at': '2017-12-07T15:41:10.071000+03:00',
    'message': 'pong',
    'attachments': [],
}


def test_quote_comments_retrieve(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/quotes/1/comments/2')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_RETRIEVE


# ------------------------------------------------------------------------------


def test_quote_comments_update(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    msg = '21345'
    data = {'message': msg}

    resp = client.patch('/api/enquiries/1/quotes/1/comments/2', data=data)
    assert_status(resp, 200)

    obj = models.QuoteComment.objects.values('message').get(id=2)

    assert obj['message'] == msg


# ------------------------------------------------------------------------------


def test_quote_comments_delete(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.delete('/api/enquiries/1/quotes/1/comments/2')
    assert_status(resp, 204)

    qs = models.QuoteComment.objects.filter(id=2)
    assert not qs.exists()


# ------------------------------------------------------------------------------


@pytest.mark.parametrize('method', ('DELETE', 'PATCH'))
def test_quote_comments_forbidden(clients, method):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    # Just to make sure that the comment is accessible...
    resp = client.get('/api/enquiries/1/quotes/1/comments/1')
    assert_status(resp, 200)

    # ...but we cannot apply the method
    # since the comment was created by another user
    resp = client.generic(method, '/api/enquiries/1/quotes/1/comments/1')
    assert_status(resp, 403)
