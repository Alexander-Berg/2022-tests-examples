# -*- coding: utf-8 -*-
import pytest
import threading

from configs import ModActiveCheckReplyDefaultConfig

from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.sync import RereadsWatcher


def check_response(use_header, use_body, response, value):
    asserts.status(response, 200)

    if use_header is True or (use_body is None and use_header is None):
        asserts.header_value(response, 'RS-Weight', str(value))
    else:
        asserts.no_header_value(response, 'RS-Weight', str(value))

    if use_body is True:
        asserts.content(response, 'rs_weight=' + str(value))
    else:
        asserts.content(response, '')


@pytest.mark.parametrize('force_conn_close', [False, True, None])
def test_force_conn_close(ctx, force_conn_close):
    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(force_conn_close=force_conn_close))

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(http.request.get())

        if force_conn_close is False:
            asserts.is_not_closed(conn.sock)
        else:
            asserts.is_closed(conn.sock)

    asserts.status(resp, 200)


@pytest.mark.parametrize(['use_header', 'use_body'], [(None, None), (False, True), (True, False), (True, True)])
def test_default(ctx, use_header, use_body):
    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(use_header=use_header, use_body=use_body))

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(http.request.get())

    check_response(use_header, use_body, resp, 1)


@pytest.mark.parametrize(['use_header', 'use_body'], [(None, None), (False, True), (True, False), (True, True)])
def test_default_override(ctx, use_header, use_body):
    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(default_weight=10, use_header=use_header, use_body=use_body))

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(http.request.get())

    check_response(use_header, use_body, resp, 10)


@pytest.mark.parametrize('workers', [1, 5])
def test_weight_file(ctx, workers):
    weight_file = ctx.manager.fs.create_file('weight')

    ctx.manager.fs.rewrite(weight_file, '100\n')

    watcher = RereadsWatcher(ctx, weight_file)

    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(workers=workers, default_weight=10, weight_file=weight_file))
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '100')

    ctx.manager.fs.rewrite(weight_file, '5000\n')
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '1000')

    ctx.manager.fs.rewrite(weight_file, '-1\n')
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '10')

    ctx.manager.fs.rewrite(weight_file, '1000\n')
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '1000')

    ctx.manager.fs.rewrite(weight_file, '0\n')
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '0')

    ctx.manager.fs.remove(weight_file)
    watcher.wait_reread()

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    asserts.header_value(resp, 'RS-Weight', '10')


def test_wrong_options(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ModActiveCheckReplyDefaultConfig(use_header=False, use_body=False))

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ModActiveCheckReplyDefaultConfig(default_weight=100000))


def test_zero_weight_at_shutdown(ctx):
    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(
        workers=10,
        shutdown_accept_connections=True,
        zero_weight_at_shutdown=True,
    ))

    def start_shutdown():
        ctx.graceful_shutdown(timeout='60s')

    def check_weight():
        resp = ctx.perform_request(http.request.get())
        asserts.status(resp, 200)
        asserts.header_value(resp, 'RS-Weight', '0')

    thread = None
    try:
        conn = ctx.create_http_connection()
        stream = conn.create_stream()
        stream.write_request_line('POST / HTTP/1.1')
        stream.write_header('Transfer-Encoding', 'chunked')
        stream.end_headers()

        thread = threading.Thread(target=start_shutdown)
        thread.start()

        for run in Multirun():
            with run:
                check_weight()

        for i in range(100):
            check_weight()

        stream.write_chunk('')
        resp = stream.read_response()
        asserts.status(resp, 200)
    finally:
        ctx.balancer.set_finished()
        if thread is not None:
            thread.join()


def test_zero_weight_at_cooldown(ctx):
    ctx.start_balancer(ModActiveCheckReplyDefaultConfig(
        workers=1,
        zero_weight_at_shutdown=True,
    ))

    def start_shutdown():
        ctx.graceful_shutdown(cooldown='10s')

    def check_weight():
        resp = ctx.perform_request(http.request.get())
        asserts.status(resp, 200)
        asserts.header_value(resp, 'RS-Weight', '0')

    thread = None
    try:
        thread = threading.Thread(target=start_shutdown)
        thread.start()

        for run in Multirun():
            with run:
                check_weight()

        for i in range(10):
            check_weight()

    finally:
        ctx.balancer.set_finished()
        if thread is not None:
            thread.join()
