# -*- coding: utf-8 -*-
import pytest
from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


DEFAULT_ADDR = ('2a02:6b8:0:3400::3:21', 443)


@pytest.mark.parametrize(
    'host',
    [
        'exp.yandex-team.ru',
        'exp-beta.yandex-team.ru',
        'ab.yandex-team.ru',
        'ab-lb.search.yandex.net',
    ],
)
def test_ab_upstream(ctx, host):
    content = 'Led Zeppelin'
    ctx.start_awacs_backend('production_expadm', SimpleConfig(http.response.ok(data=content)))
    response = ctx.perform_awacs_request(DEFAULT_ADDR, http.request.get(headers={'Host': host}), ssl=True)

    asserts.status(response, 200)
    asserts.content(response, content)
