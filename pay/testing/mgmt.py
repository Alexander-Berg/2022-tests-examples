from paysys.sre.tools.monitorings.configs.diehard.base import common

host = "diehard.test.mgmt"
children = ["pcidss-test-mgmt"]


def checks():
    return common.get_checks(children, common.EXCLUDED_CHECKS_TEST)
