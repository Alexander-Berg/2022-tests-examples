from paysys.sre.tools.monitorings.lib.checks import base
from paysys.sre.tools.monitorings.configs.reports.base import apps
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "reports.testing.apps"  # VIRT HOST
children = ['balance_reports_test']  # CGROUP


def checks():
    return merge(base.common_fin_fin(children), apps.get_checks(children))
