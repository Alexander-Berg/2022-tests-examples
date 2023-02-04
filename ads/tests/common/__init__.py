import logging
import os
import shutil
import subprocess as sp
import sys
import tempfile

import uuid
import yatest
from ads.quality.adv_machine.lib.test_cm.local_robot import LocalRobot
from robot.library.yuppie.modules.io import IO

FIXED_DATE = "20200101"
FIXED_DAYS = "1"


def prepare_paths(work_path):
    shutil.copytree(yatest.common.binary_path("ads/quality/adv_machine/tsar/cm_robot/bins"), os.path.join(work_path, "bin/tsar/bins"))
    shutil.copytree(yatest.common.binary_path("ads/quality/adv_machine/tsar/cm_robot/scripts"), os.path.join(work_path, "bin/tsar/scripts"))


def upload_tables(local_yt, src_prefix, dst_prefix):
    local_yt.upload(src_prefix, dst_prefix)


def restore_paths(tables_tool_bin, local_yt, tables_json, dst_prefix):
    cmd = [
        tables_tool_bin, 'restore',
        '--server', local_yt.get_proxy(),
        '-t', tables_json,
        '-p', dst_prefix,
        '-d', FIXED_DATE,
        '--days', FIXED_DAYS,
        '-v',
    ]
    assert sp.call(
        cmd,
        stdin=sys.stdin,
        stdout=sys.stdout,
        stderr=sys.stderr
    ) == 0, "Failed to restore tables. List: {}".format(", ".join(local_yt.list(dst_prefix)))


def upload_models_to_yt(yt_client, path_to_yt_path_rel):
    for local_path, yt_path in path_to_yt_path_rel.items():
        with open(local_path) as f:
            yt_client.write_file(yt_path, f)
        logging.info('Uploaded %s to %s', local_path, yt_path)


class TsarRobot(LocalRobot):
    def __init__(self, links, work_path, output_path, resources_path, yt_stuff=None, local_yql_api=None):
        prepare_paths(work_path)

        super(TsarRobot, self).__init__(
            cm_script_path=os.path.join(work_path, "bin", "tsar", "scripts", "cm_robot_hahn.sh"),
            yatest_links=links,
            work_path=work_path,
            output_path=output_path,
            yt_stuff=yt_stuff
        )

        self.local_yql_api = local_yql_api
        self.output_path = output_path
        self.yatest_links = links

        self.original_tables_dir = "//tmp/sandbox/make_cm_tables_sample/" + str(uuid.uuid4())
        tables_data = IO.unpack(os.path.join(resources_path, "tables_data"))
        upload_tables(self.yt, tables_data, self.original_tables_dir)

        self.tables_tool_bin = yatest.common.binary_path("ads/quality/adv_machine/cm/tables_tool/am_cm_tables")
        self.tables_description = os.path.join(yatest.common.source_path(), "ads/quality/adv_machine/tsar/cm_robot/tables.json")
        restore_paths(
            tables_tool_bin=self.tables_tool_bin,
            local_yt=self.yt,
            tables_json=self.tables_description,
            dst_prefix=self.original_tables_dir
        )

        self.cm.set_var("DEF_MR_SERVER", self.yt.get_proxy())
        self.cm.set_var("YT_PROXY", self.yt.get_proxy())
        self.cm.set_var("TestingFlag", "true")
        self.cm.set_var("StreamGraphDisableCycle", "true")

        if self.local_yql_api is not None:
            self.cm.set_var("YqlPort", str(self.local_yql_api.port))
            self.cm.set_var("YqlServer", 'localhost')

        self.cm.set_var("DefMrServer", self.yt.get_proxy())

        self.cm.set_var("YT_SPEC", "{}")

        self.cm.call_target_async('tools.ensure_yt_environment', timer=3 * 60)
        self.wait_targets(['tools.ensure_yt_environment'])

    def __enter__(self):
        if self.local_yql_api is not None:
            self.cm.set_var("YQL_API_URL", "http://localhost:{port}/api/v2".format(port=self.local_yql_api.port))
            self.cm.set_var("YQL_API_CLUSTER", "plato")

            logging.info('YQL is up: ')
            logging.info('\thttp://localhost:%s/', self.local_yql_api.port)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.local_yql_api is not None:
            from ads.quality.adv_machine.lib.test_cm.local_yql import LocalYql

            dst_path = tempfile.mkdtemp(prefix="yql_operations_", dir=self.output_path)
            logging.info('Save YQL operations dump to %s', dst_path)
            LocalYql.dump_operations(dst_path)
            self.yatest_links.set('YQL operations', dst_path)
