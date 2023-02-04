#!/usr/bin/env python
# -*- coding: utf-8 -*-

import errno
import json
import logging
import os
import pytest
import shutil
import subprocess
import sys
import time
import yatest.common

from library.python.testing.deprecated import setup_environment
from multiprocessing.pool import ThreadPool
from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
from yatest.common import source_path
from yt.wrapper import YtClient
import yt_client
import yt.yson

DONT_STOP_LOCAL_YT = 'dont_stop_local_yt'
ENABLE_DEBUG_LOGGING = 'enable_debug_logging'


def log_message(msg):
    logging.info(msg)


def yt_initialize():
    yt_client.initialize(*map(lambda s: s.encode('ascii'), sys.argv))
    log_message("NYT::Initialize is called")


@pytest.fixture  # noqa
def make_cpp_yt_init():
    spec_env_name = 'YT_SPEC'
    old_spec = os.getenv(spec_env_name)
    try:
        os.putenv(spec_env_name, json.dumps({
            u'reducer': {
                u'environment': {
                    u'Y_PYTHON_ENTRY_POINT': u'maps.search.libs.integration_testlib.test_sequence:yt_initialize'
                }
            },
            u'mapper': {
                u'environment': {
                    u'Y_PYTHON_ENTRY_POINT': u'maps.search.libs.integration_testlib.test_sequence:yt_initialize'
                }
            }
        }))
        yield
    finally:
        if old_spec:
            os.putenv(spec_env_name, old_spec)
        else:
            os.unsetenv(spec_env_name)


def make_dir(dir_name):
    if not dir_name:
        return

    try:
        os.makedirs(dir_name)
    except OSError as exc:
        if exc.errno == errno.EEXIST and os.path.isdir(dir_name):
            pass
        else:
            raise
    except:
        log_message('Error: Failed to create dir: {dir_name}, pwd : {pwd}'.
                    format(dir_name=dir_name,
                           pwd=os.getcwd()))


