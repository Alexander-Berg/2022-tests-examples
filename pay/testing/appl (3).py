from paysys.sre.tools.monitorings.configs.oebsapi.base import appl
from paysys.sre.tools.monitorings.lib.checks import base, services
from paysys.sre.tools.monitorings.lib.util.helpers import merge, check, flaps, ttl, gen_children, unreach_skip, gen_unreach_by_service
from paysys.sre.tools.monitorings.lib.util.aggregators import logic_or, downtime_skip

host = "oebsapi.testing.appl"  # VIRT HOST
children = ['oebs.wto.test']  # CGROUP


def checks():
    result = merge(
        base.common_fin_fin(children),
        services.nginx,
        appl.get_checks(children),
        services.jmx2graphite,
        services.yandex_cauth_cache,
        check('uptime', merge(flaps(900, 0), ttl(630, 330), gen_children(children, 'uptime'),
                              logic_or, unreach_skip, downtime_skip)),
    )
    result = gen_unreach_by_service(result)
    return result
