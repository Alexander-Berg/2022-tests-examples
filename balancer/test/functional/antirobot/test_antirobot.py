# -*- coding: utf-8 -*-
import pytest
import re
import time
import socket

from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef import http, http2
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.proto.http2.framing.stream import NoHTTP2FrameException
from balancer.test.util.sync import CheckersWatcher


def test_antirobot_timeout(ar_ctx):
    """
    SEPE-4122
    Если антиробот не успевает ответить или закрыть соединение за timeout,
    то запрос к антироботу считается сфейлившимся
    (в accesslog появляется информация об ошибке 110)
    """
    ar_ctx.start_antirobot_backend(SimpleDelayedConfig(response_delay=10))
    ar_ctx.start_additional_backends()
    ar_ctx.start_antirobot_balancer(antirobot_backend_timeout=1)

    response = ar_ctx.perform_request(http.request.get())

    asserts.content(response, ar_ctx.BACKEND_DATA)
    for run in Multirun():
        with run:
            accesslog = ar_ctx.manager.fs.read_file(ar_ctx.balancer.config.accesslog)
            result = re.match(r'.*(\[sub_antirobot *\[[^\[\]]* system_error ETIMEDOUT)', accesslog)
            assert result is not None


def test_post_body_timeout(ar_ctx):
    """
    SEPE-6210
    Для antirobot-а указан timeout
    Клиент отправляет POST-запрос и заголовки, через (timeout + 1) секунд отправляет тело запроса
    Балансер должен разорвать соединение с antirobot-ом через timeout а запрос клиента отправить backend-у
    """
    timeout = 1
    delta = timeout * 0.1
    ar_ctx.start_all(antirobot_backend_timeout=timeout)
    tcpdump = ar_ctx.manager.tcpdump.start(ar_ctx.antirobot_backend.server_config.port)

    request = http.request.get(data='0123456789').to_raw_request()
    with ar_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        time.sleep(timeout + 1)
        stream.write_data(request.data)
        response = stream.read_response()
    asserts.status(response, 200)

    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            assert sessions[0].is_closed(), 'Connection to antirobot hasn\'t been closed'
            assert sessions[0].get_seconds_duration() < timeout + delta, \
                'Connection hasn\'t been closed after antirobot timeout'


