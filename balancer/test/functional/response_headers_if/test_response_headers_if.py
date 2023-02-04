# -*- coding: utf-8 -*-
"""
SEPE-3995
"""
import pytest

from configs import ResponseHeadersIfConfig, ResponseHeadersIfMatcherConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError


def test_no_matching_headers(ctx):
    """
    BALANCER-870
    If there are no if_has_header in response, do nothing
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header='Black',
        header_name_1='Deep',
        header_value_1='Purple',
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Black')
    asserts.no_header(resp, 'Deep')
    asserts.header_value(resp, 'Led', 'Zeppelin')


@pytest.mark.parametrize('if_has_header', [
    'Led', 'L.*'
])
def test_matching_header(ctx, if_has_header):
    """
    BALANCER-870
    If there are if_has_header in response, then add headers from create_header
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header=if_has_header,
        header_name_1='Deep',
        header_value_1='Purple',
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Black')
    asserts.header_value(resp, 'Deep', 'Purple')
    asserts.header_value(resp, 'Led', 'Zeppelin')


@pytest.mark.parametrize('if_has_header', [
    'Led', 'L.*'
])
@pytest.mark.parametrize('delete_header', [
    'Britney', 'BrItNeY', 'b.*y',
])
def test_matching_header_delete(ctx, if_has_header, delete_header):
    """
    BALANCER-870
    BALANCER-1285
    If there are if_has_header in response, then delete headers from delete_header.
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
        'Britney': 'Spears',
        'bonney': 'clyde',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header=if_has_header,
        delete_header=delete_header,
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Black')
    asserts.no_header(resp, 'Britney')
    if '.*' in delete_header:
        asserts.no_header(resp, 'bonney')
    else:
        asserts.header_value(resp, 'bonney', 'clyde')
    asserts.header_value(resp, 'Led', 'Zeppelin')


def test_matching_header_create_and_delete(ctx):
    """
    BALANCER-870
    BALANCER-1285
    If there are if_has_header in response, then delete headers from delete_header.
    create_header inserts headers too.
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
        'Britney': 'Spears',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header='led',
        delete_header='britney',
        header_name_1='Black',
        header_value_1='Sabbath',
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Britney')
    asserts.header_value(resp, 'Black', 'Sabbath')
    asserts.header_value(resp, 'Led', 'Zeppelin')


def test_matching_header_multicreate(ctx):
    """
    BALANCER-870
    If there are if_has_header in response, then add headers from create_header,
    even if there are more than one header
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header='Led',
        header_name_1='Deep',
        header_value_1='Purple',
        header_name_2='Iron',
        header_value_2='Maiden',
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Black')
    asserts.header_value(resp, 'Deep', 'Purple')
    asserts.header_value(resp, 'Iron', 'Maiden')
    asserts.header_value(resp, 'Led', 'Zeppelin')


def test_erase_if_has_header(ctx):
    """
    BALANCER-870
    headers matching if_has_header are erased from response if erase_if_has_header is set
    """
    response = http.response.ok(headers={
        'Led': 'Zeppelin',
    })
    ctx.start_backend(SimpleConfig(response=response))
    ctx.start_balancer(ResponseHeadersIfConfig(
        if_has_header='Led',
        header_name_1='Deep',
        header_value_1='Purple',
        erase_if_has_header=True,
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.no_header(resp, 'Black')
    asserts.header_value(resp, 'Deep', 'Purple')
    asserts.no_header(resp, 'Led')


@pytest.mark.parametrize('header_name', [
    '   ',
    '',
    '\n',
    'header:value',
], ids=[
    'spaces',
    'empty',
    'newline',
    'header:value',
])
def test_bad_create_name(ctx, header_name):
    """
    BALANCER-870
    create_header must not be invalid http header
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersIfConfig(
            if_has_header='Led',
            header_name_1=header_name,
            header_value_1='Purple',
        ))


def test_matcher_with_if_cond(ctx):
    """
    BALANCER-2258
    If both of if_has_header and matcher are defined, then balancer should not start
    """
    ctx.start_backend(SimpleConfig(http.response.ok()))
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersIfMatcherConfig(
            if_has_header='Led',
            code=200,
            header_name_1='Pink',
            header_value_1='Floyd',
            erase_if_has_header=True,
        ))


@pytest.mark.parametrize('code', [
    200,
    500,
])
def test_matcher_correctness(ctx, code):
    ctx.start_backend(SimpleConfig(http.response.ok()))
    ctx.start_balancer(ResponseHeadersIfMatcherConfig(
        code=code,
        header_name_1='Pink',
        header_value_1='Floyd',
        header_name_2='Rise',
        header_value_2='Against',
    ))
    resp = ctx.perform_request(http.request.get())
    if code == 200:
        asserts.header_value(resp, 'Pink', 'Floyd')
        asserts.header_value(resp, 'Rise', 'Against')
    else:
        asserts.no_header(resp, 'Pink')
        asserts.no_header(resp, 'Rise')


@pytest.mark.parametrize('header_name', [
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
], ids=[
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
])
def test_restricted_headers_modification_does_not_start(ctx, header_name):
    """
    BALANCER-3209: Изменение заголовков Content-Length и Transfer-Encoding запрещено
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersIfConfig(
            if_has_header='Led',
            header_name_1=header_name,
            header_value_1='Purple',
        ))


@pytest.mark.parametrize('header_name', [
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
], ids=[
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
])
def test_restricted_headers_deletion_does_not_start(ctx, header_name):
    """
    BALANCER-3209: Удаление заголовков Content-Length и Transfer-Encoding запрещено
    """
    ctx.start_backend(SimpleConfig())

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ResponseHeadersIfConfig(
            if_has_header='test',
            delete_header=header_name,
        ))
