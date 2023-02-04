import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot
from ads.quality.adv_machine.cm_robot.tests.common.test_contours import run_and_check_main_contour, run_and_check_build_contour


def create_table_with_schema(yt, path, schema):
    yt.create("table", path)
    yt.alter_table(path, schema)


def test_all_contours(links, yt_stuff):
    work_path = yatest.common.work_path("test_all_contours")
    output_path = yatest.common.output_path("test_all_contours")
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        run_and_check_main_contour(local_robot)

        run_and_check_build_contour(local_robot)
