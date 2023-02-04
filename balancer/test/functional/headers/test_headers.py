# -*- coding: utf-8 -*-
"""
SEPE-3930
"""
import time

import pytest

import re
import datetime
import platform
import socket
from email.utils import parsedate

from configs import HeadersConfig, HeadersNamesakeConfig, HeadersProtoSchemeConfig,\
    HeadersOpenSSLSimpleClientConfig, HeadersCreateMultipleConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http, http2
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.stream.ssl.stream import SSLClientOptions


P0F_HEADER = 'X-P0f'
P0F_FUNC = 'p0f'


def build_datetime(microseconds):
    return datetime.datetime.fromtimestamp(microseconds / 1000000.0)


def test_delete_headers(ctx):
    """
    Проверка удаления заголовков
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_delete=1, delete_regexp='for-delete.*'))
    ctx.perform_request(http.request.get(headers={
        'For-delete-1': '1',
        'For-delete-2': '2',
        'For-delete': '3',
        'For-Delete': '',
        'for-Delete': '4',
        'X-Forwarded-For-Y': '321.333.4.3',
        'Pink': 'Floyd',
        'Led': 'Zeppelin',
    }))

    req = ctx.backend.state.get_request()
    asserts.no_header(req, 'for-delete')
    asserts.no_header(req, 'for-delete-1')
    asserts.no_header(req, 'for-delete-2')


def test_create_headers(ctx):
    """
    Проверка создания заголовков
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'x-uid', 'yandex.ru')


def test_create_existing_headers(ctx):
    """
    Если запрос содержит заголовки, указанные в create,
    то балансер должен удалить все эти заголовки и добавить заголовок с указанным значением
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create=1, multiple_hosts_enabled=True))
    ctx.perform_request(http.request.get(headers=[
        ('X-UID', 'yandex.ua'),
        ('X-UID', 'yandex.com.tr'),
    ]))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, 'x-uid')
    asserts.header_value(req, 'x-uid', 'yandex.ru')


def test_create_multiple_headers(ctx):
    """
    Проверка добавления нескольких заголовков
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_multiple=1, multiple_hosts_enabled=True))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'host', 'yandex.ru')
    asserts.header_value(req, 'port', '8765')


def assert_realip(req, header):
    assert req.headers.get_one(header) in ['127.0.0.1', '::1']


def assert_realport(req, header, port):
    asserts.header_value(req, header, str(port))


def assert_url(req, header, url):
    asserts.header_value(req, header, url)


def test_create_func_realip(ctx):
    """
    Проверка добавления заголовка с IP-адресом клиента
    """
    header = 'X-Source-IP-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='realip'))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    assert_realip(req, header)


def test_create_func_realport(ctx):
    """
    Проверка добавления заголовка с портом клиента
    """
    header = 'X-Source-Port-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='realport'))
    conn = ctx.create_http_connection()
    conn.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    assert_realport(req, header, conn.sock.sock_port)


def test_create_func_localport(ctx):
    """
    Проверка добавления заголовка с портом балансера
    """
    header = 'X-Source-Port-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='localport'))
    conn = ctx.create_http_connection()
    conn.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, str(ctx.balancer.config.port))
    assert_realport(req, header, ctx.balancer.config.port)


def test_create_func_localip(ctx):
    """
    Проверка добавления заголовка с IP-адресом балансера
    """
    header = 'X-Dest-IP-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='localip'))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    assert_realip(req, header)  # it is localhost anyway


def test_create_func_url(ctx):
    """
    Проверка добавления заголовка с URL-ом запроса
    """
    url = '/yandsearch?text=test'
    header = 'X-Req-URL'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='url'))
    ctx.perform_request(http.request.get(path=url))

    req = ctx.backend.state.get_request()
    assert_url(req, header, url)


def test_create_func_location(ctx):
    """
    Проверка добавления заголовка с host + url запроса
    """
    url = '/yandsearch?text=test'
    host = 'localhost:8081'
    header = 'Location'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='location'))
    ctx.perform_request(http.request.get(path=url, headers={'Host': host}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, host + url)


def test_create_func_no_host_location(ctx):
    """
    Если в запросе нет заголовка Host,
    то балансер должен добавить заголовок Location с значением undefined (BALANCER-389)
    """
    url = 'yandsearch?text=test'
    header = 'Location'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='location'))
    ctx.perform_request(http.request.get(path=url))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, 'undefined')


def test_create_func_host(ctx):
    """
    BALANCER-389
    Проверка добавления заголовка с host запроса
    """
    url = '/yandsearch?text=test'
    host = 'localhost:8081'
    header = 'X-Host-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='host'))
    ctx.perform_request(http.request.get(path=url, headers={'Host': host}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, '%s' % host)


def test_create_func_no_host_host(ctx):
    """
    BALANCER-389
    Если в запросе нет заголовка Host,
    то балансер должен добавить заголовок X-Host-Y с значением undefined
    """
    url = '/yandsearch?text=test'
    header = 'X-Host-Y'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='host'))
    ctx.perform_request(http.request.get(path=url))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header, 'undefined')


