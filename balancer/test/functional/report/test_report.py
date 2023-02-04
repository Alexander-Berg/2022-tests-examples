# -*- coding: utf-8 -*-
import socket
import time
import pytest

from collections import defaultdict
from random import shuffle

from configs import \
    ReportConfig, ReportSharedConfig, ReportAntirobotConfig, ReportSimpleConfig, \
    ReportSSLConfig, ReportNessConfig, ReportMultipleUuid, ReportMultipleBadReferMulti, \
    ReportMultipleBadReferNonUnique, ReportMultipleConflictingRanges

from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef.handler.server.http import \
    SimpleConfig, DummyConfig, ChunkedConfig, ContinueConfig
from balancer.test.util import sync
from balancer.test.util.process import BalancerStartError
from balancer.test.util.proto.handler.server import State
from balancer.test.util.predef import http
from balancer.test.util.proto.handler.server.http import HTTPConfig, PreparseHandler, StaticResponseConfig, StaticResponseHandler
from balancer.test.util.balancer import asserts
from balancer.test.util.backend import syn_blackhole
from balancer.test.util.proto.http.stream import HTTPReaderException


class PlanHandler(PreparseHandler):
    def handle_parsed_request(self, raw_request, stream):
        response, timeout = self.state.get_next_item()
        stream.write_response_line(response.response_line)
        if self.config.sleep_after_headers_sent:  # TODO: flag is upchk, make two timeouts
            stream.write_headers(response.headers)
            time.sleep(timeout)
        else:
            time.sleep(timeout)
            stream.write_headers(response.headers)
        stream.write_data(response.data)
        # TODO: finish connection according to rfc


class PlanState(State):
    def __init__(self, config):
        super(PlanState, self).__init__(config)
        self.__plan = sync.Queue(config.queue_timeout)
        for item in config.plan:
            if isinstance(item, tuple) or isinstance(item, list):
                response, timeout = item
            else:
                response = item
                timeout = 0
            if not response.is_raw_message():
                response = response.to_raw_response()
            self.__plan.put((response, timeout))

    def get_next_item(self):
        if not self.__plan.empty():
            return self.__plan.get()  # FIXME: need lock here
        else:
            return self.config.default_response, self.config.default_timeout


class PlanConfig(HTTPConfig):
    HANDLER_TYPE = PlanHandler
    STATE_TYPE = PlanState

    def __init__(self, plan, default_response=None, default_timeout=0, sleep_after_headers=False):
        super(PlanConfig, self).__init__()
        if default_response is None:
            default_response = http.response.raw_ok()
        self.default_response = default_response
        self.default_timeout = default_timeout
        self.sleep_after_headers_sent = sleep_after_headers
        self.plan = plan


X2XX_SELECTOR = 'x[@sc="2xx"][@ir="u"][@sr="u"]'
TIME_HIST = 'time/d[@lo][@hi]'
TIME_HIST_LO = 'time/d[@lo]'

TRUE_VALUES = ['true', 'TRUE', 'tRUe', 'yes', 'da', '1']
FALSE_VALUES = ['no', 'False', '0', 'hello']
MAP_VALUES = {
    'y': 'true',
    'n': 'false',
}

EPS = 1e-3


def gen_report_path(uuid, signal):
    return './report[@key="{}"]/{}'.format(uuid, signal)


def find_range(elems, lo=None, hi=None):
    def check_element(elem, attr_name, expected_value):
        if expected_value is None:
            return attr_name not in elem.attrib
        else:
            if attr_name not in elem.attrib:
                return False
            attr_value = float(elem.attrib[attr_name][:-1])
            return abs(attr_value - expected_value) < EPS

    for elem in elems:
        if check_element(elem, 'lo', lo) and check_element(elem, 'hi', hi):
            return elem


def sum_unistat_range(hgram, lo, hi=None):
    res = 0
    summing = False
    for elem in hgram:
        if hi is not None and elem[0] >= hi:
            break

        if summing or elem[0] <= lo:
            res += elem[1]
            summing = True

    return res


def sum_solomon_range(hist, lo, hi=None):
    res = 0
    summing = False

    if lo <= 0:
        summing = True

    for i in xrange(len(hist['bounds'])):
        if summing and (hi is None or hist['bounds'][i] <= hi):
            res += hist['buckets'][i]

        if hist['bounds'][i] >= lo:
            summing = True

    if hi is None and 'inf' in hist:
        res += hist['inf']

    return res


def base_test(
        ctx, plan,
        input_size_ranges=None, output_size_ranges=None, backend_time_ranges=None,
        legacy_ranges="10s", client_fail_time_ranges=None
):
    def build_item(item):
        key, timeout = item
        return http.response.ok(headers={'X-Report-Y': key}), timeout

    backend_plan = [build_item(item) for item in plan]
    backend = ctx.start_backend(PlanConfig(backend_plan))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        backend_timeout='10s',
        legacy_ranges=legacy_ranges,
        backend_time_ranges=backend_time_ranges,
        client_fail_time_ranges=client_fail_time_ranges,
        input_size_ranges=input_size_ranges,
        output_size_ranges=output_size_ranges
    ))

    for path, _ in plan:
        request = http.request.get(path=path)
        response = ctx.perform_request(request)
        asserts.status(response, 200)


def base_no_time_test(ctx, request_paths):
    base_test(ctx, [(request_path, 0) for request_path in request_paths])


def test_keys(ctx):
    """
    Для каждого нового ключа в статистике появляется <report> с атрибутом key, равным этому ключу
    """
    request_paths = ['/led/', '/zeppelin/']
    base_no_time_test(ctx, request_paths)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            solomon = ctx.get_solomon()
            time.sleep(3)
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)

            for request_path in request_paths:
                p = request_path.strip('/')
                if request_path == '/led/':
                    assert unistat['a=b;c=d;report-' + p + '-outgoing_2xx_summ'] == 1
                else:
                    assert unistat['report-' + p + '-outgoing_2xx_summ'] == 1
                assert solomon['report-' + p + '-outgoing_2xx']['value'] == 1
                assert ('[report u:' + p + ' ') in accesslog


def test_slow_backend(ctx):
    """
    Если backend быстро вернул заголовки и долго отправляет тело сообшения,
    то в статистике должен попасть во временной промежуток, соответствующий полному времени ответа
    """
    plan = [(http.response.ok(data='data'), 1.5)]
    backend = ctx.start_backend(PlanConfig(plan, sleep_after_headers=True))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s',
        backend_timeout='10s'
    ))

    request = http.request.get()
    response = ctx.perform_request(request)
    asserts.status(response, 200)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert sum_unistat_range(unistat['report-default-processing_time_hgram'], 1) == 1

            solomon = ctx.get_solomon()
            assert sum_solomon_range(solomon['report-default-processing_time']['hist'], 1) == 1


def test_conn_refused(ctx):
    backend = ctx.start_fake_backend()
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='127.0.0.1',
        legacy_ranges='1s,10s,500s'
    ))
    request = http.request.get()

    for i in range(10):
        ctx.perform_request_xfail(request)

        stats = ctx.get_unistat()
        assert stats['report-default-backend_fail_summ'] == i + 1
        assert stats['report-default-conn_fail_summ'] == i + 1
        assert stats['report-default-conn_refused_summ'] == i + 1
        assert stats['report-default-backend_attempt_summ'] == i + 1
        assert stats['report-default-requests_summ'] == i + 1

        stats = ctx.get_solomon()
        assert stats['report-default-backend_fail']['value'] == i + 1
        assert stats['report-default-conn_fail']['value'] == i + 1
        assert stats['report-default-conn_refused']['value'] == i + 1
        assert stats['report-default-backend_attempt']['value'] == i + 1
        assert stats['report-default-requests']['value'] == i + 1

    stats = ctx.get_unistat()
    assert stats['report-default-client_fail_summ'] == 0
    assert stats['report-default-backend_error_summ'] == 0

    stats = ctx.get_solomon()
    assert stats['report-default-client_fail']['value'] == 0
    assert stats['report-default-backend_error']['value'] == 0

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert accesslog.count('backend_fail') == 10
    assert accesslog.count('conn_fail') == 10
    assert accesslog.count('client_fail') == 0
    assert accesslog.count('other_fail') == 0


