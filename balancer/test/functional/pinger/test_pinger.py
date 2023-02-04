# -*- coding: utf-8 -*-
import pytest
import time
from datetime import timedelta

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.stdlib.multirun import Multirun

from configs import PingerConfig

from balancer.test.util.predef.handler.server.http import CloseConfig, ThreeModeConfig, SimpleConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


PINGER_REQUEST = '/pinger_admin'


class PingContext(object):
    def __init__(self):
        super(PingContext, self).__init__()
        self.__check_file = self.manager.fs.create_file('check_file')
        self.__switch_off_file = self.manager.fs.create_file('switch_off')

    @property
    def check_file(self):
        return self.__check_file

    @property
    def switch_off_file(self):
        return self.__switch_off_file

    def start_ping_backend(self, config):
        return self.start_backend(config)

    def start_ping_balancer(self, **balancer_kwargs):
        if not hasattr(self, 'backend'):
            balancer_kwargs['backend_port'] = self.manager.port.get_port()
        else:
            balancer_kwargs['backend_port'] = self.backend.server_config.port

        return self.start_balancer(PingerConfig(**balancer_kwargs))

    def start_check_file_balancer(self, **balancer_kwargs):
        return self.start_ping_balancer(check_file=self.check_file, **balancer_kwargs)

    def start_switch_off_file_balancer(self, **balancer_kwargs):
        return self.start_ping_balancer(switch_off_file=self.switch_off_file, **balancer_kwargs)

    def start_all_files_balancer(self, **balancer_kwargs):
        return self.start_ping_balancer(check_file=self.check_file, switch_off_file=self.switch_off_file, **balancer_kwargs)

    def get_backend_request_times(self):
        retval = []

        while not self.backend.state.requests.empty():
            info = self.backend.state.requests.get()
            retval.append(info.start_time)

        return retval

    def do_requests(self, num, sleep):
        for _ in range(num):
            conn = self.create_http_connection()
            stream = conn.create_stream()
            stream.write_request(http.request.raw_get())
            time.sleep(sleep)

    def request_admin(self, status=200):
        response = self.perform_request(http.request.get(path=PINGER_REQUEST))
        asserts.status(response, status)

    def request_admin_empty(self):
        self.perform_request_xfail(http.request.get(path=PINGER_REQUEST))


ping_ctx = mod_ctx.create_fixture(PingContext)


def test_delay(ping_ctx):
    """
    Балансер должен отправлять пингующие запросы с периодом delay, указанным в конфиге
    """
    n = 20
    delay = 2
    min_delta = timedelta(seconds=0.9 * delay)
    max_delta = timedelta(seconds=1.1 * delay)

    ping_ctx.start_ping_backend(CloseConfig())
    ping_ctx.start_ping_balancer(delay='%gs' % delay)

    time.sleep(delay * n)

    ping_timestamps = ping_ctx.get_backend_request_times()

    pairs = zip(ping_timestamps[:-1], ping_timestamps[1:])
    deltas = map(lambda x_y: x_y[1] - x_y[0], pairs)

    assert len(ping_timestamps) >= n - 1
    assert min(deltas) >= min_delta
    assert max(deltas) <= max_delta


def test_lo_watermark(ping_ctx):
    """
    Если доля успешных запросов меньше lo, указанного в конфиге,
    то на админский запрос балансер закрывает соединение
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(first=6, second=4))
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()


def test_hi_watermark(ping_ctx):
    """
    Если доля успешных запросов опустилась ниже lo, а затем поднялась выше hi,
    то на админский запрос балансер отвечает 200
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(prefix=10, first=10, second=0))
    ping_ctx.start_ping_balancer(delay=5, histtime=5, lo=0.3, hi=0.4)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()

    ping_ctx.do_requests(10, 0.1)
    ping_ctx.request_admin()


def test_lo_watermark_status_codes(ping_ctx):
    """
    BALANCER-1323
    Если доля успешных запросов меньше lo, указанного в конфиге,
    то на админский запрос балансер закрывает соединение.
    Статус коды, указанные в status_codes считаются "хорошими",
    остальные - "плохими".
    """
    ping_ctx.start_ping_backend(SimpleConfig(http.response.not_found()))
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8, status_codes='200')

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()


