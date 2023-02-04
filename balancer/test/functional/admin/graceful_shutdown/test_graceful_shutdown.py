# -*- coding: utf-8 -*-
import pytest
import socket
import time
import itertools
import threading

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.stdlib.multirun import Multirun

from configs import AdminShutdownConfig, AdminShutdownSSLConfig, AdminShutdownH2Config

from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.proto.http2 import errors
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2

from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.sanitizers import sanitizers

_PROTO_CASES = [
    ('http', 'http'),
    ('https', 'https'),
    ('h2', 'h2')
]

_WORKERS_CASES = [
    (2, 'workers_2')
]

_COMBINED = list(
    itertools.product(_PROTO_CASES, _WORKERS_CASES)
)

_PARAMS = [
    (i[0][0], i[1][0]) for i in _COMBINED
]

_IDS = [
    '-'.join([i[0][1], i[1][1]]) for i in _COMBINED
]


class GracefulShutdownContext(object):
    def __init__(self):
        super(GracefulShutdownContext, self).__init__()
        self.__proto = self.request.param[0]
        self.__workers = self.request.param[1]

    @property
    def proto(self):
        return self.__proto

    @property
    def workers(self):
        return self.__workers

    def start_gs_balancer(self):
        if self.__proto == 'http':
            config = AdminShutdownConfig(workers=self.__workers)
        elif self.__proto == 'https':
            config = AdminShutdownSSLConfig(cert_dir=self.certs.root_dir, workers=self.__workers)
        else:
            config = AdminShutdownH2Config(cert_dir=self.certs.root_dir, workers=self.__workers)
        balancer = self.start_balancer(config, debug=True)
        if self.__workers:
            time.sleep(1)
        return balancer

    def create_conn(self):
        if self.__proto == 'http':
            return self.create_http_connection(port=self.balancer.config.port)
        elif self.__proto == 'https':
            return self.manager.connection.http.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True)
            )
        else:
            conn = self.manager.connection.http2.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True, alpn='h2')
            )
            conn.write_preface()
            return conn

    def write_request(self, stream):
        if self.__proto == 'h2':
            stream.write_message(http2.request.get().to_raw_request())
        else:
            stream.write_request(http.request.raw_get(headers={}))

    def read_response(self, stream):
        if self.__proto == 'h2':
            return stream.read_message()
        else:
            return stream.read_response()


gs_ctx = mod_ctx.create_fixture(GracefulShutdownContext, params=_PARAMS, ids=_IDS)


@pytest.mark.parametrize(
    ['backend_timeout', 'balancer_timeout', 'response_expected'],
    [
        (6, 15, True),
        (15, 6, False)
    ],
    ids=['backend_responds_in_time', 'shutdown_timeout_is_triggered']
)
def test_graceful_shutdown(gs_ctx, backend_timeout, balancer_timeout, response_expected):
    """
    BALANCER-741
    Балансер должен работать после запроса graceful_shutdown до тех пор,
    пока либо клиент не получит ответ, либо не пройдёт timeout.
    """
    gs_ctx.start_backend(SimpleDelayedConfig(response_delay=backend_timeout))
    gs_ctx.start_gs_balancer()

    start_time = time.time()
    try:
        with gs_ctx.create_conn() as conn:
            stream = conn.create_stream()
            gs_ctx.write_request(stream)

            time.sleep(0.2)

            event = threading.Event()
            event.clear()

            def run_request():
                event.set()
                gs_ctx.graceful_shutdown(timeout='{}s'.format(balancer_timeout))

            thread = threading.Thread(target=run_request)
            thread.start()
            event.wait()
            timeout = min(backend_timeout, balancer_timeout)
            time.sleep(timeout * 0.5)

            assert gs_ctx.balancer.is_alive()
            time.sleep(timeout * 0.5 + 2)  # complete timeout + 2 for reading next request

            thread.join()

            if sanitizers.msan_enabled() or sanitizers.asan_enabled():
                time.sleep(3)

            for run in Multirun(plan=[0.1] * 20):
                with run:
                    assert not gs_ctx.balancer.is_alive(), 'time taken: {}'.format(time.time() - start_time)

            try:
                gs_ctx.read_response(stream)
                response_succeeded = True
            except:
                response_succeeded = False

            assert response_succeeded == response_expected

    finally:
        if not gs_ctx.balancer.is_alive():
            gs_ctx.balancer.set_finished()


