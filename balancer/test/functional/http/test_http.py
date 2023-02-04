# -*- coding: utf-8 -*-
import pytest
import time
import math
import socket

from configs import HTTPBalancerConfig

from balancer.test.util.balancer import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.proto.http.message import HTTPRequest, HTTPRequestLine
from balancer.test.util.stdlib.multirun import Multirun


OK_RESPONSE = 'ok'
MANY_HEADERS_REQUEST = http.request.raw_get(path='/request', headers={
    'Header1': 'Value1',
    'Header2': 'Value2',
    'Header3': ': Value3',
    'Header4': 'Value4 ',
    'Header5': ' Value5',
    'Connection': 'Close',
})
UNFINISHED_REQUEST = '''GET /request HTTP/1.1\r
Heade'''
COLON_REQUEST = '''GET / HTTP/1.1\r
AAA::BBB\r
AAA:: BBB\r
AAA: :BBB\r
AAA:::BBB\r
AAA: :BBB  \r
\r\n'''


@pytest.mark.parametrize(
    ['req', 'status', 'reason_phrase'],
    [
        ('GET / HTTP/0.8\r\n\r\n', 400, 'Bad request'),
        ('GET / HTTP/5.3\r\n\r\n', 400, 'Bad request'),
        ('GET /\r\n\r\n', 400, 'Bad request'),
        ('GET /abc?def=123\r\n\r\n', 400, 'Bad request'),
    ],
    ids=[
        'http08',
        'http53',
        'http09',
        'http09,cgi'
    ]
)
def test_low_version(ctx, req, status, reason_phrase):
    """
    SEPE-4100
    Условие: балансер получает запрос по протоколу, версия которого ниже HTTP/1.0.
    Поведение: балансер закрывает соединение, так же проверяем, что он не устанавливает соединение с бэкендом
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    tcpdump = ctx.manager.tcpdump.start(ctx.backend.server_config.port)
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write(req)
        response = stream.read_response()
    asserts.status(response, status)
    asserts.reason_phrase(response, reason_phrase)
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 0, 'Connection has been enabled!'


def base_one_packet_test(ctx, packet):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    with ctx.create_http_connection() as conn:
        conn.sock.send(packet)
        stream1 = conn.create_stream()
        resp1 = stream1.read_response()
        stream2 = conn.create_stream()
        resp2 = stream2.read_response()

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


def base_two_packets_test(ctx, first_packet, second_packet):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    with ctx.create_http_connection() as conn:
        conn.sock.send(first_packet)
        time.sleep(0.5)
        conn.sock.send(second_packet)
        stream1 = conn.create_stream()
        resp1 = stream1.read_response()
        stream2 = conn.create_stream()
        resp2 = stream2.read_response()

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


def test_one_packet_two_requests_no_data(ctx):
    """
    BALANCER-370
    Если два запроса, не содержащих тела, попадают в один tcp пакет,
    то балансер должен обработать оба запроса
    """
    base_one_packet_test(ctx, 'GET / HTTP/1.1\r\n\r\nGET / HTTP/1.1\r\n\r\n')


def test_one_packet_two_requests_with_chunked_data(ctx):
    """
    BALANCER-370
    Если два запроса, первый из которых содержит тело, попадают в один tcp пакет,
    то балансер должен обработать оба запроса
    """
    packet = ('POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n5\r\n12345\r\n0\r\n\r\n'
              'GET / HTTP/1.1\r\n\r\n'
              )
    base_one_packet_test(ctx, packet)


def test_one_packet_two_requests_with_length_data(ctx):
    """
    BALANCER-370
    Если два запроса, первый из которых содержит тело, попадают в один tcp пакет,
    то балансер должен обработать оба запроса
    """
    packet = ('POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n12345'
              'GET / HTTP/1.1\r\n\r\n'
              )
    base_one_packet_test(ctx, packet)


def test_one_packet_prev_chunked_data_new_request(ctx):
    """
    BALANCER-370
    Клиент отправляет стартовую строку и заголовки первого запроса в одном tcp пакете,
    в следующем пакете отправляет тело первого запроса и второй запрос.
    Балансер должен обработать оба запроса
    """
    first_packet = 'POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n'
    second_packet = '5\r\n12345\r\n0\r\n\r\nGET / HTTP/1.1\r\n\r\n'
    base_two_packets_test(ctx, first_packet, second_packet)


def test_one_packet_prev_length_data_new_request(ctx):
    """
    BALANCER-370
    Клиент отправляет стартовую строку и заголовки первого запроса в одном tcp пакете,
    в следующем пакете отправляет тело первого запроса и второй запрос.
    Балансер должен обработать оба запроса
    """
    first_packet = 'POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n'
    second_packet = '12345GET / HTTP/1.1\r\n\r\n'
    base_two_packets_test(ctx, first_packet, second_packet)


def test_fits_maxlen_maxreq(ctx):
    """
    Если длина стартовой строки вместе с длиной заголовков не превышает maxlen,
    и длина стартовой строки не превышает maxreq,
    то запрос перенаправляется backend-у и клиенту возвращается ответ
    """
    request = MANY_HEADERS_REQUEST
    maxlen = len(str(request))
    maxreq = len(request.request_line.path)
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(maxlen=maxlen, maxreq=maxreq))

    response = ctx.perform_request(request)
    asserts.status(response, 200)
    asserts.content(response, OK_RESPONSE)


@pytest.mark.parametrize(
    ['status', 'msg', 'maxlen_delta', 'maxreq_delta'],
    [
        (413, 'Request entity too large', -1, 0),
        (414, 'Request uri too large', 0, -1),
        pytest.param(414, 'Request uri too large', -1, -1, marks=pytest.mark.xfail),
    ],
    ids=[
        'maxlen',
        'maxreq',
        'maxlen,maxreq',
    ]
)
def test_fail_limits(ctx, status, msg, maxlen_delta, maxreq_delta):
    """
    Если длина стартовой строки вместе с длиной заголовков превышает maxlen,
    или длина url'а превышает maxreq,
    то балансер закрывает соединение с клиентом
    """
    request = MANY_HEADERS_REQUEST
    maxlen = len(str(request))
    maxreq = len(request.request_line.path)
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(maxlen=maxlen + maxlen_delta, maxreq=maxreq + maxreq_delta))

    response = ctx.perform_request(request)
    asserts.status(response, status)
    asserts.reason_phrase(response, msg)


@pytest.mark.parametrize(
    ['status', 'msg', 'maxlen_delta', 'maxreq_delta'],
    [
        (413, 'Request entity too large', 0, 0),
        pytest.param(414, 'Request uri too large', 1, -1, marks=pytest.mark.xfail),
        pytest.param(414, 'Request uri too large', 0, -1, marks=pytest.mark.xfail),
    ],
    ids=[
        'maxlen',
        'maxreq',
        'maxlen,maxreq',
    ]
)
def test_fail_limits_unfinished(ctx, status, msg, maxlen_delta, maxreq_delta):
    request = UNFINISHED_REQUEST
    maxlen = len(request)
    maxreq = len('/request')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(maxlen=maxlen + maxlen_delta, maxreq=maxreq + maxreq_delta))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write(request)
        response = stream.read_response()
    asserts.status(response, status)
    asserts.reason_phrase(response, msg)


def _headers_len_for_raw_request(request):
    return len(request.to_raw_request().headers.headers)


def _build_request_with_exact_number_of_headers(count):
    request = http.request.get()
    necessary_headers_count = _headers_len_for_raw_request(request)
    target_headers_count = count - necessary_headers_count
    headers = []
    for i in xrange(target_headers_count):
        headers.append(('h_{}'.format(i), str(i)))
    return http.request.get(headers=headers)


@pytest.mark.parametrize('maxheaders', [10, 25])
def test_maxheaders_limit_fit(ctx, maxheaders):
    """
    BALANCER-1116
    Если число заголовков в запросе не превышает maxheaders, то балансер
    обработает запрос
    """
    request = _build_request_with_exact_number_of_headers(maxheaders)
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(maxheaders=maxheaders, maxlen=64*1024, maxreq=64*1024))

    response = ctx.perform_request(request)
    asserts.status(response, 200)


@pytest.mark.parametrize('maxheaders', [10, 25])
def test_maxheaders_limit_exceed(ctx, maxheaders):
    """
    BALANCER-1116
    Если число заголовков в запросе превышает maxheaders, то балансер
    ответит кодом 413
    """
    request = _build_request_with_exact_number_of_headers(maxheaders + 1)
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(maxheaders=maxheaders, maxlen=64*1024, maxreq=64*1024))

    response = ctx.perform_request(request)
    asserts.status(response, 413)


@pytest.mark.parametrize(
    'req',
    [
        'a b c d e f g h\r\n\r\n',
        'GET / HTTP/1.1\r\nHead\r\n\r\n',
        'GET /\007 HTTP/1.1\r\nBad\000Header: Bad\001Value\r\n\r\n',
        'GET /a\tb HTTP/1.1\r\n\r\n',
        'GET / HTTTP/1.1\r\n\r\n',
        pytest.param('POST / HTTP/1.1\r\nContent-Length: -3\r\n\r\n0123456789', marks=pytest.mark.xfail),
        pytest.param('POST / HTTP/1.1\r\nContent-Length: aaa\r\n\r\n0123456789', marks=pytest.mark.xfail),
    ],
    ids=[
        'starting_line',
        'header',
        'symbols',
        'url_with_tab',
        'version',
        'negative_content_length',
        'nonnumerical_content_length',
    ]
)
def test_badhttp(ctx, req):
    """
    SEPE-4768
    BALANCER-55
    Если формат запроса не соответствует стандарту,
    то балансер закрывает соединение с клиентом,
    в errorlog-е появляется запись "http request parse error: 400",
    в статистике увеличивается счетчик badhttp
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_RESPONSE)))
    ctx.start_balancer(HTTPBalancerConfig(stats_attr='balancer'))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write(req)
        response = stream.read_response()
        asserts.is_closed(conn.sock)

    time.sleep(1)
    log = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)

    asserts.status(response, 400)
    asserts.reason_phrase(response, 'Bad request')
    assert 'http request parse error: 400' in log

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-badhttp_summ'] == 1
    assert unistat['http-http_request_parse_error_summ'] == 1


