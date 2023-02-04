import pytest
import socket

from configs import CpuLimiterConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.predef import http
from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.stdlib.multirun import Multirun


def wait_cpu_usage_stats(ctx):
    for run in Multirun():
        with run:
            unistat = ctx.get_unistat()
            assert unistat['worker-average_cpu_usage_ammv'] > 0


def rejected_conn(ctx):
    with ctx.create_http_connection() as conn:
        with pytest.raises((socket.error, HTTPReaderException)):
            conn.perform_request(http.request.get())


def check_stats(ctx, conn_hold, conn_rejected, keepalive_closed):
    unistat = ctx.get_unistat()
    assert unistat['worker-cpu_limiter_conn_hold_summ'] == conn_hold
    assert unistat['worker-cpu_limiter_conn_rejected_summ'] == conn_rejected
    assert unistat['worker-cpu_limiter_keepalive_closed_summ'] == keepalive_closed


def test_keepalive_close(ctx):
    '''
    BALANCER-1910
    Если включена опция закрывать keepalive при перегрузке cpu, то,
    когда потребление cpu превышает верхнюю границу,
    keepalive соединения закрываются.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_keepalive_close=True,
        keepalive_close_lo=0,
        keepalive_close_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        asserts.is_closed(conn.sock)

    check_stats(ctx, 0, 0, 1)


def test_disable_keepalive_close(ctx):
    '''
    BALANCER-1910
    Если опция закрывать keepalive при перегрузке cpu выключена, то,
    когда потребление cpu превышает верхнюю границу,
    keepalive соединения не закрываются.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        keepalive_close_lo=0,
        keepalive_close_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        asserts.is_not_closed(conn.sock)

    check_stats(ctx, 0, 0, 0)


def test_disable_file_keepalive_close(ctx):
    '''
    BALANCER-1910
    Если присутствует файл, выключающий все механизмы деградации, то,
    когда потребление cpu превышает верхнюю границу,
    keepalive соединения закрываются.
    '''
    file_switch = ctx.manager.fs.create_file('file_switch')

    ctx.start_balancer(CpuLimiterConfig(
        enable_keepalive_close=True,
        keepalive_close_lo=0,
        keepalive_close_hi=0,
        disable_file=file_switch,
    ))

    wait_cpu_usage_stats(ctx)

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)
        asserts.is_not_closed(conn.sock)

    check_stats(ctx, 0, 0, 0)


def test_conn_reject(ctx):
    '''
    BALANCER-1910
    Если включена опция отбрасывать новые соединения при перегрузке cpu, то,
    когда потребление cpu превышает верхнюю границу,
    новые соединия принимаются и сразу закрываются.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_hold_count=0,
        conn_reject_lo=0,
        conn_reject_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    rejected_conn(ctx)

    check_stats(ctx, 0, 1, 0)


def test_disable_conn_reject(ctx):
    '''
    BALANCER-1910
    Если опция отбрасывать новые соединения при перегрузке cpu выключена, то,
    когда потребление cpu превышает верхнюю границу,
    новые соединия принимаются и обрабатываются.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        conn_hold_count=0,
        conn_reject_lo=0,
        conn_reject_hi=0,
    ))

    wait_cpu_usage_stats(ctx)

    ctx.perform_request(http.request.get())

    check_stats(ctx, 0, 0, 0)


def test_disable_file_conn_reject(ctx):
    '''
    BALANCER-1910
    Если присутствует файл, выключающий все механизмы деградации, то,
    когда потребление cpu превышает верхнюю границу,
    новые соединения принимаются и обрабатываются.
    '''
    file_switch = ctx.manager.fs.create_file('file_switch')

    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_hold_count=0,
        conn_reject_lo=0,
        conn_reject_hi=0,
        disable_file=file_switch,
    ))

    wait_cpu_usage_stats(ctx)

    ctx.perform_request(http.request.get())

    check_stats(ctx, 0, 0, 0)


def test_conn_hold(ctx):
    '''
    BALANCER-1910
    Если установлен conn_hold_count,
    то балансер держит отброшенное соединение conn_hold_duration секунд.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_reject_lo=0,
        conn_reject_hi=0,
        conn_hold_duration="10m",
    ))

    wait_cpu_usage_stats(ctx)

    try:
        ctx.perform_request(http.request.get(), timeout=5)
    except HTTPReaderException as e:
        assert 'connection timed out' in e.message
    else:
        pytest.fail('DID NOT RAISE HTTPReaderException')

    check_stats(ctx, 1, 1, 0)


def test_conn_hold_0s(ctx):
    '''
    BALANCER-1910
    Если установлен conn_hold_count и conn_hold_duration равен 0 секунд,
    то отброшенное соединение сразу закрывается.
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_reject_lo=0,
        conn_reject_hi=0,
        conn_hold_duration="0s",
    ))

    wait_cpu_usage_stats(ctx)

    rejected_conn(ctx)
    check_stats(ctx, 0, 1, 0)


def test_push_checker_address(ctx):
    '''
    BALANCER-3162
    Проверяем что запросы прошедшие через active_check_reply
    не лимитируются если push_checker_address = true
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_hold_count=0,
        conn_reject_lo=0,
        conn_reject_hi=0,
        push_checker_address=True,
    ))

    for i in xrange(10):
        ctx.perform_request(http.request.get('/check'), source_address=('127.1.1.1', 0))

    unistat = ctx.get_unistat()
    assert unistat['worker-cpu_limiter_checker_cache_miss_summ'] == 1
    assert unistat['worker-cpu_limiter_checker_cache_size_ammv'] == 1

    wait_cpu_usage_stats(ctx)

    rejected_conn(ctx)
    check_stats(ctx, 0, 1, 0)

    for i in xrange(10):
        resp = ctx.perform_request(http.request.get('/check'), source_address=('127.1.1.1', 0))

        asserts.status(resp, 200)
        asserts.header_value(resp, 'RS-Weight', '1')


def test_dont_push_checker_address(ctx):
    '''
    BALANCER-3162
    Проверяем что запросы прошедшие через active_check_reply
    лимитируются если push_checker_address = false
    '''
    ctx.start_balancer(CpuLimiterConfig(
        enable_conn_reject=True,
        conn_hold_count=0,
        conn_reject_lo=0,
        conn_reject_hi=0,
    ))

    for i in xrange(10):
        ctx.perform_request(http.request.get('/check'))

    unistat = ctx.get_unistat()
    assert unistat['worker-cpu_limiter_checker_cache_miss_summ'] == 0
    assert unistat['worker-cpu_limiter_checker_cache_size_ammv'] == 0

    wait_cpu_usage_stats(ctx)

    rejected_conn(ctx)
    check_stats(ctx, 0, 1, 0)


def test_push_checker_cache_overflow(ctx):
    '''
    BALANCER-3162
    Проверяем что работает лимит на размер checker cache
    '''
    ctx.start_balancer(CpuLimiterConfig(push_checker_address=True, checker_address_cache_size=16))

    for i in xrange(64):
        ctx.perform_request(http.request.get('/check'), source_address=('127.1.1.{}'.format(1 + i), 0))

    for i in xrange(16):  # Проверяем что кеш rlu
        ctx.perform_request(http.request.get('/check'), source_address=('127.1.1.{}'.format(1 + 48 + i), 0))

    unistat = ctx.get_unistat()
    assert unistat['worker-cpu_limiter_checker_cache_miss_summ'] == 64
    assert unistat['worker-cpu_limiter_checker_cache_size_ammv'] == 16
