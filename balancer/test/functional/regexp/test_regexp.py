# -*- coding: utf-8 -*-
import os
import pytest
import time

from configs import RegexpUriConfig, RegexpAndConfig, RegexpHeaderConfig, RegexpIPConfig, RegexpMatchConfig, \
    RegexpNotConfig, RegexpOrConfig, RegexpPriorityConfig, RegexpUrlConfig, RegexpNoDefaultConfig, \
    RegexpCookieConfig, RegexpMethodConfig, RegexpMethodsConfig, RegexpLocalFileConfig, RegexpTestid, \
    RegexpDefaultWithMatcher, RegexpCgiParamConfig, RegexpCgiConfig, RegexpNormalizedPathConfig

from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http


MATCH_RESPONSE = 'match'
DEFAULT_RESPONSE = 'default'


def send_requests(ctx, requests_data):
    for path, expected_content in requests_data:
        response = ctx.perform_request(http.request.get(path=path))
        asserts.content(response, expected_content)


def base_uri_test(ctx, case_insensitive, paths, expected_content):
    ctx.start_balancer(RegexpUriConfig(case_insensitive=case_insensitive))

    send_requests(ctx, [(path, expected_content) for path in paths])


def base_match_test(ctx, request, expected_content, config=None):
    ctx.start_balancer(config or RegexpMatchConfig())

    response = ctx.perform_request(request)
    asserts.content(response, expected_content)


def base_test(ctx, config, request, expected_content):
    ctx.start_balancer(config)

    response = ctx.create_http_connection(host='127.0.0.1').perform_request(request)
    asserts.content(response, expected_content)


def base_ip_test(ctx, ip, expected_content):
    base_test(ctx, RegexpIPConfig(ip), http.request.get(), expected_content)


def base_uri_and_ip_test(ctx, ip, request, expected_content):
    base_test(ctx, RegexpAndConfig(ip), request, expected_content)


def base_uri_or_ip_test(ctx, ip, request, expected_content):
    base_test(ctx, RegexpOrConfig(ip), request, expected_content)


def base_bad_ip_test(ctx, ip):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(RegexpIPConfig(ip))


def check_request(ctx, request, expected_content):
    response = ctx.perform_request(request)
    asserts.content(response, expected_content)


def test_question_mark(ctx):
    """
    SEPE-4154
    ???????????????? ???????????? ?????????????????? ?????????????????????????? ?????????????? ?? path "/?"
    """
    base_uri_test(ctx, 'true', ['/?'], DEFAULT_RESPONSE)


def test_uri_case_insensitive(ctx):
    """
    ????????????, ?????????????????????????????? ?????????????????????? ?????????????????? ?????? ?????????? ????????????????,
    ???????????????????????? ?? ???????????? match
    """
    paths = ['/CAMELCASE', '/stairway']

    base_uri_test(ctx, 'true', paths, MATCH_RESPONSE)


def test_uri_case_insensitive_default(ctx):
    """
    ????????????, ???? ?????????????????????????????? ?????????????????????? ?????????????????? ?????? ?????????? ????????????????,
    ???????????????????????? ?? ???????????? default
    """
    paths = ['/lowCamelCase', '/stairway_to_heaven']

    base_uri_test(ctx, 'true', paths, DEFAULT_RESPONSE)


def test_uri_case_sensitive(ctx):
    """
    ????????????, ?????????????????????????????? ?????????????????????? ?????????????????? ?? ???????????? ????????????????,
    ???????????????????????? ?? ???????????? match
    """
    paths = ['/CamelCase', '/StAiRwAy']

    base_uri_test(ctx, 'false', paths, MATCH_RESPONSE)


def test_uri_case_sensitive_default(ctx):
    """
    ????????????, ???? ?????????????????????????????? ?????????????????????? ?????????????????? ?? ???????????? ????????????????,
    ???????????????????????? ?? ???????????? default
    """
    paths = ['/camelcase', '/Stairway']

    base_uri_test(ctx, 'false', paths, DEFAULT_RESPONSE)


