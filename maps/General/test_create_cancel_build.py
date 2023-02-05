import logging

from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.actions import cancel_build, create_build
from maps.garden.sandbox.server_autotests.lib.constants import EXAMPLE_MAP
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


logger = logging.getLogger("test_create_failing_build")


@test_case
def test_create_cancel_build(tester: ServerTester):
    build = create_build(tester, module_name=EXAMPLE_MAP, region="na")
    tester.wait_until(hf.status_in_progress, build)
    tester.wait_until(hf.tasks_in_full_info, build)
    tester.wait_until(hf.build_id_in_tasks, build)
    cancel_build(tester, build)
    tester.wait_until(hf.build_has_cancelled_event, build)
    tester.wait_until(hf.status_cancelled, build)
    hf.check_no_error_in_error_log(tester, build)
    hf.remove_build_and_wait_event(tester, build)
