# -*- coding: utf-8 -*-
from balancer.test.util import asserts


def __check_status(resp, status):
    asserts.status(resp, status)


def check_status(resp):
    __check_status(resp, 200)


def check_status_204(resp):
    __check_status(resp, 204)


def check_status_301(resp):
    __check_status(resp, 301)


def check_status_302(resp):
    __check_status(resp, 302)


def check_status_307(resp):
    __check_status(resp, 307)


def check_status_400(resp):
    __check_status(resp, 400)


def check_status_404(resp):
    __check_status(resp, 404)


def check_status_406(resp):
    __check_status(resp, 406)


def check_status_413(resp):
    __check_status(resp, 413)


def check_status_414(resp):
    __check_status(resp, 414)


def check_status_500(resp):
    __check_status(resp, 500)


def check_status_503(resp):
    __check_status(resp, 503)


def check_http_constraints(resp):
    transfer_encoding = resp.headers.get_one('transfer-encoding')
    content_length = resp.headers.get_one('content-length')
    connection = resp.headers.get_one('connection', '').lower()

    if resp.response_line.version == 'HTTP/1.0':
        assert not transfer_encoding, 'returned HTTP/1.0 with chunked transfer encoding'

    assert not (transfer_encoding and content_length), \
        'returned response with transfer-encoding and contentl-length headers'

    if content_length:
        assert int(content_length) == len(resp.data.content), 'data length is not equal to Content-Length header value'

    assert (len(connection) == 0 or connection == 'close' or connection == 'keep-alive'), \
        'Connection must be either "close" or "keep-alive" or abscent'


def check_status_500_content(resp):

    content_type = resp.headers.get_one('content-type')
    assert content_type == 'text/html; charset=utf-8', 'No valid content-type header'

    if content_type:
        assert resp.data.content == '<html><body>&nbsp;</body></html>', "error code 500 must return not null content"
