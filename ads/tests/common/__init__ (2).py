import logging
import os
import shutil
import subprocess as sp
import sys

import yatest
from ads.quality.adv_machine.lib.test_cm.local_robot import LocalRobot

FIXED_DATE = "20211020"
FIXED_DAYS = "1"


def prepare_paths(work_path):
    shutil.copytree(yatest.common.binary_path("ads/quality/adv_machine/content_merger/cm_robot/bins"), os.path.join(work_path, "bin/content_merger/bins"))
    shutil.copytree(yatest.common.binary_path("ads/quality/adv_machine/content_merger/cm_robot/scripts"), os.path.join(work_path, "bin/content_merger/scripts"))


def upload_tables(local_yt, src_prefix, dst_prefix):
    local_yt.upload(src_prefix, dst_prefix)


def restore_paths(tables_tool_bin, local_yt, sample):
    cmd = [
        tables_tool_bin, 'upload-sample-in-test',
        '--sample', sample,
    ]
    assert sp.call(
        cmd,
        stdin=sys.stdin,
        stdout=sys.stdout,
        stderr=sys.stderr,
        env={'YT_PROXY': local_yt.get_proxy()}
    ) == 0, "Failed to restore tables."


def upload_models_to_yt(yt_client, path_to_yt_path_rel):
    for local_path, yt_path in path_to_yt_path_rel.items():
        with open(local_path) as f:
            yt_client.write_file(yt_path, f)
        logging.info('Uploaded %s to %s', local_path, yt_path)


class ContentMergerRobot(LocalRobot):
    def __init__(self, links, work_path, output_path, resources_path, yt_stuff=None):
        prepare_paths(work_path)

        super(ContentMergerRobot, self).__init__(
            cm_script_path=os.path.join(work_path, "bin", "content_merger", "scripts", "cm_robot_stable.sh"),
            yatest_links=links,
            work_path=work_path,
            output_path=output_path,
            yt_stuff=yt_stuff
        )

        self.output_path = output_path

        self.tables_tool_bin = yatest.common.binary_path("ads/quality/adv_machine/cm/tables_tool/am_cm_tables")
        restore_paths(
            tables_tool_bin=self.tables_tool_bin,
            local_yt=self.yt,
            sample=os.path.join(resources_path, "tables_data")
        )

        self.cm.set_var("DEF_MR_SERVER", self.yt.get_proxy())
        self.cm.set_var("YT_PROXY", self.yt.get_proxy())
        self.cm.set_var("TestingFlag", "true")
        self.cm.set_var("StreamGraphDisableCycle", "true")

        self.cm.set_var("DefMrServer", self.yt.get_proxy())

        self.cm.set_var("YT_SPEC", "{}")

        self.cm.call_target_async('tools.ensure_yt_environment', timer=3 * 60)
        self.wait_targets(['tools.ensure_yt_environment'])

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass
