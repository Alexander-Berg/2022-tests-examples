# -*- coding: utf-8 -*-
import pytest
import time
import datetime

import balancer.test.plugin.context as mod_ctx
from configs import AntirobotCutterConfig

from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util import asserts
from balancer.test.util.predef import http


class AntirobotCutterContext(object):
    def __init__(self):
        super(AntirobotCutterContext, self).__init__()
        self.__ssl = self.request.param

    def start_main_backend(self, config):
        self.start_backend(config, name='main_backend')
        return self.main_backend

    def start_antirobot_backend(self, config):
        self.start_backend(config, name='antirobot_backend')
        return self.antirobot_backend

    def start_antirobot_cutter_balancer(self, **balancer_kwargs):
        if self.__ssl:
            balancer_kwargs['use_ssl'] = '1'
            balancer_kwargs['cert_dir'] = self.certs.root_dir

        config = AntirobotCutterConfig(**balancer_kwargs)
        return self.start_balancer(config)

    def create_conn(self):
        if self.__ssl:
            return self.manager.connection.http.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True)
            )
        else:
            return self.create_http_connection()


antirobot_cutter_ctx = mod_ctx.create_fixture(AntirobotCutterContext, params=[False, True], ids=['nossl', 'ssl'])


def test_get_passes(antirobot_cutter_ctx):
    """
    GET requests with empty body are passed to both antirobot and backend
    since ther're already parsed
    """
    antirobot_cutter_ctx.start_antirobot_backend(SimpleConfig())
    antirobot_cutter_ctx.start_main_backend(SimpleConfig())
    antirobot_cutter_ctx.start_balancer(AntirobotCutterConfig())

    original_request = http.request.get()
    response = antirobot_cutter_ctx.perform_request(original_request)
    asserts.status(response, 200)

    main_backend_request = antirobot_cutter_ctx.main_backend.state.requests.get()
    asserts.method(main_backend_request.request, 'GET')
    asserts.path(main_backend_request.request, '/')
    asserts.content(main_backend_request.request, '')

    antirobot_backend_request = antirobot_cutter_ctx.antirobot_backend.state.requests.get()
    asserts.method(antirobot_backend_request.request, 'POST')
    asserts.path(antirobot_backend_request.request, '/fullreq')
    asserts.content(antirobot_backend_request.request, 'GET / HTTP/1.1\r\n\r\n')


def request_body_test(antirobot_cutter_ctx, original_request, conn, cutter_timeout, expected_inner_body=None, send_body_immediately=False):
    start_time = datetime.datetime.today()
    stream = conn.create_stream()
    stream.write_request_line(original_request.request_line)
    stream.write_headers(original_request.headers)
    if not send_body_immediately:
        time.sleep(cutter_timeout + 2)
    stream.write_data(original_request.data)

    resp = stream.read_response()
    asserts.status(resp, 200)

    antirobot_backend_request = antirobot_cutter_ctx.antirobot_backend.state.requests.get()
    time_diff = antirobot_backend_request.start_time - start_time
    assert time_diff <= datetime.timedelta(cutter_timeout + 0.3)

    asserts.method(antirobot_backend_request.request, 'POST')
    asserts.path(antirobot_backend_request.request, '/fullreq')
    if expected_inner_body is None:
        expected_inner_body = 'POST / HTTP/1.1\r\n\r\n'
    asserts.content(antirobot_backend_request.request, expected_inner_body)

    main_backend_request = antirobot_cutter_ctx.main_backend.state.requests.get()
    asserts.method(main_backend_request.request, 'POST')
    asserts.path(main_backend_request.request, '/')
    asserts.content(main_backend_request.request, original_request.data.content)


def base_body_test(
        antirobot_cutter_ctx,
        original_request,
        connections_count,
        expected_inner_body=None,
        cutter_bytes=1024,
        antirobot_cut_bytes=None,
        send_body_immediately=False):
    cutter_timeout = 1.0

    antirobot_cutter_ctx.start_antirobot_backend(SimpleConfig())
    antirobot_cutter_ctx.start_main_backend(SimpleConfig())
    antirobot_cutter_ctx.start_antirobot_cutter_balancer(
        cutter_bytes=cutter_bytes, cutter_timeout='%fs' % cutter_timeout,
        antirobot_timeout='5s', antirobot_cut_bytes=antirobot_cut_bytes,
    )

    with antirobot_cutter_ctx.create_conn() as conn:
        for i in xrange(connections_count):
            request_body_test(antirobot_cutter_ctx, original_request, conn, cutter_timeout, expected_inner_body=expected_inner_body, send_body_immediately=send_body_immediately)


@pytest.mark.parametrize('connections', [1, 2])
def test_post_delayed_body(antirobot_cutter_ctx, connections):
    """
    BALANCER-770
    Requests with body might be passed incomplete to antirobot module.
    With antirobot.cut_request = true inner body may be incomplete,
    but the request POST /fullreq must be complete
    """
    original_request = http.request.post(data='data').to_raw_request()
    base_body_test(antirobot_cutter_ctx, original_request, connections)


@pytest.mark.parametrize('connections', [1, 2])
def test_post_delayed_body_chunked(antirobot_cutter_ctx, connections):
    """
    BALANCER-770
    Requests with body might be passed incomplete to antirobot module.
    With antirobot.cut_request = true inner body may be incomplete,
    but the request POST /fullreq must be complete
    """
    original_request = http.request.post(data=['d', 'a', 't', 'a']).to_raw_request()
    base_body_test(antirobot_cutter_ctx, original_request, connections)


@pytest.mark.parametrize('connections', [1, 2])
def test_antiribot_cut_bytes(antirobot_cutter_ctx, connections):
    """
    BALANCER-474
    BALANCER-791

    Requests with body might be passed incomplete to antirobot module.
    With antirobot.cut_request = true inner body may be incomplete,
    but the request POST /fullreq must be complete.
    Antirobot's cut_request_bytes limits the body of innter body.
    """
    cutter_bytes = 1024
    antirobot_cut_bytes = 20
    body = 'a' * cutter_bytes
    expected_inner_body = 'POST / HTTP/1.1\r\n\r\n' + body[:antirobot_cut_bytes]
    original_request = http.request.post(data=body).to_raw_request()
    base_body_test(antirobot_cutter_ctx, original_request, connections,
                   cutter_bytes=cutter_bytes, antirobot_cut_bytes=antirobot_cut_bytes,
                   expected_inner_body=expected_inner_body, send_body_immediately=True)


@pytest.mark.parametrize('connections', [1, 2])
def test_antiribot_cut_bytes_chunked(antirobot_cutter_ctx, connections):
    """
    BALANCER-474
    BALANCER-791

    Requests with body might be passed incomplete to antirobot module.
    With antirobot.cut_request = true inner body may be incomplete,
    but the request POST /fullreq must be complete.
    Antirobot's cut_request_bytes limits the body of innter body.
    """
    cutter_bytes = 1024
    antirobot_cut_bytes = 20
    body = ['a'] * cutter_bytes
    expected_inner_body = 'POST / HTTP/1.1\r\n\r\n' + ('a' * antirobot_cut_bytes)
    original_request = http.request.post(data=body).to_raw_request()
    base_body_test(antirobot_cutter_ctx, original_request, connections,
                   cutter_bytes=cutter_bytes, antirobot_cut_bytes=antirobot_cut_bytes,
                   expected_inner_body=expected_inner_body, send_body_immediately=True)
