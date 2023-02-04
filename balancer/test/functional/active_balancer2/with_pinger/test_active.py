# -*- coding: utf-8 -*-
import time
import types
import datetime
import pytest

import balancer.test.plugin.context as mod_ctx

import configs

from balancer.test.functional.active_balancer2.common import \
    get_times, check_near, assert_responses, DELAY, MIN_DELTA, MAX_DELTA, TEST_URL
from balancer.test.util.sync import Counter
from balancer.test.util.proto.handler.server import State
from balancer.test.util.proto.handler.server.http import StaticResponseHandler, StaticResponseConfig
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.sanitizers import sanitizers


class ActiveBrokenHandler(StaticResponseHandler):

    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.path == TEST_URL:
            self.state.tests_count.inc()
        if not self.config.start_broken <= self.state.tests_count.value < self.config.fin_broken:
            stream.write_response(self.config.response)
            self.finish_response()
        else:
            self.force_close()


class ActiveBrokenState(State):

    def __init__(self, config):
        super(ActiveBrokenState, self).__init__(config)
        self.__tests_count = Counter()

    @property
    def tests_count(self):
        return self.__tests_count


class ActiveBrokenConfig(StaticResponseConfig):
    HANDLER_TYPE = ActiveBrokenHandler
    STATE_TYPE = ActiveBrokenState

    def __init__(self, start_broken, fin_broken):
        super(ActiveBrokenConfig, self).__init__(response=http.response.ok())
        self.start_broken = start_broken
        self.fin_broken = fin_broken


class ActiveB2Options:
    ACTIVE_CONFIG = configs.ActiveConfig
    ACTIVE_ON_ERROR_CONFIG = configs.ActiveOnErrorConfig
    ACTIVE_NO_WORKERS_CONFIG = configs.ActiveNoWorkersConfig
    BACKEND_STATS_PATH = [
        ('ipdispatch/test/http/balancer2/backends/backends/backend[1]/enabled', '1'),
        ('ipdispatch/test/http/balancer2/backends/backends/backend[2]/enabled', '0'),
    ]


class HashingActiveB2Options:
    ACTIVE_CONFIG = configs.HashingActiveConfig
    ACTIVE_ON_ERROR_CONFIG = configs.HashingActiveOnErrorConfig
    ACTIVE_NO_WORKERS_CONFIG = configs.HashingActiveNoWorkersConfig
    BACKEND_STATS_PATH = [
        ('ipdispatch/test/http/hasher/balancer2/backends/backends/backend[1]/enabled', '1'),
        ('ipdispatch/test/http/hasher/balancer2/backends/backends/backend[2]/enabled', '0'),
    ]
    SKIP_REQUESTS = (
        http.request.get(path='/1'),
        http.request.get(path='/3'),
    )


class ActiveContext(object):
    def __init__(self):
        super(ActiveContext, self).__init__()
        if isinstance(self.request, (types.TypeType, types.ClassType)):
            self.__active_options = self.request
        else:
            self.__active_options = self.request.param

    @property
    def active_options(self):
        return self.__active_options

    def start_active_balancer(self, config_type, **options):
        if 'request' not in options:
            options['request'] = http.request.get(path=TEST_URL)
        if 'delay' not in options:
            options['delay'] = '%ds' % DELAY

        return self.start_balancer(config_type(**options))

    def start_with_valid_backends(self, steady=True):
        self.start_backend(SimpleConfig(), name='backend1')
        self.start_backend(SimpleConfig(), name='backend2')
        return self.start_active_balancer(
            self.__active_options.ACTIVE_CONFIG,
            backend_port1=self.backend1.server_config.port,
            backend_port2=self.backend2.server_config.port,
            steady=steady,
        )

    def start_with_broken_backend(self, start_broken, fin_broken, attempts=1, active_skip_attempts=None):
        self.start_backend(SimpleConfig(), name='backend1')
        self.start_backend(ActiveBrokenConfig(start_broken, fin_broken), name='backend2')
        return self.start_active_balancer(
            self.__active_options.ACTIVE_CONFIG,
            backend_port1=self.backend1.server_config.port,
            backend_port2=self.backend2.server_config.port,
            attempts=attempts,
            active_skip_attempts=active_skip_attempts)

    def send_requests_block(self, block_size=20):
        responses = list()
        request = http.request.get()
        for _ in range(block_size):
            time.sleep(0.5 * DELAY)
            self.perform_request(request)
        return responses

    def do_requests(self, block_size=20):
        responses = self.send_requests_block(block_size)
        assert_responses(responses)

    def exec_broken_backend(self, start_broken=5, fin_broken=10):
        self.start_with_broken_backend(start_broken, fin_broken, attempts=2)

        self.do_requests(block_size=30)

        tests, requests = get_times(self.backend2)

        start = tests[start_broken - 1]
        fin = tests[fin_broken - 1]

        return start, fin, requests


