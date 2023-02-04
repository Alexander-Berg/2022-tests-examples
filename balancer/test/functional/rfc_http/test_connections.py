# -*- coding: utf-8 -*-
import time
import pytest

from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, DelayedCloseConfig, CloseConfig,\
    ThreeModeHandler, ThreeModeConfig, ThreeModeChunkedDelayConfig


BACKEND_CLOSE = 0
BALANCER_CLOSE = 1

CHUNKED_DATA = ['0123', '456', '789']
DATA = '0123456789'
POST_DATA = 'abcde'

HTTP10_GET = http.request.raw_get(version='HTTP/1.0')
HTTP10_GET_CLOSE = http.request.raw_get(version='HTTP/1.0', headers={'connection': 'close'})
HTTP10_GET_KEEPALIVE = http.request.raw_get(version='HTTP/1.0', headers={'connection': 'Keep-Alive'})
HTTP10_POST_KEEPALIVE = http.request.raw_post(
    version='HTTP/1.0',
    headers={'content-length': len(POST_DATA), 'connection': 'Keep-Alive'},
    data=POST_DATA
)
HTTP11_GET = http.request.raw_get(version='HTTP/1.1')
HTTP11_GET_CLOSE = http.request.raw_get(version='HTTP/1.1', headers={'connection': 'close'})
HTTP11_GET_KEEPALIVE = http.request.raw_get(version='HTTP/1.1', headers={'connection': 'Keep-Alive'})
HTTP11_POST = http.request.raw_post(
    headers={'content-length': len(POST_DATA)},
    data=POST_DATA
)
HTTP11_HEAD = http.request.raw_head()


HTTP10_OK = http.response.raw_ok(
    version='HTTP/1.0',
    data=DATA)
HTTP10_OK_CLOSE = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'connection': 'close'},
    data=DATA)
HTTP10_OK_KEEPALIVE = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'connection': 'Keep-Alive'},
    data=DATA)
HTTP10_OK_LENGTH = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': len(DATA)},
    data=DATA)
HTTP10_OK_LENGTH_CLOSE = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': len(DATA), 'connection': 'close'},
    data=DATA)
HTTP10_OK_LENGTH_KEEPALIVE = http.response.raw_ok(
    version='HTTP/1.0',
    headers={'content-length': len(DATA), 'connection': 'Keep-Alive'},
    data=DATA)

HTTP11_OK = http.response.raw_ok(
    version='HTTP/1.1',
    data=DATA)
HTTP11_OK_CLOSE = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'connection': 'close'},
    data=DATA)
HTTP11_OK_KEEPALIVE = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'connection': 'Keep-Alive'},
    data=DATA)
HTTP11_OK_LENGTH = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'content-length': len(DATA)},
    data=DATA)
HTTP11_OK_CHUNKED = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'transfer-encoding': 'chunked'},
    data=CHUNKED_DATA)
HTTP11_OK_LENGTH_CLOSE = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'content-length': len(DATA), 'connection': 'close'},
    data=DATA)
HTTP11_OK_LENGTH_KEEPALIVE = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'content-length': len(DATA), 'connection': 'Keep-Alive'},
    data=DATA)
