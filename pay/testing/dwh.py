from paysys.sre.tools.monitorings.configs.dwh.base import dwh
from paysys.sre.tools.monitorings.configs.mdh.common import Testing, PgCluster


cfg = dwh.DwhConfig(env=Testing())

host = cfg.host_alias
namespace = cfg.project
children = cfg.children_all


def checks():
    return cfg.get_checks(
        pg_cluster=PgCluster('mdbtig3970de7m0d6l1p', size='nano'),
    )
