# -*- coding: utf-8 -*-
import pytest
import time
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.config import parametrize_thread_mode
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef.handler.server.http import SimpleDelayedConfig, ThreeModeConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'data',
        'rst_stream',
        'window_update',
        'continuation',
    ]
)
def test_unexpected_frame_idle_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to idle stream
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_frame(frame, force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
def test_client_push_promise(ctx, thread_mode):
    """
    Client sends PUSH_PROMISE frame
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(frames.PushPromise(
        length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
        push_promise_reserved=0, promised_stream_id=3, data=common.HEADERS_ENCODED
    ), force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize('has_continuation', [False, True], ids=['after_headers', 'after_continuation'])
@pytest.mark.parametrize(
    'frame',
    [
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Priority(flags=0, reserved=0, stream_id=None, exclusive=0, stream_dependency=0, weight=1),
        frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1),
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=None, data='12345'),
    ],
    ids=[
        'data',
        'headers',
        'priority',
        'rst_stream',
        'window_update',
        'unknown',
    ]
)
def test_unexpected_same_stream_frame_headers_block(ctx, thread_mode, frame, has_continuation):
    """
    Client sends non-continuation frame from the same stream in the middle of headers block
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_frame(
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
    )
    if has_continuation:
        stream.write_frame(
            frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
        )
    stream.write_frame(frame, force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize('has_continuation', [False, True], ids=['after_headers', 'after_continuation'])
@pytest.mark.parametrize(
    'frame',
    [
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Priority(flags=0, reserved=0, stream_id=None, exclusive=0, stream_dependency=0, weight=1),
        frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
        frames.Unknown(length=None, frame_type=0xA, flags=0, reserved=0, stream_id=None, data='12345'),
    ],
    ids=[
        'data',
        'headers',
        'priority',
        'rst_stream',
        'window_update',
        'continuation',
        'unknown',
    ]
)
def test_unexpected_other_stream_frame_headers_block(ctx, thread_mode, frame, has_continuation):
    """
    Client sends non-continuation frame from the other stream in the middle of headers block
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream1 = conn.create_stream()
    stream1.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream2 = conn.create_stream()
    stream2.write_frame(
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
    )
    if has_continuation:
        stream2.write_frame(
            frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
        )
    stream1.write_frame(frame, force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize('has_continuation', [False, True], ids=['after_headers', 'after_continuation'])
@pytest.mark.parametrize(
    'frame',
    [
        common.build_settings([(frames.Parameter.MAX_FRAME_SIZE, 2 ** 15)]),
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
def test_unexpected_conn_frame_headers_block(ctx, thread_mode, frame, has_continuation):
    """
    Client sends non-continuation connection frame in the middle of headers block
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_frame(
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
    )
    if has_continuation:
        stream.write_frame(
            frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
        )
    conn.write_frame(frame, force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'continuation',
    ]
)
def test_unexpected_frame_opened_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to opened stream
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    time.sleep(1)
    stream.write_frame(frame, force=True)
    err_request = ctx.backend.state.read_errors.get().raw_message
    asserts.method(err_request, 'GET')
    asserts.path(err_request, '/')
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'continuation',
    ]
)
def test_unexpected_frame_opened_stream_after_data(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to opened stream
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    data = 'A' * 10
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_chunk(data)
    time.sleep(2)
    stream.write_frame(frame, force=True)
    err_request = ctx.backend.state.read_errors.get().raw_message
    asserts.method(err_request, 'GET')
    asserts.path(err_request, '/')
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_STREAM, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'headers_es',
        'data',
        'continuation',
    ]
)
def test_unexpected_frame_half_closed_client_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to half-closed (by client) stream
    Balancer must send GOAWAY(STREAM_CLOSED) to client and close connection
    """
    conn = common.start_and_connect(
        ctx,
        SimpleDelayedConfig(http.response.ok(), response_delay=2),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_message(http2.request.get(authority=common.CUR_AUTH).to_raw_request())
    stream.write_frame(frame, force=True)
    if isinstance(frame, frames.Continuation) or (
            isinstance(frame, frames.Headers) and not (frame.flags & flags.END_STREAM)
    ):
        common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")
    else:
        common.assert_conn_error(ctx, conn, errors.STREAM_CLOSED, "ClosedByClient")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_STREAM, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'headers_es',
        'data',
        'continuation',
    ]
)
def test_unexpected_frame_closed_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to closed stream
    Balancer must send GOAWAY(STREAM_CLOSED) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_message(http2.request.get(authority=common.CUR_AUTH).to_raw_request())
    stream.read_message()
    stream.write_frame(frame, force=True)
    if isinstance(frame, frames.Continuation) or (
            isinstance(frame, frames.Headers) and not (frame.flags & flags.END_STREAM)
    ):
        common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")
    else:
        common.assert_conn_error(ctx, conn, errors.STREAM_CLOSED, "ClosedByClient")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'continuation',
    ]
)
def test_unexpected_frame_after_reset_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type after balancer sends RST_STREAM
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(
        ctx,
        ThreeModeConfig(
            prefix=1, first=1, second=0, response=http.response.ok(data=common.DATA)
        ),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_post().headers, end_stream=False)
    common.assert_stream_error(ctx, stream, common.BACKEND_ERROR_PARTIAL_RESP, "ResponseSendError")
    stream.write_frame(frame, force=True)
    common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")


@parametrize_thread_mode
@pytest.mark.parametrize(
    'frame',
    [
        frames.Headers(length=None, flags=0, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_HEADERS, reserved=0, stream_id=None,
                       data=common.SIMPLE_REQ_ENCODED),
        frames.Headers(length=None, flags=flags.END_STREAM, reserved=0, stream_id=None, data=common.SIMPLE_REQ_ENCODED),
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=common.HEADERS_ENCODED),
    ],
    ids=[
        'headers',
        'headers_eh',
        'headers_es',
        'data',
        'continuation',
    ]
)
def test_unexpected_frame_after_client_reset_stream(ctx, thread_mode, frame):
    """
    Client sends frame of unexpected type to closed stream
    Balancer must send GOAWAY(STREAM_CLOSED) to client and close connection
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_chunk('A' * 10)
    stream.reset(errors.PROTOCOL_ERROR)
    stream.write_frame(frame, force=True)
    if isinstance(frame, frames.Continuation) or (
            isinstance(frame, frames.Headers) and not (frame.flags & flags.END_STREAM)
    ):
        common.assert_conn_protocol_error(ctx, conn, "UnexpectedFrame")
    else:
        common.assert_conn_error(ctx, conn, errors.STREAM_CLOSED, "ClosedByClient")


