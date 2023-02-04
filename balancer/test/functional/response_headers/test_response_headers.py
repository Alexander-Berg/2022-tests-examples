# -*- coding: utf-8 -*-
"""
SEPE-3995
"""
import pytest
import re
import datetime

from configs import ResponseHeadersConfig, ResponseHeadersNamesakeConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig, CloseConfig
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError


def build_datetime(microseconds):
    return datetime.datetime.fromtimestamp(microseconds / 1000000.0)


def test_delete_headers(ctx):
    """
    Проверка удаления заголовков
    """
    response = http.response.ok(headers=[
        ('For-delete-1', '1'),
        ('For-delete-2', '2'),
        ('For-delete', '3'),
        ('for-Delete', '4'),
        ('Host', 'yandex.ru'),
    ])
    backend = ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port,
                                             enable_delete=True, delete_regexp='for-delete.*'))
    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'for-delete')
    asserts.no_header(resp, 'for-delete-1')
    asserts.no_header(resp, 'for-delete-2')


def test_create_headers(ctx):
    """
    Проверка создания заголовков
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, 'host', 'yandex.ru')


def test_create_existing_headers(ctx):
    """
    Если ответ содержит заголовки, указанные в create,
    то балансер должен удалить все эти заголовки и добавить заголовок с указанным значением
    """
    response = http.response.ok(headers=[
        ('Host', 'yandex.ua'),
        ('Host', 'yandex.com.tr'),
    ])
    backend = ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create=True, multiple_hosts_enabled=True))
    resp = ctx.perform_request(http.request.get())

    asserts.single_header(resp, 'host')
    asserts.header_value(resp, 'host', 'yandex.ru')


def test_create_multiple_headers(ctx):
    """
    Проверка добавления нескольких заголовков
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_multiple=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, 'host', 'yandex.ru')
    asserts.header_value(resp, 'port', '8765')


def test_create_func_realip(ctx):
    """
    Проверка добавления заголовка с IP-адресом клиента
    """
    header = 'X-Source-IP-Y'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='realip'))
    resp = ctx.perform_request(http.request.get())

    assert resp.headers.get_one(header) in ['127.0.0.1', '::1']


def test_create_func_realport(ctx):
    """
    Проверка добавления заголовка с портом клиента
    """
    header = 'X-Source-Port-Y'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='realport'))
    conn = ctx.create_http_connection()
    resp = conn.perform_request(http.request.get())

    assert int(resp.headers.get_one(header)) == conn.sock.sock_port


def test_create_func_localip(ctx):
    """
    Проверка добавления заголовка с IP-адресом балансера
    """
    header = 'X-Dest-IP-Y'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='localip'))
    resp = ctx.perform_request(http.request.get())

    assert resp.headers.get_one(header) in ['127.0.0.1', '::1']


def test_create_func_url(ctx):
    """
    Проверка добавления заголовка с URL-ом запроса
    """
    url = '/yandsearch?text=test'
    header = 'X-Req-URL'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='url'))
    resp = ctx.perform_request(http.request.get(path=url))

    asserts.header_value(resp, header, url)


def test_create_func_location(ctx):
    """
    Проверка добавления заголовка с host + url запроса
    """
    url = 'yandsearch?text=test'
    host = 'localhost:8081'
    header = 'Location'
    request = http.request.get(path=url, headers=[('host', host)])
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='location'))
    resp = ctx.perform_request(request)

    asserts.header_value(resp, header, '%s/%s' % (host, url))


def test_create_func_no_host_location(ctx):
    """
    Если в запросе нет заголовка Host,
    то балансер должен добавить заголовок Location с значением undefined (BALANCER-389)
    """
    url = 'yandsearch?text=test'
    header = 'Location'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='location'))
    resp = ctx.perform_request(http.request.get(path=url))

    asserts.header_value(resp, header, 'undefined')


