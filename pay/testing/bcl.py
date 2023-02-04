from paysys.sre.tools.monitorings.configs.mdh.common import Testing, PgCluster
from paysys.sre.tools.monitorings.configs.bcl.base import bcl


cfg = bcl.BclConfig(env=Testing())

host = cfg.host_alias
namespace = cfg.project
children = cfg.children_all


def checks():
    return cfg.get_checks(
        pg_cluster=PgCluster('d4c884e7-12f1-4557-8c24-995c9ce922f5'),
    )
