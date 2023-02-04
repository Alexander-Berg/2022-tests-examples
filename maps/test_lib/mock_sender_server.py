import re
import flask
import contextlib
import json
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server
import maps.b2bgeo.test_lib.sender_values as sender_values


@contextlib.contextmanager
def mock_sender_server():
    path_pattern = re.compile("/api/0/yandex.routing/transactional/(.*)/send")

    def _handler(environ, start_response):
        request = flask.Request(environ)

        match = path_pattern.match(request.path)

        if match is None:
            return Response(status=404)(environ, start_response)

        if "Authorization" not in request.headers:
            resp_json = {
                "result": {
                    "status": "ERROR",
                    "error": {
                        "detail": "Учетные данные не были предоставлены."
                    }
                }
            }
            return Response(json.dumps(resp_json), status=401)(environ, start_response)

        if request.headers["Authorization"] != sender_values.SENDER_BASIC_AUTH_HEADER:
            resp_json = {
                "result": {
                    "status": "ERROR",
                    "error": {
                        "detail": "API key is not valid"
                    }
                }
            }
            return Response(json.dumps(resp_json), status=401)(environ, start_response)

        if match.group(1) not in [sender_values.WELCOME_MAILING_ID, sender_values.WELCOME_MAILING_ID_TR, sender_values.INTERNAL_MAILING_ID]:
            resp_json = {
                "result": {
                    "status": "ERROR",
                    "error": {
                        "detail": "Campaign not found"
                    }
                }
            }
            return Response(json.dumps(resp_json), status=404)(environ, start_response)

        if "to_email" not in request.args or request.args["to_email"] == "":
            resp_json = {
                "result": {
                    "status": "ERROR",
                    "error": {
                        "non_field_errors": [
                            "to_email or valid to_yandex_puid argument required: no to_email"
                        ]
                    }
                }
            }
            return Response(json.dumps(resp_json), status=400)(environ, start_response)
        elif request.args["to_email"] == sender_values.EMAIL_SIMULATE_INTERNAL_ERROR:
            return Response('NOT OK', status=500)(environ, start_response)

        request_json = request.get_json(force=True)
        async_param = request_json.get("async")
        args_param = request_json.get("args")

        resp_json = {
            "params": {
                "control": {
                    "async": True if async_param is None else async_param,
                    "countdown": None,
                    "expires": 86400,
                    "for_testing": False
                },
                "source": {
                    "to_email": request.args["to_email"],
                    "header": [],
                    "ignore_empty_email": False
                }
            },
            "result": {
                "status": "OK",
                "message_id": "<20200217161427.643701.7354e11ded794bca9849b15d4237c204@ui-1.testing.ysendercloud>",
                "task_id": "689b8285-106d-4b87-a2c8-173b8da32144"
            }
        }

        if args_param is not None:
            resp_json["params"]["source"]["args"] = args_param

        return Response(json.dumps(resp_json))(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
