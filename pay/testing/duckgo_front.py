from paysys.sre.tools.monitorings.configs.diehard.base import common, front

host = "diehard.test.duckgo-front"
children = ["pcidss-test-duckgo-front"]


def checks():
    return front.get_checks(children, common.EXCLUDED_CHECKS_TEST)
