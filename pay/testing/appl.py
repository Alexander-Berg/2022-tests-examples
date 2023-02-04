from paysys.sre.tools.monitorings.configs.bi.base import appl

from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, gen_children, check, unreach_skip, flaps, gen_unreach_by_service
from paysys.sre.tools.monitorings.lib.util.aggregators import downtime_skip, logic_or
from paysys.sre.tools.monitorings.lib.checks.active.http import https_cert

host = "bi.testing.appl"
children = ['mstr_test_app']


def checks():
    checks = merge(
        https_cert('slb_https_cert', host='bi-test.yandex-team.ru', ssl_host='bi-test.yandex-team.ru'),
        appl.get_checks(children),
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )

    checks.pop('bi_rsync')

    checks = gen_unreach_by_service(checks)
    return checks
