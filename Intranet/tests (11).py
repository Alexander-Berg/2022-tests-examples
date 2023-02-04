# coding: utf-8

import io

import pytest

from procu.api import models
from procu.utils.test import assert_status, get_file_content, prepare_user

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize(
    'env,username', (('internal', 'robot-procu'), ('external', 's001@procu.ru'))
)
def test_attachment_create(clients, env, username):

    client = clients[env]
    prepare_user(client, username=username)

    content = 'foobarfoobarfoobarfoobarfoobarfoobarfoobarfoobarfoobarfoobar'

    fp = io.StringIO(content)
    fp.name = 'foo.txt'

    resp = client.post('/api/attachments', {'file': fp}, format='multipart')
    assert_status(resp, 201)

    attachment_id = resp.json()['id']

    file = models.Attachment.objects.get(id=attachment_id).file

    assert file.read().decode() == content


@pytest.mark.parametrize(
    'env,username,roles',
    (
        ('internal', 'robot-procu', ('admin',)),
        ('external', 's001@procu.ru', ()),
    ),
)
def test_attachment_retrieve(clients, env, username, roles):

    client = clients[env]
    prepare_user(client, username=username, roles=roles)

    # Assign the attachment to the request that is available to both users.
    models.Attachment.objects.filter(id=1).update(request_id=1)

    resp = client.get('/api/attachments/1')
    assert_status(resp, 200)

    assert resp.content == get_file_content(1)
