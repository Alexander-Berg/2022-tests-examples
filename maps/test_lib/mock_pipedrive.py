import contextlib
import json
import flask
from werkzeug.wrappers import Response
from maps.b2bgeo.test_lib.http_server import mock_http_server
from maps.b2bgeo.pipedrive_gate.lib.crm_infra.field_names_to_keys import field_names_to_keys_ww

ORGANIZATION_CUSTOM_FIELDS = field_names_to_keys_ww['testing']['organizationFields']
DEAL_CUSTOM_FIELDS = field_names_to_keys_ww['testing']['dealFields']
PERSON_CUSTOM_FIELDS = {}
ORGANIZATIONS = []
PERSONS = []
DEALS = []
NOTES = []


def _create_item(request, item_list, item_fields):
    data = request.get_json(force=True)
    item = {}
    for field in item_fields:
        item[field] = data.get(field)
    item['id'] = len(item_list)
    item_list.append(item)
    return item


def _format_fields(fields):
    return [{'name': k, 'key': v} for k, v in fields.items()]


def _get_response(data):
    return {"success": True, "data": data}


@contextlib.contextmanager
def mock_pipedrive():
    global ORGANIZATIONS
    global PERSONS
    global DEALS
    global NOTES

    def _handler(environ, start_response):
        request = flask.Request(environ)
        if not request.args.get('api_token'):
            resp = {"success": False, "error": "unauthorized access", "errorCode": 401}
            return Response(json.dumps(resp), status=401)(environ, start_response)
        if request.path == '/persons':
            if request.method == 'GET':
                return Response(json.dumps(_get_response(PERSONS)), status=200)(environ, start_response)
            if request.method == 'POST':
                person = _create_item(request, PERSONS, ['name', 'email', 'phone'])
                return Response(json.dumps(_get_response(person)), status=200)(environ, start_response)
        elif request.path == '/dealFields':
            if request.method == 'GET':
                data = _format_fields(DEAL_CUSTOM_FIELDS)
                return Response(json.dumps(_get_response(data)), status=200)(environ, start_response)
        elif request.path == '/personFields':
            if request.method == 'GET':
                data = _format_fields(PERSON_CUSTOM_FIELDS)
                return Response(json.dumps(_get_response(data)), status=200)(environ, start_response)
        elif request.path == '/organizationFields':
            if request.method == 'GET':
                data = _format_fields(ORGANIZATION_CUSTOM_FIELDS)
                return Response(json.dumps(_get_response(data)), status=200)(environ, start_response)
        elif request.path == '/organizations':
            if request.method == 'GET':
                return Response(json.dumps(_get_response(ORGANIZATIONS)), status=200)(environ, start_response)
            if request.method == 'POST':
                org = _create_item(request, ORGANIZATIONS, list(ORGANIZATION_CUSTOM_FIELDS.values()))
                return Response(json.dumps(_get_response(org)), status=200)(environ, start_response)
        elif request.path == '/deals':
            if request.method == 'GET':
                return Response(json.dumps(_get_response(DEALS)), status=200)(environ, start_response)
            if request.method == 'POST':
                deal = _create_item(request, DEALS, list(DEAL_CUSTOM_FIELDS.values()))
                return Response(json.dumps(_get_response(deal)), status=200)(environ, start_response)
        elif request.path == '/notes':
            if request.method == 'GET':
                return Response(json.dumps(_get_response(NOTES)), status=200)(environ, start_response)
            if request.method == 'POST':
                note = _create_item(request, NOTES, ['deal_id', 'content'])
                return Response(json.dumps(_get_response(note)), status=200)(environ, start_response)
        return Response(f"unknown path {request.path}", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
