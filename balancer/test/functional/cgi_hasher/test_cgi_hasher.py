# -*- coding: utf-8 -*-
import pytest

from configs import CgiHasherConfig

from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts


@pytest.mark.parametrize('key', ['id', 'text'])
@pytest.mark.parametrize('mode', [None, 'concatenated', 'priority'])
def test_same_hash(ctx, key, mode):
    """
    BALANCER-1129
    cgi_hasher takes value of "parameters" cgi param and calcs hash
    from its value. Two similar request must have the same result
    of backend selection.
    """

    ctx.start_balancer(CgiHasherConfig(param1=key, mode=mode))

    # first - value of cgi parameter, second - expected response
    params = [('42', '7'), ('black_sabbath', '10')]
    requests = []
    for value, expected in params:
        requests.append((expected, http.request.get(path='/?{}={}'.format(key, value))))
        requests.append((expected, http.request.get(path='/?led=zeppelin&{}={}'.format(key, value))))
        requests.append((expected, http.request.get(path='/?{}={}&black=sabbath'.format(key, value))))
        requests.append((expected, http.request.get(path='/?led=zeppelin&{}={}&black=sabbath'.format(key, value))))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/


def test_combine_hash(ctx):
    """
    BALANCER-3068
    cgi_hasher combines the results of two different hashers
    """
    param_prev = 'A'
    param = 'B'
    ctx.start_balancer(CgiHasherConfig(
        param_prev=param_prev,
        param1=param,
        combine_hashes=True,
        randomize_empty_match=False,
        backends_count=10,
    ))

    response = ctx.perform_request(http.request.get(path='/?{}=Value'.format(param_prev)))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # should produce the same result as above
    response = ctx.perform_request(http.request.get(path='/?{}=Value'.format(param)))
    asserts.status(response, 200)
    asserts.content(response, '5')

    # combined, the hashers should produce a different hash and rebalance the request
    response = ctx.perform_request(http.request.get(path='/?{}=Value&{}=Value'.format(param_prev, param)))
    asserts.status(response, 200)
    asserts.content(response, '9')


def test_multiparams_concatenated(ctx):
    """
    BALANCER-1129
    If multiple parameters are specified, they all act in hash calculating
    """
    param1 = 'id'
    param2 = 'text'
    param3 = 'hash'

    ctx.start_balancer(CgiHasherConfig(param1=param1, param2=param2, param3=param3))

    requests = []
    for p in [param1, param2, param3]:
        # do not care what param name is, hash calculated from value only
        requests.append(('7', http.request.get(path='/?{}=42'.format(p))))
        requests.append(('6', http.request.get(path='/?{}=rainbow'.format(p))))
    requests.append(('3', http.request.get(path='/?{}=42&{}=rainbow'.format(param1, param2))))
    requests.append(('1', http.request.get(path='/?{}=42&{}=rainbow&{}=dio'.format(param1, param2, param3))))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/


def test_multiparams_priority(ctx):
    """
    BALANCER-1129
    If multiple parameters are specified, they are prioritized in order of specification
    """
    param1 = 'id'
    param2 = 'text'
    param3 = 'hash'

    ctx.start_balancer(CgiHasherConfig(param1=param1, param2=param2, param3=param3, mode='priority'))

    requests = []
    for p in [param1, param2, param3]:
        # do not care what param name is, hash calculated from value only
        requests.append(('8', http.request.get(path='/?{}=xxx'.format(p))))
        requests.append(('7', http.request.get(path='/?{}=42'.format(p))))
        requests.append(('6', http.request.get(path='/?{}=rainbow'.format(p))))
    requests.append(('7', http.request.get(path='/?{}=42&{}=rainbow'.format(param1, param2))))
    requests.append(('6', http.request.get(path='/?{}=42&{}=rainbow'.format(param2, param1))))
    requests.append(('7', http.request.get(path='/?{}=42&{}=rainbow&{}=xxx'.format(param1, param2, param3))))
    requests.append(('8', http.request.get(path='/?{}=42&{}=rainbow&{}=xxx'.format(param3, param2, param1))))
    requests.append(('6', http.request.get(path='/?{}=42&{}=rainbow&{}=xxx'.format(param2, param1, param3))))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/


@pytest.mark.parametrize('mode', [None, 'concatenated', 'priority'])
def test_case_sensitive_value(ctx, mode):
    """
    BALANCER-1129
    The values of cgi params are case sensitive
    """
    key = 'id'
    ctx.start_balancer(CgiHasherConfig(param1=key, mode=mode))

    requests = []
    requests.append(('1', http.request.get(path='/?{}=acdc'.format(key))))
    requests.append(('2', http.request.get(path='/?{}=ACDC'.format(key))))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/


@pytest.mark.parametrize('randomize', [None, True])
@pytest.mark.parametrize('mode', [None, 'concatenated', 'priority'])
def test_empty_match_randomize_on(ctx, randomize, mode):
    """
    BALANCER-1129
    By default the no-match request leads to randomized hash
    """
    key = 'id'
    ctx.start_balancer(CgiHasherConfig(param1=key, randomize_empty_match=randomize, mode=mode))

    requests = [
        http.request.get(path=path) for path in (
            '/',
            '/?other=does-not-count',
            '/?definetely-not-an-id=42',
        )
    ]

    for request in requests:
        results = set()

        for _ in range(20):
            response = ctx.perform_request(request)
            asserts.status(response, 200)
            results.add(response.data.content)
            if len(results) > 1:
                break

        assert len(results) > 1, 'requests with no match ({}) should be randomized'.format(request.request_line.path)


@pytest.mark.parametrize('mode', [None, 'concatenated', 'priority'])
def test_empty_match_randomize_off(ctx, mode):
    """
    BALANCER-1129
    If randomization of empty match is switched off, then the hash is fixed
    """
    key = 'id'
    ctx.start_balancer(CgiHasherConfig(param1=key, randomize_empty_match=False, mode=mode))

    requests = [
        http.request.get(path=path) for path in (
            '/',
            '/?other=does-not-count',
            '/?definetely-not-an-id=42',
        )
    ]

    for request in requests:
        results = set()

        for _ in range(20):
            response = ctx.perform_request(request)
            asserts.status(response, 200)
            results.add(response.data.content)
            if len(results) > 1:
                break

        assert len(results) == 1, 'requests with no match ({}) should not be randomized'.format(request.request_line.path)


@pytest.mark.parametrize('insensitive', [None, False])
@pytest.mark.parametrize('mode', [None, 'concatenated', 'priority'])
def test_case_insensitive_off(ctx, insensitive, mode):
    """
    BALANCER-1129
    By default the regexps of cgi params are case sensitive
    """
    ctx.start_balancer(CgiHasherConfig(param1='id', case_insensitive=insensitive, mode=mode))

    requests = []
    requests.append(('1', http.request.get(path='/?id=acdc&ID=ACDC')))
    requests.append(('2', http.request.get(path='/?id=ACDC&ID=acdc')))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/


def test_case_insensitive_on(ctx):
    ctx.start_balancer(CgiHasherConfig(param1='id', case_insensitive=True))

    requests = []
    requests.append(('3', http.request.get(path='/?id=acdc&ID=ACDC')))
    requests.append(('3', http.request.get(path='/?ID=acdc&ID=ACDC')))

    for expected, request in requests:
        response = ctx.perform_request(request)
        asserts.status(response, 200)
        asserts.content(response, expected)  # https://www.xkcd.com/221/