HTTP11_NO_CONTENT = http.response.raw_no_content()
HTTP11_NOT_MODIFIED = http.response.raw_not_modified()
HTTP11_OK_NO_DATA = http.response.raw_ok()
HTTP11_OK_LENGTH_NO_DATA = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'content-length': len(DATA)})
HTTP11_OK_CHUNKED_NO_DATA = http.response.raw_ok(
    version='HTTP/1.1',
    headers={'transfer-encoding': 'chunked'})


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP10_GET, HTTP11_OK_LENGTH_KEEPALIVE),
        (HTTP10_GET_CLOSE, HTTP11_OK_LENGTH_KEEPALIVE),
        (HTTP10_GET_KEEPALIVE, HTTP11_OK_LENGTH_KEEPALIVE),
        (HTTP11_GET, HTTP11_OK_LENGTH_KEEPALIVE),
        (HTTP11_GET_CLOSE, HTTP11_OK_LENGTH_KEEPALIVE),
        (HTTP11_GET_KEEPALIVE, HTTP11_OK_LENGTH_KEEPALIVE),

        (HTTP11_GET, HTTP10_OK_LENGTH_KEEPALIVE),
        (HTTP11_GET, HTTP11_OK_LENGTH),
        (HTTP11_GET, HTTP11_OK_LENGTH_KEEPALIVE),

        (HTTP10_GET, HTTP11_OK_LENGTH_KEEPALIVE),

        # SEPE-7275
        # SEPE-7312
        (HTTP11_GET, HTTP11_NO_CONTENT),
        (HTTP11_GET, HTTP11_NOT_MODIFIED),
        (HTTP11_HEAD, HTTP11_OK_NO_DATA),
        (HTTP11_HEAD, HTTP11_OK_LENGTH_NO_DATA),
        (HTTP11_HEAD, HTTP11_OK_CHUNKED_NO_DATA),
    ],
    ids=[
        'client_http10',
        'client_http10_close',
        'client_http10_keepalive',
        'client_http11',
        'client_http11_close',
        'client_http11_keepalive',

        'backend_http10_length_keepalive',
        'backend_http11_length',
        'backend_http11_length_keepalive',

        'client_http10,backend_http11_length_keepalive',

        '204_response',
        '304_response',
        'head_request',
        'head_request_length_response',
        'head_request_chunked_response',
    ],
)
@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_backend_keepalive(rfc_ctx, req, backend_resp, connection_manager_required):
    """
    Проверка случаев, когда балансер должен поддерживать соединение с бэкендом
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer(keepalive_count=1, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)
    rfc_ctx.perform_request(req)
    rfc_ctx.perform_request(req)
    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert not sess.is_closed()
    backend_req = rfc_ctx.backend.state.get_raw_request()
    assert req.request_line.version == backend_req.request_line.version
    if req.request_line.version == 'HTTP/1.0':
        asserts.header_value(backend_req, 'connection', 'Keep-Alive')
    else:
        asserts.no_header(backend_req, 'connection')
    log = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.accesslog)
    assert 'ETIMEDOUT' not in log


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP11_GET_KEEPALIVE, HTTP10_OK),
        (HTTP11_GET_KEEPALIVE, HTTP10_OK_CLOSE),
        (HTTP11_GET_KEEPALIVE, HTTP10_OK_KEEPALIVE),
        (HTTP11_GET_KEEPALIVE, HTTP11_OK_CLOSE),
        (HTTP11_GET_KEEPALIVE, HTTP11_OK_KEEPALIVE),

        (HTTP10_GET, HTTP11_OK),
        (HTTP10_GET_CLOSE, HTTP11_OK),
        (HTTP10_GET_KEEPALIVE, HTTP11_OK),
        (HTTP11_GET, HTTP11_OK),
        (HTTP11_GET_CLOSE, HTTP11_OK),

        (HTTP11_GET_KEEPALIVE, HTTP11_OK),
    ],
    ids=[
        'backend_http10',
        'backend_http10_close',
        'backend_http10_keepalive',
        'backend_http11_close',
        'backend_http11_keepalive',

        'client_http10',
        'client_http10_close',
        'client_http10_keepalive',
        'client_http11',
        'client_http11_close',

        'client_http11_keepalive,backend_http11',
    ],
)
@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_backend_nokeepalive_wait_backend_close(rfc_ctx, req, backend_resp, connection_manager_required):
    """
    Проверка случаев, когда баланер должен дожидаться, когда бэкенд закроет соединение
    """
    rfc_ctx.start_rfc_backend(DelayedCloseConfig(response=backend_resp, response_delay=0.5))
    rfc_ctx.start_rfc_balancer(keepalive_count=1, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)
    rfc_ctx.perform_request(req)
    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert sess.finished_by_server()


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP10_GET, HTTP11_OK_LENGTH),
        (HTTP10_GET_CLOSE, HTTP11_OK_LENGTH),
        (HTTP10_GET_KEEPALIVE, HTTP11_OK_LENGTH),

        (HTTP11_GET_KEEPALIVE, HTTP10_OK_LENGTH_CLOSE),
        (HTTP11_GET_KEEPALIVE, HTTP11_OK_LENGTH_CLOSE),
    ],
    ids=[
        'client_http10',
        'client_http10_close',
        'client_http10_keepalive',

        'backend_http10_length_close',
        'backend_http11_length_close',
    ],
)
@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_backend_nokeepalive_close_backend_conn(rfc_ctx, req, backend_resp, connection_manager_required):
    """
    Проверка случаев, когда баланер должен закрыть соединение с бэкендом
    """
    rfc_ctx.start_rfc_backend(DelayedCloseConfig(response=backend_resp, response_delay=0.5))
    rfc_ctx.start_rfc_balancer(keepalive_count=1, backend_timeout=20, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)
    rfc_ctx.perform_request(req)
    for run in Multirun(sum_delay=10):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert sess.finished_by_client()


def send_multiple_requests(rfc_ctx, n):
    streams = [rfc_ctx.create_http_connection().create_stream() for _ in range(n)]
    for stream in streams:
        stream.write_request(HTTP11_GET)
    return [stream.read_response() for stream in streams]


# def _test_request_while_timeout(rfc_ctx):
#    """
#    SEPE-4160
#    если есть долгий запрос, то для второго запроса мы заводим новый коннекшен не смотря на то, что один уже есть
#    @TODO: должны ли мы закрывать коннкшен, у нас же один "закрывается"?
#    """
#    rfc_ctx.start_rfc_balancer('alive_to_backend')
#
#    rfc_ctx.start_rfc_backend('first_timeout', options=['--timeout', '9'])
#
#    response = rfc_ctx.create_http_connection(timeout=6).request_close(HTTP1_1_CLOSE)
#
#    asserts.content(response, 'REPLY')
#
# первый запрос еще обрабатывается, посылаем второй запрос
#    response = rfc_ctx.create_http_connection(timeout=6).request_close(HTTP1_1_CLOSE)
#
#    asserts.content(response, 'NO TIMEOUT')


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_request_close_other(rfc_ctx, connection_manager_required):
    """
    Проверяем, что балансер закрывает все коннекшены, которые превышают кол-во, указанное в keepalive_count
    """
    rfc_ctx.start_rfc_backend(SimpleConfig())
    rfc_ctx.start_rfc_balancer(keepalive_count=1, backend_timeout=20, connection_manager_required=connection_manager_required)

    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)
    send_multiple_requests(rfc_ctx, 3)
    for run in Multirun(sum_delay=10):
        with run:
            tcpdump.read_all()
            # в конфиге стоит keepalive_count = 1, значит кол-во закрытых коннекшенов должно быть на 1 меньше, чем открытых
            assert len(tcpdump.get_sessions()) == 3
            assert len(tcpdump.get_closed_sessions()) == 2


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_keepalive_count(rfc_ctx, connection_manager_required):
    """
    SEPE-5711
    Если keepalive_count = N и одновременно задается не более N запросов,
    то все они идут по уже существующим connection-ам (то есть новые не создаются)
    """
    n = 5
    rfc_ctx.start_rfc_backend(SimpleConfig())
    rfc_ctx.start_rfc_balancer(keepalive_count=n, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)

    send_multiple_requests(rfc_ctx, n)
    time.sleep(0.5)
    send_multiple_requests(rfc_ctx, n)

    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            assert len(tcpdump.get_sessions()) == n


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_keepalive_count_more_reqs(rfc_ctx, connection_manager_required):
    """
    SEPE-7561
    Если keepalive_count = 1, бэкэнд отвечает по 1.0 и закрывает
    соединение, то второй и последующие запросы должны завершаться успешно
    """
    n = 1
    rfc_ctx.start_rfc_backend(CloseConfig(response=HTTP10_OK_LENGTH))
    rfc_ctx.start_rfc_balancer(keepalive_count=n, connection_manager_required=connection_manager_required)
    for _ in xrange(1 + 2 * n):
        response = rfc_ctx.perform_request(HTTP11_GET)
        asserts.status(response, 200)


