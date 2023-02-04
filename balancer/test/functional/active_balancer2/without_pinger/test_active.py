# -*- coding: utf-8 -*-
import time
import types
import pytest
import requests

import balancer.test.plugin.context as mod_ctx

import configs

from balancer.test.functional.active_balancer2.common import get_times, check_near, assert_responses, DELAY, TEST_URL
from balancer.test.util.sync import Counter
from balancer.test.util.proto.handler.server import State
from balancer.test.util.proto.handler.server.http import StaticResponseHandler, StaticResponseConfig
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.process import BalancerStartError


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


active_ctx = mod_ctx.create_fixture(
    ActiveContext,
    params=[ActiveB2Options, HashingActiveB2Options],
    ids=['active_b2', 'hashing_active_b2'],
)

hashing_active_ctx = mod_ctx.create_fixture(
    ActiveContext,
    params=[HashingActiveB2Options],
    ids=['hashing_active_b2'],
)


def test_no_test_requests(active_ctx):
    """
    SEPE-4360
    Условия: в конфиге задается delay -- период, с которым нужно задавать тестовые запросы на backend-ы
    На балансер задается несколько групп запросов
    Внутри группы расстояние между запросами меньше delay
    Между группами расстояние больше 2 * delay

    Поведение:
    Если на балансер не идут запросы в течение delay, то тестовые запросы не задаются
    """
    active_ctx.start_with_valid_backends(steady=False)

    responses = list()

    for _ in range(3):
        responses.extend(active_ctx.send_requests_block(block_size=10))
        time.sleep(3 * DELAY)

    assert_responses(responses)
    tests1, requests1 = get_times(active_ctx.backend1)
    tests2, requests2 = get_times(active_ctx.backend2)
    assert check_near(tests1 + tests2, requests1 + requests2)


def test_no_backends(active_ctx):
    """
    SEPE-4420
    Если все backend-ы недоступны, то балансер закрывает соединение
    """
    port1 = active_ctx.manager.port.get_port()
    port2 = active_ctx.manager.port.get_port()
    balancer = active_ctx.start_balancer(active_ctx.active_options.ACTIVE_NO_WORKERS_CONFIG(port1, port2))
    time.sleep(1)

    with active_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
        asserts.is_closed(conn.sock)
    time.sleep(1)
    assert balancer.is_alive()


def test_skip_attempts(hashing_active_ctx):
    """
    BALANCER-385
    """
    hashing_active_ctx.start_with_broken_backend(0, 10000, active_skip_attempts=1)
    with hashing_active_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
    time.sleep(1.5)
    resp1 = hashing_active_ctx.perform_request(hashing_active_ctx.active_options.SKIP_REQUESTS[0])
    resp2 = hashing_active_ctx.perform_request(hashing_active_ctx.active_options.SKIP_REQUESTS[1])
    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


def test_zero_skip_attempts(hashing_active_ctx):
    """
    BALANCER-385
    """
    hashing_active_ctx.start_with_broken_backend(0, 10000, active_skip_attempts=0)
    with hashing_active_ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())
    time.sleep(1.5)
    err_count = 0
    resp_count = 0
    for req in hashing_active_ctx.active_options.SKIP_REQUESTS:
        try:
            hashing_active_ctx.perform_request(req)
            resp_count += 1
        except HTTPReaderException:
            err_count += 1

    assert resp_count == 1
    assert err_count == 1


@pytest.mark.parametrize('config', [configs.ActiveEasyConfig, configs.ActiveEasyConfigWithPinger, configs.HashingActiveEasyConfig], ids=['active', 'active_pinger', 'hashing'])
def test_steady_active(ctx, config):
    """
    BALANCER-1287: Test requests must come repeatedly when steady option equals True even without client requests
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(config(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=0.7,
        steady=True,
        request='GET /test.html HTTP/1.1\r\n\r\n',
    ))

    time.sleep(2)

    for backend in [ctx.backend1, ctx.backend2]:
        assert backend.state.requests.qsize() >= 1
        asserts.path(backend.state.get_request(), TEST_URL)


@pytest.mark.parametrize(
    ['weight1', 'weight2', 'quorum', 'hysteresis'],
    [
        (1, 1, 1, 1),
        (1, 1, 0.5, 0),
        (1, 1, 1.75, 0.25),
        (5, 5, 2.5, 2.5),
        (2, 2, 1.5, 0.5),
        (1, 0, 0.5, 0.5)
    ]
)
@pytest.mark.parametrize('steady', [True, False, None])
def test_quorum_hysteresis_succ(ctx, weight1, weight2, quorum, hysteresis, steady):
    """
    BALANCER-1415: Balancer must accept connection every time
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=0.7, weight1=weight1,
        weight2=weight2, quorum=quorum, hysteresis=hysteresis,
        steady=steady))

    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)
    time.sleep(1)
    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)


