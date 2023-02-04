# -*- coding: utf-8 -*-
"""
BALANCER-327 - балансировка leastconn
"""
from configs import LeastconnConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts


def test_round_robin_like(ctx):
    """
    Запускаемся с leastconn, отдаём запрос, ждём ответ,
    отдаём ещё один ответ, он должен уйти на второй бэкэнд
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(LeastconnConfig())

    request = http.request.get()

    response = ctx.perform_request(request)
    asserts.status(response, 200)
    if ctx.backend1.state.requests.empty():
        assert ctx.backend2.state.get_request() == request

        response = ctx.perform_request(request)
        asserts.status(response, 200)
        assert ctx.backend1.state.get_request() == request
        assert ctx.backend2.state.requests.empty()
    else:
        assert ctx.backend1.state.get_request() == request

        response = ctx.perform_request(request)
        asserts.status(response, 200)
        assert ctx.backend2.state.get_request() == request
        assert ctx.backend1.state.requests.empty()


def test_leastconn(ctx):
    """
    Запускаемся с leastconn
    Первый запрос улетает в один бэкэнд и держит с ним соединение.
    В это время req_count запросов летят на другой бэкенд.
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')

    ctx.start_balancer(LeastconnConfig())

    request = http.request.post(data=['A'])
    raw_request = request.to_raw_request()

    req_count = 10

    with ctx.create_http_connection() as long_conn:
        stream = long_conn.create_stream()
        stream.write_request_line(raw_request.request_line)
        stream.write_headers(raw_request.headers)
        stream.write_chunk('A')

        for i in xrange(req_count):
            ctx.perform_request(http.request.get())

        stream.write_chunk('')
        stream.read_response()

    qsize1 = ctx.backend1.state.requests.qsize()
    qsize2 = ctx.backend2.state.requests.qsize()

    assert qsize1 + qsize2 == req_count + 1
    assert min(qsize1, qsize2) == 1
    assert max(qsize1, qsize2) == 10
