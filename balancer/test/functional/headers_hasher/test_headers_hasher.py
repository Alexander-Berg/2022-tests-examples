# -*- coding: utf-8 -*-
import time

from configs import HeadersHasherConfig, HeadersHasherByHashConfig

from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts


def test_same_hash(ctx):
    """
    BALANCER-815
    headers_hasher takes value of "header_name" and calcs hash from it.
    Two similar request must have the same result of backend selection
    """
    header_name = 'Led'
    header_value = 'Zeppelin'
    ctx.start_balancer(HeadersHasherConfig(header_name=header_name))

    headers_a = {header_name: header_value}
    headers_b = {header_name: header_value, 'Black': 'Sabbath'}

    for headers in [headers_a, headers_b]:
        response = ctx.perform_request(http.request.get(headers=headers))
        asserts.status(response, 200)
        asserts.content(response, '2')


def test_combine_hash(ctx):
    """
    BALANCER-3068
    headers_hasher combines the results of two different hashers
    """
    header_name_prev = 'A'
    header_name = 'B'
    ctx.start_balancer(HeadersHasherConfig(
        header_name_prev=header_name_prev,
        header_name=header_name,
        combine_hashes=True,
        randomize_empty_match=False,
        backends_count=10,
    ))

    response = ctx.perform_request(http.request.get(headers={
        header_name_prev: 'Value'
    }))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # should produce the same result as above
    response = ctx.perform_request(http.request.get(headers={
        header_name: 'Value'
    }))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # combined, the hashers should produce a different hash and rebalance the request
    response = ctx.perform_request(http.request.get(headers={
        header_name_prev: 'Value',
        header_name: 'Value',
    }))
    asserts.status(response, 200)
    asserts.content(response, '9')


def test_name_ignore_case(ctx):
    """
    BALANCER-815
    headers_haser takes value of "header_name" and calcs hash from it.
    Two similar request must have the same result of backend selection.
    Case of header keys is ignored
    """
    header_name = 'Led'
    header_value = 'Zeppelin'
    ctx.start_balancer(HeadersHasherConfig(header_name=header_name))

    headers_a = {header_name: header_value}
    headers_b = {header_name.lower(): header_value}

    for headers in [headers_a, headers_b]:
        response = ctx.perform_request(http.request.get(headers=headers))
        asserts.status(response, 200)
        asserts.content(response, '2')


def test_value_no_ignore_case(ctx):
    """
    BALANCER-815
    headers_haser takes value of "header_name" and calcs hash from it.
    Two similar request must have the same result of backend selection.
    Case of header values is not ignored
    """
    header_name = 'Led'
    header_value = 'Zeppelin'
    ctx.start_balancer(HeadersHasherConfig(header_name=header_name))

    headers_a = {header_name: header_value}
    response_a = ctx.perform_request(http.request.get(headers=headers_a))
    asserts.content(response_a, '2')

    headers_b = {header_name: header_value.lower()}
    response_b = ctx.perform_request(http.request.get(headers=headers_b))
    asserts.content(response_b, '3')


def test_multiple_match(ctx):
    """
    BALANCER-815
    If multiple headers match the request, all of them are counted in hash calculation
    """
    header_name_a = 'Led'
    header_name_b = 'Black'
    ctx.start_balancer(HeadersHasherConfig(header_name='|'.join([header_name_a, header_name_b])))

    header_value_a = 'Zeppelin'
    header_value_b = 'Sabbath'

    headers_a = {header_name_a: header_value_a}
    response_a = ctx.perform_request(http.request.get(headers=headers_a))
    asserts.content(response_a, '2')

    headers_b = {header_name_a: header_value_a, header_name_b: header_value_b}
    response_b = ctx.perform_request(http.request.get(headers=headers_b))
    asserts.content(response_b, '1')


def test_randomize_empty_match(ctx):
    """
    BALANCER-815
    If no hash header found, the hash must become random.
    """
    ctx.start_balancer(HeadersHasherConfig(header_name='Led'))
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
    BALANCER-815
    If randomize_empty_match set to 0, then no randomization
    for not matching requests happens
    """
    ctx.start_balancer(HeadersHasherConfig(header_name='Led', randomize_empty_match=0))
    contents = set()
    request = http.request.get()
    for _ in xrange(10):
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        contents.add(response.data.content)
        if len(contents) > 1:
            break

    assert len(contents) == 1, 'there must be only one response due to the the constant hash'


def test_surround(ctx):
    """
    BALANCER-815
    If surround is off, partial matches are allowed
    """
    ctx.start_balancer(HeadersHasherConfig(header_name='lac', surround=1))
    headers_a = {'Black': 'Sabbath'}
    headers_b = {'Slack': 'Sabbath'}
    for headers in [headers_a, headers_b]:
        response = ctx.perform_request(http.request.get(headers=headers))
        asserts.content(response, '3')


def test_file_switch(ctx):
    """
    BALANCER-815
    BALANCER-1030
    If file_switch file exists then no hash is calculated, randomize_empty_match is no-op too
    """
    file_switch = ctx.manager.fs.create_file('file_witch')
    ctx.start_balancer(HeadersHasherByHashConfig(header_name='Led', file_switch=file_switch))
    time.sleep(2)
    contents = set()
    request = http.request.get(headers={'Led': 'Zeppelin'})

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