@pytest.mark.parametrize('connection_manager_required', [True, False])
def test_backend_keepalive_reused(ctx, connection_manager_required):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        legacy_ranges='1s,10s,500s',
        keepalive_count=1,
        connection_manager_required=connection_manager_required,
    ))
    request = http.request.get()

    n = 10
    for _ in range(n):
        ctx.perform_request(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_keepalive_reused_summ'] == n - 1


def resolve_localhost():
    return socket.getaddrinfo("localhost", None, 0, 0, socket.IPPROTO_TCP)


def test_conn_refused_localhost(ctx):
    backend = ctx.start_fake_backend()
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        legacy_ranges='1s,10s,500s'
    ))
    request = http.request.get()
    localhost_ips = resolve_localhost()
    for i in range(10):
        ctx.perform_request_xfail(request)

        stats = ctx.get_unistat()
        assert stats['report-default-backend_fail_summ'] == i + 1
        assert stats['report-default-conn_fail_summ'] == i + 1
        assert stats['report-default-conn_refused_summ'] == len(localhost_ips) * (i + 1)
        assert stats['report-default-backend_attempt_summ'] == i + 1
        assert stats['report-default-requests_summ'] == i + 1

        stats = ctx.get_solomon()
        assert stats['report-default-backend_fail']['value'] == i + 1
        assert stats['report-default-conn_fail']['value'] == i + 1
        assert stats['report-default-conn_refused']['value'] == len(localhost_ips) * (i + 1)
        assert stats['report-default-backend_attempt']['value'] == i + 1
        assert stats['report-default-requests']['value'] == i + 1

    stats = ctx.get_unistat()
    assert stats['report-default-client_fail_summ'] == 0
    assert stats['report-default-backend_error_summ'] == 0

    stats = ctx.get_solomon()
    assert stats['report-default-client_fail']['value'] == 0
    assert stats['report-default-backend_error']['value'] == 0

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert accesslog.count('backend_fail') == 10
    assert accesslog.count('conn_fail') == 10
    assert accesslog.count('client_fail') == 0
    assert accesslog.count('other_fail') == 0


def test_conn_timeout(ctx):
    connect_timeout = 0.05
    backend = ctx.start_backend(SimpleConfig(), listen_queue=0)

    hole_host, hole_port = syn_blackhole.create_syn_blackhole(backend.server_config.ports, timeout=connect_timeout)
    if not hole_host:
        pytest.skip("unable to emulate conn timeout")  # raises Skipped

    ctx.start_balancer(ReportSimpleConfig(
        hole_port,
        host=hole_host,
        legacy_ranges='1s,10s,500s',
        connect_timeout="{}s".format(connect_timeout)
    ))
    request = http.request.get()

    for i in range(10):
        ctx.perform_request_xfail(request)

        stats = ctx.get_unistat()
        assert stats['report-default-backend_fail_summ'] == i + 1
        assert stats['report-default-conn_fail_summ'] == i + 1
        assert stats['report-default-conn_timeout_summ'] == i + 1
        assert stats['report-default-backend_attempt_summ'] == i + 1
        assert stats['report-default-requests_summ'] == i + 1

        stats = ctx.get_solomon()
        assert stats['report-default-backend_fail']['value'] == i + 1
        assert stats['report-default-conn_fail']['value'] == i + 1
        assert stats['report-default-conn_timeout']['value'] == i + 1
        assert stats['report-default-backend_attempt']['value'] == i + 1
        assert stats['report-default-requests']['value'] == i + 1

    stats = ctx.get_unistat()
    assert stats['report-default-client_fail_summ'] == 0
    assert stats['report-default-backend_error_summ'] == 0

    stats = ctx.get_solomon()
    assert stats['report-default-client_fail']['value'] == 0
    assert stats['report-default-backend_error']['value'] == 0

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert accesslog.count('backend_fail') == 10
    assert accesslog.count('conn_fail') == 10
    assert accesslog.count('client_fail') == 0
    assert accesslog.count('other_fail') == 0


def test_backend_timeout(ctx):
    backend = ctx.start_backend(DummyConfig(3))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        backend_timeout='0.5s',
        host='127.0.0.1'))
    request = http.request.get()

    ctx.perform_request_xfail(request)

    stats = ctx.get_unistat()
    assert stats['report-default-conn_fail_summ'] == 0
    assert stats['report-default-backend_error_summ'] == 1
    assert stats['report-default-backend_attempt_summ'] == 1
    assert stats['report-default-requests_summ'] == 1
    assert stats['report-default-backend_fail_summ'] == 1
    assert stats['report-default-client_fail_summ'] == 0
    assert stats['report-default-other_fail_summ'] == 0
    assert stats['report-default-backend_timeout_summ'] == 1

    stats = ctx.get_solomon()
    assert stats['report-default-conn_fail']['value'] == 0
    assert stats['report-default-backend_error']['value'] == 1
    assert stats['report-default-backend_attempt']['value'] == 1
    assert stats['report-default-requests']['value'] == 1
    assert stats['report-default-backend_fail']['value'] == 1
    assert stats['report-default-client_fail']['value'] == 0
    assert stats['report-default-other_fail']['value'] == 0
    assert stats['report-default-backend_timeout']['value'] == 1

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert accesslog.count('backend_fail') == 1
    assert accesslog.count('conn_fail') == 0
    assert accesslog.count('client_fail') == 0
    assert accesslog.count('other_fail') == 0


@pytest.mark.parametrize(
    ['response', 'headers', 'custom_matchers', 'xml_counters'],
    [
        (
            http.response.custom(101, 'Switching Protocols', headers={"upgrade": "xxx"}),
            {"connection": "upgrade", "upgrade": "xxx"},
            ['default_upgrade', 'default_upgrade_xxx'],
            []
        ),
        (
            http.response.raw_custom(101, 'Switching Protocols', headers={"upgrade": "xxx", "connection": "close"}),
            {"connection": "upgrade", "upgrade": "xxx"},
            ['default_upgrade', 'default_upgrade_xxx'],
            []
        ),
        (
            http.response.custom(150, 'Random'),
            None, [], ['sc_1xx']
        ),
        (
            http.response.ok(),
            None, [], ['sc_2xx']
        ),
        (
            http.response.no_content(),
            None, [], ['sc_2xx']
        ),
        (
            http.response.partial_content(),
            None, [], ['sc_2xx']
        ),
        (
            http.response.not_modified(),
            None, [], ['sc_3xx']
        ),
        (
            http.response.not_found(),
            None, [], ['sc_4xx']
        ),
        (
            http.response.not_allowed(),
            None, [], ['sc_4xx']
        ),
        (
            http.response.service_unavailable(),
            None, [], ['sc_5xx', 'sc_503']
        ),
        (
            http.response.gateway_timeout(),
            None, [], ['sc_5xx']
        ),
    ],
    ids=[
        'switching_protocols',
        'switching_protocols_close',
        'random',
        'ok',
        'no_content',
        'partial_content',
        'not_modified',
        'not_found',
        'not_allowed',
        'service_unavailable',
        'gateway_timeout',
    ]
)
def test_status_codes(ctx, response, headers, custom_matchers, xml_counters):
    backend = ctx.start_backend(SimpleConfig(response))
    status = response.response_line.status
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        outgoing_codes=str(status) + ",599" if status != 101 else None
    ))
    request = http.request.get(headers=headers)
    matchers = ['default'] + custom_matchers
    counters = ["outgoing_{}xx".format(int(status / 100)), "outgoing_{}".format(status)]
    for i in range(10):
        ctx.perform_request(request)
        unistat = ctx.get_unistat()
        solomon = ctx.get_solomon()
        for matcher in matchers:
            for counter in counters:
                path = 'report-{}-{}_summ'.format(matcher, counter)
                assert unistat[path] == i + 1, path
            for counter in counters:
                path = 'report-{}-{}'.format(matcher, counter)
                assert solomon[path]['value'] == i + 1, path


def _parse_lo_hi(d_elem):
    lo = float(d_elem.attrib["lo"].rstrip("s"))
    hi = float(d_elem.attrib["hi"].rstrip("s"))
    return lo, hi


