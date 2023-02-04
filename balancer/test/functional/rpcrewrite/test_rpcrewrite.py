# -*- coding: utf-8 -*-
import pytest
import time

from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun

from balancer.test.util.predef.handler.server.http import SimpleConfig, CloseConfig, ChunkedConfig, DummyConfig,\
    PreparseHandler, HTTPConfig
from balancer.test.util.proto.http.stream import parse_request, serialize_request
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http

from configs import RpcRewriteConfig, RpcRewriteOnErrorConfig, RpcRewriteWeightedConfig, RpcRewriteOnErrorWeightedConfig


HOST_HEADER = 'localhost'
RPC_URL = '/proxy/'

CHUNKED_REQUEST = http.request.get(data=['12345', 'abc'], headers={'X-Metabalancer-Y': 'xxx'})
RPC_REWRITE_REQUEST = http.request.get(headers={'X-Metabalancer-Y': 'meta'}, data=['vwxyz', '123'])


def base_rpc_request_test(ctx, request, cfg, dry_run=0, data=RPC_REWRITE_REQUEST):
    if cfg == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=serialize_request(data))), name='rpc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RpcRewriteConfig(dry_run=dry_run))
    response = ctx.perform_request(request)
    asserts.status(response, 200)


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_submodule_request(ctx, config):
    """
    Балансер должен передать ответ rpc-backend-а в подмодуль
    """
    base_rpc_request_test(ctx, CHUNKED_REQUEST, config)
    backend_req = ctx.backend.state.get_request()
    asserts.content(backend_req, RPC_REWRITE_REQUEST.data.content)


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_submodule_request_dry_run(ctx, config):
    """
    При включенном dry_run балансер должен передать запрос клиента в подмодуль
    с заголовком X-Metabalancer-Y из ответа rpc-backend-а
    """
    base_rpc_request_test(ctx, CHUNKED_REQUEST, config, dry_run=1)
    backend_req = ctx.backend.state.get_request()
    asserts.content(backend_req, CHUNKED_REQUEST.data.content)
    asserts.header_value(backend_req, 'X-Metabalancer-Y', 'meta')


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_submodule_request_dry_run_no_meta(ctx, config):
    """
    При включенном dry_run балансер должен передать запрос клиента в подмодуль
    удалив заголовок X-Metabalancer-Y, если его не было в ответе backend-а
    """
    base_rpc_request_test(ctx, CHUNKED_REQUEST, config, dry_run=1, data=http.request.get())
    backend_req = ctx.backend.state.get_request()
    asserts.no_header(backend_req, 'X-Metabalancer-Y')


