import contextlib
import flask
import json
import os
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server


TAXI_POSITION = {"position": {"direction": 1, "lat": 55, "lon": 37, "speed": 100, "timestamp": 1576191600}}
TVM_ENABLED = os.environ.get('YA_COURIER_TVM_ENABLED', 'YES') == 'YES'


def _get_position(claim_id):
    if claim_id == "not_found":
        return {"code": "not_found", "message": "test_not_found_message"}, 404
    if claim_id == "not_active":
        return {"code": "offer_expired", "message": "test_not_active_message"}, 409
    if claim_id == "internal_error":
        return {"some random key": "some random value"}, 500
    return TAXI_POSITION, 200


@contextlib.contextmanager
def mock_taxi_api():
    def _handler(environ, start_response):
        request = flask.Request(environ)
        if TVM_ENABLED and "X-Ya-Service-Ticket" not in request.headers:
            return Response("No TVM2 service ticket", status=401)(environ, start_response)

        if request.path == "/v1/claims/performer-position" and request.method == "GET":
            claim_id = request.args.get("claim_id")
            response, status = _get_position(claim_id)
            return Response(json.dumps(response), status=status)(environ, start_response)

        return Response("Not Found", status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
