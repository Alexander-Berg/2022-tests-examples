# -*- coding: utf-8 -*-
import os
import time
import pytest
import shutil
import datetime
import tempfile

from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError

from configs import AccesslogConfig, AccesslogHeadersConfig, AccesslogExplicitLogConfig, NestedAccesslogConfig,\
    CacheAccesslogConfig, AccesslogAttemptsConfig, AccesslogAttemptsOnErrorConfig, AccesslogAttemptsOnError2Config,\
    Http2AccesslogConfig, AccesslogPPConfig
from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig, ThreeModeConfig
from balancer.test.util.predef import http, http2
from balancer.test.util.stream.ssl.stream import SSLClientOptions

from unified_agent_mock import InitUnifiedAgentService

IP_HEADER = 'X-Forwarded-For-Y'
PORT_HEADER = 'X-Source-Port-Y'
SOURCE_IP = '8.8.8.8'
SOURCE_PORT = '42'


def base_additional_ip_port_test(ctx, log_ip, log_port, request, expected, cfg_class=AccesslogConfig):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(cfg_class(
        additional_ip_header=log_ip,
        additional_port_header=log_port
    ))
    ctx.perform_request(request)

    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert accesslog, 'accesslog is empty'
            src = accesslog.split('\t')[1]
            assert src == '"{}"'.format(expected)


@pytest.fixture(params=[AccesslogConfig, AccesslogHeadersConfig], ids=['accesslog', 'accesslog_headers'])
def cfg_class(request):
    return request.param


@pytest.mark.parametrize(
    ['log_ip', 'log_port', 'ip_header', 'port_header', 'expected'],
    [
        (IP_HEADER, PORT_HEADER, IP_HEADER, PORT_HEADER, SOURCE_IP + ':' + SOURCE_PORT),
        (IP_HEADER, PORT_HEADER, IP_HEADER, None, SOURCE_IP + ':'),
        (IP_HEADER, PORT_HEADER, None, PORT_HEADER, ':' + SOURCE_PORT),
        (IP_HEADER, PORT_HEADER, None, None, ':'),
        (IP_HEADER, None, IP_HEADER, None, SOURCE_IP),
        (IP_HEADER, None, None, None, ''),
    ],
    ids=[
        'log_ip_port',
        'log_ip_port,no_port_header',
        'log_ip_port,no_ip_header',
        'log_ip_port,no_ip_port_headers',
        'log_ip',
        'log_ip,no_ip_header',
    ]
)
def test_additional_ip_port(ctx, log_ip, log_port, ip_header, port_header, expected, cfg_class):
    """
    BALANCER-523
    BALANCER-1286
    Если указаны параметры additional_ip_header, additional_port_header,
    то балансер должен писать в лог значения указанных заголовков
    """
    headers = dict()
    if ip_header:
        headers[ip_header] = SOURCE_IP
    if port_header:
        headers[port_header] = SOURCE_PORT

    base_additional_ip_port_test(ctx, log_ip, log_port, http.request.get(headers=headers),
                                 expected, cfg_class=cfg_class)


def test_additional_ip_port_invalid_src(ctx, cfg_class):
    """
    BALANCER-523
    BALANCER-1286
    Если указаны параметры additional_ip_header, additional_port_header,
    то балансер должен писать в лог значения указанных заголовков,
    даже если они не соответствуют формату.
    """
    ip = 'Led'
    port = 'Zeppelin'
    base_additional_ip_port_test(ctx, IP_HEADER, PORT_HEADER,
                                 http.request.get(headers={IP_HEADER: ip, PORT_HEADER: port}),
                                 ip + ':' + port, cfg_class=cfg_class)