def test_create_func_starttime(ctx):
    """
    Проверка добавления заголовка со временем начала обработки запроса
    """
    header = 'X-Start-Time'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='starttime'))
    conn = ctx.create_http_connection()
    start_time = datetime.datetime.now()
    conn.perform_request(http.request.get())
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    req_time = build_datetime(int(req.headers.get_one(header)))
    assert start_time <= req_time <= fin_time


def test_create_func_reqid(ctx):
    """
    Проверка добавления заголовка с уникальным идентификатором запроса
    """
    header = 'X-Req-ID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='reqid'))
    start_time = datetime.datetime.now()
    ctx.perform_request(http.request.get())
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    reqid = req.headers.get_one(header)
    match = re.match(r'(\d+)-(\d+)', reqid)
    assert match is not None, 'invalid reqid: %s' % reqid
    req_time = build_datetime(int(match.groups()[0]))
    assert start_time < req_time < fin_time


def test_create_func_yuid(ctx):
    """
    Проверка добавления заголовка с уникальным идентификатором, удовлетворяющим требованиям к yuid
    (см. https://wiki.yandex-team.ru/Cookies/yandexuid)
    """
    header = 'X-Random-YUID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='yuid'))
    start_time = datetime.datetime.now().replace(microsecond=0)
    ctx.perform_request(http.request.get())
    fin_time = datetime.datetime.now().replace(microsecond=0)

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    yuid = req.headers.get_one(header)
    match = re.match(r'(\d{9})(\d{10})', yuid)
    assert match is not None, 'invalid yuid: %s' % yuid
    req_time = datetime.datetime.fromtimestamp(int(match.groups()[1]))
    assert start_time <= req_time <= fin_time


@pytest.mark.parametrize('create_mod_name', ['create_func', 'create_func_weak'])
def test_create_func_multiple(ctx, create_mod_name):
    """
    If there are multiple headers in create_func (create_func_weak)
    then all of them should be added to request
    """
    realip_header = 'X-Source-IP-Y'
    realport_header = 'X-Source-Port-Y'
    url_header = 'X-Req-URL'
    url = '/?led=zeppelin'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersCreateMultipleConfig(
        create_mod_name=create_mod_name,
        realip_header=realip_header,
        realport_header=realport_header,
        url_header=url_header,
    ))
    conn = ctx.create_http_connection()
    conn.perform_request(http.request.get(path=url))

    req = ctx.backend.state.get_request()
    assert_realip(req, realip_header)
    assert_realport(req, realport_header, conn.sock.sock_port)
    assert_url(req, url_header, url)


def test_create_weak(ctx):
    """
    Если в запросе нет заголовка, указанного в create_weak, то балансер должен его добавить
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_weak=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'x-uid', 'yandex.ru')


def test_create_weak_existing_headers(ctx):
    """
    Если запрос содержит заголовки, указанные в create_weak,
    то балансер должен оставить эти заголовки и не добавлять заголовок с указанным значением
    Так же проверяем case insensitivity заголовков.
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_weak=1, multiple_hosts_enabled=True))
    ctx.perform_request(http.request.get(headers=[
        ('X-UID', 'yandex.ua'),
        ('X-UID', 'yandex.com.tr'),
    ]))

    req = ctx.backend.state.get_request()
    asserts.header_values(req, 'x-uid', ['yandex.ua', 'yandex.com.tr'])
    asserts.no_header_value(req, 'x-uid', 'yandex.ru')


def test_create_func_weak(ctx):
    """
    Если в запросе нет заголовка, указанного в create_func_weak, то балансер должен его добавить
    """
    header = 'X-Req-ID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func_weak=1, header=header, func='reqid'))
    start_time = datetime.datetime.now()
    ctx.perform_request(http.request.get())
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    reqid = req.headers.get_one(header)
    match = re.match(r'(\d+)-(\d+)', reqid)
    assert match is not None, 'invalid reqid: %s' % reqid
    req_time = build_datetime(int(match.groups()[0]))
    assert start_time < req_time < fin_time


def test_create_func_weak_existing_headers(ctx):
    """
    Если запрос содержит заголовки, указанные в create_func_weak,
    то балансер должен оставить эти заголовки и не добавлять заголовок с указанным значением
    """
    header = 'X-Req-ID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func_weak=1, header=header, func='reqid'))
    ctx.perform_request(http.request.get(headers={'X-Req-ID': 'id'}))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, header)
    asserts.header_value(req, header, 'id')


