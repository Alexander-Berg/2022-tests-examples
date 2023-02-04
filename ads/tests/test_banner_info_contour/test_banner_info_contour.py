import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_banner_info_contour(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        # TODO: Add this stages to test!
        local_robot.cm.mark_success('banner_info.prepare_company_info')

        local_robot.cm.check_call_target('banner_info.run_all', timeout=15 * 60)

        local_robot.check_finish()
