# -*- coding: utf-8 -*-
import time
import base64
import pytest

import configs
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts
from balancer.test.util.process import BalancerStartError


def test_file(ctx):
    """
    Проверка, что отдается содержимое указанного файла
    """
    status = 404
    test_html_path = ctx.static.abs_path('test.html')
    test_html = ctx.manager.fs.read_file(test_html_path)
    ctx.start_balancer(configs.ErrordocumentConfig(status, c_file=test_html_path))

    response = ctx.perform_request(http.request.get())

    asserts.status(response, status)
    asserts.reason_phrase(response, 'Not found')
    asserts.content(response, test_html)


@pytest.mark.parametrize('content', ['Hello', ''])
def test_content(ctx, content):
    """
    Проверка, что отдается текст, указанный в переменной content
    """
    status = 200
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, status)
    asserts.content(response, content)


@pytest.mark.parametrize('status', [200, 204, 304])
def test_remain_headers(ctx, status):
    """
    BALANCER-2290
    Проверка, что отдаются заголовки, указанные в remain_headers
    """
    ctx.start_balancer(configs.ErrordocumentConfig(status, remain_headers='aaa|bbb'))
    response = ctx.perform_request(http.request.get(headers={'Aaa': '111', 'Bbb': '222', 'Ccc': '333'}))

    asserts.header_value(response, 'Aaa', '111')
    asserts.header_value(response, 'Bbb', '222')
    asserts.no_header(response, 'Ccc')
    asserts.status(response, status)


@pytest.mark.parametrize('status', [200, 204, 304])
def test_headers(ctx, status):
    """
    BALANCER-2289
    Проверка, что отдаются заголовки, указанные в headers
    """
    ctx.start_balancer(configs.ErrordocumentConfig(status, headers=True))
    response = ctx.perform_request(http.request.get())

    asserts.header_value(response, 'Xxx', '999')
    asserts.header_value(response, 'Zzz', '000')
    asserts.status(response, status)


def test_base64(ctx):
    """
    Отдача статического контента, перекодированного из base64

    errordocument = {
        status = 200;
        base64 = "R0lGODlhAQABAIABAAAAAP///yH5BAEAAAEALAAAAAABAAEAAAICTAEAOw==";
    };
    """
    status = 200
    base64_ = 'R0lGODlhAQABAIABAAAAAP///yH5BAEAAAEALAAAAAABAAEAAAICTAEAOw=='
    ctx.start_balancer(configs.ErrordocumentConfig(status, c_base64=base64_))

    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, base64.b64decode(base64_))


def test_head_request(ctx):
    """
    SEPE-7427
    На HEAD-запрос балансер должен отдавать ответ из errordocument без тела
    и без заголовка content-length
    """
    status = 200
    ctx.start_balancer(configs.ErrordocumentConfig(status, content='Hello'))

    response = ctx.perform_request(http.request.head())

    asserts.status(response, status)
    asserts.empty_content(response)
    asserts.no_header(response, 'content-length')


def test_force_conn_close_off(ctx):
    """
    BALANCER-148
    С выключенной опцией force_conn_close соединение не должно закрываться балансером явно,
    число ошибок не должно расти.
    """
    status = 406
    content = 'GOAWAY\n'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content, force_conn_close=0))

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.is_not_closed(conn.sock)

    stats = ctx.get_unistat()
    asserts.status(response, status)
    assert stats['report-total-fail_summ'] == 0
    assert stats['report-total-succ_summ'] == 1


def test_force_conn_close_on(ctx):
    """
    BALANCER-148
    Со включенной опцией force_conn_close соединение должно закрываться балансером явно,
    число ошибок не должно расти.
    """
    status = 406
    content = 'GOAWAY\n'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content, force_conn_close=1))

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.is_closed(conn.sock)

    stats = ctx.get_unistat()
    asserts.status(response, status)
    assert stats['report-total-fail_summ'] == 0
    assert stats['report-total-succ_summ'] == 1