def test_headers_namesake(ctx):
    """
    SEPE-8062
    Правильность логики для одноименных заголовков в модуле headers
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersNamesakeConfig())
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_values(req, 'namesake', ['strong', 'weak'])


def test_headers_namesake_delete(ctx):
    """
    SEPE-8062
    Правильность логики для одноименных заголовков в модуле headers
    """
    ctx.start_backend(SimpleConfig())

    ctx.start_balancer(HeadersNamesakeConfig())

    ctx.perform_request(http.request.get(headers={'Namesake': 'original'}))

    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'namesake', 'strong')
    asserts.no_header_value(req, "namesake", "original")
    asserts.no_header_value(req, "namesake", "weak")


def test_proto_func(ctx):
    """
    BALANCER-322
    Функция proto - устанавливает протокол уровня приложений
    в качестве значения заголовка
    """
    ctx.start_backend(SimpleConfig())

    ctx.start_balancer(HeadersProtoSchemeConfig(cert_dir=ctx.certs.root_dir))

    # http
    conn = ctx.create_http_connection()
    resp = conn.perform_request(http.request.get())
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    for msg in (resp, req):
        asserts.header_value(msg, 'Proto', 'http')
        asserts.header_value(msg, 'Scheme', 'http')
        assert_header_matches_pattern(
            msg, 'tcpinfo', '^v=2; rtt=\\d+\\.\\d+s; rttvar=\\d+\\.\\d+s; snd_cwnd=\\d+; total_retrans=\\d+$')

    # ssl + http
    with ctx.manager.connection.http.create_ssl(port=ctx.balancer.config.http2_port) as conn:
        resp = conn.perform_request(http.request.get())
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    for msg in (resp, req):
        asserts.header_value(msg, 'Proto', 'http')
        asserts.header_value(msg, 'Scheme', 'https')

    # ssl + http2
    h2_conn = ctx.manager.connection.http2.create_ssl(
        port=ctx.balancer.config.http2_port,
        ssl_options=SSLClientOptions(
            alpn='h2',
            key=ctx.certs.abs_path('client.key'),
            cert=ctx.certs.abs_path('client.crt'),
            ca_file=ctx.certs.root_ca,
            quiet=False
        )
    )

    h2_conn.write_preface()
    time.sleep(1)

    start_time = datetime.datetime.now()
    resp = h2_conn.perform_request(http2.request.get())
    fin_time = datetime.datetime.now()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)

    for msg in (resp, req):
        asserts.header_value(msg, 'proto', 'http2')
        asserts.header_value(msg, 'scheme', 'https')
        assert_header_matches_pattern(
            msg, 'tcpinfo', '^v=2; rtt=\\d+\\.\\d+s; rttvar=\\d+[.]\\d+s; snd_cwnd=\\d+; total_retrans=\\d+$')
        # asserts.header_value(msg, 'cn', 'client.yandex.ru')
        # asserts.header_value(msg, 'subj', SUBJ)
        # asserts.header_value(msg, 'serial', SERIAL)
        # asserts.header_value(msg, 'verify', 'undefined')
        assert_header_matches_pattern(msg, 'handshake', '^handshake-time=[^,]+, no-tls-tickets, handshake-ts=[0-9]+, cipher-id=[0-9]+, protocol-id=[0-9]+$')
        assert_realip(req, 'realip')
        # assert_realport(req, 'realport', h2_conn)
        assert_realip(req, 'localip')
        req_time = build_datetime(int(req.headers.get_one('starttime')))
        assert start_time < req_time < fin_time


@pytest.mark.parametrize('header_name', [
    '   ',
    '',
    '\n',
    'header:value',
], ids=[
    'spaces',
    'empty',
    'newline',
    'header:value',
])
def test_bad_header_does_not_start(ctx, header_name):
    """
    BALANCER-115
    Не взлетаем с заведомо плохим названием заголовка
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header_name, func='realip'))


def start_ssl_balancer(ctx, **kwargs):
    ctx.start_balancer(HeadersOpenSSLSimpleClientConfig(ctx.certs.root_dir, **kwargs))
    for run in Multirun():
        with run:
            ssllog = ctx.manager.fs.read_file(ctx.balancer.config.log)
            assert ssllog


def create_https_connection(ctx, port=None, **ssl_options):
    if port is None:
        port = ctx.balancer.config.port
    if 'quiet' not in ssl_options:
        ssl_options['quiet'] = False
    if 'ca_file' not in ssl_options:
        ssl_options['ca_file'] = ctx.certs.root_ca
    return ctx.manager.connection.http.create_ssl(
        port,
        SSLClientOptions(**ssl_options)
    )


SUBJ = '/C=RU/ST=Russian Federation/O=Yandex/OU=balancer/CN=client.yandex.ru/emailAddress=balancer-dev@yandex-team.ru'
SERIAL = "05"


