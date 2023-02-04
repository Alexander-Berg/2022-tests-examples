import os
import yatest
import pytest
import logging

from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot
from ads.quality.adv_machine.lib.test_cm.common import run_cm_check
from ads.quality.adv_machine.cm_robot.cmpy.tools import get_targets

from robot.cmpy.library.config import Configuration, Config


def prepare_scripts(catalog_dir):
    os.makedirs(os.path.join(catalog_dir, "indexer_bins", "indexer_bins"))
    os.symlink(yatest.common.binary_path("ads/quality/adv_machine/mr_index/am_mr_index"), os.path.join(catalog_dir, "indexer_bins", "indexer_bins", "am_mr_index"))

    os.makedirs(os.path.join(catalog_dir, "indexer_config"))
    os.symlink(yatest.common.source_path("ads/quality/adv_machine/config/indexer"), os.path.join(catalog_dir, "indexer_config", "indexer_config"))

    os.makedirs(os.path.join(catalog_dir, "config", "config"))
    os.symlink(yatest.common.source_path("ads/quality/adv_machine/config/indexer"), os.path.join(catalog_dir, "config", "config", "indexer"))

    os.makedirs(os.path.join(catalog_dir, "bin", "robot_bins"))
    os.symlink(yatest.common.binary_path("ads/quality/adv_machine/cm_robot/robot_bins"), os.path.join(catalog_dir, "bin", "robot_bins", "robot_bins"))


@pytest.mark.parametrize('sh_script', [
    'cmpy_robot.sh',
    'cmpy_robot_prestable.sh',
    'cmpy_robot_testing.sh',
    'cmpy_robot_build_index_stable.sh',
    'cmpy_robot_build_index_testing.sh',
])
def test_cm_scripts_only(sh_script):
    work_path = yatest.common.work_path("test_cm_stable.work_" + sh_script)
    os.makedirs(work_path)
    output_path = yatest.common.output_path("test_cm_stable.output_" + sh_script)
    os.makedirs(output_path)

    prepare_scripts(work_path)

    run_cm_check(
        script_path=os.path.join(work_path, "bin", "robot_bins", "robot_bins", sh_script),
        dst=os.path.join(output_path, sh_script + '.for_debug.sh'),
        work_dir=work_path,
        env={
            "PREPROD_DIR": "//tmp",
            "USER": "test"
        }
    )


def test_targets_stable(links):
    work_path = yatest.common.work_path("test_targets_stable.work")
    output_path = yatest.common.output_path("test_targets_stable.output")
    resources_path = yatest.common.work_path()

    config = Config()
    config.Configuration = Configuration.PRODUCTION
    config.TestingFlag = False
    config.DryRun = False
    targets_dict = get_targets(config)

    logging.info('Targets for check: %s', targets_dict)

    with AdvMachineRobot(links, work_path, output_path, resources_path) as local_robot:
        for _, targets in targets_dict.items():
            for target in targets:
                local_robot.cm.mark_success(target)

        local_robot.check_finish()


def test_targets_prestable(links):
    work_path = yatest.common.work_path("test_targets_prestable.work")
    output_path = yatest.common.output_path("test_targets_prestable.output")
    resources_path = yatest.common.work_path()

    config = Config()
    config.Configuration = Configuration.BETA
    config.TestingFlag = False
    config.DryRun = False
    targets_dict = get_targets(config)

    logging.info('Targets for check: %s', targets_dict)

    with AdvMachineRobot(links, work_path, output_path, resources_path, sh_script='cmpy_robot_prestable.sh') as local_robot:
        for _, targets in targets_dict.items():
            for target in targets:
                local_robot.cm.mark_success(target)

        local_robot.check_finish()
