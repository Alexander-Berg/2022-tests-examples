# -*- coding: utf-8 -*-
import pytest
import time
import socket
import re

from common import watch_client_close, connection_manager_required
from configs import (
    ProxyConfig, ProxyStatusCodeBlacklistConfig, ProxyDefaultsConfig, ProxyRetryConfig, ProxyBufferingConfig,
    BugNotWaitingResponseConfig, ProxyResolutionErrorConfig
)

from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
import balancer.test.util.asserts as asserts2
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import (
    SimpleConfig, SimpleHandler, DummyConfig, SimpleDelayedConfig, ThreeModeChunkedDelayConfig, ChunkedConfig,
    SlowBackendReadConfig
)
from balancer.test.util.proto.http.stream import HTTPReaderException


class IpVersionFilteringHandler(SimpleHandler):

    def handle_parsed_request(self, raw_request, stream):
        ip = self.sock.sock_ip
        is_ip_v4 = ip.find(".") != -1
        is_ip_v6 = ip.find(":") != -1

        assert (is_ip_v4 or is_ip_v6) and not (is_ip_v4 and is_ip_v6), "ip address should be either IPv4 or IPv6"

        if is_ip_v4 and self.config.ignore_ip_v4:
            self.force_close()
        if is_ip_v6 and self.config.ignore_ip_v6:
            self.force_close()
        else:
            stream.write_response(self.config.response)
            self.finish_response()


class IpVersionFilteringConfig(SimpleConfig):
    HANDLER_TYPE = IpVersionFilteringHandler

    def __init__(self, ignore_ip_v4=False, ignore_ip_v6=False):
        super(IpVersionFilteringConfig, self).__init__()
        self.ignore_ip_v4 = ignore_ip_v4
        self.ignore_ip_v6 = ignore_ip_v6


