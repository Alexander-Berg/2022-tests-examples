import contextlib
import json
import uuid
import re
from werkzeug.wrappers import Response, Request
import maps.b2bgeo.test_lib.apikey_values as apikey_values
import maps.b2bgeo.test_lib.passport_uid_values as passport_uid_values
from maps.b2bgeo.test_lib.http_server import mock_http_server


def _check_key(request_args, environ, start_response, context):
    for key in ['service_token', 'user_ip', 'key']:
        if key not in request_args:
            return Response('''{{"error": "procedure 'check_key': parameter[0]: hash sitem '{}' is mandatory"}}'''.format(key),
                            status=400)(environ, start_response)
    service_token = request_args['service_token']

    if service_token not in context:
        return Response('''{"error": "Service not found"}''', status=404)(environ, start_response)

    apikey = request_args['key']

    if apikey in context[service_token]['active_apikeys']:
        resp = {
            "key_info": {
                "dt": "2017-06-05T14:56:48.355000",
                "hidden": False,
                "id": "addb7783-feb9-4842-ad92-52c0f8ee0359",
                "name": apikey,
                "user": {
                    "balance_client_id": 40080442,
                    "email": "kriss.mcmacer@yandex.ru",
                    "login": "kriss-mcmacer",
                    "name": "Pupkin Vasily",
                    "roles": [
                        "user"
                    ],
                    "uid": 285463599
                }
            },
            "result": "OK"
        }
        return Response(json.dumps(resp))(environ, start_response)

    if apikey == apikey_values.BANNED:
        return Response('{"error": "Key is banned"}', status=403)(environ, start_response)

    return Response('{"error": "Key not found"}', status=404)(environ, start_response)


def _get_link_info(request_args, environ, start_response, context):
    for key in ['service_token', 'user_ip', 'key']:
        if key not in request_args:
            return Response('''{{"error": "procedure 'get_link_info': parameter[0]: hash sitem '{}' is mandatory"}}'''.format(key),
                                status=400)(environ, start_response)

    service_token = request_args['service_token']

    if service_token not in context:
        return Response('''{"error": "Service not found"}''', status=404)(environ, start_response)

    apikey = request_args['key']

    if apikey in apikey_values.ALL_ACTIVE:
        service_name = service_token.split('_')[0]
        limit_stats = {}
        for counter, value in context[service_token]['key_counters'].get(apikey, {}).items():
            name = '_'.join([service_name, counter, "daily"])
            limit_stats[name] = {
                "value_rolled": value,
                "value_unrolled": 0
            }
        response = {
            "result": "OK",
            "link_info": {
                "key": apikey,
                "limit_stats": limit_stats
            },
            "active": True
        }

        return Response(json.dumps(response))(environ, start_response)

    if apikey == apikey_values.BANNED:
        return Response('{"error": "Key is banned"}', status=403)(environ, start_response)

    return Response('{"error": "Key not found"}', status=404)(environ, start_response)


def _update_counters(request_args, environ, start_response, context):
    for key in ['service_token', 'key']:
        if key not in request_args:
            return Response('''{{"error": "procedure 'update_counters': parameter[0]: hash sitem '{}' is mandatory"}}'''.format(key),
                                status=400)(environ, start_response)

    service_token = request_args['service_token']

    if service_token not in context:
        return Response('''{"error": "Service not found"}''', status=404)(environ, start_response)

    if not any(x in request_args.keys() for x in context[service_token]['counters']):
        return Response('{"error": "Must provide one of the units"}', status=400)(environ, start_response)

    apikey = request_args['key']

    if apikey not in apikey_values.ALL_KNOWN:
        return Response('''{{"error": "object Key({{'_id': '{}'}})"}}'''.format(apikey), status=404)(environ, start_response)

    for name in request_args.keys():
        if name in context[service_token]['counters']:
            try:
                val = int(request_args[name])
            except:
                return Response('{"error": "Unit value must be INT"}', status=400)(environ, start_response)
            context[service_token]['counters'][name] += val
            context[service_token]['key_counters'][apikey][name] += val

    return Response('{"result": "OK"}', status=200)(environ, start_response)


def _create_key(request_args, environ, start_response, context):
    for key in ['service_token', 'user_uid']:
        if key not in request_args:
            return Response('''{{"error": "procedure 'create_key': parameter[0]: hash sitem '{}' is mandatory"}}'''.format(key),
                                status=400)(environ, start_response)
    service_token = request_args['service_token']

    if service_token not in context:
        return Response('''{"error": "Service not found"}''', status=404)(environ, start_response)

    user_uid = request_args['user_uid']

    if user_uid == passport_uid_values.VALID:
        apikey = str(uuid.uuid4())
        context[service_token]['active_apikeys'].append(apikey)
        resp = {"result": {"key": apikey}}
        return Response(json.dumps(resp))(environ, start_response)
    elif user_uid == passport_uid_values.SIMULATE_INTERNAL_ERROR:
        return Response("Internal error", status=500)(environ, start_response)

    return Response('{"error": "User not found"}', status=404)(environ, start_response)


def _link_info_by_key(apikey, request, environ, start_response, context):
    if apikey in apikey_values.ALL_ACTIVE:
        service_token = request.headers['X-Service-Token']
        service_id = context[service_token]['service_id']
        project_id = context[service_token]['project_id']

        url = f'/api/v2/service/{service_id}/project/{project_id}?{environ["QUERY_STRING"]}'
        return Response(status=308, headers={'Location': url})(environ, start_response)

    if apikey == apikey_values.BANNED:
        return Response('{"error": "Key is banned"}', status=403)(environ, start_response)

    return Response('{"error": "Key not found"}', status=404)(environ, start_response)


def _get_project_link_info(request, environ, start_response, context):
    service_token = environ['HTTP_X_SERVICE_TOKEN']
    includes = request.args.get('include').split(',')
    result = context[service_token]['project_link_info_response']
    for include in includes:
        result['data'][include] = context[service_token]['INCLUDES'][include]
    return Response(json.dumps(context[service_token]['project_link_info_response']))(environ, start_response)


@contextlib.contextmanager
def mock_apikeys_server(context):
    assert isinstance(context, dict)
    assert context
    for service_data in context.values():
        assert 'counters' in service_data
        assert isinstance(service_data['counters'], dict)
        assert 'key_counters' in service_data
        assert isinstance(service_data['key_counters'], dict)
        assert 'active_apikeys' in service_data
        assert isinstance(service_data['active_apikeys'], list)

    def _handler(environ, start_response):
        request = Request(environ)

        if 'key' in request.args and request.args['key'] == apikey_values.MOCK_SIMULATE_ERROR:
            return Response("Internal error", status=500)(environ, start_response)

        handler = {
            "/api/check_key": _check_key,
            "/api/get_link_info": _get_link_info,
            "/api/update_counters": _update_counters,
            "/api/create_key": _create_key,
        }.get(request.path)
        if handler:
            return handler(request.args, environ, start_response, context)
        elif request.path.startswith("/api/v2"):
            if request.headers.get('X-Service-Token') not in context:
                return Response('{"error": "Forbidden"}', status=403)(environ, start_response)
            service_token = request.headers['X-Service-Token']
            service_id = context[service_token]['service_id']
            if m := re.match(r"/api/v2/link_info_by_key/([\w-]+)", request.path):
                return _link_info_by_key(m.group(1), request, environ, start_response, context)
            if m := re.match(rf"/api/v2/service/{service_id}/project/([\w-]+)", request.path):
                return _get_project_link_info(request, environ, start_response, context)

        return Response('{"error": "unknown method path"}', status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
