# -*- coding: utf-8 -*-

from configs import RedirectsConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


def test_get_301(ctx):
    respData = 'RESPONSE'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)))
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.get(
        path='/',
        headers={"host": "maps.yandex.ru"}
    ))
    asserts.status(resp, 301)
    asserts.header_value(resp, "location", "https://yandex.ru/maps")
    asserts.empty_content(resp)


def test_get_rewrited_301(ctx):
    respData = 'RESPONSE'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)))
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.get(
        path='/xxx/yyy/zzz/./?abc',
        headers={"host": "maps.yandex.ru"}
    ))
    asserts.status(resp, 301)
    asserts.header_value(resp, "location", "https://yandex.ru/maps/xxx/yyy?abc&zzz")
    asserts.empty_content(resp)


def test_get_302(ctx):
    respData = 'RESPONSE'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)))
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.get(
        path='/things/xxx?yyy',
        headers={"host": "yandex.ru"}
    ))
    asserts.status(resp, 302)
    asserts.header_value(resp, "location", "https://store.turbo.site/xxx?yyy")
    asserts.empty_content(resp)


def test_post(ctx):
    reqData = 'REQUEST'
    respData = 'RESPONSE'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)))
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.post(
        path='/things/xxx?yyy',
        headers={"host": "yandex.ru"},
        data=reqData
    ))
    asserts.status(resp, 302)
    asserts.header_value(resp, "location", "https://store.turbo.site/xxx?yyy")
    asserts.empty_content(resp)


def test_no_match(ctx):
    reqData = 'REQUEST'
    respData = 'RESPONSE'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)))
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.post(
        path='/things/xxx?yyy',
        headers={"host": "www.yandex.ru"},
        data=reqData
    ))
    asserts.status(resp, 200)
    asserts.content(resp, respData)

    req = ctx.backend.state.get_request()
    asserts.path(req, '/things/xxx?yyy')
    asserts.header_value(req, 'host', 'www.yandex.ru')
    asserts.content(req, reqData)


def test_post_forward(ctx):
    reqData = 'REQUEST'
    respData = 'WELL-KNOWN'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=respData)), name='forward')
    ctx.start_balancer(RedirectsConfig())

    resp = ctx.perform_request(http.request.post(
        path='/.well-known/./whatever?xxx',
        headers={"host": "maps.yandex.ru"},
        data=reqData
    ))
    asserts.status(resp, 200)
    asserts.content(resp, respData)

    req = ctx.forward.state.get_request()
    asserts.method(req, "POST")
    asserts.path(req, '/whatever')
    asserts.header_value(req, 'host', 'maps.s3.yandex.net')
    asserts.content(req, reqData)