def start_keepalive_incomplete_input(ctx, keepalive_count, watch_client_close, connection_manager_required):
    ctx.start_backend(SlowBackendReadConfig(response_before_body=True, response=http.response.ok()))
    ctx.start_balancer(ProxyConfig(
        keepalive_count=keepalive_count,
        watch_client_close=watch_client_close,
        connection_manager_required=connection_manager_required,
    ))


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_incomplete_user_nokeepalive_input(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    data = 'a' * 10
    request = http.request.raw_post(headers=[('content-length', len(data)), ('connection', 'close')])
    for i in xrange(2):
        with ctx.create_http_connection() as conn:
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write(data[:5])


@watch_client_close
@connection_manager_required
def test_incomplete_user_keepalive_input(ctx, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой. Пользовательское keep-alive соединение
    не должно ломаться.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    data = 'a' * 10
    request = http.request.raw_post(headers=[('content-length', len(data)), ('connection', 'Keep-Alive')])
    with ctx.create_http_connection() as conn:
        for i in xrange(2):
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            stream.write(data)


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_partial_user_nokeepalive_input(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    data = 'a' * 10
    request = http.request.raw_post(headers=[('content-length', len(data)), ('connection', 'close')])
    for i in xrange(2):
        with ctx.create_http_connection() as conn:
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            stream.write(data[:5])
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write(data[5:])


@watch_client_close
@connection_manager_required
def test_partial_user_keepalive_input(ctx, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой. Пользовательское keep-alive соединение
    не должно ломаться.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    data = 'a' * 10
    request = http.request.raw_post(headers=[('content-length', len(data)), ('connection', 'Keep-Alive')])
    with ctx.create_http_connection() as conn:
        for i in xrange(2):
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            stream.write(data[:5])
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            stream.write(data[5:])


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_incomplete_user_nokeepalive_input_chunked(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked'), ('connection', 'close')])
    for i in xrange(2):
        with ctx.create_http_connection() as conn:
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write_chunk('scorpions')


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_incomplete_user_keepalive_input_chunked(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой. Пользовательское keep-alive соединение
    не должно ломаться.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked'), ('connection', 'Keep-Alive')])
    with ctx.create_http_connection() as conn:
        for i in xrange(2):
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write_chunk('scorpions')
            stream.write_chunk('')


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_partial_user_nokeepalive_input_chunked(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked'), ('connection', 'close')])
    for i in xrange(2):
        with ctx.create_http_connection() as conn:
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            stream.write_chunk('led')
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write_chunk('zeppelin')


@pytest.mark.parametrize('send_after_response', [False, True])
@watch_client_close
@connection_manager_required
def test_partial_user_keepalive_input_chunked(ctx, send_after_response, watch_client_close, connection_manager_required):
    """
    BALANCER-553
    Если клиент приходит с неполным телом, а в proxy до бэкэнда включён keep-alive,
    и бэкэнд сразу после получения заголовков отдаёт полный ответ, то
    клиент должен получить полный ответ, а также последующие запросы
    не должны завершаться ошибкой. Пользовательское keep-alive соединение
    не должно ломаться.
    """
    start_keepalive_incomplete_input(ctx, 1, watch_client_close, connection_manager_required)
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked'), ('connection', 'Keep-Alive')])
    with ctx.create_http_connection() as conn:
        for i in xrange(2):
            stream = conn.create_stream()
            stream.write_request_line(request.request_line)
            stream.write_headers(request.headers)
            stream.write_chunk('led')
            response = stream.read_response().to_response()
            asserts2.status(response, 200)
            if send_after_response:
                stream.write_chunk('zeppelin')
            stream.write_chunk('')


@watch_client_close
def test_fail_on_5xx_default(ctx, watch_client_close):
    """
    SEPE-8226
    По умолчанию fail_on_5xx должно быть равно 1
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=503, reason='Service Unavailable')))
    ctx.start_balancer(ProxyDefaultsConfig(watch_client_close=watch_client_close))

    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'OK')


@watch_client_close
def test_fail_on_empty_reply_default(ctx, watch_client_close):
    """
    SEPE-8226
    BALANCER-1288
    По умолчанию fail_on_empty_reply должно быть равно 1
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(ProxyDefaultsConfig(watch_client_close=watch_client_close))

    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'OK')


@watch_client_close
def test_connect_retry_dead(ctx, watch_client_close):
    """
    BALANCER-50
    connect_retry_delay и connect_retry_timeout не связаны
    """
    ctx.start_fake_backend()
    ctx.start_balancer(ProxyRetryConfig(watch_client_close=watch_client_close))

    start = time.time()
    response = ctx.perform_request(http.request.get())
    end = time.time()

    asserts.status(response, 501)
    assert 4.0 < (end - start) < 4.1  # connect_retry_timeout = "4.5s"


@watch_client_close
def test_connect_retry_alive(ctx, watch_client_close):
    """
    BALANCER-50
    Проверить, что быстро поднятое не считается упавшим
    """
    ctx.start_fake_backend()
    ctx.start_balancer(ProxyRetryConfig(watch_client_close=watch_client_close))

    start = time.time()
    conn = ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get().to_raw_request())
    time.sleep(1.1)
    # start_balancer is not instant, it takes ~1s
    ctx.manager.backend.start(SimpleConfig(), port=ctx.backend.server_config.port)
    response = stream.read_response()
    end = time.time()

    asserts.status(response, 200)
    asserts.content(response, '')
    assert (end - start) < 3.5


@watch_client_close
def test_use_only_ipv4(ctx, watch_client_close):
    """
    BALANCER-580
    Проверить, что балансер запускается с опцией use_only_ipv4
    """
    ctx.start_backend(IpVersionFilteringConfig(ignore_ip_v6=True))
    ctx.start_balancer(ProxyConfig(
        use_only_ipv4=True, host='127.0.0.1',
        watch_client_close=watch_client_close,
    ))
    response = ctx.perform_request(http.request.post(data='data'))
    asserts.status(response, 200)


@watch_client_close
def test_use_only_ipv6(ctx, watch_client_close):
    """
    BALANCER-580
    Проверить, что балансер запускается с опцией use_only_ipv6
    """
    ctx.start_backend(IpVersionFilteringConfig(ignore_ip_v4=True))
    ctx.start_balancer(ProxyConfig(
        use_only_ipv6=True, host='::1',
        watch_client_close=watch_client_close,
    ))
    response = ctx.perform_request(http.request.post(data='data'))
    asserts.status(response, 200)


@watch_client_close
def test_use_both_ipv4_and_ipv6_failure(ctx, watch_client_close):
    """
    BALANCER-580
    Проверить, что балансер НЕ запускается с одновременно включенными
    опциями use_only_ipv4 и use_only_ipv6
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=200, reason='OK')))
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ProxyConfig(
            use_only_ipv4=True, use_only_ipv6=True,
            watch_client_close=watch_client_close,
        ))


@watch_client_close
def test_big_chunked_to_http10_client(ctx, watch_client_close):
    """
    Клиент задает запрос по HTTP/1.0, бэкенд возвращает большой chunked ответ.
    Балансер должен вернуть ответ целиком без content-length.
    """
    backend_resp = http.response.ok(data=['A' * 500000000] * 2)
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(ProxyConfig(
        backend_timeout='30s',
        watch_client_close=watch_client_close,
    ))

    conn = ctx.create_http_connection(timeout=100)
    balancer_resp = conn.perform_request_raw_response(http.request.get(version='HTTP/1.0'))

    assert backend_resp == balancer_resp.to_response()


def start_with_wait_backend(
    ctx, balancer_timeout, backend_timeout, watch_client_close
):
    ctx.start_backend(SimpleDelayedConfig(response_delay=backend_timeout))
    ctx.start_balancer(ProxyConfig(
        backend_timeout=balancer_timeout,
        watch_client_close=watch_client_close,
    ))


@watch_client_close
def test_backend_timeout_positive(ctx, watch_client_close):
    """
    SEPE-3959
    Если backend успевает ответить за timeout, то балансер передает его ответ клиенту
    """
    timeout = 2
    start_with_wait_backend(
        ctx, timeout, timeout * 0.9, watch_client_close
    )
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


@watch_client_close
def test_backend_timeout_negative(ctx, watch_client_close):
    """
    SEPE-3959
    Если backend не успевает ответить за timeout, то балансер закрывает соедниение с клиентом
    """
    timeout = 2
    start_with_wait_backend(
        ctx, timeout, timeout * 1.1, watch_client_close
    )
    ctx.perform_request_xfail(http.request.get(), timeout=timeout + 1)


@watch_client_close
def test_backend_read_timeout_positive(ctx, watch_client_close):
    timeout = 2
    ctx.start_backend(SimpleDelayedConfig(response_delay=timeout + 1))
    ctx.start_balancer(ProxyConfig(
        client_read_timeout=timeout, backend_read_timeout=timeout,
        watch_client_close=watch_client_close,
    ))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


@watch_client_close
def test_backend_read_timeout_negative(ctx, watch_client_close):
    timeout = 2
    ctx.start_backend(ChunkedConfig(response=http.response.ok(data=['chunk1', 'chunk2']), chunk_timeout=timeout * 1.1))
    ctx.start_balancer(ProxyConfig(
        client_read_timeout=timeout, backend_read_timeout=timeout,
        watch_client_close=watch_client_close,
    ))
    ctx.perform_request_xfail(http.request.get(), timeout=timeout + 1)


@watch_client_close
def test_client_read_timeout_positive(ctx, watch_client_close):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ProxyConfig(
        client_read_timeout=1, backend_read_timeout=1,
        watch_client_close=watch_client_close,
    ))
    ctx.perform_request(http.request.get())


@watch_client_close
def test_client_read_timeout_negative(ctx, watch_client_close):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ProxyConfig(
        client_read_timeout=1, backend_read_timeout=1, backend_timeout=5,
        watch_client_close=watch_client_close,
    ))

    ctx.perform_request(http.request.get())

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()
        stream.write_chunk('A' * 10)
        time.sleep(2)
        stream.write_chunk('B' * 10)
        stream.write_chunk('')
        try:
            stream.read_response()
        except HTTPReaderException:
            pass
        else:
            assert False


@watch_client_close
def test_buffering(ctx, watch_client_close):
    """
    SEPE-4787
    В опциях балансера проставлено buffering = 1
    Если backend, на который пришел запрос, не успевает ответить за timeout,
    то запрос перезадается другому backend-у.
    """
    path = '/led'
    backend_resp = http.response.ok(data=['A' * 5] * 10)
    ctx.start_backend(ThreeModeChunkedDelayConfig(
        prefix=1, first=1, second=0, response=backend_resp, chunk_timeout=1))
    ctx.start_balancer(ProxyBufferingConfig(
        watch_client_close=watch_client_close,
    ))

    balancer_resp = ctx.perform_request(http.request.get(path=path))
    asserts.content(balancer_resp, backend_resp.data.content)
    assert ctx.backend.state.requests.qsize() == 2
    req1 = ctx.backend.state.get_request()
    req2 = ctx.backend.state.get_request()
    asserts.path(req1, path)
    asserts.path(req2, path)


@pytest.mark.parametrize(
    'version',
    ['HTTP/1.0', 'HTTP/1.1'],
    ids=['http10', 'http11']
)
@watch_client_close
def test_buffering_not_modified(ctx, version, watch_client_close):
    """
    Балансер обрабатывает ответ бэкенда с пустым телом и 304 статусом.
    """
    ctx.start_backend(SimpleConfig(response=http.response.not_modified()))
    ctx.start_balancer(ProxyBufferingConfig(
        watch_client_close=watch_client_close,
    ))

    response = ctx.perform_request(http.request.get(version=version))

    asserts.status(response, 304)


@watch_client_close
def test_fail_on_5xx_on(ctx, watch_client_close):
    """
    SEPE-6563
    В модуле proxy утсановлено fail_on_5xx = 1, бэкэнд отвечает
    кодом 598 (не по RFC). Балансер закрывает соединение с клиентом.
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=598, reason='Network read timeout error')))
    ctx.start_balancer(ProxyConfig(
        fail_on_5xx=1, watch_client_close=watch_client_close,
    ))

    ctx.perform_request_xfail(http.request.get())


@watch_client_close
def test_fail_on_5xx_off(ctx, watch_client_close):
    """
    SEPE-6563
    В модуле proxy утсановлено fail_on_5xx = 0, бэкэнд отвечает
    кодом 598 (не по RFC). Балансер возвращает ответ бэкэнда
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=598, reason='Network read timeout error')))
    ctx.start_balancer(ProxyConfig(
        fail_on_5xx=0, watch_client_close=watch_client_close,
    ))

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 598)


@pytest.mark.parametrize(['response_status', 'status_code_blacklist'], [
    (598, '5xx'),
    (598, '598'),
    (598, '5xx,404'),
    (404, '4xx'),
    (404, '404'),
    (404, '5xx,404'),
])
def test_status_code_blacklist(ctx, response_status, status_code_blacklist):
    """
    BALANCER-876
    Если бэкэнд отвечает статус-кодом из status_code_blacklist, то этот запрос считается
    обработанным с ошибкой
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=response_status, reason='Network read timeout error')))
    ctx.start_balancer(ProxyStatusCodeBlacklistConfig(status_code_blacklist=status_code_blacklist))

    ctx.perform_request_xfail(http.request.get())


