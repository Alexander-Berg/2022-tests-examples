from paysys.sre.tools.monitorings.configs.balance_fop.base import fop

host = "balance_fop.balance-fop-test-l7"

children = ['balance-fop@stage=fop-test-stage']

fqdn = "balance-fop-test-l7.paysys.yandex.net"


def checks():
    return fop.checks(
        children,
        fop.l7_balancer(fqdn, ['man', 'vla'])
    )
