from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.actions import (
    create_build, enable_autostart, disable_autostart, cancel_autostart, trigger_autostart
)
from maps.garden.sandbox.server_autotests.lib.build import BuildInfo
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


def example_reduce_triggered_log(tester: ServerTester, build: BuildInfo) -> bool:
    return hf.autostarter_log_msg(tester, build, const.EXAMPLE_REDUCE)


@test_case
def test_example_reduce_autostarter(tester: ServerTester):
    disable_autostart(tester, const.EXAMPLE_REDUCE)
    enable_autostart(tester, const.EXAMPLE_REDUCE)

    # Prepare initial reduce build

    region = "cis1"
    map_build = create_build(tester, module_name=const.EXAMPLE_MAP, region=region)
    tester.wait_until(hf.status_completed, map_build, const.FIVE_MINUTE)

    reduce_page = hf.get_module_page_response(tester, module_name=const.EXAMPLE_REDUCE)
    reduce_build = create_build(
        tester,
        module_name=const.EXAMPLE_REDUCE,
        extra_params={"releaseName": reduce_page["properties"]["nextReleaseName"]},
        source_id=f"{map_build.module_name}:{map_build.build_id}",
    )
    tester.wait_until(hf.status_completed, reduce_build, const.FIVE_MINUTE)

    # Prepare trigger builder and call autostarter

    trigger_build = create_build(tester, module_name=const.EXAMPLE_MAP, region=region)
    tester.wait_until(hf.status_completed, trigger_build, const.FIVE_MINUTE)
    if tester.user_contour:
        trigger_autostart(tester, const.EXAMPLE_REDUCE, trigger_build)
    tester.wait_until(example_reduce_triggered_log, trigger_build, timeout=const.TWO_MINUTE)
    cancel_autostart(tester, const.EXAMPLE_REDUCE)


@test_case
def test_example_deployment_autostarter(tester: ServerTester):
    enable_autostart(tester, const.EXAMPLE_DEPLOYMENT)
    module_page_response = hf.get_module_page_response(tester, module_name=const.EXAMPLE_REDUCE)
    extra_params = {"releaseName": module_page_response["properties"]["nextReleaseName"]}
    build = create_build(tester, module_name=const.EXAMPLE_REDUCE, region="cis2", extra_params=extra_params)
    tester.wait_until(hf.status_completed, build, const.FIVE_MINUTE)
    if tester.user_contour:
        trigger_autostart(tester, const.EXAMPLE_DEPLOYMENT, build)
    tester.wait_until(hf.example_reduce_triggered_example_deployment, build, const.FIVE_MINUTE)
    disable_autostart(tester, const.EXAMPLE_DEPLOYMENT)
