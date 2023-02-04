from paysys.sre.tools.monitorings.configs.diehard.base import common, proxy

host = "diehard.test.proxy"
children = ["pcidss-test-proxy"]


def checks():
    return proxy.get_checks(children, common.EXCLUDED_CHECKS_TEST)
