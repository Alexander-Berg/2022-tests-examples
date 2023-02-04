import mock
import flask
import json
import unittest

from genisys.web import auth

from .base import GenisysWebTestCase


class AuthTestCase(GenisysWebTestCase):
    CONFIG = {
        'BYPASS_AUTH': False
    }

    def setUp(self):
        super(AuthTestCase, self).setUp()

        def testview():
            return flask.jsonify({
                'username': flask.g.username,
                'usergroups': flask.g.usergroups,
                'auth_error': getattr(flask.g, 'auth_error', 'n/a'),
                'group_auth_error': getattr(flask.g, 'group_auth_error', 'n/a'),
            })

        self.app.add_url_rule('/testview', view_func=testview)

    def _resp(self, content):
        class Resp(object):
            def json(self):
                return content
        return Resp()

    def test_no_cookies(self):
        response = self.client.get('/testview')
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://passport.yandex-team.ru/passport?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview')

    def test_both_cookies_valid(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "age": 3647, "expires_in": 7772353, "ttl": "5", "error": "OK",
                "status": {"value": "VALID", "id": 0},
                "uid": {"value":"112"},
                "login": "user-login"
            })
            with mock.patch('genisys.web.auth.get_groups') as mock_get_groups:
                mock_get_groups.return_value = ['group1', 'group2']
                response = self.client.get(
                    '/testview',
                    headers={'Cookie': 'Session_id=csid; sessionid2=csid2'},
                    environ_overrides={'REMOTE_ADDR': '1.2.3.4',
                                       'HTTP_X_FORWARDED_FOR': '3.4.5.6,7.8.9.10'}
                )
        self.assertEquals(response.status_code, 200)
        self.assertEquals(json.loads(response.data.decode('latin1')),
                          {'usergroups': ['group1', 'group2'],
                           'username': 'user-login',
                           'group_auth_error': 'n/a',
                           'auth_error': 'n/a'})
        mock_get.assert_called_once_with(
            'https://blackbox.yandex-team.ru/blackbox?method=sessionid&'
            'sessionid=csid&sslsessionid=csid2&userip=7.8.9.10&'
            'host=test.serv&format=json',
            timeout=3, verify=True, headers={}
        )
        mock_get_groups.assert_called_once_with('user-login', self.config)

    def test_failed_group_auth(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "age": 3647, "expires_in": 7772353, "ttl": "5", "error": "OK",
                "status": {"value": "VALID", "id": 0},
                "uid": {"value":"112"},
                "login": "user-login"
            })
            with mock.patch('genisys.web.auth.get_groups') as mock_get_groups:
                mock_get_groups.side_effect = ZeroDivisionError()
                response = self.client.get(
                    '/testview',
                    headers={'Cookie': 'Session_id=csid; sessionid2=csid2'},
                    environ_overrides={'REMOTE_ADDR': '1.2.3.4'}
                )
        self.assertEquals(response.status_code, 200)
        self.maxDiff = None
        response = json.loads(response.data.decode('latin1'))
        self.assertEquals(response['auth_error'], 'n/a')
        self.assertEquals(response['username'], 'user-login')
        self.assertEquals(response['usergroups'], [])
        self.assertEquals(response['group_auth_error']['exc_class'],
                          'ZeroDivisionError')

    def test_failed_auth(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.side_effect = RuntimeError
            response = self.client.get(
                '/testview',
                headers={'Cookie': 'Session_id=csid; sessionid2=csid2'},
                environ_overrides={'REMOTE_ADDR': '1.2.3.4'}
            )
        self.assertEquals(response.status_code, 200)
        self.maxDiff = None
        response = json.loads(response.data.decode('latin1'))
        self.assertEquals(response['group_auth_error'], 'n/a')
        self.assertEquals(response['username'], None)
        self.assertEquals(response['usergroups'], [])
        self.assertEquals(response['auth_error']['exc_class'],
                          'RuntimeError')

    def test_invalid_cookies(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "status": {"value": "INVALID", "id": 5},
                "error": "signature has bad format or is broken",
                "session_fraud": 0
            })
            response = self.client.get(
                '/testview',
                headers={'Cookie': 'Session_id=csid; sessionid2=csid2'},
                environ_overrides={'REMOTE_ADDR': '1.2.3.4'}
            )
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://passport.yandex-team.ru/passport?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview')

    def test_missing_some_cookies(self):
        response = self.client.get('/testview/2',
                                   headers={'Cookie': 'Session_id=csid'})
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://passport.yandex-team.ru/passport?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview%2F2')

        response = self.client.get('/testview?foo=bar',
                                   headers={'Cookie': 'sessionid2=csid2'})
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://passport.yandex-team.ru/passport?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview%3Ffoo%3Dbar')

    def test_bypass_auth(self):
        self.config['BYPASS_AUTH'] = True
        self.config['BYPASS_AUTH_AS'] = 'someuser'
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            response = self.client.get('/testview',)
        self.assertEquals(response.status_code, 200)
        self.assertEquals(json.loads(response.data.decode('latin1')),
                          {'usergroups': [], 'username': 'someuser',
                           'group_auth_error': 'n/a', 'auth_error': 'n/a'})
        mock_get.assert_not_called()

    def test_renew(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "age": 3647, "expires_in": 7772353, "ttl": "5", "error": "OK",
                "status": {"value": "NEED_RESET", "id": 0},
                "uid": {"value":"112"},
                "login": "user-login"
            })
            response = self.client.get(
                '/testview',
                headers={'Cookie': 'Session_id=csid; sessionid2=csid2'},
                environ_overrides={'REMOTE_ADDR': '1.2.3.4',
                                   'HTTP_X_FORWARDED_FOR': '3.4.5.6,7.8.9.10'}
            )
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://pass.yandex-team.ru/resign?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview')

    def test_oauth_valid(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "connection_id": "t:772",
                "error": "OK", "have_hint": False, "have_password": True,
                "karma": {"value": 0}, "karma_status": {"value": 0},
                "login": "megauser",
                "oauth": {
                    "client_ctime": "2015-09-01 16:37:45",
                    "client_homepage": "https://genisys.yandex-team.ru/",
                    "client_icon": None,
                    "client_id": "12121121112111112111112111111121",
                    "client_name": "genisys",
                    "ctime": "2015-09-02 17:23:19",
                    "device_id": "",
                    "device_name": "",
                    "expire_time": None,
                    "is_ttl_refreshable": False,
                    "issue_time": "2015-11-24 13:41:56",
                    "meta": "",
                    "scope": "staff:read",
                    "token_id": "772",
                    "uid": "1120000000055555"
                },
                "status": {"id": 0, "value": "VALID"},
                "uid": {"hosted": False, "lite": False, "value": "1120055555"}
            })
            with mock.patch('genisys.web.auth.get_groups') as mock_get_groups:
                mock_get_groups.return_value = ['group5']
                response = self.client.get(
                    '/testview',
                    headers={'Authorization': 'OAuth 33556677999000'},
                    environ_overrides={'REMOTE_ADDR': '1.2.3.4',
                                       'HTTP_X_FORWARDED_FOR': '3.4.5.6'}
                )
        self.assertEquals(response.status_code, 200)
        self.assertEquals(json.loads(response.data.decode('latin1')),
                          {'usergroups': ['group5'], 'username': 'megauser',
                           'group_auth_error': 'n/a', 'auth_error': 'n/a'})
        mock_get.assert_called_once_with(
            'https://blackbox.yandex-team.ru/blackbox?method=oauth&'
            'userip=3.4.5.6&format=json',
            timeout=3, verify=True,
            headers={'Authorization': 'OAuth 33556677999000'}
        )

    def test_oauth_invalid(self):
        with mock.patch.object(self.app.blackbox_requests_session, 'get') as mock_get:
            mock_get.return_value = self._resp({
                "error": "expired_token",
                "status": {"id": 5, "value": "INVALID"}
            })
            response = self.client.get(
                '/testview',
                headers={'Authorization': 'OAuth 33556677999000'},
                environ_overrides={'REMOTE_ADDR': '1.2.3.4',
                                   'HTTP_X_FORWARDED_FOR': '3.4.5.6'}
            )
        self.assertEquals(response.status_code, 302)
        self.assertEquals(response.location,
                          'https://passport.yandex-team.ru/passport?retpath='
                          'http%3A%2F%2Ftest.serv%3A500%2Ftestview')


