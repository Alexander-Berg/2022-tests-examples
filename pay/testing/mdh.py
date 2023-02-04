from paysys.sre.tools.monitorings.configs.mdh.base import mdh
from paysys.sre.tools.monitorings.configs.mdh.common import Testing


cfg = mdh.MdhConfig(env=Testing())

host = cfg.host_alias
namespace = cfg.project
children = cfg.children_all


def checks():
    return cfg.get_checks(
        pg_cluster='mdbtqpadhhc820ji13ir',
    )
