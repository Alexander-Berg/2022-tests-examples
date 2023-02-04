# -*- coding: utf-8 -*-
import pytest

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.sync import CheckersWatcher


LENGTH_DATA = '0123456789'
CHUNKED_DATA = ['12345', 'abc']


def check_antirobot_request(req, expected_inner_request):
    asserts.method(req, 'POST')
    asserts.path(req, '/fullreq')
    assert req.data.content == expected_inner_request


def base_test_antirobot_inner_request(common_ctx, client_request, expected_inner_request):
    common_ctx.start_all()
    response = common_ctx.perform_request(client_request)
    asserts.status(response, 200)
    for run in Multirun():
        with run:
            req = common_ctx.antirobot_backend.state.get_request()
            check_antirobot_request(req, expected_inner_request)


def test_inner_get(common_ctx):
    """
    SEPE-4671
    SEPE-6000
    SEPE-6210
    BALANCER-122
    BALANCER-474

    GET запрос заворачивается в тело запроса к антироботу,
    во внутреннем запросе нет Content-Length и Transfer-Encoding
    """
    base_test_antirobot_inner_request(
        common_ctx,
        http.request.get(),
        'GET / HTTP/1.1\r\n\r\n')


def test_inner_post_content_length(common_ctx):
    """
    SEPE-4671
    SEPE-6000
    SEPE-6210
    BALANCER-122
    BALANCER-474

    POST запрос заворачивается в тело запроса к антироботу,
    во внутреннем запросе нет Content-Length и Transfer-Encoding
    """
    base_test_antirobot_inner_request(
        common_ctx,
        http.request.post(data='data'),
        'POST / HTTP/1.1\r\n\r\ndata')


def test_inner_post_chunked(common_ctx):
    """
    SEPE-4671
    SEPE-6000
    SEPE-6210
    BALANCER-122
    BALANCER-474

    POST запрос заворачивается в тело запроса к антироботу,
    во внутреннем запросе нет Content-Length и Transfer-Encoding,
    и для вложенного запроса нет чанков - тело внутреннего запроса
    передаётся, как если бы это было content-length тело
    """
    base_test_antirobot_inner_request(
        common_ctx,
        http.request.post(data=['d', 'a', 't', 'a']),
        'POST / HTTP/1.1\r\n\r\ndata')


def perform_checked_timeout_request(common_ctx, conn, full_body_expected):
    req = http.request.post(data='data').to_raw_request()

    stream = conn.create_stream()
    stream.write_request_line(req.request_line)
    stream.write_headers(req.headers)

    if not full_body_expected:
        for run in Multirun():
            with run:
                antirobot_request = common_ctx.antirobot_backend.state.get_request()
                check_antirobot_request(antirobot_request, 'POST / HTTP/1.1\r\n\r\n')

    stream.write_data(req.data)
    resp = stream.read_response()
    asserts.status(resp, 200)

    if full_body_expected:
        for run in Multirun():
            with run:
                antirobot_request = common_ctx.antirobot_backend.state.get_request()
                check_antirobot_request(antirobot_request, 'POST / HTTP/1.1\r\n\r\ndata')


def make_checked_timeout_request(common_ctx, full_body_expected=False):
    with common_ctx.create_http_connection() as conn:
        perform_checked_timeout_request(common_ctx, conn, full_body_expected=full_body_expected)


def test_cut_request(common_ctx):
    """
    BALANCER-474

    При cut_request = true антиробот не ждёт тела запроса.
    Во внутреннем запросе будут лишь строка запроса и заголовки
    """
    common_ctx.start_all(cut_request=True)
    make_checked_timeout_request(common_ctx, full_body_expected=False)


def test_cut_request_keepalive(common_ctx):
    """
    BALANCER-474
    BALANCER-799

    При cut_request = true антиробот не ждёт тела запроса.
    Во внутреннем запросе будут лишь строка запроса и заголовки.
    Keepalive запросы должны отрабатывать корректно
    """
    common_ctx.start_all(cut_request=True)
    with common_ctx.create_http_connection() as conn:
        # first keepalive request
        perform_checked_timeout_request(common_ctx, conn, full_body_expected=False)
        # second keepalive request in a single session
        perform_checked_timeout_request(common_ctx, conn, full_body_expected=False)


