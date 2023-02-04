from paysys.sre.tools.monitorings.configs.hyperion.base import db
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service


host = "hyperion.testing.db"
children = ['thyp-apps2e.yandex.ru']


def checks():
    checks = merge(
        db.get_checks(children),
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime', 'HOST'),
                              logic_or, unreach_skip, downtime_skip)),
        check('grid-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'grid-up', 'HOST'),
                               logic_or, unreach_skip, downtime_skip)),
        check('database-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'database-up', 'HOST'),
                                   logic_or, unreach_skip, downtime_skip)),
    )

    checks.pop('audit-splunk')
    checks.pop('oracle_memory_usage')
    checks.pop('oracle-patches')
    checks.pop('oracle-status')
    checks.pop('psdbstat')
    checks.pop('ps_segment_info')
    checks.pop('ps_ts_info')
    checks.pop('rm-logs')
    checks.pop('shared_pool_free_mem')
    checks.pop('standby-lag')
    checks.pop('store_audit')
    checks.pop('user-objects-in-sys')

    checks_with_child = {}
    for name, data in checks.items():
        if 'children' in data:
            checks_with_child[name] = data
            continue
        checks_with_child[name] = merge(
            data,
            gen_children(children, name, 'HOST')
        )

    checks = gen_unreach_by_service(checks_with_child)
    return checks
