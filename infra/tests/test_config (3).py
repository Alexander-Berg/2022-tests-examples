# -*- coding: utf-8 -*-
from balancer.test.util import asserts
from balancer.test.util.predef import http

from balancer.test.util.predef.handler.server.http import SimpleConfig


DEFAULT_ADDR = ('2a02:6b8:0:3400::1:36', 80)


def test_default(ctx):
    response = ctx.perform_awacs_request(DEFAULT_ADDR, http.request.get(headers={'Host': 'Unknown'}))

    asserts.status(response, 404)
    asserts.content(response, 'Unknown host')


def test_sasruweb_noapache(ctx):
    backend = ctx.start_awacs_backend('production_noapache_sas_web_rkub', SimpleConfig())
    response = ctx.perform_awacs_request(DEFAULT_ADDR, http.request.get(headers={'Host': 'sasruweb.noapache.yandex.net'}))
    backend_req = backend.state.get_request()

    asserts.status(response, 200)
    asserts.header_value(backend_req, 'Host', 'sasruweb.noapache.yandex.net')
