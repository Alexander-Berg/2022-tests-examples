# -*- coding: utf-8 -*-
"""
BALANCER-185
test cases:
    1. fast client passes through
    2. get client passes through
    2.1 slow get client passes through
    3. hi bytes sent then slowing passes if hi in pass_timeout
    4. full request < lo bytes passes through
    5. incomplete request in < lo bytes does not pass through (even no request to backend)
    5. full request in (lo, hi) bytes passes through if in pass_timeout
    6. full request in < lo bytes does not pass through if not in pass_timeout
    7. full request in (lo, hi) bytes does not pass through if not in pass_timeout
    9. full passed post is not affected by recv_timeout
    10. if client sends portions in pass_timeout and each portion in recv_timeout - it passes through
    10. if client sends hi in pass_timeout and each portion slower than in recv_timeout - it breaks
    10. if client sends (lo, hi) in pass_timeout and each portion slower than in recv_timeout - it breaks
    10. Not starting when lo_bytes > hi_bytes
"""
import pytest
import time
import itertools

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.stdlib.multirun import Multirun

from configs import ThresholdConfig, ThresholdSSLConfig, ThresholdOnPassFailureConfig, ThresholdOnPassFailureSSLConfig

from balancer.test.util.predef.handler.server.http import ChunkedConfig, DummyConfig
from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.predef import http


_SSL_CASES = [(False, 'nossl'), (True, 'ssl')]
_WORKERS_CASES = [(None, 'workers_nil'), (2, 'workers_2')]
_COMBINED = list(itertools.product(_SSL_CASES, _WORKERS_CASES))
_PARAMS = [(i[0][0], i[1][0]) for i in _COMBINED]
_IDS = ['-'.join([i[0][1], i[1][1]]) for i in _COMBINED]


class ThresholdContext(object):
    def __init__(self):
        super(ThresholdContext, self).__init__()
        self.__ssl = self.request.param[0]
        self.__workers = self.request.param[1]

    @property
    def ssl(self):
        return self.__ssl

    def start_th_balancer(self, **balancer_kwargs):
        if self.ssl:
            config = ThresholdSSLConfig(backend_port=self.backend.server_config.port,
                                        cert_dir=self.certs.root_dir,
                                        workers=self.__workers,
                                        **balancer_kwargs)
        else:
            config = ThresholdConfig(backend_port=self.backend.server_config.port, workers=self.__workers, **balancer_kwargs)
        balancer = self.start_balancer(config)
        if self.__workers:
            time.sleep(0.5)
        return balancer

    def start_all(self, backend_config=None, **balancer_kwargs):
        if backend_config is None:
            backend_config = ChunkedConfig()
        self.start_backend(backend_config)
        self.start_th_balancer(**balancer_kwargs)
        return self.balancer

    def create_conn(self):
        if self.__ssl:
            return self.manager.connection.http.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True)
            )
        else:
            return self.create_http_connection()


th_ctx = mod_ctx.create_fixture(ThresholdContext, params=_PARAMS, ids=_IDS)


class ThresholdOnPassFailureContext(ThresholdContext):
    def __init__(self):
        super(ThresholdOnPassFailureContext, self).__init__()
        self.__workers = self.request.param[1]

    def start_on_pass_failure_balancer(self, **balancer_kwargs):
        if self.ssl:
            config = ThresholdOnPassFailureSSLConfig(backend_port=self.backend.server_config.port,
                                                     on_pass_failure_backend_port=self.on_pass_failure_backend.server_config.port,
                                                     cert_dir=self.certs.root_dir,
                                                     workers=self.__workers,
                                                     **balancer_kwargs)
        else:
            config = ThresholdOnPassFailureConfig(backend_port=self.backend.server_config.port,
                                                  on_pass_failure_backend_port=self.on_pass_failure_backend.server_config.port,
                                                  workers=self.__workers,
                                                  **balancer_kwargs)

        self.start_balancer(config)
        if self.__workers:
            time.sleep(0.5)

    def start_all(self, backend_config=None, on_pass_failure_backend_config=None, **balancer_kwargs):
        if backend_config is None:
            backend_config = ChunkedConfig(response=http.response.ok(data=['backend']))

        if on_pass_failure_backend_config is None:
            on_pass_failure_backend_config = ChunkedConfig(response=http.response.ok(data=['on_pass_timeout_failure_backend']))

        self.start_backend(backend_config)
        self.start_backend(on_pass_failure_backend_config, name='on_pass_failure_backend')
        self.start_on_pass_failure_balancer(**balancer_kwargs)


on_pass_failure_ctx = mod_ctx.create_fixture(ThresholdOnPassFailureContext, params=_PARAMS, ids=_IDS)


