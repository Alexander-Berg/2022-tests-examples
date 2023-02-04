# -*- coding: utf-8 -*-
import time
import pytest

from configs import Balancer2NamePolicyConfig, Balancer2NameFromHeaderPolicyConfig, Balancer2NameFromDefaultHeaderPolicyConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig
#  from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from balancer.test.util import asserts


def setup_env(ctx, name, from_header, attempts=None, weights_file=None, strict=None, allow_zero_weights=None):
    if from_header == 'default':
        ctx.start_balancer(Balancer2NameFromDefaultHeaderPolicyConfig(attempts=attempts, weights_file=weights_file, strict=strict, allow_zero_weights=allow_zero_weights))
        request = http.request.get(headers={'X-Yandex-Balancing-Hint': name + '_hint'})
    elif from_header == 'specified':
        header_name = 'X-Yandex-Another-Balancing-Hint'
        ctx.start_balancer(Balancer2NameFromHeaderPolicyConfig(header_name=header_name, attempts=attempts, weights_file=weights_file, strict=strict, allow_zero_weights=allow_zero_weights))
        request = http.request.get(headers={header_name: name + '_hint'})
    elif from_header == 'none':
        ctx.start_balancer(Balancer2NamePolicyConfig(name=name, attempts=attempts, weights_file=weights_file, strict=strict, allow_zero_weights=allow_zero_weights))
        request = http.request.get()

    return request

from_header_modes = ['none', 'default', 'specified']


@pytest.mark.parametrize('from_header', from_header_modes)
@pytest.mark.parametrize('name', ['first', 'second'])
def test_by_name_policy(ctx, name, from_header):
    """
    by_name_policy selects backend according to its name.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='first')), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, name, from_header)

    for _ in xrange(3):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)
        asserts.content(resp, name)


@pytest.mark.parametrize('from_header', from_header_modes)
def test_by_name_policy_fail(ctx, from_header):
    """
    When by_name_policy backend fails, another one is selected
    """
    ctx.start_backend(DummyConfig(), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, 'first', from_header, attempts=2)

    for _ in xrange(3):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)
        asserts.content(resp, 'second')


@pytest.mark.parametrize('from_header', from_header_modes)
def test_by_name_policy_stict_fail(ctx, from_header):
    """
    When by_name_policy backend fails, another one is selected
    """
    ctx.start_backend(DummyConfig(), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, 'first', from_header, attempts=2, strict=True)

    for _ in xrange(3):
        ctx.perform_request_xfail(request)


@pytest.mark.parametrize('from_header', from_header_modes)
def test_by_name_policy_missing(ctx, from_header):
    """
    BALANCER-1322
    Normal alogrithm logic applies when by_name_policy/name contains
    name of non-existent backend.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='first')), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, 'zeppelin', from_header)

    responses = set()

    for _ in xrange(3):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)
        content = resp.data.content
        responses.add(content)

    assert len(responses) == 2, 'not all backends were selected'


@pytest.mark.parametrize('from_header', from_header_modes)
def test_by_name_policy_switched_off(ctx, from_header):
    """
    BALANCER-1322
    Normal alogrithm logic applies when by_name_policy/name contains
    name of backend switched off by file weights.
    """
    weights_file = ctx.manager.fs.create_file('weights')
    ctx.manager.fs.rewrite(weights_file, 'first,-1\nsecond,1\n')

    time.sleep(2)

    ctx.start_backend(SimpleConfig(response=http.response.ok(data='first')), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, 'first', from_header, attempts=2, weights_file=weights_file)

    for _ in xrange(3):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)
        asserts.content(resp, 'second')


@pytest.mark.parametrize('from_header', from_header_modes)
def test_by_name_policy_switched_off_allowed_zero_weights(ctx, from_header):
    """
    BALANCER-1491
    Ignore zero file weigths on allow_zero_weights option
    """
    weights_file = ctx.manager.fs.create_file('weights')
    ctx.manager.fs.rewrite(weights_file, 'first,-1\nsecond,1\n')

    time.sleep(2)

    ctx.start_backend(SimpleConfig(response=http.response.ok(data='first')), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='second')), name='second_backend')

    request = setup_env(ctx, 'first', from_header, attempts=2, weights_file=weights_file, allow_zero_weights=True)

    for _ in xrange(3):
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)
        asserts.content(resp, 'first')
