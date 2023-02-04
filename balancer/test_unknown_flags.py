# -*- coding: utf-8 -*-
import pytest
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef.handler.server.http import SimpleDelayedConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


def test_padded_flag_not_padded_frame(ctx):
    """
    Balancer must ignore padded flag if it is not defined for the frame type
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(frames.Settings(
        length=None, flags=flags.PADDED, reserved=0, data=[frames.Parameter(2 ** 16 - 1, 2 ** 32 - 1)],
    ))
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


FULL_FLAGS = 2 ** 8 - 1


def unknown_flags(frame_type):
    result = FULL_FLAGS
    for flag in frame_type.FLAGS:
        result -= flag
    return result


@pytest.mark.parametrize(
    'frame',
    [
        frames.Settings(
            length=None, flags=0, reserved=0, data=[frames.Parameter(2 ** 16 - 1, 2 ** 32 - 1)],
        ),
        frames.Ping(flags=0, reserved=0, data='01234567'),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=1),
        frames.Goaway(
            length=None, flags=0, reserved=0, goaway_reserved=0,
            last_stream_id=0, error_code=errors.PROTOCOL_ERROR, data='',
        ),
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=0, data='12345'),
    ],
    ids=[
        'settings',
        'ping',
        'window_update',
        'goaway',
        'unknown',
    ]
)
def test_unknown_flags_conn_frame(ctx, frame):
    """
    Balancer must ignore unknown flags in connection frames
    """
    frame.flags = unknown_flags(type(frame))
    conn = common.start_and_connect(ctx)
    conn.write_frame(frame)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_unknown_flags_priority(ctx):
    """
    Balancer must ignore unknown flags in PRIORITY frames
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=True)
    stream.write_frame(frames.Priority(
        flags=unknown_flags(frames.Priority), reserved=0, stream_id=None,
        exclusive=0, stream_dependency=0, weight=16,
    ))
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_unknown_flags_headers(ctx):
    """
    Balancer must ignore unknown flags in HEADERS frames
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=unknown_flags(frames.Headers) | flags.END_STREAM | flags.END_HEADERS,
        reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ))
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_continuation_ignore_end_stream(ctx):
    """
    Balancer must ignore END_STREAM flag in CONTINUATION frame
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=0,
        reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ))
    stream.write_frame(frames.Continuation(
        length=None, flags=flags.END_STREAM | flags.END_HEADERS,
        reserved=0, stream_id=None, data=common.HEADERS_ENCODED
    ))
    stream.write_chunk(data, end_stream=True)
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.headers_values(req, common.HEADERS)
    asserts.content(req, data)


def test_unknown_flags_continuation(ctx):
    """
    Balancer must ignore unknown flags in CONTINUATION frames
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=flags.END_STREAM,
        reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED
    ))
    stream.write_frame(frames.Continuation(
        length=None, flags=unknown_flags(frames.Continuation) | flags.END_HEADERS,
        reserved=0, stream_id=None, data=common.HEADERS_ENCODED
    ))
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.headers_values(req, common.HEADERS)


def test_unknown_flags_data(ctx):
    """
    Balancer must ignore unknown flags in PRIORITY frames
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(frames.Data(
        length=None, flags=unknown_flags(frames.Data) | flags.END_STREAM, reserved=0, stream_id=None, data=data
    ))
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.content(req, data)


def test_unknown_flags_stream_window_update(ctx):
    """
    Balancer must ignore unknown flags in WINDOW_UPDATE stream frames
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=True)
    stream.write_frame(frames.WindowUpdate(
        flags=unknown_flags(frames.WindowUpdate), reserved=0, stream_id=None,
        window_update_reserved=0, window_size_increment=1
    ))
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_unknown_flags_reset_stream(ctx):
    """
    Balancer must ignore unknown flags in RST_STREAM frames
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=True)
    stream.write_frame(frames.RstStream(
        flags=unknown_flags(frames.RstStream), reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR
    ), force=True)

    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)
