# -*- coding: utf-8 -*-
import pytest
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


def base_settings_test(ctx, params, backend_resp=None):
    conn = common.start_and_connect(ctx, backend_resp)
    conn.write_frame(common.build_settings(params))
    frame = common.wait_settings(conn)
    assert frame.flags & flags.ACK
    return conn, frame


def test_ack_settings_frame(ctx):
    """
    Client sends SETTINGS frame without ACK flag
    Balancer must send SETTINGS frame with ACK flag in response
    """
    frame = base_settings_test(ctx, [(frames.Parameter.ENABLE_PUSH, 0)])[1]
    assert len(frame.data) == 0


def test_settings_header_table_size(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_HEADER_TABLE_SIZE parameter
    Balancer must apply it and send SETTINGS frame with ACK flag in response
    """
    table_size = 42
    base_settings_test(ctx, [(frames.Parameter.HEADER_TABLE_SIZE, table_size)])
    # TODO: check that balancer applied new value


def test_settings_disable_push(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_ENABLE_PUSH: 0 parameter
    Balancer must apply it and send SETTINGS frame with ACK flag in response
    """
    base_settings_test(ctx, [(frames.Parameter.ENABLE_PUSH, 0)])
    # TODO: check that balancer applied new value (push is needed)


def test_settings_enable_push(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_ENABLE_PUSH: 1 parameter
    Balancer must apply it and send SETTINGS frame with ACK flag in response
    """
    conn = base_settings_test(ctx, [(frames.Parameter.ENABLE_PUSH, 0)])[0]
    conn.write_frame(common.build_settings([(frames.Parameter.ENABLE_PUSH, 1)]))
    frame = common.wait_settings(conn)
    assert frame.flags & flags.ACK
    # TODO: check that balancer applied new value (push is needed)


def test_settings_max_concurrent_streams(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_MAX_CONCURRENT_STREAMS parameter
    Balancer must apply it and send SETTINGS frame with ACK flag in response
    """
    stream_count = 5
    base_settings_test(ctx, [(frames.Parameter.MAX_CONCURRENT_STREAMS, stream_count)])
    # TODO: check that balancer applied new value (push is needed)


def test_settings_max_frame_size(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_MAX_FRAME_SIZE parameter
    Balancer must apply it and send SETTINGS frame with ACK flag in response
    """
    frame_size = 2 ** 15
    name = 'vader'
    value = 'N' + 'O' * 42  # frame_size  # TODO: check long headers too
    data = 'A' * (frame_size + 1)
    conn = base_settings_test(
        ctx, [(frames.Parameter.MAX_FRAME_SIZE, frame_size)],
        backend_resp=http.response.ok(headers={name: value}, data=data)
    )[0]
    resp = conn.perform_request_raw_response(http2.request.get())
    for frame in resp.frames:
        assert frame.length <= frame_size
    asserts.header_value(resp, name, value)
    asserts.content(resp, data)


@pytest.mark.parametrize('value', [2 ** 14 - 1, 2 ** 24], ids=['lt', 'gt'])
def test_settings_max_frame_size_wrong_value(ctx, value):
    """
    Client sends SETTINGS frame with SETTINGS_MAX_FRAME_SIZE parameter
    which value is outside the [2 ^ 14, 2 ^ 24 - 1] range
    Balancer must send GOAWAY(PROTOCOL_ERROR) to client and close connection
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(common.build_settings([(frames.Parameter.MAX_FRAME_SIZE, value)]))
    common.assert_conn_protocol_error(ctx, conn, "InvalidSetting")


def test_settings_max_header_list_size(ctx):
    """
    Client sends SETTINGS frame with SETTINGS_MAX_HEADER_LIST_SIZE parameter
    Balancer must send SETTINGS frame with ACK flag in response
    Balancer must ignore this parameter when forwarding backend response
    """
    header_list_size = 42
    name = 'vader'
    value = 'N' + 'O' * header_list_size
    conn = base_settings_test(ctx, [(frames.Parameter.MAX_HEADER_LIST_SIZE, header_list_size)],
                              backend_resp=http.response.ok(headers={name: value}))[0]
    resp = conn.perform_request_raw_response(http2.request.get())
    asserts.header_value(resp, name, value)


def test_settings_unknown(ctx):
    """
    Balancer must not treat unknown settings as an error
    """
    conn = base_settings_test(ctx, [(2 ** 16 - 1, 2 ** 32 - 1)])[0]
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)
