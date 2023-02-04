# coding=utf-8
from paysys.sre.tools.monitorings.configs.configshop.base import base
from paysys.sre.tools.monitorings.lib.checks.active.http import https
from paysys.sre.tools.monitorings.lib.notifications import Notifications
from paysys.sre.tools.monitorings.lib.util.helpers import (
    merge, check, gen_children_deploy
)


notifications = (
    Notifications()
        .set_telegram(group=["billing_configshop_notifications"], delay=30)
        .telegram
)

defaults = {'namespace': 'configshop'}

host = "billing.configshop-test"
project_id = "configshop"

children = {}
for du in ['api', 'infratasks', 'tasks']:
    children[du] = 'configshop@stage=configshop-test-stage;deploy_unit={du}'.format(du=du)

l7_balancers = [
    {
        "namespace": "configshop.test.billing.yandex.net",
        "datacenters": ["man", "sas", "vla"],
        "host": "configshop.test.billing.yandex.net",
        "http_ports": [80],
        "https_ports": [443],
        "services": [
            "rtc_balancer_configshop_test_billing_yandex_net_man",
            "rtc_balancer_configshop_test_billing_yandex_net_sas",
            "rtc_balancer_configshop_test_billing_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 10, "crit": 50, "flaps": {"stable": 300, "critical": 600}},
            "attempts_backend_errors": {"warn": 20, "crit": 100, "flaps": {"stable": 300, "critical": 600}},
        },
    },
]


def checks():
    base_checks = base.get_checks(
        children,
        l7_balancers,
    )
    base_checks = merge(
        base_checks,
        https(
            'configshop-https-check', 443, ok_codes=[200], crit=5,
            headers={"Host": "configshop.test.billing.yandex.net"}
        ),
        check('configshop-https-check', gen_children_deploy(children['api'], 'api')),
    )

    return base_checks
