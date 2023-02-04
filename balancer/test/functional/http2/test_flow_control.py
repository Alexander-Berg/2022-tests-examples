# -*- coding: utf-8 -*-
import pytest
import time
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.proto.http2.framing.stream import NoHTTP2FrameException
from balancer.test.util.predef.handler.server.http import SimpleConfig, ChunkedConfig, MultiActionConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


def build_window_update(size):
    return frames.WindowUpdate(
        flags=0, reserved=0, stream_id=0, window_update_reserved=0, window_size_increment=size,
    )


LONG_DATA_LEN = common.DEFAULT_WINDOW_SIZE + 1
LONG_DATA = 'A' * LONG_DATA_LEN
SMALL_WINDOW_SIZE = 42
SHORT_DATA = 'A' * 1024


def read_incomplete_data(conn):
    result = 0
    with pytest.raises(NoHTTP2FrameException):
        while True:
            frame = conn.wait_frame(frames.Data)
            result += frame.length
    return result


def read_incomplete_payload(conn):
    import collections
    result = collections.defaultdict(str)
    result_len = 0
    with pytest.raises(NoHTTP2FrameException):
        while True:
            frame = conn.wait_frame(frames.Data)
            result[frame.stream_id] += frame.data
            result_len += frame.length
    return result, result_len


def test_init_conn_window(ctx):
    """
    Backend response data length is greater than default connection window size
    Balancer must send a piece of data that fits connection window and wait for WINDOW_UPDATE frame
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=LONG_DATA))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    data_len = read_incomplete_data(conn)
    assert 0 < data_len <= common.DEFAULT_WINDOW_SIZE

    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    resp = stream.read_message()
    asserts.content(resp, LONG_DATA)


def test_conn_window_multi_stream(ctx):
    """
    Client sends two requests
    Single backend response fits in stream window, but two responses don't fit in connection window
    Balancer must use the same connection window for both responses
    """
    data = 'A' * (common.DEFAULT_WINDOW_SIZE / 2 + 1)
    conn = common.start_and_connect(ctx, http.response.ok(data=data))
    stream1 = conn.create_stream()
    stream1.write_message(http2.request.get().to_raw_request())
    stream2 = conn.create_stream()
    stream2.write_message(http2.request.get().to_raw_request())

    result_a, data_len_a = read_incomplete_payload(conn)
    assert 0 < data_len_a <= common.DEFAULT_WINDOW_SIZE
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    result_b, data_len_b = read_incomplete_payload(conn)
    assert data_len_a + data_len_b == 2 * len(data)
    assert result_a[1] + result_b[1] == data
    assert result_a[3] + result_b[3] == data


def test_init_stream_window(ctx):
    """
    Backend response data length is greater than default stream window size
    Balancer must send a piece of data that fits stream window and wait for WINDOW_UPDATE frame
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=LONG_DATA))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    data_len = read_incomplete_data(conn)
    assert 0 < data_len <= common.DEFAULT_WINDOW_SIZE

    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    resp = stream.read_message()
    asserts.content(resp, LONG_DATA)


def test_increase_window_size_new_stream(ctx):
    """
    Client increases initial stream window size
    Balancer must use this size for all new streams
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=LONG_DATA))
    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, 2 * common.DEFAULT_WINDOW_SIZE)]))
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    resp = conn.perform_request(http2.request.get())
    asserts.content(resp, LONG_DATA)


def test_decrease_window_size_new_stream(ctx):
    """
    Client decreases initial stream window size
    Balancer must use this size for all new streams
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=SHORT_DATA))
    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, SMALL_WINDOW_SIZE)]))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    data_len = read_incomplete_data(conn)
    assert 0 < data_len <= SMALL_WINDOW_SIZE

    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    resp = stream.read_message()
    asserts.content(resp, SHORT_DATA)


