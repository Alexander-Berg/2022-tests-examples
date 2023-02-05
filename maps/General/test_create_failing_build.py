import logging

from maps.garden.sandbox.server_autotests.lib.constants import EXAMPLE_MAP, TWO_MINUTE, FIVE_MINUTE
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.actions import create_build
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


logger = logging.getLogger("test_create_failing_build")


@test_case
def test_create_failing_build_tr(tester: ServerTester):
    test_create_failing_build(tester, region="tr", error_type="RuntimeError")


@test_case
def test_create_failing_build_aao(tester: ServerTester):
    test_create_failing_build(tester, region="aao", error_type="AutotestsFailedError")


def test_create_failing_build(tester: ServerTester, region: str, error_type: str):
    build = create_build(tester, module_name=EXAMPLE_MAP, region=region)
    tester.wait_until(hf.status_failed, build, FIVE_MINUTE)
    hf.build_has_failed_event(tester, build)
    hf.check_error_in_error_log(tester, build, error_type)
    hf.tasks_in_full_info(tester, build)
    tester.wait_until(hf.error_in_tasks, build, TWO_MINUTE)
    hf.remove_build_and_wait_event(tester, build)
