from paysys.sre.tools.monitorings.lib.util.helpers import check, gen_children, merge, gen_unreach_by_service, flaps, ttl, unreach_skip
from paysys.sre.tools.monitorings.configs.oebs.base import appl, database
from paysys.sre.tools.monitorings.lib.checks import base, services
from paysys.sre.tools.monitorings.lib.util.aggregators import logic_or, downtime_skip

host = "oebs.testing.clones"  # VIRT HOST
children = ['oebs_hr_singlenode', 'oebs_test_singlenode']  # CGROUP
exclude_db_checks = [
    'rm-logs', 'ro-services', 'db-parameters', 'db-file_locations', 'ps_ts_info', 'ps_segment_info',
    'mvlogmove', 'shared_pool_free_mem', 'ebs-processes',
    'matview-refresh', 'matview-monitor', 'fk-indexing'
]

exclude_appl_checks = ['OACore_monitor', 'outsource-mailer']

modif_db_checks = ['user-objects-in-sys', 'oracle-status', 'etcc-check']

modif_appl_checks = ['autopatch', 'icm', 'opp_check', 'opp_flap', 'wf_bgproc_check', 'XXSA']


def checks():
    ldap_modif = services.ldap.copy()
    ldap_modif.pop('ldap-limits')

    database_global = services.oracle_database.copy()
    database_project = database.get_checks(children)

    database_result = merge(database_global, database_project)
    [database_result.pop(exclude) for exclude in exclude_db_checks]
    [database_result[modif].update(logic_or) for modif in modif_db_checks]

    appl_checks = appl.get_checks(children)
    [appl_checks[modif].update(logic_or) for modif in modif_appl_checks]
    [appl_checks.pop(exclude) for exclude in exclude_appl_checks]

    base_checks = base.common_fin_fin(children)
    _ = base_checks.pop('host_memory_usage')
    _ = base_checks.pop('link_utilization')

    result = merge(
        base_checks,
        services.nginx,
        services.oracle_rac,
        database_result,
        ldap_modif,
        services.yandex_cauth_cache,
        services.haproxy,
        services.stunnel,
        appl_checks,
        check('check_depersonal', gen_children(['oebs_test_singlenode'], 'check_depersonal')),
        services.sslcerts,
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
        check('grid-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'grid-up'),
                               logic_or, unreach_skip, downtime_skip)),
        check('database-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'database-up'),
                                   logic_or, unreach_skip, downtime_skip)),
        check('pending_requests', merge(flaps(900, 0), ttl(700, 300), unreach_skip, downtime_skip)),
    )
    result = gen_unreach_by_service(result)
    return result
