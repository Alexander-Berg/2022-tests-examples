from paysys.sre.tools.monitorings.configs.trust.base import rpslimiter, utils
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children

host = "trust.test.rpslimiter"
children = empty_children

thresholds = {
    "trust_atlas_test": utils.thresholds(5, 20),
    "trust_gateway_test": utils.thresholds(5, 20),
    "trust_test": utils.thresholds(5, 20),
}


def checks():
    return rpslimiter.get_checks(host, thresholds.keys())


def yasm_template():
    return rpslimiter.get_yasm_template(host, thresholds)
