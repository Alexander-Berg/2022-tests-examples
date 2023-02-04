from paysys.sre.tools.monitorings.configs.diehard.base import back, common

host = "diehard.test.back"
children = ["pcidss-test-back"]


def checks():
    return back.get_checks(children, common.EXCLUDED_CHECKS_TEST)
