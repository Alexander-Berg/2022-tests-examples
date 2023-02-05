from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.actions import create_source_build, scan_resources
from maps.garden.sandbox.server_autotests.lib.build import BuildInfo
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


@test_case
def test_source_build(tester: ServerTester):
    build = create_source_build(tester, const.EXTRA_POI_BUNDLE)
    tester.wait_until(hf.status_completed, build, const.TEN_MINUTE)
    tester.wait_until(hf.build_has_completed_event, build, const.TWO_MINUTE)
    hf.check_no_error_in_error_log(tester, build)
    tester.wait_until(hf.tasks_in_full_info, build)
    tester.wait_until(hf.build_in_module_statistics, build)
    hf.remove_build_and_wait_event(tester, build)


@test_case
def test_scan_resources(tester: ServerTester):
    scan_resources(tester, const.YMAPSDF_SRC)
    tester.wait_until(hf.scan_in_logs, BuildInfo(const.YMAPSDF_SRC, 0), timeout=const.FIVE_MINUTE)
