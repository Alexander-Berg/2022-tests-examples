# -*- coding: utf-8 -*-
from configs import H100Config

from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util import asserts
from balancer.test.util.predef import http


def start_all(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(H100Config(backend.server_config.port))
    return backend


def test_no_except(ctx):
    '''
    BALANCER-480
    Requests without Expect: 100-continue header
    just pass through.
    '''
    start_all(ctx)
    response = ctx.perform_request(http.request.post(data='data'))
    asserts.status(response, 200)


def test_expect_delete(ctx):
    '''
    BALANCER-480
    Requests with Expect: header, but not 100-continue value
    pass through with Expect: headers deleted
    '''
    backend = start_all(ctx)
    response = ctx.perform_request(http.request.post(headers={'Expect': '200-stop'}, data='data'))
    asserts.status(response, 200)
    req = backend.state.get_request()
    asserts.no_header(req, 'Expect')


def test_100_continue(ctx):
    '''
    BALANCER-480
    Requests with Expect: 100-continue header get HTTP 100 Continue
    answer from balancer, these requests don't get to backends. Next, the
    original request with Expect: headers deleted get to backend and
    backend's answer is transferred to client
    '''
    backend = start_all(ctx)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.post(headers={'Expect': '100-continue'}, data='data').to_raw_request())
        resp = stream.read_next_response()
        asserts.status(resp, 100)
        resp2 = stream.read_next_response()
        asserts.status(resp2, 200)

    req = backend.state.get_request()
    asserts.no_header(req, 'Expect')
    asserts.method(req, 'POST')


def test_100_continue_no_body_sent(ctx):
    '''
    BALANCER-480
    Requests with Expect: 100-continue header get HTTP 100 Continue
    answer from balancer, these requests don't get to backends. Next, the
    original request with Expect: headers deleted get to backend and
    backend's answer is transferred to client. The 100 Continue answer
    is sent immediately after parsing message headers, body is not required
    '''
    start_all(ctx)

    req = http.request.post(headers={'Expect': '100-continue'}, data='data').to_raw_request()
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        resp = stream.read_next_response()
        asserts.status(resp, 100)