def test_ip(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_ip_test(ctx, '127.0.0.1', MATCH_RESPONSE)


def test_ip_with_mask_ok(ctx):
    """
    SEPE-4614
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_ip_test(ctx, '126.0.0.0/7', MATCH_RESPONSE)


def test_ip_with_mask_fail(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_ip_test(ctx, '126.0.0.0/24', DEFAULT_RESPONSE)


def test_ip_with_range_ok(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_ip_test(ctx, '125.0.0.0-128.0.0.0', MATCH_RESPONSE)


def test_ip_with_range_fail(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_ip_test(ctx, '125.0.0.0-126.0.0.0', DEFAULT_RESPONSE)


def test_ip_default(ctx):
    """
    ???????? IP-?????????? ?????????????? ???? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_ip_test(ctx, '128.0.0.1', DEFAULT_RESPONSE)


def test_uri_and_ip(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ?? ???????????? ?????????????????????????? ?????????????????????? ??????????????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_uri_and_ip_test(ctx, '127.0.0.1', http.request.get(), MATCH_RESPONSE)


def test_uri_and_ip_bad_uri(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???? ?????????????????????????? ?????????????????????? ??????????????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_uri_and_ip_test(ctx, '127.0.0.1', http.request.get(path='/wiruwoeuiwoe'), DEFAULT_RESPONSE)


def test_uri_and_ip_bad_ip(ctx):
    """
    ???????? ???????????? ?????????????????????????? ?????????????????????? ??????????????????,
    ???? IP-?????????? ?????????????? ???? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_uri_and_ip_test(ctx, '128.0.0.1', http.request.get(), DEFAULT_RESPONSE)


def test_uri_and_ip_default(ctx):
    """
    ???????? ???????????? ???? ?????????????????????????? ?????????????????????? ??????????????????,
    ?? IP-?????????? ?????????????? ???? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_uri_and_ip_test(ctx, '128.0.0.1', http.request.get(path='/wiruwoeuiwoe'), DEFAULT_RESPONSE)


def test_priority(ctx):
    """
    ???????? ???????????? ?????????????????????????? ???????????????????????? ???????????????????? ???????????????????? ????????????????????,
    ???? ???? ?????????? ?????????????????? ?? ???????????? ?? ???????????????????? ??????????????????????
    """
    ctx.start_balancer(RegexpPriorityConfig(3, 2, 1))

    requests_data = [
        ('/very_long_uri', 'match1'),
        ('/very_short_uri', 'match2'),
        ('/just_uri', 'match3')
    ]

    send_requests(ctx, requests_data)


def test_priority_equal(ctx):
    """
    ???????? ???????????? ?????????????????????????? ???????????????????????? ???????????????????? ???????????????????? ????????????????????,
    ?? ?????????? ?????? ???????? ?????????????????? ???????????? ?? ???????????????????? ??????????????????????,
    ???? ???????????? ?????????? ?????????????????? ?? ???????? ???? ?????????? ????????????.
    """
    priority = 2.0
    priority_low = 1.0
    ctx.start_balancer(RegexpPriorityConfig(priority, priority, priority_low))

    response = ctx.perform_request(http.request.get(path='/very_long_uri'))

    assert response.data.content in ['match1', 'match2']


def test_uri_or_ip(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ?? ???????????? ?????????????????????????? ?????????????????????? ??????????????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_uri_or_ip_test(ctx, '127.0.0.1', http.request.get(), MATCH_RESPONSE)


def test_uri_or_ip_bad_uri(ctx):
    """
    ???????? IP-?????????? ?????????????? ?????????????????? ?? ?????????????????? ????????,
    ?? ???????????? ???? ?????????????????????????? ?????????????????????? ??????????????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_uri_or_ip_test(ctx, '127.0.0.1', http.request.get(path='/wiruwoeuiwoe'), MATCH_RESPONSE)


def test_uri_or_ip_bad_ip(ctx):
    """
    ???????? ???????????? ?????????????????????????? ?????????????????????? ??????????????????,
    ?? IP-?????????? ?????????????? ???? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? match
    """
    base_uri_or_ip_test(ctx, '128.0.0.1', http.request.get(), MATCH_RESPONSE)


def test_uri_or_ip_default(ctx):
    """
    ???????? ???????????? ???? ?????????????????????????? ?????????????????????? ??????????????????,
    ?? IP-?????????? ?????????????? ???? ?????????????????? ?? ?????????????????? ????????,
    ???? ???????????? ???????????????????????? ?? ???????????? default
    """
    base_uri_or_ip_test(ctx, '128.0.0.1', http.request.get(path='/wiruwoeuiwoe'), DEFAULT_RESPONSE)


def test_ipv4_broken_ip(ctx):
    """
    ???????? ?? ?????????????? ???????????? ???????????????????????? IPv4 ?????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '256.0.0.1')


def test_ipv4_negative_mask(ctx):
    """
    SEPE-7932
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '127.0.0.1/-1')


def test_ipv4_long_mask(ctx):
    """
    SEPE-7932
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '127.0.0.1/33')


def test_ipv4_broken_mask(ctx):
    """
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '127.0.0.1/abc')


def test_ipv6_multiple_double_colons(ctx):
    """
    ???????? ?? ?????????????? ???????????? ???????????????????????? IPv6 ?????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '3::2::1')


def test_ipv6_broken_ip(ctx):
    """
    ???????? ?? ?????????????? ???????????? ???????????????????????? IPv4 ?????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '10000::')


def test_ipv6_negative_mask(ctx):
    """
    SEPE-7932
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '::1/-1')


