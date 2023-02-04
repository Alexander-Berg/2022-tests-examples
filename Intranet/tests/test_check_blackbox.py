# encoding: utf-8
from __future__ import unicode_literals

import itertools
import json
import urlparse

import pytest
import tornado.httpclient
from tornado import gen

from intranet.webauth.lib import step
from intranet.webauth.tests.utils import MockResponse, MockRequest

good_creds = {'Session_id': 'good_Session_id', 'sessionid2': 'good_sessionid2'}
lone_secure_creds = {'Session_id': 'secure_Session_id'}
lone_insecure_creds = {'Session_id': 'insecure_Session_id'}
good_token_header = {'Authorization': 'OAuth good_token'}
ext_good_creds = {'Session_id': 'ext_good_Session_id', 'sessionid2': 'ext_good_sessionid2'}
ext_lone_secure_creds = {'Session_id': 'ext_secure_Session_id'}
ext_lone_insecure_creds = {'Session_id': 'ext_insecure_Session_id'}
ext_good_token_header = {'Authorization': 'OAuth ext_good_token'}
invalid_token_header = {'Authorization': '123456'}
alternative_good_token_header = {'Webauth-Authorization': 'OAuth good_token'}
alternative_invalid_token_header = {'Webauth-Authorization': '12312312'}
user_ip = "0:0:0:0:0:0:0:1"

internal_hosts = ["test.yandex-team.ru", "blabla.testing.yandex-team.ru"]
external_hosts = ["test.yandex-team.com", "yandex.net", "bla.yandex.net", "yandex.ru", "ba.yandex.ru"]
HOST = None


# TODO: check internal/external blackbox instances


class MockHTTPClient(object):
    def is_token_good(self, token):
        if HOST in internal_hosts:
            good_tokens = [good_token_header['Authorization'][6:]]
        else:
            good_tokens = [ext_good_token_header['Authorization'][6:]]
        return token in good_tokens

    def is_token_bad(self, token):
        if HOST in internal_hosts:
            bad_tokens = [ext_good_token_header['Authorization'][6:]]
        else:
            bad_tokens = [good_token_header['Authorization'][6:]]
        return token in bad_tokens

    def are_cookies_bad(self, sessionid, sessionid2):
        if HOST in internal_hosts:
            bad_cookies = [(ext_good_creds['Session_id'], ext_good_creds['sessionid2']),
                           (ext_lone_secure_creds['Session_id'], ''),
                           (ext_lone_insecure_creds['Session_id'], '')]
        else:
            bad_cookies = [(good_creds['Session_id'], good_creds['sessionid2']),
                           (lone_secure_creds['Session_id'], ''),
                           (lone_insecure_creds['Session_id'], '')]
        return (sessionid, sessionid2) in bad_cookies

    def are_cookies_secure(self, sessionid, sessionid2):
        if HOST in internal_hosts:
            secure_cookies = [(good_creds['Session_id'], good_creds['sessionid2']),
                              (lone_secure_creds['Session_id'], '')]
        else:
            secure_cookies = [(ext_good_creds['Session_id'], ext_good_creds['sessionid2']),
                              (ext_lone_secure_creds['Session_id'], '')]
        return (sessionid, sessionid2) in secure_cookies

    @gen.coroutine
    def fetch(self, url):
        if type(url) == tornado.httpclient.HTTPRequest:
            url = url.url
        good_response = '{"status":{"value":"VALID"},"dbfields":{"accounts.login.uid":"dummy-user"},"auth":{"secure":true},"uid":{"value":"testuid"}}'
        insecure_response = '{"status":{"value":"VALID"},"dbfields":{"accounts.login.uid":"dummy-user"},"auth":{"secure":false},"uid":{"value":"testuid"}}'
        bad_response = '{"status":{"value":"INVALID"}}'
        query = urlparse.parse_qs(urlparse.urlsplit(url)[3])
        method = query.get('method', [''])[0]
        if method == 'oauth':
            token = query.get('oauth_token', [''])[0]
            if self.is_token_good(token):
                raise gen.Return(MockResponse(good_response))
            if self.is_token_bad(token):
                raise gen.Return(MockResponse(bad_response))
            assert False  # we should not reach this point (oauth method)
        elif method == 'sessionid':
            sessionid = query.get('sessionid', [''])[0]
            sessionid2 = query.get('sslsessionid', [''])[0]
            if self.are_cookies_bad(sessionid, sessionid2):
                raise gen.Return(MockResponse(bad_response))
            if not self.are_cookies_secure(sessionid, sessionid2):
                raise gen.Return(MockResponse(insecure_response))
            raise gen.Return(MockResponse(good_response))
        else:
            assert False  # there should always be one of these methods


