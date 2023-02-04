# -*- coding: utf-8 -*-
import pytest
from configs import HTTP2Config
from balancer.test.util.balancer import asserts
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2 import message as mod_msg
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.proto.http2.framing.stream import NoHTTP2FrameException
import balancer.test.util.proto.http2.hpack._hpack as hpack
from balancer.test.util.predef.handler.server.http import HTTPConfig, SimpleConfig

BACKEND_ERROR_EMPTY_RESP = errors.INTERNAL_ERROR
BACKEND_ERROR_PARTIAL_RESP = errors.INTERNAL_ERROR

DEFAULT_WINDOW_SIZE = 65535
DEFAULT_MAX_FRAME_SIZE = 2 ** 14
SCHEME = 'https'
PATH = '/Led/Zeppelin'
SINGLE_DATA = 'Queen'
DATA = ['Pink', 'Floyd']
CONTENT = ''.join(DATA)
HEADERS = {
    'led': 'Zeppelin',
    'pink': 'Floyd',
    'black': 'Sabbath',
}
CUR_AUTH = 'localhost'
# CUR_AUTH = 'www.google.ru'


def create_conn(ctx, setup=True, ssl=True):
    host = 'localhost'
    port = ctx.balancer.config.port
    # host = 'google.ru'
    # port = 443
    if ssl:
        conn = ctx.manager.connection.http2.create_pyssl(
            host=host,
            port=port,
            ssl_options=SSLClientOptions(alpn='h2'),
        )
    else:
        conn = ctx.manager.connection.http2.create(
            host=host,
            port=port,
        )
    if setup:
        conn.write_preface()
        wait_settings(conn)  # preface SETTINGS
    return conn


def encode_headers(headers):
    return hpack.Encoder().encode(mod_msg.RawHTTP2Message.build_headers(headers))


GET_METHOD = 2                 # :method: GET
ROOT_PATH = 4                  # :path: /
AUTH_LOCAL = (1, CUR_AUTH)     # :authority
HTTPS_SCHEME = 7               # :scheme: https
SIMPLE_REQ = [GET_METHOD, ROOT_PATH, AUTH_LOCAL, HTTPS_SCHEME]
SIMPLE_REQ_ENCODED = encode_headers(SIMPLE_REQ)
HEADERS_ENCODED = encode_headers(HEADERS)


STATIC_TABLE_SIZE = 61
DEFAULT_HEADER_TABLE_SIZE = 4096

HPACK_NAME = 'led'
HPACK_VALUE = 'Zeppelin'
SECURE_NAME = 'pink'
SECURE_VALUE = 'Floyd'


def assert_conn_error(ctx, conn, err_code, reason, last_stream_id=None):
    frame = conn.wait_frame(frames.Goaway)
    assert frame.error_code == err_code
    if last_stream_id is not None:
        assert frame.last_stream_id == last_stream_id
    asserts.is_closed(conn.sock)
    unistat = ctx.get_unistat()
    assert unistat["http2-go_away_send-{}_summ".format(errors.ERROR_LIST[err_code])] == 1
    if errors.NO_ERROR == err_code:
        assert unistat["http2-conn_no_error_send-{}_summ".format(reason)] == 1
    elif errors.PROTOCOL_ERROR == err_code:
        assert unistat["http2-conn_protocol_error_send-{}_summ".format(reason)] == 1
    elif errors.INTERNAL_ERROR == err_code:
        assert unistat["http2-conn_internal_error_send-{}_summ".format(reason)] == 1
    elif errors.FLOW_CONTROL_ERROR == err_code:
        assert unistat["http2-conn_flow_control_error_send-{}_summ".format(reason)] == 1
    elif errors.STREAM_CLOSED == err_code:
        assert unistat["http2-conn_stream_closed_error_send-{}_summ".format(reason)] == 1
    elif errors.COMPRESSION_ERROR == err_code:
        assert unistat["http2-compression_error_send-{}_summ".format(reason)] == 1
    assert unistat["http2-unknown_go_away_send-{}_summ".format(errors.ERROR_LIST[err_code])] == 0


def assert_conn_protocol_error(ctx, conn, reason, last_stream_id=None):
    assert_conn_error(ctx, conn, errors.PROTOCOL_ERROR, reason, last_stream_id)


def assert_stream_error(ctx, stream, err_code, reason, cnt=1, cnt_err=None):
    if cnt_err is None:
        cnt_err = cnt
    with pytest.raises(errors.StreamError) as exc_info:
        while True:
            stream.read_frame()

    assert exc_info.value.error_code == err_code
    unistat = ctx.get_unistat()
    assert unistat["http2-rst_stream_send-{}_summ".format(errors.ERROR_LIST[err_code])] == cnt_err
    if errors.NO_ERROR == err_code:
        assert unistat["http2-stream_no_error_send-{}_summ".format(reason)] == cnt
    elif errors.PROTOCOL_ERROR == err_code:
        assert unistat["http2-stream_protocol_error_send-{}_summ".format(reason)] == cnt
    elif errors.INTERNAL_ERROR == err_code:
        assert unistat["http2-stream_internal_error_send-{}_summ".format(reason)] == cnt
    elif errors.FLOW_CONTROL_ERROR == err_code:
        assert unistat["http2-stream_flow_control_error_send-{}_summ".format(reason)] == cnt
    elif errors.STREAM_CLOSED == err_code:
        assert unistat["http2-stream_stream_closed_error_send-{}_summ".format(reason)] == cnt
    elif errors.REFUSED_STREAM == err_code:
        assert unistat["http2-refused_stream_error_send-{}_summ".format(reason)] == cnt
    assert unistat["http2-unknown_rst_stream_send-{}_summ".format(errors.ERROR_LIST[err_code])] == 0


def assert_stream_protocol_error(ctx, stream, reason, cnt="1"):
    assert_stream_error(ctx, stream, errors.PROTOCOL_ERROR, reason, cnt)


def assert_no_error(conn):
    with pytest.raises(NoHTTP2FrameException):
        while True:
            conn.read_frame()


def assert_no_frame(conn, frame_type):
    with pytest.raises(NoHTTP2FrameException):
        conn.wait_frame(frame_type)


def assert_ok_streams(streams, content=None):
    responses = list()
    for stream in streams:
        responses.append(stream.read_message().to_response())

    for resp in responses:
        asserts.status(resp, 200)
        if content is not None:
            asserts.content(resp, content)


def start_all(ctx, backend=None, **balancer_kwargs):  # backend config or backend response
    if isinstance(backend, HTTPConfig):
        ctx.start_backend(backend)
    elif backend is None:
        ctx.start_backend(SimpleConfig())
    else:
        ctx.start_backend(SimpleConfig(backend))
    ctx.start_balancer(HTTP2Config(certs_dir=ctx.certs.root_dir, **balancer_kwargs))


def start_and_connect(ctx, backend=None, ssl=True, **balancer_kwargs):
    start_all(ctx, backend, **balancer_kwargs)
    conn = create_conn(ctx, ssl=ssl)
    return conn


def wait_settings(conn):
    return conn.wait_frame(frames.Settings)


ACK_SETTINGS_FRAME = frames.Settings(
    length=None, flags=flags.ACK, reserved=0, data=[],
)


def build_settings(params):
    return frames.Settings(
        length=None, flags=0, reserved=0, data=[
            frames.Parameter(identifier, value) for (identifier, value) in params
        ],
    )