def test_get_passes(th_ctx):
    """
    GET requests (or requests without body in future)
    pass through
    """
    th_ctx.start_all()
    with th_ctx.create_conn() as conn:
        resp = conn.perform_request(http.request.get())
    asserts.status(resp, 200)


def test_slow_get_client_passes(th_ctx):
    """
    GET requests (or requests without body in future)
    pass through even if the client does not satisfy
    timeout requirements
    """
    timeout = 1
    th_ctx.start_all(pass_timeout='%ds' % timeout, recv_timeout='%ds' % timeout,
                     lo_bytes=1000, hi_bytes=10000)
    request = http.request.get(headers=[('host', 'localhost')]).to_raw_request()
    stream = th_ctx.create_conn().create_stream()
    stream.write_request_line(request.request_line)
    for name, value in request.headers.items():
        time.sleep(1.5 * timeout)
        stream.write_header(name, value)
    stream.end_headers()
    stream.write_data(request.data)
    resp = stream.read_response()
    asserts.status(resp, 200)


def test_fast_post_length(th_ctx):
    """
    POST which is faster than pass_timeout and recv_timeout
    just passes through regardless of hi_bytes
    """
    th_ctx.start_all(pass_timeout='10s', lo_bytes=10 ** 6, hi_bytes=10 ** 6)
    resp = th_ctx.create_conn().perform_request(http.request.post(data='0123456789'))
    asserts.status(resp, 200)


def test_fast_post_chunked(th_ctx):
    """
    POST which is faster than pass_timeout and recv_timeout
    just passes through regardless of hi_bytes
    """
    th_ctx.start_all(pass_timeout='10s', lo_bytes=10 ** 6, hi_bytes=10 ** 6)
    resp = th_ctx.create_conn().perform_request(http.request.post(data=['0123456789'] * 10))
    asserts.status(resp, 200)


def test_fast_post_chunked_slow_backend(th_ctx):
    """
    Full POST request passes down to backend. The client
    gets an answer even if backend answers slower than
    recv_timeout on client
    """
    backend_answer_delay = 2
    response = http.response.ok(data=['hello', 'world'])
    config = ChunkedConfig(response=response, chunk_timeout=backend_answer_delay)
    th_ctx.start_all(backend_config=config,
                     pass_timeout='10s', recv_timeout='%ds' % (backend_answer_delay / 2),
                     lo_bytes=10 ** 6, hi_bytes=10 ** 6)
    resp = th_ctx.create_conn().perform_request(http.request.post(data=['0123456789'] * 10))
    asserts.status(resp, 200)


def test_close_and_next_request(th_ctx):
    """
    BALANCER-275
    First request closes without reading answer.
    The next fast enough request must be answered
    """
    th_ctx.start_all(pass_timeout='10s')
    stream = th_ctx.create_conn().create_stream()
    stream.write_request(http.request.post(data='0123456789').to_raw_request())
    resp = th_ctx.create_conn().perform_request(http.request.post(data='0123456789'))
    asserts.status(resp, 200)


def test_lo_bytes_empty_body(th_ctx):
    """
    If client sends less than lo_bytes, then request
    should not be propagated down.
    """
    th_ctx.start_all(pass_timeout='10s', recv_timeout='1s', lo_bytes=0, hi_bytes=10)
    stream = th_ctx.create_conn().create_stream()
    stream.write_request(http.request.raw_post(headers=[('transfer-encoding', 'chunked')], data=None))
    time.sleep(2)
    assert th_ctx.backend.state.accepted.value == 0


def test_lo_bytes_partial(th_ctx):
    """
    If client sends less than lo_bytes, then request
    should not be propagated down.
    """
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    th_ctx.start_all(pass_timeout='10s', recv_timeout='1s', lo_bytes=100, hi_bytes=1000)
    with th_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write_chunk('A' * 50)
        time.sleep(2)
        assert th_ctx.backend.state.accepted.value == 0


def test_hi_bytes_passes(th_ctx):
    """
    If client sends hi_bytes within pass_timeout, connection propagates
    to backend
    """
    hi_bytes = 1000
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    th_ctx.start_all(pass_timeout='10s', recv_timeout='10s',
                     lo_bytes=hi_bytes / 1000, hi_bytes=hi_bytes)
    with th_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write_chunk('A' * (hi_bytes / 2))
        assert th_ctx.backend.state.accepted.value == 0
        time.sleep(1)
        stream.write_chunk('B' * (hi_bytes / 2))
        for run in Multirun(plan=[0.1] * 20):
            with run:
                assert th_ctx.backend.state.accepted.value == 1
        stream.write_chunk('')
        resp = stream.read_response()
        asserts.status(resp, 200)


