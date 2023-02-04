# -*- coding: utf-8 -*-
import time
import pytest

from configs import Balancer2HashPolicyConfig, Balancer2HashPolicyBackendsConfig, Balancer2HashPolicyStabilityConfig

from balancer.test.util.proto.http.stream import HTTPReaderException
from balancer.test.util.predef.handler.server.http import DummyConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts


def test_by_hash_works(ctx):
    """
    BALANCER-1071
    by_hash_policy uses hashes calculated by hasher
    """
    ctx.start_balancer(Balancer2HashPolicyConfig())

    req_a = http.request.get(path='/a')
    req_b = http.request.get(path='/b')

    for _ in xrange(10):
        resp = ctx.perform_request(req_a)
        asserts.status(resp, 200)
        asserts.content(resp, 'first')

    for _ in xrange(10):
        resp = ctx.perform_request(req_b)
        asserts.status(resp, 200)
        asserts.content(resp, 'second')


def _do_test_stability(ctx):
    for hash_value in ['led', 'zeppelin']:
        req_single = http.request.get(path='/single', headers={'hash': hash_value})
        req_multi = http.request.get(path='/multi', headers={'hash': hash_value})

        resp_single = ctx.perform_request(req_single)
        resp_multi = ctx.perform_request(req_multi)

        asserts.status(resp_single, 200)
        asserts.status(resp_multi, 200)

        assert resp_single.data.content == resp_multi.data.content


def test_by_hash_policy_stability(ctx):
    """
    BALANCER-1071
    BALANCER-1304
    by_hash_policy is stable in terms that depends only on the contents
    of switched on backends and their weights
    """
    ctx.start_balancer(Balancer2HashPolicyStabilityConfig())
    _do_test_stability(ctx)


def test_by_hash_policy_stability_weights_file(ctx):
    """
    BALANCER-1071
    BALANCER-1304
    by_hash_policy is stable in terms that depends only on the contents
    of switched on backends and their weights
    """
    weights_file = ctx.manager.fs.create_file('weights')

    ctx.start_balancer(Balancer2HashPolicyStabilityConfig(weights_file=weights_file))

    ctx.manager.fs.rewrite(weights_file, 'first,10\nfourth,5\n,second,2\nfifth,5\n')

    time.sleep(5)

    _do_test_stability(ctx)


def test_by_hash_with_unique_rrobin(ctx):
    """
    BALANCER-3182
    Round robin algorithm should exclude the first backend chosen by by_hash_policy.
    All subsequent requests should be handled by unique_policy. Checking that first
    excluded backend will be not chosen a second time in unique_policy.
    """
    ctx.start_backend(DummyConfig(timeout=0), name='first_backend')
    ctx.start_backend(DummyConfig(timeout=0), name='second_backend')
    ctx.start_balancer(Balancer2HashPolicyBackendsConfig(attempts=5))
    req = http.request.get(path='/a')

    for i in xrange(5):
        with pytest.raises(HTTPReaderException):
            ctx.perform_request(req)

        assert ctx.first_backend.state.requests.qsize() == i+1
        assert ctx.second_backend.state.requests.qsize() == i+1
