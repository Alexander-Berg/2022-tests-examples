import logging
import os
import subprocess
import subprocess as sp
import tempfile
from os.path import join as pj

import sys
import uuid
import yatest
import yt.wrapper as yt

from ads.quality.adv_machine.lib.test_cm.common import copy_scripts
from ads.quality.adv_machine.lib.test_cm.local_robot import LocalRobot

FIXED_DATE = "20220329"
FIXED_DAYS = "1"


def prepare_paths(cm_scripts, exec_bins, catalog_dir):
    os.makedirs(cm_scripts)
    copy_scripts(yatest.common.build_path("ads/quality/adv_machine/cm"), os.path.join(cm_scripts, "cm"))

    os.makedirs(os.path.join(catalog_dir, "config"))
    os.symlink(yatest.common.source_path("ads/quality/adv_machine/config"), os.path.join(catalog_dir, "config", "config"))

    os.makedirs(exec_bins)
    src_robot_bins = yatest.common.binary_path("ads/quality/adv_machine/cm_robot/robot_bins")
    for binary in os.listdir(src_robot_bins):
        os.symlink(pj(src_robot_bins, binary), pj(exec_bins, binary))

    os.makedirs(os.path.join(catalog_dir, "indexer_bins", "indexer_bins"))
    os.symlink(yatest.common.binary_path("ads/quality/adv_machine/mr_index/am_mr_index"), os.path.join(catalog_dir, "indexer_bins", "indexer_bins", "am_mr_index"))

    os.makedirs(os.path.join(catalog_dir, "indexer_config"))
    os.symlink(yatest.common.source_path("ads/quality/adv_machine/config/indexer"), os.path.join(catalog_dir, "indexer_config", "indexer_config"))


def restore_paths(tables_tool_bin, local_yt, tables_json, sample):
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


def move_tables(tables_tool_bin, local_yt, tables_json, dst_prefix):
    cmd = [
        tables_tool_bin, 'sample',
        '--server', local_yt.get_proxy(),
        '-i', '//home/advquality/adv_machine/robot/latest/STREAMS.MERGED_FINAL.4mr_beta',
        '-t', tables_json,
        '-p', dst_prefix,
        '-d', FIXED_DATE,
        '--days', FIXED_DAYS,
        '-s', "0",  # disables sampling
        '-v',
    ]
    sp.check_call(
        cmd,
        stdin=sys.stdin,
        stdout=sys.stdout,
        stderr=sys.stderr
    )


def check_diff(local_yt, original_tables_dir, new_tables_dir):
    expected_tables = set(yt.list(original_tables_dir))
    new_tables = set(yt.list(new_tables_dir))
    symm_diff = expected_tables.symmetric_difference(new_tables)
    assert len(symm_diff) == 0, "Tables sets are different, symm_diff: {}".format(", ".join(symm_diff))

    diff_output = "//tmp/sandbox/make_cm_tables_sample/" + str(uuid.uuid4())
    diff_jobs = []

    am_mr_utils_bin_path = yatest.common.binary_path("ads/quality/adv_machine/tools/mr_utils/am_mr_utils")
    for table_name in expected_tables:
        cmd = [
            am_mr_utils_bin_path, "diff",
            '-s', local_yt.get_proxy(),
            '--old', os.path.join(original_tables_dir, table_name),
            '--new', os.path.join(new_tables_dir, table_name),
            '--dst', os.path.join(diff_output, table_name),
            '-v',
        ]
        diff_jobs.append(sp.Popen(cmd,
                                  stdin=sys.stdin,
                                  stdout=sys.stdout,
                                  stderr=sys.stderr))

    for job in diff_jobs:
        job.wait()
        assert job.returncode == 0, "Diff failed: {}".format(job.returncode)

    for table_name in expected_tables:
        full_path = os.path.join(diff_output, table_name)
        rows_count = local_yt.get(full_path + "/@row_count")
        assert rows_count == 0, "Diff in table {}, {} rows:\n{}".format(table_name, rows_count, list(yt.read_table(full_path)))


def make_robot_attempt(local_robot, datetime, prefix, tablename):
    subprocess.check_call([
        pj(local_robot.exec_bins, 'mr_attempt_yt'),
        'set-id',
        '-s', local_robot.yt.get_proxy(),
        '-a', prefix + '/' + datetime,
        '-i', datetime,
        '-v'],
        stdout=sys.stdout,
        stderr=sys.stderr
    )

    local_robot.yt.copy(
        prefix + '/latest/' + tablename,
        prefix + '/' + datetime + '/' + tablename
    )

    subprocess.check_call([
        pj(local_robot.exec_bins, 'mr_attempt_yt'),
        'set-status',
        '-s', local_robot.yt.get_proxy(),
        '-a', prefix + '/' + datetime,
        '-t', 'success',
        '-v'],
        stdout=sys.stdout,
        stderr=sys.stderr
    )