def check_unistat_hgram(unistat, name, intervals=None):
    if name not in unistat:
        raise Exception("histogram {} not found in unistat answer".format(name))

    hgram = unistat[name]

    if intervals is not None:
        intervals = map(int, intervals.replace("s", "").split(","))
        assert map(lambda x: x[0], hgram)[1:] == intervals, "intervals differs for histogram '{}'".format(name)

    prev = None
    for elem in hgram:
        if prev is None:
            prev = elem
        else:
            assert elem[0] > prev[0]


def check_solomon_hist(unistat_hgram, solomon_hist, name):
    assert list(map(lambda x: x[0], unistat_hgram)[1:]) == solomon_hist['bounds'],\
        'intervals differ for unistat and solomon histogram differ for {}'.format(name)

    unistat_values = list(map(lambda x: x[1], unistat_hgram))
    solomon_values = solomon_hist['buckets']
    if 'inf' in solomon_hist:
        solomon_values.append(solomon_hist['inf'])

    assert unistat_values == solomon_values, \
        'values differ for unistat and solomon histogram differ for {}'.format(name)


def test_all_default_ranges_and_default(ctx):
    backend = ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ReportSimpleConfig(
            backend.server_config.port,
            all_default_ranges=True,
            input_size_ranges="default",
        ))


@pytest.mark.parametrize(
    'legacy_ranges',
    [None, "1s,10s,60s"],
    ids=['nil', 'ranges']
)
@pytest.mark.parametrize(
    'backend_time_ranges',
    [None, "1s,10s,60s"],
    ids=['nil', 'backend_time_ranges']
)
@pytest.mark.parametrize(
    'client_fail_time_ranges',
    [None, "1s,10s,60s"],
    ids=['nil', 'client_fail_time_ranges']
)
@pytest.mark.parametrize(
    'input_size_ranges',
    [None, "1,5,10,500,1000"],
    ids=['nil', 'input_size_ranges']
)
@pytest.mark.parametrize(
    'output_size_ranges',
    [None, "1,5,10,500,1000"],
    ids=['nil', 'output_size_ranges']
)
def test_ranges_combinations(ctx, legacy_ranges, backend_time_ranges, client_fail_time_ranges,
                             input_size_ranges, output_size_ranges):
    """
    BALANCER-1358
    BALANCER-1640
    Ренжи правильно считаются независимо друг от друга, в значениях правильное число запросов, правильный диапазон.
    Балансер считает ренжи независимо от отключения старых ranges, если хотя бы какие-то ренжи включены.
    """
    if not any([legacy_ranges, backend_time_ranges, client_fail_time_ranges, input_size_ranges, output_size_ranges]):
        return

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges=legacy_ranges,
        backend_time_ranges=backend_time_ranges,
        client_fail_time_ranges=client_fail_time_ranges,
        input_size_ranges=input_size_ranges,
        output_size_ranges=output_size_ranges
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        request = http.request.get().to_raw_request()
        stream = conn.create_stream()
        stream.write_request(request)
        stream.write_request(request)

    time.sleep(1)
    unistat = ctx.get_unistat()
    solomon = ctx.get_solomon()

    if backend_time_ranges:
        check_unistat_hgram(unistat, 'report-default-backend_time_hgram', backend_time_ranges)

        name = 'report-default-backend_time'
        check_solomon_hist(unistat[name + '_hgram'], solomon[name]['hist'], name)

    if client_fail_time_ranges:
        check_unistat_hgram(unistat, 'report-default-client_fail_time_hgram', client_fail_time_ranges)

        name = 'report-default-client_fail_time'
        check_solomon_hist(unistat[name + '_hgram'], solomon[name]['hist'], name)

    if input_size_ranges:
        check_unistat_hgram(unistat, 'report-default-input_size_hgram', input_size_ranges)

        name = 'report-default-input_size'
        check_solomon_hist(unistat[name + '_hgram'], solomon[name]['hist'], name)

    if output_size_ranges:
        check_unistat_hgram(unistat, 'report-default-output_size_hgram', output_size_ranges)

        name = 'report-default-output_size'
        check_solomon_hist(unistat[name + '_hgram'], solomon[name]['hist'], name)


def check_client_fail(ctx):
    unistat = ctx.get_unistat()
    solomon = ctx.get_solomon()

    check_unistat_hgram(unistat, 'report-default-client_fail_time_hgram')

    name = 'report-default-client_fail_time'
    check_solomon_hist(unistat[name + '_hgram'], solomon[name]['hist'], name)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1

    solomon = ctx.get_solomon()
    assert solomon['report-default-client_fail']['value'] == 1

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert accesslog.count('backend_fail') == 0
    assert accesslog.count('conn_fail') == 0
    assert accesslog.count('client_fail') == 1
    assert accesslog.count('other_fail') == 0


def test_client_write_error(ctx):
    backend = ctx.start_backend(SimpleConfig(
        response=http.response.ok(data="A" * 2 ** 25)
    ))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges="1s,10s,60s",
        client_fail_time_ranges="1s,10s,60s",
        client_write_timeout="1s",
        backend_timeout="100s",
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()

        stream.write_request_line("GET / HTTP/1.1")
        stream.end_headers()

        stream.read_response_line()
        stream.read_headers()
        time.sleep(1.5)

        with pytest.raises(HTTPReaderException):
            stream.read_data()

    check_client_fail(ctx)


def test_client_flush_error(ctx):
    backend = ctx.start_backend(SimpleConfig(
        response=http.response.ok(data="A" * 2 ** 25)
    ))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges="1s,10s,60s",
        client_fail_time_ranges="10s,60s",
        backend_timeout="2s",
        buffer=2 ** 25 + 2 ** 14
    ))

    with ctx.create_http_connection(timeout=600) as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get().to_raw_request())

    time.sleep(1)
    check_client_fail(ctx)


def test_map(ctx):
    """
    BALANCER-1597
    Помимо общей агрегации добавляется агрегация по доменам
    """
    def gen_ru_host(i):
        hosts = [
            'www.yandex.ru',
            'www.yandex.by',
            'www.yandex.ua',
            'www.yandex.kz',
            'm.yandex.ru',
            'm.yandex.by',
            'm.yandex.ua',
            'm.yandex.kz',
            'yandex.ru',
            'yandex.by',
            'yandex.ua',
            'yandex.kz',
        ]
        return hosts[i % len(hosts)]

    def get_request(host):
        return http.request.get(headers={'Host': host})

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
    ))

    to_ru = 7
    to_com = 5
    to_com_tr = 3
    to_any = 1

    requests = \
        [get_request(gen_ru_host(i)) for i in xrange(to_ru)] + \
        [get_request('www.yandex.com')] * to_com + \
        [get_request('www.yandex.com.tr')] * to_com_tr + \
        [get_request('something')] * to_any
    shuffle(requests)

    for request in requests:
        ctx.perform_request(request)

    stats = ctx.get_unistat()
    assert stats['report-default_ru-succ_summ'] == to_ru
    assert stats['report-default_com-succ_summ'] == to_com
    assert stats['report-default_com_tr-succ_summ'] == to_com_tr
    assert stats['report-default-succ_summ'] == to_ru + to_com + to_com_tr + to_any

    stats = ctx.get_solomon()
    assert stats['report-default_ru-succ']['value'] == to_ru
    assert stats['report-default_com-succ']['value'] == to_com
    assert stats['report-default_com_tr-succ']['value'] == to_com_tr
    assert stats['report-default-succ']['value'] == to_ru + to_com + to_com_tr + to_any