@parametrize_thread_mode
def test_data_frame_just_after_reset_stream(ctx, thread_mode):
    """
    Client sends DATA frame just after balancer sends RST_STREAM
    Balancer must not treat it as an error an must update connection window size
    """
    conn = common.start_and_connect(
        ctx,
        ThreeModeConfig(
            prefix=1, first=1, second=0, response=http.response.ok(data=common.DATA)
        ),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_post().headers, end_stream=False)
    common.assert_stream_error(ctx, stream, common.BACKEND_ERROR_PARTIAL_RESP, "ResponseSendError")
    stream.write_frame(
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        force=True,
    )
    common.assert_no_error(conn)
    # TODO: check that window size is updated


@parametrize_thread_mode
def test_data_frame_much_later_reset_stream(ctx, thread_mode):
    """
    Client sends DATA frame mush later than balancer sends RST_STREAM
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    We intentionally avoid this potentially harmful behavior and send RST_STREAM(STREAM_CLOSED) instead
    """
    conn = common.start_and_connect(
        ctx,
        ThreeModeConfig(
            prefix=1, first=1, second=0, response=http.response.ok(data=common.DATA)
        ),
        streams_closed_max=0,
        thread_mode=thread_mode,
    )

    stream1 = conn.create_stream()
    stream1.write_headers(http2.request.raw_post().headers, end_stream=False)
    common.assert_stream_error(ctx, stream1, common.BACKEND_ERROR_PARTIAL_RESP, "ResponseSendError")

    # we need a second stream to flush the first one from the garbage collection queue
    stream2 = conn.create_stream()
    stream2.write_headers(http2.request.raw_post().headers, end_stream=True)
    common.assert_stream_error(ctx, stream2, common.BACKEND_ERROR_PARTIAL_RESP, "BackendError", cnt_err=2)
    stream1.write_frame(
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data='12345'),
        force=True,
    )
    # Here should have been connection error according to the spec
    common.assert_stream_error(ctx, stream1, errors.STREAM_CLOSED, "AlreadyErased")
