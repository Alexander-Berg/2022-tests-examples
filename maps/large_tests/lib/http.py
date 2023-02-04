import requests
import logging
import json


logger = logging.getLogger("http")


def decode(values):
    if isinstance(values, dict):
        return {
            decode(name): decode(values[name])
            for name in values
        }
    elif isinstance(values, list):
        return [
            decode(value)
            for value in values
        ]
    elif callable(getattr(values, "decode", None)):
        return values.decode("utf-8")
    else:
        return values


class HttpResponse(list):
    def __init__(self, code, body):
        list.__init__(self, [code, body])
        self.code = code
        self.body = body

    def __rshift__(self, code):
        assert self.code == code, "got %s, expect %s, body: %s" % (self.code, code, self.body)
        return self.body


def http_request(method, url, **kwargs):
    to_print = "%s %s" % (method, url)
    params = kwargs.get("params")
    headers = kwargs.get("headers")
    data = kwargs.get("data")
    json_data = kwargs.get("json")

    BODY_LENGTH_FOR_LOG = 10000

    if params:
        to_print += "?" + "&".join(map(lambda x: str(x[0]) + "=" + str(x[1]), params.items()))

    if headers:
        to_print += "\n" + "\n".join(map(lambda x: str(x[0]) + ": " + str(x[1]), headers.items()))

    if data:
        if isinstance(data, str) or isinstance(data, bytes):
            to_print += "\n====\n%s\n====\n" % data[:BODY_LENGTH_FOR_LOG]
        else:
            to_print += "\n====\n" + "&".join(map(lambda x: str(x[0]) + "=" + str(x[1]), data.items())) + "\n====\n"
    elif json_data:
        to_print += "\n====\n" + json.dumps(json_data) + "\n====\n"

    logger.info(to_print)

    response = getattr(requests, method.lower())(url, **kwargs)

    content_type = response.headers.get('content-type')
    if content_type in ['application/protobuf', 'application/x-protobuf', 'application/x-binary', 'application/zip']:
        body_for_log = response.content[:BODY_LENGTH_FOR_LOG]
        body = response.content
    else:
        body_for_log = response.text[:BODY_LENGTH_FOR_LOG]
        body = decode(response.text)

    logger.info("[%d] -> %s" % (response.status_code, body_for_log))
    return HttpResponse(response.status_code, body)


def http_request_json(*vargs, parse_float=None, **kwargs):
    status, body = http_request(*vargs, **kwargs)
    json_body = None
    if len(body) > 0:
        json_body = decode(json.loads(body, parse_float=parse_float))
    return HttpResponse(status, json_body)
