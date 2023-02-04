# -*- coding: utf-8 -*-
from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


ALL_BACKENDS = ['addrs_upper_man', 'addrs_upper_sas', 'addrs_upper_vla']
DEFAULT_ADDR = ('2a02:6b8:0:3400::2:68', 17140)


def test_headers(ctx):
    backend = ctx.start_awacs_backend(ALL_BACKENDS, SimpleConfig())
    response = ctx.perform_awacs_request(DEFAULT_ADDR, http.request.get())
    backend_req = backend.state.get_request()

    asserts.status(response, 200)
    asserts.headers(backend_req, ['x-start-time', 'x-req-id', 'x-source-port-y', 'x-forwarded-for-y'])
