# -*- coding: utf-8 -*-

import pytest
import re
from configs import LogHeadersConfig

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.balancer import asserts


SESSION_ID_NAME = 'Session_id'
SESSION_ID = SESSION_ID_NAME + '=1:1111111111.2.2.3333333333333:QQQQQQQQQQqqqqqqqqqqqq:44.4|444444444.4.4.4:4|5:555555.555555.jkijiJJJJJJJJjjjj5555-fFf5f'
SESSION_ID_MASKED = SESSION_ID_NAME + '=1:1111111111.2.2.3333333333333:QQQQQQQQQQqqqqqqqqqqqq:44.4|444444444.4.4.4:4|5:555555.555555.XXXXXXXXXXXXXXXXXXXXXXXXXXX'

SESSIONID2_NAME = 'sessionid2'
SESSIONID2 = SESSIONID2_NAME + '=2:2222222222.2.2.2222222222222:PPPPPPPPPPPPPPpppppppp:99.9|999999999.9.9.9:9|9:999999.777777.jiorJojirjIJIJJJJJJJJ-EEE5s'
SESSIONID2_MASKED = SESSIONID2_NAME + '=2:2222222222.2.2.2222222222222:PPPPPPPPPPPPPPpppppppp:99.9|999999999.9.9.9:9|9:999999.777777.XXXXXXXXXXXXXXXXXXXXXXXXXXX'

SESSGUARD_NAME = 'sessguard'
SESSGUARD_ID = SESSGUARD_NAME + '=1:1111111111.2.2.3333333333333:QQQQQQQQQQqqqqqqqqqqqq:44.4|444444444.4.4.4:4|5:555555.555555.thisissessguard'
SESSGUARD_ID_MASKED = SESSGUARD_ID = SESSGUARD_NAME + '=1:1111111111.2.2.3333333333333:QQQQQQQQQQqqqqqqqqqqqq:44.4|444444444.4.4.4:4|5:555555.555555.XXXXXXXXXXXXXXX'

WEBAUTH_NAME = 'webauth_oauth_token'
WEBAUTH_ID = WEBAUTH_NAME + '=AQAD-qLALALAAAAgpjVKk9pM777diIqq1DCjR9s'
WEBAUTH_ID_MASKED = WEBAUTH_NAME + '=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'


def read_accesslog(ctx):
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert accesslog, 'accesslog is empty'
            return accesslog


@pytest.mark.parametrize('name_re', ['reqid', 'REQID'])
def test_reqid_logging(ctx, name_re):
    """
    BALANCER-1119
    accesslog has log_headers record with reqid,
    name_re is case insensitive.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re=name_re))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert re.search('log_headers <::reqid:.+::>', accesslog, re.I) is not None


def test_multiple_matches(ctx):
    """
    BALANCER-1119
    The user's headers are logged together with balancer ones.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='.*reqid'))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert re.search('log_headers.* <::reqid:.+::>', accesslog, re.I) is not None
    assert '<::x-reqid:scorpions::>' in accesslog


def test_no_match(ctx):
    """
    BALANCER-1119
    If there is no matches, then nothing is logged
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='kekid'))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert 'log_headers' in accesslog
    assert 'log_headers <::' not in accesslog


@pytest.mark.parametrize('name_re', [None, '?', 'abc[def'], ids=['nil', 'question', 'unclosed'])
def test_bad_name_re(ctx, name_re):
    """
    BALANCER-1119
    Balancer won't start if name_re has invalid regexp or no regexp at all
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(LogHeadersConfig(name_re=name_re))


