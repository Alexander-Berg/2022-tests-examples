import datetime

from configs import ProxyConfig

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import HTTPServerHandler, HTTPConfig, CounterState


class StreamingHandler(HTTPServerHandler):
    def handle_request(self, stream):
        counter = self.state.counter.inc()
        if self.config.keepalive and counter < 1:
            self.append_request(stream.read_request())
            stream.write_response(http.response.ok().to_raw_response())
            return

        stream.read_request_line()
        stream.read_headers()

        if self.config.write:
            stream.write_line('HTTP/1.1 200 OK')
            stream.write_header('content-length', '65536')
            stream.end_headers()
            stream.write('A' * 32768)

        while self.sock.recv():
            pass
        self.force_close()


class StreamingConfig(HTTPConfig):
    HANDLER_TYPE = StreamingHandler
    STATE_TYPE = CounterState

    def __init__(self, keepalive=False, write=False):
        super(StreamingConfig, self).__init__()
        self.keepalive = keepalive
        self.write = write


def test_watch_client_close_get(ctx):
    ctx.start_backend(StreamingConfig())
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        watch_client_close=True,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1


def test_watch_client_close_write_partial(ctx):
    ctx.start_backend(StreamingConfig(write=True))
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        watch_client_close=True,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())
        stream.read_response_line()
        stream.read_headers()
        assert conn.sock.recv(32768) == 'A' * 32768

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1


def test_watch_client_close_post(ctx):
    ctx.start_backend(StreamingConfig())
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        watch_client_close=True,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_post(headers={'content-length': '65536'}))
        stream.write('A' * 32768)

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1


def test_watch_client_close_chunked_transfer(ctx):
    ctx.start_backend(StreamingConfig())
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        watch_client_close=True,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_post(headers={'transfer-encoding': 'chunked'}))
        stream.write_chunk('A' * 32768)

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1


def test_watch_client_close_pipelining(ctx):
    ctx.start_backend(StreamingConfig())
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        watch_client_close=True,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())
        stream.write_request(http.request.raw_post(headers={'content-length': '1048576'}))
        stream.write('A' * 1048576)
        conn.sock.send_rst()

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-client_fail_summ'] == 1


def test_watch_client_close_backend_keepalive(ctx):
    ctx.start_backend(StreamingConfig(keepalive=True))
    ctx.start_balancer(ProxyConfig(
        backend_timeout='10s',
        keepalive_count=1,
        watch_client_close=True,
    ))

    ctx.perform_request(http.request.get())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())

    req_time = ctx.backend.state.streams.get(timeout=30).duration
    assert req_time < datetime.timedelta(seconds=5)

    unistat = ctx.get_unistat()
    assert unistat['report-default-succ_summ'] == 1
    assert unistat['report-default-client_fail_summ'] == 1