@pytest.mark.parametrize(
    'req',
    [
        http.request.post(data='0123456789'),
        CHUNKED_REQUEST,
        http.request.get(path='/yandsearch?text=abc&param=value'),
        http.request.head(),
        http.request.raw_get(
            path='/uni.ws', headers=[('Connection', 'Upgrade'), ('Upgrade', 'websockets')]
        ).to_request(),
    ],
    ids=[
        'length',
        'chunked',
        'cgi',
        'head',
        'upgrade',
    ]
)
@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_request_length(ctx, req, config):
    """
    SEPE-7598
    BALANCER-1309
    Клиент задает запрос
    Балансер должен передать запрос в модуль rpc в теле POST-запроса
    """
    base_rpc_request_test(ctx, req, config)
    rpc_req = ctx.rpc.state.get_request()
    parsed_req = parse_request(rpc_req.data.content).to_request()
    assert parsed_req == req


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_request_url(ctx, config):
    """
    Балансер должен завернуть клиентский запрос в тело POST-запроса с указанным url,
    с указанным заголовком Host и content-type: application/octet-stream
    и переслать его в модуль rpc
    """
    base_rpc_request_test(ctx, CHUNKED_REQUEST, config)
    rpc_req = ctx.rpc.state.get_request()
    asserts.path(rpc_req, RPC_URL)
    asserts.header_value(rpc_req, 'host', HOST_HEADER)
    asserts.header_value(rpc_req, 'content-type', 'application/octet-stream')


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_request_keepalive(ctx, config):
    """
    SEPE-7443
    BALANCER-1309
    Балансер должен игнорировать заголовок Connection в теле ответа от модуля rpc
    """
    rpc_data = http.request.raw_get(
        headers={'X-Metabalancer-Y': 'meta', 'Transfer-Encoding': 'chunked', 'Connection': 'close'},
        data=['vwxyz', '123'])
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=serialize_request(rpc_data))), name='rpc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(config(dry_run=0, host=HOST_HEADER, url=RPC_URL))

    tcpdump = ctx.manager.tcpdump.start(ctx.backend.server_config.port)
    conn = ctx.create_http_connection()
    response = conn.perform_request_raw_response(http.request.get())
    backend_req = ctx.backend.state.get_raw_request()

    asserts.status(response, 200)
    asserts.no_header_value(backend_req, 'Connection', 'Close')
    asserts.no_header_value(response, 'Connection', 'Close')
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1, 'More than one connection has been enabled'
            assert not sessions[0].is_closed(), 'Connection has been closed'


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_request_close(ctx, config):
    """
    SEPE-7443
    BALANCER-1309
    Балансер должен игнорировать заголовок Connection в теле ответа от модуля rpc
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=serialize_request(http.request.get()))), name='rpc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(config(dry_run=0, host=HOST_HEADER, url=RPC_URL, keepalive_count=0))

    tcpdump = ctx.manager.tcpdump.start(ctx.backend.server_config.port)
    conn = ctx.create_http_connection()
    response = conn.perform_request_raw_response(http.request.raw_get(headers={'connection': 'close'}))
    backend_req = ctx.backend.state.get_raw_request()

    asserts.status(response, 200)
    asserts.header_value(backend_req, 'Connection', 'Close')
    asserts.header_value(response, 'Connection', 'Close')
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1, 'More than one connection has been enabled'
            assert sessions[0].is_closed(), 'Connection hasn\'t been closed'


def base_bad_rpc_backend_test(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RpcRewriteConfig(dry_run=0, host=HOST_HEADER, url=RPC_URL))
    response = ctx.perform_request(CHUNKED_REQUEST)
    asserts.status(response, 200)
    backend_req = ctx.backend.state.get_request()
    assert backend_req == CHUNKED_REQUEST, 'Request to backend is not equal to client request'


def base_bad_rpc_backend_test_on_error(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(RpcRewriteOnErrorConfig(dry_run=0, host=HOST_HEADER, url=RPC_URL))
    response = ctx.perform_request(CHUNKED_REQUEST)
    asserts.status(response, 404)
    backend_req = ctx.on_error_backend.state.get_request()
    assert backend_req == CHUNKED_REQUEST, 'Request to on_error_backend is not equal to client request'
    assert ctx.backend.state.requests.empty()


@pytest.mark.parametrize(
    'has_on_error',
    [False, True],
    ids=['no_on_error', 'on_error']
)
def test_no_rpc_backend(ctx, has_on_error):
    """
    Если rpc backend не работает, то балансер должен отправить клиентский запрос в подмодуль
    """
    ctx.start_fake_backend(name='rpc')
    if has_on_error:
        base_bad_rpc_backend_test_on_error(ctx)
    else:
        base_bad_rpc_backend_test(ctx)


@pytest.mark.parametrize(
    'has_on_error',
    [False, True],
    ids=['no_on_error', 'on_error']
)
def test_broken_rpc_backend(ctx, has_on_error):
    """
    Если rpc backend обрывает содинение во время ответа,
    то балансер должен отправить клиентский запрос в подмодуль
    """
    ctx.start_backend(CloseConfig(response=http.response.raw_ok(
        headers={'Transfer-Encoding': 'chunked'}, data='5\r\nabcde\r\n')), name='rpc')
    if has_on_error:
        base_bad_rpc_backend_test_on_error(ctx)
    else:
        base_bad_rpc_backend_test(ctx)


@pytest.mark.parametrize(
    'has_on_error',
    [False, True],
    ids=['no_on_error', 'on_error']
)
def test_rpc_backend_timeout(ctx, has_on_error):
    """
    Если rpc backend таймаутится во время ответа,
    то балансер должен отправить клиентский запрос в подмодуль
    """
    ctx.start_backend(ChunkedConfig(response=http.response.ok(data=['A' * 10] * 4), chunk_timeout=3), name='rpc')
    if has_on_error:
        base_bad_rpc_backend_test_on_error(ctx)
    else:
        base_bad_rpc_backend_test(ctx)


@pytest.mark.parametrize(
    'has_on_error',
    [False, True],
    ids=['no_on_error', 'on_error']
)
def test_rpc_backend_no_inner_request(ctx, has_on_error):
    """
    Если в ответе rpc backend нет нового запроса,
    то балансер должен отправить клиентский запрос в подмодуль
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='')), name='rpc')
    if has_on_error:
        base_bad_rpc_backend_test_on_error(ctx)
    else:
        base_bad_rpc_backend_test(ctx)


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_main_backend_timeout(ctx, config):
    """
    SEPE-7629
    BALANCER-1309
    Если при рабочем перезаписывающем бэкэнде основной бэкэнд отваливается по таймауту,
    то балансер не должен повторно отправлять запрос (ни перезаписанный, ни оригинальный)
    основному бэкэнду
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=serialize_request(RPC_REWRITE_REQUEST))), name='rpc')
    ctx.start_backend(ChunkedConfig(response=http.response.ok(data=['A' * 10] * 10), chunk_timeout=1))
    ctx.start_balancer(config(dry_run=0, host=HOST_HEADER, url=RPC_URL, timeout='500ms'))

    ctx.perform_request_xfail(http.request.get(headers={'Led': 'Zeppelin'}))

    assert ctx.backend.state.requests.qsize() == 1, 'Got more than one request!'


@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_file_switch(ctx, config):
    """
    BALANCER-1078
    BALANCER-1309
    При наличии файла из file_switch запросы не идут в rpc секцию
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    file_switch = ctx.manager.fs.create_file('file_switch')
    response = http.response.ok(data=serialize_request(RPC_REWRITE_REQUEST))
    rpc_backend = ctx.start_backend(SimpleConfig(response=response), name='rpc')
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(config(file_switch=file_switch))

    time.sleep(2)

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    assert rpc_backend.state.requests.empty()

    ctx.manager.fs.remove(file_switch)
    time.sleep(2)

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    request = rpc_backend.state.get_request()
    assert request is not None


