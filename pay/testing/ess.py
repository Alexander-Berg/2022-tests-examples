from paysys.sre.tools.monitorings.configs.hyperion.base import ess

from paysys.sre.tools.monitorings.lib.checks.active.http import https_cert
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service


host = "hyperion.testing.ess"
children = ['hyp_test_ess']


def checks():
    checks = merge(
        ess.get_checks(children),
        https_cert('ess_https_cert', host='thyp-ess2e.yandex.ru', ssl_host='thyp-ess2e.yandex.ru', port=8080),
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )

    checks = gen_unreach_by_service(checks)
    return checks
