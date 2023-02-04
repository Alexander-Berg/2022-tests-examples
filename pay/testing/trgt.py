from paysys.sre.tools.monitorings.configs.bi.base import trgt
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or

host = "bi.testing.trgt"
children = ['bi_test_target']


def checks():
    checks = merge(
        trgt.get_checks(children),
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )

    checks = gen_unreach_by_service(checks)
    return checks