def test_ipv6_long_mask(ctx):
    """
    SEPE-7932
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '::1/129')


def test_ipv6_broken_mask(ctx):
    """
    ???????? ?? ?????????????? ???????????? IPv4 ?????????? ?? ???????????????????????? ???????????? ?? match_source_ip,
    ???? ???????????????? ???? ???????????? ??????????????????????
    """
    base_bad_ip_test(ctx, '::1/abc')


def test_header_regexp(ctx):
    """
    SEPE-7993
    ??????????????????, ?????? ???? ?????????????????? ?????????????????? ???????????????? ???????????? dispatch
    """
    ctx.start_balancer(RegexpHeaderConfig())

    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Led': 'Lamp'}), 'Stairway')


def test_cookie_regexp_no_surround(ctx):
    """
    BALANCER-1544
    ??????????????????, ?????? 1) ???????????? ???? ?????????????????? ????????, 2) ???????????? ???????? ???????????? ?? ???????????? rfc 6265
    BALANCER-1725
    ???????????? ???? ???????????? ???? RFC, ?????????????????? ???????????????????? ????????????????
    """
    ctx.start_balancer(RegexpCookieConfig(cookie='Foo=Bar', surround=False))

    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Foo=Bar'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'foo=Bar'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Foo=bar'}), DEFAULT_RESPONSE)

    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; Foo=Bar; Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; Foo=Bar, Bum=Baz'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla, Foo=Bar; Bum=Baz'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; Foo=Bar;Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla;Foo=Bar; Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla;Foo=Bar;Bum=Baz'}), MATCH_RESPONSE)


def test_cookie_regexp_surround(ctx):
    """
    BALANCER-1544
    ??????????????????, ?????? ???????????? ???? ?????????????????? ????????
    """
    ctx.start_balancer(RegexpCookieConfig(cookie='Foo=Bar', surround=True))

    check_request(ctx, http.request.raw_get(headers={'Cookie': 'aFoo=Bara'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'afoo=Bara'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'aFoo=bara'}), DEFAULT_RESPONSE)


def test_cookie_regexp_insensitive(ctx):
    """
    BALANCER-1544
    ??????????????????, ?????? ???????????? ???? ?????????????????? ????????
    """
    ctx.start_balancer(RegexpCookieConfig(cookie='Foo=Bar', case_insensitive=True))

    check_request(ctx, http.request.raw_get(headers={'Cookie': 'foo=bar'}), MATCH_RESPONSE)

    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; foo=bar; Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; foo=bar, Bum=Baz'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla, foo=bar; Bum=Baz'}), DEFAULT_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla; foo=bar;Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla;foo=bar; Bum=Baz'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Cookie': 'Bla=Bla;foo=bar;Bum=Baz'}), MATCH_RESPONSE)


def test_duplicate_header_regexp(ctx):
    """
    SEPE-8389
    ???????? ?????????????????? ?????????????????????? ?????????????????? ??????, ???? ???????? ?????? ?????????????? ???????????????? ?????????? ????????????
    ?? ?????????????????? ???????????? ?? ???????????? ?? ???????????????????? ??????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig())

    check_request(ctx, http.request.raw_get(headers=[('Led', 'Zeppelin'), ('Led', 'Lamp')]), MATCH_RESPONSE)