def test_graceful_shutdown_port_ready(gs_ctx):
    """
    BALANCER-741
    Балансер должен отпустить порт сразу после ответа на /admin?action=graceful_shutdown
    """
    gs_ctx.start_backend(SimpleDelayedConfig(response_delay=1))
    gs_ctx.start_gs_balancer()

    # TODO: move to util
    def check_port_available(port):
        sock = socket.socket()
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind(('localhost', port))
            return True
        except:
            return False
        finally:
            sock.close()

    try:
        with gs_ctx.create_conn() as conn:
            stream = conn.create_stream()
            gs_ctx.write_request(stream)

            time.sleep(0.2)

            gs_ctx.graceful_shutdown(timeout='1s')
            assert check_port_available(gs_ctx.balancer.config.port)
            assert check_port_available(gs_ctx.balancer.config.admin_port)

        for run in Multirun():
            with run:
                assert not gs_ctx.balancer.is_alive()
    finally:
        if not gs_ctx.balancer.is_alive():
            gs_ctx.balancer.set_finished()


def test_graceful_shutdown_multiple_requests(gs_ctx):
    """
    BALANCER-741
    Балансер должен ответить на два запроса подряд в рамках одной сессии, находясь в режиме graceful_shutdown
    """
    gs_ctx.start_backend(SimpleDelayedConfig(response_delay=0.5))
    gs_ctx.start_gs_balancer()

    requests_count = 2

    try:
        with gs_ctx.create_conn() as conn:
            streams = list()
            for i in xrange(requests_count):
                stream = conn.create_stream()
                gs_ctx.write_request(stream)
                streams.append(stream)

            time.sleep(0.2)

            def run_request():
                gs_ctx.graceful_shutdown(timeout='3s')

            thread = threading.Thread(target=run_request)
            thread.start()

            for stream in streams:
                gs_ctx.read_response(stream)

            thread.join()
        for run in Multirun():
            with run:
                assert not gs_ctx.balancer.is_alive()
    finally:
        if not gs_ctx.balancer.is_alive():
            gs_ctx.balancer.set_finished()


@pytest.mark.parametrize('timeout', [None, '10s'], ids=['no-timeout', '10s'])
def test_graceful_shutdown_no_connections(gs_ctx, timeout):
    """
    BALANCER-741
    Balancer should stop without waiting for timeout
    if there are no active connections no matter what options
    are passed
    """
    gs_ctx.start_backend(SimpleConfig())
    gs_ctx.start_gs_balancer()
    try:
        gs_ctx.graceful_shutdown(timeout=timeout)

        for run in Multirun(plan=[0.1] * 50):
            with run:
                assert not gs_ctx.balancer.is_alive()
    finally:
        gs_ctx.balancer.set_finished()


def test_graceful_shutdown_bad_timeout(ctx):
    """
    BALANCER-741
    If bad params are passed to graceful_shutdown, then no action should be performed
    """
    ctx.start_balancer(AdminShutdownConfig(
        workers=1,
    ))
    ctx.graceful_shutdown(cooldown='upchk', timeout='upchk', close_timeout='upchk')

    time.sleep(1)
    assert ctx.balancer.is_alive()


class Http2Context(object):
    def __init__(self):
        super(Http2Context, self).__init__()
        self.__workers = self.request.param

    def start_and_connect(self):
        self.start_backend(SimpleConfig())
        self.start_balancer(AdminShutdownH2Config(cert_dir=self.certs.root_dir, workers=self.__workers), debug=True)
        if self.__workers:
            time.sleep(1)
        conn = self.manager.connection.http2.create_ssl(
            self.balancer.config.port,
            SSLClientOptions(ca_file=self.certs.root_ca, quiet=True, alpn='h2')
        )
        conn.write_preface()
        return conn

    def set_finished(self):
        for _ in range(20):
            if not self.balancer.is_alive():
                break
            time.sleep(1)
        if not self.balancer.is_alive():
            self.balancer.set_finished()


