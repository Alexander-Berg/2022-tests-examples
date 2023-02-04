from paysys.sre.tools.monitorings.lib.checks.reactor import Checks as ReactorChecks
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children

host = "balance-test.reactor"

children = empty_children


def checks():
    reactor_project_id = 7892453
    reactor = ReactorChecks("balance_test", reactor_project_id, "balance")
    return reactor.bundle()