def test_status_code_classes(ping_ctx):
    """
    BALANCER-1323
    Если доля успешных запросов меньше lo, указанного в конфиге,
    то на админский запрос балансер закрывает соединение.
    Статус коды, указанные в status_codes считаются "хорошими",
    остальные - "плохими".
    """
    ping_ctx.start_ping_backend(SimpleConfig(http.response.not_found()))
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8, status_codes='200,4xx')

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin()


def test_lo_watermark_status_codes_exceptions(ping_ctx):
    """
    BALANCER-1323
    Если доля успешных запросов меньше lo, указанного в конфиге,
    то на админский запрос балансер закрывает соединение.
    Статус коды, указанные в status_codes считаются "хорошими",
    остальные - "плохими".
    Статус код может быть задан как "класс" - 4xx. В этом случае можно
    считать отдельные коды "плохими" в status_codes_exceptions.
    """
    ping_ctx.start_ping_backend(SimpleConfig(http.response.not_found()))
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8, status_codes='200,4xx', status_codes_exceptions='404')

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()


def test_above_lo(ping_ctx):
    """
    Если доля успешных запросов не опускалась ниже lo, то на админский запрос балансер отвечает 200
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_ping_balancer(delay=2, lo=0.6, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin()


def test_below_hi(ping_ctx):
    """
    Если доля успешных запросов опустилась ниже lo, и не поднялась выше hi,
    то на админский запрос балансер закрывает соединение
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(prefix=10, first=10, second=0))
    ping_ctx.start_ping_balancer(delay=5, histtime=5, lo=0.4, hi=0.6)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()

    ping_ctx.do_requests(10, 0.1)
    ping_ctx.request_admin_empty()


