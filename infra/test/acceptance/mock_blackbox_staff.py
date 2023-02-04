import logging
import json
import re
import urllib.parse

from gevent.pywsgi import WSGIServer


class MockBlackbox(object):
    def __init__(self, listen_address, cookie_pairs):
        self.cookie_pairs = cookie_pairs
        self.invalid_result = json.dumps({
            'error': 'expired_token',
            'status': {'value': 'INVALID'},
        }).encode('latin1')
        self.server = WSGIServer(listen_address, self._handle_request,
                                 log=logging.getLogger('mockbb'))

    def _handle_request(self, env, start_response):
        # genisys/web/auth.py:check_auth()
        if env['PATH_INFO'] != '/' or env['REQUEST_METHOD'] != 'GET':
            start_response('400 Bad Request', [])
            return [b'Bad Request']
        qs = {k: v[-1]
              for k, v in urllib.parse.parse_qs(env['QUERY_STRING']).items()
              if v}
        if not qs.get('host').startswith('127.0.0.1') \
                or not qs.get('userip') == '127.0.0.1' \
                or not qs.get('format') == 'json' \
                or not qs.get('method') == 'sessionid' \
                or 'sessionid' not in qs \
                or 'sslsessionid' not in qs:
            start_response('400 Bad Request', [])
            return [b'Bad query params']
        start_response('200 OK', [])
        cookie_pair = (qs['sessionid'], qs['sslsessionid'])
        if cookie_pair in self.cookie_pairs:
            valid_result = json.dumps({
                'error': 'OK',
                'status': {'value': 'VALID'},
                'login': self.cookie_pairs[cookie_pair]
            }).encode('latin1')
            return [valid_result]
        else:
            return [self.invalid_result]

    def start(self):
        self.server.start()

    def stop(self):
        self.server.stop()

    def serve_forever(self):
        self.server.serve_forever()


class MockStaff(object):
    def __init__(self, listen_address, auth_header,
                 valid_usernames, valid_groupnames):
        self.auth_header = auth_header
        self.valid_usernames = set(valid_usernames)
        self.valid_groupnames = set(valid_groupnames)
        self.server = WSGIServer(listen_address, self._handle_request,
                                 log=logging.getLogger('mockstaff'))

    def _handle_request(self, env, start_response):
        if env['PATH_INFO'] not in ('/persons', '/groups') \
                or env['REQUEST_METHOD'] != 'GET':
            start_response('400 Bad Request', [])
            return [b'Bad Request']
        if not env.get('HTTP_AUTHORIZATION') == self.auth_header:
            start_response('401 Unauthorized', [])
            return [b'Wrong or missing "Authorization" header']
        qs = {k: v[-1]
              for k, v in urllib.parse.parse_qs(env['QUERY_STRING']).items()
              if v}
        result = None
        if env['PATH_INFO'] == '/persons':
            # /persons
            if set(qs) == set(('login', '_one', '_fields')) \
                    and qs['_one'] == '1' \
                    and qs['_fields'] == 'groups.group.url':
                # genisys/web/auth.py:get_groups()
                if qs['login'] in self.valid_usernames:
                    result = {'groups': []}
                else:
                    result = {}
            elif set(qs) == set(('login', '_fields',
                                 'official.is_dismissed', '_limit')) \
                    and qs['_fields'] == 'login' \
                    and qs['official.is_dismissed'] == 'false':
                # genisys/web/forms.py:UserListField._validate_data()
                logins = set(qs['login'].split(','))
                valid_logins = logins.intersection(self.valid_usernames)
                result = {'result': [{'login': login}
                                     for login in valid_logins]}
        else:
            # /groups
            if set(qs) == set(('_fields', 'is_deleted', '_query', '_limit')) \
                    and qs['_fields'] == 'url' \
                    and qs['is_deleted'] == 'false':
                # genisys/web/forms.py:UserListField._validate_data()
                m = re.match(r"^url in \[(.*)\]$", qs['_query'])
                if m:
                    [groupnames] = m.groups()
                    groupnames = {g.strip("'") for g in groupnames.split(',')}
                    valid_groupnames = groupnames.intersection(
                        self.valid_groupnames
                    )
                    result = {'result': [{'url': groupname}
                                         for groupname in valid_groupnames]}
        if result is None:
            start_response('400 Bad Request', [])
            return [b'Request was not understood']
        start_response('200 OK', [('Content-Type', 'application/json')])
        return [json.dumps(result).encode('latin1')]

    def start(self):
        self.server.start()

    def stop(self):
        self.server.stop()

    def serve_forever(self):
        self.server.serve_forever()


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    print('Serving MockBlackbox on 127.0.0.1:7082')
    mock_blackbox = MockBlackbox(('127.0.0.1', 7082), {
        ('1', '1'): 'user1',
        ('2', '2'): 'user2',
    })
    print('Serving MockStaff on 127.0.0.1:7083')
    mock_staff = MockStaff(
        listen_address=('127.0.0.1', 7083),
        auth_header='here',
        valid_usernames=['user1', 'user2'],
        valid_groupnames=['group1', 'group2']
    )
    mock_staff.start()
    try:
        mock_blackbox.serve_forever()
    except KeyboardInterrupt:
        mock_staff.stop()
