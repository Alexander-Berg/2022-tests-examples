import paysys.sre.tools.monitorings.configs.oplata.base.oplata as oplata
from paysys.sre.tools.monitorings.lib.notifications import Notifications

notifications = Notifications()
notifications.set_iron_woman(logins=[
    '@svc_billing_swat_team:main',
    '@svc_billing_swat_team:reserve',
], delay=300)

defaults = {'namespace': 'oplata.testing'}
host = "oplata.testing"

children = {}
for du in ['admin', 'payments', 'sdk', 'workers-fast-moderation', 'workers-moderation', 'workers-payments']:
    children[du] = 'oplata@stage=oplata-test-stage;deploy_unit=%s' % du

l7_balancer = [
    {
        "namespace": "oplata.test.billing.yandex.net",
        "datacenters": ["man", "sas", "vla"],
        "host": "oplata.test.billing.yandex.net",
        "http_ports": [80],
        "https_ports": [443],
        "services": [
            "rtc_balancer_oplata_test_billing_yandex_net_man",
            "rtc_balancer_oplata_test_billing_yandex_net_sas",
            "rtc_balancer_oplata_test_billing_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 10, "crit": 50}
        },
    }
]


def checks():
    return oplata.get_checks(children, l7_balancer, 'test')
