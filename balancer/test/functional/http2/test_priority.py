# -*- coding: utf-8 -*-
import pytest
import time
import common
from balancer.test.util.balancer import asserts
from balancer.test.util.proto.http2 import errors
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.predef.handler.server.http import SimpleDelayedConfig, ThreeModeConfig, ThreeModeHandler, \
    MultiResponseConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2


def headers_dependency_stream(conn, exclusive, stream_dependency, weight):
    stream = conn.create_stream()
    stream.write_frame(frames.Headers(
        length=None, flags=flags.PRIORITY | flags.END_HEADERS | flags.END_STREAM, reserved=0, stream_id=None,
        exclusive=exclusive, stream_dependency=stream_dependency, weight=weight, data=common.SIMPLE_REQ_ENCODED
    ))
    return stream


def priority_dependency_stream(conn, exclusive, stream_dependency, weight):
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.write_priority(exclusive, stream_dependency, weight)
    return stream


def prio_parametrize(func):
    @pytest.mark.parametrize(
        'prio_stream', [headers_dependency_stream, priority_dependency_stream],
        ids=['headers', 'priority'],
    )
    @pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
    @pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
    def result_func(ctx, prio_stream, exclusive, weight):
        return func(ctx, prio_stream, exclusive, weight)

    return result_func


@prio_parametrize
def test_zero_dependency(ctx, prio_stream, exclusive, weight):
    """
    Client sends HEADERS or PRIORITY frame with stream dependency of 0x0.
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = prio_stream(conn, exclusive, 0, weight)
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)


@prio_parametrize
def test_opened_stream_dependency(ctx, prio_stream, exclusive, weight):
    """
    Client sends HEADERS or PRIORITY frame with stream dependency on opened stream.
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream1 = conn.create_stream()
    stream1.write_message(http2.request.get().to_raw_request())
    stream2 = prio_stream(conn, exclusive, stream1.stream_id, weight)
    resp1 = stream1.read_message().to_response()
    resp2 = stream2.read_message().to_response()
    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