@pytest.mark.parametrize(
    ['weight1', 'weight2', 'quorum', 'hysteresis'],
    [
        (1, 1, 1, 2),
        (1, 0, 0.5, 1),
        (1, 1, 1, 1.1),
        (5, 5, 5, 25),
        (2, 2, 1.5, 2.51),
        (1, 0, 0.5001, 0.5)
    ]
)
@pytest.mark.parametrize('steady', [True, False, None])
def test_quorum_hysteresis_fail(ctx, weight1, weight2, quorum, hysteresis, steady):
    '''
    BALANCER-1415
    Balancer must not accept connection because of lack of weight.
    '''
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=0.7, weight1=weight1,
        weight2=weight2, quorum=quorum, hysteresis=hysteresis,
        steady=steady))

    for i in xrange(3):
        ctx.perform_request_xfail(http.request.get())
    time.sleep(1)
    for i in xrange(3):
        ctx.perform_request_xfail(http.request.get())


@pytest.mark.parametrize(
    ['weight1', 'weight2', 'quorum', 'hysteresis'],
    [
        (1, 1, 1, 1),
        (1, 1, 0.5, 0),
        (5, 5, 2.5, 2.5),
        (2, 2, 1.5, 0.5)
    ]
)
@pytest.mark.parametrize('steady', [True, False, None])
def test_quorum_hysteresis_succ_succ(ctx, weight1, weight2, quorum, hysteresis, steady):
    '''
    BALANCER-1415
    On a first request both backends are enabled and response is ok.
    On a second request only second backend is enabled but it's enough to work.
    '''
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=0.7, weight1=weight1,
        weight2=weight2, quorum=quorum, hysteresis=hysteresis,
        steady=steady))

    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)

    ctx.backend1.stop()
    time.sleep(1)

    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)


@pytest.mark.parametrize(
    ['weight1', 'weight2', 'quorum', 'hysteresis'],
    [
        (1, 0, 0.5, 0.5),
        (1, 1, 1.75, 0.25)
    ]
)
@pytest.mark.parametrize('steady', [True, False, None])
def test_quorum_hysteresis_succ_fail(ctx, weight1, weight2, quorum, hysteresis, steady):
    '''
    BALANCER-1415
    After stopping the first backend balancer fails on each request.
    '''
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=0.7, weight1=weight1,
        weight2=weight2, quorum=quorum, hysteresis=hysteresis,
        steady=steady))

    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)

    ctx.backend1.stop()
    time.sleep(1)

    for i in xrange(3):
        ctx.perform_request_xfail(http.request.get())


class ActiveHandler(StaticResponseHandler):

    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.path == '/change_state':
            self.state.change_state()
            stream.write_response(self.config.response)
            self.finish_response()
            return

        if self.state.working:
            stream.write_response(self.config.response)
            self.finish_response()
        else:
            self.force_close()


class ActiveState(State):

    def __init__(self, config):
        super(ActiveState, self).__init__(config)
        self.__working = True

    def change_state(self):
        self.__working = not self.__working

    @property
    def working(self):
        return self.__working


class OneShotState(State):
    def __init__(self, config):
        super(OneShotState, self).__init__(config)
        self.__working = True

    def change_state(self):
        self.__working = not self.__working

    @property
    def working(self):
        if self.__working:
            return self.__working
        self.__working = True
        return False


class ActiveConfig(StaticResponseConfig):
    HANDLER_TYPE = ActiveHandler
    STATE_TYPE = ActiveState

    def __init__(self):
        super(ActiveConfig, self).__init__(response=http.response.ok())


class OneShotConfig(StaticResponseConfig):
    HANDLER_TYPE = ActiveHandler
    STATE_TYPE = OneShotState

    def __init__(self):
        super(OneShotConfig, self).__init__(response=http.response.ok())


@pytest.mark.parametrize(
    ['weight1', 'weight2', 'quorum', 'hysteresis'],
    [
        (1, 1, 1.1, 0.25)
    ]
)
def test_quorum_hysteresis_several_workers(ctx, weight1, weight2, quorum, hysteresis):
    '''
    BALANCER-1659
        Test that balancer update algorithm state asynchronously
        Shutdown all backends and after activate one of theme there are not enough for working
    '''
    steady = True
    delay = 1
    ctx.start_backend(ActiveConfig(), name='backend1')
    ctx.start_backend(ActiveConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        delay=delay, weight1=weight1,
        weight2=weight2, quorum=quorum, hysteresis=hysteresis,
        steady=steady, workers=10))

    for i in xrange(3):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)

    requests.get('http://localhost:%s/change_state' % ctx.backend1.server_config.port)
    requests.get('http://localhost:%s/change_state' % ctx.backend2.server_config.port)
    time.sleep(2 * delay)
    requests.get('http://localhost:%s/change_state' % ctx.backend1.server_config.port)
    time.sleep(2 * delay)

    for i in xrange(20):
        ctx.perform_request_xfail(http.request.get())


