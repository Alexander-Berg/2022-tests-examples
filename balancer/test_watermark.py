# -*- coding: utf-8 -*-
import time
import pytest

from configs import Balancer2WatermarkPolicyConfig

from balancer.test.util.predef.handler.server.http import ThreeModeConfig, SimpleConfig, DummyConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from balancer.test.util import asserts


@pytest.mark.parametrize('workers', [None, 4])
def test_no_fails(ctx, workers):
    """
    In case of no fails there are no retries
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(lo=0.5, hi=0.7, workers=workers, shared=workers is not None))

    request = http.request.get()
    requests_count = 50
    for _ in xrange(requests_count):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)

    assert ctx.backend.state.accepted.value == requests_count


@pytest.mark.parametrize('hi', [0.2, 0.5, 0.7])
@pytest.mark.parametrize('attempts', [2, 4])
@pytest.mark.parametrize('workers', [None, 4])
def test_stop_retries_on_fail_excceding_hi(ctx, hi, attempts, workers):
    """
    Retries are switched off when fail rate becomes greater than hi
    """
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(lo=0.1, hi=hi, attempts=attempts, workers=workers, shared=workers is not None))

    request = http.request.get()
    requests_before_retries_stop = int(hi / 0.01)
    for _ in xrange(requests_before_retries_stop / attempts):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert ctx.backend.state.accepted.value == (requests_before_retries_stop / attempts) * attempts  # fixing int conversion error
    actual_accepted = ctx.backend.state.accepted.value

    count = 10
    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert (ctx.backend.state.accepted.value - actual_accepted) <= (count * attempts)


@pytest.mark.parametrize('workers', [None, 4])
def test_enable_retries_on_success_exceeding_lo(ctx, workers):
    """
    Retries are switched off when fail rate becomes greater than hi.
    Retries are switched back on when success rate becomes greater than lo
    """
    attempts = 2
    hi = 0.5
    lo = 0.25

    request = http.request.get()
    requests_before_retries_stop = int(hi / 0.01)
    requests_before_retries_start = int((hi - lo) / 0.01)

    ctx.start_backend(ThreeModeConfig(prefix=requests_before_retries_stop, first=requests_before_retries_start))
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(lo=lo, hi=hi, attempts=attempts, workers=workers, shared=workers is not None))

    for _ in xrange(requests_before_retries_stop / attempts):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert ctx.backend.state.accepted.value == (requests_before_retries_stop / attempts) * attempts  # fixing int conversion error
    actual_accepted = ctx.backend.state.accepted.value

    count = requests_before_retries_start
    for _ in xrange(count):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)

    time.sleep(1)
    assert (ctx.backend.state.accepted.value - actual_accepted) == count
    actual_accepted = ctx.backend.state.accepted.value

    count = 10/attempts
    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    assert (ctx.backend.state.accepted.value - actual_accepted) == count * attempts


def test_bad_hi_lo_relation(ctx):
    """
    lo must be not greater than hi
    """
    ctx.start_backend(SimpleConfig())
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Balancer2WatermarkPolicyConfig(hi=0.5, lo=0.7))


@pytest.mark.parametrize('file_data', [None, '', 'sas, 1.0'], ids=['no_file', 'empty_file', 'not_relevant_file'])
@pytest.mark.parametrize('workers', [None, 4])
def test_params_file_without_hi_lo(ctx, file_data, workers):
    """
    BALANCER-1082
    Retries are switched off when fail rate becomes greater than hi.
    If params_file contains no lo|hi, <value>, then balancer uses values from config
    """
    attempts = 2
    hi = 0.2

    params_file = ctx.manager.fs.create_file('params_file')
    if file_data is None:
        ctx.manager.fs.remove(params_file)
    else:
        ctx.manager.fs.rewrite(params_file, file_data)

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(lo=0.1, hi=hi, attempts=attempts, params_file=params_file))

    request = http.request.get()
    requests_before_retries_stop = int(hi / 0.01)
    for _ in xrange(requests_before_retries_stop / attempts):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert ctx.backend.state.accepted.value == (requests_before_retries_stop / attempts) * attempts  # fixing int conversion error
    actual_accepted = ctx.backend.state.accepted.value

    count = 10
    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert (ctx.backend.state.accepted.value - actual_accepted) <= (count * attempts)


def _exceed_and_check_attempts(ctx, hi, attempts):
    old_accepted = ctx.backend.state.accepted.value
    request = http.request.get()
    requests_before_retries_stop = int(hi / 0.01)
    for _ in xrange(requests_before_retries_stop / attempts):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert ctx.backend.state.accepted.value - old_accepted == (requests_before_retries_stop / attempts) * attempts  # fixing int conversion error


def _check_attempts_limited(ctx, attempts, count=10):
    request = http.request.get()
    old_accepted = ctx.backend.state.accepted.value

    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    time.sleep(1)
    assert ctx.backend.state.accepted.value - old_accepted < count * attempts


def _check_attempts_not_limited(ctx, attempts, count=10):
    request = http.request.get()
    old_accepted = ctx.backend.state.accepted.value

    for _ in xrange(count):
        ctx.perform_request_xfail(request)

    time.sleep(1)

    assert ctx.backend.state.accepted.value - old_accepted == count * attempts


@pytest.mark.parametrize('workers', [None, 4])
def test_params_file_with_hi(ctx, workers):
    """
    BALANCER-1082
    Retries are switched off when fail rate becomes greater than hi.
    If params_file contains hi, <value>, then balancer replaces config's values with the files' ones
    """
    attempts = 2
    config_hi = 0.2
    file_hi = 0.5

    params_file = ctx.manager.fs.create_file('params_file')
    ctx.manager.fs.rewrite(params_file, 'hi, {}'.format(file_hi))

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(
        lo=0.1,
        hi=config_hi,
        attempts=attempts,
        params_file=params_file,
        workers=workers,
        shared=workers is not None
    ))

    time.sleep(5)

    _exceed_and_check_attempts(ctx, file_hi, attempts)
    _check_attempts_limited(ctx, attempts)


@pytest.mark.parametrize('workers', [None, 4])
def test_invalid_params_file(ctx, workers):
    """
    BALANCER-1643
    If params_file is invalid, balancer writes a record in errorlog.
    """
    params_file = ctx.manager.fs.create_file('params_file')
    ctx.manager.fs.rewrite(params_file, 'hi,')

    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(
        lo=0.1,
        hi=0.5,
        params_file=params_file,
        workers=workers,
        shared=workers is not None
    ))

    time.sleep(5)

    ctx.perform_request(http.request.get())

    time.sleep(3)

    errorlog = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
    assert 'watermark_policy Error parsing params_file:' in errorlog


@pytest.mark.parametrize('workers', [None, 4])
def test_switch_file_disable(ctx, workers):
    attempts = 4
    hi = 0.2
    switch_file = ctx.manager.fs.create_file('switch_file')

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(
        lo=0.1,
        hi=hi,
        attempts=attempts,
        switch_file=switch_file,
        switch_key='test_switch',
        switch_default=True,
        workers=workers,
        shared=workers is not None
    ))

    time.sleep(5)
    _exceed_and_check_attempts(ctx, hi, attempts)
    _check_attempts_limited(ctx, attempts)

    ctx.manager.fs.rewrite(switch_file, 'test_switch,0')

    time.sleep(2)
    _check_attempts_not_limited(ctx, attempts, count=100)


@pytest.mark.parametrize('workers', [None, 4])
def test_switch_file_with_non_matching_key(ctx, workers):
    attempts = 4
    hi = 0.2
    switch_file = ctx.manager.fs.create_file('switch_file')

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(
        lo=0.1,
        hi=hi,
        attempts=attempts,
        switch_file=switch_file,
        switch_key='test_switch',
        switch_default=False,
        workers=workers,
        shared=workers is not None
    ))

    time.sleep(5)
    _exceed_and_check_attempts(ctx, hi, attempts)
    _check_attempts_not_limited(ctx, attempts)

    ctx.manager.fs.rewrite(switch_file, 'not_a_test_switch,1')

    time.sleep(2)
    _exceed_and_check_attempts(ctx, hi, attempts)
    _check_attempts_not_limited(ctx, attempts)


@pytest.mark.parametrize('workers', [None, 4])
def test_invalid_switch_file(ctx, workers):
    """
    BALANCER-1643
    If switch_file is invalid, balancer writes a record in errorlog.
    """
    switch_file = ctx.manager.fs.create_file('switch_file')
    ctx.manager.fs.rewrite(switch_file, 'test_switch,')

    ctx.start_backend(DummyConfig())
    ctx.start_balancer(Balancer2WatermarkPolicyConfig(
        lo=0.1,
        hi=0.2,
        switch_file=switch_file,
        switch_key='test_switch',
        workers=workers,
        shared=workers is not None
    ))

    time.sleep(5)

    ctx.perform_request_xfail(http.request.get())

    time.sleep(3)

    errorlog = ctx.manager.fs.read_file(ctx.balancer.config.errorlog)
    assert 'watermark_policy Error parsing switch_file:' in errorlog
