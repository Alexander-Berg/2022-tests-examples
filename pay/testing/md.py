from paysys.sre.tools.monitorings.configs.bi.base import md
from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

host = "bi.testing.md"
children = ""


def checks():
    checks = merge(
        md.get_checks(),
        postgres("mdb4iqtnjdplsainj625", "db_mstr_md_test", flaps_critical=0),
        postgres("mdb4iqtnjdplsainj625", "db_mstr_hist_list_test", flaps_critical=0),
    )

    return checks
