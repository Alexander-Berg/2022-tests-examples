from paysys.sre.tools.monitorings.lib.checks.qloud import base_checks
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

app = "paysys.scrooge"
children = "paysys.scrooge.testing.scrooge@type=ext"
balancer = "paysys.slb.paysys-int-test.balancer-l7@type=ext"
service = "scrooge"
hostname = "scrooge-test.paysys.yandex.net"
env = "testing"

host = "trust.test.scrooge"


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
        postgres("mdb34abv3ufk0ib2ntk7", "db1", tags=["trust_postgres"])
    )