def test_quorum_in_shared_memory(ctx):
    '''
    BALANCER-1659
        Test that if one of workers fail quorum - hysteresis than all workers are failed
    '''
    workers_count, worker_start_delay, ping_delay = 20, 0.01, 0.3
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(ActiveConfig(), name='backend2')
    ctx.start_backend(OneShotConfig(), name='backend3')
    ctx.start_balancer(configs.ActiveThreeBackends(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        backend_port3=ctx.backend3.server_config.port,
        delay=ping_delay,
        quorum=2, hysteresis=0.5,
        workers=workers_count))

    # Wait until all workers are on
    time.sleep(2 * ping_delay + workers_count * worker_start_delay)

    # Disable 2 backend, wait until all workers knows about it
    requests.get('http://localhost:%s/change_state' % ctx.backend2.server_config.port)
    time.sleep(2 * ping_delay)

    # Disable 3'd backend for one ping
    requests.get('http://localhost:%s/change_state' % ctx.backend3.server_config.port)
    time.sleep(2 * ping_delay)

    for i in xrange(20):
        ctx.perform_request_xfail(http.request.get())


def test_ignore_active_quorum_failed_mode_ignores_active_checks(ctx):
    '''
    BALANCER-2222
        Test that if quorum is failed and ignore_active_quorum_failed is set
        balancer ignores active check results
    '''
    workers_count, worker_start_delay, ping_delay = 1, 0.01, 0.3
    ctx.start_backend(SimpleConfig(http.response.ok(data='backend1')), name='backend1')
    ctx.start_backend(ActiveConfig(), name='backend2')
    ctx.start_backend(ActiveConfig(), name='backend3')
    ctx.start_balancer(configs.IgnoreActiveBackends(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        backend_port3=ctx.backend3.server_config.port,
        delay=ping_delay,
        quorum=1.5, hysteresis=0.3,
        workers=workers_count))

    time.sleep(2 * ping_delay + workers_count * worker_start_delay)
    requests.get('http://localhost:%s/change_state' % ctx.backend3.server_config.port)
    requests.get('http://localhost:%s/change_state' % ctx.backend2.server_config.port)
    time.sleep(3 * ping_delay)

    failed = 0
    good = 0
    for i in xrange(30):
        try:
            requests.get('http://localhost:%s/' % ctx.balancer.config.port)
            good += 1
        except:
            failed += 1
        time.sleep(0.1)
    assert failed > 0
    assert good > 0

    requests.get('http://localhost:%s/change_state' % ctx.backend2.server_config.port)
    time.sleep(3 * ping_delay)
    failed = 0
    good = 0
    for i in xrange(30):
        try:
            requests.get('http://localhost:%s/' % ctx.balancer.config.port)
            good += 1
        except:
            failed += 1
            pass
        time.sleep(0.1)
    assert failed == 0
    assert good > 0


def test_config_parse_error(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend1')), name='backend1')
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'RS-Weight': '10'}, data='backend2')), name='backend2')

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(configs.ActiveConfig(
            backend_port1=ctx.backend1.server_config.port,
            backend_port2=ctx.backend2.server_config.port,
            request=http.request.get(), workers=5, use_backend_weight=True))


def test_alive_flag(ctx):
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')

    ctx.start_balancer(configs.ActiveEasyConfigWithPinger(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        workers=5))

    time.sleep(2)

    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)

    assert 'active-default-max_delay_of_pings_hgram' in unistat
    assert unistat['active-default-all_alive_flag_ammv'] > 0

    ctx.backend1._finish()
    ctx.backend2._finish()
    time.sleep(5)

    assert ctx.get_unistat(port=ctx.balancer.config.stats_port)['active-default-all_alive_flag_ammv'] == 0


def test_tcp_check_and_http_check(ctx):
    ctx.start_fake_backend(name='backend1')
    ctx.start_fake_backend(name='backend2')
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(configs.ActiveEasyConfig(
            backend_port1=ctx.backend1.server_config.port,
            backend_port2=ctx.backend2.server_config.port,
            request='GET / HTTP/1.1\r\n\r\n', tcp_check=True
        ))


def test_tcp_check(ctx):
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_fake_backend(name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
        backend_port1=ctx.backend1.server_config.port,
        backend_port2=ctx.backend2.server_config.port,
        request=None, tcp_check=True
    ))

    time.sleep(2)

    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
    assert unistat['active-default-all_alive_flag_ammv'] == 1
    asserts.status(ctx.perform_request(http.request.get()), 200)


@pytest.mark.parametrize('connection_manager_required', [True, False])
def test_skip_keepalive_in_ping(ctx, connection_manager_required):
    """
    BALANCER-2874 ping requests should not use keepalive connections
    """
    backend1 = ctx.start_backend(SimpleConfig(), name='backend1')
    backend2 = ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(configs.ActiveEasyConfig(
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
