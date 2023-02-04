# -*- coding: utf-8 -*-
import time

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from configs import ModBalancer2OnErrorConfig


VALID_HOST = 'pbcznloiqpakow2g.man.yp-c.yandex.net'
INVALID_HOST = 'invalid.local'
DNS_SERVER_IP = '127.0.0.43'


def test_async_dns_failed_on_error(ctx):
    """
    BALANCER-570
    Если балансер не может отрезолвить бэкенд, то запросы должны попадать в секцию on_error.
    """
    port = ctx.manager.port.get_port()
    ctx.manager.dnsfake.start(port=port, ip=DNS_SERVER_IP)
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(ModBalancer2OnErrorConfig(
        dns_async_resolve=True, backend_host=INVALID_HOST,
        dns_ip=DNS_SERVER_IP, dns_port=port))

    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_error')


def test_async_dns_revived(ctx):
    """
    BALANCER-570
    Если балансер не мог отрезолвить бэкенд, а потом dns поднялся,
    то балансер должен отрезолвить бэкенд и отправить в него запрос
    """
    dns_timeout = 1
    dns_ttl = 2
    port = ctx.manager.port.get_port()
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='ok')))
    ctx.start_balancer(ModBalancer2OnErrorConfig(
        dns_async_resolve=True, backend_host=VALID_HOST,
        dns_timeout=dns_timeout, dns_ttl=dns_ttl, attempts=1,
        dns_ip=DNS_SERVER_IP, dns_port=port))

    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_error')

    ctx.manager.dnsfake.start(port=port, ip=DNS_SERVER_IP)  # start DNS server
    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'on_error')  # failure for this host is still in cache

    time.sleep(dns_ttl * 2)  # sleep till cache is obsolete

    response = ctx.perform_request(http.request.get())
    asserts.content(response, 'ok')
