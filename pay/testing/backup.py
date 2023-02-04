from paysys.sre.tools.monitorings.lib.checks.base import common

host = "balance-test.backup-test"

children = ['backup-test']


def checks():
    return common(children)