def test_increase_window_size_current_stream(ctx):
    """
    Client increases initial stream window size
    Balancer must use this size for all currently opened streams
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=LONG_DATA))
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    data_len = read_incomplete_data(conn)
    assert 0 < data_len <= common.DEFAULT_WINDOW_SIZE

    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, 2 * common.DEFAULT_WINDOW_SIZE)]))
    resp = stream.read_message()
    asserts.content(resp, LONG_DATA)


def test_decrease_window_size_current_stream(ctx):
    """
    Client decreases initial stream window size
    Balancer must use this size for all currently opened streams
    """
    conn = common.start_and_connect(ctx, http.response.ok(data=LONG_DATA), backend_timeout=100)
    conn.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    data_len = read_incomplete_data(conn)
    assert 0 < data_len <= common.DEFAULT_WINDOW_SIZE

    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, SMALL_WINDOW_SIZE)]))
    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE - SMALL_WINDOW_SIZE))
    data_len = read_incomplete_data(conn)
    assert data_len == 0

    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    resp = stream.read_message()
    asserts.content(resp, LONG_DATA)


@pytest.mark.parametrize('data_len', [1, 15000], ids=['short', 'long'])
@pytest.mark.parametrize('combined_end', [False, True], ids=['delayed', 'combined'])
def test_balancer_conn_window_update(ctx, data_len, combined_end):
    """
    Balancer must increase connection window after processing a request with body
    """
    data = 'A' * data_len
    conn = common.start_and_connect(ctx, http.response.ok(data='OK'))
    conn.write_frame(common.ACK_SETTINGS_FRAME)
    stream = conn.create_stream()

    req = http2.request.post(data=data).to_raw_request()
    stream.write_headers(req.headers, end_stream=False)
    stream.write_chunk(req.data.chunks[0], end_stream=combined_end)

    frame1 = conn.wait_frame(frames.WindowUpdate)
    assert frame1.stream_id == 0
    assert frame1.window_size_increment == data_len

    # BALANCER-2158 after the stream is closed by the client we can stop sending its WINDOW_UPDATEs
    if not combined_end:
        frame2 = conn.wait_frame(frames.WindowUpdate)
        assert frame2.stream_id == 1
        assert frame2.window_size_increment == data_len
        stream.write_chunk("", end_stream=True)

    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    asserts.content(resp, "OK")


def check_conn_and_stream_updates(conn, stream, data_len):
    frame1 = conn.wait_frame(frames.WindowUpdate)
    frame2 = conn.wait_frame(frames.WindowUpdate)
    if frame1.stream_id == 0:
        conn_update = frame1
        stream_update = frame2
    else:
        conn_update = frame2
        stream_update = frame1

    assert conn_update.stream_id == 0
    assert conn_update.window_size_increment == data_len
    assert stream_update.stream_id == stream.stream_id
    assert stream_update.window_size_increment == data_len


@pytest.mark.parametrize('data_len', [1, 15000], ids=['short', 'long'])
def test_balancer_stream_window_update(ctx, data_len):
    """
    Balancer must increase stream and connection windows after processing a part of request body
    """
    data = 'A' * data_len
    conn = common.start_and_connect(ctx, http.response.ok(data='OK'))
    conn.write_frame(common.ACK_SETTINGS_FRAME)
    stream = conn.create_stream()
    stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
    stream.write_chunk(data, end_stream=False)
    check_conn_and_stream_updates(conn, stream, data_len)


def test_balancer_window_update_padded_data(ctx):
    """
    Balancer must use full padded DATA frame length when sending WINDOW_UPDATE
    """
    data = 'A' * 10
    padding = 'B' * 10
    frame = frames.Data(
        length=None, flags=flags.PADDED, reserved=0, stream_id=None,
        pad_length=None, data=data, padding=padding,
    )
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_frame(frame)
    check_conn_and_stream_updates(conn, stream, frame.length)


def test_balancer_empty_data_frame_no_window_update(ctx):
    """
    Balancer must not send window update when client sends empty DATA frame
    """
    conn = common.start_and_connect(ctx, http.response.ok(data='OK'))
    conn.write_frame(common.ACK_SETTINGS_FRAME)
    stream = conn.create_stream()
    stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
    stream.write_chunk('', end_stream=True)
    common.assert_no_frame(conn, frames.WindowUpdate)
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


@pytest.mark.parametrize(
    'chunk_len',
    [200, common.DEFAULT_MAX_FRAME_SIZE],
    ids=['small_chunk', 'default_chunk']
)
def test_balancer_updates_internal_window(ctx, chunk_len):
    """
    BALANCER-1181
    Balancer must update internal window each time it sends WINDOW_UPDATE frame
    """
    conn = common.start_and_connect(ctx, SimpleConfig(), backend_timeout=1000)
    stream = conn.create_stream()
    stream.write_headers(http2.request.post().to_raw_request().headers, end_stream=False)
    data = LONG_DATA
    while len(data) > chunk_len:
        stream.write_chunk(data[:chunk_len], end_stream=False)
        data = data[chunk_len:]
        # wait for conn and stream WINDOW_UPDATE frames
        conn.wait_frame(frames.WindowUpdate)
        conn.wait_frame(frames.WindowUpdate)
    stream.write_chunk(data, end_stream=True)
    resp = stream.read_message().to_response()
    backend_req = ctx.backend.state.get_request()

    asserts.status(resp, 200)
    asserts.content(backend_req, LONG_DATA)


def test_conn_max_window_size(ctx):
    """
    Client sends conn WINDOW_UPDATE which causes connection window to become 2 ^ 31 - 1
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(build_window_update(2 ** 31 - common.DEFAULT_WINDOW_SIZE - 1))
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_stream_max_window_size(ctx):
    """
    Client sends stream WINDOW_UPDATE which causes stream window to become 2 ^ 31 - 1
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx, ChunkedConfig(http.response.ok(data=common.DATA), chunk_timeout=1))
    stream = conn.create_stream()
    stream.write_headers(http2.request.get().to_raw_request().headers, end_stream=False)
    stream.write_frame(build_window_update(2 ** 31 - common.DEFAULT_WINDOW_SIZE - 1))
    stream.write_chunk('A' * 10, end_stream=True)
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


def test_zero_conn_window_update(ctx):
    """
    Client sends connection WINDOW_UPDATE with 0 window size increment
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(build_window_update(0))
    common.assert_conn_protocol_error(ctx, conn, 'ZeroWindowUpdate')


