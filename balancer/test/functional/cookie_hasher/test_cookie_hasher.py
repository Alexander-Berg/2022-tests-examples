# -*- coding: utf-8 -*-
import time

from configs import CookieHasherConfig, CookieHasherByHashConfig

from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts


def _make_cookie_headers(pairs_iter):
    value = ';'.join('{}={}'.format(key, value) for key, value in pairs_iter)
    return {'Cookie': value}


def test_same_hash(ctx):
    """
    BALANCER-1106
    cookie_hasher takes value of "cookie" and calcs hash from it.
    Two similar requests must have the same result of backend selection
    """
    cookie_name = 'Led'
    cookie_value = 'Zeppelin'
    ctx.start_balancer(CookieHasherConfig(cookie=cookie_name))

    headers_a = _make_cookie_headers([(cookie_name, cookie_value)])
    headers_b = _make_cookie_headers([(cookie_name, cookie_value), ('domain', '.yandex.ru')])

    for headers in [headers_a, headers_b]:
        response = ctx.perform_request(http.request.get(headers=headers))
        asserts.status(response, 200)
        asserts.content(response, '2')


def test_combine_hash(ctx):
    """
    BALANCER-3068
    cookie_hasher combines the results of two different hashers
    """
    cookie_prev = 'A'
    cookie = 'B'
    ctx.start_balancer(CookieHasherConfig(
        cookie_prev=cookie_prev,
        cookie=cookie,
        combine_hashes=True,
        randomize_empty_match=False,
        backends_count=10,
    ))

    response = ctx.perform_request(http.request.get(headers={'Cookie': '{}=Value'.format(cookie_prev)}))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # should produce the same result as above
    response = ctx.perform_request(http.request.get(headers={'Cookie': '{}=Value'.format(cookie)}))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # combined, the hashers should produce a different hash and rebalance the request
    response = ctx.perform_request(http.request.get(headers={'Cookie': '{}=Value; {}=Value'.format(cookie, cookie_prev)}))
    asserts.status(response, 200)
    asserts.content(response, '9')


def test_value_no_ignore_case(ctx):
    """
    BALANCER-1106
    cookie_hasher takes value of "cookie" and calcs hash from it.
    Two similar request must have the same result of backend selection.
    Case of header values is not ignored
    """
    cookie_name = 'Led'
    cookie_value = 'Zeppelin'
    ctx.start_balancer(CookieHasherConfig(cookie=cookie_name))

    headers_a = _make_cookie_headers([(cookie_name, cookie_value)])
    response_a = ctx.perform_request(http.request.get(headers=headers_a))
    asserts.content(response_a, '2')

    headers_b = _make_cookie_headers([(cookie_name, cookie_value.lower())])
    response_b = ctx.perform_request(http.request.get(headers=headers_b))
    asserts.content(response_b, '3')


def test_randomize_empty_match(ctx):
    """
    BALANCER-1106
    If no hash header found, the hash must become random.
    """
    ctx.start_balancer(CookieHasherConfig(cookie='Led'))
    contents = set()
    request = http.request.get()
    for _ in xrange(10):
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        contents.add(response.data.content)
        if len(contents) > 1:
            break

    assert len(contents) > 1, 'there must be more than one response'


def test_randomize_empty_match_off(ctx):
    """
    BALANCER-1106
    If randomize_empty_match set to 0, then no randomization
    for not matching requests happens
    """
    ctx.start_balancer(CookieHasherConfig(cookie='Led', randomize_empty_match=0))
    contents = set()
    request = http.request.get()
    for _ in xrange(10):
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        contents.add(response.data.content)
        if len(contents) > 1:
            break

    assert len(contents) == 1, 'there must be only one response due to the the constant hash'


def test_file_switch(ctx):
    """
    BALANCER-1106
    If file_switch file exists then no hash is calculated, randomize_empty_match is no-op too
    """
    file_switch = ctx.manager.fs.create_file('file_switch')
    ctx.start_balancer(CookieHasherByHashConfig(cookie='Led', file_switch=file_switch))
    time.sleep(2)
    contents = set()
    headers = _make_cookie_headers([('Led', 'Zeppelin')])
    request = http.request.get(headers=headers)

    for _ in xrange(10):
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        contents.add(response.data.content)

    assert len(contents) > 1, 'there must be multiple responses when file_switch is on'

    ctx.manager.fs.remove(file_switch)
    time.sleep(2)
    contents_hashed = set()
    for _ in xrange(10):
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        contents_hashed.add(response.data.content)
        if len(contents_hashed) > 1:
            break

    assert len(contents_hashed) == 1, 'there must be only one response due to the the constant hash'