@pytest.mark.skipif(True, reason='Need to be discussed')
def test_unknown_method(ctx):
    u"""
    BALANCER-55
    Если запрос содержит неизвестный метод,
    то балансер отвечает 501 статусом,
    в errorlog-е появляется запись "...",
    в статистике увеличивается счетчик ...
    """
    pass


def test_cut_server_header(ctx):
    """
    Балансер не должен передавать заголовок Server от backend-а клиенту
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'Server': 'simple server'})))
    ctx.start_balancer(HTTPBalancerConfig())

    response = ctx.perform_request(http.request.get())

    asserts.no_header(response, 'server')


def start_nokeepalive(ctx):
    no_keepalive_file = ctx.manager.fs.create_file('no_keepalive_file')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(no_keepalive_file=no_keepalive_file))
    time.sleep(2)
    return no_keepalive_file


def check_keepalive(ctx, request):
    with ctx.create_http_connection() as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
        asserts.is_not_closed(conn.sock)


def check_nokeepalive(ctx, request):
    with ctx.create_http_connection() as conn:
        response = conn.perform_request(request)
        asserts.status(response, 200)
        asserts.is_closed(conn.sock)


def test_no_keepalive_file(ctx):
    """
    SEPE-5734
    При появлении файла, указанного в no_keepalive_file,
    балансер должен всегда закрывать соединение после ответа клиенту
    """
    no_keepalive_file = ctx.manager.fs.create_file('no_keepalive_file')
    ctx.manager.fs.remove(no_keepalive_file)
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(no_keepalive_file=no_keepalive_file))

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)

        ctx.manager.fs.rewrite(no_keepalive_file, '')
        time.sleep(1.1)

        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        asserts.is_closed(conn.sock)


def test_no_keepalive_file_removed(ctx):
    """
    SEPE-5734
    После удаления файла, указанного в no_keepalive_file,
    балансер не должен закрывать соединение после ответа клиенту при включенном keepalive
    """
    no_keepalive_file = start_nokeepalive(ctx)

    check_nokeepalive(ctx, http.request.get())

    ctx.manager.fs.remove(no_keepalive_file)
    time.sleep(1.1)

    check_keepalive(ctx, http.request.get())


def test_no_keepalive_file_http10(ctx):
    """
    SEPE-5734
    Если есть файл, указанный в no_keepalive_file и клиент задает запрос по HTTP/1.0
    с заголовком Connection: Keep-Alive, то балансер должен закрыть соединение после ответа
    """
    start_nokeepalive(ctx)
    check_nokeepalive(ctx, http.request.raw_get(version='HTTP/1.0', headers={'Connection': 'Keep-Alive'}))


def test_no_keepalive_file_close(ctx):
    """
    SEPE-5734
    Балансер должен закрывать соединение с клиентом, если есть заголовок Connection: Close,
    вне зависимости от того, есть или нет файл, указанный в no_keepalive_file
    """
    no_keepalive_file = start_nokeepalive(ctx)

    check_nokeepalive(ctx, http.request.raw_get(headers={'Connection': 'Close'}))

    ctx.manager.fs.remove(no_keepalive_file)
    time.sleep(1.1)

    check_nokeepalive(ctx, http.request.raw_get(headers={'Connection': 'Close'}))


def test_no_keepalive_file_nokeepalive(ctx):
    """
    SEPE-5734
    Балансер должен закрывать соединение с клиентом, если в конфиге указан keepalive = 0,
    вне зависимости от того, есть или нет файл, указанный в no_keepalive_file
    """
    no_keepalive_file = ctx.manager.fs.create_file('no_keepalive_file')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(no_keepalive_file=no_keepalive_file, keepalive=0))

    check_nokeepalive(ctx, http.request.get())

    ctx.manager.fs.remove(no_keepalive_file)
    time.sleep(1.1)

    check_nokeepalive(ctx, http.request.get())


def test_trace_not_allowed(ctx):
    """
    SEPE-5563
    Если allow_trace = 0, то балансер должен возвращать 405 на запрос TRACE
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(allow_trace=0))

    response = ctx.perform_request(http.request.trace())
    asserts.status(response, 405)