@pytest.mark.parametrize(
    'config',
    [RpcRewriteWeightedConfig, RpcRewriteOnErrorWeightedConfig],
    ids=['no_on_error', 'on_error']
)
def test_submodule_attempts(ctx, config):
    """
    В конфиге указано два backend-а в подмодуле, attempts = 2
    Если один backend не отвечает, то балансер должен задать запрос в другой
    """
    if config == RpcRewriteOnErrorWeightedConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=serialize_request(RPC_REWRITE_REQUEST))), name='rpc')
    ctx.start_backend(DummyConfig(), name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')
    ctx.start_balancer(config(dry_run=0, host=HOST_HEADER, url=RPC_URL, timeout='500ms'))

    ctx.perform_request_xfail(http.request.get(headers={'Black': 'Sabbath'}))

    assert ctx.backend1.state.requests.qsize() == 1
    assert ctx.backend2.state.requests.qsize() == 1


@pytest.mark.parametrize(
    'data',
    [
        'GET /request HTTP/1.1\r\nHead',
        'GET /request HTTP/1.1\r\nConnection: Close\r\nTransfer-Encoding: chunked\r\n\r\n5\r\n12345\r\n',
        'GET /request HTTP/1.1\r\nConnection: Close\r\nContent-Length: 5\r\n\r\n1234',
    ],
    ids=[
        'headers',
        'chunked_data',
        'length_data',
    ]
)
@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_broken_rpc_response(ctx, data, config):
    """
    SEPE-7833
    BALANCER-592
    BALANCER-1309
    Если в теле ответа rpc backend-а невалдный запрос,
    то балансер должен отправить в submodule клиентский запрос без изменений
    и сделать запись в accesslog
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=data)), name='rpc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(config(dry_run=0))
    response = ctx.perform_request(CHUNKED_REQUEST)
    backend_req = None
    if config == RpcRewriteOnErrorConfig:
        asserts.status(response, 404)
        backend_req = ctx.on_error_backend.state.get_request()
    else:
        asserts.status(response, 200)
        backend_req = ctx.backend.state.get_request()

    assert backend_req == CHUNKED_REQUEST
    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'rpcrewrite' in log
            assert 'http_parse_error 400' in log


class ConnectionAnyHandler(PreparseHandler):
    def handle_parsed_request(self, raw_request, stream):
        stream.write_line('{} 200 OK'.format(self.config.protocol_version))
        stream.write_header('Content-Length', '0')
        # stream.write_header('Content-Length', '0')
        if self.config.connection_header is not None:
            stream.write_header('Connection', self.config.connection_header)
        stream.end_headers()

        if self.config.connection_force_close is not None:
            """
            Check force_close status
            """
            if self.config.connection_force_close:
                self.force_close()

        elif self.config.connection_header is not None:
            """
            Not using force to close connection, let's follow RFC and
            check Connection header
            """
            if self.config.connection_header.lower() == "close":
                self.force_close()

        else:
            """
            Not using force, and header Connection is not defined
            """
            if self.config.protocol_version == "HTTP/1.0":
                self.force_close()


class ConnectionAnyConfig(HTTPConfig):
    HANDLER_TYPE = ConnectionAnyHandler

    def __init__(self, protocol_version, connection_header=None, connection_force_close=None):
        super(ConnectionAnyConfig, self).__init__()
        self.protocol_version = protocol_version
        self.connection_header = connection_header
        self.connection_force_close = connection_force_close


@pytest.mark.parametrize(
    ['protocol', 'closed', 'header', 'force'],
    [
        ('HTTP/1.0', True, None, None),
        ('HTTP/1.0', True, None, 0),
        ('HTTP/1.0', False, 'Keep-Alive', None),
        ('HTTP/1.0', True, 'Close', None),
        ('HTTP/1.0', True, 'Close', 1),
        ('HTTP/1.1', False, None, None),
        ('HTTP/1.1', False, 'Keep-Alive', None),
        ('HTTP/1.1', True, 'Close', None),
        ('HTTP/1.1', True, 'Close', 1),
    ],
    ids=[
        'http10',
        'http10_wrong',
        'http10_keepalive',
        'http10_close',
        'http10_close_wrong',
        'http11',
        'http11_keepalive',
        'http11_close',
        'http11_close_wrong',
    ]
)
@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpc_backend(ctx, protocol, closed, header, force, config):
    """
    Balancer should keep or close connection to rpc backend according to rfc
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    ctx.start_backend(ConnectionAnyConfig(protocol, header, force), name='rpc')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(config(dry_run=0, host=HOST_HEADER, keepalive_count=0, url=RPC_URL))

    tcpdump = ctx.manager.tcpdump.start(ctx.rpc.server_config.port)
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get(headers={'Connection': 'Close'}))
    conn.close()
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1, 'More than one connection has been enabled'
            if closed:
                assert sessions[0].is_closed(), 'Connection hasn\'t been closed'
            else:
                assert not sessions[0].is_closed(), 'Connection has been closed'


