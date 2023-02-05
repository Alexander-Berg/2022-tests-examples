import logging

from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.actions import create_build
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


DEPLOY_STEPS = [
    const.DEPLOY_TESTING,
    const.DEPLOY_PRESTABLE,
    const.DEPLOY_STABLE
]

logger = logging.getLogger("test_post")


@test_case
def test_pipeline(tester: ServerTester):
    ex_map_build = create_build(tester, module_name=const.EXAMPLE_MAP, region="cis2")
    tester.wait_until(hf.status_completed, ex_map_build, const.FIVE_MINUTE)
    tester.wait_until(hf.build_has_completed_event, ex_map_build)
    hf.check_no_error_in_error_log(tester, ex_map_build)
    tester.wait_until(hf.tasks_in_full_info, ex_map_build)
    tester.wait_until(hf.build_in_module_statistics, ex_map_build)

    module_page_response = hf.get_module_page_response(tester, module_name=const.EXAMPLE_REDUCE)
    extra_params = {"releaseName": module_page_response["properties"]["nextReleaseName"]}
    source_id = f"{ex_map_build.module_name}:{ex_map_build.build_id}"
    ex_reduce_build = create_build(tester, module_name=const.EXAMPLE_REDUCE, extra_params=extra_params, source_id=source_id)
    tester.wait_until(hf.status_completed, ex_reduce_build, const.FIVE_MINUTE)
    tester.wait_until(hf.build_has_completed_event, ex_reduce_build)
    hf.check_no_error_in_error_log(tester, ex_reduce_build)
    tester.wait_until(hf.tasks_in_full_info, ex_reduce_build)
    tester.wait_until(hf.build_in_module_statistics, ex_reduce_build)

    ex_deploy_builds = []
    for deploy_step in DEPLOY_STEPS:
        logger.info(f"Deploy_step `{deploy_step}`")
        extra_params = {"deployStep": deploy_step}
        build = create_build(
            tester,
            module_name=const.EXAMPLE_DEPLOYMENT,
            deploy_step=deploy_step,
            extra_params=extra_params,
            source_id=f"{ex_reduce_build.module_name}:{ex_reduce_build.build_id}"
        )
        ex_deploy_builds.append(build)
        tester.wait_until(hf.status_completed, build, const.FIVE_MINUTE)
        tester.wait_until(hf.build_has_completed_event, build)
        hf.check_no_error_in_error_log(tester, build)
        tester.wait_until(hf.tasks_in_full_info, build)
        tester.wait_until(hf.build_in_module_statistics, build)
        hf.check_full_hierarchy(tester, build)
    for build in ex_deploy_builds:
        hf.remove_build_and_wait_event(tester, build)
    hf.remove_build_and_wait_event(tester, ex_reduce_build)
    hf.remove_build_and_wait_event(tester, ex_map_build)