h2_ctx = mod_ctx.create_fixture(Http2Context, params=[2], ids=['workers_2'])


def test_http2_no_streams_goaway(h2_ctx):
    """
    BALANCER-1227
    Balancer must send GOAWAY(NO_ERROR) with maximum valid last_stream_id
    to HTTP/2 client connection even if no streams have been opened
    """
    conn = h2_ctx.start_and_connect()
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        frame = conn.wait_frame(frames.Goaway)

        thread.join()
        assert frame.error_code == errors.NO_ERROR
        assert frame.last_stream_id == 2 ** 31 - 1
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_http2_no_opened_streams_goaway(h2_ctx):
    """
    BALANCER-1227
    Balancer must send GOAWAY(NO_ERROR) with maximum valid last_stream_id
    to HTTP/2 client connection even if there are no currently opened streams
    """
    conn = h2_ctx.start_and_connect()
    conn.perform_request(http2.request.get())
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        frame = conn.wait_frame(frames.Goaway)

        thread.join()
        assert frame.error_code == errors.NO_ERROR
        assert frame.last_stream_id == 2 ** 31 - 1
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_http2_ping_after_goaway(h2_ctx):
    """
    BALANCER-1227
    Balancer must send PING after GOAWAY to ensure that client received GOAWAY
    """
    conn = h2_ctx.start_and_connect()
    conn.perform_request(http2.request.get())
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        conn.wait_frame(frames.Goaway)
        frame = conn.wait_frame(frames.Ping)

        thread.join()
        assert frame.flags & flags.ACK == 0
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_http2_goaway_after_ping_no_opened_streams(h2_ctx):
    """
    BALANCER-1227
    Balancer sends GOAWAY(NO_ERROR) and PING without ACK
    Client sends PING with ACK
    Balancer must send GOAWAY(NO_ERROR) with last stream id set to max stream id opened before client's PING with ACK
    """
    conn = h2_ctx.start_and_connect()
    conn.perform_request(http2.request.get())
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        conn.wait_frame(frames.Goaway)
        ping_frame = conn.wait_frame(frames.Ping)
        conn.write_frame(frames.Ping(
            flags=flags.ACK, reserved=0, data=ping_frame.data,
        ))
        frame = conn.wait_frame(frames.Goaway)
        thread.join()
        assert frame.error_code == errors.NO_ERROR
        assert frame.last_stream_id == 1
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_http2_new_stream_before_ping_ack(h2_ctx):
    """
    BALANCER-1227
    If client opens stream before sending PING ACK then balancer must not reset this stream
    """
    data = 'A' * 10
    conn = h2_ctx.start_and_connect()
    conn.perform_request(http2.request.get())
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        conn.wait_frame(frames.Goaway)
        ping_frame = conn.wait_frame(frames.Ping)

        stream = conn.create_stream()
        stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
        conn.write_frame(frames.Ping(
            flags=flags.ACK, reserved=0, data=ping_frame.data,
        ))
        frame = conn.wait_frame(frames.Goaway)
        stream.write_chunk(data, end_stream=True)
        resp = stream.read_message().to_response()
        h2_ctx.backend.state.get_request()  # first request
        backend_req = h2_ctx.backend.state.get_request()

        thread.join()

        assert frame.error_code == errors.NO_ERROR
        assert frame.last_stream_id == stream.stream_id
        asserts.status(resp, 200)
        asserts.content(backend_req, data)
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_http2_new_stream_after_ping_ack(h2_ctx):
    """
    BALANCER-1227
    If client opens stream after sending PING ACK then balancer must send RST_STREAM(REFUSED_STREAM)
    """
    conn = h2_ctx.start_and_connect()
    conn.perform_request(http2.request.get())
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='4s')

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        conn.wait_frame(frames.Goaway)
        ping_frame = conn.wait_frame(frames.Ping)

        conn.write_frame(frames.Ping(
            flags=flags.ACK, reserved=0, data=ping_frame.data,
        ))
        stream = conn.create_stream()
        stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
        goaway_frame = conn.wait_frame(frames.Goaway)
        with pytest.raises(errors.StreamError) as exc_info:
            while True:
                stream.read_frame()
        rst_frame = exc_info.value

        thread.join()
        assert goaway_frame.error_code == errors.NO_ERROR
        assert goaway_frame.last_stream_id == 1
        assert rst_frame.error_code == errors.REFUSED_STREAM
    finally:
        conn.close()
        h2_ctx.set_finished()


