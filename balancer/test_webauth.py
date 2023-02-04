# -*- coding: utf-8 -*-
import pytest

from configs import WebAuthConfig, WebAuthUncompletedConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


REDIR_URL = 'https://ya.ru/?token={csrf_token}&state={csrf_state}&app_id={app_id}'
SET_COOKIE = 'token={csrf_token}; domain=.yandex.ru; Max-Age=3600'
WEBAUTH_PARAMS = {
    'csrf_state': 'state',
    'csrf_token': 'token',
    'app_id': '1234'
}


def _start_webauth_balancer(ctx, cfg, auth_status, content='', **kwargs):
    ctx.start_backend(SimpleConfig(response=http.response.custom(
        status=auth_status,
        reason=str(auth_status),
        headers={
            'webauth-csrf-state': WEBAUTH_PARAMS['csrf_state'],
            'webauth-csrf-token': WEBAUTH_PARAMS['csrf_token'],
            'webauth-oauth-app-id': WEBAUTH_PARAMS['app_id'],
        }
    )))

    args = {
        'redir_url': REDIR_URL,
        'set_cookie': SET_COOKIE,
    }
    args.update(kwargs)
    ctx.start_balancer(cfg(**args))


@pytest.mark.parametrize(
    ['auth_status', 'content'],
    [
        (200, 'ok'),
        (403, 'on_forbidden'),
        (204, 'on_error'),
        (503, 'on_error'),
    ]
)
def test_auth_result(ctx, auth_status, content):
    _start_webauth_balancer(ctx, WebAuthConfig, auth_status)
    response = ctx.perform_request(http.request.get())
    asserts.content(response, content)


def test_redirect(ctx):
    _start_webauth_balancer(ctx, WebAuthConfig, 401)
    response = ctx.perform_request(http.request.get())
    asserts.header_value(response, 'location', REDIR_URL.format(**WEBAUTH_PARAMS))
    asserts.header_value(response, 'set-cookie', SET_COOKIE.format(**WEBAUTH_PARAMS))


def test_redirect_with_retpath(ctx):
    _start_webauth_balancer(ctx, WebAuthConfig, 401, redir_url="https://passport.{yandex_domain}/auth?retpath={retpath}")

    for yandex_domain in ['yandex.ru', 'yandex-team.ru']:
        response = ctx.perform_request(http.request.get('/test-url?haha=da', headers={'Host': yandex_domain}))
        asserts.header_value(response, 'location', 'https://passport.{yandex_domain}/auth?retpath=http://{yandex_domain}/test-url?haha=da'.format(yandex_domain=yandex_domain))

        response = ctx.perform_request(http.request.get('/test-url?haha=da', headers={'Host': 'test.' + yandex_domain}))
        asserts.header_value(response, 'location', 'https://passport.{yandex_domain}/auth?retpath=http://test.{yandex_domain}/test-url?haha=da'.format(yandex_domain=yandex_domain))

        response = ctx.perform_request(http.request.get('/test-url?haha=da', headers={'Host': 'test.testovich.' + yandex_domain}))
        asserts.header_value(response, 'location', 'https://passport.{yandex_domain}/auth?retpath=http://test.testovich.{yandex_domain}/test-url?haha=da'.format(yandex_domain=yandex_domain))


@pytest.mark.parametrize('auth_status', [401, 403, 204, 503])
def test_uncomplete(ctx, auth_status):
    _start_webauth_balancer(ctx, WebAuthUncompletedConfig, auth_status)
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_forbidden')


def test_checker_headers(ctx):
    _start_webauth_balancer(
        ctx, WebAuthConfig,
        auth_status=200,
        role="test_role",
        path="/test-path"
    )

    ctx.perform_request(http.request.get(headers={
        'X-User': 'admin',
        'X-Another-User': 'hacker',
        'Host': 'yandex.ru'
    }))

    request = ctx.backend.state.requests.get()
    assert request.request.request_line.path == '/test-path'
    assert request.request.headers.get_all('x-user') == ['admin']
    assert request.request.headers.get_all('x-another-user') == ['hacker']
    assert request.request.headers.get_all('webauth-idm-role') == ['test_role']
    assert request.request.headers.get_all('webauth-retpath') == ['http://yandex.ru/']


def test_options_passthrough(ctx):
    _start_webauth_balancer(ctx, WebAuthConfig, 403, allow_options_passthrough=True)
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_forbidden')

    response = ctx.perform_request(http.request.custom('OPTIONS'))
    asserts.content(response, 'ok')


def test_redirect_bypass_by_header(ctx):
    _start_webauth_balancer(ctx, WebAuthConfig, 401, header_name_redirect_bypass='X-No-Redir')

    response = ctx.perform_request(http.request.get())
    asserts.header_value(response, 'location', REDIR_URL.format(**WEBAUTH_PARAMS))
    asserts.header_value(response, 'set-cookie', SET_COOKIE.format(**WEBAUTH_PARAMS))

    response = ctx.perform_request(http.request.get(headers={'X-No-Redir': 'true'}))
    asserts.content(response, 'on_forbidden')
    asserts.no_header(response, 'location')
    asserts.no_header(response, 'set-cookie')