def test_matcher_map_path(ctx):
    """
    BALANCER-1597
    Помимо общей агрегации добавляется агрегация по путям
    """
    def get_request(path, **headers):
        return http.request.get(path=path, headers=headers)

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
    ))

    requests = \
        [
            get_request('/foo', host="www.yandex.ru"),
            get_request('/foo', host='yandex.com.tr'),
            get_request('/foo;', host="www.yandex.ru"),
            get_request('/foo?', host="www.yandex.ru"),
            get_request('/foobar', host="www.yandex.ru"),
            get_request('/foo/bar', host="www.yandex.ru")
        ] + \
        [get_request('/bar', host="www.yandex.ru", connection="upgrade", upgrade="websocket")] + \
        [get_request('/baz', host="www.yandex.ru", connection="upgrade", upgrade="XXX")] + \
        [get_request('/baz', host="www.yandex.ru", connection="upgrade", upgrade="xxx")]
    shuffle(requests)

    for request in requests:
        ctx.perform_request(request)

    time.sleep(3)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    unistat = ctx.get_unistat()
    solomon = ctx.get_solomon()
    for uuid, val in [
        ('default', 9),
        ('default_ru', 8),
        ('default_com_tr', 1),
        ('default_foo', 6),
        ('default_foobar', 1),
        ('default_bar', 1),
        ('default_upgrade', 3),
        ('default_upgrade_xxx', 2)
    ]:
        assert unistat['report-{}-succ_summ'.format(uuid)] == val
        assert solomon['report-{}-succ'.format(uuid)]['value'] == val
        assert accesslog.count("u:{} ".format(uuid)) == val


@pytest.mark.parametrize(
    ['status', 'tag'],
    [
        (100, '1xx'),
        (202, '2xx'),
        (302, '3xx'),
        (403, '4xx'),
    ],
    ids=[
        '1xx',
        '2xx',
        '3xx',
        '4xx',
    ]
)
def test_status(ctx, status, tag):
    """
    BALANCER-27
    Базовый тест на распределение ответов по группам в соответствии с кодом ответа
    """
    count = 5

    request = http.request.get()
    plan = [(http.response.some(status, 'reason', data='data'), 0)] * count

    backend = ctx.start_backend(PlanConfig(plan))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='10s',
        backend_timeout='10s'
    ))

    for _ in range(count):
        response = ctx.perform_request(request)
        asserts.status(response, status)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-sc_' + tag + '_summ'] == count


def test_100_continue(ctx):
    """
    BALANCER-1396
    Считаем не только 1xx ответы, но и последущие тоже.
    """
    resp_data = 'A' * 20
    backend = ctx.start_backend(ContinueConfig(
        continue_response=http.response.some(status=100, reason='Continue', data=None),
        response=http.response.ok(data=resp_data)
    ))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='10s',
        backend_timeout='10s'
    ))

    request = http.request.get(headers={'Expect': '100-continue'}, data=['12345']).to_raw_request()
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        resp1 = stream.read_next_response()
        stream.write_data(request.data)
        resp2 = stream.read_next_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp1, 100)
    asserts.status(resp2, 200)
    asserts.content(resp2, resp_data)
    asserts.header_value(req, 'expect', '100-continue')
    asserts.content(req, request.data.content)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-sc_1xx_summ'] == 1
            assert unistat['report-default-sc_2xx_summ'] == 1


def test_error_status(ctx):
    """
    BALANCER-786
    Error while getting response is reported in sc="err" tag
    """
    backend = ctx.start_backend(DummyConfig())
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='10s',
        backend_timeout='100ms'
    ))

    count = 5
    request = http.request.get()
    for _ in range(count):
        ctx.perform_request_xfail(request)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-fail_summ'] == count


def base_keepalive_test(ctx, request, path):
    count = 5

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='10s',
        backend_timeout='10s'
    ))

    for _ in range(count):
        response = ctx.perform_request(request)
        asserts.status(response, 200)

    for run in Multirun():
        with run:
            stats = ctx.get_unistat()
            assert stats['report-default-' + path + '_summ'] == count

            stats = ctx.get_solomon()
            assert stats['report-default-' + path]['value'] == count


@pytest.mark.parametrize(
    'req',
    [
        http.request.raw_get(),
        http.request.raw_get(headers={'Connection': 'Keep-Alive'}),
        http.request.raw_get(version='HTTP/1.0', headers={'Connection': 'Keep-Alive'}),
    ],
    ids=[
        'http11',
        'http11_keepalive',
        'http10_keepalive',
    ]
)
def test_keepalive(ctx, req):
    """
    Keepalive-запросы должны учитываться в статистике в report/report/ka
    """
    base_keepalive_test(ctx, req, 'ka')


@pytest.mark.parametrize(
    'req',
    [
        http.request.raw_get(headers={'Connection': 'Close'}),
        http.request.raw_get(version='HTTP/1.0'),
        http.request.raw_get(version='HTTP/1.0', headers={'Connection': 'Close'}),
    ],
    ids=[
        'http11_close',
        'http10',
        'http10_close',
    ]
)
def test_nokeepalive(ctx, req):
    """
    Nokeepalive-запросы должны учитываться в статистике в report/report/nka
    """
    base_keepalive_test(ctx, req, 'nka')


def base_reuse_test(ctx, request, use_keepalive, requests_count, paths_and_counts):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='10s',
        backend_timeout='10s'
    ))

    if use_keepalive:
        with ctx.create_http_connection() as conn:
            for _ in range(requests_count):
                response = conn.perform_request(request)
                asserts.status(response, 200)
    else:
        for _ in range(requests_count):
            response = ctx.perform_request(request)
            asserts.status(response, 200)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            solomon = ctx.get_solomon()
            for path, count in paths_and_counts:
                assert unistat['report-default-' + path + '_summ'] == count
                assert solomon['report-default-' + path]['value'] == count


@pytest.mark.parametrize(
    'req',
    [
        http.request.raw_get(),
        http.request.raw_get(headers={'Connection': 'Keep-Alive'}),
        http.request.raw_get(version='HTTP/1.0', headers={'Connection': 'Keep-Alive'}),
    ],
    ids=[
        'http11',
        'http11_keepalive',
        'http10_keepalive',
    ]
)
def test_reuse(ctx, req):
    """
    BALANCER-974
    Переиспользованые запросы должны учитываться в статистике в report/report/reused,
    при этом первый запрос не переиспользован и должен попасть в report/report/nreused.
    """
    count = 5
    base_reuse_test(ctx, req, True, count, [('reused', count - 1), ('nreused', 1)])


@pytest.mark.parametrize(
    'req',
    [
        http.request.raw_get(headers={'Connection': 'Close'}),
        http.request.raw_get(version='HTTP/1.0'),
        http.request.raw_get(version='HTTP/1.0', headers={'Connection': 'Close'}),
    ],
    ids=[
        'http11_close',
        'http10',
        'http10_close',
    ]
)
def test_no_reuse(ctx, req):
    """
    BALANCER-974
    Переиспользованые запросы должны учитываться в статистике в report/report/reused,
    при этом первый запрос не переиспользован и должен попасть в report/report/nreused.
    """
    count = 5
    base_reuse_test(ctx, req, False, count, [('nreused', count)])


