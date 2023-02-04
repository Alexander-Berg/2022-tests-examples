import contextlib
import flask
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server


@contextlib.contextmanager
def mock_send_push_notification():
    def _handler(environ, start_response):
        request = flask.Request(environ)
        data = request.get_json(force=True)
        if request.path == '/send' and request.method == 'POST':
            for key in ['event', 'user', 'title', 'body']:
                if key not in data:
                    return Response('''{{"error": "procedure 'send': parameter[0]: hash sitem '{}' is mandatory"}}'''.format(key),
                                        status=400)(environ, start_response)
            if data['user'] == 'internal_error':
                return Response("Internal error", status=500)(environ, start_response)
            for key in data:
                if not isinstance(data[key], str):
                    return Response('''{{"error": "procedure 'send': parameter[0]: hash sitem '{}' must be string"}}'''.format(key),
                                        status=400)(environ, start_response)
            if len(data['user']) > 15 or not data['user'].isdigit():
                return Response('''{{"error": "procedure 'send': 'user' number is too long - max length is 15 digits according to ITU-T E.164}}''',
                                    status=400)(environ, start_response)
            if len(data) != 4:
                return Response('''{{"error": "procedure 'send': too many request fields"}}''',
                                    status=422)(environ, start_response)
            return Response('Push notification was sent', status=200)(environ, start_response)
        else:
            return Response(f"Unknown path {request.path}", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
