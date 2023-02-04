# -*- coding: utf-8 -*-
import pytest
import time
import OpenSSL

import common
from configs import CpuLimiterConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.predef import http2
from balancer.test.util.proto.http2 import errors
from balancer.test.util.stdlib.multirun import Multirun


def wait_cpu_usage_stats(ctx):
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['worker-average_cpu_usage_ammv'] > 0


def wait_http2_disable(ctx):
    for i in range(60):
        conn = common.create_conn(ctx, setup=False)
        conn.write_preface()
        try:
            common.wait_settings(conn)
        except OpenSSL.SSL.ZeroReturnError:
            break
        finally:
            conn.close()

        time.sleep(1)
    else:
        raise Exception('Timeout')


def check_stats(ctx, disabled, closed):
    unistat = ctx.get_unistat()
    assert unistat['worker-cpu_limiter_http2_disabled_summ'] == disabled
    assert unistat['worker-cpu_limiter_http2_closed_summ'] == closed


def test_drop_http2_above_limit(ctx):
    '''
    BALANCER-2002
    Если включена опция перестать работать по http2 при перегрузке cpu, то,
    когда потребление cpu превышает верхнюю границу,
    новые http2 соединения перестают приниматься.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        certs_dir=ctx.certs.root_dir,
        enable_http2_drop=True,
        http2_drop_lo=0,
        http2_drop_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    conn = common.create_conn(ctx, setup=False)
    conn.write_preface()

    with pytest.raises(OpenSSL.SSL.ZeroReturnError):
        common.wait_settings(conn)

    check_stats(ctx, 1, 0)


def test_disable_http2_drop(ctx):
    '''
    BALANCER-2002
    Если опция перестать работать по http2 при перегрузке cpu выключена, то,
    когда потребление cpu превышает верхнюю границу, http2 не перестает работает.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        certs_dir=ctx.certs.root_dir,
        http2_drop_lo=0,
        http2_drop_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    conn = common.create_conn(ctx)
    backend_resp = conn.perform_request(http2.request.get())
    asserts.status(backend_resp, 200)

    check_stats(ctx, 0, 0)


def test_disable_file_http2_drop(ctx):
    '''
    BALANCER-2002
    Если присутствует файл, выключающий все механизмы деградации, то,
    когда потребление cpu превышает верхнюю границу, http2 не перестает работает.
    '''
    file_switch = ctx.manager.fs.create_file('file_switch')

    ctx.start_balancer(CpuLimiterConfig(
        certs_dir=ctx.certs.root_dir,
        enable_http2_drop=True,
        http2_drop_lo=0,
        http2_drop_hi=0,
        disable_file=file_switch,
    ))

    wait_cpu_usage_stats(ctx)

    conn = common.create_conn(ctx)
    backend_resp = conn.perform_request(http2.request.get())
    asserts.status(backend_resp, 200)

    check_stats(ctx, 0, 0)


def test_drop_http2_file(ctx):
    '''
    BALANCER-2002
    Если присутствует файл, включающий деградацию через выключение http2,
    то новые http2 запросы перестают приниматься.
    '''
    file_switch = ctx.manager.fs.create_file('file_switch')

    ctx.start_balancer(CpuLimiterConfig(
        certs_dir=ctx.certs.root_dir,
        disable_http2_file=file_switch,
    ))

    conn = common.create_conn(ctx, setup=False)
    conn.write_preface()

    with pytest.raises(OpenSSL.SSL.ZeroReturnError):
        common.wait_settings(conn)

    check_stats(ctx, 1, 0)


def test_shutdown_http2_file(ctx):
    '''
    BALANCER-2002
    Если присутствует файл, включающий деградацию через выключение http2,
    то нельзя создать новые стримы, но текущие стримы могут доработать.
    '''
    file_switch = ctx.manager.fs.create_file('file_switch')
    ctx.manager.fs.remove(file_switch)

    ctx.start_balancer(CpuLimiterConfig(
        certs_dir=ctx.certs.root_dir,
        disable_http2_file=file_switch,
    ))

    conn = common.create_conn(ctx)

    good_stream = conn.create_stream()
    good_stream.write_message(http2.request.get().to_raw_request())

    ctx.manager.fs.rewrite(file_switch, '')
    wait_http2_disable(ctx)

    bad_stream = conn.create_stream()
    bad_stream.write_message(http2.request.get().to_raw_request())

    resp = good_stream.read_message().to_response()
    asserts.status(resp, 200)

    common.assert_stream_error(ctx, bad_stream, errors.REFUSED_STREAM, 'Shutdown')
    assert conn._server_state.context.go_away

    check_stats(ctx, 1, 1)
