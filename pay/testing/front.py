from paysys.sre.tools.monitorings.configs.diehard.base import common, front
from paysys.sre.tools.monitorings.lib.checks.active.http import https
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "diehard.test.front"
children = ["pcidss-test-front"]
http_host = "diehard-mock-test.paysys.yandex.net"


def checks():
    return merge(
        front.get_checks(children, common.EXCLUDED_CHECKS_TEST),
        https(name="https_inbound", headers={"Host": http_host}, port=8043),
        https(name="https_outbound", headers={"Host": http_host}, port=8044),
    )
