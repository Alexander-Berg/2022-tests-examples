import mock
from six.moves.urllib.parse import urlparse
from sepelib.yandex import blackbox

import pytest
from requests import RequestException
import socket


@pytest.fixture
def blackbox_rsp():
    res = {
        'error': u'OK',
        'status': {'value': u'VALID'},
        'dbfields': 'test_data',
        'oauth': {"client_id": u'cli_id', "scope": u'test_scope'}
    }
    return res


def test_smart_str():
    s = 'test_str'
    u = u'test_str'
    b = b'test_str'
    assert blackbox.smart_str(s) == b
    assert blackbox.smart_str(u) == b
    assert blackbox.smart_str(b) == b


# blackbox.urlencode tests are copied from django.tests
def test_urlencode_tuples():
    assert blackbox.urlencode((('a', '1'), ('b', '2'), ('c', '3'))) == 'a=1&b=2&c=3'


def test_urlencode_dict():
    # Copied from django.tests
    # Dictionaries are treated as unordered.
    result = blackbox.urlencode({'a': '1', 'b': '2', 'c': '3'})
    assert result in ['a=1&b=2&c=3', 'a=1&c=3&b=2', 'b=2&a=1&c=3', 'b=2&c=3&a=1', 'c=3&a=1&b=2', 'c=3&b=2&a=1']


def test_urlencode_dict_with_bytes_values():
    assert blackbox.urlencode({'a': b'abc'}) == 'a=abc'


def test_join_url_params():
    fields = ['1', '2', '3']
    ip = '1.2.3.4'
    url = blackbox.join_url_params('http://test.url.com', {'dbfields': fields, 'userip': '::ffff:' + ip})
    query = urlparse(url).query
    assert 'dbfields' in query
    assert 'dbfields=1,2,3' in query.replace('%2C', ',')
    assert 'userip={}'.format(ip) in query


def test_http_get():
    rsp = mock.Mock()
    rsp.status_code = 200
    rsp.headers = {'content-type': 'text/xml'}
    with mock.patch('requests.get', return_value=rsp):
        blackbox._http_get('test.url', params={})


def test_http_not_ok():
    rsp = mock.Mock()
    rsp.status_code = 444
    rsp.headers = {'content-type': 'text/xml'}
    rsp.text = 'test'
    with mock.patch('requests.get', return_value=rsp):
        with pytest.raises(blackbox.BlackboxError):
            txt, ct = blackbox._http_get('test.url', params={})
            assert txt == rsp.text
            assert ct == 'text/html'


def test_http_fail():
    with mock.patch('requests.get', side_effect=RequestException):
        with pytest.raises(blackbox.BlackboxError):
            blackbox._http_get('test.url', params={})

    with mock.patch('requests.get', side_effect=socket.error):
        with pytest.raises(blackbox.BlackboxError):
            blackbox._http_get('test.url', params={})


def test_blackbox_json_call():
    with mock.patch('sepelib.yandex.blackbox._http_get', return_value=('{"test_key": "test_val"}', 'application/json')):
        d = blackbox._blackbox_json_call('url', {})
        assert d == {"test_key": "test_val"}


def test_blackbox_json_call_notjson():
    with mock.patch('sepelib.yandex.blackbox._http_get', return_value=('{}', 'application/notjson')):
        with pytest.raises(blackbox.BlackboxError):
            blackbox._blackbox_json_call('test.url', {})


def test_valiate_session_id_error(blackbox_rsp):
    blackbox_rsp['error'] = 'NOTOK'
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        with pytest.raises(blackbox.BlackboxError):
            blackbox.validate_session_id('sesid', 'userip', 'host')


def test_valiate_session_id(blackbox_rsp):
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        valid, redirect, fields = blackbox.validate_session_id('sesid', 'userip', 'host')
        assert valid
        assert not redirect
        assert fields == 'test_data'


def test_valiate_session_id_redirect(blackbox_rsp):
    blackbox_rsp['status']['value'] = 'NEED_RESET'
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        valid, redirect, fields = blackbox.validate_session_id('sesid', 'userip', 'host')
        assert valid
        assert redirect
        assert fields == 'test_data'


def test_valiate_session_id_not_valid(blackbox_rsp):
    blackbox_rsp['status']['value'] = 'NOAUTH'
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        valid, redirect, fields = blackbox.validate_session_id('sesid', 'userip', 'host')
        assert not valid
        assert redirect
        assert fields is None


def test_validate_oauth_token(blackbox_rsp):
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        v, f, cli_id, scope, err = blackbox.validate_oauth_token('myip', authorization_header='OAuthHeader')
    assert v
    assert f == 'test_data'
    assert cli_id == 'cli_id'
    assert scope == 'test_scope'
    assert err is None


def test_validate_oauth_token_noauth_data():
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', mock.Mock()):
        with pytest.raises(RuntimeError):
            blackbox.validate_oauth_token('myip')


def test_validate_oauth_token_invalid_rsp(blackbox_rsp):
    del blackbox_rsp['oauth']
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        with pytest.raises(blackbox.BlackboxError):
            blackbox.validate_oauth_token('myip', authorization_header='OAuthHeader')


def test_validate_oauth_token_error(blackbox_rsp):
    blackbox_rsp['error'] = u'NOTOK'
    with mock.patch('sepelib.yandex.blackbox._blackbox_json_call', return_value=blackbox_rsp):
        v, f, cli_id, scope, err = blackbox.validate_oauth_token('myip', authorization_header='OAuthHeader')
    assert not v
    assert f is None
    assert cli_id is None
    assert scope is None
    assert err == 'NOTOK'
