# -*- coding: utf-8 -*-
import pytest

from configs import Balancer2ConnectionAttemptsConfig, Balancer2TwoLevelsConfig, Balancer2ConnectionAttemptsReturnLast5xxConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleHandler, DummyConfig
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stdlib.multirun import Multirun


class Simple503Config(SimpleConfig):
    HANDLER_TYPE = SimpleHandler

    def __init__(self):
        super(Simple503Config, self).__init__(http.response.service_unavailable())


@pytest.mark.parametrize(['connection_attempts', 'fast_attempts'], [(None, None), (None, 0), (0, None)])
def test_default_connection_attempts_off(ctx, connection_attempts, fast_attempts):
    """
    BALANCER-1247
    connection_attempts parameter = 0 by default
    """
    ctx.start_fake_backend()
    ctx.start_backend(SimpleConfig(), name='second_backend')
    ctx.start_balancer(
        Balancer2ConnectionAttemptsConfig(attempts=1, connection_attempts=connection_attempts, fast_attempts=fast_attempts))

    for run in Multirun():
        with run:
            ctx.perform_request_xfail(http.request.get())
            break


@pytest.mark.parametrize(['connection_attempts', 'fast_attempts'], [(None, 1), (1, None)])
def test_connection_attempts_on(ctx, connection_attempts, fast_attempts):
    """
    BALANCER-1247
    when connection_attempts is non-zero, then unsuccesfull connections do not count
    in attempts until connection_attemps are not run to zero
    """
    ctx.start_fake_backend()
    ctx.start_backend(SimpleConfig(), name='second_backend')
    ctx.start_balancer(Balancer2ConnectionAttemptsConfig(attempts=1, connection_attempts=connection_attempts, fast_attempts=fast_attempts))

    resp = ctx.perform_request(http.request.get(data='hello world'))
    asserts.status(resp, 200)


@pytest.mark.parametrize(['connection_attempts', 'fast_attempts'], [(None, 1), (1, None)])
def test_fail_after_connection(ctx, connection_attempts, fast_attempts):
    """
    BALANCER-1247
    When connection succeeds, but sending request does not, it is counted as failure, and
    attemps are decremented, not connection_attempts
    """
    ctx.start_backend(DummyConfig(timeout=1))
    ctx.start_backend(SimpleConfig(), name='second_backend')
    ctx.start_balancer(Balancer2ConnectionAttemptsConfig(attempts=1, connection_attempts=connection_attempts, fast_attempts=fast_attempts))

    ctx.perform_request_xfail(http.request.get())

    assert ctx.backend.state.accepted.value == 1
    assert ctx.second_backend.state.accepted.value == 0


def test_fast_503(ctx):
    """
    BALANCER-1537
    We when we got 503 error we will choose another backend
    """
    ctx.start_backend(Simple503Config())
    ctx.start_backend(SimpleConfig(), name='second_backend')
    ctx.start_balancer(Balancer2ConnectionAttemptsConfig(
        attempts=1,
        fast_attempts=1,
        fast_503=True))

    resp = ctx.perform_request(http.request.get(data='fast_503'))
    asserts.status(resp, 200)


def test_no_fast_503(ctx):
    """
    BALANCER-1537
    We when we got 503 error we will not choose another backend
    """
    ctx.start_backend(Simple503Config())
    ctx.start_backend(SimpleConfig(), name='second_backend')

    ctx.start_balancer(Balancer2ConnectionAttemptsConfig(attempts=1, fast_attempts=1))

    ctx.perform_request_xfail(http.request.get())


def test_fast_503_with_retun_last_5xx(ctx):
    """
    BALANCER-3285
    We when we got 503 error we will choose another backend
    """
    ctx.start_backend(Simple503Config(), name='backend_503_1')
    ctx.start_backend(Simple503Config(), name='backend_503_2')
    ctx.start_backend(Simple503Config(), name='backend_503_3')
    ctx.start_backend(SimpleConfig(), name='backend_last_200')
    ctx.start_balancer(Balancer2ConnectionAttemptsReturnLast5xxConfig(
        attempts=1,
        fast_attempts=3,
        fast_503=True,
        return_last_5xx=True))

    resp = ctx.perform_request(http.request.get(data='fast_503'))
    asserts.status(resp, 200)


def test_incompatible_options(ctx):
    """
    BALANCER-1537
    We cannot use connection_attempts along with fast_attempts or fast_503
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_fake_backend(name='second_backend')

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Balancer2ConnectionAttemptsConfig(
            attempts=1,
            connection_attempts=1,
            fast_attempts=1))

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Balancer2ConnectionAttemptsConfig(
            attempts=1,
            connection_attempts=1,
            fast_503=True))


def test_two_levels(ctx):
    """
    BALANCER-1537
    In two level configuration with one attempt and connection_attempts we will stop on timeout or 503
    """
    ctx.start_backend(DummyConfig(timeout=3), name='backend_timeouted')
    ctx.start_backend(Simple503Config(), name='backend_503')
    ctx.start_fake_backend(name='backend_fake')

    ctx.start_balancer(Balancer2TwoLevelsConfig
                       (connection_attempts=3))

    requests_count = 10
    for i in range(requests_count):
        ctx.perform_request(http.request.get())

    assert ctx.backend_timeouted.state.accepted.value > 0
    assert ctx.backend_503.state.accepted.value > 0
    assert ctx.backend_timeouted.state.accepted.value + ctx.backend_503.state.accepted.value == requests_count


def test_two_levels_fast_503(ctx):
    """
    BALANCER-1537
    In two level configuration with one attempt and fast_503 we will stop only on timeout
    """

    ctx.start_backend(DummyConfig(timeout=3), name='backend_timeouted')
    ctx.start_backend(Simple503Config(), name='backend_503')
    ctx.start_fake_backend(name='backend_fake')

    ctx.start_balancer(Balancer2TwoLevelsConfig
                       (fast_attempts=3,
                        fast_503=True))

    requests_count = 10
    for i in range(requests_count):
        ctx.perform_request(http.request.get())

    assert ctx.backend_timeouted.state.accepted.value > 0
    assert ctx.backend_503.state.accepted.value > 0
    assert ctx.backend_timeouted.state.accepted.value == requests_count