@prio_parametrize
def test_closed_stream_dependency(ctx, prio_stream, exclusive, weight):
    """
    Client sends HEADERS or PRIORITY frame with stream dependency on closed stream.
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream1 = conn.create_stream()
    stream1.write_message(http2.request.get().to_raw_request())
    resp1 = stream1.read_message().to_response()
    stream2 = prio_stream(conn, exclusive, stream1.stream_id, weight)
    resp2 = stream2.read_message().to_response()
    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


@prio_parametrize
def test_idle_stream_dependency(ctx, prio_stream, exclusive, weight):
    """
    Client sends HEADERS or PRIORITY frame with stream dependency on idle stream.
    """
    parent_stream_id = 3
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream1 = prio_stream(conn, exclusive, parent_stream_id, weight)
    resp1 = stream1.read_message().to_response()
    stream2 = conn.create_stream(parent_stream_id)
    stream2.write_message(http2.request.get().to_raw_request())
    resp2 = stream2.read_message().to_response()
    asserts.status(resp1, 200)
    asserts.status(resp2, 200)


@prio_parametrize
def test_self_stream_dependency(ctx, prio_stream, exclusive, weight):
    """
    Client sends HEADERS or PRIORITY frame with stream dependency on itself
    Balancer must send RST_STREAM(PROTOCOL_ERROR) to client
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    stream = prio_stream(conn, exclusive, 1, weight)
    common.assert_stream_error(ctx, stream, errors.PROTOCOL_ERROR, "InvalidPriority")
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_closed_stream_zero_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on closed stream with stream dependency of 0x0
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.read_message()
    stream.write_priority(exclusive, 0, weight)
    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_closed_stream_other_stream_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on closed stream with stream dependency on other stream
    """
    conn = common.start_and_connect(ctx)
    stream1 = conn.create_stream()
    stream1.write_message(http2.request.get().to_raw_request())
    stream1.read_message()

    stream2 = conn.create_stream()
    stream2.write_message(http2.request.get().to_raw_request())
    stream2.read_message()
    stream2.write_priority(exclusive, stream1, weight)

    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_idle_stream_zero_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on idle stream with stream dependency of 0x0
    """
    conn = common.start_and_connect(ctx)
    conn.write_frame(frames.Priority(
        flags=0, reserved=0, stream_id=1,
        exclusive=exclusive, stream_dependency=0, weight=weight,
    ))
    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_idle_stream_other_stream_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on idle stream with stream dependency on other stream
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    stream.read_message()

    conn.write_frame(frames.Priority(
        flags=0, reserved=0, stream_id=101,
        exclusive=exclusive, stream_dependency=stream.stream_id, weight=weight,
    ))

    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_idle_stream_self_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on idle stream with stream dependency on itself
    Balancer must send RST_STREAM(PROTOCOL_ERROR) to client but not allowed to in the case of idle streams
    Balancer is also allowed to escalate stream errors to the connection level
    In this case it is the only valid option available
    """
    stream_id = 1
    conn = common.start_and_connect(ctx)
    conn.write_frame(frames.Priority(
        flags=0, reserved=0, stream_id=stream_id,
        exclusive=exclusive, stream_dependency=stream_id, weight=weight,
    ))
    common.assert_conn_error(ctx, conn, errors.PROTOCOL_ERROR, "InvalidPriority")


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_implicitly_closed_stream_zero_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on implicitly closed stream with stream dependency of 0x0
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream(101)
    stream.write_message(http2.request.get().to_raw_request())
    stream.read_message()

    conn.write_frame(frames.Priority(
        flags=0, reserved=0, stream_id=1,
        exclusive=exclusive, stream_dependency=0, weight=weight,
    ))
    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize('exclusive', [0, 1], ids=['not_exclusive', 'exclusive'])
@pytest.mark.parametrize('weight', [0, 1, 16, 255], ids=['zero', 'one', 'default', 'max'])
def test_implicitly_closed_stream_other_stream_dependency(ctx, exclusive, weight):
    """
    Client sends PRIORITY frame on implicitly closed stream with stream dependency on other stream
    """
    conn = common.start_and_connect(ctx)
    stream = conn.create_stream(101)
    stream.write_message(http2.request.get().to_raw_request())
    stream.read_message()

    conn.write_frame(frames.Priority(
        flags=0, reserved=0, stream_id=1,
        exclusive=exclusive, stream_dependency=stream.stream_id, weight=weight,
    ))

    common.assert_no_error(conn)
    resp = conn.perform_request(http2.request.get())
    asserts.status(resp, 200)


class ThreeModeDelayHandler(ThreeModeHandler):
    def handle_prefix(self, raw_request, stream):
        self.__handle(stream, self.config.prefix_delay)

    def handle_first(self, raw_request, stream):
        self.__handle(stream, self.config.first_delay)

    def handle_second(self, raw_request, stream):
        self.__handle(stream, self.config.second_delay)

    def __handle(self, stream, delay):
        time.sleep(delay)
        stream.write_response(self.config.response)
        self.finish_response()


class ThreeModeDelayConfig(ThreeModeConfig):
    HANDLER_TYPE = ThreeModeDelayHandler

    def __init__(self, prefix=0, first=10, second=10,
                 prefix_delay=0, first_delay=0, second_delay=0, response=None):
        super(ThreeModeDelayConfig, self).__init__(prefix, first, second, response)
        self.prefix_delay = prefix_delay
        self.first_delay = first_delay
        self.second_delay = second_delay


def test_parent_first(ctx):
    """
    Balancer must not wait for child stream response when forwarding parent stream response
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=0, prefix_delay=0.5, first_delay=7, response=http.response.ok(data=common.DATA),
    ), backend_timeout=10)
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, child_stream], content=common.CONTENT)
    assert parent_stream.close_timestamp < child_stream.close_timestamp


def test_child_first(ctx):
    """
    Balancer must not wait for parent stream response if child stream response is ready
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=0, prefix_delay=7, first_delay=0.5, response=http.response.ok(data=common.DATA),
    ), backend_timeout=10)
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, child_stream], content=common.CONTENT)
    assert parent_stream.close_timestamp > child_stream.close_timestamp


def test_heavyweight_first(ctx):
    """
    Balancer must not wait for lightweight stream response when forwarding heavyweight stream response
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=1, prefix_delay=7, first_delay=5, second_delay=0.5,
        response=http.response.ok(data=common.DATA),
    ), backend_timeout=10)
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)

    common.assert_ok_streams([parent_stream, light_stream, heavy_stream], content=common.CONTENT)
    assert heavy_stream.close_timestamp < light_stream.close_timestamp


