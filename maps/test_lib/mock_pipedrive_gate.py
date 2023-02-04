import contextlib
import json
import flask
from werkzeug.wrappers import Response
from maps.b2bgeo.test_lib.http_server import mock_http_server

VALUE_TRIGGERING_INTERNAL_ERROR = 'simulate_pipedrive_error'

FIELD_VEHICLE_PARK_SIZE = [
    "Количество курьеров",
    "Şirketinize ait araç veya kurye sayısı",
    "Vehicle park size"
]


def _unicode_unescape(string_value):
    return string_value.encode('latin-1').decode('unicode_escape')


def _is_valid_tag(tag, courier_count):
    return any(_unicode_unescape(tag) == t for t in [f'[Routing] / Test-drive ({courier_count})',
                                                     f'[Routing] / Test-drive-TURKEY ({courier_count})'])


def _get_tag(header):
    return header.split(' (')[0]


def _get_vehicle_park_size(params):
    for param in FIELD_VEHICLE_PARK_SIZE:
        if param in params:
            return params[param]
    return None


@contextlib.contextmanager
def mock_pipedrive_gate():
    def _handler(environ, start_response):
        request = flask.Request(environ)

        if request.path == '/form':
            if request.method == 'GET':
                return Response('OK', status=200)(environ, start_response)
            if request.method == 'POST':
                if request.headers.get('X-B2BGEO-PRODUCT') is None:
                    return Response("header X-B2BGEO-PRODUCT not found", status=422)(environ, start_response)

                request_form = request.form.to_dict()

                params = {}
                for field_key, field in request_form.items():
                    try:
                        if not field_key.startswith('field_'):  # for dadata and noop
                            params[field_key] = json.loads(field)
                            continue
                        field = json.loads(field)
                        if field["value"] == VALUE_TRIGGERING_INTERNAL_ERROR:
                            return Response('NOT OK', status=500)(environ, start_response)
                        field["question"]["label"]["ru"]
                        params[field["question"]["label"]["ru"]] = field["value"]
                    except (KeyError, TypeError) as error:
                        return Response(f"invalid field {field_key}: {error}", status=422)(environ, start_response)
                if not _is_valid_tag(request.headers['X-B2BGEO-PRODUCT'], _get_vehicle_park_size(params)):
                    error = f"invalid header X-B2BGEO-PRODUCT value {request.headers['X-B2BGEO-PRODUCT']}"
                    return Response(error, status=422)(environ, start_response)

                resp_json = {
                    'organization_id': 400 if params.get('dadata') is None else None,
                    'deal_id': 100,
                    'person_id': 200,
                    'note_id': 300,
                }
                return Response(json.dumps(resp_json), status=200, headers={'y-test-tag': _get_tag(request.headers['X-B2BGEO-PRODUCT'])})(environ, start_response)
            return Response(f"unknown path {request.path}", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
