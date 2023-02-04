import pytest
import time

from common import start_balancer, CLIENT_HEADERS_CRY, BACKEND_HEADERS, CRYPROX_HEADERS

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, HTTPServerHandler, HTTPConfig
from balancer.test.util.proto.http.stream import HTTPReaderException


class StreamingHandler(HTTPServerHandler):
    def handle_request(self, stream):
        stream.read_request_line()
        stream.read_headers()

        if self.config.headers:
            stream.write_line('HTTP/1.1 200 OK')
            stream.write_headers(self.config.headers)
            if self.config.content_length:
                stream.write_header('content-length', str(self.config.content_length))
            stream.end_headers()

            if self.config.data:
                stream.write(self.config.data)

        if self.config.read:
            while self.sock.recv():
                pass
        else:
            time.sleep(60)

        self.force_close()


class StreamingConfig(HTTPConfig):
    HANDLER_TYPE = StreamingHandler

    def __init__(self, headers=None, content_length=None, data=None, read=True):
        self.headers = headers
        self.content_length = content_length
        self.data = data
        self.read = read
        super(StreamingConfig, self).__init__()


def test_rewind_cryprox_conn_error(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 524288)))
    ctx.start_fake_backend(name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 1
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-conn_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_rewind_cryprox_http_error(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 524288)))
    ctx.start_backend(SimpleConfig(http.response.service_unavailable()), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 1
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_rewind_cryprox_write_timeout(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(StreamingConfig(), name='cryprox_backend')

    start_balancer(ctx, cryprox_backend_timeout='1s')

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 1
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_rewind_cryprox_read_timeout(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 524288)))
    ctx.start_backend(StreamingConfig(read=False), name='cryprox_backend')

    start_balancer(ctx, cryprox_backend_timeout='1s')

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 1
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_no_rewind_cryprox_write_timeout(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 524288)))
    ctx.start_backend(StreamingConfig(headers=CRYPROX_HEADERS, content_length=1048576, data='B' * 524288), name='cryprox_backend')

    start_balancer(ctx, cryprox_backend_timeout='1s')

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 1
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_no_rewind_cryprox_read_timeout(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 524288)))
    ctx.start_backend(StreamingConfig(read=False, headers=CRYPROX_HEADERS, content_length=1048576, data='B' * 524288), name='cryprox_backend')

    start_balancer(ctx, cryprox_backend_timeout='1s')

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 1
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_no_rewind_buffer_overflow(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS, data='A' * 2097152)))
    ctx.start_backend(StreamingConfig(), name='cryprox_backend')

    start_balancer(ctx)

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 1
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 1
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_service_error_before_rewird(ctx):
    ctx.start_backend(StreamingConfig(headers=BACKEND_HEADERS, content_length=1048576, data='A' * 524288))
    ctx.start_backend(StreamingConfig(), name='cryprox_backend')

    start_balancer(ctx, service_backend_timeout='1s')

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 0
    assert unistat['report-service_backend-backend_fail_summ'] == 1


def test_service_error_after_rewird(ctx):
    ctx.start_backend(StreamingConfig(headers=BACKEND_HEADERS, content_length=1048576, data='A' * 524288))
    ctx.start_fake_backend(name='cryprox_backend')

    start_balancer(ctx, service_backend_timeout='2s')

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 1
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 1
    assert unistat['report-service_backend-backend_fail_summ'] == 1


def test_service_error_no_rewird(ctx):
    ctx.start_backend(StreamingConfig(headers=BACKEND_HEADERS, content_length=1048576, data='A' * 524288))
    ctx.start_backend(StreamingConfig(headers=CRYPROX_HEADERS, content_length=1048576, data='B' * 524288), name='cryprox_backend')

    start_balancer(ctx, service_backend_timeout='1s')

    with pytest.raises(HTTPReaderException):
        ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['cryprox-rewind_buffer_overflow_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 0
    assert unistat['report-service_backend-backend_fail_summ'] == 1