def test_no_cut_request_file(common_ctx):
    """
    BALANCER-474
    BALANCER-488

    Если указана опция no_cut_request_file и такого файла не существует,
    то поведение такое же, как если бы cut_request = true
    """
    no_cut_request_file = common_ctx.manager.fs.create_file('no_cut_request_file')
    watcher = CheckersWatcher(common_ctx, no_cut_request_file)
    common_ctx.start_all(cut_request=None, no_cut_request_file=no_cut_request_file)
    watcher.wait_checker(is_exists=True)
    common_ctx.manager.fs.remove(no_cut_request_file)
    watcher.wait_checker(is_exists=False)

    make_checked_timeout_request(common_ctx, full_body_expected=False)


def test_no_cut_request_file_exists(common_ctx):
    """
    BALANCER-474
    BALANCER-488

    Если указана опция no_cut_request_file и такой файл существует
    то поведение такое же, как если бы cut_request = false
    """
    no_cut_request_file = common_ctx.manager.fs.create_file('no_cut_request_file')
    watcher = CheckersWatcher(common_ctx, no_cut_request_file)
    common_ctx.start_all(cut_request=None, no_cut_request_file=no_cut_request_file)
    common_ctx.manager.fs.rewrite(no_cut_request_file, '')
    watcher.wait_checker(is_exists=True)
    make_checked_timeout_request(common_ctx, full_body_expected=True)


def test_no_cut_request_file_state_change(common_ctx):
    """
    BALANCER-474
    BALANCER-488

    Если указана опция no_cut_request_file и такой файл существует
    то поведение такое же, как если бы cut_request = false. И наоборот.
    Проверяем, что этот файл проверяется не только на запуске
    """
    no_cut_request_file = common_ctx.manager.fs.create_file('no_cut_request_file')
    watcher = CheckersWatcher(common_ctx, no_cut_request_file)
    common_ctx.start_all(cut_request=None, no_cut_request_file=no_cut_request_file)

    common_ctx.manager.fs.rewrite(no_cut_request_file, '')
    watcher.wait_checker(is_exists=True)
    make_checked_timeout_request(common_ctx, full_body_expected=True)

    common_ctx.manager.fs.remove(no_cut_request_file)
    watcher.wait_checker(is_exists=False)

    make_checked_timeout_request(common_ctx, full_body_expected=False)


@pytest.mark.parametrize('forward', ['true', 'yes', '1'])
def test_forward_to_user(common_ctx, forward):
    """
    SEPE-4133
    BALANCER-1313
    Если ответ антиробота содержит хедер X-ForwardToUser-Y: {true, yes, 1},
    то клиентский запрос считается роботным
    """
    common_ctx.start_all(antirobot_headers={'X-ForwardToUser-Y': forward})
    common_ctx.do_robot_request(http.request.get())
    unistat = common_ctx.get_unistat()
    assert unistat['report-service_total-fail_summ'] == 0


@pytest.mark.parametrize(
    'forward',
    ['false', 'no', '0', '', 'wiyefgweif'],
    ids=['false', 'no', '0', 'empty', 'random']
)
def test_not_forward_to_user(common_ctx, forward):
    """
    SEPE-4133
    Если в ответе антирбота значение заголовка X-ForwardToUser-Y не true, yes или 1,
    то клиентский запрос не считается роботным
    """
    common_ctx.start_all(antirobot_headers={'X-ForwardToUser-Y': forward})
    common_ctx.do_not_robot_request(http.request.get())


def test_not_forward_to_user_no_headers(common_ctx):
    """
    Если ответ антиробота не содержит заголовок X-ForwardToUser-Y,
    то клиентский запрос не считается роботным
    """
    common_ctx.start_all()
    common_ctx.do_not_robot_request(http.request.get())


def base_test_antirobot_request(common_ctx, request, expected_content):
    response = common_ctx.perform_request(request)
    asserts.status(response, 200)
    for run in Multirun():
        with run:
            antirobot_request = common_ctx.antirobot_backend.state.get_request()
            asserts.content(antirobot_request, expected_content)


def test_get_requests(common_ctx):
    """
    SEPE-6210
    SEPE-8697
    Клиент задает GET запрос
    балансер должен передать backend-у клиентский запрос в теле POST-запроса
    При этом должен сохраниться заголок Connection, если он представлен.
    """
    common_ctx.start_all()
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers={'Connection': 'Keep-Alive'}),
        'GET / HTTP/1.1\r\nConnection: Keep-Alive\r\n\r\n'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers={'Connection': 'Close'}),
        'GET / HTTP/1.1\r\nConnection: Close\r\n\r\n'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(),
        'GET / HTTP/1.1\r\n\r\n'
    )