def test_histtime(ping_ctx):
    """
    Для определения состояния backend-ов используются только те запросы, возраст которых меньше histtime
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(prefix=10, first=10, second=0))
    ping_ctx.start_ping_balancer(delay=1, histtime=1, lo=0.8, hi=0.9)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()
    time.sleep(0.5)

    ping_ctx.do_requests(10, 0.1)
    ping_ctx.request_admin()


def test_keepalive_client(ping_ctx):
    """
    Если клиент задает запрос с keepalive, то балансер должен вернуть ответ backend-а и не закрывать соединение
    """
    ping_ctx.start_ping_backend(SimpleConfig(response=http.response.ok(data=['A' * 10] * 2)))
    ping_ctx.start_ping_balancer(keepalive=1)

    conn = ping_ctx.create_http_connection()
    response = conn.perform_request(http.request.get())
    asserts.content(response, 'A' * 20)
    asserts.is_not_closed(conn.sock)


def test_keepalive_check(ping_ctx):
    """
    Если backend работает, то на админский запрос с keepalive
    балансер должен ответить 200 и не закрывать соеднинение
    """
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_ping_balancer(keepalive=1)

    conn = ping_ctx.create_http_connection()
    response = conn.perform_request(http.request.get(path=PINGER_REQUEST))
    asserts.status(response, 200)
    asserts.is_not_closed(conn.sock)


def test_keepalive_check_close(ping_ctx):
    """
    Если backend не отвечает, то на админский запрос с keepalive балансер должен закрыть соединение
    """
    ping_ctx.start_ping_balancer(keepalive=1)

    for run in Multirun(plan=[0.1] * 30):  # waiting for first ping request
        with run:
            ping_ctx.perform_request_xfail(http.request.get(path=PINGER_REQUEST))


def test_check_file(ping_ctx):
    """
    При появлении файла, указанного в enable_tcp_check_file,
    балансер должен отвечать 200 на админский запрос, даже если backend-ы не отвечают
    """
    ping_ctx.manager.fs.remove(ping_ctx.check_file)
    ping_ctx.start_check_file_balancer()

    ping_ctx.request_admin_empty()
    ping_ctx.manager.fs.rewrite(ping_ctx.check_file, '')

    for run in Multirun(plan=[0.1] * 20):
        with run:
            ping_ctx.request_admin()


def test_check_file_backend(ping_ctx):
    """
    Если backend отвечает и есть файл, указанный в enable_tcp_check_file,
    то балансер должен отвечать 200 на админский запрос
    """
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_check_file_balancer()

    ping_ctx.request_admin()


def test_check_file_backend_keepalive(ping_ctx):
    """
    Если backend отвечает, есть файл, указанный в enable_tcp_check_file, и включен keepalive,
    то балансер должен отвечать 200 на админский запрос и не закрывать соединение
    """
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_check_file_balancer(keepalive=1)

    conn = ping_ctx.create_http_connection()
    response = conn.perform_request(http.request.get(path=PINGER_REQUEST))
    asserts.status(response, 200)
    asserts.is_not_closed(conn.sock)


def test_check_file_keepalive(ping_ctx):
    """
    Если backend не отвечает, есть файл, указанный в enable_tcp_check_file, и включен keepalive,
    то балансер должен отвечать 200 на админский запрос и не закрывать соединение
    """
    ping_ctx.start_check_file_balancer(keepalive=1)

    conn = ping_ctx.create_http_connection()
    response = conn.perform_request(http.request.get(path=PINGER_REQUEST))
    asserts.status(response, 200)
    asserts.is_not_closed(conn.sock)


def test_check_file_removed(ping_ctx):
    """
    При удалении файла, указанного в enable_tcp_check_file,
    балансер должен закрывать соединение на админский запрос, если backend-ы не отвечают
    """
    ping_ctx.start_check_file_balancer()
    time.sleep(1)

    ping_ctx.request_admin()

    ping_ctx.manager.fs.remove(ping_ctx.check_file)

    for run in Multirun(plan=[0.1] * 20):
        with run:
            ping_ctx.request_admin_empty()


def test_check_file_removed_backend(ping_ctx):
    """
    Если tcp_check_file отсутствует, а backend работает,
    то балансер должен вернуть 200 на админский запрос
    """
    ping_ctx.manager.fs.remove(ping_ctx.check_file)
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_check_file_balancer()

    ping_ctx.request_admin()


_SWITCH_OFF_DATA_ON = [
    'switch_off,1',
    'switch_off, 1',
    'switch_off,60',
    'test,42\nswitch_off,1',
    'switch_off,1\ntest,42',
]


@pytest.mark.parametrize('switch_off_data', _SWITCH_OFF_DATA_ON, ids=[repr(s).strip("'") for s in _SWITCH_OFF_DATA_ON])
def test_switch_off_file_on(ping_ctx, switch_off_data):
    """
    Если есть switch_off_file и в нём есть строка switch_off_key, 1,
    то админка должна фейлить запросы несмотря на состояние бэкэнда
    """
    with open(ping_ctx.switch_off_file, 'wb') as f:
        f.write(switch_off_data)

    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()


_SWITCH_OFF_DATA_OFF = [
    '',
    'switch_off,-1',
    'switch_off, -1',
    'switch_off,0',
    'test,42\nswitch_off,-1',
    'switch_off,-1\ntest,42',
    'test,42'
]


@pytest.mark.parametrize('switch_off_data', _SWITCH_OFF_DATA_OFF, ids=[repr(s).strip("'") for s in _SWITCH_OFF_DATA_OFF])
def test_switch_off_file_off(ping_ctx, switch_off_data):
    """
    Если есть switch_off_file и в нём есть строка switch_off_key, [digit],
    где [digit] - число, не превышающее 0,
    то админка должна отдавать состояние бекенда
    """
    with open(ping_ctx.switch_off_file, 'wb') as f:
        f.write(switch_off_data)

    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin()


@pytest.mark.parametrize('switch_off_key', [
    'off',
    'my_off',
    'switch off',
])
def test_switch_off_file_switch_key(ping_ctx, switch_off_key):
    """
    Если есть switch_off_file и в нём есть строка [switch_off_key], [digit],
    где [digit] - число больше 0,
    то админка должна фейлить запросы несмотря на состояние бэкэнда
    """
    with open(ping_ctx.switch_off_file, 'wb') as f:
        f.write('%s,1' % switch_off_key)

    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8, switch_off_key=switch_off_key)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()


def test_switch_off_file_does_not_exist(ping_ctx):
    """
    Если есть параметр switch_off_file, но самого файла не существует,
    то админка должна отдавать состояние бекенда
    """
    ping_ctx.manager.fs.remove(ping_ctx.switch_off_file)

    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin()


def test_switch_off_file_erased(ping_ctx):
    """
    Если есть switch_off_file со строкой switch_off,1, то админка должна
    отадавать признак неуспеха. После удаления этого файла админка должна
    начать возвращать истинное состояние бэкэнда.
    """
    with open(ping_ctx.switch_off_file, 'wb') as f:
        f.write('switch_off,1')

    ping_ctx.start_ping_backend(ThreeModeConfig(first=100, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin_empty()

    ping_ctx.manager.fs.remove(ping_ctx.switch_off_file)
    time.sleep(1.5)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin()


def test_admin_error_replier_ok(ping_ctx):
    """
    BALANCER-622
    Если есть подмодуль в секции admin_error_replier, то
    на админские запросы к модулю должен отдаваться стандартный ответ,
    если бэкэнд включён
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(first=6, second=4))
    admin_error_replier_status = 500
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8, admin_error_replier_status=admin_error_replier_status)

    ping_ctx.do_requests(1, 0.1)
    ping_ctx.request_admin()


