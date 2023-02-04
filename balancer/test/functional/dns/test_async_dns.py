# -*- coding: utf-8 -*-
import datetime
import pytest
import time

from configs import AsyncDnsConfig

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.balancer import asserts


TESTS_DNS_TIMEOUT = '5s'  # like default for c-ares
TESTS_RESOLVE_TIMEOUT = TESTS_DNS_TIMEOUT  # min(TESTS_DNS_TIMEOUT, resolve deadline) is used for DNS resolve
TESTS_CONNECT_TIMEOUT = '5s'
TESTS_BACKEND_TIMEOUT = '5s'
TESTS_CACHED_IP = '127.0.0.1'
TESTS_DNS_SRV_IP = '127.0.0.42'


def start_all(ctx, with_dns=True, **kwargs):
    if with_dns:
        ctx.manager.dnsfake.start(port=kwargs["dns_port"], ip=kwargs["dns_ip"])
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AsyncDnsConfig(**kwargs))


def start_all_with_dns_delay(ctx, dns_delay=0, **kwargs):
    ctx.manager.dnsfake.start(port=kwargs["dns_port"], delay_sec=dns_delay, ip=kwargs["dns_ip"])
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(AsyncDnsConfig(**kwargs))


def make_requests_check_result(ctx, count):
    for _ in range(count):
        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)


def test_async_dns_several_requests(ctx):
    """
    Several requests are successfully handled
    """
    port = ctx.manager.port.get_port()
    start_all(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port)

    make_requests_check_result(ctx, 5)


def test_async_dns_ttl(ctx):
    """
    DNS works after cached records expired
    """
    dns_ttl = 1.0
    port = ctx.manager.port.get_port()
    start_all(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port, dns_ttl='{}s'.format(dns_ttl),
              dns_timeout=TESTS_DNS_TIMEOUT, resolve_timeout=TESTS_RESOLVE_TIMEOUT)

    make_requests_check_result(ctx, 2)

    time.sleep(2 * dns_ttl)

    make_requests_check_result(ctx, 2)


def test_reset_dns_cache(ctx):
    """
    DNS works after reset cache command
    """
    port = ctx.manager.port.get_port()
    start_all(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port, dns_timeout=TESTS_DNS_TIMEOUT,
              resolve_timeout=TESTS_RESOLVE_TIMEOUT)

    for _ in range(5):
        reset_response = ctx.perform_request(http.request.get('/admin/events/call/reset_dns_cache'),
                                             port=ctx.balancer.config.admin_port)
        asserts.status(reset_response, 200)

        response = ctx.perform_request(http.request.get())
        asserts.status(response, 200)


def test_default_resolve_timeout(ctx):
    """
    Resolve deadline is less than 5 seconds by default. It should be 1 second according to
    https://st.yandex-team.ru/BALANCER-1296, though DNS timeout is unlimited by default.
    """
    long_timeout = '5s'
    dns_delay = 5  # DNS would sleep for this time in request handling

    port = ctx.manager.port.get_port()
    start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port, dns_delay=dns_delay,
                             connect_timeout=long_timeout, backend_timeout=long_timeout)

    start = datetime.datetime.now()
    ctx.perform_request_xfail(http.request.get())
    time_taken = datetime.datetime.now() - start

    assert time_taken < datetime.timedelta(seconds=dns_delay)


@pytest.mark.parametrize('dns', [False, True], ids=['resolve_timeout', 'dns_timeout'])
def test_resolve_or_dns_timeout_and_no_cached_ip(ctx, dns):
    """
    Resolve deadline is limited by instance/dns_timeout and proxy/resolve_timeout
    If no cached_ip is present, request fails
    """
    long_timeout = '10s'
    dns_delay = 5  # DNS would sleep for this time in request handling
    short_timeout = '100ms'
    if dns:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port,
                                 connect_timeout=long_timeout, backend_timeout=long_timeout,
                                 resolve_timeout=long_timeout, dns_timeout=short_timeout, dns_delay=dns_delay)
    else:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port,
                                 connect_timeout=long_timeout, backend_timeout=long_timeout,
                                 resolve_timeout=short_timeout, dns_timeout=long_timeout, dns_delay=dns_delay)

    start = datetime.datetime.now()
    ctx.perform_request_xfail(http.request.get())
    time_taken = datetime.datetime.now() - start

    assert time_taken < datetime.timedelta(seconds=dns_delay)


@pytest.mark.parametrize('dns', [False, True], ids=['resolve_timeout', 'dns_timeout'])
def test_resolve_or_dns_timeout_and_cached_ip(ctx, dns):
    """
    Resolve deadline is limited by instance/dns_timeout and proxy/resolve_timeout
    If cached_ip is present, it is used
    """
    long_timeout = '10s'
    dns_delay = 5  # DNS would sleep for this time in request handling
    short_timeout = '500ms'
    if dns:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port,
                                 connect_timeout=long_timeout, backend_timeout=long_timeout,
                                 resolve_timeout=long_timeout, dns_timeout=short_timeout,
                                 dns_delay=dns_delay, cached_ip=TESTS_CACHED_IP)
    else:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port,
                                 connect_timeout=long_timeout, backend_timeout=long_timeout,
                                 resolve_timeout=short_timeout, dns_timeout=long_timeout,
                                 dns_delay=dns_delay, cached_ip=TESTS_CACHED_IP)

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.get())
    time_taken = datetime.datetime.now() - start

    assert time_taken < datetime.timedelta(seconds=dns_delay)
    asserts.status(resp, 200)


@pytest.mark.parametrize('dns', [False, True], ids=['resolve_timeout', 'dns_timeout'])
def test_dns_timeout_is_not_affected_by_backend_or_connect_timeout(ctx, dns):
    """
    DNS resolve timeout depends on resolve_timeout/dns_timeout
    and not on connect_timeout/backend_timeout
    """
    connect_timeout = '4s'
    backend_timeout = '4s'
    dns_delay = 10
    dns_timeout = '{}s'.format(dns_delay * 2)
    huge_timeout = dns_delay * 10
    # dns_delay > (connect_timeout + backend_timeout)
    if dns:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port, connect_timeout=connect_timeout,
                                 backend_timeout=backend_timeout, dns_delay=dns_delay, dns_timeout=dns_timeout,
                                 resolve_timeout=huge_timeout)
    else:
        port = ctx.manager.port.get_port()
        start_all_with_dns_delay(ctx, dns_ip=TESTS_DNS_SRV_IP, dns_port=port, connect_timeout=connect_timeout,
                                 backend_timeout=backend_timeout, dns_delay=dns_delay, dns_timeout=huge_timeout,
                                 resolve_timeout=dns_timeout)

    start = datetime.datetime.now()
    resp = ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    time_taken = datetime.datetime.now() - start

    assert datetime.timedelta(seconds=dns_delay) < time_taken < datetime.timedelta(seconds=2 * dns_delay)
