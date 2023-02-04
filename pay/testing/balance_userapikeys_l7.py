from paysys.sre.tools.monitorings.configs.apikeys.base import balance_userapikeys_l7

defaults = {'namespace': 'userapikeys'}

host = 'billing-userapikeys-l7.testing'
children = ['billing-userapikeys@stage=billing-userapikeys-test-stage']
hostname = 'balance-userapikeys-test-l7.paysys.yandex.net'
l7_balancers = [
    {
        'namespace': hostname,
        'datacenters': ['man', 'sas', 'vla'],
        'host': hostname,
        'http_ports': [80],
        'https_ports': [443],
        'services': [
            'rtc_balancer_balance-userapikeys-test-l7_paysys_yandex_net_man',
            'rtc_balancer_balance-userapikeys-test-l7_paysys_yandex_net_testing_sas',
            'rtc_balancer_balance-userapikeys-test-l7_paysys_yandex_net_testing_vla',
        ],
        'checks': {
            'cpu_usage': {'warn': 60, 'crit': 80},
            'cpu_wait': {'warn': 0.5, 'crit': 1},
            'codes_5xx': {'warn': 0.5, 'crit': 1},
            'attempts_backend_errors': {'warn': 1, 'crit': 2},
        },
    },
]


def checks():
    return balance_userapikeys_l7.get_checks(
        children,
        l7_balancers
    )