def test_no_ranges(ctx):
    """
    Если в модуле report не указан параметр ranges,
    то в нем все равно собирается статистика
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        backend_timeout='10s',
    ))

    ctx.perform_request(http.request.get())

    unistat = ctx.get_unistat()
    assert 'report-default-succ_summ' in unistat
    assert 'report-default-inprog_ammv' in unistat
    assert 'report-default-fail_summ' in unistat
    assert 'report-default-backend_time_hgram' not in unistat
    assert 'report-default-output_size_hgram' not in unistat
    assert 'report-default-input_size_hgram' not in unistat
    assert 'report-default-client_fail_time_hgram' not in unistat

    solomon = ctx.get_solomon()
    assert 'report-default-succ' in solomon
    assert 'report-default-inprog' in solomon
    assert 'report-default-fail' in solomon
    assert 'report-default-backend_time' not in solomon
    assert 'report-default-output_size' not in solomon
    assert 'report-default-input_size' not in solomon
    assert 'report-default-client_fail_time' not in solomon


def test_single_refer(ctx):
    """
    Если ответ проходит через report, в котором в refers указан только один uuid,
    то этот ответ должен быть учтен в report с этим uuid
    """
    backend = ctx.start_backend(PlanConfig([], default_response=http.response.ok(data='data')))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s',
        backend_timeout='10s'
    ))
    ctx.perform_request(http.request.get(path='/storage/'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-refers-succ_summ'] == 1

            solomon = ctx.get_solomon()
            assert solomon['report-refers-succ']['value'] == 1


def test_multiple_refers(ctx):
    """
    Если ответ проходит через report, в котором в refers указано несколько uuid,
    то этот ответ должен быть учтен во всех report с этими uuid
    """
    backend = ctx.start_backend(PlanConfig([], default_response=http.response.ok(data='data')))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s',
        backend_timeout='10s'
    ))

    ctx.perform_request(http.request.get(path='/multirefers/'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-refers-succ_summ'] == 1
            assert unistat['report-default-succ_summ'] == 1

            solomon = ctx.get_solomon()
            assert solomon['report-refers-succ']['value'] == 1
            assert solomon['report-default-succ']['value'] == 1


def test_self_refer(ctx):
    """
    Если refer в модуле report указывает на этот же модуль,
    то ответ должен учитываться только один раз
    TODO: make base for previous three tests
    """
    backend = ctx.start_backend(PlanConfig([], default_response=http.response.ok(data='data')))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s',
        backend_timeout='10s'
    ))

    ctx.perform_request(http.request.get(path='/selfrefer/'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-selfrefer-succ_summ'] == 1

            solomon = ctx.get_solomon()
            assert solomon['report-selfrefer-succ']['value'] == 1


def test_refer_to_refer(ctx):
    """
    Если первый модуль report указывает на второй, а второй на третий,
    то при прохождении ответа через первый модуль, статистика в третьем не должна измениться
    """
    backend = ctx.start_backend(PlanConfig([], default_response=http.response.ok(data='data')))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s',
        backend_timeout='10s'
    ))

    ctx.perform_request(http.request.get(path='/transitivity/'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-transitivity-succ_summ'] == 1
            assert unistat['report-transitivity_first-succ_summ'] == 1

            solomon = ctx.get_solomon()
            assert solomon['report-transitivity-succ']['value'] == 1
            assert solomon['report-transitivity_first-succ']['value'] == 1

    unistat = ctx.get_unistat()
    assert unistat['report-transitivity_second-succ_summ'] == 0

    solomon = ctx.get_solomon()
    assert solomon['report-transitivity_second-succ']['value'] == 0


def base_bad_backend_test(ctx, backend_port):
    ctx.start_balancer(ReportConfig(
        backend_port,
        legacy_ranges='1s',
        backend_timeout='1s'
    ))

    ctx.perform_request_xfail(http.request.get(path='/'))

    for run in Multirun():
        with run:
            stats = ctx.get_unistat()
            assert stats['report-default-fail_summ'] == 1

            stats = ctx.get_solomon()
            assert stats['report-default-fail']['value'] == 1


def test_timeout_backend(ctx):
    """
    Если backend успел за таймаут отправить заголовки, но не успел отправить тело сообщения,
    то информация о ключах не должна появиться в статистике
    В статистике должна появиться информация об ошибке
    """
    plan = [(http.response.ok(data='data'), 2.0)]
    backend = ctx.start_backend(PlanConfig(plan, sleep_after_headers=True))
    base_bad_backend_test(ctx, backend.server_config.port)


def test_no_backend(ctx):
    """
    Если на порту бэкенда никто не слушает, то в статистике должна появиться информация об ошибке
    """
    backend_port = ctx.manager.port.get_port()
    base_bad_backend_test(ctx, backend_port)


def test_close_backend(ctx):
    """
    Если бэкенд ничего не отвечает а просто закрывает соединение,
    то в статистике должна появиться информация об ошибке
    """
    backend = ctx.start_backend(DummyConfig())
    base_bad_backend_test(ctx, backend.server_config.port)


def test_multiple_reports_to_same_uuid(ctx):
    """
    Если ответ проходит через несколько модулей report, которые указывают на один и тот же uuid,
    то в report с этим uuid значение должно увеличиться для каждого из этих модулей
    TODO: оставить
    """
    ctx.start_balancer(ReportSharedConfig(first_refers='second', top_refers='second'))
    ctx.perform_request(http.request.get(path='/first'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-second-succ_summ'] == 2

            solomon = ctx.get_solomon()
            assert solomon['report-second-succ']['value'] == 2


def test_ranges_and_refers(ctx):
    """
    Если ответ проходит через два модуля report, и один из них указан у другого в refers,
    то в нем этот запрос должен быть учтен дважды
    TODO: оставить, skip
    """
    ctx.start_balancer(ReportSharedConfig(first_refers='top'))
    ctx.perform_request(http.request.get(path='/first'))

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-top-succ_summ'] == 2

            solomon = ctx.get_solomon()
            assert solomon['report-top-succ']['value'] == 2


def base_antirobot_forward_test(ctx, headers, ir, sr):
    checker = ctx.start_backend(SimpleConfig(response=http.response.ok(headers=headers)))
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportAntirobotConfig(checker.server_config.port, backend.server_config.port))

    ctx.perform_request(http.request.get())
    req = backend.state.get_request()
    asserts.headers_values(req, headers)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-2xx-' + ir + '-' + sr + '_hgram'][0][1] == 1


def base_unknown_antirobot_forward_test(ctx, header):
    checker = ctx.start_backend(SimpleConfig())
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportAntirobotConfig(checker.server_config.port, backend.server_config.port))

    ctx.perform_request(http.request.get())
    req = backend.state.get_request()
    asserts.no_header(req, header)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-2xx-u-u_hgram'][0][1] == 1


@pytest.mark.parametrize('sr_value', TRUE_VALUES)
def test_suspected_antirobot_forward(ctx, sr_value):
    """
    Если в модуле антиробот в запрос добавляется заголовок X-Yandex-Suspected-Robot: true/yes/da/1,
    то это должно отразиться в статистике.
    """
    base_antirobot_forward_test(ctx, {'X-Yandex-Suspected-Robot': sr_value}, 'u', 'y')


@pytest.mark.parametrize('sr_value', FALSE_VALUES)
def test_not_suspected_antirobot_forward(ctx, sr_value):
    """
    Если в модуле антиробот в запрос добавляется заголовок X-Yandex-Suspected-Robot со значением,
    отличным от true/yes/da/1, то это должно отразиться в статистике.
    """
    base_antirobot_forward_test(ctx, {'X-Yandex-Suspected-Robot': sr_value}, 'u', 'n')


def test_unknown_suspected_antirobot_forward(ctx):
    """
    Если в ответе антиробота отсутствует заголовок X-Yandex-Suspected-Robot,
    то это должно отразиться в статистике
    """
    base_unknown_antirobot_forward_test(ctx, 'X-Yandex-Suspected-Robot')


@pytest.mark.parametrize('ir_value', TRUE_VALUES)
def test_internal_antirobot_forward(ctx, ir_value):
    """
    Если в модуле антиробот в запрос добавляется заголовок X-Yandex-Internal-Request: true/yes/da/1,
    то это должно отразиться в статистике.
    """
    base_antirobot_forward_test(ctx, {'X-Yandex-Internal-Request': ir_value}, 'y', 'u')


@pytest.mark.parametrize('ir_value', FALSE_VALUES)
def test_not_internal_antirobot_forward(ctx, ir_value):
    """
    Если в модуле антиробот в запрос добавляется заголовок X-Yandex-Internal-Request со значением,
    отличным от true/yes/da/1, то это должно отразиться в статистике.
    """
    base_antirobot_forward_test(ctx, {'X-Yandex-Internal-Request': ir_value}, 'n', 'u')


def test_unknown_internal_antirobot_forward(ctx):
    """
    Если в ответе антиробота отсутствует заголовок X-Yandex-Internal-Request,
    то это должно отразиться в статистике
    """
    base_unknown_antirobot_forward_test(ctx, 'X-Yandex-Internal-Request')


@pytest.mark.parametrize(
    ['internal', 'suspected'],
    [('n', 'n'), ('n', 'y'), ('y', 'n'), ('y', 'y')],
    ids=[
        'not_internal,not_robot',
        'not_internal,robot',
        'internal,not_robot',
        'internal,robot',
    ]
)
def test_antirobot_forward(ctx, internal, suspected):
    """
    Проверка статистики при наличии обоих заголовков X-Yandex-Internal-Request и X-Yandex-Suspected-Robot
    в ответе антиробота
    """
    base_antirobot_forward_test(
        ctx,
        {
            'X-Yandex-Internal-Request': MAP_VALUES[internal],
            'X-Yandex-Suspected-Robot': MAP_VALUES[suspected],
        },
        internal, suspected
    )


def base_antirobot_request_test(ctx, headers, ir, sr, disable_sslness=None):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness
    ))

    ctx.perform_request(http.request.get(headers=headers))
    req = backend.state.get_request()
    asserts.headers_values(req, headers)

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-2xx-' + ir + '-' + sr + '_hgram'][0][1] == 1


def base_unknown_antirobot_request_test(ctx, header, disable_sslness=None):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness
    ))

    for run in Multirun():
        with run:
            ctx.perform_request(http.request.get())
            req = backend.state.get_request()
            asserts.no_header(req, header)

            unistat = ctx.get_unistat()
            assert unistat['report-default-2xx-u-u_hgram'][0][1] == 1


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize('sr_value', TRUE_VALUES)
def test_suspected_robot_request(ctx, sr_value, disable_sslness):
    """
    Если запрос клиента содержит заголовок X-Yandex-Suspected-Robot: true/yes/da/1,
    то это должно отразиться в статистике.
    """
    base_antirobot_request_test(ctx, {'X-Yandex-Suspected-Robot': sr_value}, 'u', 'y', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize('sr_value', FALSE_VALUES)
def test_not_suspected_robot_request(ctx, sr_value, disable_sslness):
    """
    Если запрос клиента содержит заголовок X-Yandex-Suspected-Robot со значением,
    отличным от true/yes/da/1, то это должно отразиться в статистике.
    """
    base_antirobot_request_test(ctx, {'X-Yandex-Suspected-Robot': sr_value}, 'u', 'n', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
def test_unknown_suspected_robot_request(ctx, disable_sslness):
    """
    Если в запросе клиента отсутствует заголовок X-Yandex-Suspected-Robot,
    то это должно отразиться в статистике
    """
    base_unknown_antirobot_request_test(ctx, 'X-Yandex-Suspected-Robot', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize('ir_value', TRUE_VALUES)
def test_internal_request(ctx, ir_value, disable_sslness):
    """
    Если запрос клиента содержит заголовок X-Yandex-Internal-Request: true/yes/da/1,
    то это должно отразиться в статистике.
    """
    base_antirobot_request_test(ctx, {'X-Yandex-Internal-Request': ir_value}, 'y', 'u', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize('ir_value', FALSE_VALUES)
def test_not_internal_request(ctx, ir_value, disable_sslness):
    """
    Если запрос клиента содержит заголовок X-Yandex-Internal-Request со значением,
    отличным от true/yes/da/1, то это должно отразиться в статистике.
    """
    base_antirobot_request_test(ctx, {'X-Yandex-Internal-Request': ir_value}, 'n', 'u', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
def test_unknown_internal_request(ctx, disable_sslness):
    """
    Если в запросе клиента отсутствует заголовок X-Yandex-Internal-Request,
    то это должно отразиться в статистике
    """
    base_unknown_antirobot_request_test(ctx, 'X-Yandex-Internal-Request', disable_sslness=disable_sslness)


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    ['internal', 'suspected'],
    [('n', 'n'), ('n', 'y'), ('y', 'n'), ('y', 'y')],
    ids=[
        'not_internal,not_robot',
        'not_internal,robot',
        'internal,not_robot',
        'internal,robot',
    ]
)
def test_client_request(ctx, internal, suspected, disable_sslness):
    """
    Проверка статистики при наличии обоих заголовков X-Yandex-Internal-Request и X-Yandex-Suspected-Robot
    в запросе клиента
    """
    base_antirobot_request_test(
        ctx,
        {
            'X-Yandex-Internal-Request': MAP_VALUES[internal],
            'X-Yandex-Suspected-Robot': MAP_VALUES[suspected],
        },
        internal, suspected, disable_sslness=disable_sslness
    )


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_inprog_ok(ctx, disable_sslness, disable_robotness):
    """
    В статистике inprog должен появляться после того, как клиент отправил стартовую строку и заголовки и исчезать после того,
    как бэкенд вернул ответ.
    """
    chunk_timeout = 2
    request = http.request.post(data=['led', 'zeppelin']).to_raw_request()
    backend = ctx.start_backend(ChunkedConfig(
        response=http.response.ok(data=['pink', 'floyd']),
        chunk_timeout=chunk_timeout
    ))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        before_unistat = ctx.get_unistat()
        before_solomon = ctx.get_solomon()

        stream.write_headers(request.headers)
        time.sleep(0.5)

        inprog_start_unistat = ctx.get_unistat()
        inprog_start_solomon = ctx.get_solomon()

        stream.write_data(request.data)
        stream.read_response_line()
        stream.read_headers()
        time.sleep(chunk_timeout - 1)

        inprog_fin_unistat = ctx.get_unistat()
        inprog_fin_solomon = ctx.get_solomon()

        stream.read_data()

        after_unistat = ctx.get_unistat()
        after_solomon = ctx.get_solomon()

    assert before_unistat['report-default-inprog_ammv'] == 0
    assert inprog_start_unistat['report-default-inprog_ammv'] == 1
    assert inprog_fin_unistat['report-default-inprog_ammv'] == 1
    assert after_unistat['report-default-inprog_ammv'] == 0

    assert before_solomon['report-default-inprog']['value'] == 0
    assert inprog_start_solomon['report-default-inprog']['value'] == 1
    assert inprog_fin_solomon['report-default-inprog']['value'] == 1
    assert after_solomon['report-default-inprog']['value'] == 0


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_inprog_client_fail(ctx, disable_sslness, disable_robotness):
    """
    Если клиент закрывает соединение, не отправив запрос полностью,
    то из inprog соединение должно исчезнуть после истечения backend_timeout из-за BALANCER-386
    """
    backend_timeout = 2
    request = http.request.post(data=['led', 'zeppelin']).to_raw_request()
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        backend_timeout=backend_timeout,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    conn = ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request_line(request.request_line)
    stream.write_headers(request.headers)
    time.sleep(0.5)  # inprog should appear in statistics
    conn.close()
    time.sleep(backend_timeout - 1)

    before_unistat = ctx.get_unistat()
    before_solomon = ctx.get_solomon()
    time.sleep(1)
    after_unistat = ctx.get_unistat()
    after_solomon = ctx.get_solomon()

    assert before_unistat['report-default-inprog_ammv'] == 1
    assert after_unistat['report-default-inprog_ammv'] == 0

    assert before_solomon['report-default-inprog']['value'] == 1
    assert after_solomon['report-default-inprog']['value'] == 0


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_inprog_backend_timeout(ctx, disable_sslness, disable_robotness):
    """
    Если бэкенд отвечает медленно,
    то по истечении backend_timeout информация о соединении должна исчезнуть из inprog
    """
    chunk_timeout = 3
    request = http.request.get()
    backend = ctx.start_backend(ChunkedConfig(
        response=http.response.ok(data=['pink', 'floyd']), chunk_timeout=chunk_timeout))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        backend_timeout=chunk_timeout - 1,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    ctx.perform_request_xfail(request)
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-inprog_ammv'] == 0

            solomon = ctx.get_solomon()
            assert solomon['report-default-inprog']['value'] == 0


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_succ(ctx, disable_sslness, disable_robotness):
    """
    Информация об успешных запросах должна отражаться в статистике succ
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    ctx.perform_request(http.request.get())
    ctx.perform_request(http.request.post())
    ctx.perform_request(http.request.head())

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-succ_summ'] == 3
            assert unistat['report-default-requests_summ'] == 3

            solomon = ctx.get_solomon()
            assert solomon['report-default-succ']['value'] == 3
            assert solomon['report-default-requests']['value'] == 3