class AdvMachineRobot(LocalRobot):
    def __init__(self, links, work_path, output_path, resources_path, yt_stuff=None, yql_api=None, sh_script=None):
        self.output_path = output_path
        cm_scripts = os.path.join(output_path, "scripts", "robot_cm")
        self.exec_bins = os.path.join(work_path, "bin", "robot_bins", "robot_bins")
        self.yql_api = yql_api
        self.yatest_links = links

        prepare_paths(cm_scripts, self.exec_bins, work_path)
        cm_script_path = os.path.join(self.exec_bins, sh_script or "cmpy_robot.sh")

        super(AdvMachineRobot, self).__init__(
            cm_script_path=cm_script_path,
            yatest_links=links,
            work_path=work_path,
            output_path=output_path,
            yt_stuff=yt_stuff,
            catalog_dir=resources_path
        )

        if self.yt:
            self.tables_tool_bin = yatest.common.binary_path("ads/quality/adv_machine/cm/tables_tool/am_cm_tables")
            self.tables_description = os.path.join(yatest.common.source_path(), "ads/quality/adv_machine/cm/tables_tool/tables.json")
            restore_paths(
                tables_tool_bin=self.tables_tool_bin,
                local_yt=self.yt,
                sample=os.path.join(resources_path, "tables_data"),
                tables_json=self.tables_description
            )

        self.cm.set_var("SOLOMON_ROBOT_SERVICE", "test")

        self.cm.set_var("STREAM_GRAPH_DISABLE_CYCLE", "true")

        self.cm.set_var("DEF_MR_SERVER", self.yt.get_proxy() if self.yt else 'test')
        self.cm.set_var("YT_PROXY", self.yt.get_proxy() if self.yt else 'test')
        self.cm.set_var("BIN_DIR", self.exec_bins)
        self.cm.set_var("CMPY_BIN", os.path.join(self.exec_bins, "cmpy"))
        self.cm.set_var("SETTINGS_IS_STABLE", "false")
        self.cm.set_var("SETTINGS_IS_TESTING", "true")
        self.cm.set_var("YT_TEMP_DIR", "//tmp/adv_machine")
        self.cm.set_var("MR_TMP", "//tmp/adv_machine")
        self.cm.set_var("TMP_PREFIX", "//tmp/adv_machine")
        self.cm.set_var("MR_RUNTIME", "YT")
        self.cm.set_var("CATALOG_DIR", resources_path)
        self.cm.set_var("CM_CONFIG_DIR", os.path.join(cm_scripts, "cm", "config"))

        self.cm.set_var('SETTINGS_FIXED_DATE_END', FIXED_DATE)
        self.cm.set_var('SETTINGS_FIXED_DAYS_PERIOD', FIXED_DAYS)
        self.cm.set_var('PeriodStatsDaysPeriod', FIXED_DAYS)
        self.cm.set_var('PeriodStatsDateEnd', FIXED_DATE)
        self.cm.set_var('CatProfilesDaysPeriod', FIXED_DAYS)
        self.cm.set_var('CatProfilesDateEnd', FIXED_DATE)
        self.cm.set_var('ECOM_OFFER_STATIC_STATS_FIXED_DATE_END', FIXED_DATE)
        self.cm.set_var('ECOM_OFFER_STATIC_STATS_DAYS_PERIOD', FIXED_DAYS)
        self.cm.set_var('TESTING_FLAG', 'true')

        self.cm.set_var('CAT_PROFILES_CONFIG', os.path.join(resources_path, 'config/config/adv-machine-robot-cm-hahn/cat_profiles.json'))
        self.cm.set_var('CAT_RESOURCES_CONFIG', os.path.join(resources_path, 'config/config/adv-machine-robot-cm-hahn/cat_resources.json'))

        self.cm.set_var('STATS_THREAD_COUNT', '4')
        self.cm.set_var('FIXED_YT_CPU_LIMIT_PER_JOB', '1')
        self.cm.set_var('FIXED_YT_REPLICATION_FACTOR', '1')

        self.cm.set_var("YT_SPEC", "{}")
        self.cm.set_var("YT_PREFIX", '//home/advquality/')

        if self.yt:
            self.yt.create_dir("//tmp/adv_machine")

            self.yt.create_dir('//home/advquality/adv_machine/daily_stats')
            self.yt.create_dir('//home/advquality/adv_machine/period_stats')
            self.yt.create_dir('//home/advquality/adv_machine/stats')
            self.yt.create_dir('//home/advquality/adv_machine/cat_machine/stats/catm-profiles-from-JoinedEFHProfileHitLog')
            self.yt.create_dir('//home/advquality/adv_machine/cat_machine/stats/catm-profiles-from-JoinedYabarEFHWVProfileHitLog')
            self.yt.create_dir('//home/advquality/adv_machine/cat_machine/stats/catm-profiles-from-JoinedYabarEFHFactorsProfileHitLog')

    def __enter__(self):
        if self.yql_api is not None:
            self.cm.set_var("YQL_API_HOST", "localhost")
            self.cm.set_var("YQL_API_PORT", str(self.yql_api.port))
            self.cm.set_var("YQL_API_CLUSTER", "plato")

            logging.info('YQL is up: ')
            logging.info('\thttp://localhost:%s/', self.yql_api.port)

        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.yql_api is not None:
            from ads.quality.adv_machine.lib.test_cm.local_yql import LocalYql

            dst_path = tempfile.mkdtemp(prefix="yql_operations_", dir=self.output_path)
            logging.info('Save YQL operations dump to %s', dst_path)
            LocalYql.dump_operations(dst_path)
            self.yatest_links.set('YQL operations', dst_path)

    def check_diff(self):
        new_tables_dir = "//tmp/sandbox/make_cm_tables_sample/" + str(uuid.uuid4())
        move_tables(
            local_yt=self.yt,
            tables_tool_bin=self.tables_tool_bin,
            tables_json=self.tables_description,
            dst_prefix=new_tables_dir
        )

        check_diff(
            local_yt=self.yt,
            original_tables_dir=self.original_tables_dir,
            new_tables_dir=new_tables_dir
        )


def create_table_with_schema(yt, path, schema):
    yt.create(path, "table")
    yt.alter_table(path, schema)
