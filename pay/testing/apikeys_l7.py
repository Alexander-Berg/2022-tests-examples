from paysys.sre.tools.monitorings.lib.checks.nanny import awacs
from paysys.sre.tools.monitorings.lib.util.helpers import create_subchecks


host = 'apikeys-l7.testing'
fqdn = 'apikeys-test-l7.paysys.yandex.net'
children = [
    'rtc_balancer_apikeys-test-l7_paysys_yandex_net_man',
    'rtc_balancer_apikeys-test-l7_paysys_yandex_net_vla',
    'rtc_balancer_apikeys-test-l7_paysys_yandex_net_sas'
]

https_ports = ['8668']
http_ports = ['8666', '18025']


def checks():
    return create_subchecks(
        'frontend',
        fqdn,
        awacs(
            host,
            fqdn,
            children,
            http_ports,
            https_ports,
            ping_uri='/slb_ping'
        )
    )