def test_ssl_client_cert_info(ctx):
    """
    BALANCER-953
    BALANCER-1656
    Adding CN, subject and serial number from client's cert
    """
    ctx.start_backend(SimpleConfig())
    start_ssl_balancer(ctx)

    conn = create_https_connection(
        ctx,
        key=ctx.certs.abs_path('client.key'),
        cert=ctx.certs.abs_path('client.crt'),
    )
    conn.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'cn', 'client.yandex.ru')
    asserts.header_value(req, 'subj', SUBJ)
    asserts.header_value(req, 'serial', SERIAL)


def test_ssl_client_cert_info_undefined(ctx):
    """
    BALANCER-953
    If client doesn't have a certificate,
    balancer should add headers for CN and subject with 'undefined' value
    """
    ctx.start_backend(SimpleConfig())
    start_ssl_balancer(
        ctx,
        verify_peer=False,
        fail_if_no_peer_cert=False,
    )

    conn = create_https_connection(ctx)
    conn.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'cn', 'undefined')
    asserts.header_value(req, 'subj', 'undefined')
    asserts.header_value(req, 'serial', 'undefined')


def test_ssl_client_ja3_enabled(ctx):
    """
    BALANCER-1887
    Test TLS ja3 fingerprinting
    """
    ctx.start_backend(SimpleConfig())
    start_ssl_balancer(
        ctx,
        ja3_enabled=True,
    )

    conn = create_https_connection(
        ctx,
        key=ctx.certs.abs_path('client.key'),
        cert=ctx.certs.abs_path('client.crt'),
    )
    conn.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'ja3', (
        '771,4866-4867-4865-49196-49200-159-52393-52392-52394-49195-49199-158-'
        '49188-49192-107-49187-49191-103-49162-49172-57-49161-49171-51-157-'
        '156-61-60-53-47-255,11-10-35-5-22-23-13-43-45-51,29-23-30-25-24,0-1-2'
    ))


def test_ssl_client_ja3_empty_extensions(ctx):
    """
    BALANCER-1887
    Test TLS ja3 fingerprinting
    """
    ctx.start_backend(SimpleConfig())
    start_ssl_balancer(
        ctx,
        ja3_enabled=True,
    )

    sock = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
    sock.settimeout(1)
    sock.connect(("localhost", ctx.balancer.config.port))

    # client hello with 0 extensions size
    data = "\x16\x03\x01\x00\xe5\x01\x00\x00\xe1\x03\x03\x8e\x75\x48\x80\x46" \
        "\xd4\x2d\xf4\xf6\x3a\xe2\x89\x02\x47\x4d\xd9\xdf\x55\x2d\xc4\x99" \
        "\x7d\x8d\x54\xfb\xc4\x25\xbe\xf7\x2b\xe6\xee\x00\x00\x54\xcc\xa9" \
        "\xcc\xa8\xcc\xaa\xc0\x30\xc0\x2c\xc0\x28\xc0\x24\xc0\x14\xc0\x0a" \
        "\x00\x9f\x00\x6b\x00\x39\xff\x85\x00\xc4\x00\x88\x00\x81\x00\x9d" \
        "\x00\x3d\x00\x35\x00\xc0\x00\x84\xc0\x2f\xc0\x2b\xc0\x27\xc0\x23" \
        "\xc0\x13\xc0\x09\x00\x9e\x00\x67\x00\x33\x00\xbe\x00\x45\x00\x9c" \
        "\x00\x3c\x00\x2f\x00\xba\x00\x41\xc0\x12\xc0\x08\x00\x16\x00\x0a" \
        "\x00\xff\x01\x00\x00\x00\x00\x00\x00\x1c\x00\x1a\x00\x00\x17\x68" \
        "\x74\x74\x70\x32\x2e\x70\x72\x69\x65\x6d\x6b\x61\x2e\x79\x61\x6e" \
        "\x64\x65\x78\x2e\x72\x75\x00\x0b\x00\x02\x01\x00\x00\x0a\x00\x08" \
        "\x00\x06\x00\x1d\x00\x17\x00\x18\x00\x0d\x00\x1c\x00\x1a\x06\x01" \
        "\x06\x03\xef\xef\x05\x01\x05\x03\x04\x01\x04\x03\xee\xee\xed\xed" \
        "\x03\x01\x03\x03\x02\x01\x02\x03\x00\x10\x00\x0e\x00\x0c\x02\x68" \
        "\x32\x08\x68\x74\x74\x70\x2f\x31\x2e\x31"

    sock.sendall(data)

    ssl_type = sock.recv(1)
    assert ssl_type == '\x15', "Read type is not Handshake"


def test_ssl_client_ja3_disabled(ctx):
    """
    BALANCER-1887
    Test TLS ja3 fingerprinting
    """
    header = 'X-Ja3'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='ja3'))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-Ja3', '0,,,,')


def assert_header_matches_pattern(msg, name, pattern):
    assert name in msg.headers, 'Header "%s" not found' % name
    values = msg.headers[name]
    assert len(values) == 1, 'Multiple values detected for header "%s"' % name
    value = values[0]
    assert re.compile(pattern).match(value), 'Header "%s" has value "%s", which doesn\'t match pattern "%s"' % (name, value, pattern)


