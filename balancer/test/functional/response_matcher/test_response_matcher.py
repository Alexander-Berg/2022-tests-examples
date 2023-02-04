# -*- coding: utf-8 -*-
import pytest

from configs import ResponseMatcherConfig, HeadersForwardConfig, DiedBackendsConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.proto.http.stream import HTTPReaderException


@pytest.mark.parametrize(
    ['code', 'content'],
    [
        ('200', 'matched'),
        ('500', 'module')
    ]
)
def test_correctness(ctx, code, content):
    ctx.start_backend(SimpleConfig(http.response.ok(data='module')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(data='matched')), name='backend2')
    ctx.start_balancer(ResponseMatcherConfig(code=code))
    response = ctx.perform_request(http.request.get())
    asserts.content(response, content)


@pytest.mark.parametrize(
    ['header', 'header_value', 'forward_headers'],
    [
        ('location', 'abc', 'location'),
        ('Location', 'abc', 'location'),
        ('location', 'abc', 'LOCATION'),
        ('location', 'abc', 'location|some-header'),
        ('location', 'abc', '.*'),
    ]
)
def test_forward_headers(ctx, header, header_value, forward_headers):
    ctx.start_backend(SimpleConfig(http.response.ok(data='matched')))
    ctx.start_balancer(HeadersForwardConfig(header=header, header_value=header_value, forward_headers=forward_headers))
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'matched')
    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, header_value)


@pytest.mark.parametrize('start_backend', ['backend1', 'backend2'])
def test_died_backends(ctx, start_backend):
    ctx.start_backend(SimpleConfig(http.response.ok()), name=start_backend)
    kwargs = {
        'code': '200',
        'backend1_port': ctx.manager.port.get_port(),
        'backend2_port': ctx.manager.port.get_port(),
    }
    kwargs.update({start_backend: getattr(ctx, start_backend).server_config.port})
    ctx.start_balancer(DiedBackendsConfig(code='200'))
    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get())


def test_proxy_big_answer(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=1000*'a')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(data='matched')), name='backend2')
    ctx.start_balancer(ResponseMatcherConfig(code=200, buffer_size=10))
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'matched')


@pytest.mark.parametrize(
    ['method', 'content'],
    [
        ('GET', 'matched'),
        ('OPTIONS', 'module'),
        ('POST', 'module'),
        ('PUT', 'module'),
        ('PATCH', 'module'),
        ('DELETE', 'module'),
        ('CONNECT', 'module'),
    ]
)
def test_bad_requests(ctx, method, content):
    ctx.start_backend(SimpleConfig(http.response.ok(data='module')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(data='matched')), name='backend2')
    ctx.start_balancer(ResponseMatcherConfig())
    response = ctx.perform_request(http.request.custom(method))
    asserts.content(response, content)


def test_get_with_body(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data='module')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(data='matched')), name='backend2')
    ctx.start_balancer(ResponseMatcherConfig())
    response = ctx.perform_request(http.request.get(data='some body'))
    asserts.content(response, 'module')
