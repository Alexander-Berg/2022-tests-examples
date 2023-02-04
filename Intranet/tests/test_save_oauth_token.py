# encoding: utf-8
from __future__ import unicode_literals

import os
import time
from urllib import quote

import pytest
import tornado.web
from mock import patch
from tornado.httpclient import HTTPError

from intranet.webauth.lib import settings
from intranet.webauth.lib.crypto_utils import encrypt_and_sign, get_hmac
from intranet.webauth.lib.save_oauth_token_handler import SaveOAuthTokenHandler


@pytest.fixture
def app():
    application = tornado.web.Application([
        (r'/save_oauth_token', SaveOAuthTokenHandler),
    ])
    return application


@pytest.mark.parametrize('original_url, domain', [
    ('https://qa-test.crowdtest.yandex.ru', '.crowdtest.yandex.ru'),
    ('https://qa-test.crowdtest.yandex-team.ru', '.crowdtest.yandex-team.ru'),
    ('https://qa.test.crowdtest.yandex-team.com.ge', '.crowdtest.yandex-team.com.ge'),
    ('https://qa.test.crowdtest.yandex.com.tr', '.crowdtest.yandex.com.tr'),
    ('https://crowdtest.yandex.ru', '.crowdtest.yandex.ru'),
    ('https://homecrowdtest.yandex.ru', None),
    ('https://qa-test.yandex.ru', None),
    ('https://qa.test.yandex.com.tr', None),
    ('https://qa.test.crowdtest.yandex.comatr', None),
    ('https://qa-test.yandex-team.ru', None),
    ('https://qa-test.yandex-team.comage', None),
])
@pytest.mark.gen_test
def test_set_cookie(http_client, base_url, original_url, domain):
    with patch('os.urandom', return_value=os.urandom(16)):
        message = 'test_token'
        token = quote(encrypt_and_sign(b'{}:{}'.format(int(time.time()), message)))
        encrypted_token = quote(encrypt_and_sign(message))

        csrf_token = b'test_csrf_token'
        state = quote('{}:{}'.format(quote(original_url), get_hmac(csrf_token)))

        with patch.object(SaveOAuthTokenHandler, 'set_cookie') as mocked_set_cookie,\
                patch.object(SaveOAuthTokenHandler, 'get_cookie') as mocked_get_cookie,\
                pytest.raises(HTTPError) as err:
            mocked_get_cookie.return_value = csrf_token
            yield http_client.fetch(
                base_url + '/save_oauth_token?token={}&state={}'.format(token, state),
                follow_redirects=False,
            )

    assert err.value.code == 302
    assert err.value.response.headers.get('Location') == original_url

    mocked_set_cookie.assert_any_call(
        name=settings.WEBAUTH_CLIENT_TOKEN_COOKIE,
        value=encrypted_token,
        domain=domain,
        secure=True,
        httponly=True,
        samesite='None',
    )