class BrokenHTTPHandler(ThreeModeHandler):

    def handle_second(self, raw_request, stream):
        stream.write('error')


class BrokenHTTPConfig(ThreeModeConfig):
    HANDLER_TYPE = BrokenHTTPHandler


class RSTHandler(ThreeModeHandler):

    def handle_second(self, raw_request, stream):
        self.sock.send_rst()


class RSTConfig(ThreeModeConfig):
    HANDLER_TYPE = RSTHandler


@pytest.mark.parametrize(
    ['backend_config', 'timeout'],
    [
        (
            ThreeModeChunkedDelayConfig(
                prefix=1, first=10, second=0, chunk_timeout=3,
                response=http.response.ok(data=[DATA[:5], DATA[5:]])
            ),
            3
        ),
        (BrokenHTTPConfig(prefix=1, first=10, second=0, response=HTTP11_OK_LENGTH_KEEPALIVE), 0),
        (RSTConfig(prefix=1, first=10, second=0, response=HTTP11_OK_LENGTH_KEEPALIVE), 0),
    ],
    ids=[
        'connection_timeout',
        'broken_http',
        'connection_reset',
    ]
)
@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_broken_backend(rfc_ctx, backend_config, timeout, connection_manager_required):
    """
    SEPE-5711
    Если при общении с бэкендом возникает ошибка,
    то балансер должен закрыть соединение и больше не отправлять по нему запросы
    """
    rfc_ctx.start_rfc_backend(backend_config)
    rfc_ctx.start_rfc_balancer(keepalive_count=1, backend_timeout=2, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)

    rfc_ctx.perform_request_xfail(HTTP11_GET)
    time.sleep(timeout)
    response = rfc_ctx.perform_request(HTTP11_GET)

    asserts.content(response, DATA)
    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 2, 'Only one connection has been enabled'
            assert sessions[0].is_closed(), 'Connection hasn\'t been closed'