def test_post_length_requests(common_ctx):
    """
    SEPE-6210
    SEPE-8697
    Клиент задает запрос с хедером Content-Length
    балансер должен передать backend-у клиентский запрос в теле POST-запроса
    При этом должен сохраниться заголок Connection, если он представлен.
    """
    common_ctx.start_all()
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Content-Length', len(LENGTH_DATA)), ('Connection', 'Keep-Alive')],
                             data=LENGTH_DATA),
        'GET / HTTP/1.1\r\nConnection: Keep-Alive\r\n\r\n0123456789'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Content-Length', len(LENGTH_DATA)), ('Connection', 'Close')],
                             data=LENGTH_DATA),
        'GET / HTTP/1.1\r\nConnection: Close\r\n\r\n0123456789'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Content-Length', len(LENGTH_DATA))], data=LENGTH_DATA),
        'GET / HTTP/1.1\r\n\r\n0123456789'
    )


def test_chunked_requests(common_ctx):
    """
    SEPE-4671
    SEPE-6210
    SEPE-8697
    Клиент задает запрос с хедером Transfer-Encoding
    балансер должен передать backend-у клиентский запрос в теле POST-запроса
    При этом должен сохраниться заголок Connection, если он представлен.
    """
    common_ctx.start_all()
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Transfer-Encoding', 'chunked'), ('Connection', 'Keep-Alive')],
                             data=CHUNKED_DATA),
        'GET / HTTP/1.1\r\nConnection: Keep-Alive\r\n\r\n12345abc'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Transfer-Encoding', 'chunked'), ('Connection', 'Close')],
                             data=CHUNKED_DATA),
        'GET / HTTP/1.1\r\nConnection: Close\r\n\r\n12345abc'
    )
    base_test_antirobot_request(
        common_ctx,
        http.request.raw_get(headers=[('Transfer-Encoding', 'chunked')], data=CHUNKED_DATA),
        'GET / HTTP/1.1\r\n\r\n12345abc'
    )


def xheaders_on_antirobot_general(common_ctx, headers):
    common_ctx.start_all(antirobot_headers={'X-ForwardToUser': 'yes'})
    common_ctx.perform_request(http.request.raw_get(headers=[
        (header, 'i-m-hacker') for header in headers
    ]))

    # request from balancer to antirobot
    antirobot_request = common_ctx.antirobot_backend.state.get_request()

    # Check that no x-headers in post-request to antirobot.
    for header in headers:
        asserts.no_header(antirobot_request, header)

    # FIXME: after serializing message to string
    xheaders_request = '''GET / HTTP/1.1\r
{}\r\n'''.format(''.join(header + ': i-m-hacker\r\n'for header in headers))

    # Check that balancer didn't modify original request.
    asserts.content(antirobot_request, xheaders_request)


def test_xheaders_on_antirobot(common_ctx):
    """
    Проверяем, что запрос от пользователя прилетает на антиробот без существенных изменений. В частности,
    что модуль не вырезает заголовки из запроса
    пользователя и не добавляет эти заголовки во внутренний POST-запрос до антиробота.
    """
    xheaders_on_antirobot_general(common_ctx, [
        'X-Yandex-Suspected-Robot',
        'X-Yandex-Internal-Request',
        'X-Antirobot-Robotness-Y',
        'X-Antirobot-Suspiciousness-Y',
        'X-Antirobot-Jws-Info',
    ])


def test_length_antirobot_response(common_ctx):
    """
    Антиробот отвечает с хедером Content-Length
    балансер должен передать клиенту тело ответа без изменений
    """
    common_ctx.start_antirobot_backend(SimpleConfig(http.response.ok(headers={'X-ForwardToUser-Y': 'yes'},
                                                                     data=common_ctx.ANTIROBOT_DATA)))
    common_ctx.start_additional_backends()
    common_ctx.start_antirobot_balancer()
    response = common_ctx.perform_request(http.request.get())
    asserts.content(response, common_ctx.ANTIROBOT_DATA)


def test_chunked_antirobot_response(common_ctx):
    """
    Антиробот отвечает с хедером Transfer-Encoding: chunked
    балансер должен передать клиенту тело ответа без изменений
    """
    common_ctx.start_antirobot_backend(SimpleConfig(http.response.ok(headers={'X-ForwardToUser-Y': 'yes'},
                                                                     data=[common_ctx.ANTIROBOT_DATA])))
    common_ctx.start_additional_backends()
    common_ctx.start_antirobot_balancer()
    response = common_ctx.perform_request(http.request.get())
    asserts.content(response, common_ctx.ANTIROBOT_DATA)
