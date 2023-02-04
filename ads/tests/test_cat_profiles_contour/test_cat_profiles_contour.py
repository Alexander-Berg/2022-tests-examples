import os
import shutil
import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_stats_contour(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    shutil.copy(
        os.path.join(resources_path, 'geodata4-tree+ling.bin'),
        os.path.join(resources_path, 'geodata5.bin')
    )

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        local_robot.cm.call_target_async("cat_profiles_attempt.mark_ready", timer=15 * 60)
        local_robot.wait_targets([
            "cat_profiles_attempt.mark_ready",
            "cat_profiles.upload_and_release_cat_resources",
            "cat_profiles.upload_and_release_idfs",
            "cat_profiles.upload_and_release_goals_dict"
        ])

        local_robot.check_finish()