def test_requests(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
    ))

    ctx.perform_request(http.request.get())
    ctx.backend.stop()
    ctx.perform_request_xfail(http.request.get())

    unistat = ctx.get_unistat()
    assert unistat['report-default-succ_summ'] == 1
    assert unistat['report-default-fail_summ'] == 1
    assert unistat['report-default-requests_summ'] == 2

    solomon = ctx.get_solomon()
    assert solomon['report-default-succ']['value'] == 1
    assert solomon['report-default-fail']['value'] == 1
    assert solomon['report-default-requests']['value'] == 2


def create_https_conn(ctx):
    return ctx.manager.connection.http.create_pyssl(
        ctx.balancer.config.port,
    )


def selector(disable_sslness=False, disable_robotness=False,
             status="2xx", robotness="u", internalness="u", sslness="cu"):
    retval = status

    if not disable_robotness:
        retval += '-%s-%s' % (robotness, internalness)

    if not disable_sslness:
        if disable_robotness:
            retval += '-u-u-%s' % sslness
        else:
            retval += '-%s' % sslness

    retval += '_hgram'

    return retval


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_parts_disabled(ctx, disable_sslness, disable_robotness):
    """
    BALANCER-705
    BALANCER-706
    flags to disable robotness+internallness and sslness
    """

    select = selector(
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    )

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        legacy_ranges='1s,10s,500s',
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    ctx.perform_request(http.request.get())
    backend.state.get_request()

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-' + select][0][1] == 1


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_parts_disabled_client_ssl(ctx, disable_sslness, disable_robotness):
    """
    BALANCER-705
    BALANCER-706
    flags to disable robotness+internallness and sslness
    """

    select = selector(disable_sslness=disable_sslness,
                      disable_robotness=disable_robotness,
                      sslness="CU")

    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSSLConfig(
        ctx.certs.root_dir,
        backend.server_config.port,
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    ))

    with create_https_conn(ctx) as conn:
        conn.perform_request(http.request.get())

    backend.state.get_request()

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-' + select][0][1] == 1


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_refers_equal(ctx, disable_sslness, disable_robotness):
    """
    BALANCER-705
    BALANCER-706
    refering the report with same disableness settings is ok
    """
    ctx.start_balancer(ReportNessConfig(
        default_disable_sslness=disable_sslness,
        default_disable_robotness=disable_robotness,
        scorpions_disable_sslness=disable_sslness,
        scorpions_disable_robotness=disable_robotness
    ))
    ctx.perform_request(http.request.get())

    select = selector(
        disable_sslness=disable_sslness,
        disable_robotness=disable_robotness
    )
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-default-' + select][0][1] == 1
            assert unistat['report-scorpions-' + select][0][1] == 1


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_refers_greater(ctx, disable_sslness, disable_robotness):
    """
    BALANCER-705
    BALANCER-706
    refering the report with more detailed stats settings is ok.
    The lesser module counts "agregate" stats, the more detailed module
    does not loose information
    """
    ctx.start_balancer(ReportNessConfig(
        default_disable_sslness=disable_sslness,
        default_disable_robotness=disable_robotness,
        scorpions_disable_sslness=None,
        scorpions_disable_robotness=None
    ))

    default_paths = defaultdict(int)

    for robotness in [None, 'no', 'yes']:
        if robotness is not None:
            headers = {'X-Yandex-Suspected-Robot': robotness}
        else:
            headers = {}
        for internalness in [None, 'no', 'yes']:
            if internalness is not None:
                headers['X-Yandex-Internal-Request'] = internalness

            ctx.perform_request(http.request.get(headers=headers))
            path = selector(
                disable_sslness=disable_sslness,
                disable_robotness=disable_robotness,
                robotness=(robotness and robotness[0] or 'u'),
                internalness=(internalness and internalness[0] or 'u'),
                sslness='cu'
            )
            default_paths[path] += 1

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            for path, value in default_paths.iteritems():
                assert unistat['report-default-' + path][0][1] == value
            for robotness in [None, 'n', 'y']:
                for internalness in [None, 'n', 'y']:
                    path = selector(
                        robotness=(robotness or 'u'),
                        internalness=(internalness or 'u'),
                        sslness='cu'
                    )
                    assert unistat['report-scorpions-' + path][0][1] == 1


