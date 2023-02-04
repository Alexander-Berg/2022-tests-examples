import requests
import json
from maps.b2bgeo.test_lib.mock_blackbox import mock_blackbox


def test_unknown_path():
    with mock_blackbox() as bb:
        resp = requests.get(bb + '/unknown')
        assert resp.status_code == 404


def test_oauth_request():
    with mock_blackbox() as bb:
        params = {
            'method': 'oauth'
        }
        headers = {
            'Authorization': 'dummy_oauth:111'
        }
        resp = requests.get(bb + '/blackbox', params=params, headers=headers)
        assert resp.status_code == 200
        data = json.loads(resp.content)
        assert 'login' in data
        assert data['login'] == 'dummy_oauth'
        assert data.get('uid', {}).get('value') == '111'


def test_cookie_request():
    with mock_blackbox() as bb:
        params = {
            'method': 'sessionid',
            'multisession': 'yes',
            'sessionid': 'dummy_cookie',
            'sslsessionid': '123456'
        }
        resp = requests.get(bb + '/blackbox', params=params)
        assert resp.status_code == 200
        data = json.loads(resp.content)
        assert 'users' in data
        assert len(data['users']) == 2
        assert 'default_uid' in data

        def _get_user(users, uid):
            for user in users:
                if user['id'] == uid:
                    return user
            return None

        user = _get_user(data['users'], data['default_uid'])
        assert 'login' in user
        assert user['login'] == 'dummy_cookie'
        assert user.get('uid', {}).get('value') == '123456'
