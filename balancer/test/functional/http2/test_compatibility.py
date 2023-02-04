# -*- coding: utf-8 -*-
import pytest
import common
import time

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef import http2
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.proto.http.stream import serialize_request

import configs


RPC_REWRITE_REQUEST = http.request.get(headers={'X-Metabalancer-Y': 'meta'}, data=['vwxyz', '123'])
CHUNKED_DATA = ['Led', 'Zeppelin']
CHUNKED_CONTENT = ''.join(CHUNKED_DATA)


def rpcrewrite(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CHUNKED_DATA)))
    ctx.start_backend(
        SimpleConfig(response=http.response.ok(data=serialize_request(RPC_REWRITE_REQUEST))), name='rpc')
    ctx.start_balancer(configs.RpcRewriteConfig(certs_dir=ctx.certs.root_dir))


def threshold(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CHUNKED_DATA)))
    ctx.start_balancer(configs.ThresholdConfig(certs_dir=ctx.certs.root_dir))
    return None


def cutter(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CHUNKED_DATA)))
    ctx.start_balancer(configs.CutterConfig(certs_dir=ctx.certs.root_dir))
    return None


def antirobot(ctx):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CHUNKED_DATA)))
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='antirobot')
    ctx.start_balancer(configs.AntirobotConfig(certs_dir=ctx.certs.root_dir))
    return 'antirobot'


def errordocument(ctx, content=CHUNKED_CONTENT):
    e_file = ctx.manager.fs.create_file('xxx')
    with open(e_file, 'w') as f:
        f.write(content)
    ctx.start_balancer(configs.ErrorDocumentConfig(certs_dir=ctx.certs.root_dir, e_file=e_file))


@pytest.mark.parametrize(
    'setup_func',
    [
        rpcrewrite,
        threshold,
        cutter,
        antirobot,
        errordocument,
    ],
    ids=[
        'rpcrewrite',
        'threshold',
        'cutter',
        'antirobot',
        'errordocument'
    ],
)
def test_chunked_response(ctx, setup_func):
    """
    BALANCER-1270
    If backend response body is chunked then balancer must decode it
    """
    sub_report = setup_func(ctx)
    with common.create_conn(ctx) as conn:
        resp = conn.perform_request(http2.request.get())
    asserts.content(resp, CHUNKED_CONTENT)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert unistat['http2-end_stream_send_summ'] == 1
    assert unistat['http2-end_stream_recv_summ'] == 1
    assert unistat['report-total-succ_summ'] == 1
    assert unistat['report-total-fail_summ'] == 0
    if sub_report:
        assert unistat['report-{}-succ_summ'.format(sub_report)] == 1
        assert unistat['report-{}-fail_summ'.format(sub_report)] == 0


@pytest.mark.parametrize(
    'setup_func',
    [
        threshold,
        cutter,
        antirobot,
    ],
    ids=[
        'threshold',
        'cutter',
        'antirobot',
    ],
)
def test_aborted_request(ctx, setup_func):
    """
    BALANCER-2814
    Proper error accounting
    """
    sub_report = setup_func(ctx)
    with common.create_conn(ctx) as conn:
        stream = conn.create_stream()
        stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
        time.sleep(1)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['http2-h2_conn_open_summ'] == 1
    # assert unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert unistat['http2-end_stream_send_summ'] == 0
    assert unistat['http2-end_stream_recv_summ'] == 0
    assert unistat['http2-stream_conn_abort_summ'] == 1
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0
    if sub_report:
        assert unistat['report-{}-inprog_ammv'.format(sub_report)] == 0
        assert unistat['report-{}-succ_summ'.format(sub_report)] == 0
        assert unistat['report-{}-other_fail_summ'.format(sub_report)] == 0
        assert unistat['report-{}-fail_summ'.format(sub_report)] == 1
        assert unistat['report-{}-client_fail_summ'.format(sub_report)] == 1


def test_aborted_request_rpcrewrite(ctx):
    """
    BALANCER-2814
    Proper error accounting
    """
    rpcrewrite(ctx)
    with common.create_conn(ctx) as conn:
        stream = conn.create_stream()
        stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
        time.sleep(1)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_close_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert unistat['http2-end_stream_send_summ'] == 0
    assert unistat['http2-end_stream_recv_summ'] == 0
    assert unistat['http2-stream_conn_abort_summ'] == 1
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0
    assert unistat['report-rpc-inprog_ammv'] == 0
    assert unistat['report-rpc-succ_summ'] == 0
    assert unistat['report-rpc-other_fail_summ'] == 0
    assert unistat['report-rpc-fail_summ'] == 0
    assert unistat['report-rpc-client_fail_summ'] == 0


def test_cancelled_request_errordocument(ctx):
    """
    BALANCER-2814
    Proper error accounting
    """
    errordocument(ctx, 'a'*1024*1024*64)
    with common.create_conn(ctx) as conn:
        stream = conn.create_stream()
        stream.write_headers(http2.request.raw_custom(common.SIMPLE_REQ).headers, end_stream=False)
        time.sleep(1)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['http2-h2_conn_open_summ'] == 1
    assert unistat['http2-h2_conn_abort_summ'] == 1
    assert unistat['http2-h2_conn_inprog_ammv'] == 0
    assert unistat['http2-h2_conn_active_ammv'] == 0
    assert unistat['http2-stream_client_open_summ'] == 1
    assert unistat['http2-stream_dispose_summ'] == 1
    assert unistat['http2-stream_inprog_ammv'] == 0
    assert unistat['http2-stream_conn_abort_summ'] == 1
    assert unistat['http2-end_stream_recv_summ'] == 0
    assert unistat['http2-end_stream_send_summ'] == 0
    assert unistat['report-total-succ_summ'] == 0
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 1
    assert unistat['report-total-other_fail_summ'] == 0