def test_request_content_chunked(ctx):
    """
    BALANCER-352
    Если клиент задает запрос с телом, то errordocument должен вычитать тело,
    чтобы можно было задавать запросы по этому же соединению
    Случай chunked запроса
    """
    status = 200
    content = 'Hello'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content))

    with ctx.create_http_connection() as conn:
        conn.perform_request(http.request.post(data=['12345', 'abcde']))
        response = conn.perform_request(http.request.get())

    asserts.status(response, status)
    asserts.content(response, content)


def test_request_content_length(ctx):
    """
    BALANCER-352
    Если клиент задает запрос с телом, то errordocument должен вычитать тело,
    чтобы можно было задавать запросы по этому же соединению
    Случай content-length запроса
    """
    status = 200
    content = 'Hello'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content))

    with ctx.create_http_connection() as conn:
        conn.perform_request(http.request.post(data='12345'))
        response = conn.perform_request(http.request.get())

    asserts.status(response, status)
    asserts.content(response, content)


def test_request_fail_counter(ctx):
    """
    BALANCER-2814
    Errordocument should correctly maintain error counters.
    """
    status = 200
    content = 'Hello'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content))

    req = http.request.post(data=['a'] * 2).to_raw_request()
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(req.request_line)
        stream.write_headers(req.headers)
        stream.write_chunk('a')
        time.sleep(1)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0


def test_no_rst_small_socket_buffer(ctx):
    """
    BALANCER-702
    Если размер буфера сокета меньше тела запроса и выставлен флаг force_conn_close,
    то балансер должен корректно закрыть соединение а не слать RST
    """
    status = 200
    content = 'Hello'
    ctx.start_balancer(configs.ErrordocumentConfig(status, content=content, socket_buffer=2048, force_conn_close=1))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)
    resp = ctx.perform_request(http.request.get(data='A' * 1024 * 1024))
    asserts.content(resp, content)
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()

            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert not sess.reset


@pytest.mark.parametrize('status', [204, 304])
@pytest.mark.parametrize(
    ['source', 'data'],
    [
        ('content', 'xxx'),
        ('c_base64', 'eHh4'),
        ('c_file', None)
    ]
)
def test_null_content_file(ctx, status, source, data):
    """
    BALANCER-2228
    Балансер не должен стартовать, если задано тело в виде файла и код ответа такой,
    что по rfc у него не может быть тела.
    """
    test_html_path = ctx.static.abs_path('test.html')
    data = data if source != 'c_file' else test_html_path

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(configs.ErrordocumentConfig(status, **{source: data}))


@pytest.mark.parametrize(
    ['content', 'c_file', 'c_base64'],
    [
        ('xxx', True, None),
        ('xxx', None, 'eHh4'),
        (None, True, 'eHh4')
    ]
)
def test_conflicting_content(ctx, content, c_file, c_base64):
    """
    BALANCER-2228
    Балансер не должен стартовать, если задано тело и код ответа такой,
    что по rfc у него не может быть тела.
    """
    test_html_path = ctx.static.abs_path('test.html')
    c_file = test_html_path if c_file else None

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(configs.ErrordocumentConfig(200, **{
            'content': content,
            'c_file': c_file,
            'c_base64': c_base64
        }))


@pytest.mark.parametrize(
    ['name', 'value'],
    [
        ('', 'empty name'),
        ('foo bar', 'invalid name'),
        ('invalid-value', '\r'),
        ('invalid-value', '\n'),
        ('connection', 'close'),
        ('content-length', '0'),
        ('transfer-encoding', 'chunked'),
    ],
    ids=[
        'empty-name',
        'invalid-name',
        'invalid-value1',
        'invalid-value2',
        'connection',
        'content-length',
        'transfer-encoding',
    ]
)
def test_invalid_header(ctx, name, value):
    """
    BALANCER-2289
    Should not allow invalid headers
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(configs.ErrordocumentConfig(200, **{
            'bad_header_name': name,
            'bad_header_value': value,
        }))
