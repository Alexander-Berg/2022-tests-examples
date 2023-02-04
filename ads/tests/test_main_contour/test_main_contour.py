import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot
from ads.quality.adv_machine.cm_robot.tests.common.test_contours import run_and_check_main_contour


def test_main_contour(links, yt_stuff):
    work_path = yatest.common.work_path("test_main_contour")
    output_path = yatest.common.output_path("test_main_countour")
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        run_and_check_main_contour(local_robot)
