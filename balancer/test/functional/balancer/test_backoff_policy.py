# -*- coding: utf-8 -*-
import pytest

from configs import Balancer2BackoffPolicyConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts


@pytest.mark.parametrize('prob', [0.0, 1.0])
def test_no_fails(ctx, prob):
    """
    In case of no fails there are no retries
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Balancer2BackoffPolicyConfig(prob1=prob))

    request = http.request.get()
    resp = ctx.perform_request(request)
    asserts.status(resp, 200)

    assert ctx.backend.state.accepted.value == 1


@pytest.mark.parametrize('attempts', [2, 3])
def test_fail_retries_on(ctx, attempts):
    """
    In case of failing to get response and last probability in list is 1.0,
    then next attempts are always performed.
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2BackoffPolicyConfig(prob1=1.0, attempts=attempts))

    request = http.request.get()
    ctx.perform_request_xfail(request)

    assert ctx.backend.state.accepted.value == attempts


@pytest.mark.parametrize('attempts', [2, 3])
def test_fail_retries_off(ctx, attempts):
    """
    In case of failing to get response and last probability in list is 0.0,
    then next attempts are never performed.
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2BackoffPolicyConfig(prob1=0.0, attempts=attempts))

    request = http.request.get()
    ctx.perform_request_xfail(request)

    assert ctx.backend.state.accepted.value == 1


def test_fail_some_retries(ctx):
    """
    In case of failing to get response and first probability is 1.0,
    last probability in list is 0.0, then two attempts are performed.
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2BackoffPolicyConfig(prob1=1.0, prob2=0.0, attempts=3))

    request = http.request.get()
    ctx.perform_request_xfail(request)

    assert ctx.backend.state.accepted.value == 2
