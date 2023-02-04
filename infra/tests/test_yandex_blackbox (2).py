import json
import requests
import requests.structures
import pytest
import six

from sepelib.yandex import blackbox


def make_fake_session(response_body):
    response = requests.Response()
    response.status_code = 200
    response._content = six.ensure_binary(json.dumps(response_body), 'utf-8')
    response.headers = requests.structures.CaseInsensitiveDict(
        data={'Content-Type': 'application/json'}
    )

    class Session(object):
        def get(self, *args, **kwargs):
            return response

    return Session()


def test_error_response():
    response_body = {
        'error': 'BlackBox error: Missing or empty oauth token',
        'exception': {
            'id': 2,
            'value': ''
        }
    }
    session = make_fake_session(response_body)
    valid, fields, client_id, scope, error = blackbox.validate_oauth_token('127.0.0.1',
                                                                           oauth_token='OAuth',
                                                                           session=session)
    assert valid is False
    assert fields is None
    assert client_id is None
    assert scope is None
    assert error == response_body['error']


def test_good_response():
    response_body = {u'connection_id': u't:234234234',
                     u'dbfields': {u'accounts.login.uid': u'nekto0n'},
                     u'error': u'OK',
                     u'have_hint': False,
                     u'have_password': True,
                     u'karma': {u'value': 0},
                     u'karma_status': {u'value': 0},
                     u'login': u'nekto0n',
                     u'oauth': {u'client_ctime': u'2014-08-13 12:06:56',
                                u'client_homepage': u'',
                                u'client_icon': None,
                                u'client_id': u'1111',
                                u'client_name': u'dev-nanny API',
                                u'ctime': u'2015-02-10 15:45:48',
                                u'device_id': u'',
                                u'device_name': u'',
                                u'expire_time': None,
                                u'is_ttl_refreshable': False,
                                u'issue_time': u'2015-12-25 10:48:55',
                                u'meta': u'',
                                u'scope': u'staff:read',
                                u'token_id': u'234345345',
                                u'uid': u'234234234'},
                     u'status': {u'id': 0, u'value': u'VALID'},
                     u'uid': {u'hosted': False, u'lite': False, u'value': u'34534545454'}}
    session = make_fake_session(response_body)
    valid, fields, client_id, scope, error = blackbox.validate_oauth_token('127.0.0.1',
                                                                           oauth_token='OAuth token value',
                                                                           session=session)
    assert valid is True
    assert fields == {u'accounts.login.uid': u'nekto0n'}
    assert client_id == '1111'
    assert scope == 'staff:read'
    assert error is None


def test_invalid_response():
    response_body = {'some': 'body'}
    session = make_fake_session(response_body)
    with pytest.raises(blackbox.BlackboxError):
        blackbox.validate_oauth_token('127.0.0.1',
                                      oauth_token='OAuth token value',
                                      session=session)