def test_default_header_regexp(ctx):
    """
    SEPE-7993
    ???????? ???? ???????? ???? ???????????????????? ?????????????? ???? ???????????????? ???? ?????????? ????????????????????, ???? ???????????? ???????????? ???????? ?? default
    """
    ctx.start_balancer(RegexpHeaderConfig())

    check_request(ctx, http.request.get(), DEFAULT_RESPONSE)


def test_different_header_regexp(ctx):
    """
    SEPE-7993
    ????????????, ?????????? ?? ?????????????? ???????????????????????? ?????????????????? ???????? ????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host'))

    check_request(ctx,
                  http.request.raw_get(path='/yandsearch?text=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&lr=213',
                                       headers={'Host': 'yandex.ru'}),
                  'Stairway')


def test_header_name(ctx):
    ctx.start_balancer(RegexpHeaderConfig(name='Host'))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'leD': 'Zeppelin'}), MATCH_RESPONSE)


def test_header_name_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? name_surround = true, ???? ???????????????? ???????????? ??????????????????, ?????? ?????????????????? ?????????? ??????????????????
    ???????????????? ???????????????????? ????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', name_surround=True))
    check_request(ctx, http.request.raw_get(headers={'X-Led-Y': 'Zeppelin'}), MATCH_RESPONSE)


def test_header_name_default_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ????-?????????????????? name_surround = false
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host'))
    check_request(ctx, http.request.raw_get(headers={'X-Led-Y': 'Zeppelin'}), DEFAULT_RESPONSE)


def test_header_name_not_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? name_surround = false, ???? ???????????????? ???????????? ??????????????????, ?????? ?????? ?????? ??????????????????
    ???????????????? ???????????????????? ????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', name_surround=False))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'X-Led-Y': 'Zeppelin'}), DEFAULT_RESPONSE)


def test_header_value_sensitive(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? case_insensitive = false, ???? ???????????????? ???????????? ?????????????????? ?????????????? ???????????????? ??????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', case_insensitive=False))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Led': 'zeppeliN'}), DEFAULT_RESPONSE)


def test_header_value_default_sensitive(ctx):
    """
    BALANCER-471
    BALANCER-617
    ????-?????????????????? case_insensitive = false
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host'))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Led': 'zeppeliN'}), DEFAULT_RESPONSE)


def test_header_value_insensitive(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? case_insensitive = true, ???? ???????????????? ???????????? ???????????????????????? ?????????????? ???????????????? ??????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', case_insensitive=True))
    check_request(ctx, http.request.raw_get(headers={'Led': 'zeppeliN'}), MATCH_RESPONSE)


def test_header_value_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? surround = true, ???? ???????????????? ???????????? ??????????????????, ?????? ?????????????????? ???????????????? ??????????????????
    ???????????????? ???????????????????? ????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', surround=True))
    check_request(ctx, http.request.raw_get(headers={'Led': 'A-Zeppelin-B'}), MATCH_RESPONSE)


def test_header_value_default_not_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ????-?????????????????? surround = true
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host'))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Led': 'A-Zeppelin-B'}), DEFAULT_RESPONSE)