def test_lightweight_first(ctx):
    """
    Balancer must not wait for heavyweight stream response if lightweight stream response is ready
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=1, prefix_delay=7, first_delay=5, second_delay=0.5,
        response=http.response.ok(data=common.DATA),
    ), backend_timeout=10)
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, heavy_stream, light_stream], content=common.CONTENT)
    assert heavy_stream.close_timestamp > light_stream.close_timestamp


def test_child_stream_timeout(ctx):
    """
    Balancer must forward parent stream response even if child stream timed out
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=0, prefix_delay=0.5, first_delay=10, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_stream_error(ctx, child_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream], content=common.CONTENT)


def test_child_stream_broken(ctx):
    """
    Balancer must forward parent stream response even if child stream is broken
    """
    conn = common.start_and_connect(ctx, ThreeModeConfig(
        prefix=0, first=1, second=1, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_stream_error(ctx, child_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream], content=common.CONTENT)


def test_child_stream_client_reset(ctx):
    """
    Balancer must forward parent stream response even if client resets child stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(data=common.DATA), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    child_stream.reset(errors.PROTOCOL_ERROR)

    common.assert_ok_streams([parent_stream], content=common.CONTENT)


def test_parent_stream_timeout(ctx):
    """
    Balancer must forward child stream response even if parent stream timed out
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=0, prefix_delay=10, first_delay=0.5, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_stream_error(ctx, parent_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([child_stream], content=common.CONTENT)


def test_parent_stream_broken(ctx):
    """
    Balancer must forward child stream response even if parent stream is broken
    """
    conn = common.start_and_connect(ctx, ThreeModeConfig(
        prefix=1, first=1, second=0, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_stream_error(ctx, parent_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([child_stream], content=common.CONTENT)


def test_parent_stream_client_reset(ctx):
    """
    Balancer must forward child stream response even if client resets parent stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(data=common.DATA), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    child_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    parent_stream.reset(errors.PROTOCOL_ERROR)

    common.assert_ok_streams([child_stream], content=common.CONTENT)


def test_lightweight_stream_timeout(ctx):
    """
    Balancer must forward heavyweight stream response even if lightweight stream timed out
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=1, prefix_delay=2, first_delay=10, second_delay=0.5,
        response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)

    common.assert_stream_error(ctx, light_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream, heavy_stream], content=common.CONTENT)


def test_lightweight_stream_broken(ctx):
    """
    Balancer must forward heavyweight stream response even if lightweight stream is broken
    """
    conn = common.start_and_connect(ctx, ThreeModeConfig(
        prefix=0, first=1, second=1, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)

    common.assert_stream_error(ctx, light_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream, heavy_stream], content=common.CONTENT)


def test_lightweight_stream_client_reset(ctx):
    """
    Balancer must forward heavyweight stream response even if client resets lightweight stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(data=common.DATA), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)
    light_stream.reset(errors.PROTOCOL_ERROR)

    common.assert_ok_streams([parent_stream, heavy_stream], content=common.CONTENT)


def test_heavyweight_stream_timeout(ctx):
    """
    Balancer must forward lightweight stream response even if heavyweight stream timed out
    """
    conn = common.start_and_connect(ctx, ThreeModeDelayConfig(
        prefix=1, first=1, second=1, prefix_delay=2, first_delay=0.5, second_delay=10,
        response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)

    common.assert_stream_error(ctx, heavy_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream, light_stream], content=common.CONTENT)


def test_heavyweight_stream_broken(ctx):
    """
    Balancer must forward lightweight stream response even if heavyweight stream is broken
    """
    conn = common.start_and_connect(ctx, ThreeModeConfig(
        prefix=0, first=2, second=1, response=http.response.ok(data=common.DATA),
    ))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)

    common.assert_stream_error(ctx, heavy_stream, common.BACKEND_ERROR_EMPTY_RESP, "BackendError")
    common.assert_ok_streams([parent_stream, light_stream], content=common.CONTENT)


def test_heavyweight_stream_client_reset(ctx):
    """
    Balancer must forward lightweight stream response even if client resets heavyweight stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(data=common.DATA), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    time.sleep(0.5)
    light_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    time.sleep(0.5)
    heavy_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 128)
    heavy_stream.reset(errors.PROTOCOL_ERROR)

    common.assert_ok_streams([parent_stream, light_stream], content=common.CONTENT)


def test_insert_exclusive(ctx):
    """
    Insert exclusive stream between parent stream and non-exclusive child streams
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    exclusive_stream = headers_dependency_stream(conn, 1, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2, exclusive_stream])


def test_insert_exclusive_before_exclusive(ctx):
    """
    Insert exclusive stream between parent stream and another exclusive child stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 1, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 1, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2])


