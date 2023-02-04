# coding: utf-8

import pytest
from pkg_resources import resource_string

from procu.api import models
from procu.utils.test import assert_status, mock_wf, prepare_user

from django.core import mail

pytestmark = [pytest.mark.django_db]


COMMENT_LIST = {
    'results': [
        {
            'id': 1,
            'enquiry': 1,
            'created_at': '2017-12-07T20:20:22.005000+03:00',
            'updated_at': '2017-12-07T20:20:22.005000+03:00',
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
            'message': 'foobar',
            'message_html': 'foobar',
            'is_from_email': False,
            'attachments': [
                {
                    'id': 5,
                    'filename': 'test12.docx',
                    'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://5/test12.docx',
                    'created_at': '2018-05-22T19:16:21.527000+03:00',
                }
            ],
            'can_edit': True,
            'invitees': [],
        }
    ]
}


def test_enquiry_comments_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/comments')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_LIST


# ------------------------------------------------------------------------------


def test_enquiry_comments_create(clients):

    mail.outbox = []

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {
        'message': 'foobar',
        'attachments': [6],
        'invitees': ['robot-procu-test'],
    }

    html = resource_string('procu', f'fixtures/wf/response.html').decode()

    with mock_wf(html):
        resp = client.post('/api/enquiries/1/comments', data=data)
        assert_status(resp, 201)

    comment_id = resp.json()['id']

    msg = models.EnquiryComment.objects.get(id=comment_id)

    assert msg.message_html == html

    invitees = list(msg.invitees.values_list('username', flat=True))
    assert invitees == ['robot-procu-test']

    attachments = list(msg.attachments.values_list('id', flat=True))
    assert attachments == [6]

    assert any('призывает' in m.subject for m in mail.outbox)


# ------------------------------------------------------------------------------


COMMENT_RETRIEVE = {
    'id': 1,
    'message': 'foobar',
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
    'updated_at': '2017-12-07T20:20:22.005000+03:00',
    'attachments': [
        {
            'id': 5,
            'filename': 'test12.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://5/test12.docx',
            'created_at': '2018-05-22T19:16:21.527000+03:00',
        }
    ],
    'invitees': [],
}


def test_enquiry_comments_retrieve(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/comments/1')
    assert_status(resp, 200)

    assert resp.json() == COMMENT_RETRIEVE


# ------------------------------------------------------------------------------


def test_enquiry_comments_update(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    msg = '21345'
    data = {'message': msg}

    with mock_wf(msg):
        resp = client.patch('/api/enquiries/1/comments/1', data=data)
        assert_status(resp, 200)

    obj = models.EnquiryComment.objects.values('message', 'message_html').get(
        id=1
    )

    assert obj['message'] == msg
    assert obj['message_html'] == msg


# ------------------------------------------------------------------------------


def test_enquiry_comments_delete(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.delete('/api/enquiries/1/comments/1')
    assert_status(resp, 204)

    qs = models.EnquiryComment.objects.filter(id=1)
    assert not qs.exists()


# ------------------------------------------------------------------------------


@pytest.mark.parametrize('method', ('DELETE', 'PATCH'))
def test_enquiry_comments_forbidden(clients, method):

    client = clients['internal']
    prepare_user(client, username='robot-procu-test', roles=['admin'])

    # Just to make sure that the comment is accessible...
    resp = client.get('/api/enquiries/1/comments/1')
    assert_status(resp, 200)

    # ...but we cannot apply the method
    # since the comment was created by another user
    resp = client.generic(method, '/api/enquiries/1/comments/1')
    assert_status(resp, 403)