@pytest.mark.parametrize(
    'disable_sslness',
    [None, False, True],
    ids=['sslness_nil', 'sslness_false', 'sslness_true']
)
@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_refers_lesser(ctx, disable_sslness, disable_robotness):
    """
    BALANCER-705
    BALANCER-706
    refering the report with less detailed settings settings is ok.
    The lesser module counts "agregate" stats, the more detailed module
    does not loose information
    """
    ctx.start_balancer(ReportNessConfig(
        default_disable_sslness=None,
        default_disable_robotness=None,
        scorpions_disable_sslness=disable_sslness,
        scorpions_disable_robotness=disable_robotness
    ))

    scorpions_paths = defaultdict(int)

    for robotness in [None, 'no', 'yes']:
        if robotness is not None:
            headers = {'X-Yandex-Suspected-Robot': robotness}
        else:
            headers = {}

        for internalness in [None, 'no', 'yes']:
            if internalness is not None:
                headers['X-Yandex-Internal-Request'] = internalness

            ctx.perform_request(http.request.get(headers=headers))
            path = selector(
                disable_sslness=disable_sslness,
                disable_robotness=disable_robotness,
                robotness=(robotness and robotness[0] or 'u'),
                internalness=(internalness and internalness[0] or 'u'),
                sslness='cu'
            )
            scorpions_paths[path] += 1

    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            for path, value in scorpions_paths.iteritems():
                assert unistat['report-scorpions-' + path][0][1] == value

            for robotness in [None, 'n', 'y']:
                for internalness in [None, 'n', 'y']:
                    path = selector(
                        robotness=(robotness or 'u'),
                        internalness=(internalness or 'u'),
                        sslness='cu'
                    )
                    assert unistat['report-default-' + path][0][1] == 1


def test_multiple_uuid(ctx):
    """
    BALANCER-2298
    using multiple uuids
    """
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportMultipleUuid(backend.server_config.port))

    for uuid in ('a', 'b', 'c'):
        for suff in ('', '/foo', '/bar'):
            ctx.perform_request(http.request.get(path='/{}{}'.format(uuid, suff)))

    unistat = ctx.get_unistat()

    assert unistat['report-a-succ_summ'] == 9
    assert unistat['report-b-succ_summ'] == 6
    assert unistat['report-c-succ_summ'] == 3

    assert unistat['report-a_foo-succ_summ'] == 2
    assert unistat['report-b_foo-succ_summ'] == 1
    assert unistat['report-c_foo-succ_summ'] == 1

    assert unistat['report-a_bar-succ_summ'] == 1
    assert 'report-b_bar-succ_summ' not in unistat
    assert 'report-c_bar-succ_summ' not in unistat

    solomon = ctx.get_solomon()

    assert solomon['report-a-succ']['value'] == 9
    assert solomon['report-b-succ']['value'] == 6
    assert solomon['report-c-succ']['value'] == 3

    assert solomon['report-a_foo-succ']['value'] == 2
    assert solomon['report-b_foo-succ']['value'] == 1
    assert solomon['report-c_foo-succ']['value'] == 1

    assert solomon['report-a_bar-succ']['value'] == 1
    assert 'report-b_bar-succ' not in solomon
    assert 'report-c_bar-succ' not in solomon


def test_bad_refer_multi(ctx):
    """
    BALANCER-2298
    Refering multiple uuid is forbidden
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ReportMultipleBadReferMulti())


def test_bad_refer_non_unique(ctx):
    """
    BALANCER-2298
    Refering non-unique uuid is forbidden
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ReportMultipleBadReferNonUnique())


