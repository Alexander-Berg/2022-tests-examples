import contextlib
import flask
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server


@contextlib.contextmanager
def mock_sendsms():
    def _handler(environ, start_response):
        request = flask.Request(environ)

        if request.path == '/sendsms' and request.method == 'GET':
            message = """
                <?xml version="1.0" encoding="UTF-8"?>
                <doc>
                    <message-sent id="127000000003456" />
                    <gates ids="15" />
                </doc>
            """
            headers = {'Content-Type': 'text/xml'}
            return Response(message, status=200, headers=headers)(environ, start_response)

        return Response(f"unknown path {request.path}", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
