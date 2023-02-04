# coding: utf-8
import requests
import pytest
import json

import yatest.common

import ads.quality.phf.phf_direct_loader.lib.auth as auth

from ads.quality.phf.phf_direct_loader.lib.app import make_app
from ads.quality.phf.phf_direct_loader.lib.config import TestingConfig
from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    init_test_database, TEST_TEMPLATE, CORRECT_TEMPLATES_PATH)

TEST_LOGIN = 'npytincev'  # this login has distribution permission in real IDM
TEST_PSWD = 'qwas2332'
SESSION_ID_COOKIE_NAME = 'Session_id'
USER_IP = '127.0.0.1'


class TestConfigWithAuth(TestingConfig):
    DIRECT_CLIENT_LOGIN = yatest.common.get_param('DIRECT_TEST_CLIENT')
    DIRECT_TOKEN = yatest.common.get_param('DIRECT_TEST_TOKEN')
    PERMISSION_MANAGER = auth.CryptaPermissionManager('http://api.crypta.yandex.net', yatest.common.get_param('CRYPTA_TOKEN'))
    AUTH_ENABLED = True


def get_session_id():
    resp = requests.post('https://passport-test.yandex.ru/auth',
                         {'login': TEST_LOGIN,
                          'passwd': TEST_PSWD})

    session_id = None

    if SESSION_ID_COOKIE_NAME in resp.cookies:
        session_id = resp.cookies[SESSION_ID_COOKIE_NAME]
    else:
        if resp.history is not None:
            for h in resp.history:
                if SESSION_ID_COOKIE_NAME in h.cookies:
                    session_id = h.cookies[SESSION_ID_COOKIE_NAME]
                    break

    if session_id is None:
        raise ValueError('Can not get session id!')

    return session_id


def test_get_login_correct():
    session_id = get_session_id()

    assert TestConfigWithAuth.BLACKBOX_MANAGER.get_login(session_id,
                                                         USER_IP,
                                                         TestConfigWithAuth.AUTH_USER_DOMAIN) == TEST_LOGIN


def test_get_login_auth_error():
    with pytest.raises(auth.AuthError):
        TestConfigWithAuth.BLACKBOX_MANAGER.get_login('bad_session_id',
                                                      USER_IP,
                                                      TestConfigWithAuth.AUTH_USER_DOMAIN)


def test_check_distribution_permission_correct_login():
    assert TestConfigWithAuth.PERMISSION_MANAGER.check(TEST_LOGIN, 'distribution')


def check_distribution_permission_bad_login():
    assert not TestConfigWithAuth.PERMISSION_MANAGER.check('some_login', 'distribution')


def make_app_client_with_auth():
    return make_app(TestConfigWithAuth).test_client()


def test_api_auth_error():
    client = make_app_client_with_auth()
    assert client.get("clients", environ_base={'REMOTE_ADDR': USER_IP}).status_code == 403


def test_api_auth_no_error():
    client = make_app_client_with_auth()
    init_test_database()

    session_id = get_session_id()

    client.set_cookie('test', 'Session_id', session_id)
    assert client.post(CORRECT_TEMPLATES_PATH,
                       data=json.dumps(TEST_TEMPLATE),
                       content_type='application/json',
                       environ_base={'REMOTE_ADDR': USER_IP}).status_code == 200