def test_additional_ip_port_multiple_headers(ctx, cfg_class):
    """
    BALANCER-523
    BALANCER-1286
    Если запрос содержит несколько заголовков additional_ip_header и additional_port_header,
    то балансер должен использовать значения первых из них.
    """
    headers = [
        (IP_HEADER, '8.8.8.8'),
        (IP_HEADER, '9.9.9.9'),
        (IP_HEADER, '10.10.10.10'),
        (PORT_HEADER, '42'),
        (PORT_HEADER, '43'),
        (PORT_HEADER, '44'),
        (PORT_HEADER, '45'),
    ]
    base_additional_ip_port_test(ctx, IP_HEADER, PORT_HEADER, http.request.get(headers=headers),
                                 '10.10.10.10:45', cfg_class=cfg_class)


def test_no_additional_ip_port(ctx):
    """
    BALANCER-523
    BALANCER-1286
    Если параметры additional_ip_header и additional_port_header не указаны,
    то в логе ip:port входящего соединения и timestamp должны быть разделены одним символом табуляции
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig(
        additional_ip_header=None,
        additional_port_header=None
    ))
    ctx.perform_request(http.request.get(headers={IP_HEADER: '8.8.8.8', PORT_HEADER: '42'}))

    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert accesslog, 'accesslog is empty'
            timestamp = accesslog.split('\t')[1][:25]
            datetime.datetime.strptime(timestamp, '%Y-%m-%dT%H:%M:%S.%f')


def test_additional_port_no_additional_ip(ctx):
    """
    BALANCER-523
    BALANCER-1286
    Если в конфиге указан additional_port_header, но не указан additional_ip_header,
    то балансер не должен запуститься
    """
    ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(AccesslogConfig(
            additional_ip_header=None,
            additional_port_header=PORT_HEADER
        ))


def test_log_file_mode_000(ctx):
    """
    SEPE-4140
    Балансер должен падать при старте, если у него нет прав на запись в лог-файл
    """
    accesslog = ctx.manager.fs.create_file('accesslog')
    os.chmod(accesslog, 0)

    ctx.start_backend(SimpleConfig())
    # TODO: check return code
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(AccesslogExplicitLogConfig(accesslog))


def test_long_request_line(ctx):
    """
    SEPE-4712
    Условия: размер стартовой строки больше 64Kb
    Поведение: балансер должен полностью записать стартовую строку в лог
    """
    length = 100500
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig(maxreq=length * 2, maxlen=length * 2))

    path = '/' + 'A' * length

    response = ctx.perform_request(http.request.get(path=path))
    asserts.status(response, 200)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert path in log, 'Request not found in accesslog'
            assert 'succ 200' in log, 'Status not found in accesslog'


def test_nested_accesslog(ctx):
    """
    BALANCER-67
    При наличии вложенных модулей accesslog'ов писать во все логи, а не только в самый
    вложенный.
    """
    led_path = "/led/"
    zep_path = "/zeppelin/"

    ctx.start_balancer(NestedAccesslogConfig())
    ctx.perform_request(http.request.get(path=led_path))
    ctx.perform_request(http.request.get(path=zep_path))

    for run in Multirun():
        with run:
            common_log = ctx.manager.fs.read_file(ctx.balancer.config.common_log)
            led_log = ctx.manager.fs.read_file(ctx.balancer.config.led_log)
            zep_log = ctx.manager.fs.read_file(ctx.balancer.config.zeppelin_log)

            assert led_path in common_log
            assert zep_path in common_log
            assert led_path in led_log
            assert zep_path in zep_log


def test_accesslog_cache_hit(ctx):
    """
    SEPE-4622
    Если ответ берется из кеша, то в accesslog-е должна появиться запись [cache hit]
    """
    key = 'test'
    path = '/' + key
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CacheAccesslogConfig())

    ctx.perform_request(http.request.get(path=path))
    time.sleep(1)  # Wait until balancer stores response in cache
    ctx.perform_request(http.request.get(path=path))

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[cache_server get %s//GET hit valid]' % key in log


def test_accesslog_fail_first(ctx):
    """
    SEPE-3925
    Если backend не отвечает на запрос с первой попытки, но отвечает со второй,
    то в accesslog должна быть информация об обеих попытках
    """
    ctx.start_backend(ThreeModeConfig(prefix=1, first=1, second=0))
    ctx.start_balancer(AccesslogAttemptsConfig())

    ctx.perform_request(http.request.get())

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'http_parse_error 400' in log
            assert 'succ 200' in log


def test_accesslog_balancer_on_error(ctx):
    """
    BALANCER-963
    accesslog должен отражать информацию о секции on_error в модуле balancer
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(AccesslogAttemptsOnErrorConfig())
    ctx.perform_request(http.request.get())

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            print log
            assert '[on_error [errordocument ' in log