def test_trace_allowed(ctx):
    """
    SEPE-5563
    Если allow_trace = 1, то балансер должен перенаправлять запрос TRACE backend-у
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(allow_trace=1))

    response = ctx.perform_request(http.request.trace())
    asserts.status(response, 200)


def test_trace_allow_header(ctx):
    """
    SEPE-5583
    Если allow_trace = 0, то на запрос TRACE балансер должен ответить 405 с заголовком Allow
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(allow_trace=0))

    response = ctx.perform_request(http.request.trace())
    asserts.header(response, 'Allow')


@pytest.mark.parametrize(
    ('on_by_config', 'on_by_file'),
    [(False, False), (True, False), (False, True)],
    ids=["default", "on_by_config", "on_by_file"]
)
def test_allow_webdav(ctx, on_by_config, on_by_file):
    """
    BALANCER-3153, BALANCER-3164
    webdav, the only http extension in the standard, should be allowed by default
    but it should be possible to disable it
    """
    ctx.start_backend(SimpleConfig())

    if on_by_config:
        ctx.start_balancer(HTTPBalancerConfig(allow_webdav=True))
    elif on_by_file:
        allow_webdav_file = ctx.manager.fs.create_file('allow_webdav_file')
        ctx.start_balancer(HTTPBalancerConfig(allow_webdav_file=allow_webdav_file))
    else:
        ctx.start_balancer(HTTPBalancerConfig())

    webDavReq = HTTPRequest(
        HTTPRequestLine('PROPFIND', '/', 'HTTP/1.1'),
        None,
        None,
    )

    # http should always keep working
    asserts.status(ctx.perform_request(HTTPRequest(HTTPRequestLine('DELETE', '/', 'HTTP/1.1'), None, None,)), 200)
    # garbage should never be accepted
    asserts.status(ctx.perform_request(HTTPRequest(HTTPRequestLine('XXX', '/', 'HTTP/1.1'), None, None,)), 400)
    # http2 preface method should never be accepted
    asserts.status(ctx.perform_request(HTTPRequest(HTTPRequestLine('PRI', '*', 'HTTP/1.1'), None, None,)), 400)

    webDavResp = ctx.perform_request(webDavReq)

    if on_by_file or on_by_config:
        asserts.status(webDavResp, 200)
    else:
        asserts.status(webDavResp, 405)

    if on_by_file:
        ctx.manager.fs.remove(allow_webdav_file)
        time.sleep(1.1)
        asserts.status(ctx.perform_request(webDavReq), 405)