active_with_pinger_ctx = mod_ctx.create_fixture(
    ActiveContext,
    params=[ActiveB2Options, HashingActiveB2Options],
    ids=['active_b2', 'hashing_active_b2'],
)


def test_tests_during_client_requests(active_with_pinger_ctx):
    """
    Условия: балансер с одним worker-ом
    В конфиге задается delay -- период, с которым нужно задавать тестовые запросы на backend-ы
    На балансер задаются клиентские запросы с периодом меньшим чем delay

    Поведение:
    Если на балансер идут клиентские запросы с периодом, меньшим delay,
    то балансер задает на backend-ы тестовые запросы
    """
    active_with_pinger_ctx.start_with_valid_backends()

    active_with_pinger_ctx.do_requests()

    tests1, requests1 = get_times(active_with_pinger_ctx.backend1)
    tests2, requests2 = get_times(active_with_pinger_ctx.backend2)
    assert check_near(requests1, tests1)
    assert check_near(requests2, tests2)


def test_delay_between_tests(active_with_pinger_ctx):
    """
    Условия: балансер с одним worker-ом
    В конфиге задается delay -- период, с которым нужно задавать тестовые запросы на backend-ы
    На балансер задаются клиентские запросы с периодом меньшим чем delay

    Поведение:
    Балансер задает тестовые запросы с периодом delay
    """

    def check_tests_delta(tests):
        neighbours = zip(tests[:-1], tests[1:])
        check_neighbours = lambda t1_t2: MIN_DELTA < t1_t2[1] - t1_t2[0] < MAX_DELTA
        return all(map(check_neighbours, neighbours))

    active_with_pinger_ctx.start_with_valid_backends()

    active_with_pinger_ctx.do_requests()

    tests1 = get_times(active_with_pinger_ctx.backend1)[0]
    tests2 = get_times(active_with_pinger_ctx.backend2)[0]
    assert check_tests_delta(tests1)
    assert check_tests_delta(tests2)


def test_broken_backend_no_requests(active_with_pinger_ctx):
    """
    Условия: на балансер задаются клиентские запросы
    Один из backend-ов сначала отвечает на запросы, потом перестает отвечать, потом снова отвечает

    Поведение:
    Пока backend не отвечает на тестовые запросы, клиентские запросы на него не задаются
    """
    start, fin, requests = active_with_pinger_ctx.exec_broken_backend()
    start = start + datetime.timedelta(seconds=3)
    if sanitizers.msan_enabled() or sanitizers.asan_enabled():
        start = start + datetime.timedelta(seconds=3)

    during = filter(lambda x: start < x <= fin, requests)
    assert during == []


def test_broken_backend_revived(active_with_pinger_ctx):
    """
    Условия: на балансер задаются клиентские запросы
    Один из backend-ов сначала отвечает на запросы, потом перестает отвечать, потом снова отвечает

    Поведение:
    Когда backend начинает отвечать на тестовые запросы, на него начинают задаваться клиентские запросы
    """
    _, fin, requests = active_with_pinger_ctx.exec_broken_backend()
    after = filter(lambda x: x > fin, requests)

    assert after != []


