from paysys.sre.tools.monitorings.lib.util.helpers import empty_children
from paysys.sre.tools.monitorings.lib.checks.yt import Checks as YtChecks

host = "balance-test.yt"

children = empty_children


def checks():
    yt = YtChecks(
        solomon_project_id="balance",
        host=host,
        clusters_list=['hahn', 'arnold', 'vanga', 'freud']
    )

    return yt.account_bundle(account_list=['balance-test'])