@pytest.mark.parametrize(
    ['robot', 'internal', 'eu', 'robotness', 'suspiciousness', 'jws_info'],
    [
        (None, None, None, None, None, None),
        (None, "asdfab", None, None, None, None),
        ("0", "yes", None, None, None, None),
        ("no", None, None, None, None, None),
        (None, None, None, "1.0", "1.0", "VALID"),
        (None, "asdfab", None, "1.0", "1.0", "DEFAULT"),
        ("0", "yes", None, "1.0", "0.0", "SUSP"),
        ("no", None, None, "1.0", "0.0", "INVALID"),
        (None, None, "yes", None, None, None),
    ],
    ids=[
        'robot_empty,internal_empty,eu_empty,robotness_empty,suspiciousness_empty,jws_info_empty',
        'robot_empty,internal_random,eu_empty,robotness_empty,suspiciousness_empty,jws_info_empty',
        'robot_0,internal_yes,eu_empty,robotness_empty,suspiciousness_empty,jws_info_empty',
        'robot_no,internal_empty,eu_empty,robotness_empty,suspiciousness_empty,jws_info_empty',
        'robot_empty,internal_empty,eu_empty,robotness_one,suspiciousness_1,jws_info_valid',
        'robot_empty,internal_random,eu_empty,robotness_one,suspiciousness_1,jws_info_default',
        'robot_0,internal_yes,eu_empty,robotness_one,suspiciousness_0,jws_info_susp',
        'robot_no,internal_empty,eu_empty,robotness_one,suspiciousness_0,jws_info_invalid',
        'robot_empty,internal_empty,eu_yes,robotness_empty,suspiciousness_empty,jws_info_empty',
    ]
)
def test_xheaders_forward(ar_ctx, robot, internal, eu, robotness, suspiciousness, jws_info):
    """
    BALANCER-271
    Пробрасывать заголовки
    X-Yandex-Suspected-Robot,
    X-Yandex-Internal-Request,
    X-Yandex-EU-Request,
    X-Antirobot-Robotness-Y,
    X-Antirobot-Suspiciousness-Y,
    X-Antirobot-Jws-Info
    на бекенд
    """
    antirobot_headers = dict()
    if internal is not None:
        antirobot_headers['X-Yandex-Internal-Request'] = internal
    if robot is not None:
        antirobot_headers['X-Yandex-Suspected-Robot'] = robot
    if eu is not None:
        antirobot_headers['X-Yandex-EU-Request'] = eu
    if robotness is not None:
        antirobot_headers['X-Antirobot-Robotness-Y'] = robotness
    if suspiciousness is not None:
        antirobot_headers['X-Antirobot-Suspiciousness-Y'] = suspiciousness
    if jws_info is not None:
        antirobot_headers['X-Antirobot-Jws-Info'] = jws_info

    ar_ctx.start_all(antirobot_headers=antirobot_headers)
    ar_ctx.perform_request(http.request.get(headers={
        'X-Yandex-Internal-Request': 'i-m-hacker',
        'X-Yandex-Suspected-Robot': 'i-m-hacker',
        'X-Yandex-EU-Request': 'i-m-hacker',
        'X-Antirobot-Robotness-Y': 'i-m-hacker',
        'X-Antirobot-Suspiciousness-Y': 'i-m-hacker',
    }))

    req = ar_ctx.backend.state.get_request()

    if internal is None:
        asserts.no_header(req, 'X-Yandex-Internal-Request')
    else:
        asserts.header_value(req, 'X-Yandex-Internal-Request', internal)

    if robot is None:
        asserts.no_header(req, 'X-Yandex-Suspected-Robot')
    else:
        asserts.header_value(req, 'X-Yandex-Suspected-Robot', robot)

    if eu is None:
        asserts.no_header(req, 'X-Yandex-EU-Request')
    else:
        asserts.header_value(req, 'X-Yandex-EU-Request', eu)

    if robotness is None:
        asserts.no_header(req, 'X-Antirobot-Robotness-Y')
    else:
        asserts.header_value(req, 'X-Antirobot-Robotness-Y', robotness)

    if suspiciousness is None:
        asserts.no_header(req, 'X-Antirobot-Suspiciousness-Y')
    else:
        asserts.header_value(req, 'X-Antirobot-Suspiciousness-Y', suspiciousness)

    if jws_info is None:
        asserts.no_header(req, 'X-Antirobot-Jws-Info')
    else:
        asserts.header_value(req, 'X-Antirobot-Jws-Info', jws_info)


def test_xheaders_in_response(ar_ctx):
    """
    BALANCER-3433
    Проверяем, что заголовки антиробота вылетают наружу.
    Раньше они удалялись балансером, позже антиробот стал это
    делать сам и из балансера этот функционал выпилили.
    """
    internal = '0'
    robot = '1'
    robotness = '1.0'
    suspiciousness = '1.0'
    jws_info = 'VALID'

    ar_ctx.start_all(antirobot_headers={
        'X-Yandex-Internal-Request': internal,
        'X-Yandex-Suspected-Robot': robot,
        'X-Antirobot-Robotness-Y': robotness,
        'X-Antirobot-Suspiciousness-Y': suspiciousness,
        'X-Antirobot-Jws-Info': jws_info,
        'X-ForwardToUser-Y': 'yes',
    })

    resp = ar_ctx.perform_request(http.request.get())

    # asserts.header_value(resp, 'X-Yandex-Internal-Request', internal)
    asserts.header_value(resp, 'X-Yandex-Suspected-Robot', robot)
    asserts.header_value(resp, 'X-Antirobot-Robotness-Y', robotness)
    asserts.header_value(resp, 'X-Antirobot-Suspiciousness-Y', suspiciousness)
    asserts.header_value(resp, 'X-Antirobot-Jws-Info', jws_info)


