import mock
import pytest
from sepelib.yandex.oauth import TokenChecker, OAuthException


def test_token_checker():
    t = TokenChecker('test_id', ['direct:api-1month', 'metrika:write'])
    assert t.check('test_id')
    assert t.check('some_id', 'direct:api-1month')
    with pytest.raises(OAuthException):
        t.check('id')


def test_get_token_by_authorization_code(oauth_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={'access_token': 'token', 'expires': 100})
    with mock.patch('requests.post', return_value=r):
        oauth_client_mock.get_token_by_authorization_code(1)


def test_get_token_by_authorization_code_no_access(oauth_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={})
    with mock.patch('requests.post', return_value=r):
        with pytest.raises(OAuthException):
            oauth_client_mock.get_token_by_authorization_code(2)
