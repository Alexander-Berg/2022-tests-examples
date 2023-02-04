# -*- coding: utf-8 -*-
import pytest
import time

from configs import RegexpPathScorpConfig, RegexpPathSelfGeneratingConfig, RegexpNoDefaultConfig, RegexpOnlyDefaultConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


def send_requests(ctx, requests_data):
    for path, expected_content in requests_data:
        response = ctx.perform_request(http.request.get(path=path))
        asserts.content(response, expected_content)


def test_default_match(ctx):
    """
    BALANCER-664
    default section in case of no-match
    """
    ctx.start_balancer(RegexpPathScorpConfig())
    send_requests(ctx, ((p, 'default') for p in ['/default', '/accept', 'metallica']))


def test_only_path(ctx):
    """
    BALANCER-664
    only path is counted, everything after question mark
    does not participate in pattern matching
    """
    ctx.start_balancer(RegexpPathScorpConfig(scorp_pattern='.*scorpions.*'))
    send_requests(ctx, ((p, 'default') for p in ['/?scorpions', '/?scorpions=hurricane', '/?wind=of-change&scorpions']))


@pytest.mark.parametrize('prios', [
    (1, 2),
    (2, 1),
], ids=[
    '1-2',
    '2-1',
])
def test_different_patterns(ctx, prios):
    """
    BALANCER-664
    with non-overlapping patterns there is no ambiguity
    in matching
    """
    ctx.start_balancer(RegexpPathScorpConfig(scorp_pattern='/hurricane.*', scorp_priority=prios[0],
                                             scorpions_pattern='/scorpions.*', scorpions_priority=prios[1]))
    send_requests(ctx, ((p, 'default') for p in ['/default', '/motorhead']))
    send_requests(ctx, ((p, 'scorp') for p in ['/hurricane', '/hurricane?rock=you']))
    send_requests(ctx, ((p, 'scorpions') for p in ['/scorpions', '/scorpions?still=loving-you']))


def test_short_path_high_prio(ctx):
    """
    BALANCER-664
    with overlapping patterns request goes to section
    with higher prio
    """
    ctx.start_balancer(RegexpPathScorpConfig(scorp_pattern='/scorp.*', scorp_priority=2,
                                             scorpions_pattern='/scorpions.*', scorpions_priority=1))
    send_requests(ctx, ((p, 'default') for p in ['/default', '/motorhead']))
    send_requests(ctx, ((p, 'scorp') for p in ['/scorp', '/scorp?rock=you']))
    send_requests(ctx, ((p, 'scorp') for p in ['/scorpions', '/scorpions?still=loving-you']))


def test_long_path_high_prio(ctx):
    """
    BALANCER-664
    with overlapping patterns request goes to section
    with higher prio
    """
    ctx.start_balancer(RegexpPathScorpConfig(scorp_pattern='/scorp.*', scorp_priority=1,
                                             scorpions_pattern='/scorpions.*', scorpions_priority=2))
    send_requests(ctx, ((p, 'default') for p in ['/default', '/motorhead']))
    send_requests(ctx, ((p, 'scorp') for p in ['/scorp', '/scorp?rock=you']))
    send_requests(ctx, ((p, 'scorpions') for p in ['/scorpions', '/scorpions?still=loving-you']))


def test_insensitive(ctx):
    """
    BALANCER-664
    with case_insensitive = true case does not matter
    """
    ctx.start_balancer(RegexpPathScorpConfig(scorp_pattern='/hurricane.*',
                                             scorpions_pattern='/scorpions.*', scorpions_insensitive=True))
    send_requests(ctx, ((p, 'scorpions') for p in ['/scorpions', '/SCORPIONS', '/sCoRPiOnS?sTiLl=LOVinG-you']))


def test_lots_of_regexps(ctx):
    """
    BALANCER-664
    using self-generating config to check that lots of regexps work
    """
    count = 2048

    width = 0
    tmp_count = count
    while tmp_count > 0:
        width += 1
        tmp_count /= 10

    path_format = '/%%0%dd' % width

    ctx.start_balancer(RegexpPathSelfGeneratingConfig(count), timeout=60)
    for i in xrange(count):
        path = path_format % i
        response = ctx.perform_request(http.request.get(path=path))
        asserts.content(response, '%d' % i)


def test_no_default(ctx):
    """
    BALANCER-664 BALANCER-788
    Request is erroneous if it did not match any section and there is no default section.
    """
    ctx.start_balancer(RegexpNoDefaultConfig())

    request = http.request.get(path='/')
    response = ctx.perform_request(request)
    asserts.content(response, 'root_section')

    request = http.request.get(path='/something')
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