@pytest.mark.xfail(reason='not implemented')
def test_http2_reset_streams_on_timeout(h2_ctx):
    """
    BALANCER-1227
    Balancer must reset all opened streams before closing connection on graceful shutdown timeout
    """
    timeout = 3
    conn = h2_ctx.start_and_connect()
    streams = list()
    for _ in range(3):
        stream = conn.create_stream()
        stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
        streams.append(stream)
    try:
        event = threading.Event()
        event.clear()

        def run_request():
            event.set()
            h2_ctx.graceful_shutdown(timeout='{}s'.format(timeout))

        thread = threading.Thread(target=run_request)
        thread.start()
        event.wait()

        conn.wait_frame(frames.Goaway)
        ping_frame = conn.wait_frame(frames.Ping)
        conn.write_frame(frames.Ping(
            flags=flags.ACK, reserved=0, data=ping_frame.data,
        ))
        conn.wait_frame(frames.Goaway)

        time.sleep(timeout)
        for stream in streams:
            with pytest.raises(errors.StreamError) as exc_info:
                while True:
                    stream.read_frame()
            rst_frame = exc_info.value
            assert rst_frame.error_code == errors.PROTOCOL_ERROR
        thread.join()
    finally:
        conn.close()
        h2_ctx.set_finished()


def test_shutdown_invalid_config(ctx):
    ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(AdminShutdownConfig(
            workers=1,
            shutdown_accept_connections=True,
            shutdown_close_using_bpf=True,
        ))


def test_shutdown_accept_connections(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AdminShutdownConfig(
        workers=1,
        shutdown_accept_connections=True,
    ))

    def start_shutdown():
        ctx.graceful_shutdown(timeout='60s')

    try:
        conn = ctx.create_http_connection()
        stream = conn.create_stream()
        stream.write_request_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()

        thread = threading.Thread(target=start_shutdown)
        thread.start()

        resp = ctx.perform_request(http.request.get())
        asserts.status(resp, 200)

        stream.write_chunk('')
        resp = stream.read_response()
        asserts.status(resp, 200)
    finally:
        ctx.balancer.set_finished()


def test_shutdown_close_using_bpf(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AdminShutdownConfig(
        workers=1,
        shutdown_close_using_bpf=True,
    ))

    def start_shutdown():
        ctx.graceful_shutdown(timeout='60s')

    thread = None
    try:
        conn = ctx.create_http_connection()
        stream = conn.create_stream()
        stream.write_request_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()

        thread = threading.Thread(target=start_shutdown)
        thread.start()

        for run in Multirun():
            with run:
                with pytest.raises(socket.timeout):
                    ctx.create_http_connection(timeout=1)

        stream.write_chunk('')
        resp = stream.read_response()
        asserts.status(resp, 200)
    finally:
        ctx.balancer.set_finished()
        if thread is not None:
            thread.join()


def test_shutdown_cooldown(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AdminShutdownConfig(
        workers=1,
    ))

    def start_shutdown():
        ctx.graceful_shutdown(cooldown='10s')

    def check_nokeepalive():
        with ctx.create_http_connection() as conn:
            resp = conn.perform_request(http.request.get())
            asserts.status(resp, 200)
            asserts.is_closed(conn.sock)

    thread = None
    try:
        thread = threading.Thread(target=start_shutdown)
        thread.start()

        for run in Multirun():
            with run:
                check_nokeepalive()

        for i in range(10):
            check_nokeepalive()

    finally:
        ctx.balancer.set_finished()
        if thread is not None:
            thread.join()
