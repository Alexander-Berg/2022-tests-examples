# -*- coding: utf-8 -*-
import pytest
import time

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from configs import ProxyConfigAsyncDns


VALID_HOST = 'pbcznloiqpakow2g.man.yp-c.yandex.net'
INVALID_HOST = 'balancer.dns.test.invalid.ru'
DNS_SERVER_IP = '127.0.0.44'
BACKEND_IP = '127.0.0.1'
INVALID_BACKEND_IP = '127.0.0.99'


def start_all(ctx, ip=DNS_SERVER_IP, resolve_ipv4=None, resolve_ipv6=None, **kwargs):
    port = ctx.manager.port.get_port()
    ctx.manager.dnsfake.start(port=port, ip=ip, resolve_ipv4=resolve_ipv4, resolve_ipv6=resolve_ipv6)
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ProxyConfigAsyncDns(dns_ip=ip, dns_port=port, **kwargs))


def test_balancer_only_ipv4_resolve_only_ipv6_fail(ctx):
    """
    BALANCER-580
    Если либо выставлена опция use_only_ipv4 и DNS сервер зарезолвил только в IPv6,
    то балансер должен закрыть соединение с клиентом
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv6='::1', use_only_ipv4=True)
    ctx.perform_request_xfail(http.request.get())


def test_balancer_only_ipv6_resolve_only_ipv4_fail(ctx):
    """
    BALANCER-580
    либо выставлена опция use_only_ipv6 и DNS сервер зарезолвил только в IPv4,
    то балансер должен закрыть соединение с клиентом
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv4='127.0.0.1', use_only_ipv6=True)
    ctx.perform_request_xfail(http.request.get())


def test_balancer_only_ipv4_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, use_only_ipv4=True)
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_resolve_only_ipv4_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv4='127.0.0.1')
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_balancer_only_ipv4_resolve_only_ipv4_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv4='127.0.0.1', use_only_ipv4=True)
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_balancer_only_ipv6_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, use_only_ipv6=True)
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_resolve_only_ipv6_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv6='::1')
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_balancer_only_ipv6_resolve_only_ipv6_works(ctx):
    """
    """
    start_all(ctx, host=VALID_HOST, resolve_ipv6='::1', use_only_ipv6=True)
    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200


def test_need_resolve_works_as_expected(ctx):
    """
    BALANCER-517
    BALANCER-639
    Если need_resolve = false, то балансер не должен резолвить адрес бэкенда,
    а использовать указанный в cached_ip
    """
    # Requests would succeed if resolve is used
    start_all(ctx, host=VALID_HOST, need_resolve='false', cached_ip=INVALID_BACKEND_IP)
    for x in xrange(3):
        ctx.perform_request_xfail(http.request.get())


@pytest.mark.parametrize(
    ['host', 'need_resolve', 'cached_ip'],
    [
        (VALID_HOST, False, BACKEND_IP),
        (INVALID_HOST, False, BACKEND_IP),
        (INVALID_HOST, True, BACKEND_IP),
    ],
    ids=[
        'host_valid_cached_ip_valid',
        'cached_ip_valid',
        'need_resolve_cached_ip_valid'
    ]
)
def test_need_resolve_and_cached_ip_work_as_expected(ctx, host, need_resolve, cached_ip):
    start_all(ctx, host=host, need_resolve=need_resolve, cached_ip=cached_ip)
    for x in xrange(3):
        response = ctx.perform_request(http.request.get())
        assert response.status_line.status == 200


@pytest.mark.parametrize(
    ['host', 'need_resolve', 'cached_ip'],
    [
        (VALID_HOST, True, INVALID_BACKEND_IP),
    ],
    ids=[
        'host_valid_need_resolve',
    ]
)
def test_need_resolve_and_invalid_cached_ip_work_as_expected(ctx, host, need_resolve, cached_ip):
    start_all(ctx, host=host, need_resolve=need_resolve, cached_ip=cached_ip)
    ctx.perform_request_xfail(http.request.get())
    time.sleep(1)  # wait till resolved
    for x in xrange(3):
        response = ctx.perform_request(http.request.get())
        assert response.status_line.status == 200


def test_invalid_host_no_cached_ip(ctx):
    start_all(ctx, host=INVALID_HOST, need_resolve='true', cached_ip=INVALID_BACKEND_IP)
    for x in xrange(3):
        ctx.perform_request_xfail(http.request.get())


def test_dns_revived_no_cached_ip(ctx):
    """
    BALANCER-638
    В конфиге не указан cached_ip
    Если dns упал а затем поднялся, то балансер после успешного резолва адреса бэкенда
    не должен больше дергать getaddrinfo
    Балансер не должен дергать getaddrinfo с пустым host (для cached_ip)
    """
    dns_timeout = 1
    dns_ttl = 2
    port = ctx.manager.port.get_port()
    ctx.manager.dnsfake.start(port=port, ip=DNS_SERVER_IP)  # start DNS server
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='ok')))
    ctx.start_balancer(ProxyConfigAsyncDns(host=VALID_HOST, dns_timeout=dns_timeout,
                                           dns_ttl=dns_ttl, dns_ip=DNS_SERVER_IP, dns_port=port))

    ctx.manager.dnsfake.stop()
    ctx.perform_request_xfail(http.request.get())

    ctx.manager.dnsfake.start(port=port, ip=DNS_SERVER_IP)  # start DNS server again

    ctx.perform_request_xfail(http.request.get())  # error still in cache
    time.sleep(dns_ttl * 2)  # sleep till cache is obsolete

    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200

    ctx.manager.dnsfake.stop()  # stop resolving

    response = ctx.perform_request(http.request.get())
    assert response.status_line.status == 200  # cached value is still actual, though DNS server down