def test_pass_timeout_passes(th_ctx):
    """
    BALANCER-549
    If client sends bytes in (lo_bytes, hi_bytes) within pass_timeout,
    connection propagates to backend
    """
    hi_bytes = 1000
    pass_timeout = 2
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    th_ctx.start_all(pass_timeout='%ds' % pass_timeout, recv_timeout='10s',
                     lo_bytes=hi_bytes / 1000, hi_bytes=hi_bytes)
    with th_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write_chunk('A' * (hi_bytes / 2))
        assert th_ctx.backend.state.accepted.value == 0
        for run in Multirun(plan=[0.1] * 30):
            with run:
                assert th_ctx.backend.state.accepted.value == 1
        stream.write_chunk('')
        resp = stream.read_response()
        asserts.status(resp, 200)


def test_disable_pass_timeout(th_ctx):
    """
    If pass_timeout == 0, then request line and headers should be propagated down
    without waiting for request body
    """
    path = '/somewhere'
    request = http.request.raw_post(path=path, headers=[('transfer-encoding', 'chunked')], data=None)
    th_ctx.start_all(pass_timeout=0, recv_timeout='10s', lo_bytes=0, hi_bytes=0)
    with th_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request(request)
        time.sleep(1)

    err_request = th_ctx.backend.state.read_errors.get().raw_message
    asserts.path(err_request, path)


def test_fail_on_hi_bytes_lesser_lo_bytes(th_ctx):
    """
    If hi_bytes < lo_bytes, then balancer should not start
    """
    backend_port = th_ctx.manager.port.get_port()
    with pytest.raises(BalancerStartError):
        th_ctx.start_balancer(ThresholdConfig(backend_port, pass_timeout='100s', recv_timeout='100s',
                                              lo_bytes=42, hi_bytes=41))


def test_fail_on_disabled_pass_timeout_with_non_zero_hi_bytes(th_ctx):
    """
    If pass_timeout is disabled and hi_bytes != 0, then balancer should not start
    """
    backend_port = th_ctx.manager.port.get_port()
    with pytest.raises(BalancerStartError):
        th_ctx.start_balancer(ThresholdConfig(backend_port, pass_timeout=0, recv_timeout='100s',
                                              lo_bytes=0, hi_bytes=1))


@pytest.mark.parametrize('method', [
    'POST', 'PUT', 'GET'
])
def test_conn_close_on_partial_send(th_ctx, method):
    """
    BALANCER-275
    Request is send partially, then client closes connection.
    Connection to backend should be dropped.
    """
    chunks = 5
    chunk_len = 20
    if method == 'POST':
        request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    elif method == 'PUT':
        request = http.request.raw_put(headers=[('transfer-encoding', 'chunked')])
    elif method == 'GET':
        request = http.request.raw_get(headers=[('transfer-encoding', 'chunked')])
    else:
        pytest.fail('unknown method %s' % method)
    th_ctx.start_all(pass_timeout='10s', recv_timeout='10s', lo_bytes=0, hi_bytes=10, backend_timeout='20s')
    tcpdump = th_ctx.manager.tcpdump.start(th_ctx.backend.server_config.port)

    with th_ctx.create_conn() as conn:
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        for _ in xrange(chunks):
            stream.write_chunk('A' * chunk_len)
        time.sleep(1)
    time.sleep(1)

    unistat = th_ctx.get_unistat()
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0

    for run in Multirun(sum_delay=6):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert sess.finished_by_client()  # client is balancer, server is backend
    err_request = th_ctx.backend.state.read_errors.get().raw_message
    asserts.content(err_request, 'A' * (chunks * chunk_len))


def test_conn_wait_on_partial_send(th_ctx):  # TODO: get rid of copypaste
    """
    BALANCER-278
    Request is send partially, then client waits.
    Connection to backend should be dropped.
    """
    chunks = 5
    chunk_len = 20
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    th_ctx.start_all(pass_timeout='10s', recv_timeout='1s', lo_bytes=0, hi_bytes=10)
    tcpdump = th_ctx.manager.tcpdump.start(th_ctx.backend.server_config.port)

    conn = th_ctx.create_conn()
    stream = conn.create_stream()
    stream.write_request_line(request.request_line)
    stream.write_headers(request.headers)
    for _ in xrange(chunks):
        stream.write_chunk('A' * chunk_len)

    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert sess.finished_by_client()  # client is balancer, server is backend
    err_request = th_ctx.backend.state.read_errors.get().raw_message
    asserts.content(err_request, 'A' * (chunks * chunk_len))
    assert th_ctx.balancer.is_alive()


