from paysys.sre.tools.monitorings.configs.ift.base import ift
from paysys.sre.tools.monitorings.configs.mdh.common import Testing, PgCluster


cfg = ift.IftConfig(env=Testing())

host = cfg.host_alias
namespace = cfg.project
children = cfg.children_all


def checks():
    return cfg.get_checks(
        pg_cluster=PgCluster('mdb3hmq9boougkhf1o31', size='nano'),
    )