class MockRedisClientPool(object):
    def __init__(self):
        self.storage = {}
        self.reads = 0
        self.writes = 0

    @gen.coroutine
    def connected_client(self):
        raise gen.Return(self)

    def __enter__(self):
        return self

    def __exit__(self, *args, **kwargs):
        pass

    @gen.coroutine
    def call(self, cmd, *args):
        assert cmd in ('GET', 'SET', 'SETEX')
        if cmd == 'GET':
            self.reads += 1
            result = self.storage.get(args[0])
        elif cmd == 'SET':
            self.writes += 1
            key = args[0]
            value = args[1]
            self.storage[key] = value
            result = None
        elif cmd == 'SETEX':
            self.writes += 1
            key = args[0]
            # exptime = args[1]
            value = args[2]
            self.storage[key] = value
            result = None
        raise gen.Return(result)


def mock_hash(message):
    return message


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.parametrize('header', [{}, invalid_token_header])
@pytest.mark.gen_test
def test_invalid_token(mocker, monkeypatch, header, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.TokenStep(MockRequest(header, host=host), user_ip).check()
    assert bb_status is None
    assert bb_info == 'No OAuth token provided'
    assert redis_pool.reads == 0
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.gen_test
def test_valid_alternarive_internal_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    headers = {}
    headers.update(alternative_good_token_header)
    headers.update(invalid_token_header)

    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(headers, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('dummy-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 1
    assert len(redis_pool.storage) == 1
    assert redis_pool.storage.keys()[0].startswith('credentials/token/internal/')
    assert redis_pool.storage.values()[0] == json.dumps(('dummy-user', 'testuid'))


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.gen_test
def test_valid_alternarive_internal_token_ext_auth(mocker, monkeypatch, host):
    global HOST
    HOST = host
    headers = {}
    headers.update(alternative_good_token_header)
    headers.update(ext_good_token_header)

    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(headers, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('dummy-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 1
    assert len(redis_pool.storage) == 1
    assert redis_pool.storage.keys()[0].startswith('credentials/token/internal/')
    assert redis_pool.storage.values()[0] == json.dumps(('dummy-user', 'testuid'))


@pytest.mark.parametrize('host', external_hosts)
@pytest.mark.gen_test
def test_valid_alternarive_token_ext_domain(mocker, monkeypatch, host):
    global HOST
    HOST = host
    headers = {}
    headers.update(alternative_good_token_header)
    headers.update(invalid_token_header)

    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(headers, host=host), user_ip).check()
    assert bb_status is False
    assert bb_user_info == 'Can not validate token via blackbox'
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', external_hosts)
@pytest.mark.gen_test
def test_invalid_alternarive_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    headers = {}
    headers.update(alternative_invalid_token_header)
    headers.update(good_token_header)

    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.TokenStep(MockRequest(headers, host=host), user_ip).check()
    assert bb_status is None
    assert bb_info == 'No OAuth token provided'
    assert redis_pool.reads == 0
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.gen_test
def test_bad_internal_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.TokenStep(MockRequest(ext_good_token_header, host=host), user_ip).check()
    assert bb_status is False
    assert bb_info == 'Can not validate token via blackbox'
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', external_hosts)
@pytest.mark.gen_test
def test_bad_external_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.TokenStep(MockRequest(good_token_header, host=host), user_ip).check()
    assert bb_status is False
    assert bb_info == 'Can not validate token via blackbox'
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.gen_test
def test_good_internal_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(good_token_header, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('dummy-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 1
    assert len(redis_pool.storage) == 1
    assert redis_pool.storage.keys()[0].startswith('credentials/token/internal/')
    assert redis_pool.storage.values()[0] == json.dumps(('dummy-user', 'testuid'))


@pytest.mark.parametrize('host', external_hosts)
@pytest.mark.gen_test
def test_good_external_token(mocker, monkeypatch, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(ext_good_token_header, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('dummy-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 1
    assert len(redis_pool.storage) == 1
    assert redis_pool.storage.keys()[0].startswith('credentials/token/external/')
    assert redis_pool.storage.values()[0] == json.dumps(('dummy-user', 'testuid'))


@pytest.mark.parametrize('host', [internal_hosts[0], external_hosts[0]])
@pytest.mark.gen_test
def test_token_in_cache(mocker, monkeypatch, host):
    global HOST
    HOST = host
    location = 'internal' if host in internal_hosts else 'external'
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    mocker.patch('intranet.webauth.lib.step.get_hash', side_effect=mock_hash)
    token_value = good_token_header['Authorization'][6:]
    cache_key = 'credentials/token/{}/<NONE>/{}'.format(location, token_value)
    redis_pool.storage[cache_key] = json.dumps(('cached-user', 'testuid'))
    bb_status, bb_user_info = yield step.TokenStep(MockRequest(good_token_header, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('cached-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0


internal_bad_cookies_group = [(lone_insecure_creds, 'Can not validate secure cookie via blackbox'),
                              (ext_good_creds, 'Can not validate cookies via blackbox'),
                              (ext_lone_insecure_creds, 'Can not validate cookies via blackbox'),
                              (ext_lone_secure_creds, 'Can not validate cookies via blackbox')]
external_bad_cookies_group = [(ext_lone_insecure_creds, 'Can not validate secure cookie via blackbox'),
                              (good_creds, 'Can not validate cookies via blackbox'),
                              (lone_insecure_creds, 'Can not validate cookies via blackbox'),
                              (lone_secure_creds, 'Can not validate cookies via blackbox')]
bad_cookies_params = [(x, y[0], y[1]) for x, y in list(itertools.product(internal_hosts, internal_bad_cookies_group)) +
                      list(itertools.product(external_hosts, external_bad_cookies_group))]


@pytest.mark.parametrize('host,cookies,info', bad_cookies_params)
@pytest.mark.gen_test
def test_bad_sessionid(mocker, monkeypatch, cookies, info, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.CookiesStep(MockRequest(cookies=cookies, host=host), user_ip).check()
    assert bb_status is False
    assert bb_info == info
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0


@pytest.mark.parametrize('host', internal_hosts)
@pytest.mark.gen_test
def test_no_sessionid(mocker, monkeypatch, host):
    global HOST
    HOST = host
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_info = yield step.CookiesStep(MockRequest(host=host), user_ip).check()
    assert bb_status is None
    assert bb_info == 'No SessionID in cookies'
    assert redis_pool.reads == 0
    assert redis_pool.writes == 0


external_good_cookies_group = [ext_good_creds, ext_lone_secure_creds]
internal_good_cookies_group = [good_creds, lone_secure_creds]
good_cookies_params = [(x, y) for x, y in list(itertools.product(internal_hosts, internal_good_cookies_group)) +
                       list(itertools.product(external_hosts, external_good_cookies_group))]


@pytest.mark.parametrize('host,cookies', good_cookies_params)
@pytest.mark.gen_test
def test_good_sessionid(mocker, monkeypatch, cookies, host):
    global HOST
    HOST = host
    location = 'internal' if host in internal_hosts else 'external'
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    bb_status, bb_user_info = yield step.CookiesStep(MockRequest(cookies=cookies, host=host), user_ip).check()
    assert bb_status
    assert bb_user_info == ('dummy-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 1
    assert len(redis_pool.storage) == 1
    assert redis_pool.storage.keys()[0].startswith('credentials/cookies/{}/'.format(location))
    assert redis_pool.storage.values()[0] == json.dumps(('dummy-user', 'testuid'))


@pytest.mark.parametrize('host,cookies', good_cookies_params)
@pytest.mark.gen_test
def test_sessionid_in_cache(mocker, monkeypatch, cookies, host):
    global HOST
    HOST = host
    location = 'internal' if host in internal_hosts else 'external'
    redis_pool = MockRedisClientPool()
    monkeypatch.setattr('intranet.webauth.lib.utils._redis_instance', redis_pool)
    mocker.patch("tornado.httpclient.AsyncHTTPClient", side_effect=MockHTTPClient)
    step_object = step.CookiesStep(MockRequest(cookies=cookies, host=host), user_ip)
    cache_key = step_object.get_cache_key(
        session_id=cookies.get('Session_id'),
        session_id2=cookies.get('sessionid2')
    )
    assert cache_key.startswith('credentials/cookies/{}/<NONE>/'.format(location))
    redis_pool.storage[cache_key] = json.dumps(('cached-user', 'testuid'))

    bb_status, bb_user_info = yield step_object.check()
    assert bb_status
    assert bb_user_info == ('cached-user', 'testuid')
    assert redis_pool.reads == 1
    assert redis_pool.writes == 0