@pytest.mark.parametrize(['response_status', 'status_code_blacklist', 'status_code_blacklist_exceptions'], [
    (598, '5xx', '598'),
    (598, '5xx,404', '598'),
    (404, '4xx', '404'),
])
def test_status_code_blacklist_exceptions(ctx, response_status, status_code_blacklist, status_code_blacklist_exceptions):
    """
    BALANCER-876
    Если бэкэнд отвечает статус-кодом из status_code_blacklist, то этот запрос считается
    обработанным с ошибкой, если этого статус-кода нет в status_code_blacklist_exceptions
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=response_status, reason='Network read timeout error')))
    ctx.start_balancer(ProxyStatusCodeBlacklistConfig(
        status_code_blacklist=status_code_blacklist,
        status_code_blacklist_exceptions=status_code_blacklist_exceptions
    ))

    response = ctx.perform_request(http.request.get())
    asserts.status(response, response_status)


@pytest.mark.parametrize(['response_status', 'status_code_blacklist', 'status_code_blacklist_exceptions'], [
    (598, '5xx', '503'),
    (598, '5xx,404', '500'),
    (404, '4xx', '403'),
])
def test_status_code_blacklist_exceptions_miss(ctx, response_status, status_code_blacklist, status_code_blacklist_exceptions):
    """
    BALANCER-876
    Если бэкэнд отвечает статус-кодом из status_code_blacklist, то этот запрос считается
    обработанным с ошибкой, если этого статус-кода нет в status_code_blacklist_exceptions.
    Усли статус кода нет в писке exceptions, применяются правила из blacklist.
    """
    ctx.start_backend(SimpleConfig(response=http.response.some(status=response_status, reason='Network read timeout error')))
    ctx.start_balancer(ProxyStatusCodeBlacklistConfig(
        status_code_blacklist=status_code_blacklist,
        status_code_blacklist_exceptions=status_code_blacklist_exceptions
    ))

    ctx.perform_request_xfail(http.request.get())


@watch_client_close
def test_not_waiting_response(ctx, watch_client_close):
    """
    SEPE-5371
    Включены одновременно errorlog и accesslog, включена опция keepalive к backend-у
    Backend только слушает порт, ничего не отвечая
    Клиент дважды задает запрос и закрывает соединение, не дожидаясь ответа
    После этого backend закрывает соединение
    Балансер не должен упасть
    """
    timeout = 2
    ctx.start_backend(DummyConfig(timeout=timeout))
    ctx.start_balancer(BugNotWaitingResponseConfig(
        backend_timeout=timeout + 5,
        watch_client_close=watch_client_close,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())
    time.sleep(timeout + 2)

    assert ctx.balancer.is_alive()


@watch_client_close
def test_count_body_length(ctx, watch_client_close):
    """
    BALANCER-703
    Длина тела будет записана в лог
    """
    ctx.start_backend(SimpleConfig(), name='backend')
    ctx.start_balancer(ProxyConfig(
        watch_client_close=watch_client_close,
    ))

    msg_len = 50
    response = ctx.perform_request(http.request.post(data='A' * msg_len))
    asserts.status(response, 200)

    for run in Multirun():
        with run:
            logdata = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert re.search('\\s%d/\\d+\\s' % msg_len, logdata)


@watch_client_close
def test_count_body_length_chunked(ctx, watch_client_close):
    """
    BALANCER-703
    Длина тела будет записана в лог
    """
    ctx.start_backend(SimpleConfig(), name='backend')
    ctx.start_balancer(ProxyConfig(
        watch_client_close=watch_client_close,
    ))

    parts = ['A' * 10 for i in xrange(5)]
    response = ctx.perform_request(http.request.post(data=parts))
    asserts.status(response, 200)

    msg_len = sum(map(len, parts))
    for run in Multirun():
        with run:
            logdata = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert re.search('\\s%d/\\d+\\s' % msg_len, logdata)


@pytest.mark.parametrize(['cached_ip', 'log_level', 'output_expected'],
                         [(None, None, True),
                         ('127.0.0.1', None, False),
                         ('127.0.0.1', 'INFO', True)],
                         ids=['no_cached_ip', 'cached_ip', 'cached_ip_info_log_level'])
@watch_client_close
def test_errorlog_entry_on_resolution_error(ctx, cached_ip, log_level, output_expected, watch_client_close):
    """
    BALANCER-865
    Если происходит ошибка резолва адреса, в errorlog должно записаться сообщение об ошибке.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="OK")))
    ctx.start_balancer(ProxyResolutionErrorConfig(
        cached_ip=cached_ip, log_level=log_level,
        watch_client_close=watch_client_close,
    ))

    if cached_ip:
        resp = ctx.perform_request(http.request.get())
        asserts.status(resp, 200)
        asserts.content(resp, "OK")
    else:
        ctx.perform_request_xfail(http.request.get())

    assert ctx.balancer.is_alive()

    if not output_expected:
        time.sleep(1)
        log_data = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
        assert "resolving failed" not in log_data
    else:
        for run in Multirun():
            with run:
                log_data = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
                assert "resolving failed" in log_data