def test_trace_keepalive(ctx):
    """
    SEPE-6004
    Если allow_trace = 0 и клиент задает запрос TRACE с keepalive,
    то балансер должен вернуть 405 и не закрывать соединение
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(allow_trace=0))

    with ctx.create_http_connection() as conn:
        response = ctx.perform_request(http.request.raw_trace(headers={'Connection': 'Keep-Alive'}))
        asserts.status(response, 405)
        asserts.is_not_closed(conn.sock)


def test_trace_allowed_keepalive(ctx):
    """
    SEPE-6004
    Если allow_trace = 1 и backend ответил 405 на клиентский запрос TRACE с keepalive,
    то балансер не должен закрывать соединение
    """
    ctx.start_backend(SimpleConfig(response=http.response.not_allowed()))
    ctx.start_balancer(HTTPBalancerConfig(allow_trace=1))

    with ctx.create_http_connection() as conn:
        response = ctx.perform_request(http.request.raw_trace(headers={'Connection': 'Keep-Alive'}))
        asserts.status(response, 405)
        asserts.is_not_closed(conn.sock)


@pytest.mark.parametrize('multiple_hosts_enabled', [None, False])
def test_multiple_hosts_disabled(ctx, multiple_hosts_enabled):
    """
    BALANCER-1141
    https://tools.ietf.org/html/rfc7230#section-5.4
    Если пришёл запрос с несколькими заголовками Host, то балансер
    должен ответить HTTP 400 и закрыть соединение
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(multiple_hosts_enabled=multiple_hosts_enabled))

    http_versions = ['HTTP/1.0', 'HTTP/1.1']
    headers = [('Host', 'yandex.ru'), ('Host', 'scorpions')]

    for version in http_versions:
        request = http.request.get(version=version, headers=headers)
        response = ctx.perform_request(request)
        asserts.status(response, 400)