def base_broken_client_test(rfc_ctx, data, connection_manager_required):
    timeout = 2

    rfc_ctx.start_rfc_backend(ThreeModeChunkedDelayConfig(prefix=1, first=10, second=0, chunk_timeout=timeout,
                              response=http.response.ok(data=data)))
    rfc_ctx.start_rfc_balancer(keepalive_count=2, backend_timeout=15, connection_manager_required=connection_manager_required)
    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)

    rfc_ctx.perform_request_xfail(HTTP11_GET, timeout=timeout / 2)

    time.sleep(timeout)
    response = rfc_ctx.perform_request(HTTP11_GET)
    time.sleep(3 * timeout)

    asserts.content(response, ''.join(data))
    return tcpdump


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_broken_client_keepalive(rfc_ctx, connection_manager_required):
    """
    SEPE-5711
    keepalive_count > 0
    Если в процессе посылки данных от backend-а к клиенту последний отрубается,
    и балансер после этого отсылает клиенту только один пакет,
    то балансер должен дождаться ответа backend-а и не закрывать соединение
    """
    tcpdump = base_broken_client_test(rfc_ctx, ['a' * 10], connection_manager_required)
    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1, 'More than one connection has been enabled'
            assert not sessions[0].is_closed(), 'Connection has been finished'


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_broken_client_nokeepalive(rfc_ctx, connection_manager_required):
    """
    SEPE-5711
    SEPE-5793
    keepalive_count > 0
    Если в процессе посылки данных от backend-а к клиенту последний отрубается,
    и балансеру после этого надо отослать клиенту больше одного пакета,
    то балансер должен дождаться ответа backend-а, закрыть соединение,
    и написать в лог об ошибке клиента
    """
    tcpdump = base_broken_client_test(rfc_ctx, ['a' * 10] * 6, connection_manager_required)

    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 2, 'Only one connection has been enabled'
            assert sessions[0].is_closed(), 'Broken connection hasn\'t been closed'
            assert not sessions[1].is_closed(), 'Valid connection has been closed'

    for run in Multirun():
        with run:
            log = rfc_ctx.manager.fs.read_file(rfc_ctx.balancer.config.accesslog)
            assert ('client write system_error EIO' in log) or ('client write system_error EPIPE' in log)


