import paysys.sre.tools.monitorings.configs.billing30.base.accounts as accounts
from paysys.sre.tools.monitorings.lib.notifications import Notifications
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.util.solomon import solomon_absence_expression_monitoring

notifications = (
    Notifications()
        .set_telegram(group=["hot_billing_accounter_notifications"], delay=30)
)

defaults = {'namespace': 'newbilling-accounter'}

host = "billing30.accounts-test"
project_id = "newbilling-accounter"

children = empty_children
children_api = ['billing-accounts@stage=billing-accounts-test-stage;deploy_unit=api']
children_tasks = ['billing-accounts@stage=billing-accounts-test-stage;deploy_unit=tasks']
children_lbexporter = ['billing-accounts@stage=billing-accounts-test-stage;deploy_unit=lbexporter']
children_sequencer = ['billing-accounts@stage=billing-accounts-test-stage;deploy_unit=sequencer']

l7_balancers = [
    {
        "namespace": "accounts.test.billing.yandex.net",
        "datacenters": ["man", "sas", "vla"],
        "host": "accounts.test.billing.yandex.net",
        "http_ports": [80],
        "https_ports": [443],
        "services": [
            "rtc_balancer_accounts_test_billing_yandex_net_man",
            "rtc_balancer_accounts_test_billing_yandex_net_sas",
            "rtc_balancer_accounts_test_billing_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 5, "crit": 10},
            "attempts_backend_errors": {"warn": 10, "crit": 20},
        },
    },
]


def checks():
    base_checks = merge(
        accounts.get_checks(
            children_api,
            children_tasks,
            children_sequencer,
            children_lbexporter,
            l7_balancers,
        ),
        _api_2xx(),
    )
    solomon_checks = merge(accounts.lbexporter_db_deadlock(project_id, 'test', 600, 3600),)

    for key, val in solomon_checks.items():
        val["aggregator_kwargs"] = merge(val.get("aggregator_kwargs", {}), {"nodata_mode": "force_crit"})

    return merge(base_checks, solomon_checks)


def _api_2xx():
    selectors = {
        "cluster": "test",
        "service": "api",
        "host": "cluster",
        "status": "2*",
        "sensor": "requests",
    }
    expr = solomon_absence_expression_monitoring(
        project_id=project_id,
        selectors=selectors,
        warn_threshold=2,
        crit_threshold=1,
        description="Successful API 2xx",
    )
    return {
        "api-2xx": {
            "solomon": expr,
            'aggregator_kwargs': {'nodata_mode': 'force_crit'},
        }
    }
