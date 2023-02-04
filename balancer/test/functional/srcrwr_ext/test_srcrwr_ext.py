# -*- coding: utf-8 -*-

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from configs import SrcrwrExtConfig


DNS_SERVER_IP = '127.0.0.44'
CORRECT_HOST1 = "m-man-pbcznloiqpakow2g-{}-yp.tun.si.turbopages.org"
CORRECT_HOST2 = "MaN-pbcznloiQpakow2g-{}-yp.tun.Si.turbopages.org"
CORRECT_HOST3 = "sas-abcdefghijklmn2g-{}-yp.tun.si.turbopages.org"  # not resolved
CORRECT_HOST4 = "m-man-pbcznloiqpakow2g-{}-yp.tun.si.yandex-team.ru"
INVALID_HOST1 = "sas-abcdefghijklmn2g-yp.tun.si.turbopages.org"
INVALID_HOST2 = "sasikjfwiedfipoewjp"
INVALID_HOST3 = "sas-abcdefghijklmn2g-12345-yp.tun.si.turbopages"
INVALID_HOST4 = "-abcdefghijklmn2g-12345-yp.tun.si.turbopages.org"
INVALID_HOST5 = "m-man-pbcznloiqpakow2g-12345-yp.tun.turbopages.org"
ERROR_RESPONSE = 502


def start_all(ctx, default_backend_confg, srcrwr_ext_backend_config, **balancer_kwargs):
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(srcrwr_ext_backend_config, name='srcrwr_ext_backend')
    port = ctx.manager.port.get_port()
    ctx.manager.dnsfake.start(port=port, ip=DNS_SERVER_IP)
    ctx.start_balancer(SrcrwrExtConfig(remove_prefix='m', domains='yp-c.yandex.net', dns_ip=DNS_SERVER_IP, dns_port=port, **balancer_kwargs))


def test_srcrwr_ext_works(ctx):
    """
    Correct srcrwr_ext request correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    # Correct and resolved
    host1 = {'Host': CORRECT_HOST1.format(ctx.balancer.config.srcrwr_ext_backend_port)}
    response = ctx.perform_request(http.request.get(headers=host1))
    assert response.status == 200

    # Correct and resolved
    host2 = {'Host': CORRECT_HOST2.format(ctx.balancer.config.srcrwr_ext_backend_port)}
    response = ctx.perform_request(http.request.get(headers=host2))
    assert response.status == 200

    # Correct, but not resolved
    host3 = {'Host': CORRECT_HOST3.format(ctx.balancer.config.srcrwr_ext_backend_port)}
    ctx.perform_request_xfail(http.request.get(headers=host3))

    # Correct and resolved
    host4 = {'Host': CORRECT_HOST4.format(ctx.balancer.config.srcrwr_ext_backend_port)}
    response = ctx.perform_request(http.request.get(headers=host4))
    assert response.status == 200

    # Doesn't match regexp for srcrwr_ext
    invalid_host1 = {'Host': INVALID_HOST1}
    response = ctx.perform_request(http.request.get(headers=invalid_host1))
    assert response.status == ERROR_RESPONSE

    # Doesn't match regexp for srcrwr_ext
    invalid_host1 = {'Host': INVALID_HOST1}
    response = ctx.perform_request(http.request.get(headers=invalid_host1))
    assert response.status == ERROR_RESPONSE

    # Doesn't match regexp for srcrwr_ext
    invalid_host2 = {'Host': INVALID_HOST2}
    response = ctx.perform_request(http.request.get(headers=invalid_host2))
    assert response.status == ERROR_RESPONSE

    # Doesn't match regexp for srcrwr_ext
    invalid_host3 = {'Host': INVALID_HOST3}
    response = ctx.perform_request(http.request.get(headers=invalid_host3))
    assert response.status == ERROR_RESPONSE

    # Doesn't match regexp for srcrwr_ext
    invalid_host4 = {'Host': INVALID_HOST4}
    response = ctx.perform_request(http.request.get(headers=invalid_host4))
    assert response.status == ERROR_RESPONSE

    # Doesn't match regexp for srcrwr_ext
    invalid_host5 = {'Host': INVALID_HOST5}
    response = ctx.perform_request(http.request.get(headers=invalid_host5))
    assert response.status == ERROR_RESPONSE

    assert ctx.srcrwr_ext_backend.state.accepted.value == 3


def test_srcrwr_ext_several_headers_works(ctx):
    """
    Several headers parsed correctly
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    h = {'Host': CORRECT_HOST1.format(ctx.balancer.config.srcrwr_ext_backend_port), 'User-Agent': 'curl/7.58.0', 'Accept': '*/*'}
    response = ctx.perform_request(http.request.get(headers=h))
    assert response.status == 200