def test_admin_error_replier_off(ping_ctx):
    """
    BALANCER-622
    Если есть подмодуль в секции admin_error_replier, то
    на админские запросы к модулю должен отдаваться результат этой секции,
    если бэкэнд выключен
    """
    ping_ctx.start_ping_backend(ThreeModeConfig(first=6, second=4))
    admin_error_replier_status = 500
    ping_ctx.start_ping_balancer(lo=0.7, hi=0.8, admin_error_replier_status=admin_error_replier_status)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin(status=admin_error_replier_status)


def test_admin_error_replier_off_by_file(ping_ctx):
    """
    BALANCER-622
    Если есть подмодуль в секции admin_error_replier, то
    на админские запросы к модулю должен отдаваться результат этой секции,
    если бэкэнд выключен файловой ручкой
    """
    with open(ping_ctx.switch_off_file, 'wb') as f:
        f.write('switch_off, 1')

    admin_error_replier_status = 500

    ping_ctx.start_ping_backend(ThreeModeConfig(first=7, second=3))
    ping_ctx.start_switch_off_file_balancer(delay=2, lo=0.6, hi=0.8, admin_error_replier_status=admin_error_replier_status)

    ping_ctx.do_requests(9, 0.1)
    ping_ctx.request_admin(status=admin_error_replier_status)


@pytest.mark.parametrize('connection_manager_required', [True, False])
def test_skip_keepalive_in_ping(ping_ctx, connection_manager_required):
    """
    BALANCER-2874 ping requests should not use keepalive connections
    """
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_ping_balancer(
        delay=0.1,
        keepalive=1,
        keepalive_count=30,
        connection_manager_required=connection_manager_required
    )
    tcpdump = ping_ctx.manager.tcpdump.start(ping_ctx.backend.server_config.port)

    time.sleep(2)
    tcpdump.read_all()
    closed_sessions = tcpdump.get_closed_sessions()
    sessions = tcpdump.get_sessions()
    assert len(sessions) > 10
    assert len(closed_sessions) > 10


@pytest.mark.parametrize('connection_manager_required', [True, False])
def test_keep_keepalive_in_req(ping_ctx, connection_manager_required):
    """
    BALANCER-2874 ping requests should not use keepalive connections but user requests should not be affected
    """
    ping_ctx.start_ping_backend(SimpleConfig())
    ping_ctx.start_ping_balancer(
        delay=600,
        histtime=1200,
        keepalive=1,
        keepalive_count=30,
        connection_manager_required=connection_manager_required
    )

    tcpdump = ping_ctx.manager.tcpdump.start(ping_ctx.backend.server_config.port)
    time.sleep(1)

    for _ in range(10):
        conn = ping_ctx.create_http_connection()
        response = conn.perform_request(http.request.get(headers={"connection": "close"}))
        asserts.status(response, 200)

    time.sleep(1)
    tcpdump.read_all()
    closed_sessions = tcpdump.get_closed_sessions()
    sessions = tcpdump.get_sessions()
    assert len(sessions) < 3
    assert len(closed_sessions) < 3
