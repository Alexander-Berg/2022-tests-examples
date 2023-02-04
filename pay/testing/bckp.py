from paysys.sre.tools.monitorings.configs.diehard.base import bckp, common

host = "diehard.test.db-bckp"
children = ["pcidss-test-db-bckp"]


def checks():
    return bckp.get_checks(children, common.EXCLUDED_CHECKS_TEST)
