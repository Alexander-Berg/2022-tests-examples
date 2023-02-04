import contextlib
import json
import copy
from werkzeug.wrappers import Response, Request
from library.python import resource
from maps.b2bgeo.test_lib import passport_uid_values
from maps.b2bgeo.test_lib.http_server import mock_http_server

oauth_template = json.loads(resource.find("/oauth_template.json").decode())
cookie_template = json.loads(resource.find("/cookie_template.json").decode())

NEOPHONISH_UID = "89998"


def _get_error(message):
    return {
        "exception": {
            "value": "INVALID_PARAMS",
            "id": 2
        },
        "error": message
    }


def _get_phones(uid):
    if uid == NEOPHONISH_UID:
        return [
            {
                "id": "474276206",
                "attributes": {
                    "102": "+70000000094",
                    "105": "1"
                }
            }
        ]
    return []


def _get_oauth_data(login, uid):
    data = copy.copy(oauth_template)
    data['oauth']['uid'] = uid
    data['uid']['value'] = uid
    data['login'] = login
    data['phones'] = _get_phones(uid)
    return data


def _get_cookie_data(login, uid):
    data = copy.copy(cookie_template)
    data['default_uid'] = uid
    default_user = data['users'][1]
    default_user['id'] = uid
    default_user['uid']['value'] = uid
    default_user['login'] = login
    default_user['phones'] = _get_phones(uid)
    return data


def _get_user_info(request, method):
    if method == 'oauth':
        headers = request.headers
        auth = headers['Authorization']
        login, uid = auth.split(':')
        return login, uid
    elif method == 'sessionid':
        login = request.args['sessionid']
        uid = request.args['sslsessionid']
        return login, uid
    else:
        raise Exception('unsupported method')


def _blackbox_handler(environ, start_response):
    request = Request(environ)
    args = request.args
    method = args.get('method')
    login, uid = _get_user_info(request, method)
    if uid == passport_uid_values.INVALID:
        data = {"status": {"value": "INVALID", "id": 5}, "error": "token_expired"}
    elif method == 'oauth':
        data = _get_oauth_data(login, uid)
    else:
        multisession = args.get('multisession', 'no')
        if multisession != 'yes':
            raise Exception('unsupported method')
        data = _get_cookie_data(login, uid)
    return Response(f'{json.dumps(data)}', status=200)(environ, start_response)


@contextlib.contextmanager
def mock_blackbox():

    def _handler(environ, start_response):
        request = Request(environ)
        if request.path != '/blackbox':
            return Response('Not Found', status=404)(environ, start_response)
        try:
            return _blackbox_handler(environ, start_response)
        except Exception as ex:
            return Response(json.dumps(_get_error(str(ex))), status=200)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