def test_multiple_hosts_enabled(ctx):
    """
    BALANCER-1141
    https://tools.ietf.org/html/rfc7230#section-5.4
    Если в конфиге явно указано разрешение на множественные заголовки
    Host, то балансер пробрасывает их "как есть"
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(multiple_hosts_enabled=True))

    response = ctx.perform_request(http.request.get(headers=[('Host', 'yandex.ru'), ('Host', 'scorpions')]))
    asserts.status(response, 200)


def test_patch(ctx):
    """
    SEPE-7952
    Проверка PATCH запросов с Content-Length
    """
    request = http.request.patch(data='12345abc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    response = ctx.perform_request(request)
    backend_req = ctx.backend.state.get_request()

    asserts.status(response, 200)
    assert backend_req == request


def test_patch_chunked(ctx):
    """
    SEPE-7952
    Проверка chunked PATCH запросов
    """
    request = http.request.patch(data=['12345', 'abc'])
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    response = ctx.perform_request(request)
    backend_req = ctx.backend.state.get_request()

    asserts.status(response, 200)
    assert backend_req == request


def test_header_colon(ctx):
    """
    SEPE-3892
    Поведение: балансер не должен отгрызать :: у хедеров
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig())

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write(COLON_REQUEST)
        stream.read_response()

    req = ctx.backend.state.get_request()
    asserts.header_values(req, 'AAA', [':BBB', ': BBB', ':BBB', '::BBB', ':BBB'])


def test_keepalive_requests(ctx):
    count = 3
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(keepalive_requests=count))

    with ctx.create_http_connection() as conn:
        for _ in xrange(count - 1):
            response = conn.perform_request(http.request.get())
            asserts.status(response, 200)
            asserts.is_not_closed(conn.sock)

        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        asserts.is_closed(conn.sock)


@pytest.mark.parametrize('timeout', ['0s', '5s'])
def test_keepalive_timeout(ctx, timeout):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(keepalive_timeout=timeout))

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        if timeout == '0s':
            asserts.is_closed(conn.sock)
        else:
            asserts.is_not_closed(conn.sock)

            time.sleep(5.5)

            try:
                conn.perform_request(http.request.get())
            except socket.error as e:
                assert e.errno == 104
                asserts.is_closed(conn.sock)


def test_slow_send(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(keepalive_timeout="0.1s"))

    packet = 'GET / HTTP/1.1\r\n\r\n'
    with ctx.create_http_connection() as conn:
        conn.sock.send(packet[:1])
        time.sleep(0.5)
        conn.sock.send(packet[1:])

        stream = conn.create_stream()
        response = stream.read_response()
        asserts.status(response, 200)
        asserts.is_closed(conn.sock)


@pytest.mark.parametrize('probability', [0.5, 1.0])
def test_keepalive_probability(ctx, probability):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(keepalive_drop_probability=probability))

    request = http.request.get()
    closed, count = 0, 0
    for _ in xrange(50):
        with ctx.create_http_connection() as conn:
            for __ in xrange(2):
                count += 1
                conn.perform_request(request)
                if conn.sock.is_closed():
                    closed += 1
                    break
    variance = math.sqrt(count * probability * (1 - probability))
    abs_variance = 3 * variance
    expected = count * probability
    assert expected - abs_variance <= closed <= expected + abs_variance