@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_reopen_connection(rfc_ctx, connection_manager_required):
    """
    SEPE-7871
    Балансер отправляет backend-у запрос и получает ответ по keepalive соединению,
    после этого backend закрывает соединение
    При следующем запросе балансер должен проверить соединение и переоткрыть его
    """
    rfc_ctx.start_rfc_backend(CloseConfig(response=HTTP11_OK_LENGTH_KEEPALIVE))
    rfc_ctx.start_rfc_balancer(keepalive_count=1, connection_manager_required=connection_manager_required)

    tcpdump = rfc_ctx.manager.tcpdump.start(rfc_ctx.backend.server_config.port)
    rfc_ctx.perform_request(HTTP11_GET)
    time.sleep(1)
    response = rfc_ctx.perform_request(HTTP11_GET)
    asserts.status(response, 200)
    for run in Multirun(sum_delay=5):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 2


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (HTTP10_GET_KEEPALIVE,  HTTP10_OK),  # SEPE-3957
        (HTTP11_GET,            HTTP10_OK),
        (HTTP11_GET_KEEPALIVE,  HTTP10_OK),
        (HTTP10_POST_KEEPALIVE, HTTP10_OK),
        (HTTP11_POST,           HTTP10_OK),
        (HTTP10_GET_KEEPALIVE,  HTTP11_OK_CHUNKED),  # TODO(ilezhankin): SEPE-3957 is broken in this scenario
        (HTTP11_GET,            HTTP11_OK_CHUNKED),
        (HTTP11_GET_KEEPALIVE,  HTTP11_OK_CHUNKED),
        (HTTP10_POST_KEEPALIVE, HTTP11_OK_CHUNKED),
        (HTTP11_POST,           HTTP11_OK_CHUNKED),
    ],
    ids=[
        'http10_keepalive,backend_http11_length',
        'http11,backend_http11_length',
        'http11_keepalive,backend_http11_length',
        'http10_post_keepalive,backend_http11_length',
        'http11_post,backend_http11_length',
        'http10_keepalive,backend_http11_chunked',
        'http11,backend_http11_chunked',
        'http11_keepalive,backend_http11_chunked',
        'http10_post_keepalive,backend_http11_chunked',
        'http11_post,backend_http11_chunked',
    ]
)
def test_keepalive_client(rfc_ctx, req, backend_resp):
    """
    Проверка случаев, когда балансер должен поддерживать keepalive с клиентом
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=backend_resp))
    rfc_ctx.start_rfc_balancer(keepalive=True)

    with rfc_ctx.create_http_connection() as conn:
        response = conn.perform_request(req)
        if 'content-length' in backend_resp.headers:
            asserts.is_not_closed(conn.sock)

    asserts.content(response, DATA)


@pytest.mark.parametrize(
    ['req', 'keepalive'],
    [
        (HTTP10_GET, True),
        (HTTP10_GET_CLOSE, True),
        (HTTP11_GET_CLOSE, True),

        (HTTP10_GET, False),
        (HTTP10_GET_CLOSE, False),
        (HTTP10_GET_KEEPALIVE, False),
        (HTTP11_GET, False),
        (HTTP11_GET_CLOSE, False),
        (HTTP11_GET_KEEPALIVE, False),
    ],
    ids=[
        'http10',
        'http10_close',
        'http11_close',

        'http10,nokeepalive',
        'http10_close,nokeepalive',
        'http10_keepalive,nokeepalive',
        'http11,nokeepalive',
        'http11_close,nokeepalive',
        'http11_keepalive,nokeepalive',
    ]
)
def test_nokeepalive_client(rfc_ctx, req, keepalive):
    """
    Проверка случаев, когда балансер должен закрывать соединение с клиентом
    """
    rfc_ctx.start_rfc_backend(SimpleConfig(response=HTTP10_OK))
    rfc_ctx.start_rfc_balancer(keepalive=keepalive)

    with rfc_ctx.create_http_connection() as conn:
        response = conn.perform_request(req)
        asserts.is_closed(conn.sock)

    asserts.content(response, DATA)