def test_header_value_not_surround(ctx):
    """
    BALANCER-471
    BALANCER-617
    ???????? surround = false, ???? ???????????????? ???????????? ??????????????????, ?????? ?????? ???????????????? ??????????????????
    ???????????????? ???????????????????? ????????????????????
    """
    ctx.start_balancer(RegexpHeaderConfig(name='Host', surround=False))
    check_request(ctx, http.request.raw_get(headers={'Led': 'Zeppelin'}), MATCH_RESPONSE)
    check_request(ctx, http.request.raw_get(headers={'Led': 'A-Zeppelin-B'}), DEFAULT_RESPONSE)


def test_match_not(ctx):
    """
    BALANCER-121
    match_not - ?????????????????????? ?????????????????? ???????????????????????? matcher'??
    """
    ctx.start_balancer(RegexpNotConfig())

    cases = [('yandex.ru', 'yandex.ru'), ('yandex.by', 'some yandex'), ('bingo.com', 'not yandex')]

    for host, response in cases:
        request = http.request.raw_get(headers={'Host': host})
        check_request(ctx, request, response)


def test_match_post(ctx):
    """
    BALANCER-297 - match ???????????? ???? ?????????????? ??????????????; POST ???????????? ????????????????????
    match_fsm = { match = "POST.*"; };
    """
    base_match_test(ctx, http.request.post(data='0123456789'), MATCH_RESPONSE)


def test_not_match_get(ctx):
    """
    BALANCER-297 - match ???????????? ???? ?????????????? ??????????????; GET ???? ???????????? ????????????????????
    match_fsm = { match = "POST.*"; };
    """
    base_match_test(ctx, http.request.get(), DEFAULT_RESPONSE)


def test_url(ctx):
    """
    BALANCER-625 - url ???????????? path + cgi:
    /path?cgi=param
    """
    request = http.request.get(path='/delurl.xml?url=http://antisharlatan.net/')
    config = RegexpUrlConfig()
    base_match_test(ctx, request, 'match1', config=config)


def test_url_utf8(ctx):
    """
    MINOTAUR-1003 - url ???????????? path + cgi ?? utf8:
    /video/search?text=????????????%20??????%20????????%20????????????????%20????????????
    """
    request = http.request.get(path='/video/search?text=????????????%20??????%20????????%20????????????????%20????????????')
    config = RegexpUrlConfig()
    base_match_test(ctx, request, 'match2', config=config)


def test_no_default(ctx):
    """
    BALANCER-787
    ?????????????????? ???? ???????????????????? ?????????????? ?????? ???????????????????? ?? ???????????????????? default-????????????
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


def test_multiple_fsm_matchers(ctx):
    """
    BALANCER-2098
    Should not allow more than one matcher in a match_fsm section
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(RegexpUriConfig(second_matcher=True))


@pytest.mark.parametrize(
    'method',
    [
        'GET',
        'OPTIONS',
        'POST',
        'PUT',
        'PATCH',
        'DELETE',
        'CONNECT',
        'TRACE',
    ]
)
def test_methods(ctx, method):
    config = RegexpMethodConfig()
    base_match_test(ctx, http.request.custom(method), method.lower(), config=config)


@pytest.mark.parametrize(
    'method',
    [
        'HEAD',
    ]
)
def test_special_methods(ctx, method):
    config = RegexpMethodConfig()
    base_match_test(ctx, http.request.custom(method), '', config=config)


@pytest.mark.parametrize(
    'method',
    [
        'GET',
        'OPTIONS',
        'POST',
        'PUT',
        'PATCH',
        'DELETE',
        'CONNECT',
        'TRACE',
    ]
)
def test_several_methods(ctx, method):
    config = RegexpMethodsConfig()
    content = 'default'
    if method in ['GET', 'POST', 'PUT']:
        content = 'match'
    base_match_test(ctx, http.request.custom(method), content, config=config)


@pytest.mark.parametrize('workers', [4])
def test_local_file_matcher(ctx, workers):
    switch_file = ctx.manager.fs.create_file('switch_file')

    os.unlink(switch_file)

    ctx.start_balancer(RegexpLocalFileConfig(workers=workers, path=switch_file))

    response = ctx.perform_request(http.request.get())
    asserts.content(response, DEFAULT_RESPONSE)

    ctx.manager.fs.rewrite(switch_file, '')
    time.sleep(4)

    response = ctx.perform_request(http.request.get())
    asserts.content(response, MATCH_RESPONSE)


