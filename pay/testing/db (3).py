from paysys.sre.tools.monitorings.configs.vertica.base import db
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service


host = "vertica.testing.db"
children = ['vrt_test']


def checks():
    checks = merge(
        db.get_checks(children),
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )
    checks.pop('database_copycluster')

    checks = gen_unreach_by_service(checks)
    return checks