def test_create_func_host(ctx):
    """
    BALANCER-389
    Проверка добавления заголовка с host запроса
    """
    url = 'yandsearch?text=test'
    host = 'localhost:8081'
    header = 'X-Host-Y'
    request = http.request.get(path=url, headers=[('host', host)])
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='host'))
    resp = ctx.perform_request(request)

    asserts.header_value(resp, header, '%s' % host)


def test_create_func_no_host_host(ctx):
    """
    BALANCER-389
    Если в запросе нет заголовка Host,
    то балансер должен добавить заголовок X-Host-Y с значением undefined
    """
    url = 'yandsearch?text=test'
    header = 'X-Host-Y'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='host'))
    resp = ctx.perform_request(http.request.get(path=url))

    asserts.header_value(resp, header, 'undefined')


def test_create_func_starttime(ctx):
    """
    Проверка добавления заголовка со временем начала обработки запроса
    """
    header = 'X-Start-Time'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='starttime'))
    with ctx.create_http_connection() as conn:
        start_time = datetime.datetime.now()
        resp = conn.perform_request(http.request.get())
        fin_time = datetime.datetime.now()

    req_time = build_datetime(int(resp.headers.get_one(header)))
    assert start_time < req_time < fin_time


def test_create_func_reqid(ctx):
    """
    Проверка добавления заголовка с уникальным идентификатором запроса
    """
    header = 'X-Req-ID'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='reqid'))
    with ctx.create_http_connection() as conn:
        start_time = datetime.datetime.now()
        resp = conn.perform_request(http.request.get())
        fin_time = datetime.datetime.now()

    asserts.header(resp, header)
    reqid = resp.headers.get_one(header)
    m = re.match(r'(\d+)-(\d+)', reqid)
    assert m is not None, 'invalid reqid: %s' % reqid
    req_time = build_datetime(int(m.groups()[0]))
    assert start_time < req_time < fin_time


def test_create_func_yuid(ctx):
    """
    Проверка добавления заголовка с уникальным идентификатором, удовлетворяющим требованиям к yuid
    (см. https://wiki.yandex-team.ru/Cookies/yandexuid)
    """
    header = 'X-Random-YUID'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True,
                                             header=header, func='yuid'))
    with ctx.create_http_connection() as conn:
        start_time = datetime.datetime.now().replace(microsecond=0)
        resp = conn.perform_request(http.request.get())
        fin_time = datetime.datetime.now().replace(microsecond=0)

    asserts.header(resp, header)
    yuid = resp.headers.get_one(header)
    m = re.match(r'(\d{9})(\d{10})', yuid)
    assert m is not None, 'invalid yuid: %s' % yuid
    req_time = datetime.datetime.fromtimestamp(int(m.groups()[1]))
    assert start_time <= req_time <= fin_time


def test_create_weak(ctx):
    """
    Если в ответе нет заголовка, указанного в create_weak, то балансер должен его добавить
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_weak=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, 'host', 'yandex.ru')


def test_create_weak_existing_headers(ctx):
    """
    Если ответ содержит заголовки, указанные в create_weak,
    то балансер должен оставить эти заголовки и не добавлять заголовок с указанным значением
    """
    response = http.response.ok(headers=[
        ('Host', 'yandex.ua'),
        ('Host', 'yandex.com.tr')
    ])
    backend = ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_weak=True, multiple_hosts_enabled=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_values(resp, 'host', ['yandex.ua', 'yandex.com.tr'])
    asserts.no_header_value(resp, 'host', 'yandex.ru')


def test_create_func_weak(ctx):
    """
    Если в ответе нет заголовка, указанного в create_func_weak, то балансер должен его добавить
    """
    header = 'X-Req-ID'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func_weak=True,
                                             header=header, func='reqid'))
    with ctx.create_http_connection() as conn:
        start_time = datetime.datetime.now()
        resp = conn.perform_request(http.request.get())
        fin_time = datetime.datetime.now()

    asserts.header(resp, header)
    reqid = resp.headers.get_one(header)
    m = re.match(r'(\d+)-(\d+)', reqid)
    assert m is not None, 'invalid reqid: %s' % reqid
    req_time = build_datetime(int(m.groups()[0]))
    assert start_time < req_time < fin_time


def test_create_func_weak_existing_headers(ctx):
    """
    Если ответ содержит заголовки, указанные в create_func_weak,
    то балансер должен оставить эти заголовки и не добавлять заголовок с указанным значением
    """
    header = 'X-Req-ID'
    response = http.response.ok(headers=[(header, 'id')])
    backend = ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func_weak=True,
                                             header=header, func='reqid', multiple_hosts_enabled=True))
    resp = ctx.perform_request(http.request.get())

    asserts.single_header(resp, header)
    asserts.header_value(resp, header, 'id')


def test_response_headers_namesake(ctx):
    """
    SEPE-8062
    Правильность логики для одноименных заголовков в модуле response_headers
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersNamesakeConfig(backend.server_config.port))

    resp = ctx.perform_request(http.request.get())

    asserts.header_values(resp, 'header', ['strong', 'weak'])


