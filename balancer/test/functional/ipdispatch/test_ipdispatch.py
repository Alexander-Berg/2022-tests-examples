# -*- coding: utf-8 -*-
import pytest
import netifaces

from configs import IPDispatchConfig, IpsPortsIPDispatchConfig, IPDispatchInsideHttpConfig, IPDispatchPortOnlyConfig, IPDispatchBindHttpConfig, DisabledConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError
from balancer.test.util.proto.http.stream import HTTPReaderException


def test_ipdispatch(ctx):
    """
    Общая функциональность модуля dispatch
    Запрос направляется в разные подмодули в зависимости от адреса и порта, к которому присоединился клиент
    """
    ctx.start_balancer(IPDispatchConfig())

    led_response = ctx.perform_request(http.request.get(), port=ctx.balancer.config.led_port)
    zeppelin_response = ctx.perform_request(http.request.get(), port=ctx.balancer.config.zeppelin_port)

    asserts.content(led_response, 'Led')
    asserts.content(zeppelin_response, 'Zeppelin')


@pytest.mark.parametrize(
    ['enable_ips', 'enable_ip', 'enable_ports', 'enable_port'],
    [
        (True, False, False, True),
        (True, True, False, True),
        (False, True, True, False),
        (False, True, True, True),
        (True, False, True, False),
        (True, True, True, True),
    ],
    ids=[
        'ips',
        'ips_ip',
        'ports',
        'ports_port',
        'ips_cross_ports',
        'ips_ip_cross_ports_port',
    ]
)
def test_ips_ports(ctx, enable_ips, enable_ip, enable_ports, enable_port):
    """
    BALANCER-541
    Проверка указания нескольких адресов и портов в конфиге.
    """
    ctx.start_balancer(IpsPortsIPDispatchConfig(enable_ips, enable_ip, enable_ports, enable_port))
    if enable_ips:
        ips = ['::1', '127.0.0.1']
    else:
        ips = ['::1']

    conf = ctx.balancer.config
    if enable_ports:
        ports = [conf.electric_port, conf.light_port, conf.orchestra_port]
    else:
        ports = [conf.electric_port]

    for ip in ips:
        for port in ports:
            response = ctx.create_http_connection(port=port, host=ip).perform_request(http.request.get())
            asserts.status(response, 200)
            asserts.content(response, 'OK')


def test_connection_reset_when_inside_http(ctx):
    """
    BALANCER-872
    Проверяем, что балансер закрывает соединение при нематчинге порта и ipdispatch внутри http.
    """
    ctx.start_balancer(IPDispatchInsideHttpConfig())

    led_response = ctx.perform_request(http.request.get(), port=ctx.balancer.config.led_port)
    asserts.content(led_response, 'Led')

    try:
        ctx.perform_request(http.request.get(), port=ctx.balancer.config.zeppelin_port)
        pytest.fail(msg='Zeppelin response succeeded somehow. The test configuration must be invalid.')
    except HTTPReaderException as e:
        assert 'connection timed out' not in e.message


def test_ipdispatch_port_only(ctx):
    """
    BALANCER-897
    Если в секции есть только port, но нет ip, то должны матчиться все запросы на этот порт,
    включая локальные
    """
    ctx.start_balancer(IPDispatchPortOnlyConfig())

    led_response = ctx.perform_request(http.request.get(), port=ctx.balancer.config.led_port)
    zeppelin_response = ctx.perform_request(http.request.get(), port=ctx.balancer.config.zeppelin_port)

    asserts.content(led_response, 'Led')
    asserts.content(zeppelin_response, 'Zeppelin')


def test_disabled_on(ctx):
    """
    BALANCER-1094
    Если адрес в addrs/admin_addrs в состоянии disabled, то даже плохой адрес
    не запрещает работу программы
    """
    ctx.start_balancer(DisabledConfig(disabled=1))
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'main')


def test_disabled_off_bad_ip(ctx):
    """
    BALANCER-1094
    Если адрес в addrs/admin_addrs то плохой адрес
    запрещает работу программы
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(DisabledConfig(disabled=0))


def test_bind_local_external_interfaces(ctx):
    """
    BALANCER-1703
    Проверяем, что localhost биндится на localhost и "*" биндится на интерфейсы отличные от 127.0.0.1 и ::1
    """
    ctx.start_balancer(IPDispatchBindHttpConfig())

    local_response = ctx.create_http_connection(
        port=ctx.balancer.config.port,
        host='localhost'
    ).perform_request(http.request.get())
    asserts.content(local_response, 'local')

    for iface in netifaces.interfaces():
        if iface.startswith("eth") or iface.startswith("en"):
            info = netifaces.ifaddresses(iface)
            ips = info.get(netifaces.AF_INET, []) + info.get(netifaces.AF_INET6, [])
            if ips:
                for addr in ips:
                    # skip link-local
                    if not addr['addr'].startswith("fe80"):
                        external_response = ctx.create_http_connection(
                            port=ctx.balancer.config.port,
                            host=addr['addr']
                        ).perform_request(http.request.get())
                        asserts.content(external_response, 'external')