@pytest.mark.parametrize(
    'req',
    [
        'GET / HTTP/1.1\r\n\r\n\r\n\r\nGET / HTTP/1.1\r\n\r\n'
        'GET / HTTP/1.1\r\n\r\n\r\r\r\n\n\rGET / HTTP/1.1\r\n\r\n'
    ],
)
def test_extra_spaces(ctx, req):
    """
    BALANCER-2173
    Балансер должен игнорировать лишние пробельные символы в конце запроса
    """
    base_one_packet_test(ctx, req)


@pytest.mark.parametrize('allow_client_hints_restore', [True, False])
@pytest.mark.parametrize('disable_client_hints_restore', [True, False])
def test_client_headers_empty(ctx, allow_client_hints_restore, disable_client_hints_restore):
    """
    BALANCER-3222
    Балансер должен добавить дополнительные заголовки при обработке Client Hints заголовков,
    если это разрешено через опции и не запрещено через файл.
    Проверяем пустой запрос.
    """
    if disable_client_hints_restore:
        disable_client_hints_restore_file = ctx.manager.fs.create_file('disable_client_hints_restore_file')
    else:
        disable_client_hints_restore_file = None
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(
        allow_client_hints_restore=allow_client_hints_restore,
        client_hints_ua_header='Y-CH-Restored',
        client_hints_ua_proto_header='Y-CH-Restored-Proto',
        disable_client_hints_restore_file=disable_client_hints_restore_file
    ))

    ctx.perform_request(http.request.get(headers={}))
    req = ctx.backend.state.get_request()
    if allow_client_hints_restore and not disable_client_hints_restore:
        asserts.header_value(req, 'Y-CH-Restored', '')
        asserts.header_value(req, 'Y-CH-Restored-Proto', '')
    else:
        asserts.no_header(req, 'Y-CH-Restored')
        asserts.no_header(req, 'Y-CH-Restored-Proto')


@pytest.mark.parametrize('allow_client_hints_restore', [True, False])
@pytest.mark.parametrize('disable_client_hints_restore', [True, False])
def test_client_headers_chrome(ctx, allow_client_hints_restore, disable_client_hints_restore):
    """
    BALANCER-3222
    Балансер должен добавить дополнительные заголовки при обработке Client Hints заголовков,
    если это разрешено через опции и не запрещено через файл.
    Проверяем запрос из Chrome v91.
    """
    if disable_client_hints_restore:
        disable_client_hints_restore_file = ctx.manager.fs.create_file('disable_client_hints_restore_file')
    else:
        disable_client_hints_restore_file = None
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(
        allow_client_hints_restore=allow_client_hints_restore,
        client_hints_ua_header='Y-CH-Restored',
        client_hints_ua_proto_header='Y-CH-Restored-Proto',
        disable_client_hints_restore_file=disable_client_hints_restore_file
    ))

    ctx.perform_request(http.request.get(headers={
        "sec-ch-ua": "\" Not;A Brand\";v=\"99\", \"Google Chrome\";v=\"91\", \"Chromium\";v=\"91\"",
        "sec-ch-ua-mobile": "?0"
    }))
    req = ctx.backend.state.get_request()
    if allow_client_hints_restore and not disable_client_hints_restore:
        asserts.header_value(req, 'Y-CH-Restored', 'Mozilla/5.0 ( ; ) AppleWebKit/537.36 (KHTML, like Gecko)' +
                                '  Not;A Brand/99 Chrome/91 Chrome/91  Safari/537.36')
        asserts.header_value(req, 'Y-CH-Restored-Proto', 'GgZXZWJLaXQiBkNocm9tZTIIQ2hyb21pdW1iBAAAAABqBAAAAA' +
                                'ByBAAAAAB6BAAAAACCAQQAAAAAigEEAAAAAJIBBAAAAACaAQQAAAAAogEEAAAAAKoBBAAAAAA=')
    else:
        asserts.no_header(req, 'Y-CH-Restored')
        asserts.no_header(req, 'Y-CH-Restored-Proto')


def test_client_headers_size(ctx):
    """
    BALANCER-3290
    Балансер не должен добавить дополнительные заголовки при превышении объема заголовков
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HTTPBalancerConfig(
        allow_client_hints_restore=True,
        client_hints_ua_header='Y-CH-Restored',
        client_hints_ua_proto_header='Y-CH-Restored-Proto'
    ))

    ctx.perform_request(http.request.get(headers={
        "sec-ch-ua": "A" * 2050,
        "sec-ch-ua-mobile": "B" * 2050
    }))
    req = ctx.backend.state.get_request()
    asserts.no_header(req, 'Y-CH-Restored')
    asserts.no_header(req, 'Y-CH-Restored-Proto')