def test_response_headers_namesake_delete(ctx):
    """
    SEPE-8062
    Правильность логики для одноименных заголовков в модуле response_headers
    """
    response = http.response.ok(headers=[('Header', 'Query')])
    backend = ctx.start_backend(SimpleConfig(response=response))

    ctx.start_balancer(ResponseHeadersNamesakeConfig(backend.server_config.port))

    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, 'header', 'strong')
    asserts.no_header_value(resp, "header", "query")
    asserts.no_header_value(resp, "header", "weak")


def test_response_headers_http10(ctx):
    """
    Клиент задает запрос  по HTTP/1.1
    Backend отвечает по HTTP/1.0  и при этом не указывает Content-Length
    (то есть предлагает балансеру дождаться закрытия соединения);
    Балансер отдает клиенту ответ с HTTP/1.1 с Transfer-Encoding: chunked.
    """
    msg = 'A' * 50
    response = http.response.raw_ok(version='HTTP/1.0', data=msg)
    backend = ctx.start_backend(CloseConfig(response=response))

    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create=True))
    conn = ctx.create_http_connection()
    resp = conn.perform_request_raw_response(http.request.get())

    asserts.content(resp, msg)
    asserts.is_chunked(resp)


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
    backend = ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_create_func=True, header=header_name, func='realip'))


def test_append_headers(ctx):
    """
    BALANCER-1638
    Проверка добавления значения к заголовку
    """
    header = 'X-UID'
    backend = ctx.start_backend(SimpleConfig(http.response.ok(headers=[('X-UID', 'yandex.ru')])))
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_append=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, header, 'yandex.ru, yandex.com')


def test_append_weak_headers(ctx):
    """
    BALANCER-1638
    Для weak в случае наличия заголовка поведение как у create
    """
    header = 'X-UID'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_append=True))
    resp = ctx.perform_request(http.request.get())

    asserts.header_value(resp, header, 'yandex.com')


def test_append_weak_empty_headers(ctx):
    """
    BALANCER-1638
    Если заголовка нет, то добавление не происходит
    """
    header = 'X-UID'
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, enable_append_weak=True))
    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, header)


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
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port, rules_file=rules_file, enable_create=True))
    resp = ctx.perform_request(http.request.get())

    if kind == 'empty':
        asserts.header(resp, 'Host')
        asserts.header_value(resp, 'Host', 'yandex.ru')
        asserts.no_header(resp, 'X-Custom2')
    elif kind == 'append':
        asserts.header(resp, 'Host')
        asserts.header_value(resp, 'Host', 'yandex.ru')
        asserts.header(resp, 'X-Custom2')
        asserts.header_value(resp, 'X-Custom2', 'file')
    elif kind == 'override':
        asserts.no_header(resp, 'Host')
        asserts.header(resp, 'X-Custom2')
        asserts.header_value(resp, 'X-Custom2', 'file')

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
    backend = ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port,
                                                 enable_create_func=True, header=header_name, func='realip'))


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
    backend = ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersConfig(backend.server_config.port,
                                                 enable_delete=True, delete_regexp=header_name))
