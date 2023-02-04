# -*- coding: utf-8 -*-
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef import http2


def test_unknown_conn_frame(ctx):
    """
    Balancer must ignore connection frames of unknown type
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=0, data='12345'),
    )
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_unknown_idle_stream_frame(ctx):
    """
    Balancer must ignore stream frames of unknown type
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=1, data='12345'),
    )
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_unknown_opened_stream_frame(ctx):
    """
    Balancer must ignore stream frames of unknown type
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=None, data='12345'),
    )
    stream.write_chunk('A' * 10, end_stream=True)
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_unknown_opened_stream_frame_end_stream_flag(ctx):
    """
    Balancer must not treat a frame of unknown type with END_STREAM flag set
    as the end of currently opened stream
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(
        frames.Unknown(length=None, frame_type=0xA, flags=flags.END_STREAM, reserved=0, stream_id=None, data='12345'),
    )
    stream.write_chunk(data, end_stream=True)
    resp = stream.read_message().to_response()
    req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.content(req, data)


def test_unknown_closed_stream_frame(ctx):
    """
    Balancer must ignore stream frames of unknown type
    """
    conn = common.start_and_connect(ctx)
    resp = conn.perform_request(http2.request.get())
    conn.write_frame(
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=1, data='12345'),
    )
    asserts.status(resp, 200)
