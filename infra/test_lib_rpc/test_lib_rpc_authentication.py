from __future__ import unicode_literals
import contextlib

import inject
import pytest

from sepelib.core import config
from sepelib.yandex import oauth

from infra.swatlib.auth import passport
from awacs.lib import rpc

EMPTY_REQUEST = rpc.authentication.Request('/', 'host', '127.0.0.1', None, '', '')


@contextlib.contextmanager
def toggle_auth(value):
    old = config.get_value('run.auth')
    config.set_value('run.auth', value)
    yield
    config.set_value('run.auth', old)


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def test_auth_disable_works():
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    with toggle_auth(False):
        auth_subject = auth.authenticate_request(EMPTY_REQUEST)
    assert auth_subject.login == 'anonymous'


def test_oauth_no_header_or_cookie():
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    with toggle_auth(True), pytest.raises(rpc.exceptions.UnauthenticatedError):
        auth.authenticate_request(EMPTY_REQUEST)


def test_oauth_in_cache():
    auth_header = 'token'
    login = 'bumblebee'
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    auth.oauth_header_cache[auth_header] = login
    request = rpc.authentication.Request('/', 'host', '127.0.0.1', auth_header, '', '')
    with toggle_auth(True):
        auth_subject = auth.authenticate_request(request)
    assert auth_subject.login == login


def test_oauth_client_error_handling():
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()

    class ErrorRaisingOAuthClient(oauth.IOAuth):
        def get_user_login_by_authorization_header(self, *args, **kwargs):
            raise Exception('That did not work')

    class PassportClientStub(passport.IPassportClient):
        pass

    auth.oauth_client = ErrorRaisingOAuthClient()
    auth.passport_client = PassportClientStub()

    request = rpc.authentication.Request('/', 'host', '127.0.01', 'OAuth token', '', '')
    with toggle_auth(True), pytest.raises(rpc.exceptions.UnauthenticatedError):
        auth.authenticate_request(request)


def test_oauth_ok():
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    login = 'username'
    auth_header = 'OAuth token'

    class OAuthClient(oauth.IOAuth):
        def get_user_login_by_authorization_header(self, *args, **kwargs):
            return login

    class PassportClientStub(passport.IPassportClient):
        pass

    auth.oauth_client = OAuthClient()
    auth.passport_client = PassportClientStub()  # It is passed to oauth client upon call

    request = rpc.authentication.Request('/', 'host', '127.0.01', auth_header, '', '')
    with toggle_auth(True):
        auth_subject = auth.authenticate_via_oauth(request)
    assert auth_subject.login == login
    # Check that login is cached
    assert auth_header in auth.oauth_header_cache


def test_session_id_no_cookie():
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    with toggle_auth(True):
        assert auth.authenticate_via_session_cookie(EMPTY_REQUEST) is None


def test_session_id_in_cache():
    session_id = 'dfasdfrdvasdfasdfadf'
    login = 'bumblebee'
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()
    auth.session_id_cache[session_id] = login
    request = rpc.authentication.Request('/', 'host', '127.0.0.1', '', session_id, '')
    with toggle_auth(True):
        auth_subject = auth.authenticate_via_session_cookie(request)
    assert auth_subject.login == login


def test_passport_client_error_handling():
    session_id = 'dfasdfrdvasdfasdfadf'
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()

    class RaisingPassportClient(object):
        def check_passport_cookie(self, *args, **kwargs):
            raise Exception('That did not work')

    auth.passport_client = RaisingPassportClient()
    request = rpc.authentication.Request('/', 'host', '127.0.0.1', '', session_id, '')
    with toggle_auth(True), pytest.raises(rpc.exceptions.UnauthenticatedError):
        auth.authenticate_via_session_cookie(request)


def test_passport_redirect_url():
    login = 'bumble_bee'
    redirect_url = 'https://passport.yandex-team.ru/auth?'
    session_id = 'dfasdfrdvasdfasdfadf'
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()

    class RedirectingPassportClient(object):
        def check_passport_cookie(self, *args, **kwargs):
            return passport.PassportCheckResult(login, redirect_url)

    auth.passport_client = RedirectingPassportClient()
    request = rpc.authentication.Request('/', 'host', '127.0.0.1', '', session_id, '')
    with toggle_auth(True), pytest.raises(rpc.exceptions.UnauthenticatedError) as e:
        auth.authenticate_via_session_cookie(request)
    assert e.value.redirect_url == redirect_url


def test_session_id_ok():
    login = 'bumble_bee'
    session_id = 'dfasdfrdvasdfasdfadf'
    auth = rpc.authentication.CachingPassportAuthenticator.from_inject()

    class OkPassportClient(object):
        def check_passport_cookie(self, *args, **kwargs):
            return passport.PassportCheckResult(login, None)

    auth.passport_client = OkPassportClient()
    request = rpc.authentication.Request('/', 'host', '127.0.0.1', '', session_id, '')
    with toggle_auth(True):
        auth_subject = auth.authenticate_request(request)
    assert auth_subject.login == login
    # Check that cache is filled
    assert auth.session_id_cache[session_id] == login