def test_multiple_conflicting_ranges(ctx):
    """
    BALANCER-2298
    Modules with common uuids and different ranges are forbidden
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ReportMultipleConflictingRanges())


def test_labels(ctx):
    request_paths = ['/led/']
    base_no_time_test(ctx, request_paths)

    solomon = ctx.get_solomon()

    assert solomon['report-led-outgoing_2xx']['labels']['a'] == 'b'
    assert solomon['report-led-outgoing_2xx']['labels']['c'] == 'd'

    unistat = ctx.get_unistat()
    assert 'a=b;c=d;report-led-outgoing_2xx_summ' in unistat or 'c=d;a=b;report-led-outgoing_2xx_summ' in unistat


class DelayedSlowHandler(StaticResponseHandler):
    """
    Reads request, sleeps first_delay time, writes headers, sleeps second_delay time and writes body
    """
    def handle_parsed_request(self, raw_request, stream):
        time.sleep(self.config.first_delay)
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        time.sleep(self.config.second_delay)
        stream.write_data(self.config.response.data)
        self.finish_response()


class DelayedSlowConfig(StaticResponseConfig):
    HANDLER_TYPE = DelayedSlowHandler

    def __init__(self, response=None, first_delay=0, second_delay=0):
        if response is None:
            response = http.response.ok()
        super(DelayedSlowConfig, self).__init__(response)
        self.first_delay = first_delay
        self.second_delay = second_delay


def test_ttfb(ctx):
    backend = ctx.start_backend(DelayedSlowConfig(response=http.response.ok(data='Hello world'), first_delay=2, second_delay=2))
    ctx.start_balancer(ReportConfig(
        backend.server_config.port,
        legacy_ranges='1s,2s,3s,4s,5s,6s,7s,8s,9s,10s',
    ))

    ctx.perform_request(http.request.get('/zeppelin/'))

    unistat = ctx.get_unistat()

    assert sum_unistat_range(unistat['report-zeppelin-processing_time_hgram'], 3, 5) == 1
    assert sum_unistat_range(unistat['report-zeppelin-backend_ttfb_hgram'], 1, 3) == 1


@pytest.mark.parametrize('code', [200, 500])
def test_headers_size(ctx, code):
    backend = ctx.start_backend(SimpleConfig(http.response.custom(code, reason='test')))
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        all_default_ranges=True,
        signal_set='full',
        fail_on_5xx=1
    ))

    for s in [100, 1100, 2200]:
        request = http.request.get(headers={
            'test': 'a'*s
        })
        if code == 500:
            ctx.perform_request_xfail(request)
        else:
            ctx.perform_request(request)

    unistat = ctx.get_unistat()

    assert sum_unistat_range(unistat['report-default-input_headers_size_hgram'], 0, 4096) == 3
    expected_failures = 3 if code == 500 else 0
    assert sum_unistat_range(unistat['report-default-backend_fail_input_headers_size_hgram'], 0, 4096) == expected_failures


@pytest.mark.parametrize(
    'disable_robotness',
    [None, False, True],
    ids=['robotness_nil', 'robotness_false', 'robotness_true']
)
def test_disable_robotness(ctx, disable_robotness):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        legacy_ranges='1s,10s,500s',
        disable_robotness=disable_robotness
    ))

    request = http.request.get(headers={
        'X-Yandex-Internal-Request': 'yes',
        'X-Yandex-Suspected-Robot': 'yes',
    })

    ctx.perform_request(request)
    unistat = ctx.get_unistat()

    signal_suffix = '-u-u' if disable_robotness else '-y-y'
    signal_name = 'report-default-2xx' + signal_suffix + '_hgram'

    hgram = unistat[signal_name]
    assert sum_unistat_range(hgram, 0) == 1


def test_disable_signals(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        all_default_ranges=True,
        disable_signals=','.join([
            'requests',
            'nka',
            'ka',
            'backend_time',
            'client_fail_time',
            'backend_ttfb',
            'input_size',
            'output_size'
        ]),
    ))
    ctx.perform_request(http.request.get('/'))
    unistat = ctx.get_unistat()
    assert 'report-default-requests_summ' not in unistat
    assert 'report-default-nka_summ' not in unistat
    assert 'report-default-ka_summ' not in unistat
    assert 'report-default-backend_time_hgram' not in unistat
    assert 'report-default-client_fail_time_hgram' not in unistat
    assert 'report-default-backend_ttfb_hgram' not in unistat
    assert 'report-default-input_size_hgram' not in unistat
    assert 'report-default-output_size_hgram' not in unistat


@pytest.mark.parametrize('signal_set', ['full', 'default', 'minimal', 'reduced'])
def test_signal_set(ctx, signal_set):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        all_default_ranges=True,
        signal_set=signal_set,
        disable_robotness=True,
        disable_sslness=True,
        hide_legacy_signals=True,
    ))

    ctx.perform_request(http.request.get('/'))
    unistat = ctx.get_unistat()
    signals = set()
    for signal in unistat:
        if signal.startswith('report-default-'):
            if signal.endswith("hgram"):
                signals.add(signal[len('report-default-'):-6])
            else:
                signals.add(signal[len('report-default-'):-5])

    if signal_set == 'minimal':
        assert signals == {
            'backend_attempt',
            'backend_error',
            'backend_time',
            'backend_timeout',
            'backend_ttfb',
            'client_fail',
            'client_timeout',
            'conn_other_error',
            'conn_refused',
            'conn_timeout',
            'fail',
            'inprog',
            'input_size',
            'limited_backend_attempt',
            'outgoing_101',
            'outgoing_2xx',
            'outgoing_3xx',
            'outgoing_404',
            'outgoing_4xx',
            'outgoing_5xx',
            'output_size',
            'processing_time',
            'requests',
            'sc_2xx',
            'sc_3xx',
            'sc_4xx',
            'sc_503',
            'sc_5xx',
            'succ',
        }
    elif signal_set == 'reduced':
        assert signals == {
            'backend_attempt',
            'backend_error',
            'backend_fail',
            'backend_time',
            'backend_timeout',
            'backend_ttfb',
            'client_fail',
            'client_timeout',
            'conn_fail',
            'conn_other_error',
            'conn_refused',
            'conn_timeout',
            'fail',
            'inprog',
            'input_size',
            'limited_backend_attempt',
            'no_backends_error',
            'outgoing_101',
            'outgoing_1xx',
            'outgoing_2xx',
            'outgoing_3xx',
            'outgoing_404',
            'outgoing_4xx',
            'outgoing_5xx',
            'output_size',
            'processing_time',
            'requests',
            'sc_1xx',
            'sc_2xx',
            'sc_3xx',
            'sc_4xx',
            'sc_503',
            'sc_5xx',
            'succ',
        }
    elif signal_set == 'default':
        assert signals == {
            'backend_attempt',
            'backend_error',
            'backend_fail',
            'backend_keepalive_reused',
            'backend_time',
            'backend_timeout',
            'backend_ttfb',
            'client_error',
            'client_fail_time',
            'client_fail',
            'client_timeout',
            'conn_fail',
            'conn_other_error',
            'conn_refused',
            'conn_timeout',
            'fail',
            'hedged_attempts',
            'hedged_succ',
            'inprog',
            'input_size',
            'input_speed',
            'ka',
            'limited_backend_attempt',
            'nka',
            'no_backends_error',
            'nreused',
            'other_fail',
            'outgoing_101',
            'outgoing_1xx',
            'outgoing_2xx',
            'outgoing_3xx',
            'outgoing_404',
            'outgoing_4xx',
            'outgoing_5xx',
            'output_size',
            'output_speed',
            'processing_time',
            'requests',
            'reused',
            'sc_1xx',
            'sc_2xx',
            'sc_3xx',
            'sc_4xx',
            'sc_503',
            'sc_5xx',
            'succ',
        }
    elif signal_set == 'full':
        assert signals == {
            'backend_attempt',
            'backend_error',
            'backend_fail',
            'backend_fail_input_headers_size',
            'backend_keepalive_reused',
            'backend_short_read_answer',
            'backend_time',
            'backend_timeout',
            'backend_ttfb',
            'backend_write_error',
            'client_error',
            'client_fail_time',
            'client_fail',
            'client_timeout',
            'conn_fail',
            'conn_other_error',
            'conn_refused',
            'conn_timeout',
            'fail',
            'hedged_attempts',
            'hedged_succ',
            'inprog',
            'input_headers_size',
            'input_size',
            'input_speed',
            'ka',
            'limited_backend_attempt',
            'nka',
            'no_backends_error',
            'nreused',
            'other_fail',
            'outgoing_101',
            'outgoing_1xx',
            'outgoing_2xx',
            'outgoing_3xx',
            'outgoing_404',
            'outgoing_4xx',
            'outgoing_5xx',
            'output_size',
            'output_speed',
            'processing_time',
            'requests',
            'reused',
            'sc_1xx',
            'sc_2xx',
            'sc_3xx',
            'sc_4xx',
            'sc_503',
            'sc_5xx',
            'succ',
        }


def test_enable_signals(ctx):
    backend = ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ReportSimpleConfig(
        backend.server_config.port,
        host='localhost',
        all_default_ranges=True,
        signal_set='minimal',
        disable_robotness=True,
        disable_sslness=True,
        hide_legacy_signals=True,
        enable_signals='client_error'
    ))

    ctx.perform_request(http.request.get('/'))
    unistat = ctx.get_unistat()
    assert 'report-default-client_error_summ' in unistat