def test_ssl_handshake_info(ctx):
    """
    BALANCER-973
    Передаём время SSL-хэндшейка в бэкэнды
    """
    ctx.start_backend(SimpleConfig())

    start_ssl_balancer(
        ctx,
        verify_peer=False,
        fail_if_no_peer_cert=False,
        ticket=ctx.certs.abs_path('default_ticket.0.pem')

    )

    with create_https_connection(ctx) as conn1:
        conn1.perform_request(http.request.get())
        req1 = ctx.backend.state.get_request()
        assert_header_matches_pattern(req1, 'handshake', '^handshake-time=[^,]+, no-tls-tickets, handshake-ts=[0-9]+, cipher-id=[0-9]+, protocol-id=[0-9]+$')
        sess = conn1.sock.ssl_options.sess_out

    with create_https_connection(ctx, sess_in=sess) as conn2:
        conn2.perform_request(http.request.get())
        req2 = ctx.backend.state.get_request()
        assert_header_matches_pattern(req2, 'handshake', '^handshake-time=[^,]+, tls-tickets, handshake-ts=[0-9]+, cipher-id=[0-9]+, protocol-id=[0-9]+$')


def ticket_request_processor(ctx, header):
    sess = None
    with create_https_connection(ctx, sess_in=sess) as conn:
        conn.perform_request(http.request.get())
        req = ctx.backend.state.get_request()
        asserts.header(req, header)
        sess = conn.sock.ssl_options.sess_out
        ticket_name = req.headers.get_one(header)
        assert ticket_name == ""

    with create_https_connection(ctx, sess_in=sess) as conn:
        conn.perform_request(http.request.get())
        req = ctx.backend.state.get_request()
        asserts.header(req, header)
        return {"header": req.headers.get_one(header), "data": conn.sock.handshake_info.ticket_data}


def ticket_data_getter(data, idx):
    data = data.split('\n')
    return ''.join([i for i in data[idx][7:54].replace('-', ' ').split()]).upper()


def test_ssl_ticket_name(ctx):
    """
    BALANCER-3063
    Проверяем идентификатор ключа
    """
    header = "ticket-name"

    ctx.start_backend(SimpleConfig())

    start_ssl_balancer(
        ctx,
        verify_peer=False,
        fail_if_no_peer_cert=False,
        ticket=ctx.certs.abs_path('default_ticket.0.pem'),
    )

    ticket_data = ticket_request_processor(ctx, header)
    assert ticket_data["header"] == ticket_data_getter(ticket_data["data"], 0)


def test_ssl_ticket_iv(ctx):
    """
    BALANCER-3063
    Проверяем IV ключа
    """
    header = "ticket-iv"

    ctx.start_backend(SimpleConfig())

    start_ssl_balancer(
        ctx,
        verify_peer=False,
        fail_if_no_peer_cert=False,
        ticket=ctx.certs.abs_path('default_ticket.0.pem'),
    )

    ticket_data = ticket_request_processor(ctx, header)
    assert ticket_data["header"] == ticket_data_getter(ticket_data["data"], 1)


def test_create_func_market_reqid(ctx):
    """
    BALANCER-1638
    Проверяем маркетный reqid
    """
    header = 'X-Req-ID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='market_reqid'))
    start_time = datetime.datetime.now()
    time.sleep(0.01)
    ctx.perform_request(http.request.get())
    time.sleep(0.01)
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    reqid = req.headers.get_one(header)
    match = re.match(r'(\d+)/([0-9a-f]{32})', reqid)
    assert match is not None, 'invalid reqid: %s' % reqid
    req_time = build_datetime(int(match.groups()[0]) * 1000)
    assert start_time < req_time < fin_time