@watch_client_close
def test_errorlog_entry_on_client_closing(ctx, watch_client_close):
    """
    BALANCER-1858
    Если клиент разрывает соединение, то в errorlog должно записываться сообщение об ошибке
    из модулей proxy и errorlog.
    """
    ctx.start_backend(SimpleDelayedConfig(response_delay=2.5))
    ctx.start_balancer(ProxyConfig(
        watch_client_close=watch_client_close,
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
        stream.write_request(http.request.get().to_raw_request())

    while True:
        unistat = ctx.get_unistat()
        if unistat['report-default-fail_summ'] == 1:
            break
        time.sleep(1)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
            if watch_client_close:
                assert 'client watch error' in log
            else:
                assert 'client flush error' in log
            assert '(TSystemError) (Input/output error)' in log or '(TSystemError) (Broken pipe)' in log


@watch_client_close
def test_backend_timeout_slow_client(ctx, watch_client_close):
    """
    BALANCER-1622
    Проверяем backend_timeout с медленным клиентом.
    """
    backend_timeout = 1
    ctx.start_backend(SlowBackendReadConfig(http.response.ok(), response_before_body=True))
    ctx.start_balancer(ProxyConfig(
        backend_timeout=backend_timeout,
        watch_client_close=watch_client_close,
    ))
    data = ["A"] * 400

    with ctx.create_http_connection() as conn:
        start = time.time()
        stream = conn.create_stream()
        stream.write_line("POST / HTTP/1.1")
        stream.write_header("Transfer-Encoding", "chunked")
        stream.end_headers()

        response = stream.read_response()
        asserts.status(response, 200)

        with pytest.raises(socket.error):
            for d in data:
                stream.write_chunk(d)
                time.sleep(0.01)
            stream.write_chunk("")

        assert time.time() - start <= 1.1 * backend_timeout

    stats = ctx.get_unistat()
    assert stats['http-http_unfinished_backend_stream_error_summ'] == 0
    assert stats['http-http_unfinished_client_stream_error_summ'] == 0
    time.sleep(1)
    logdata = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert 'client_fail' in logdata
    assert 'client read' in logdata


@watch_client_close
def test_backend_timeout_slow_backend(ctx, watch_client_close):
    """
    BALANCER-1622
    Проверяем backend_timeout с медленным бекендом.
    """
    backend_timeout = 1
    ctx.start_backend(SlowBackendReadConfig(
        response=http.response.ok(),
        chunk_delay=1,
        response_before_body=True,
        recv_buffer_size=2048
    ))
    ctx.start_balancer(ProxyConfig(
        backend_timeout=backend_timeout,
        watch_client_close=watch_client_close,
        socket_out_buffer=2048
    ))
    data = ["A" * 2**16] * 400

    with ctx.create_http_connection() as conn:
        start = time.time()
        stream = conn.create_stream()
        stream.write_line("POST / HTTP/1.1")
        stream.write_header("Transfer-Encoding", "chunked")
        stream.end_headers()

        response = stream.read_response()
        asserts.status(response, 200)

        with pytest.raises(socket.error):
            for d in data:
                stream.write_chunk(d)
                time.sleep(0.01)
            stream.write_chunk("")

        assert time.time() - start <= 1.1 * backend_timeout

    stats = ctx.get_unistat()
    assert stats['http-http_unfinished_client_stream_error_summ'] == 0
    assert stats['http-http_unfinished_backend_stream_error_summ'] == 0
    time.sleep(1)
    logdata = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert 'backend_fail' in logdata
    assert 'backend flush' not in logdata
    assert 'backend write' in logdata


@watch_client_close
def test_allow_connection_upgrade_false(ctx, watch_client_close):
    ctx.start_backend(SimpleConfig(response=http.response.custom(status=101, reason='Switching Protocols')))
    ctx.start_balancer(ProxyConfig(
        allow_connection_upgrade=False,
        watch_client_close=watch_client_close,
    ))
    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers={"Connection": "Upgrade", "Upgrade": "websocket"}))
    headers = ctx.backend.state.get_raw_request().headers["Connection"]
    assert "Upgrade" not in headers