def test_regexp_testid_(ctx):
    ctx.start_balancer(RegexpTestid())

    response1 = ctx.perform_request(http.request.get(headers={'X-Yandex-ExpBoxes': '12,0,35;77257,0,43;224959,0,61;225413,0,27'}))
    response2 = ctx.perform_request(http.request.get(headers={'X-Yandex-ExpBoxes': '123,0,35;77257,0,43;224959,0,61;225413,0,27'}))

    assert response1.data.content in ['match1']
    assert response2.data.content in ['match2']


def test_default_with_matcher(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(RegexpDefaultWithMatcher())


def test_cgi_param(ctx):
    ctx.start_balancer(RegexpCgiParamConfig(case_insensitive=False))

    response1 = ctx.perform_request(http.request.get(path='/test?acdb=cabd'))
    response2 = ctx.perform_request(http.request.get(path='/test?ACDB=CABD'))
    response3 = ctx.perform_request(http.request.get(path='./test?ab=dc&ba=cd'))
    response4 = ctx.perform_request(http.request.get(path='/test?ba=dc&baa=cd'))

    assert response1.data.content == 'match'
    assert response2.data.content == 'default'
    assert response3.data.content == 'fallback'
    assert response4.data.content == 'default'


def test_cgi_param_case_insensitive(ctx):
    ctx.start_balancer(RegexpCgiParamConfig(case_insensitive=True))

    response1 = ctx.perform_request(http.request.get(path='/test?ACDB=CABD'))
    response2 = ctx.perform_request(http.request.get(path='./test?aB=dc&ba=cd'))

    assert response1.data.content == 'match'
    assert response2.data.content == 'fallback'


def test_cgi_match(ctx):
    """
    BALANCERSUPPORT-562
    """
    ctx.start_balancer(RegexpCgiConfig(cgi="[?](.*&)?uuid=[0-9a-f]{32}(&.*)?"))

    for p in [
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078',
        '/?&uuid=94005fbeddc7ff134f1b44b2fa173078',
        '/?x&uuid=94005fbeddc7ff134f1b44b2fa173078',
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078&',
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078&x',
    ]:
        resp = ctx.perform_request(http.request.get(path=p))
        asserts.content(resp, 'match')

    for p in [
        '/?uuid=94005fbeddc7ff134f1b44b2fa17307',
        '/?uuid=94005fbeddc7ff134f1b44b2fa1730788',
    ]:
        resp = ctx.perform_request(http.request.get(path=p))
        asserts.content(resp, 'default')


def test_cgi_not_match(ctx):
    """
    BALANCERSUPPORT-562
    """
    ctx.start_balancer(RegexpCgiConfig(cgi="(.*&)?uuid=[0-9a-f]{32}(&.*)?"))

    for p in [
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078',
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078&',
        '/?uuid=94005fbeddc7ff134f1b44b2fa173078&x',
    ]:
        resp = ctx.perform_request(http.request.get(path=p))
        asserts.content(resp, 'default')


def test_normalized_path(ctx):
    ctx.start_balancer(RegexpNormalizedPathConfig())

    resp = ctx.perform_request(http.request.get('http://yandex.ru/test/123', headers={'Host': 'yandex.ru'}))
    asserts.header_value(resp, 'x-url', 'http://yandex.ru/test/123')
    asserts.status(resp, 200)
    asserts.content(resp, 'match')

    resp = ctx.perform_request(http.request.get('/test/123', headers={'Host': 'yandex.ru'}))
    asserts.header_value(resp, 'x-url', '/test/123')
    asserts.status(resp, 200)
    asserts.content(resp, 'match')

    resp = ctx.perform_request(http.request.get('http://yandex.ru/test/456', headers={'Host': 'yandex.ru'}))
    asserts.header_value(resp, 'x-url', 'http://yandex.ru/test/456')
    asserts.status(resp, 404)
    asserts.content(resp, 'default')