def test_add_non_exclusive_with_exclusive(ctx):
    """
    Client tries to add non-exclusive stream to parent stream, which already has an exclusive dependency.
    Balancer must add non-exclusive stream as a child stream to the dependent stream.
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    exclusive_stream = headers_dependency_stream(conn, 1, parent_stream.stream_id, 16)
    non_exclusive_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_ok_streams([parent_stream, exclusive_stream, non_exclusive_stream])


def test_add_non_exclusive_with_exclusive_chain(ctx):
    """
    Client tries to add non-exclusive stream to parent stream,
    which already has an exclusively dependent chain of streams.
    Balancer must add non-exclusive stream as a child stream to the last one stream in the chain.
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    exclusive_stream1 = headers_dependency_stream(conn, 1, parent_stream.stream_id, 16)
    exclusive_stream2 = headers_dependency_stream(conn, 1, exclusive_stream1.stream_id, 16)
    exclusive_stream3 = headers_dependency_stream(conn, 1, exclusive_stream2.stream_id, 16)
    non_exclusive_stream = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)

    common.assert_ok_streams([
        parent_stream, exclusive_stream1, exclusive_stream2, exclusive_stream3, non_exclusive_stream
    ])


def test_turn_to_exclusive(ctx):
    """
    Client turns non-exclusive stream dependency into exclusive
    Other dependent streams of the same parent must be moved to be
    dependent streams of the exclusively dependent stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream1.write_priority(1, parent_stream, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2])


def test_move_with_subtree(ctx):
    """
    A stream must be moved with its subtree
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream3 = headers_dependency_stream(conn, 0, stream2.stream_id, 16)
    stream2.write_priority(0, stream1, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2, stream3])


def test_move_exclusive_with_subtree(ctx):
    """
    A stream must be moved with its subtree
    If the stream becomes and exclusive dependency then all existing dependencies of a new parent stream
    must be moved to be dependent streams of the exclusively dependent stream
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, stream1.stream_id, 16)
    stream3 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream4 = headers_dependency_stream(conn, 0, stream3.stream_id, 16)
    stream3.write_priority(1, stream1, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2, stream3, stream4])


def test_move_to_child(ctx):
    """
    Moving a stream to be dependent on its child stream
    At first child stream is moved to be dependent on the reprioritized stream's previous parent
    The moved dependency retains its weight
    Then reprioritized stream is moved as usual
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream3 = headers_dependency_stream(conn, 0, stream2.stream_id, 16)
    parent_stream.write_priority(0, stream2, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2, stream3])