@watch_client_close
def test_allow_connection_upgrade_true(ctx, watch_client_close):
    ctx.start_backend(SimpleConfig(response=http.response.custom(status=101, reason='Switching Protocols')))
    ctx.start_balancer(ProxyConfig(
        allow_connection_upgrade=True,
        watch_client_close=watch_client_close,
    ))
    response = ctx.perform_request(http.request.get(headers={"Connection": "Upgrade", "Upgrade": "websocket"}))
    asserts.status(response, 101)
    headers = ctx.backend.state.get_raw_request().headers["Connection"]
    assert "Upgrade" in headers


@watch_client_close
def test_allow_multiple_connection_tokens(ctx, watch_client_close):
    ctx.start_backend(SimpleConfig(response=http.response.ok()))
    ctx.start_balancer(ProxyConfig(
        allow_connection_upgrade=True,
        watch_client_close=watch_client_close,
    ))
    ctx.perform_request(http.request.get(headers={"Connection": "upgrade, keep-alive"}))
    headers = ctx.backend.state.get_raw_request().headers["Connection"]
    assert "Upgrade" in headers


@watch_client_close
def test_client_flush_exceeding_backend_timeout(ctx, watch_client_close):
    """
    BALANCER-1774
    backend_timeout should not affect client flush
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="A" * 2**25)))
    ctx.start_balancer(ProxyConfig(
        backend_timeout="2s", buffer=2**25 + 2**14,
        watch_client_close=watch_client_close,
    ))
    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
        time.sleep(2.5)
        response = stream.read_response()
        asserts.status(response, 200)
        assert len(response.data.content) == 2**25


def test_client_sending_resp_headers_error(ctx):
    """
    BALANCER-1716
    Если случилась ошибка во время отправки заголовков клиенту,
    то она должна учитываться как ошибка клиента.
    """
    ctx.start_backend(SimpleDelayedConfig(response_delay=2.5))
    ctx.start_balancer(ProxyConfig(buffer=0))
    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
        stream.write_request(http.request.get().to_raw_request())

    while True:
        stats = ctx.get_unistat()
        if stats['report-default-backend_attempt_summ'] == 2:
            break
        time.sleep(1)

    assert stats['report-default-backend_error_summ'] == 0
    assert stats['report-default-client_error_summ'] == 1


@watch_client_close
def test_backend_write_timeout(ctx, watch_client_close):
    """
    BALANCER-1698
    backend_write_timeout should abort slow backend writes
    """
    ctx.start_backend(SlowBackendReadConfig(
        response=http.response.ok(),
        chunk_delay=1.5,
        response_before_body=True,
        recv_buffer_size=2048
    ))
    ctx.start_balancer(ProxyConfig(
        backend_timeout="100s", backend_write_timeout="1s",
        socket_out_buffer=2048,
        watch_client_close=watch_client_close,
    ))
    data = ["A" * 2**16] * 400

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_line("POST / HTTP/1.1")
        stream.write_header("Transfer-Encoding", "chunked")
        stream.end_headers()

        response = stream.read_response()
        asserts.status(response, 200)

        start = time.time()

        with pytest.raises(socket.error):
            for d in data:
                start = time.time()
                stream.write_chunk(d)
            stream.write_chunk("")

        assert time.time() - start <= 2

        time.sleep(1)
        logdata = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
        assert 'backend_fail' in logdata
        assert 'backend flush' not in logdata
        assert 'backend write' in logdata


@watch_client_close
def test_client_write_timeout(ctx, watch_client_close):
    """
    BALANCER-1698
    client_write_timeout should abort slow client writes
    """
    ctx.start_backend(SimpleConfig(
        response=http.response.ok(data="A" * 2**25),
    ))
    ctx.start_balancer(ProxyConfig(
        backend_timeout="100s", client_write_timeout="1s",
        watch_client_close=watch_client_close,
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()

        stream.write_request_line("GET / HTTP/1.1")
        stream.end_headers()

        stream.read_response_line()
        stream.read_headers()
        time.sleep(1.5)

        with pytest.raises(HTTPReaderException):
            stream.read_data()


@pytest.mark.skip("flaky, need to reimplement")
@watch_client_close
def test_client_read_timeout(ctx, watch_client_close):
    """
    BALANCER-1698
    client_read_timeout should abort slow client reads
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()))
    ctx.start_balancer(ProxyConfig(
        backend_timeout="100s", client_read_timeout="1s",
        watch_client_close=watch_client_close,
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()

        stream.write_line("POST / HTTP/1.1")
        stream.write_header("Content-Length", "1")
        stream.end_headers()
        time.sleep(2)
        with pytest.raises(socket.error):
            stream.write("A")


@watch_client_close
@connection_manager_required
def test_keepalive_parameters_enabled(ctx, watch_client_close, connection_manager_required):
    """
    BALANCER-1450
    Проверить, что при выставленных в прокси keepalive параметрах прокси посылает keepalive пробы.
    (проверить количество проб можно только вручную ввиду невозможности оборвать соединение в тестах.)
    """
    idle = 1
    cnt = 5
    intvl = 1

    ctx.start_backend(SimpleConfig(), name='backend')
    ctx.start_balancer(ProxyConfig(
        keepalive_count=1, tcp_keep_idle=idle, tcp_keep_cnt=cnt, tcp_keep_intvl=intvl,
        watch_client_close=watch_client_close,
        connection_manager_required=connection_manager_required,
    ))
    tcpdump = ctx.manager.tcpdump.start(ctx.backend.server_config.port)
    ctx.perform_request(http.request.get())

    time.sleep(5)
    tcpdump.read_all()
    sessions = tcpdump.get_sessions()
    assert len(sessions) > 0
    sess = sessions[0]
    client_packets = sess.other_client_packets
    assert len(client_packets) >= 3  # если больше 3х, то там точно есть пробы


@watch_client_close
def test_http_response_parse_error(ctx, watch_client_close):
    """
    BALANCER-2265
    Если формат ответа бэкенда не соответствует http протоколу,
    то балансер закрывает соединение с клиентом,
    в errorlog-е появляется запись "http response parse error: 400",
    в статистике увеличивается счетчик http_response_parse_error.
    """
    ctx.start_backend(SimpleConfig(response=http.response.custom(status=200, reason='', headers=[('Content-Length\r\n\r\n', 5)])))
    ctx.start_balancer(ProxyConfig(
        watch_client_close=watch_client_close,
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_request_line("GET / HTTP/1.1")
        stream.end_headers()
        with pytest.raises(HTTPReaderException):
            stream.read_response()
        asserts.is_closed(conn.sock)

    time.sleep(1)
    log = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)

    assert 'http response parse error: 400' in log
    assert 'content-length' in log

    unistat = ctx.get_unistat()
    assert unistat['proxy-http_response_parse_error_summ'] == 1
    assert unistat['proxy-http_complete_response_parse_error_summ'] == 1


@connection_manager_required
def test_keepalive_timeout(ctx, connection_manager_required):
    ctx.start_backend(SimpleConfig(), name='backend')
    ctx.start_balancer(ProxyConfig(
        keepalive_count=1, keepalive_timeout='1s',
        connection_manager_required=connection_manager_required,
    ))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    unistat = ctx.get_unistat()
    assert unistat['proxy-unused_keepalives_ammv'] == 1

    time.sleep(2)
    unistat = ctx.get_unistat()
    assert unistat['proxy-unused_keepalives_ammv'] == 0
