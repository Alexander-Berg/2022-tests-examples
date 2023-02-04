import paysys.sre.tools.monitorings.configs.billing30.base.payout as payout
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children, merge
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

defaults = {'namespace': 'billing30.payout.testing'}
host = "billing30-test.payout-test"

children = empty_children
children_api = ['billing-payout@stage=billing-payout-test-stage;deploy_unit=api-gate']
children_tasks = ['billing-payout@stage=billing-payout-test-stage;deploy_unit=tasks']
children_gates = ['billing-payout@stage=billing-payout-test-stage;deploy_unit=gates']

l7_balancers = [
    {
        "namespace": "payout.test.billing.yandex.net",
        "datacenters": ["man", "sas", "vla"],
        "host": "payout.test.billing.yandex.net",
        "http_ports": [80],
        "https_ports": [443],
        "services": [
            "rtc_balancer_payout_test_billing_yandex_net_man",
            "rtc_balancer_payout_test_billing_yandex_net_sas",
            "rtc_balancer_payout_test_billing_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
]


def checks():
    return merge(
        payout.get_checks(
            children_api,
            children_tasks,
            children_gates,
            l7_balancers,
        ),
        postgres("mdbjllbb9cqvkdlku2gm", "payoutdb"),
    )
