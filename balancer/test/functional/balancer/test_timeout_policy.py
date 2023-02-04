# -*- coding: utf-8 -*-
import time

from configs import Balancer2TimeoutPolicyConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts


def test_no_fails(ctx):
    """
    In case of no fails there are no retries
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Balancer2TimeoutPolicyConfig(timeout="100ms"))

    request = http.request.get()
    resp = ctx.perform_request(request)
    asserts.status(resp, 200)

    assert ctx.backend.state.accepted.value == 1


def test_stop_retries_after_timeout(ctx):
    """
    Retries are switched off when timeout of timeout_policy had passed
    """
    backend_processing_time = 6
    ctx.start_backend(DummyConfig(backend_processing_time))
    ctx.start_balancer(Balancer2TimeoutPolicyConfig(timeout='1s'))

    ctx.perform_request_xfail(http.request.get())

    time.sleep(1)

    assert ctx.backend.state.accepted.value == 1


def test_make_retries_before_timeout(ctx):
    """
    Retries are not switched off if timeout of timeout_policy had not passed
    """
    backend_processing_time = 1
    ctx.start_backend(DummyConfig(backend_processing_time))
    ctx.start_balancer(Balancer2TimeoutPolicyConfig(timeout='5s'))

    ctx.perform_request_xfail(http.request.get())

    time.sleep(1)

    assert ctx.backend.state.accepted.value == 2


def test_params_file(ctx):
    """
    BALANCER-1083
    params_file with timeout, value replaces config's value
    """
    backend_processing_time = 6
    params_file = ctx.manager.fs.create_file('params_file')
    ctx.manager.fs.rewrite(params_file, 'timeout, 5s')

    ctx.start_backend(DummyConfig(backend_processing_time))
    ctx.start_balancer(Balancer2TimeoutPolicyConfig(timeout='1s', params_file=params_file))

    time.sleep(2)

    ctx.perform_request_xfail(http.request.get())

    time.sleep(1)

    assert ctx.backend.state.accepted.value == 1


def test_invalid_params_file(ctx):
    """
    BALANCER-1643
    If params_file is invalid, balancer writes a record in errorlog.
    """
    params_file = ctx.manager.fs.create_file('params_file')
    ctx.manager.fs.rewrite(params_file, 'timeout,')

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Balancer2TimeoutPolicyConfig(timeout='1s', params_file=params_file))

    time.sleep(5)

    ctx.perform_request(http.request.get())

    time.sleep(3)

    errorlog = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
    assert 'timeout_policy Error parsing params_file:' in errorlog