def test_on_error(active_with_pinger_ctx):
    """
    SEPE-4447
    Условия: ни один из указанных backend-ов не работает

    Поведение:
    вернуть то, что указано в секции on_error
    """
    port1 = active_with_pinger_ctx.manager.port.get_port()
    port2 = active_with_pinger_ctx.manager.port.get_port()
    active_with_pinger_ctx.start_balancer(active_with_pinger_ctx.active_options.ACTIVE_ON_ERROR_CONFIG(port1, port2))
    time.sleep(1)

    response = active_with_pinger_ctx.perform_request(http.request.get())

    asserts.status(response, 503)
    asserts.reason_phrase(response, 'Service unavailable')
    asserts.content(response, 'Error')


def test_backend_failed(active_with_pinger_ctx):
    """
    SEPE-8241
    Один из backend-ов не отвечает на тестовые запросы
    Другой клиент ответил на первый клиентский и тестовый запрос и умер
    На следующий клиентский запрос балансер должен разорвать соединение и не зависнуть
    """
    active_with_pinger_ctx.start_with_broken_backend(0, 10, attempts=2)

    with active_with_pinger_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
    time.sleep(1)
    active_with_pinger_ctx.backend1.finish()
    with active_with_pinger_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
        asserts.is_closed(conn.sock)

    asserts.status(response, 200)


def test_active_with_pinger_request_ratio(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '1'}, data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend2')), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        weight1=10, weight2=1, use_backend_weight=True, delay=0.5))
    time.sleep(3)
    counter1 = 0
    counter2 = 0
    for _ in xrange(100):
        response = ctx.perform_request(http.request.get())
        if str(response.data) == 'backend1':
            counter1 += 1
        elif str(response.data) == 'backend2':
            counter2 += 1
        else:
            assert False
        asserts.status(response, 200)
    assert counter1 + counter2 == 100
    assert 45 < counter1 < 55
    assert 45 < counter2 < 55


def test_active_with_pinger_backend_without_weight(ctx):
    '''
        Test that if backend doesn't send 'rs_weight' header to pinger,
        the backend gets default weight and still gets requests
    '''
    ctx.start_backend(SimpleConfig(http.response.ok(data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '4'}, data='backend2')), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        weight1=1, weight2=1, use_backend_weight=True, delay=0.5))
    time.sleep(3)
    counter1 = 0
    counter2 = 0
    for _ in xrange(100):
        response = ctx.perform_request(http.request.get())
        if str(response.data) == 'backend1':
            counter1 += 1
        elif str(response.data) == 'backend2':
            counter2 += 1
        else:
            assert False
        asserts.status(response, 200)
        time.sleep(0.5)
    # distribution should be around 4:1
    assert counter1 + counter2 == 100
    assert counter1 >= 5
    assert counter2 >= 70


def test_active_with_pinger_with_disabled_file_checker(ctx):
    '''
    Проверяем, что backend_weight_disable_file отключает использование вес бэкендов.
    '''
    disable_file = ctx.manager.fs.create_file('disable_file')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '1'}, data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend2')), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        weight1=10, weight2=1, use_backend_weight=True, delay=0.5, backend_weight_disable_file=disable_file))
    time.sleep(3)
    counter1 = 0
    counter2 = 0
    for _ in xrange(100):
        response = ctx.perform_request(http.request.get())
        if str(response.data) == 'backend1':
            counter1 += 1
        elif str(response.data) == 'backend2':
            counter2 += 1
        else:
            assert False
        asserts.status(response, 200)
    assert counter1 + counter2 == 100
    assert 85 < counter1

    ctx.manager.fs.remove(disable_file)
    time.sleep(2)

    counter1 = 0
    counter2 = 0
    for _ in xrange(100):
        response = ctx.perform_request(http.request.get())
        if str(response.data) == 'backend1':
            counter1 += 1
        elif str(response.data) == 'backend2':
            counter2 += 1
        else:
            assert False
        asserts.status(response, 200)
    assert counter1 + counter2 == 100
    assert 45 < counter1 < 55
    assert 45 < counter2 < 55


