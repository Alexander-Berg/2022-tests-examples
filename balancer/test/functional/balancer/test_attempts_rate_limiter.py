# -*- coding: utf-8 -*-
import time
import pytest

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import DummyConfig, SimpleConfig, SimpleHandler
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError

from configs import Balancer2AttemptsRateLimiterConfig, Balancer2AttemptsRateLimiterTwoBackendsConfig


class Simple503Config(SimpleConfig):
    HANDLER_TYPE = SimpleHandler

    def __init__(self):
        super(Simple503Config, self).__init__(http.response.service_unavailable())


ATTEMPTS = 4
COEFF = 0.98
LIMIT = 1.3
WAIT_INTERVAL = 1
ERRORLOG_WAIT_INTERVAL = 3


def _perform_requests(ctx, count=25):
    request = http.request.get()
    old_accepted = ctx.backend.state.accepted.value

    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    time.sleep(WAIT_INTERVAL)

    return ctx.backend.state.accepted.value - old_accepted


def _warmup(ctx):
    _perform_requests(ctx, count=50)


def _check_attempts_limited(ctx, limit=LIMIT):
    count = 500
    attempts = _perform_requests(ctx, count)
    rate = 1.0 * (attempts - count) / count
    assert 0.99 * limit < rate <= 1.01 * limit


def _check_attempts_not_limited(ctx, attempts=ATTEMPTS):
    count = 500
    assert _perform_requests(ctx, count) == count * attempts


@pytest.mark.parametrize('workers', [None, 4])
@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (COEFF, None),
        (None, 10)
    ])
def test_limiting(ctx, workers, coeff, max_budget):
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
        attempts=ATTEMPTS,
        coeff=coeff,
        limit=LIMIT,
        max_budget=max_budget,
        workers=workers,
    ))

    _warmup(ctx)
    _check_attempts_limited(ctx)


@pytest.mark.parametrize('workers', [None, 4])
@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (COEFF, None),
        (None, 10)
    ])
def test_not_limiting_connection_attempts(ctx, workers, coeff, max_budget):
    count = 200

    ctx.start_fake_backend(name='backend')
    ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
        attempts=ATTEMPTS,
        coeff=coeff,
        limit=LIMIT,
        max_budget=max_budget,
        workers=workers,
    ))

    for _ in xrange(50):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    def sum_all_attempts():
        return ctx.get_unistat()['report-test-backend_attempt_summ']

    old_attempts = sum_all_attempts()

    for _ in xrange(count):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    new_attempts = sum_all_attempts()

    assert new_attempts - old_attempts == ATTEMPTS * count


@pytest.mark.parametrize('workers', [None, 4])
@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (COEFF, None),
        (None, 10)
    ])
def test_connection_attempts_passes_to_next_backend(ctx, workers, coeff, max_budget):
    count = 50

    ctx.start_fake_backend(name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')

    ctx.start_balancer(Balancer2AttemptsRateLimiterTwoBackendsConfig(
        attempts=1,
        fast_attempts=2,
        coeff=coeff,
        limit=LIMIT,
        max_budget=max_budget,
        workers=workers,
    ))

    for _ in xrange(100):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    old_accepted = ctx.backend2.state.accepted.value

    for _ in xrange(count):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    # first request passes to first backend, gets ECONNREFUSED then passes to second backend
    assert ctx.backend2.state.accepted.value - old_accepted == count


@pytest.mark.parametrize('workers', [None, 4])
@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (COEFF, None),
        (None, 10)
    ])
def test_fast_attempts_passes_to_next_backend(ctx, workers, coeff, max_budget):
    count = 50

    ctx.start_backend(Simple503Config(), name='backend1')
    ctx.start_backend(DummyConfig(), name='backend2')

    ctx.start_balancer(Balancer2AttemptsRateLimiterTwoBackendsConfig(
        attempts=1,
        fast_attempts=2,
        coeff=coeff,
        limit=LIMIT,
        max_budget=max_budget,
        workers=workers,
    ))

    for _ in xrange(100):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    old_accepted = ctx.backend2.state.accepted.value

    for _ in xrange(count):
        ctx.perform_request_xfail(http.request.get())

    time.sleep(WAIT_INTERVAL)

    # first request passes to first backend, gets 503 then passes to second backend
    assert ctx.backend2.state.accepted.value - old_accepted == count


@pytest.mark.parametrize('workers', [None, 4])
@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (COEFF, None),
        (None, 10)
    ])
def test_not_limiting_single_attempt(ctx, workers, coeff, max_budget):
    count = 25

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
        attempts=1,
        coeff=coeff,
        limit=0.5,
        max_budget=max_budget,
        workers=workers,
    ))

    time.sleep(WAIT_INTERVAL)

    _warmup(ctx)

    attempts = _perform_requests(ctx, count=count)
    assert attempts >= count


@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (0.1, None),
        (None, 10)
    ])
def test_rate_limiter_signals(ctx, coeff, max_budget):
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
        attempts=4,
        coeff=coeff,
        limit=0.1,
        max_budget=max_budget,
    ))

    for _ in range(40):
        ctx.perform_request_xfail(http.request.get())

    limited_attempts = ctx.get_unistat()['report-test-limited_backend_attempt_summ']

    assert limited_attempts > 0


def test_mixed_arl_versions(ctx):
    ctx.start_backend(DummyConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
            attempts=4,
            coeff=0.5,
            limit=0.5,
            max_budget=10
        ))


def test_hedgeds_with_old_arl(ctx):
    ctx.start_backend(DummyConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
            attempts=4,
            coeff=0.5,
            limit=0.5,
            hedged_delay='1s'
        ))


@pytest.mark.parametrize(
    ('coeff', 'max_budget'),
    [
        (0, None),
        (None, 0.5)
    ])
def test_return_last_with_arl(ctx, coeff, max_budget):
    ctx.start_backend(Simple503Config())
    ctx.start_balancer(Balancer2AttemptsRateLimiterConfig(
        attempts=3,
        coeff=coeff,
        limit=0.5,
        max_budget=max_budget,
        return_last_5xx=True
    ))
    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 503)

    limited_attempts = ctx.get_unistat()['report-test-limited_backend_attempt_summ']

    assert limited_attempts > 0
