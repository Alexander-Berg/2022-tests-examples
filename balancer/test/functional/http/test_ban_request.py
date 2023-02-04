# -*- coding: utf-8 -*-

from configs import HTTPStaticConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.predef import http


def assert_stats(ctx, num):
    unistat = ctx.get_unistat()
    assert unistat['http-banned_requests_summ'] == num


def test_ip(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,ip,127\.0\.0\.1')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get(), source_address=('127.0.0.1', 0))
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_ip_not_banned(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,ip,127\.0\.0\.1')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get(), source_address=('127.1.2.3', 0))
    asserts.status(response, 200)
    assert_stats(ctx, 0)


def test_ipv6(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,ip,::[12]')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_path(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,path,/te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/test'))
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_path_not_banned(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,path,/te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/te'))
    asserts.status(response, 200)
    assert_stats(ctx, 0)


def test_cgi(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,cgi,\?te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/qwerty?test'))
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_cgi_not_banned(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,cgi,\?te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/qwerty?te'))
    asserts.status(response, 200)
    assert_stats(ctx, 0)


def test_url(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,url,/te.+\?te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/test?test'))
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_url_not_banned(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,url,/te.+\?te.+')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/testtesttesttest'))
    asserts.status(response, 200)
    assert_stats(ctx, 0)


def test_header(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,header,qwert[y]:[a]sdfgh')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get(headers={
        'qwertya': 'sdfgh',
        'qwert': 'a',
        'qwerty': 'asdfgh',
    }))
    asserts.status(response, 404)
    assert_stats(ctx, 1)


def test_header_not_banned(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, r'1,404,header,qwert[y]:[a]sdfgh')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get(headers={'qwerty': 'a'}))
    asserts.status(response, 200)

    response = ctx.perform_request(http.request.get(headers={'qwertya': 'sdfgh'}))
    asserts.status(response, 200)

    assert_stats(ctx, 0)


def test_probability(ctx):
    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, '0.5,404,path,/test')
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    banned = 0
    for i in range(500):
        response = ctx.perform_request(http.request.get('/test'))
        banned += response.status == 404
    assert 200 <= banned <= 300


def test_many_rules(ctx):
    rules = '\n'.join([r'1,404,ip,127.0.0.1', r'1,403,path,/test'])

    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, rules)
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get(), source_address=('127.0.0.1', 0))
    asserts.status(response, 404)
    assert_stats(ctx, 1)

    response = ctx.perform_request(http.request.get('/test'))
    asserts.status(response, 403)
    assert_stats(ctx, 2)

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    assert_stats(ctx, 2)


def test_first_bad_rule(ctx):
    rules = '\n'.join([r'1,404,test,test', r'1,404,path,*', r'1,404,path,[', r'1,404,path,/test'])

    ban_requests_file = ctx.manager.fs.create_file('ban_requests_file')
    ctx.manager.fs.rewrite(ban_requests_file, rules)
    ctx.start_balancer(HTTPStaticConfig(
        ban_requests_file=ban_requests_file
    ))

    response = ctx.perform_request(http.request.get('/test'))
    asserts.status(response, 404)
    assert_stats(ctx, 1)