def test_zero_stream_window_update(ctx):
    """
    Client sends stream WINDOW_UPDATE with 0 window size increment
    Balancer must send RST_STREAM(PROTOCOL_ERROR) to client
    """
    conn = common.start_and_connect(ctx, ChunkedConfig(http.response.ok(data=common.DATA), chunk_timeout=1))
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.write_frame(build_window_update(0))
    common.assert_stream_error(ctx, stream, errors.PROTOCOL_ERROR, "ZeroWindowUpdate")


def test_exceed_conn_max_window_size(ctx):
    """
    Client sends conn WINDOW_UPDATE which causes connection window to exceed 2 ^ 31 - 1
    Balancer must send GOAWAY(FLOW_CONTROL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(build_window_update(2 ** 31 - common.DEFAULT_WINDOW_SIZE))
    common.assert_conn_error(ctx, conn, errors.FLOW_CONTROL_ERROR, 'WindowOverflow')


def test_exceed_stream_max_window_size(ctx):
    """
    Client sends stream WINDOW_UPDATE which causes stream window to exceed 2 ^ 31 - 1
    Balancer must send RST_STREAM(FLOW_CONTROL_ERROR) to client
    """
    conn = common.start_and_connect(ctx, ChunkedConfig(http.response.ok(data=common.DATA), chunk_timeout=1))
    stream = conn.create_stream()
    stream.write_headers(http2.request.get().to_raw_request().headers, end_stream=False)
    stream.write_frame(build_window_update(2 ** 31 - common.DEFAULT_WINDOW_SIZE))
    common.assert_stream_error(ctx, stream, errors.FLOW_CONTROL_ERROR, "WindowOverflow")


def test_exceed_stream_max_window_size_on_initial_window_size_update(ctx):
    """
    Client sends SETTINGS_INITIAL_WINDOW_SIZE which causes stream window to exceed 2 ^ 31 - 1
    Balancer must send RST_STREAM(FLOW_CONTROL_ERROR) to client
    """
    conn = common.start_and_connect(ctx, ChunkedConfig(http.response.ok(data=common.DATA), chunk_timeout=1))
    stream = conn.create_stream()
    stream.write_headers(http2.request.get().to_raw_request().headers, end_stream=False)
    stream.write_frame(build_window_update(2 ** 31 - common.DEFAULT_WINDOW_SIZE - 1))
    conn.write_frame(common.build_settings([(frames.Parameter.INITIAL_WINDOW_SIZE, common.DEFAULT_WINDOW_SIZE + 1)]))
    common.assert_stream_error(ctx, stream, errors.FLOW_CONTROL_ERROR, "WindowOverflow")


def test_window_update_closed_stream(ctx):
    """
    Client sends WINDOW_UPDATE frame to closed stream
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_message(http2.request.get(authority=common.CUR_AUTH).to_raw_request())
    stream.read_message()
    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE))
    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_window_update_after_client_reset_stream(ctx):
    """
    Client sends WINDOW_UPDATE frame after RST_STREAM
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.write_chunk('A' * 10)
    stream.reset(errors.PROTOCOL_ERROR)
    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE), force=True)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_window_update_after_balancer_reset_stream(ctx):
    """
    Client sends WINDOW_UPDATE frame after balancer sends RST_STREAM
    Balancer must not treat it as an error
    """
    conn = common.start_and_connect(ctx, MultiActionConfig(
        actions=[MultiActionConfig.close_conn, MultiActionConfig.send_response],
        response=http.response.ok(data=common.DATA),
    ))
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_get().headers, end_stream=True)
    common.assert_stream_error(ctx, stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    time.sleep(5)
    stream.write_frame(build_window_update(common.DEFAULT_WINDOW_SIZE), force=True)
    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


def test_stream_window_update_timeout_slow_backend(ctx):
    """
    BALANCER-1152
    If client does not increase stream window in client_window_update_timeout
    but response data is not ready then balancer must not reset stream
    """
    window_update_timeout = 3
    data = ['A' * common.DEFAULT_WINDOW_SIZE] * 2
    conn = common.start_and_connect(
        ctx,
        ChunkedConfig(
            http.response.ok(data=data),
            chunk_timeout=2 * window_update_timeout
        ),
        backend_timeout=60
    )
    conn.write_window_update(common.DEFAULT_WINDOW_SIZE)
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    sum_len = 0
    while sum_len < common.DEFAULT_WINDOW_SIZE:
        frame = stream.wait_frame(frames.Data)
        sum_len += len(frame.data)
    time.sleep(window_update_timeout + 1)
    stream.write_window_update(common.DEFAULT_WINDOW_SIZE)
    resp = stream.read_message().to_response()
    asserts.content(resp, ''.join(data))


def test_connection_window_update_timeout_slow_backend(ctx):
    """
    BALANCER-1152
    If client does not increase connection window in client_window_update_timeout
    but response data is not ready then balancer must not reset stream
    """
    window_update_timeout = 3
    data = ['A' * common.DEFAULT_WINDOW_SIZE] * 2
    conn = common.start_and_connect(
        ctx,
        ChunkedConfig(
            http.response.ok(data=data),
            chunk_timeout=2 * window_update_timeout
        ),
        backend_timeout=60
    )
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.write_window_update(common.DEFAULT_WINDOW_SIZE)
    sum_len = 0
    while sum_len < common.DEFAULT_WINDOW_SIZE:
        frame = stream.wait_frame(frames.Data)
        sum_len += len(frame.data)
    time.sleep(window_update_timeout + 1)
    conn.write_window_update(common.DEFAULT_WINDOW_SIZE)
    resp = stream.read_message().to_response()
    asserts.content(resp, ''.join(data))


@pytest.mark.parametrize(
    'ack',
    [False, True],
    ids=["noack", "ack"]
)
@pytest.mark.parametrize(
    'size',
    [common.DEFAULT_WINDOW_SIZE - 1, common.DEFAULT_WINDOW_SIZE + 1],
    ids=["smaller", "bigger"]
)
def test_custom_server_initial_window_size(ctx, ack, size):
    """
    BALANCER-1973
    BALANCER-2174
    Balancer should respect its initial_window_size promises
    """
    default_size = common.DEFAULT_WINDOW_SIZE
    conn = common.start_and_connect(
        ctx,
        initial_window_size=size,
        max_frame_size=default_size
    )

    if ack:
        conn.write_frame(common.ACK_SETTINGS_FRAME)

    stream = conn.create_stream()
    stream.write_message(http2.request.post(
        data='A' * default_size
    ).to_raw_request())

    if ack:
        if size > default_size:
            resp = stream.read_message().to_response()
            asserts.status(resp, 200)
        else:
            common.assert_conn_error(ctx, conn, errors.FLOW_CONTROL_ERROR, "ConsumeUnderflow")
    else:
        # BALANCER-2174 We should always treat unacked settings in the client's favour
        resp = stream.read_message().to_response()
        asserts.status(resp, 200)