def test_move_to_exclusive_child(ctx):
    """
    Moving a stream to be dependent on its exclusively dependent child stream
    At first child stream is moved to be dependent on the reprioritized stream's previous parent
    The moved dependency retains its weight
    The moved dependency becomes non-exclusive
    Then reprioritized stream is moved as usual
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    other_stream = conn.create_stream()
    other_stream.write_message(http2.request.get().to_raw_request())
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream3 = headers_dependency_stream(conn, 1, stream2.stream_id, 16)
    parent_stream.write_priority(0, stream3, 16)

    common.assert_ok_streams([other_stream, parent_stream, stream1, stream2, stream3])


def test_move_exclusive_to_child(ctx):
    """
    Moving a stream to be exclusively dependent on its child stream
    At first child stream is moved to be dependent on the reprioritized stream's previous parent
    The moved dependency retains its weight
    Then reprioritized stream is moved as usual
    """
    conn = common.start_and_connect(ctx, SimpleDelayedConfig(http.response.ok(), response_delay=2))
    parent_stream = conn.create_stream()
    parent_stream.write_message(http2.request.get().to_raw_request())
    stream1 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream2 = headers_dependency_stream(conn, 0, parent_stream.stream_id, 16)
    stream3 = headers_dependency_stream(conn, 0, stream2.stream_id, 16)
    parent_stream.write_priority(1, stream2, 16)

    common.assert_ok_streams([parent_stream, stream1, stream2, stream3])


def check_priorities_nginx(conn, streams, update_size):
    while streams:
        conn.write_window_update(update_size)
        frame = conn.wait_frame(frames.Data)
        top_stream = streams[0]
        assert frame.stream_id == top_stream.stream_id
        if top_stream.is_closed():
            del streams[0]


def test_priority_dependencies_nginx(ctx):
    """
    BALANCER-1139
    If response data is ready in multiple streams
    then balancer must send data to parent stream
    """
    stream_count = 10
    update_size = common.DEFAULT_WINDOW_SIZE / 5
    conn = common.start_and_connect(ctx, http.response.ok(data='A' * common.DEFAULT_WINDOW_SIZE))
    conn.perform_request(http2.request.get())  # fill connection window
    streams = list()
    for i in range(stream_count):
        streams.append(headers_dependency_stream(conn, 1, 1 + 2 * i, 16))
    time.sleep(5)  # balancer needs a delay to apply priorities

    check_priorities_nginx(conn, streams, update_size)


def test_priority_weights_nginx(ctx):
    """
    BALANCER-1139
    If response data is ready in multiple streams
    then balancer must send data to the heaviest stream
    """
    stream_count = 10
    update_size = common.DEFAULT_WINDOW_SIZE / 5
    conn = common.start_and_connect(ctx, http.response.ok(data='A' * common.DEFAULT_WINDOW_SIZE))
    conn.perform_request(http2.request.get())  # fill connection window
    streams = list()
    for i in range(stream_count):
        streams.append(headers_dependency_stream(conn, 0, 0, i))
    streams.reverse()
    time.sleep(5)  # balancer needs a delay to apply priorities

    check_priorities_nginx(conn, streams, update_size)


def test_priority_weights_nginx_edge_override(ctx):
    """
    BALANCER-1326 priority override
    """
    resps = {}
    prios = [2**i for i in range(9)]

    for prio in prios + [0, 257, "xxx"]:
        resps["/prio_{}".format(prio)] = http.response.ok(
            headers=[("x-yandex-h2-prio-edge", prio)],
            data='A' * common.DEFAULT_WINDOW_SIZE
        )

    conn = common.start_and_connect(
        ctx, backend=MultiResponseConfig(resps)
    )

    conn.write_window_update(2 * common.DEFAULT_WINDOW_SIZE)
    conn.perform_request(http2.request.get("/prio_0"))    # test invalid priority value
    conn.perform_request(http2.request.get("/prio_257"))  # test invalid priority value
    conn.perform_request(http2.request.get("/prio_xxx"))  # test invalid priority value
    # content window should be empty by now

    streams = []
    for prio in prios:
        stream = conn.create_stream()
        stream.write_message(
            http2.request.get(path="/prio_{}".format(prio)).to_raw_request()
        )
        streams.append(stream)
    streams.reverse()

    time.sleep(5)  # balancer needs a delay to apply priorities
    check_priorities_nginx(conn, streams, update_size=common.DEFAULT_WINDOW_SIZE / 5)


@pytest.mark.parametrize('content_type', [
    ["text", "css"],
    ["text", "html"],
    ["text", "xml"],
    ["text", "ecmascript"],
    ["text", "javascript"],
    ["text", "typescript"],
    ["application", "xml"],
    ["application", "ecmascript"],
    ["application", "javascript"],
    ["application", "typescript"],
    ["application", "xhtml+xml"],
    ["application", "xslt+xml"],
    ["font", "whatever"],
])
def test_priority_weights_nginx_edge_by_content_type(ctx, content_type):
    """
    BALANCER-1326 priority heuristic
    """
    content_type = "/".join(content_type)
    conn = common.start_and_connect(
        ctx, backend=MultiResponseConfig({
            "/high_prio": http.response.ok(
                headers=[('content-type', "xxx; " + content_type + "; xxx")],
                data='A' * common.DEFAULT_WINDOW_SIZE
            ),
            "/low_prio": http.response.ok(
                headers=[('content-type', 'images/png')],
                data='A' * common.DEFAULT_WINDOW_SIZE
            )
        })
    )
    conn.perform_request(http2.request.get(path="/high_prio"))  # fill connection window

    streams = []
    for path in ["/low_prio", "/high_prio"]:
        stream = conn.create_stream()
        stream.write_message(http2.request.get(path=path).to_raw_request())
        streams.append(stream)
    streams.reverse()

    time.sleep(5)  # balancer needs a delay to apply priorities
    check_priorities_nginx(conn, streams, update_size=common.DEFAULT_WINDOW_SIZE / 5)