def test_disable_recv_timeout(th_ctx):
    """
    If recv_timeout == 0, then balancer should keep connection until backend_timeout expires
    """
    hi_bytes = 5
    data = 'A' * (hi_bytes * 2)
    backend_timeout = 5
    request = http.request.raw_post(path='/somewhere', headers=[('transfer-encoding', 'chunked')])
    th_ctx.start_all(pass_timeout='100s', recv_timeout=0,
                     lo_bytes=0, hi_bytes=hi_bytes, backend_timeout='%ds' % backend_timeout)

    tcpdump = th_ctx.manager.tcpdump.start(th_ctx.backend.server_config.port)
    conn = th_ctx.create_conn()
    stream = conn.create_stream()
    stream.write_request_line(request.request_line)
    stream.write_headers(request.headers)
    stream.write_chunk(data)
    time.sleep(backend_timeout - 1)
    asserts.is_not_closed(conn.sock)
    time.sleep(2)
    asserts.is_closed(conn.sock)

    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == 1
            sess = sessions[0]
            assert backend_timeout - 1 < sess.get_seconds_duration() < backend_timeout + 1
    err_request = th_ctx.backend.state.read_errors.get().raw_message
    asserts.path(err_request, '/somewhere')
    asserts.content(err_request, data)


def make_on_pass_timeout_query(on_pass_failure_ctx, conn):
    main_accepted = on_pass_failure_ctx.backend.state.accepted.value
    accepted = on_pass_failure_ctx.on_pass_failure_backend.state.accepted.value
    request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
    stream = conn.create_stream()
    stream.write_request_line(request.request_line)
    stream.write_headers(request.headers)
    stream.write_chunk('A' * 50)
    for run in Multirun(plan=[0.1] * 40):
        with run:
            assert on_pass_failure_ctx.backend.state.accepted.value - main_accepted == 0
            assert on_pass_failure_ctx.on_pass_failure_backend.state.accepted.value - accepted == 1

    stream.write_chunk('B' * 20)
    stream.write_chunk('')

    req = on_pass_failure_ctx.on_pass_failure_backend.state.get_request()
    assert req.data.content == 'A' * 50 + 'B' * 20
    resp = stream.read_response()
    asserts.status(resp, 200)
    asserts.content(resp, 'on_pass_timeout_failure_backend')


@pytest.mark.parametrize('series_count', [
    1, 3,
])
def test_on_pass_timeout_failure(on_pass_failure_ctx, series_count):
    """
    BALANCER-672
    When pass_timeout condition fails control is transfered to
    on_pass_timeout_failure section. Series of keepalive requests
    should work too
    """
    on_pass_failure_ctx.start_all(pass_timeout='1s', lo_bytes=100, hi_bytes=1000)
    with on_pass_failure_ctx.create_conn() as conn:
        for i in xrange(series_count):
            make_on_pass_timeout_query(on_pass_failure_ctx, conn)


@pytest.mark.parametrize('fail_before', [
    False, True
])
@pytest.mark.parametrize('fail_after', [
    False, True
])
def test_on_pass_timeout_mixed(on_pass_failure_ctx, fail_before, fail_after):
    """
    BALANCER-672
    If first request fullfils pass_timeout, if goes
    to main module
    """
    on_pass_failure_ctx.start_all(pass_timeout='1s', lo_bytes=100, hi_bytes=1000)
    with on_pass_failure_ctx.create_conn() as conn:
        if fail_before:
            make_on_pass_timeout_query(on_pass_failure_ctx, conn)

        data = 'data'
        resp = conn.perform_request(http.request.post(data=data))
        asserts.content(resp, 'backend')
        for run in Multirun(plan=[0.1] * 20):
            with run:
                assert on_pass_failure_ctx.backend.state.accepted.value == 1
        req = on_pass_failure_ctx.backend.state.get_request()
        asserts.content(req, data)

        if fail_after:
            make_on_pass_timeout_query(on_pass_failure_ctx, conn)


def test_on_pass_timeout_broken_backend(on_pass_failure_ctx):
    """
    BALANCER-672
    If on_pass_timeout_failure backend fails,
    balancer should close connection to client and should not fail.
    """
    on_pass_failure_ctx.start_all(pass_timeout='1s', lo_bytes=100, hi_bytes=1000,
                                  on_pass_failure_backend_config=DummyConfig())
    with on_pass_failure_ctx.create_conn() as conn:
        request = http.request.raw_post(headers=[('transfer-encoding', 'chunked')])
        stream = conn.create_stream()
        stream.write_request_line(request.request_line)
        stream.write_headers(request.headers)
        stream.write_chunk('A' * 50)
        time.sleep(3)

        stream.write_chunk('B' * 20)
        stream.write_chunk('')

        asserts.is_closed(conn.sock, timeout=2)

    assert on_pass_failure_ctx.balancer.is_alive()
    resp = on_pass_failure_ctx.perform_request(http.request.post(data='Led'))
    asserts.content(resp, 'backend')
