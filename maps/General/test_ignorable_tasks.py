from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.actions import create_build, ignore_tasks
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


@test_case
def test_ignorable_tasks(tester: ServerTester):
    ex_map_build = create_build(tester, module_name=const.EXAMPLE_MAP, region="saa")
    tester.wait_until(hf.status_failed, ex_map_build, timeout=const.FIVE_MINUTE)
    ignore_tasks(tester, ex_map_build)
    # Ignore exception here becouse build has terminal state "failed"
    tester.wait_until(hf.status_completed, ex_map_build, timeout=const.HALF_MINUTE, ignore_exception=True)
    ex_reduce_build = create_build(tester, module_name=const.EXAMPLE_REDUCE, region="saa")
    tester.wait_until(hf.status_completed, ex_reduce_build, timeout=const.FIVE_MINUTE)
