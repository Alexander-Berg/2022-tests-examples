import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_page_resource(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        local_robot.cm.call_target_async("page_resource.run_all", timer=15 * 60)
        local_robot.wait_targets(["page_resource.run_all", "page_resource.upload_and_release_dict"])

        local_robot.check_finish()
