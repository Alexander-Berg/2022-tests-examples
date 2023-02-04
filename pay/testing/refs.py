from paysys.sre.tools.monitorings.configs.mdh.common import Testing, PgCluster
from paysys.sre.tools.monitorings.configs.refs.base import refs


cfg = refs.RefsConfig(env=Testing())

host = cfg.host_alias
namespace = cfg.project
children = cfg.children_all


def checks():
    return cfg.get_checks(
        pg_cluster=PgCluster('9d797f5b-2dbb-4e31-bc1f-edc30e88bf0b', size='micro'),
    )