class YtStagesTestBase(object):
    """ Интеграционный тест

    Attributes:
        pack                 Список этапов теста.
    """

    # replace it in subclasses
    pack = None
    cypress_dir = None

    stuff = None
    dump_dir = 'dump_dir'

    @classmethod
    def setup_class(cls):
        if cls.cypress_dir is None:
            cls.cypress_dir = os.path.abspath('./tables_dir')
        else:
            cls.cypress_dir = source_path(cls.cypress_dir)
        cls.stuff = YtStuff(config=YtConfig(forbid_chunk_storage_in_tmpfs=True,
                                            local_cypress_dir=cls.cypress_dir,
                                            enable_debug_logging=yatest.common.get_param(ENABLE_DEBUG_LOGGING, False)))
        cls.stuff.start_local_yt()

        cls.bin_dir = setup_environment.setup_bin_dir()
        cls.exit_codes = {}

        for stage in cls.pack:
            for file_name in stage.input_files + stage.output_files:
                dir_name = os.path.dirname(file_name)
                make_dir(dir_name)

    @classmethod
    def teardown_class(cls):
        if cls.stuff and not bool(yatest.common.get_param(DONT_STOP_LOCAL_YT, False)):
            log_message("Local YT stopped")
            cls.stuff.stop_local_yt()
        else:
            log_message("Local YT wasn't stopped, you can use it to browse state of tables & files")
            log_message("You can find it at {}".format(cls.stuff.get_server()))

    @classmethod
    def stage_name(cls, stage):
        stage_class_name = stage.__name__ if isinstance(stage, type) else stage.__class__.__name__
        return "{test_class_name}.{stage_class_name}".format(test_class_name=cls.__name__,
                                                             stage_class_name=stage_class_name)

    def pytest_generate_tests(self, metafunc):
        if 'stage' in metafunc.fixturenames:
            metafunc.parametrize("stage", self.pack, False,
                                 list(map(lambda x: self.stage_name(x), self.pack)))

    def test_run_stage(self, stage):
        stage_name = self.stage_name(stage)
        log_message("{} started ".format(stage_name))
        time_before = time.time()

        self.exit_codes["cmd." + stage_name] = self.run_stage(stage)
        self.exit_codes["dumper." + stage_name] = self.dump_text_results(stage)

        time_after = time.time()
        log_message("{} elapsed in {:.2f} seconds".format(stage_name, time_after - time_before))

    def launcher(self, cmd):
        env = os.environ.copy()
        for k, v in self.stuff.env.items():
            if k == 'PATH' and 'PATH' in env:
                env[k] = v + ":" + env[k]
            else:
                env[k] = v
        process = subprocess.Popen(cmd, shell=True, env=env)
        process.wait()
        return process.returncode

    def run_stage(self, stage):
        bin = self.bin_dir
        server = self.stuff.get_server()
        os.putenv('YT_PROXY', server)

        for file_name in stage.input_files:
            input_file_name = yatest.common.source_path(file_name)
            if os.path.isfile(input_file_name):
                shutil.copyfile(input_file_name, file_name)

        if stage.cmds and stage.name_program:
            for user_cmd in stage.cmds:
                binary = os.path.join(bin, stage.name_program)
                cmd = user_cmd.format(mr_server=server,
                                      program=binary,
                                      bin_dir=self.bin_dir,
                                      dump_dir=self.dump_dir)
                cmd = " ".join(cmd.split())
                cmd_exit_code = self.launcher(cmd)
                if cmd_exit_code != 0:
                    pytest.fail("command {} failed with exit code {:d} ".format(cmd, cmd_exit_code))
                    return cmd_exit_code

        if stage.func:
            yt_initialize()
            for changing_kwargs in stage.kwargs_list:
                kwargs = {
                    key: value.format(mr_server=server) if isinstance(value, str) else value
                    for key, value in list(stage.kwargs.items()) + list(changing_kwargs.items())
                }
                stage.func(**kwargs)

        return 0

    def dump_text_results(self, stage):
        make_dir(self.dump_dir)

        yt_client = YtClient(proxy=self.stuff.get_server(), token='')
        for table, sorted_table_path in self.sort_output_tables(yt_client, stage.output_tables).items():
            dump_file_path = os.path.join(self.dump_dir, table.replace('/', ':') + '.txt')
            with open(dump_file_path, 'w') as dump_file:
                for row in yt_client.read_table(sorted_table_path):
                    json.dump(self.prepare_for_dump(row),
                              dump_file,
                              indent=2,
                              ensure_ascii=False,
                              sort_keys=True)
                    dump_file.write('\n')

        for file_name in stage.output_files:
            if os.path.isfile(file_name):
                shutil.copyfile(file_name, os.path.join(self.dump_dir, file_name.replace('/', ':') + '.txt'))

        return 0

    def prepare_for_dump(self, obj):
        if isinstance(obj, float):
            return round(obj, 6)
        if isinstance(obj, yt.yson.yson_types.YsonStringProxy):
            return ' '.join(f'{b:02x}' for b in yt.yson.get_bytes(obj))
        if isinstance(obj, dict):
            return {k: self.prepare_for_dump(v) for k, v in obj.items()}
        if isinstance(obj, (list, tuple)):
            return [self.prepare_for_dump(o) for o in obj]
        return obj

    def sort_output_tables(self, yt_client, tables):
        if not tables:
            return {}

        pool = ThreadPool(processes=min(10, len(tables)))
        table_paths = pool.map(lambda table: self.sort_table(yt_client, table), tables)
        sorted_table_paths = {
            tables[idx]: table_paths[idx]
            for idx in range(len(tables))
        }
        return sorted_table_paths

    def sort_table(self, yt_client, table_path):
        schema = list(yt_client.get_attribute(table_path, 'schema'))
        sort_by = [schema_item['name'] for schema_item in schema if schema_item['type'] != 'any']

        if not sort_by:
            return table_path

        sorted_table_path = table_path + '.sorted'
        yt_client.run_sort(table_path, sorted_table_path, sort_by)
        return sorted_table_path

    def test_files(self):
        canonical_files = {}
        yacf = yatest.common.canonical_file
        for f in os.listdir(self.dump_dir):
            canonical_files[f.replace(':', '/')] = yacf(os.path.join(self.dump_dir, f))

        result = {
            'files': canonical_files
        }
        return result

    def test_nothing_failed(self):
        for key, value in self.exit_codes.items():
            if value != 0:
                pytest.fail("An error occurred during test case execution: {} {:d}".format(key, value))