class GetGroupsTestCase(unittest.TestCase):
    CONFIG = {
        'STAFF_HEADERS': {'x-staff-header': 'bzz'},
        'STAFF_TIMEOUT': 4,
        'STAFF_URI': 'https://staff'
    }

    class Response(object):
        def __init__(self, status_code, json):
            self.status_code = status_code
            self.json = lambda: json
        def raise_for_status(self):
            if self.status_code != 200:
                raise Exception(str(self.status_code))

    def test(self):
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = self.Response(200, {'groups': [
                {"group": {"url": "ml-search"}},
                {"group": {"url": "mister-spb"}},
                {"group": {"url": "affiliation-yandex"}},
                {"group": {"url": "spb-searchstaff"}},
                {"group": {"url": "srchstaff"}},
                {"group": {"url": "svc_skynet"}},
                {"group": {"url": "svc_skynet_development"}},
            ]})
            groups = auth.get_groups('megauser', self.CONFIG)
        self.assertEquals(groups, [
            'affiliation-yandex',
            'mister-spb',
            'ml-search',
            'spb-searchstaff',
            'srchstaff',
            'svc_skynet',
            'svc_skynet_development'
        ])
        mock_get.assert_called_once_with(
            'https://staff/persons',
            allow_redirects=False,
            headers={'x-staff-header': 'bzz'},
            params={'_fields': 'groups.group.url',
                    'login': 'megauser',
                    '_one': '1'},
            timeout=4
        )
