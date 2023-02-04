from paysys.sre.tools.monitorings.lib.util.helpers import merge, check, gen_children, ttl, flaps, unreach_skip, gen_unreach_by_service
from paysys.sre.tools.monitorings.lib.checks import base, services
from paysys.sre.tools.monitorings.lib.checks.active.http import https_cert
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or

host = "crm.testing.db"  # VIRT HOST
children = ['crm_test_db']  # CGROUP

exclude_db_checks = ['db-parameters', 'oracle-status', 'psdbstat', 'db-file_locations',
                     'ps_ts_info', 'ps_segment_info', 'matview-refresh', 'shared_pool_free_mem', 'matview-monitor', 'mvlogmove', 'user-objects-in-sys', 'fk-indexing', 'net_services']

exclude_rac_checks = ['iscsi-ips', 'iscsi-params', 'oracle-patches']


def checks():

    database_result = services.oracle_database.copy()
    [database_result.pop(exclude) for exclude in exclude_db_checks]

    rac_result = services.oracle_rac.copy()
    [rac_result.pop(exclude) for exclude in exclude_rac_checks]

    result = merge(base.common_fin_fin(children),
                   rac_result,
                   database_result,
                   check('oracle-status-pcrmcdbt',
                         gen_children(['tcrm-db1f.yandex.ru'], 'oracle-status-pcrmcdbt', 'HOST'), ttl(3700, 300)),
                   check('oracle-status-dcrmcdb',
                         gen_children(['tcrm-db1f.yandex.ru'], 'oracle-status-dcrmcdb', 'HOST'), ttl(3700, 300)),
                   services.haproxy,
                   services.stunnel,
                   https_cert(name='https_certificate', port=443, warn=30, crit=14, host='tcrm-myt.yandex-team.ru', ssl_host='tcrm-myt.yandex-team.ru'),
                   services.nginx,
                   check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                                         logic_or, unreach_skip, downtime_skip)),
                   check('grid-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'grid-up'),
                                          logic_or, unreach_skip, downtime_skip)),
                   check('database-up', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'database-up'),
                                              logic_or, unreach_skip, downtime_skip)),
                   )
    result = gen_unreach_by_service(result)
    result.pop('mount')
    return result