def test_no_xheaders_in_response(ar_ctx):
    """
    Заголовки X-ForwardToUser-Y и X-Antirobot-Ban-Source-Ip удаляются в модуле antirobot.
    """
    ar_ctx.start_all(antirobot_headers={
        'X-ForwardToUser-Y': 'yes',
        'X-Antirobot-Ban-Source-Ip': '1',
    })

    resp = ar_ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'X-ForwardToUser-Y')
    asserts.no_header(resp, 'X-Antirobot-Ban-Source-Ip')


def test_file_switch(ar_ctx):
    """
    BALANCER-868
    С существующим file_switch запрос считается не-роботным и сразу пробрасывается в подмодуль.
    После удаления file_switch запросы в антироботный бэкэнд восстанавливаются.
    """
    file_switch = ar_ctx.manager.fs.create_file('file_switch')
    watcher = CheckersWatcher(ar_ctx, file_switch)
    ar_ctx.start_all(file_switch=file_switch)
    watcher.wait_checker(is_exists=True)

    response = ar_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, ar_ctx.BACKEND_DATA)
    assert ar_ctx.antirobot_backend.state.requests.empty()

    ar_ctx.manager.fs.remove(file_switch)
    watcher.wait_checker(is_exists=False)

    response = ar_ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, ar_ctx.BACKEND_DATA)
    assert not ar_ctx.antirobot_backend.state.requests.empty()


def file_switch_clean_headers_general(ar_ctx, headers):
    file_switch = ar_ctx.manager.fs.create_file('file_switch')
    watcher = CheckersWatcher(ar_ctx, file_switch)
    ar_ctx.start_all(file_switch=file_switch)
    watcher.wait_checker(is_exists=True)

    response = ar_ctx.perform_request(http.request.get(headers=[
        (header, 'qwerty') for header in headers
    ]))

    asserts.status(response, 200)
    asserts.content(response, ar_ctx.BACKEND_DATA)
    assert ar_ctx.antirobot_backend.state.requests.empty()

    request = ar_ctx.backend.state.get_request()
    for header in headers:
        asserts.no_header(request, header)


def test_file_switch_clean_headers(ar_ctx):
    """
    BALANCER-868
    С существующим file_switch запрос уходит в подмодуль, при этом антиробото-специфичные заголовки
    удаляются из запроса.
    """
    file_switch_clean_headers_general(ar_ctx, [
        'X-Yandex-Internal-Request',
        'X-Yandex-Suspected-Robot',
        'X-Antirobot-Robotness-Y',
        'X-Antirobot-Suspiciousness-Y',
        'X-Antirobot-Jws-Info',
    ])


def test_client_error(ar_ctx):
    """
    BALANCER-1716
    Если случилась ошибка во время отправки 302 robot клиенту,
    то запрос не должен пролетать в бекенд.
    """
    ar_ctx.start_antirobot_backend(SimpleDelayedConfig(
        http.response.ok(headers={'X-ForwardToUser-Y': '1'}),
        response_delay=2.5
    ))
    ar_ctx.start_additional_backends()
    ar_ctx.start_antirobot_balancer()

    with ar_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        for i in range(5):
            stream.write_request(http.request.get().to_raw_request())

    for run in Multirun(sum_delay=20):
        with run:
            unistat = ar_ctx.get_unistat()
            assert unistat['report-service_total-requests_summ'] > 0 and unistat['http-http_unfinished_backend_stream_error_summ'] > 0

    assert ar_ctx.backend.state.requests.empty()


