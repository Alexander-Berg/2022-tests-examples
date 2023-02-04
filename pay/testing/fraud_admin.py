from paysys.sre.tools.monitorings.lib.checks.qloud import base_checks
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

app = "paysys.trust-fraud-admin"
children = "paysys.trust-fraud-admin.testing.fraud-admin@type=ext"
balancer = "paysys.slb.paysys-int-test.balancer-l7@type=ext"
service = "trust-fraud-admin"
hostname = "trust-fa-test.paysys.yandex-team.ru"
env = "testing"

host = "trust.test.fraud-admin.app"


def checks():
    return merge(
        base_checks(
            app,
            children,
            balancer,
            service,
            hostname,
            env,
        ),
        postgres("285fe0dd-a5af-42f9-bee7-d25155aac224", "fraud_admin_test")
    )
