from paysys.sre.tools.monitorings.configs.balance_app.base import balance
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.services import ps_ping, ps_closedc
from paysys.sre.tools.monitorings.lib.checks.mds import s3_mds


host = "balance-test.balance-test"

children = ['ps-front-test', 'ps-back-test']
zk_hosts = ['greed-ts1h.paysys.yandex.net', 'greed-tm1h.paysys.yandex.net']


def checks():
    return merge(
        balance.get_checks(children),
        balance.zookeeper([(host, 'zookeeper', 'HOST') for host in zk_hosts]),
        balance.xscript_socket,
        ps_ping,
        ps_closedc,
        s3_mds(
            mds_installation_type='testing',
            alias='balance-s3-logs',
            service_id=1996,
        ),
    )