@pytest.mark.parametrize('dry_run', [0, 1])
@pytest.mark.parametrize(
    'config',
    [RpcRewriteConfig, RpcRewriteOnErrorConfig],
    ids=['no_on_error', 'on_error']
)
def test_rpcheader_answer(ctx, dry_run, config):
    """
    BALANCER-525
    BALANCER-1309
    If rpc backend answers succesfully, then rpc_success_header: 1 is sent
    to backend.
    """
    if config == RpcRewriteOnErrorConfig:
        ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='on_error_backend')
    rpc_success_header = 'X-Rpc-Answer'
    rpc_request = http.request.get()
    ctx.start_backend(SimpleConfig(http.response.ok(data=serialize_request(rpc_request))), name='rpc')
    ctx.start_backend(SimpleConfig(http.response.ok()))
    ctx.start_balancer(config(rpc_success_header=rpc_success_header, dry_run=dry_run))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    backend_req = ctx.backend.state.get_request()
    asserts.header_value(backend_req, rpc_success_header, '1')


def test_rpcheader_no_answer(ctx):
    """
    BALANCER-525
    BALANCER-1309
    If rpc backend does not answer succesfully, then
    rpc_success_header: 0 is sent to backend.
    """
    rpc_success_header = 'X-Rpc-Answer'
    ctx.start_fake_backend(name='rpc')
    ctx.start_backend(SimpleConfig(http.response.ok()))
    ctx.start_balancer(RpcRewriteConfig(rpc_success_header=rpc_success_header))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    backend_req = ctx.backend.state.get_request()
    asserts.header_value(backend_req, rpc_success_header, '0')


def test_forward_to_user(ctx):
    """
    SEARCH-9733
    Если rpc-бэкенд вернул ответ с заголовком "X-ForwardToUser-Y: 1",
    то пользователю нужно вернуть этот ответ, стерев заголовок
    """
    ctx.start_backend(SimpleConfig(http.response.not_found(headers={'X-ForwardToUser-Y': '1'})), name='rpc')
    ctx.start_backend(SimpleConfig(http.response.ok()))
    ctx.start_balancer(RpcRewriteConfig())

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 404)
    asserts.no_header(response, 'X-ForwardToUser-Y')

    assert ctx.backend.state.requests.qsize() == 0, 'backend has received request, but shouldn\'t'


@pytest.mark.parametrize('header_name', [
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
], ids=[
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
])
def test_restricted_headers_modification_does_not_start(ctx, header_name):
    """
    BALANCER-3209: Изменение заголовков Content-Length и Transfer-Encoding запрещено
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(RpcRewriteConfig(rpc_success_header=header_name))
