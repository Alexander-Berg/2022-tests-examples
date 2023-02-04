# -*- coding: utf-8 -*-
import pytest
import common
from balancer.test.util import asserts
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2 import message as mod_msg
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef import http2


@pytest.mark.parametrize(
    'frame',
    [
        frames.Settings(
            length=5, flags=0, reserved=0, data=[
                frames.Parameter(frames.Parameter.MAX_FRAME_SIZE, 2 ** 15),
            ],
        ),
        frames.Settings(
            length=7, flags=0, reserved=0, data=[
                frames.Parameter(frames.Parameter.MAX_FRAME_SIZE, 2 ** 15),
            ],
        ),
        (frames.Ping(flags=0, reserved=0, data='01234567'), 7),
        (frames.Ping(flags=0, reserved=0, data='01234567'), 9),
        (frames.WindowUpdate(flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=1), 3),
        (frames.WindowUpdate(flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=1), 5),
        frames.Headers(length=0, flags=flags.PADDED, reserved=0, stream_id=1, pad_length=0, data='', padding=''),
        frames.Headers(length=4, flags=flags.PRIORITY, reserved=0, stream_id=1,
                       exclusive=0, stream_dependency=0, weight=1, data=''),
    ],
    ids=[
        'settings_lt',
        'settings_gt',
        'ping_lt',
        'ping_gt',
        'window_update_lt',
        'window_update_gt',
        'headers_padded',
        'headers_priority',
    ]
)
def test_conn_frame_size_error(ctx, frame):
    """
    Client sends frame with stream identifier of 0 with wrong frame length
    Balancer must send GOAWAY(FRAME_SIZE_ERROR) to client and close connection
    """
    if isinstance(frame, tuple):  # TODO: generic kwargs in frames constructors
        frame, length = frame
        frame.length = length
    conn = common.start_and_connect(ctx)
    conn.write_frame(frame)
    common.assert_conn_error(ctx, conn, errors.FRAME_SIZE_ERROR, None)


@pytest.mark.parametrize(
    'frame',
    [
        (frames.Priority(flags=0, reserved=0, stream_id=None, exclusive=0, stream_dependency=0, weight=1), 4),
        (frames.Priority(flags=0, reserved=0, stream_id=None, exclusive=0, stream_dependency=0, weight=1), 6),
        (
            frames.WindowUpdate(
                flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1
            ), 3
        ),
        (
            frames.WindowUpdate(
                flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1
            ), 5
        ),
        (frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR), 3),
        (frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR), 5),
    ],
    ids=[
        'priority_lt',
        'priority_gt',
        'window_update_lt',
        'window_update_gt',
        'rst_stream_lt',
        'rst_stream_gt',
    ]
)
def test_stream_frame_size_error(ctx, frame):
    """
    Client sends frame with wrong frame length
    Balancer must send GOAWAY(FRAME_SIZE_ERROR) to client and close connection
    """
    if isinstance(frame, tuple):  # TODO: generic kwargs in frames constructors
        frame, length = frame
        frame.length = length
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(frame, force=True)
    # according to rfc this error must be treated as a stream error
    # but google and nginx treat it as a connection error
    common.assert_conn_error(ctx, conn, errors.FRAME_SIZE_ERROR, None)


@pytest.mark.parametrize(
    'frame',
    [
        frames.Settings(
            length=None, flags=0, reserved=0, data=[
                frames.Parameter(frames.Parameter.MAX_FRAME_SIZE, 2 ** 15),
            ],
        ),
        frames.Ping(flags=0, reserved=0, data='01234567'),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=1),
        frames.Headers(length=None, flags=0, reserved=0, stream_id=1, data=''),
    ],
    ids=[
        'settings',
        'ping',
        'window_update',
        'headers',
    ]
)
def test_conn_frame_size_error_exceed_max_size(ctx, frame):
    """
    Client sends frame with stream identifier of 0 with frame length greater than SETTINGS_MAX_FRAME_SIZE
    Balancer must send GOAWAY(FRAME_SIZE_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    max_size = conn.server_settings[frames.Parameter.MAX_FRAME_SIZE]
    assert max_size < 2 ** 24 - 1, 'wrong test configuration'
    frame.length = max_size + 1
    conn.write_frame(frame)
    common.assert_conn_error(ctx, conn, errors.FRAME_SIZE_ERROR, None)


@pytest.mark.parametrize(
    'frame',
    [
        frames.Priority(flags=0, reserved=0, stream_id=None, exclusive=0, stream_dependency=0, weight=1),
        frames.WindowUpdate(flags=0, reserved=0, stream_id=None, window_update_reserved=0, window_size_increment=1),
        frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=errors.PROTOCOL_ERROR),
        frames.Data(length=None, flags=0, reserved=0, stream_id=None, data=''),
        frames.Continuation(length=None, flags=0, reserved=0, stream_id=None, data=''),
    ],
    ids=[
        'priority',
        'window_update',
        'rst_stream',
        'data',
        'continuation',
    ]
)
def test_stream_frame_size_error_exceed_max_size(ctx, frame):
    """
    Client sends frame with frame length greater than SETTINGS_MAX_FRAME_SIZE
    Balancer must send GOAWAY(FRAME_SIZE_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    max_size = conn.server_settings[frames.Parameter.MAX_FRAME_SIZE]
    assert max_size < 2 ** 24 - 1, 'wrong test configuration'
    frame.length = max_size + 1
    stream = conn.create_stream()
    if not isinstance(frame, frames.Continuation):
        stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    else:
        data = conn.encoder.encode(mod_msg.RawHTTP2Message.build_headers(common.SIMPLE_REQ))
        stream.write_frame(frames.Headers(
            length=None, flags=flags.END_STREAM, reserved=0, stream_id=None, data=data,
        ))
    stream.write_frame(frame, force=True)
    # according to rfc this error must be treated as a stream error
    # but google and nginx treat it as a connection error
    common.assert_conn_error(ctx, conn, errors.FRAME_SIZE_ERROR, None)


@pytest.mark.parametrize(
    'ack',
    [True, False],
    ids=["noack", "ack"]
)
def test_custom_server_max_frame_size(ctx, ack):
    """
    BALANCER-1973
    BALANCER-2174
    Balancer should respect its initial_window_size promises
    """
    size = common.DEFAULT_MAX_FRAME_SIZE + 1
    conn = common.start_and_connect(
        ctx,
        max_frame_size=size,
        initial_window_size=common.DEFAULT_WINDOW_SIZE
    )

    if ack:
        conn.write_frame(common.ACK_SETTINGS_FRAME)

    stream = conn.create_stream()
    stream.write_message(http2.request.post(data='A' * size).to_raw_request())

    # BALANCER-2174 We should always treat unacked settings in the client's favour
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
