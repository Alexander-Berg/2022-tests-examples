import os
import yatest

from ads.quality.adv_machine.lib.test_cm.common import run_cm_check
from ads.quality.adv_machine.content_merger.cm_robot.tests.common import prepare_paths


def test_cm():
    work_path = yatest.common.work_path()
    prepare_paths(work_path)

    run_cm_check(
        script_path=os.path.join(work_path, "bin", "content_merger", "scripts", "cm_robot_stable.sh"),
        dst=yatest.common.output_path('result_cm_robot_for_debug.sh'),
        hostlist=os.path.join(work_path, "bin", "content_merger", "scripts", "hostlist")
    )
