from paysys.sre.tools.monitorings.configs.diehard.base import common, galera
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "diehard.test.galera"
children = ["pcidss-test-db-galera"]


def checks():
    return merge(
        galera.get_checks(children, common.EXCLUDED_CHECKS_TEST),
        galera.mysql_status("Com_select", children),
        galera.mysql_status("Com_insert", children),
        galera.mysql_status("Com_update", children),
        galera.mysql_status("Com_delete", children),
        galera.mysql_free_connections(children),
    )
