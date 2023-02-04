# -*- coding: utf-8 -*-
import time
from datetime import timedelta

import pytest

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.stdlib.multirun import Multirun

from configs import BufferConfig

from balancer.test.util.predef.handler.server.http import ChunkedConfig, SimpleHandler, SimpleConfig
from balancer.test.util.predef import http


class SmallBufHandler(SimpleHandler):
    def handle_parsed_request(self, raw_request, stream):
        self.sock.set_send_buffer_size(1)
        super(SmallBufHandler, self).handle_parsed_request(raw_request, stream)


class SmallBufConfig(SimpleConfig):
    HANDLER_TYPE = SmallBufHandler

    def __init__(self):
        super(SmallBufConfig, self).__init__(http.response.ok(data='A' * 200000))


class BufContext(object):
    def start_buf_backend(self, config):
        return self.start_backend(config)

    def start_buf_balancer(self, **balancer_kwargs):
        return self.start_balancer(BufferConfig(
            backend_port=self.backend.server_config.port, **balancer_kwargs))


buf_ctx = mod_ctx.create_fixture(BufContext)


@pytest.mark.xfail(reason="broken")
def test_dying_client(buf_ctx):
    """
    SEPE-8036
    Клиент задает запрос и умирает
    Балансер должен записать в логи об ошибке отправки данных клиенту
    """
    pytest.fail()  # explicitly failing test to prevent flaps
    buf_ctx.start_buf_backend(ChunkedConfig(
        response=http.response.ok(data=['A' * 10] * 2), chunk_timeout=0.5))
    balancer = buf_ctx.start_buf_balancer()

    conn = buf_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    conn.sock.close()

    for run in Multirun():
        with run:
            errorlog = buf_ctx.manager.fs.read_file(balancer.config.errorlog)
            accesslog = buf_ctx.manager.fs.read_file(balancer.config.accesslog)

            assert '(Input/output error)' in errorlog
            assert 'client system_error EIO' in accesslog


def test_full_buffer(buf_ctx):
    """
    Если буфер переполняется, то балансер не должен читать данные от backend-а
    """
    timeout = 28
    buf_ctx.start_buf_backend(SmallBufConfig())
    buf_ctx.start_buf_balancer(
        backend_timeout='1000s',
        buffer_=1,
        socket_buffer=1)

    conn = buf_ctx.create_http_connection(timeout=1000)
    conn.sock.set_recv_buffer_size(1)
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    time.sleep(timeout)

    stream.read_response()
    stream_info = buf_ctx.backend.state.streams.get()

    assert stream_info.duration.total_seconds() > timeout


def test_not_full_buffer(buf_ctx):
    """
    Если ответ backend-а полностью помещается в буфер, то балансер должен его прочитать,
    даже если клиент медленно вычитывает данные
    """
    timeout = 28
    buf_ctx.start_buf_backend(SmallBufConfig())
    buf_ctx.start_buf_balancer(
        backend_timeout='50s',
        buffer_=1024 * 1024,
        socket_buffer=1)

    conn = buf_ctx.create_http_connection(timeout=50)
    conn.sock.set_recv_buffer_size(1)
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    time.sleep(timeout)

    stream.read_response()
    stream_info = buf_ctx.backend.state.streams.get()

    assert stream_info.duration.total_seconds() < timeout


def test_not_full_buffer_workers(buf_ctx):
    """
    SEPE-8565
    Если ответ backend-а полностью помещается в буфер, то балансер должен его прочитать,
    даже если клиент медленно вычитывает данные
    Тест на многочилдовую конфигурацию
    """
    timeout = 28
    buf_ctx.start_buf_backend(SmallBufConfig())
    buf_ctx.start_buf_balancer(
        backend_timeout='50s',
        buffer_=1024 * 1024,
        socket_buffer=1,
        workers=2)

    conn1 = buf_ctx.create_http_connection(timeout=50)
    conn2 = buf_ctx.create_http_connection(timeout=50)
    conn1.sock.set_recv_buffer_size(1)
    stream1 = conn1.create_stream()
    stream1.write_request(http.request.raw_get())
    conn2.sock.set_recv_buffer_size(1)
    stream2 = conn2.create_stream()
    stream2.write_request(http.request.raw_get())
    time.sleep(timeout)

    stream1.read_response()
    stream2.read_response()
    stream_info1 = buf_ctx.backend.state.streams.get()
    stream_info2 = buf_ctx.backend.state.streams.get()

    assert stream_info1.duration.total_seconds() < timeout
    assert stream_info2.duration.total_seconds() < timeout


def test_backend_released(buf_ctx):
    """
    Если ответ backend-а полностью помещается в буфер, то балансер должен его прочитать и отпустить соединение,
    даже если клиент медленно вычитывает данные
    """
    buf_ctx.start_buf_backend(SmallBufConfig())
    buf_ctx.start_buf_balancer(backend_timeout='50s', buffer_=1024 * 1024, socket_buffer=1)

    tcpdump = buf_ctx.manager.tcpdump.start(buf_ctx.backend.server_config.port)
    conn = buf_ctx.create_http_connection(timeout=100)
    conn.sock.set_recv_buffer_size(1)
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    time.sleep(28)
    buf_ctx.create_http_connection(timeout=50).perform_request(http.request.get())
    time.sleep(2)
    stream.read_response()
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sess = tcpdump.get_sessions()
            assert len(sess) == 1, 'More than one session has been enabled'


@pytest.mark.xfail(reason='extremly flaky')
def test_response(buf_ctx):
    """
    Балансер должен отправлять данные клиенту по мере их поступления от backend-а, а не после того,
    как прочитает весь ответ
    """
    pytest.fail()  # explicitly failing test to prevent flaps
    max_delta = timedelta(seconds=0.5)
    buf_ctx.start_buf_backend(SmallBufConfig())
    buf_ctx.start_buf_balancer(backend_timeout='50s', buffer_=1024 * 1024, socket_buffer=1)

    tcpdump = buf_ctx.manager.tcpdump.start(buf_ctx.balancer.config.port)
    conn = buf_ctx.create_http_connection(timeout=50)
    conn.sock.set_recv_buffer_size(1)
    conn.perform_request(http.request.get())
    stream = conn.create_stream()
    stream.write_request(http.request.raw_get())
    stream_info = buf_ctx.backend.state.streams.get()
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) > 0
            server_packets = sessions[0].server_packets
            deltas = map(lambda (x, y): y - x, zip(server_packets[:-1], server_packets[1:]))

            assert server_packets[0] - stream_info.start_time < max_delta
            deltas_less_than_max = all(map(lambda x: x < max_delta, deltas))
            assert deltas_less_than_max
