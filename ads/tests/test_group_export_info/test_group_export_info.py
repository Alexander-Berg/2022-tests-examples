import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_group_export_info(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        local_robot.cm.call_target_async("group_export_info.run_all", timer=15 * 60)
        local_robot.wait_targets(["group_export_info.run_all"])

        local_robot.cm.call_target_async("group_export_info.stream_enable", timer=15 * 60)
        local_robot.wait_targets(["group_export_info.stream_enable"])

        local_robot.check_finish()
