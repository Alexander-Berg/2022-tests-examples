import logging

from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib import actions
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


logger = logging.getLogger("test_contours")


@test_case
def test_module_versions(tester: ServerTester):
    actions.activate_module_version(tester, const.EXAMPLE_MAP, index=-2)
    actions.activate_module_version(tester, const.EXAMPLE_MAP, index=-1)
    build = actions.create_build(tester, module_name=const.EXAMPLE_MAP, region="cis2")
    tester.wait_until(hf.status_completed, build, const.FIVE_MINUTE)