@pytest.mark.parametrize(
    ["cookie_fields", "log_cookie_meta"],
    [
        ('lol', True),
        ('lol', False),
        (None, True)
    ]
)
def test_match_one_cookie(ctx, cookie_fields, log_cookie_meta):
    """
    Match only one cookie
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields=cookie_fields, log_cookie_meta=log_cookie_meta))
    response = ctx.perform_request(http.request.get(headers={
        'Cookie': 'lol=123',
        'Host': 'foo.bar',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    if cookie_fields:
        assert '<::Cookie:lol=123::>' in accesslog
    if log_cookie_meta:
        assert '<::CookieMeta:7:lol=;::>' in accesslog


@pytest.mark.parametrize(
    ["cookie_fields", "log_cookie_meta"],
    [
        ('lol', True),
        ('lol', False),
        (None, True)
    ]
)
def test_match_one_cookie_any_place(ctx, cookie_fields, log_cookie_meta):
    """
    Match only one cookie at any position
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields=cookie_fields, log_cookie_meta=log_cookie_meta))
    response = ctx.perform_request(http.request.get(headers={
        'Cookie': 'kek=321;lol=123;acdc=432',
        'Host': 'foo.bar',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    if cookie_fields:
        assert '<::Cookie:lol=123::>' in accesslog
    if log_cookie_meta:
        assert '<::CookieMeta:24:kek=;lol=;acdc=;::>' in accesslog


@pytest.mark.parametrize(
    ["cookie_fields", "log_cookie_meta"],
    [
        ('lol', True),
        ('lol', False),
        (None, True)
    ]
)
def test_match_one_cookie_any_place_split(ctx, cookie_fields, log_cookie_meta):
    """
    Match only one cookie at any position
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields=cookie_fields, log_cookie_meta=log_cookie_meta))
    response = ctx.perform_request(http.request.get(headers={
        'Cookie': ['kek=321', 'lol=123', 'acdc=432'],
        'Host': 'foo.bar',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    if cookie_fields:
        assert '<::Cookie:lol=123::>' in accesslog
    if log_cookie_meta:
        assert '<::CookieMeta:7:kek=;::>' in accesslog
        assert '<::CookieMeta:7:lol=;::>' in accesslog
        assert '<::CookieMeta:8:acdc=;::>' in accesslog


def test_match_many_cookies(ctx):
    """
    MINOTAUR-1217
    Match more then one header
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields='lol,kek'))
    response = ctx.perform_request(http.request.get(headers={
        'Cookie': 'lol=123; kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:lol=123; kek=321::>' in accesslog


def test_match_many_cookies_any_place(ctx):
    """
    MINOTAUR-1217
    Match more then one header at any position
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields='lol,kek'))
    response = ctx.perform_request(http.request.get(headers={
        'Cookie': 'ozzy=643; lol=123; acdc=432;kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:lol=123; kek=321::>' in accesslog


def test_matches_both_re_and_cookies(ctx):
    """
    MINOTAUR-1217
    Test combined logging options
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='.*reqid', cookie_fields='lol,kek'))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'ozzy=643; lol=123; acdc=432;kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert re.search('log_headers.* <::reqid:.+::>', accesslog, re.I) is not None
    assert '<::x-reqid:scorpions::>' in accesslog
    assert '<::Cookie:lol=123; kek=321::>' in accesslog


def test_matches_cookie_fields_and_session_sign_is_masked(ctx):
    """
    BALANCER-3128
    Mask session signatures
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(cookie_fields='kek,yandexuid,' + SESSION_ID_NAME + ',' + SESSIONID2_NAME))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'yandexuid=1233654789655541044; ' + SESSION_ID + '; acdc=432; ' + SESSIONID2 + '; kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:yandexuid=1233654789655541044; ' + SESSION_ID_MASKED + '; ' + SESSIONID2_MASKED + '; kek=321::>' in accesslog


def test_matches_re_cookie_and_session_sign_is_masked(ctx):
    """
    BALANCER-3128
    Mask session signatures
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='Cookie'))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'yandexuid=1233654789655541044; ' + SESSION_ID + '; acdc=432; ' + SESSIONID2 + '; kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:yandexuid=1233654789655541044; ' + SESSION_ID_MASKED + '; acdc=432; ' + SESSIONID2_MASKED + '; kek=321::>' in accesslog


def test_matches_both_re_and_cookie_fields_and_session_sign_is_masked(ctx):
    """
    BALANCER-3128
    Mask session signatures
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='.*reqid', cookie_fields='kek,yandexuid,' + SESSION_ID_NAME + ',' + SESSIONID2_NAME))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'yandexuid=1233654789655541044; ' + SESSION_ID + '; acdc=432; ' + SESSIONID2 + '; kek=321',
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert re.search('log_headers.* <::reqid:.+::>', accesslog, re.I) is not None
    assert '<::x-reqid:scorpions::>' in accesslog
    assert '<::Cookie:yandexuid=1233654789655541044; ' + SESSION_ID_MASKED + '; ' + SESSIONID2_MASKED + '; kek=321::>' in accesslog


def test_not_key_value_by_re_handled(ctx):
    """
    BALANCER-3405
    Non key-value cookie field
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='Cookie'))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'yandexuid=1233654789655541044; ' + SESSION_ID + '; acdc=432; ' + SESSIONID2 + '; notkeyvaluecookie; '
                  + SESSGUARD_ID + '; ' + WEBAUTH_ID,
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:yandexuid=1233654789655541044; ' + SESSION_ID_MASKED + '; acdc=432; ' + SESSIONID2_MASKED + '; ' \
           + SESSGUARD_ID_MASKED + '; ' + WEBAUTH_ID_MASKED + '::>' in accesslog


