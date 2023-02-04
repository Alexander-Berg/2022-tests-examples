import pytest
import random
import zlib

from configs import DefaultConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig, ChunkedConfig
from balancer.test.util.stdlib.multirun import Multirun


DATA = '123456789' * 10000


def decode_gzip(data):
    return zlib.decompress(data, 16 + zlib.MAX_WBITS)


def encode_gzip(data):
    return zlib.compress(data)


def test_compression_enabled(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA)))
    ctx.start_balancer(DefaultConfig())

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip'}))
    asserts.status(r, 200)
    assert r.headers['content-encoding'] == ['gzip']

    assert decode_gzip(r.data.content) == DATA


def test_compression_disabled(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA)))
    ctx.start_balancer(DefaultConfig(enable_compression=False))

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip'}))
    asserts.status(r, 200)
    assert not r.headers['content-encoding']

    assert r.data.content == DATA


def test_compression_codecs(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA)))
    ctx.start_balancer(DefaultConfig(compression_codecs='br'))

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip'}))
    asserts.status(r, 200)
    assert not r.headers['content-encoding']

    assert r.data.content == DATA


def test_compression_order(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA)))
    ctx.start_balancer(DefaultConfig(compression_codecs='br, gzip'))

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip,br'}))
    asserts.status(r, 200)
    assert r.headers['content-encoding'] == ['br']


def test_compression_invalid_encoding(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA)))
    ctx.start_balancer(DefaultConfig())

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'xxx'}))
    asserts.status(r, 200)
    assert not r.headers['content-encoding']

    assert r.data.content == DATA


@pytest.mark.parametrize('status_code', [204, 304])
def test_compression_disabled_status_code(ctx, status_code):
    ctx.start_backend(SimpleConfig(http.response.custom(status_code, 'no')))
    ctx.start_balancer(DefaultConfig())

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip'}))
    asserts.status(r, status_code)
    assert not r.headers['content-encoding']

    assert not r.data.content


def test_compression_disabled_header(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(data=DATA, headers={'Content-Encoding': 'gzip'})))
    ctx.start_balancer(DefaultConfig())

    r = ctx.perform_request(http.request.get(headers={'Accept-Encoding': 'gzip'}))
    asserts.status(r, 200)
    assert r.headers['content-encoding'] == ['gzip']

    assert r.data.content == DATA


def test_compression_backend_error(ctx):
    ctx.start_backend(ChunkedConfig(http.response.ok(data=[DATA] * 2), chunk_timeout=10))
    ctx.start_balancer(DefaultConfig())

    ctx.perform_request_xfail(http.request.get(headers={'Accept-Encoding': 'gzip'}))

    unistat = ctx.get_unistat()
    assert unistat['report-service_total-backend_fail_summ'] == 1


def test_compression_client_error(ctx):
    data = ''.join(chr(random.randint(0, 127)) for _ in xrange(100000))

    ctx.start_backend(SimpleDelayedConfig(http.response.ok(data=data), response_delay=5))
    ctx.start_balancer(DefaultConfig())

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.get(headers={'Accept-Encoding': 'gzip'}).to_raw_request())

    for run in Multirun(sum_delay=20):
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-service_total-client_fail_summ'] == 1

    assert ctx.backend.state.get_request()


def test_decompression_enabled(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig(enable_decompression=True))

    r = ctx.perform_request(http.request.post(headers={'Content-Encoding': 'gzip'}, data=encode_gzip(DATA)))
    asserts.status(r, 200)

    assert ctx.backend.state.get_request().data.content == DATA


def test_decompression_disabled(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig())

    data_encoded = encode_gzip(DATA)

    r = ctx.perform_request(http.request.post(headers={'Content-Encoding': 'gzip'}, data=data_encoded))
    asserts.status(r, 200)

    assert ctx.backend.state.get_request().data.content == data_encoded


def test_decompression_invalid_encoding(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig(enable_decompression=True))

    data_encoded = encode_gzip(DATA)

    r = ctx.perform_request(http.request.post(headers={'Content-Encoding': 'xxx'}, data=data_encoded))
    asserts.status(r, 200)

    assert ctx.backend.state.get_request().data.content == data_encoded


def test_decompression_broken_input(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig(enable_decompression=True))

    data = '\x00\x00'
    ctx.perform_request_xfail(http.request.post(headers={'Content-Encoding': 'gzip'}, data=data))

    unistat = ctx.get_unistat()
    assert unistat['report-service_total-fail_summ'] == 1  # TODO(smalukav): client fail


def test_decompression_backend_error(ctx):
    ctx.start_backend(SimpleDelayedConfig(http.response.ok(), response_delay=15))
    ctx.start_balancer(DefaultConfig(enable_decompression=True))

    data_encoded = encode_gzip(DATA)

    ctx.perform_request_xfail(http.request.post(headers={'Content-Encoding': 'gzip'}, data=data_encoded))

    unistat = ctx.get_unistat()
    assert unistat['report-service_total-backend_attempt_summ'] == 1
    assert unistat['report-service_total-backend_fail_summ'] == 1


def test_decompression_client_error(ctx):
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(DefaultConfig(enable_decompression=True))

    data_encoded = encode_gzip(DATA)
    request = http.request.custom('POST', headers={'Content-Encoding': 'gzip', 'content-length': 1024})
    request = str(request.to_raw_request()) + data_encoded

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write(request)

    for run in Multirun(sum_delay=20):
        with run:
            unistat = ctx.get_unistat()
            assert unistat['report-service_total-fail_summ'] == 1  # TODO(smalukav): client fail

    assert unistat['report-service_total-backend_attempt_summ'] == 1
