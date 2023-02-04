# -*- coding: utf-8 -*-

from configs import Balancer2SimplePolicyConfig
from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


def test_simple_policy(ctx):
    """
    simple_policy никак не помечает бэкенды, поэтому запрос может попасть в один бэкенд несколько раз.
    """
    ctx.start_backend(SimpleConfig(response=http.response.service_unavailable()), name='first_backend')
    ctx.start_backend(SimpleConfig(response=http.response.service_unavailable()), name='second_backend')
    ctx.start_balancer(Balancer2SimplePolicyConfig())

    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)

    qsize1 = ctx.first_backend.state.requests.qsize()
    qsize2 = ctx.second_backend.state.requests.qsize()
    assert qsize1 > 0
    assert qsize2 > 0
    assert qsize1 + qsize2 == 3