def test_not_key_value_by_cookie_fields_handled(ctx):
    """
    BALANCER-3405
    Non key-value cookie field
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(name_re='.*reqid', cookie_fields='kek,yandexuid,' + SESSION_ID_NAME + ','
                                                                         + SESSGUARD_NAME + ',' + SESSIONID2_NAME + ','
                                                                         + WEBAUTH_NAME))
    response = ctx.perform_request(http.request.get(headers={
        'X-Reqid': 'scorpions',
        'Cookie': 'yandexuid=1233654789655541044; ' + SESSION_ID + '; acdc=432; ' + SESSIONID2 + '; notkeyvaluecookie; '
                  + '; ' + WEBAUTH_ID + '; ' + SESSGUARD_ID,
    }))
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert re.search('log_headers.* <::reqid:.+::>', accesslog, re.I) is not None
    assert '<::x-reqid:scorpions::>' in accesslog
    assert '<::Cookie:yandexuid=1233654789655541044; ' + SESSION_ID_MASKED + '; ' + SESSIONID2_MASKED + '; ' \
           + WEBAUTH_ID_MASKED + '; ' + SESSGUARD_ID_MASKED + '::>' in accesslog


def test_md5body(ctx):
    """
    BALANCER-2470
    Balancer has to calculate md5 of body if asked
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data="ok")))
    ctx.start_balancer(LogHeadersConfig(log_body_md5=True))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::body_md5:444bcb3a3fcf8389296c49467f27e1d6::>' in accesslog


def test_log_reponse_headers(ctx):
    """
    Balancer has to log response headers if asked
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'aaa': 'bbb',
    }, data="ok")))
    ctx.start_balancer(LogHeadersConfig(response_name_re='.*'))
    response = ctx.perform_request(http.request.get())

    asserts.status(response, 200)
    asserts.content(response, 'ok')
    accesslog = read_accesslog(ctx)
    assert '<::aaa:bbb::>' in accesslog


def test_log_setcookie_meta(ctx):
    """
    Logging set-cookie metadata
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': ['a=b; HttpOnly; Secure; Max-Age=123; xxx=yyy', 'c', 'ccc=ddd'],
    }, data="ok")))
    ctx.start_balancer(LogHeadersConfig(log_set_cookie_meta=True, cookie_fields="ccc"))
    response = ctx.perform_request(http.request.get(headers={
        'host': 'foo.bar',
        'cookie': 'ccc=eee',
    }))

    asserts.status(response, 200)
    asserts.content(response, 'ok')
    asserts.header_values(response, 'set-cookie', ['a=b; HttpOnly; Secure; Max-Age=123; xxx=yyy', 'c', 'ccc=ddd'])
    accesslog = read_accesslog(ctx)
    assert '<::SetCookieMeta:a=1:a; HttpOnly; Secure; Max-Age=123; xxx=yyy::>' in accesslog
    assert '<::Cookie:ccc=eee::>' in accesslog
    assert '<::SetCookie:ccc=ddd::>' in accesslog
    assert '<::SetCookieMeta:c::>' in accesslog


def test_log_setcookie(ctx):
    """
    Logging set-cookie metadata
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': ['c=d', 'ccc=ddd; xxx=yyy'],
    }, data="ok")))
    ctx.start_balancer(LogHeadersConfig(log_set_cookie=True, cookie_fields="ccc"))
    response = ctx.perform_request(http.request.get(headers={
        'host': 'foo.bar',
        'cookie': 'ccc=eee',
    }))

    asserts.status(response, 200)
    asserts.content(response, 'ok')
    asserts.header_values(response, 'set-cookie', ['c=d', 'ccc=ddd; xxx=yyy'])
    accesslog = read_accesslog(ctx)
    assert '<::Cookie:ccc=eee::>' in accesslog
    assert '<::SetCookie:ccc=ddd; xxx=yyy::>' in accesslog
    assert '<::SetCookieMeta:c=d::>' not in accesslog
