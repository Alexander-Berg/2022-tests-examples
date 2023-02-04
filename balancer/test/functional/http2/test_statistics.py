# -*- coding: utf-8 -*-
import time

import pytest

import common
from balancer.test.util.config import parametrize_thread_mode
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.predef.handler.server.http import SimpleDelayedConfig, NoReadConfig, SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.predef import http2
from balancer.test.util.proto.http2.framing.frames import RstStream
from balancer.test.util.proto.http2 import errors
from balancer.test.util.stdlib.multirun import Multirun


@parametrize_thread_mode
def test_statistics_h2_conn_open(ctx, thread_mode):
    """
    Client opens new http2 connection
    Balancer must increment h2_conn_open counter
    """
    common.start_and_connect(ctx, thread_mode=thread_mode)
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['http2-h2_conn_open_summ'] == 1
            assert unistat['http2-h2_conn_open_active_summ'] == 0
            assert unistat['http2-h2_conn_close_summ'] == 0
            assert unistat['http2-h2_conn_inprog_ammv'] == 1
            assert unistat['http2-h2_conn_active_ammv'] == 0
            assert unistat['http2-stream_client_open_summ'] == 0
            assert unistat['http2-stream_dispose_summ'] == 0
            assert unistat['http2-stream_inprog_ammv'] == 0
            assert unistat['report-service_total_h2-inprog_ammv'] == 0


@parametrize_thread_mode
def test_statistics_h2_conn_close(ctx, thread_mode):
    """
    Client closes http2 connection
    Balancer must increment h2_conn_close counter
    """
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)
    conn.close()
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['http2-h2_conn_open_summ'] == 1
            assert unistat['http2-h2_conn_open_active_summ'] == 0
            assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 1
            assert unistat['http2-h2_conn_inprog_ammv'] == 0
            assert unistat['http2-h2_conn_active_ammv'] == 0
            assert unistat['http2-stream_client_open_summ'] == 0
            assert unistat['http2-stream_dispose_summ'] == 0
            assert unistat['http2-stream_inprog_ammv'] == 0
            assert unistat['report-service_total-inprog_ammv'] == 0