def test_accesslog_balancer2_on_error(ctx):
    """
    BALANCER-963
    accesslog должен отражать информацию о секции on_error в модуле balancer2
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(AccesslogAttemptsOnError2Config())
    ctx.perform_request(http.request.get())

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[on_error [errordocument ' in log


def test_accesslog_http2(ctx):
    """
    BALANCER-1191
    http2 запросы должны иметь вид GET / HTTP/2
    """
    accesslog = ctx.manager.fs.create_file('accesslog')
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Http2AccesslogConfig(accesslog=accesslog, certs_dir=ctx.certs.root_dir))

    h2_conn = ctx.manager.connection.http2.create_ssl(port=ctx.balancer.config.port, ssl_options=SSLClientOptions(alpn='h2'))
    h2_conn.write_preface()
    resp = h2_conn.perform_request(http2.request.get())
    asserts.status(resp, 200)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'GET / HTTP/2' in log


def test_accesslog_http(ctx):
    """
    BALANCER-1399
    http basic test
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'GET / HTTP/1.1' in log


def test_accesslog_unified_agent_unix(ctx):
    """
    unified agent unix socket test
    """
    tmp_dir = tempfile.mkdtemp(prefix="unified_sock_test")
    unix_socket = os.path.join(tmp_dir, "uf.sock")

    service, thread = InitUnifiedAgentService(unix_socket)
    thread.start()

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig(custom_accesslog="unified_agent:unix://{0}".format(unix_socket)))

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)

    found = False
    for run in Multirun():
        with run:
            for i in service.payload:
                if 'GET / HTTP/1.1' in i:
                    found = True

    thread.join()
    shutil.rmtree(tmp_dir)

    stats = ctx.get_unistat()
    # Value could be 246 bytes in a case when both client
    # and backend ports are two bytes long(e.g. 80, 80)
    assert 245 < stats["unified_agent_uf.sock_ack_summ"] < 253

    if not found:
        raise Exception("No GET request was found in Unified Agent payload")


def test_accesslog_unified_agent_unix_errors(ctx):
    """
    В случае если unified agent логер не может соединиться с основным
    сервисом мы должны это поймать с помощью этого счетчика
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig(custom_accesslog="unified_agent:unix://dev/null"))

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)

    stats = ctx.get_unistat()
    assert stats["unified_agent_null_errors_summ"] > 0


@pytest.mark.parametrize('disabled', [0, 1], ids=["enabled", "disabled"])
def test_accesslog_instance_dir(ctx, disabled):
    """
    MINOTAUR-1290
    log current dir to differentiate services
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AccesslogConfig(log_instance_dir=(disabled != 1)))
    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    d = os.path.basename(os.path.abspath(os.path.curdir))
    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)

            if disabled:
                assert d not in log
            else:
                assert "\t{} ".format(d) in log


@pytest.mark.parametrize('errors_only', [True, False])
def test_accesslog_errors_only(ctx, errors_only):
    ctx.start_balancer(AccesslogPPConfig(errors_only=errors_only))
    resp = ctx.perform_request(http.request.get(path='/200'))
    asserts.status(resp, 200)
    resp = ctx.perform_request(http.request.get(path='/404'))
    asserts.status(resp, 404)
    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'succ 404' in log
            if errors_only:
                assert 'succ 200' not in log
            else:
                assert 'succ 200' in log