def test_antirobot_set_cookie(ar_ctx):
    """
    BALANCER-3267
    Проверяем, что X-Antirobot-Set-Cookie перекладывается в Cookie и отправляется пользователю
    """
    ar_ctx.start_antirobot_backend(SimpleConfig(
        http.response.ok(headers={'X-Antirobot-Set-Cookie': ['biscuit', 'platzchen']}),
    ))
    ar_ctx.start_additional_backends()
    ar_ctx.start_antirobot_balancer()

    response = ar_ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.header_values(response, 'Set-Cookie', ['biscuit', 'platzchen'])


@pytest.mark.parametrize('use_http2', [False, True], ids=['', 'http2'])
def test_antirobot_ban_source_ip(ar_ctx, use_http2):
    """
    BALANCER-3301
    Проверяем, что ip адрес блокируется, если антиробот вернул заголовок X-Antirobot-Ban-Source-Ip
    """
    ar_ctx.start_antirobot_backend(SimpleConfig(
        http.response.ok(headers={'X-Antirobot-Ban-Source-Ip': '1'}),
    ))
    ar_ctx.start_additional_backends()
    ban_addresses_disable_file = ar_ctx.manager.fs.get_unique_name('ban_addresses_disable_file')
    ar_ctx.start_antirobot_balancer(
        ban_addresses_disable_file=ban_addresses_disable_file,
        root_module='headers'
    )
    request = http2.request.get() if use_http2 else http.request.get()
    expected_errors = (socket.error, NoHTTP2FrameException if use_http2 else HTTPReaderException)

    # текущее соединение должно быть закрыто
    with ar_ctx.create_http_connection(http2=use_http2) as conn:
        with pytest.raises(expected_errors):
            conn.perform_request(request)
        for run in Multirun(sum_delay=20):
            with run:
                unistat = ar_ctx.get_unistat()
                assert unistat['report-service_total-requests_summ'] == 1
                assert unistat['ban-addresses_ammv'] == 1
                assert unistat['ban-conns_summ'] == 0
                assert unistat['worker-tcp_conns_ammv'] == 0
        asserts.is_closed(conn.sock)

    # новое соединение должно рваться
    with pytest.raises(expected_errors):
        ar_ctx.perform_request(request, http2=use_http2)
    with pytest.raises(expected_errors):
        ar_ctx.perform_request(request, http2=use_http2)
    for run in Multirun(sum_delay=20):
        with run:
            unistat = ar_ctx.get_unistat()
            assert unistat['report-service_total-requests_summ'] == 1
            assert unistat['ban-addresses_ammv'] == 1
            assert unistat['ban-conns_summ'] == 2
            assert unistat['worker-tcp_conns_ammv'] == 0

    filepath = ar_ctx.manager.fs.create_file(ban_addresses_disable_file)
    assert filepath == ban_addresses_disable_file
    watcher = CheckersWatcher(ar_ctx, ban_addresses_disable_file)
    watcher.wait_checker(is_exists=True)

    with ar_ctx.create_http_connection(http2=use_http2) as conn:
        response = conn.perform_request(request)

        asserts.status(response, 200)

        for run in Multirun(sum_delay=20):
            with run:
                unistat = ar_ctx.get_unistat()
                assert unistat['report-service_total-requests_summ'] == 2
                assert unistat['ban-addresses_ammv'] == 0
                assert unistat['ban-conns_summ'] == 2
                assert unistat['worker-tcp_conns_ammv'] == 1

        asserts.is_not_closed(conn.sock)
        ar_ctx.manager.fs.remove(ban_addresses_disable_file)
        watcher.wait_checker(is_exists=False)

        # текущее соединение должно быть закрыто
        with pytest.raises(expected_errors):
            conn.perform_request(request)
        for run in Multirun(sum_delay=20):
            with run:
                unistat = ar_ctx.get_unistat()
                assert unistat['report-service_total-requests_summ'] == 3
                assert unistat['ban-addresses_ammv'] == 1
                assert unistat['ban-conns_summ'] == 2
                assert unistat['worker-tcp_conns_ammv'] == 0
        asserts.is_closed(conn.sock)
