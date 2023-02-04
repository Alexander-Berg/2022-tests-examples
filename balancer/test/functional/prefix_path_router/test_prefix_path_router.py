# -*- coding: utf-8 -*-
import time

from configs import PrefixPathRouterConfig, PrefixPathRouterSelfGeneratingConfig, PrefixPathRouterNoDefaultConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


def send_requests(ctx, requests_data):
    for path, expected_content in requests_data:
        response = ctx.perform_request(http.request.get(path=path))
        asserts.content(response, expected_content)


def test_prefix_path_router(ctx):
    """
    BALANCER-618
    Тесты на модуль prefix_path_router
    """
    ctx.start_balancer(PrefixPathRouterConfig())
    send_requests(ctx, ((p, 'default') for p in ['/default', '/accept', 'metallica', '/accc']))
    send_requests(ctx, ((p, 'ac') for p in ['/ac', '/ac/', '/ac/c', '/ac/dcdc', '/ac/dcdc/zzz/']))
    send_requests(ctx, ((p, 'acc') for p in ['/acc', '/acc/', '/acc/dc']))
    send_requests(ctx, ((p, 'ac/dc') for p in ['/ac/dc', '/ac/dc/', '/ac/dc/back/in/black']))


def test_prefix_path_router_case_sensitive_by_default(ctx):
    """
    BALANCER-618
    Тесты на модуль prefix_path_router: case_sensitive по умолчанию
    """
    ctx.start_balancer(PrefixPathRouterConfig())
    send_requests(ctx, ((p, 'ac/dc') for p in ['/ac/dc', '/ac/dc/']))
    send_requests(ctx, ((p, 'default') for p in ['/AC/DC', '/aC/dC/']))
    send_requests(ctx, ((p, 'ac') for p in ['/ac/DC', '/ac/']))


def test_prefix_path_router_case_insensitive_mode(ctx):
    """
    BALANCER-618
    Тесты на модуль prefix_path_router: включён параметр case_insensitive
    """
    ctx.start_balancer(PrefixPathRouterConfig(case_insensitive=True))
    send_requests(ctx, ((p, 'ac/dc') for p in ['/ac/dc', '/ac/Dc/', '/AC/DC', '/AC/DC/HELLS/BELLS']))


def test_prefix_path_router_lots_of_paths(ctx):
    """
    BALANCER-618
    Проверяем что prefix_path_router работает с большим количеством путей
    """
    count = 2048

    width = 0
    tmp_count = count
    while tmp_count > 0:
        width += 1
        tmp_count /= 10

    path_format = '/%%0%dd' % width

    ctx.start_balancer(PrefixPathRouterSelfGeneratingConfig(count))
    for i in xrange(count):
        path = path_format % i
        response = ctx.perform_request(http.request.get(path=path))
        asserts.content(response, '%d' % i)


def test_no_default(ctx):
    """
    BALANCER-787
    Проверить на отсутствие падений при нематчинге и отсутсивии default-секции
    """
    ctx.start_balancer(PrefixPathRouterNoDefaultConfig())

    request = http.request.get(path='/ac/dc')
    response = ctx.perform_request(request)
    asserts.content(response, 'ac/dc')

    request = http.request.get(path='/something')
    ctx.perform_request_xfail(request)

    time.sleep(0.2)

    assert ctx.balancer.is_alive()
