import logging

from maps.garden.sandbox.server_autotests.lib.constants import EXAMPLE_MAP, FIVE_MINUTE
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester
from maps.garden.sandbox.server_autotests.lib.actions import create_build, pin_build, unpin_build


logger = logging.getLogger("test_create_failing_build")


@test_case
def test_create_delete_build(tester: ServerTester):
    build = create_build(tester, module_name=EXAMPLE_MAP, region="cis1")
    tester.wait_until(hf.status_completed, build, timeout=FIVE_MINUTE)
    hf.check_time(tester, build)
    tester.wait_until(hf.build_has_completed_event, build)
    hf.check_no_error_in_error_log(tester, build)
    tester.wait_until(hf.tasks_in_full_info, build)
    tester.wait_until(hf.build_in_module_statistics, build)
    tester.wait_until(hf.build_in_build_statistics, build)
    key = hf.check_storage_and_get_key(tester, build)
    pin_build(tester, build)
    tester.wait_until(hf.build_has_pinned_event, build)
    unpin_build(tester, build)
    tester.wait_until(hf.build_has_unpinned_event, build)
    hf.remove_build_and_wait_event(tester, build)
    hf.check_removed_build_error_log(tester, build)
    tester.wait_until(hf.tasks_in_full_info, build)
    tester.wait_until(hf.build_in_module_statistics, build)
    tester.wait_until(hf.build_in_build_statistics, build)
    hf.check_removed_build_storage(tester, key)
