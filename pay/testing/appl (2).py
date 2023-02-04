from paysys.sre.tools.monitorings.configs.hyperion.base import appl

from paysys.sre.tools.monitorings.lib.checks.active.http import https_cert
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service


host = "hyperion.testing.appl"
children = ['hyp_test_apps']


def checks():
    checks = merge(
        appl.get_checks(children),
        https_cert('slb_https_cert', host='hyp-test.yandex-team.ru', ssl_host='hyp-test.yandex-team.ru'),
        check('locks_health', ttl(700, 350), gen_children(
            'paysys_backup_sox', [
                'failed_locks_hyp',
                'staled_clients_hyp',
                'staled_locks_hyp',
                'unfetched_locks_hyp'
            ])),
        {'UNREACHABLE': gen_children(['paysys_backup_sox', 'hyp_test_apps'], 'UNREACHABLE')},
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )

    checks.pop('hyp_rsync')

    checks = gen_unreach_by_service(checks)
    return checks
