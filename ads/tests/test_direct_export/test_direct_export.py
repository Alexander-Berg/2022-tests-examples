import logging

import yatest

from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_direct_export_info(links, yt, yql_api):
    logging.getLogger().setLevel(logging.DEBUG)
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt, yql_api) as local_robot:
        local_robot.cm.call_target_async("direct_export.run_all", timer=15 * 60)
        local_robot.wait_targets(["direct_export.run_all"])

        local_robot.cm.call_target_async("direct_export.stream_enable", timer=15 * 60)
        local_robot.wait_targets(["direct_export.stream_enable"])

        local_robot.check_finish()