def test_append_headers(ctx):
    """
    BALANCER-1638
    Проверка добавления значения к заголовку
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1))
    ctx.perform_request(http.request.get(headers={'X-UID': 'yandex.ru'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-UID', 'yandex.ru, yandex.com')


def test_append_delimiter_headers(ctx):
    """
    Проверка добавления значения к заголовку с нестандартным разделителем
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1, delimiter=";"))
    ctx.perform_request(http.request.get(headers={'X-UID': 'yandex.ru'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-UID', 'yandex.ru;yandex.com')


def test_append_as_create_headers(ctx):
    """
    BALANCER-1638
    Если заголовок отсутствует, то добавление должно вести себя как создание заголовка
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-UID', 'yandex.com')


def test_append_weak_headers(ctx):
    """
    BALANCER-1638
    Для weak в случае наличия заголовка поведение как у create
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append_weak=1))
    ctx.perform_request(http.request.get(headers={'X-UID': 'yandex.ru'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-UID', 'yandex.ru, yandex.eu')


def test_append_weak_empty_headers(ctx):
    """
    BALANCER-1638
    Если заголовка нет, то добавление не происходит
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append_weak=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.no_header(req, 'X-UID')


def test_append_weak_priority_headers(ctx):
    """
    BALANCER-1638
    Append имеет больший приоритет, чем Append Weak
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1, enable_append_weak=1))
    ctx.perform_request(http.request.get(headers={'X-UID': 'yandex.ru'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-UID', 'yandex.ru, yandex.com')


def test_append_create_priority_headers(ctx):
    """
    BALANCER-1638
    Create имеет больший приоритет, чем Append чтобы не делать дополнительных проверок
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create=1, enable_append=1))
    ctx.perform_request(http.request.get(headers={'X-UID': 'yandex.uk'}))

    req = ctx.backend.state.get_request()
    asserts.header_values(req, 'x-uid', ['yandex.ru', 'yandex.com'])


def test_append_func_localip_headers(ctx):
    """
    BALANCER-1638
    Проверим основной разделитель для Append
    """
    header = 'X-Local-Ip'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append_func=1, header=header, func='localip'))
    ctx.perform_request(http.request.get(headers={header: "1.0.0.127"}))

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    localip = req.headers.get_one(header)
    assert localip in ("1.0.0.127, 127.0.0.1", "1.0.0.127, ::1")


def test_append_func_market_reqid_headers(ctx):
    """
    BALANCER-1638
    Основную работу мы уже проверили, теперь проверяем кастомный разделитель для маркета
    """
    header = 'X-Req-ID'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append_func=1, header=header, func='market_reqid'))
    start_time = datetime.datetime.now()
    time.sleep(0.01)
    ctx.perform_request(http.request.get(headers={header: "0"}))
    time.sleep(0.01)
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    reqid = req.headers.get_one(header)
    match = re.match(r'(\d+)/(\d+)/([0-9a-f]{32})', reqid)
    assert match is not None, 'invalid reqid: %s' % reqid
    assert int(match.groups()[0]) == 0
    req_time = build_datetime(int(match.groups()[1]) * 1000)
    assert start_time < req_time < fin_time


def test_copy_headers(ctx):
    """
    BALANCER-1676
    Проверяем копирование заголовков из одного поля в другое
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_copy=1, copy_src_header="x-real-ip", copy_dst_header="X-Forwarded-For"))
    ctx.perform_request(http.request.get(headers={'X-Real-IP': '2'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-Real-IP', '2')
    asserts.header_value(req, 'X-Forwarded-For', '2')

    ctx.perform_request(http.request.get(headers={'X-Real-IP': '1'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-Real-IP', '1')
    asserts.header_value(req, 'X-Forwarded-For', '1')


def test_copy_delete_headers(ctx):
    """
    BALANCER-1676
    Проверяем копирование заголовков из одного поля в другое, если исходное поле удаляется
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_copy=1, copy_src_header="X-REAL-IP", copy_dst_header="X-Forwarded-For", enable_delete=1, delete_regexp='x-real.*'))
    ctx.perform_request(http.request.get(headers={'x-real-IP': '1'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'x-forwarded-for', '1')
    asserts.no_header(req, 'x-real-ip')


def test_copy_weak_headers(ctx):
    """
    BALANCER-1676
    Проверяем weak копирование
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_copy_weak=1, copy_src_header="X-Real-Ip", copy_dst_header="X-Forwarded-For"))
    ctx.perform_request(http.request.get(headers={'X-Real-IP': '2', 'X-Forwarded-For': '1'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'X-Forwarded-For', '1')


def test_copy_modified_headers(ctx):
    """
    BALANCER-1676
    Проверяем при изменении исходного заголовка в скопированном будет старое значение
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1, enable_copy=1, copy_src_header="x-uid", copy_dst_header="X-Forwarded-For"))
    ctx.perform_request(http.request.get(headers={'x-uid': '1'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'x-uid', '1, yandex.com')
    asserts.header_value(req, 'X-Forwarded-For', '1')


def test_copy_delete_create_headers(ctx):
    """
    BALANCER-1676
    Проверяем при удалении и пересоздании исходного заголовка в скопированном будет старое значение
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create=1, enable_delete=1, delete_regexp='x-uid.*', enable_copy=1, copy_src_header="x-uid", copy_dst_header="X-Forwarded-For"))
    ctx.perform_request(http.request.get(headers={'x-uid': '1'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'x-uid', 'yandex.ru')
    asserts.header_value(req, 'x-forwarded-for', '1')


def test_copy_create_headers(ctx):
    """
    BALANCER-1676
    Проверяем что копирование приоритетнее создания заголовка
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create=1, enable_copy=1, copy_src_header="X-Real-Ip", copy_dst_header="x-uid"))
    ctx.perform_request(http.request.get(headers={'X-Real-IP': '2'}))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, 'x-uid')
    asserts.header_value(req, 'x-uid', '2')


def test_copy_create_weak_headers(ctx):
    """
    BALANCER-1676
    Проверяем что копирование затирает weak создание
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_weak=1, enable_copy=1, copy_src_header="X-Real-Ip", copy_dst_header="x-uid"))
    ctx.perform_request(http.request.get(headers={'X-Real-IP': '2'}))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, 'x-uid')
    asserts.header_value(req, 'x-uid', '2')


def test_copy_append_headers(ctx):
    """
    BALANCER-1676
    Проверяем что копирование затирает strong append
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_append=1, enable_copy=1, copy_src_header="X-Real-Ip", copy_dst_header="x-uid"))
    ctx.perform_request(http.request.get(headers={'X-Real-IP': '2'}))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, 'x-uid')
    asserts.header_value(req, 'x-uid', '2')


@pytest.mark.parametrize(
    ['header', 'value'],
    [('header', 'va\nue'), ('hea<der', 'value'), ('hea<der', 'va\nue')]
)
def test_create_invalid_headers(ctx, header, value):
    """
    BALANCER-2031
    """
    ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HeadersConfig(
            enable_create_custom=1,
            header=header,
            value=value,
        ))


@pytest.mark.parametrize('header_value', [
    'time',
    'time:',
    'time:+-1',
    'time:+1v',
], ids=[
    'no_dots',
    'no_value',
    'extra_chars',
    'typo',
])
def test_bad_time_value_does_not_start(ctx, header_value):
    """
    Не взлетаем с заведомо плохим значением time:xx
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HeadersConfig(enable_create_func=1, header='Expires', func=header_value))


def test_create_func_time(ctx):
    """
    Проверяем time функцию
    """
    header = 'Expires'
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header, func='time:+1h'))
    ts = datetime.datetime.utcnow()
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header(req, header)
    import sys
    sys.stderr.write(req.headers.get_one(header) + "\n")
    expires = datetime.datetime.fromtimestamp(time.mktime(parsedate(req.headers.get_one(header))))

    diff = expires - ts
    assert 3590 < diff.total_seconds() < 3610


@pytest.mark.parametrize('func_type', ['weak', 'append', 'append_weak', 'create'])
@pytest.mark.parametrize('enabled', [True, False])
@pytest.mark.parametrize('preexist', [True, False])
def test_explicit_p0f(ctx, func_type, enabled, preexist):
    """
    Проверяем генерацию p0f фингерпринта при явном включении опции -
    если есть использование p0f в заголовках всегда включаем его.
    """
    if platform.system() != 'Linux':
        pytest.skip('Test for linux only')

    ctx.start_backend(SimpleConfig())

    if func_type == 'weak':
        ctx.start_balancer(HeadersConfig(enable_create_func_weak=1, p0f_enabled=enabled, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'append':
        ctx.start_balancer(HeadersConfig(enable_append_func=1, p0f_enabled=enabled, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'append_weak':
        ctx.start_balancer(HeadersConfig(enable_append_func_weak=1, p0f_enabled=enabled, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'create':
        ctx.start_balancer(HeadersConfig(enable_create_func=1, p0f_enabled=enabled, header=P0F_HEADER, func=P0F_FUNC))

    if preexist:
        ctx.perform_request(http.request.get(headers={P0F_HEADER: 'preexist'}))
    else:
        ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    if preexist:
        asserts.header(req, P0F_HEADER)
        p0f_header = req.headers.get_one(P0F_HEADER)
        if func_type != 'weak':
            assert sum(1 for c in p0f_header if c == ':') == 7
        else:
            assert sum(1 for c in p0f_header if c == ':') == 0
    else:
        if func_type != 'append_weak':
            asserts.header(req, P0F_HEADER)
            p0f_header = req.headers.get_one(P0F_HEADER)
            assert sum(1 for c in p0f_header if c == ':') == 7
        else:
            assert P0F_HEADER not in req.headers, 'Header ' + P0F_HEADER + ' is present\n'


@pytest.mark.parametrize('func_type', ['weak', 'append', 'append_weak', 'create'])
@pytest.mark.parametrize('preexist', [True, False])
def test_implicit_p0f(ctx, func_type, preexist):
    """
    Проверяем генерацию p0f фингерпринта при неявном включении опции
    """
    if platform.system() != 'Linux':
        pytest.skip('Test for linux only')

    ctx.start_backend(SimpleConfig())
    if func_type == 'weak':
        ctx.start_balancer(HeadersConfig(enable_create_func_weak=1, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'append':
        ctx.start_balancer(HeadersConfig(enable_append_func=1, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'append_weak':
        ctx.start_balancer(HeadersConfig(enable_append_func_weak=1, header=P0F_HEADER, func=P0F_FUNC))
    elif func_type == 'create':
        ctx.start_balancer(HeadersConfig(enable_create_func=1, header=P0F_HEADER, func=P0F_FUNC))

    if preexist:
        ctx.perform_request(http.request.get(headers={P0F_HEADER: 'preexist'}))
    else:
        ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    if preexist:
        asserts.header(req, P0F_HEADER)
        p0f_header = req.headers.get_one(P0F_HEADER)
        if func_type != 'weak':
            assert sum(1 for c in p0f_header if c == ':') == 7
        else:
            assert sum(1 for c in p0f_header if c == ':') == 0
    else:
        if func_type != 'append_weak':
            asserts.header(req, P0F_HEADER)
            p0f_header = req.headers.get_one(P0F_HEADER)
            assert sum(1 for c in p0f_header if c == ':') == 7
        else:
            assert P0F_HEADER not in req.headers, 'Header ' + P0F_HEADER + ' is present\n'


def test_traceparent(ctx):
    """
    BALANCER-3406
    Проверяем генерацию заголовка traceparent
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(enable_create_func=1, header='traceparent', func='traceparent'))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header(req, 'traceparent')
    traceparent_header = req.headers.get_one('traceparent')
    match = re.match(r'00-([0-9a-f]{32})-([0-9a-f]{16})-01', traceparent_header)
    assert match
    assert(len(match.group(0)) == len(traceparent_header))
    assert(match.group(1) != '0' * 32)
    assert(match.group(2) != '0' * 16)


@pytest.mark.parametrize(
    ['content', 'kind'],
    [
        ('', 'empty'),
        ('{"create":{"X-Custom2":"file"}}', 'append'),
        ('{"override":true,"create":{"X-Custom2":"file"}}', 'override'),
    ],
    ids=[
        'empty',
        'append',
        'override'
])
def test_rules_file(ctx, content, kind):
    """
    BALANCER-3176
    Проверяем задание правил через файл
    """
    rules_file=ctx.manager.fs.create_file('rules_file')
    ctx.manager.fs.rewrite(rules_file, content)
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(HeadersConfig(rules_file=rules_file, enable_create_custom=1, header='X-Custom', value='config'))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    if kind == 'empty':
        asserts.header(req, 'X-Custom')
        asserts.header_value(req, 'X-Custom', 'config')
        asserts.no_header(req, 'X-Custom2')
    elif kind == 'append':
        asserts.header(req, 'X-Custom')
        asserts.header_value(req, 'X-Custom', 'config')
        asserts.header(req, 'X-Custom2')
        asserts.header_value(req, 'X-Custom2', 'file')
    elif kind == 'override':
        asserts.no_header(req, 'X-Custom')
        asserts.header(req, 'X-Custom2')
        asserts.header_value(req, 'X-Custom2', 'file')

    ctx.manager.fs.remove(rules_file)


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
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HeadersConfig(enable_create_func=1, header=header_name, func='realip'))


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
def test_restricted_headers_deletion_does_not_start(ctx, header_name):
    """
    BALANCER-3209: Удаление заголовков Content-Length и Transfer-Encoding запрещено
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HeadersConfig(enable_delete=1, delete_regexp=header_name))


@pytest.mark.parametrize('weak', [True, False])
@pytest.mark.parametrize('preexist', [True, False])
@pytest.mark.parametrize(
    ['content', 'kind'],
    [
        ('', 'empty'),
        ('valid', 'valid'),
        ('i\nvalid', 'invalid'),
        (None, 'missed')
    ],
    ids=[
        'empty',
        'valid',
        'invalid',
        'missed'
])
def test_create_from_file(ctx, weak, preexist, content, kind):
    """
    BALANCER-3263
    Проверяем задание заголовка через файл
    """
    headerfile=ctx.manager.fs.create_file('headerfile')
    if kind != 'missed':
        ctx.manager.fs.rewrite(headerfile, content)
    ctx.start_backend(SimpleConfig())
    enable_create_from_file=None if weak else 1
    enable_create_from_file_weak=1 if weak else None
    ctx.start_balancer(HeadersConfig(
        filename=headerfile,
        enable_create_from_file=enable_create_from_file,
        enable_create_from_file_weak=enable_create_from_file_weak,
        header='X-Custom'
    ))
    if preexist:
        ctx.perform_request(http.request.get(headers={'X-Custom': 'preexist'}))
    else:
        ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    if kind == 'empty' or kind == 'invalid':
        if preexist:
            asserts.header(req, 'X-Custom')
            asserts.header_value(req, 'X-Custom', 'preexist')
        else:
            asserts.no_header(req, 'X-Custom')
    elif kind == 'valid':
        if preexist and weak:
            asserts.header(req, 'X-Custom')
            asserts.header_value(req, 'X-Custom', 'preexist')
        else:
            asserts.header(req, 'X-Custom')
            asserts.header_value(req, 'X-Custom', content)

    if kind != 'missed':
        ctx.manager.fs.remove(headerfile)