def test_stat_max_delay_of_pings_with_enabled_pinger(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend2')), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        workers=5))
    time.sleep(1)
    for _ in xrange(30):
        unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)['active-default-max_delay_of_pings_hgram']
        assert sum(s for _, s in unistat[:5]) > 0
        assert sum(s for _, s in unistat[5:]) == 0
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)
        time.sleep(0.5)


def test_stat_max_delay_of_pings_with_disabled_pinger(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend2')), name='backend2')
    ctx.start_balancer(configs.ActiveConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        request=http.request.get(),
        workers=5))
    time.sleep(1)
    for _ in xrange(30):
        unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)['active-default-max_delay_of_pings_hgram']
        assert sum(s for _, s in unistat[:4]) > 0
        assert sum(s for _, s in unistat[4:]) == 0
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)
        time.sleep(0.5)


def test_pinger_stats_counters_succ_and_fail(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'})), name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        workers=2,
        delay=1.2))
    time.sleep(1)
    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
    prev_total = unistat['total_pings_summ']
    prev_succ = unistat['successful_pings_summ']
    prev_fail = unistat['failed_pings_summ']
    time.sleep(1)
    for _ in xrange(7):
        unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
        total = unistat['total_pings_summ']
        succ = unistat['successful_pings_summ']
        fail = unistat['failed_pings_summ']
        assert prev_total + 2 <= total <= prev_total + 4
        assert prev_succ + 1 <= succ <= prev_succ + 2
        assert prev_fail + 1 <= fail <= prev_fail + 2
        prev_total = total
        prev_succ = succ
        prev_fail = fail
        time.sleep(1)


def test_pinger_stats_counters_status_and_parse(ctx):
    ctx.start_backend(SimpleConfig(http.response.not_found(headers={'RS-Weight': '10'})), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok()), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        workers=2,
        delay=1.2,
        use_backend_weight=True))
    time.sleep(1)
    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
    prev_total = unistat['total_pings_summ']
    prev_status = unistat['bad_status_pings_summ']
    prev_parse = unistat['parse_failed_pings_summ']
    time.sleep(1)
    for _ in xrange(7):
        unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
        total = unistat['total_pings_summ']
        status = unistat['bad_status_pings_summ']
        parse = unistat['parse_failed_pings_summ']
        assert prev_total + 2 <= total <= prev_total + 4
        assert prev_status + 1 <= status <= prev_status + 2
        assert prev_parse + 1 <= parse <= prev_parse + 2
        prev_total = total
        prev_status = status
        prev_parse = parse
        time.sleep(1)


def test_tcp_check(ctx):
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_fake_backend(name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        request=None, tcp_check=True
    ))

    time.sleep(2)

    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
    assert unistat['active-default-all_alive_flag_ammv'] == 1
    assert unistat['failed_pings_summ'] > 0
    assert unistat['connection_other_error_pings_summ'] > 0
    asserts.status(ctx.perform_request(http.request.get()), 200)


@pytest.mark.parametrize('connection_manager_required', [True, False])
def test_skip_keepalive_in_ping(ctx, connection_manager_required):
    """
    BALANCER-2874 ping requests should not use keepalive connections
    """
    backend1 = ctx.start_backend(SimpleConfig(), name='backend1')
    backend2 = ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        delay=0.1,
        backend_port1=backend1.server_config.port,
        backend_port2=backend2.server_config.port,
        keepalive_count=50,
        connection_manager_required=connection_manager_required,
    ))

    tcpdump = ctx.manager.tcpdump.start(backend1.server_config.port)
    time.sleep(2)

    tcpdump.read_all()
    closed_sessions = tcpdump.get_closed_sessions()
    sessions = tcpdump.get_sessions()
    assert len(sessions) > 10
    assert len(closed_sessions) > 10
