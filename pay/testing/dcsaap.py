from paysys.sre.tools.monitorings.configs.dcsaap.base import dcsaap
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres
from paysys.sre.tools.monitorings.lib.util.helpers import merge, empty_children

host = "dcsaap.testing"
children = empty_children
children_app = ['billing-dcs@stage=billing-dcs-test-stage;deploy_unit=app']
children_celery = ['billing-dcs@stage=billing-dcs-test-stage;deploy_unit=celery']
fqdn = "dcs-test-l7.paysys.yandex.net"

defaults = {'namespace': 'dcsaap.testing'}


def checks():
    return merge(
        dcsaap.checks(
            children_app,
            children_celery,
            dcsaap.l7_balancer(fqdn, ['sas', 'vla'])
        ),
        postgres("mdb0s9ktcn7kj2a5pjf1", "balance_dcs_test", query_time_limit=50, alive_hosts=True,
                 alive_hosts_warn_threshold=1, alive_hosts_crit_threshold=0)
    )
