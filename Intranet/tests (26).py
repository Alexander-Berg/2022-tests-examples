# coding: utf-8

import re

import pytest
from django.contrib.auth.tokens import default_token_generator
from django.core import mail

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


USER_INTERNAL = {
    'id': 1,
    'username': 'robot-procu',
    'full_name': 'Робот Закупок',
    'is_staff': True,
}

USER_EXTERNAL = {
    'id': 2,
    'username': 's001@procu.ru',
    'email': 's001@procu.ru',
    'full_name': 'Test User-001',
    'first_name': 'Test',
    'last_name': 'User-001',
    'is_staff': False,
    'is_deleted': False,
    'is_clickable': False,
}


@pytest.mark.parametrize(
    'env,username,data',
    (
        ('internal', 'robot-procu', USER_INTERNAL),
        ('external', 's001@procu.ru', USER_EXTERNAL),
    ),
)
def test_current(clients, env, username, data):

    client = clients[env]
    prepare_user(client, username=username)

    resp = client.get('/api/users/current')

    assert_status(resp, 200)
    assert resp.json() == data


# ------------------------------------------------------------------------------


@pytest.mark.parametrize('env', ('internal', 'external'))
def test_csrf(clients, env):

    client = clients[env]

    resp = client.get('/api/users/csrf')
    assert_status(resp, 200)

    token_cookie = getattr(
        resp.cookies.get('csrftoken'), 'value', '__missing__'
    )

    assert resp.json() == {'results': token_cookie}


# ------------------------------------------------------------------------------


class TestChangePassword:
    def test_changed(self, clients):

        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        new_password = '1234567'

        data = {
            'old_password': '123456',
            'new_password': new_password,
            'new_password_confirmation': new_password,
        }

        expected = {'result': 'Пароль изменён'}

        # Change password
        resp = client.post('/api/users/change_password', data=data)
        assert_status(resp, 200)
        assert resp.json() == expected

        # Check if we are still authenticated
        resp = client.get('/api/users/current')
        assert_status(resp, 200)

        # Check if the password has really changed
        user = models.User.objects.get(username='s001@procu.ru')
        assert user.check_password(new_password)

    def test_wrong_old_password(self, clients):
        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        data = {
            'old_password': '1234567',
            'new_password': '1234567',
            'new_password_confirmation': '1234567',
        }

        expected = {
            'old_password': ['Неверный пароль'],
            'detail': '[old_password]: Неверный пароль',
        }

        resp = client.post('/api/users/change_password', data=data)
        assert_status(resp, 400)
        assert resp.json() == expected

    def test_new_passwords_do_not_match(self, clients):

        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        data = {
            'old_password': '123456',
            'new_password': '1234567',
            'new_password_confirmation': '12345678',
        }

        expected = {
            'new_password_confirmation': ['Пароли не совпадают'],
            'detail': '[new_password_confirmation]: Пароли не совпадают',
        }

        resp = client.post('/api/users/change_password', data=data)
        assert_status(resp, 400)
        assert resp.json() == expected

    def test_password_cannot_be_blank(self, clients):

        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        data = {
            'old_password': '123456',
            'new_password': '',
            'new_password_confirmation': '',
        }

        resp = client.post('/api/users/change_password', data=data)
        assert_status(resp, 400)


class TestRestore:
    def test_get_email(self, clients):

        mail.outbox = []

        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        resp = client.post(
            '/api/users/restore/email', data={'email': 'foo@procu.ru'}
        )
        assert_status(resp, 200)
        assert 'result' in resp.json()
        assert not mail.outbox

        resp = client.post(
            '/api/users/restore/email', data={'email': 's001@procu.ru'}
        )
        assert_status(resp, 200)
        assert mail.outbox

        msg = mail.outbox[-1]
        assert 'token=' in msg.body

    def test_restore_password(self, clients):

        client = clients['external']
        prepare_user(client, username='s001@procu.ru', csrf=True)

        user = models.User.objects.get(username='s001@procu.ru')

        token = default_token_generator.make_token(user)
        new_password = '123123'

        resp = client.post(
            f'/api/users/restore/password?user={user.id}&token={token}',
            data={
                'password': new_password,
                'password_confirmation': new_password,
            },
        )
        assert_status(resp, 200)

        user.refresh_from_db()
        assert user.check_password(new_password)


def test_internal_login_authorised(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu')

    resp = client.get('/api/users/yalogin')
    assert_status(resp, 200)

    assert 'url' not in resp.json()


def test_internal_login_unauthorised(clients):

    client = clients['internal']

    resp = client.get('/api/users/yalogin')
    assert_status(resp, 401)

    assert 'url' in resp.json()


@pytest.mark.parametrize('pwd', ('123456', '1234567'))
def test_external_login(clients, pwd):
    client = clients['external']

    resp = client.post(
        '/api/users/login', data={'username': 's001@procu.ru', 'password': pwd}
    )

    if resp.status_code == 200:
        # Check that authentication is successful

        client.cookies.update(resp.cookies)

        resp = client.get('/api/users/current')
        assert_status(resp, 200)

    else:
        assert_status(resp, 401)


def test_external_logout(clients):

    client = clients['external']
    prepare_user(client, username='s001@procu.ru', csrf=True)

    resp = client.post('/api/users/logout')
    assert_status(resp, 200)

    client.cookies.update(resp.cookies)

    resp = client.get('/api/users/current')
    assert_status(resp, 401)
