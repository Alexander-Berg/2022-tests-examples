# -*- coding: utf-8 -*-

from configs import MetaConfig

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.balancer import asserts


def read_accesslog(ctx):
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert accesslog, 'accesslog is empty'
            return accesslog


def test_meta(ctx):
    """
    Logging set-cookie metadata
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(MetaConfig())
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '[meta test-id' in accesslog
    assert '<::fieldA:aaa::>' in accesslog
    assert '<::fieldB:bbb::>' in accesslog
