# -*- coding: utf-8 -*-
import pytest
import time

from configs import RegexpHostConfig, RegexpHostSelfGeneratingConfig, RegexpNoDefaultConfig, RegexpOnlyDefaultConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


def send_requests(ctx, requests_data):
    for host, expected_content in requests_data:
        response = ctx.perform_request(http.request.get(path='/', headers={'Host': host}))
        asserts.content(response, expected_content)


def test_default_match(ctx):
    """
    BALANCER-673
    Default section in case of no-match
    """
    ctx.start_balancer(RegexpHostConfig())
    send_requests(ctx, ((h, 'default') for h in ['ac.dc', 'gov.no']))


def test_full_match(ctx):
    """
    BALANCER-673
    Full match without .* works
    """
    ctx.start_balancer(RegexpHostConfig())
    send_requests(ctx, [('yandex.ru', 'yandex.ru')])
    send_requests(ctx, ((h, 'default') for h in ['scorpions.ru', 'yandex-team.ru', 'team-yandex.ru', 'yandex.rust']))


def test_insensitive(ctx):
    """
    BALANCER-673
    with case_insensitive = true case does not matter
    """
    ctx.start_balancer(RegexpHostConfig(case_insensitive=True))
    send_requests(ctx, ((h, 'kub') for h in ['yandex.By', 'yandex.KZ', 'yandex.uA', 'Google.by']))


def test_star_match(ctx):
    """
    BALANCER-673
    Match like *.by works
    """
    ctx.start_balancer(RegexpHostConfig())
    send_requests(ctx, ((h, 'kub') for h in ['yandex.by', 'yandex.kz', 'yandex.ua', 'google.by']))


def test_no_overlap(ctx):
    r"""
    BALANCER-673
    Patterns like .*\.com and .*\.com\.tr are different
    """
    ctx.start_balancer(RegexpHostConfig())
    send_requests(ctx, ((h, 'com') for h in ['yandex.com', 'google.com']))
    send_requests(ctx, ((h, 'com.tr') for h in ['yandex.com.tr', 'google.com.tr']))


@pytest.mark.parametrize('params', [
    (2, 1, 'rock'),
    (1, 2, 'roll'),
], ids=[
    'rock-higher',
    'roll-higher',
])
def test_prio(ctx, params):
    """
    BALANCER-673
    In case of overlapping regexps the one with the higher prio
    wins. On non-overlapping match prios do not have effect
    """
    rock_prio, roll_prio, answer = params
    ctx.start_balancer(RegexpHostConfig(rock_prio=rock_prio, roll_prio=roll_prio))
    send_requests(ctx, ((h, answer) for h in ['rock.and.roll', 'rock-n-roll']))
    send_requests(ctx, ((h, 'rock') for h in ['rock', 'rocking']))
    send_requests(ctx, ((h, 'roll') for h in ['unroll', 'and.roll']))


def test_lots_of_regexps(ctx):
    """
    BALANCER-673
    using self-generating config to check that lots of regexps work
    """
    count = 2048

    width = 0
    tmp_count = count
    while tmp_count > 0:
        width += 1
        tmp_count /= 10

    host_format = 'yandex.%%0%dd' % width

    ctx.start_balancer(RegexpHostSelfGeneratingConfig(count), timeout=60)
    for i in xrange(count):
        host = host_format % i
        response = ctx.perform_request(http.request.get(path='/', headers={'Host': host}))
        asserts.content(response, '%d' % i)


def test_no_default(ctx):
    """
    BALANCER-673 BALANCER-788
    Request is erroneous if it did not match any section and there is no default section.
    """
    ctx.start_balancer(RegexpNoDefaultConfig())

    request = http.request.get(path='/', headers={'Host': 'yandex.ru'})
    response = ctx.perform_request(request)
    asserts.content(response, 'yandex.ru')

    request = http.request.get(path='/', headers={'Host': 'google.com'})
    ctx.perform_request_xfail(request)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['report-total-succ_summ'] == 1
    assert unistat['report-total-fail_summ'] == 1
    assert unistat['report-total-backend_fail_summ'] == 1
    assert unistat['report-total-client_fail_summ'] == 0
    assert unistat['report-total-other_fail_summ'] == 0


def test_only_default(ctx):
    """
    BALANCER-943
    Балансер должен нормально работать если указана только default-секция.
    """
    ctx.start_balancer(RegexpOnlyDefaultConfig())
    send_requests(ctx, ((h, 'default') for h in ['ac.dc', 'gov.no']))