@parametrize_thread_mode
def test_statistics_h2_stream_client_open(ctx, thread_mode):
    """
    Client opens new http2 stream
    Balancer must increment stream_client_open counter
    """
    conn = common.start_and_connect(
        ctx,
        SimpleDelayedConfig(http.response.ok(), response_delay=3),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['http2-h2_conn_open_summ'] == 1
            assert unistat['http2-h2_conn_open_active_summ'] == 1
            assert unistat['http2-h2_conn_close_summ'] == 0
            assert unistat['http2-h2_conn_inprog_ammv'] == 1
            assert unistat['http2-h2_conn_active_ammv'] == 1
            assert unistat['http2-stream_client_open_summ'] == 1
            assert unistat['http2-stream_dispose_summ'] == 0
            assert unistat['http2-stream_inprog_ammv'] == 1
            assert unistat['http2-end_stream_send_summ'] == 0
            assert unistat['http2-end_stream_recv_summ'] == 0


@parametrize_thread_mode
def test_statistics_h2_end_stream_send(ctx, thread_mode):
    """
    Balancer finished sending response to client
    Balancer must increment end_stream_send counter
    """
    conn = common.start_and_connect(
        ctx,
        NoReadConfig(
            force_close=True, response=http.response.ok(),
        ),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
    stream.read_message()
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['http2-end_stream_send_summ'] == 1
            assert unistat['http2-end_stream_recv_summ'] == 0
            assert unistat['report-service_total-inprog_ammv'] == 0


@parametrize_thread_mode
def test_statistics_h2_end_stream_recv(ctx, thread_mode):
    """
    Client finished sending request to balancer
    Balancer must increment the end_stream_recv counter
    """
    conn = common.start_and_connect(
        ctx,
        SimpleDelayedConfig(http.response.ok(), response_delay=20),
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_message(http2.request.get().to_raw_request())
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['http2-end_stream_recv_summ'] == 1
            assert unistat['http2-end_stream_send_summ'] == 0


def _start_balancer_conn_and_stream(ctx, thread_mode):
    conn = common.start_and_connect(
        ctx,
        SimpleConfig(response=http.response.ok(data="A" * (common.DEFAULT_WINDOW_SIZE + 1))),
        backend_timeout=10,
        thread_mode=thread_mode,
    )
    stream = conn.create_stream()
    stream.write_headers(
        http2.request.raw_custom(common.SIMPLE_REQ).headers,
        end_stream=True
    )
    stream.read_frame()
    return conn, stream


def _assert_service_total(proto, unistat, key, val, nonval):
    for i, suff in enumerate(["", "_h1", "_h2"]):
        if i == proto or i == 0:
            expected = val
        else:
            expected = nonval

        unistat_key = 'report-service_total{}-{}'.format(suff, key)
        if key == 'inprog':
            unistat_key += '_ammv'
        else:
            unistat_key += '_summ'

        if i == proto or i == 0:
            assert unistat.get(unistat_key, int(expected)) == int(expected)
        else:
            assert unistat[unistat_key] == int(expected)


def _assert_service_total_h1(unistat, key, val, nonval='0'):
    _assert_service_total(1, unistat, key, val, nonval)


def _assert_service_total_h2(unistat, key, val, nonval='0'):
    _assert_service_total(2, unistat, key, val, nonval)


@parametrize_thread_mode
def test_statistics_h2_stream_cancel_recv(ctx, thread_mode):
    """
    The client sent rst_stream=CANCEL to the balancer
    The balancer must increment the error counters
    """
    conn, stream = _start_balancer_conn_and_stream(ctx, thread_mode)
    stream.write_frame(RstStream(flags=0, reserved=0, stream_id=stream.stream_id, error_code=errors.CANCEL), force=True)
    time.sleep(0.5)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 0
    assert unistat['http-balancer-cancelled_session_http2_ResetByClient_summ'] == 1
    assert unistat['http2-rst_stream_recv-CANCEL_summ'] == 1
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_open_active_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_success_summ'] == 0
    assert unistat['http2-stream_inprog_ammv'] == 0

    for key in ("ka", "reused", "succ"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "fail", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


_PROTOCOL_ERROR = (
    '',
    'PROTOCOL_ERROR',
)
_PROTOCOL_ERROR_FAILED_PING = (
    'Failed ping.',
    'PROTOCOL_ERROR_Failed_ping',
)
_PROTOCOL_ERROR_ERROR_102_READING_FROM_SOCKET = (
    'Error 102 reading from socket.',
    'PROTOCOL_ERROR_Error_102_reading_from_socket',
)
_PROTOCOL_ERROR_CLOSING_CURRENT_SESSIONS = (
    'Closing current sessions.',
    'PROTOCOL_ERROR_Closing_current_sessions',
)

_ALL_CHROME_PROTOCOL_ERRORS = [
    _PROTOCOL_ERROR,
    _PROTOCOL_ERROR_FAILED_PING,
    _PROTOCOL_ERROR_ERROR_102_READING_FROM_SOCKET,
    _PROTOCOL_ERROR_CLOSING_CURRENT_SESSIONS,
]

_ALL_CHROME_PROTOCOL_ERROR_CODES = list(map(lambda x: x[1], _ALL_CHROME_PROTOCOL_ERRORS))


@parametrize_thread_mode
@pytest.mark.parametrize(
    ['message', 'error'],
    _ALL_CHROME_PROTOCOL_ERRORS,
    ids=_ALL_CHROME_PROTOCOL_ERROR_CODES
)
def test_statistics_h2_goaway_protocol_error_recv(ctx, thread_mode, message, error):
    """
    The client sent goaway=PROTOCOL_ERROR to the balancer
    The balancer must increment the error counters
    """
    conn, stream = _start_balancer_conn_and_stream(ctx, thread_mode)
    conn.write_frame(frames.Goaway(
        length=None, flags=0, reserved=0, goaway_reserved=0,
        last_stream_id=1, error_code=errors.PROTOCOL_ERROR, data=message,
    ), force=True)
    time.sleep(0.5)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 0
    assert unistat['http-balancer-cancelled_session_http2_ConnError_summ'] == 1
    for err in _ALL_CHROME_PROTOCOL_ERROR_CODES:
        assert unistat['http2-go_away_recv-' + err + '_summ'] == int(err == error)
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_open_active_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0

    for key in ("ka", "reused", "fail"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "succ", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


@parametrize_thread_mode
def test_statistics_h2_stream_protocol_error_after_cancel_recv(ctx, thread_mode):
    """
    The client sent rst_stream=CANCEL and then rst_stream=PROTOCOL_ERROR to the balancer
    The balancer must increment the error counters
    """
    conn, stream = _start_balancer_conn_and_stream(ctx, thread_mode)
    stream.write_frame(RstStream(flags=0, reserved=0, stream_id=stream.stream_id, error_code=errors.CANCEL), force=True)
    stream.write_frame(
        RstStream(flags=0, reserved=0, stream_id=stream.stream_id, error_code=errors.PROTOCOL_ERROR), force=True)
    time.sleep(0.5)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 0
    assert unistat['http-balancer-cancelled_session_http2_ResetByClient_summ'] == 1
    assert unistat['http2-rst_stream_recv-CANCEL_summ'] == 1
    assert unistat['http2-rst_stream_recv-PROTOCOL_ERROR_summ'] == 0
    assert unistat['http2-rst_stream_recv-PROTOCOL_ERROR_after_CANCEL_summ'] == 1
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_open_active_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_success_summ'] == 0
    assert unistat['http2-stream_inprog_ammv'] == 0

    for key in ("ka", "reused", "succ"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "fail", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


@parametrize_thread_mode
def test_statistics_h2_stream_error_recv(ctx, thread_mode):
    """
    The client sent rst_stream=PROTOCOL_ERROR to the balancer
    The balancer must increment the error counters
    """
    conn, stream = _start_balancer_conn_and_stream(ctx, thread_mode)
    stream.write_frame(
        RstStream(flags=0, reserved=0, stream_id=stream.stream_id, error_code=errors.PROTOCOL_ERROR),
        force=True
    )
    time.sleep(0.5)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 0
    assert unistat['http-balancer-cancelled_session_http2_ResetByClient_summ'] == 1
    assert unistat['http2-rst_stream_recv-PROTOCOL_ERROR_summ'] == 1
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_open_active_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_success_summ'] == 0
    assert unistat['http2-stream_inprog_ammv'] == 0

    for key in ("ka", "reused", "fail"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "succ", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


@parametrize_thread_mode
def test_statistics_h2_conn_error(ctx, thread_mode):
    """
    The client closed the connection
    The balancer must increment the error counter
    """
    conn, stream = _start_balancer_conn_and_stream(ctx, thread_mode)
    time.sleep(0.3)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 0
    assert (unistat['http-balancer-cancelled_session_http2_ConnError_summ'] +
            unistat['http-balancer-cancelled_session_http2_ConnClose_summ']) == 1
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_open_active_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_conn_abort_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert \
        unistat['http2-send_frames_eio_summ'] + \
        unistat['http2-recv_frame_eio_summ'] + \
        unistat['http2-send_frames_rst_summ'] + \
        unistat['http2-recv_frame_rst_summ'] + \
        unistat['http2-send_frames_pipe_summ'] + \
        unistat['http2-send_frames_ssl_summ'] + \
        unistat['http2-recv_frame_ssl_summ'] + \
        unistat['http2-send_frames_other_summ'] + \
        unistat['http2-recv_frame_other_summ'] == 1
    assert unistat['http2-recv_frame_cancel_summ'] == 0

    for key in ("ka", "reused", "fail"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "succ", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


@parametrize_thread_mode
def test_statistics_h2_max_conn(ctx, thread_mode):
    """
    The balancer cancelled the connection
    The balancer must increment the error counter
    """
    common.start_all(
        ctx,
        SimpleConfig(response=http.response.ok(data="A" * (common.DEFAULT_WINDOW_SIZE + 1))),
        backend_timeout=10,
        maxconn=1,
        thread_mode=thread_mode,
    )

    conn1 = common.create_conn(ctx)
    stream1 = conn1.create_stream()
    stream1.write_headers(
        http2.request.raw_custom(common.SIMPLE_REQ).headers,
        end_stream=True
    )
    stream1.read_frame()
    time.sleep(0.5)

    conn2 = common.create_conn(ctx)
    conn2.write_frame(common.build_settings(
        [(frames.Parameter.INITIAL_WINDOW_SIZE, common.DEFAULT_WINDOW_SIZE + 1)]
    ))
    stream2 = conn2.create_stream()
    stream2.write_headers(
        http2.request.raw_custom(common.SIMPLE_REQ).headers,
        end_stream=True
    )
    stream2.read_message()
    conn2.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http-balancer-cancelled_session_summ'] == 1
    assert unistat['http-balancer-cancelled_session_http2_ConnCancel_summ'] == 1
    assert unistat['http2-h2_conn_open_summ'] == 2
    assert unistat['http2-h2_conn_open_active_summ'] == 2
    assert unistat['http2-h2_conn_abort_summ'] + unistat['http2-h2_conn_close_summ'] == 2
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 2
    assert unistat['http2-stream_conn_abort_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 2
    assert unistat['http2-stream_success_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert unistat['http2-recv_frame_cancel_summ'] == 1

    for key in ("ka", "reused"):
        _assert_service_total_h2(unistat, key, '2')

    for key in ("succ", "fail"):
        _assert_service_total_h2(unistat, key, '1')

    for key in ("nka", "nreused", "inprog"):
        _assert_service_total_h2(unistat, key, '0')


@parametrize_thread_mode
def test_statistics_h2_parallel_streams(ctx, thread_mode):
    num_streams = 3
    reqs_data = ['Electric', 'Light', 'Orchestra']
    reqs = [
        http2.request.post(
            '/{}'.format(i), data=reqs_data[i]
        ).to_raw_request() for i in range(num_streams)
    ]
    conn = common.start_and_connect(ctx, thread_mode=thread_mode)

    streams = list()
    resps = list()
    backend_reqs = list()
    for i in range(num_streams):
        stream = conn.create_stream()
        stream.write_headers(reqs[i].headers, end_stream=False)
        streams.append(stream)
        time.sleep(0.5)

        unistat = ctx.get_unistat()
        assert unistat['http2-h2_conn_open_summ'] == 1
        assert unistat['http2-h2_conn_open_active_summ'] == 1
        assert unistat['http2-h2_conn_inprog_ammv'] == 1
        assert unistat['http2-h2_conn_active_ammv'] == 1
        assert unistat['http2-stream_client_open_summ'] == i + 1
        assert unistat['http2-stream_inprog_ammv'] == i + 1

    for i in range(num_streams):
        streams[i].write_data(reqs[i].data)
        resps.append(streams[i].read_message().to_response())
        backend_reqs.append(ctx.backend.state.get_request())
        time.sleep(0.5)

        unistat = ctx.get_unistat()
        assert unistat['http2-end_stream_recv_summ'] == i + 1
        assert unistat['http2-end_stream_send_summ'] == i + 1
        assert unistat['http2-stream_dispose_summ'] == i + 1
        assert unistat['http2-stream_success_summ'] == i + 1

    unistat = ctx.get_unistat()
    assert unistat['http2-stream_inprog_ammv'] == 0


def create_h1_conn(ctx):
    host = 'localhost'
    port = ctx.balancer.config.port
    return ctx.manager.connection.http.create_pyssl(
        host=host,
        port=port,
        ssl_options=SSLClientOptions(alpn='http/1.1'),
    )


@parametrize_thread_mode
def test_statistics_h1_conn_open(ctx, thread_mode):
    """
    Client opens new http1 connection
    Balancer must increment h1_conn_open counter
    """
    common.start_all(ctx, thread_mode=thread_mode)
    create_h1_conn(ctx)

    unistat = ctx.get_unistat()
    assert unistat['http2-h1_conn_open_summ'] == 1
    assert unistat['http2-h1_conn_close_summ'] == 0
    assert unistat['http2-h1_conn_inprog_ammv'] == 1


@parametrize_thread_mode
def test_statistics_h1_conn_close(ctx, thread_mode):
    """
    Client closes http1 connection
    Balancer must increment h1_conn_close counter
    """
    common.start_all(ctx, thread_mode=thread_mode)
    conn = create_h1_conn(ctx)
    conn.close()
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http2-h1_conn_open_summ'] == 1
    assert unistat['http2-h1_conn_close_summ'] == 1
    assert unistat['http2-h1_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0


@parametrize_thread_mode
def test_statistics_h1_no_stream_counters(ctx, thread_mode):
    """
    Client opens new http1 stream
    Balancer must not increment stream counters
    """
    common.start_all(
        ctx,
        SimpleDelayedConfig(http.response.ok(), response_delay=3),
        thread_mode=thread_mode,
    )
    conn = create_h1_conn(ctx)
    conn.perform_request(http.request.get())
    time.sleep(0.5)  # do not need multirun, because counters must stay the same

    unistat = ctx.get_unistat()
    assert unistat['http2-stream_client_open_summ'] == 0
    assert unistat['http2-end_stream_send_summ'] == 0
    assert unistat['http2-end_stream_recv_summ'] == 0
    assert unistat['http2-h1_conn_open_summ'] == 1
    assert unistat['http2-h1_conn_close_summ'] == 0
    assert unistat['http2-h1_conn_inprog_ammv'] == 1

    for key in ("ka", "nreused", "succ"):
        _assert_service_total_h1(unistat, key, '1')

    for key in ("nka", "reused", "fail", "inprog"):
        _assert_service_total_h1(unistat, key, '0')

    conn.perform_request(http.request.get())
    time.sleep(0.5)

    unistat = ctx.get_unistat()
    assert unistat['http2-stream_client_open_summ'] == 0
    assert unistat['http2-end_stream_send_summ'] == 0
    assert unistat['http2-end_stream_recv_summ'] == 0
    assert unistat['http2-h1_conn_open_summ'] == 1
    assert unistat['http2-h1_conn_close_summ'] == 0
    assert unistat['http2-h1_conn_inprog_ammv'] == 1

    for key in ("ka", "succ"):
        _assert_service_total_h1(unistat, key, '2')

    for key in ("reused", "nreused"):
        _assert_service_total_h1(unistat, key, '1')

    for key in ("nka", "fail", "inprog"):
        _assert_service_total_h1(unistat, key, '0')
