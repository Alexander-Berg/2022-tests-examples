# -*- coding: utf-8 -*-
import os
import pytest
import tarfile

import yatest.common
import yatest_lib.ya

from balancer.test.util.config import BaseBalancerConfig
from balancer.test.util.fs import FileSystemManager
from balancer.test.util import settings as mod_settings


def __bundle_from_cmdline(test_tools, function_fs_manager):
    bundle = test_tools.get_tool(mod_settings.Tool(
        yatest_option_name='bundle',
    ))

    if bundle is None:
        return None

    if os.path.isdir(bundle):
        path = bundle
    else:
        with tarfile.open(bundle, 'r') as tf:
            tf.extractall(path=function_fs_manager.root_dir)
        path = function_fs_manager.abs_path('bundle')
    return FileSystemManager(path)


def __build_bundle(test_tools, settings, function_fs_manager, function_process_manager):
    namespace_path = settings.get_build_var('AWACS_NAMESPACE')
    l7heavy_project = settings.get_build_var('L7HEAVY_PROJECT')
    if namespace_path:
        try:
            awacsctl_bin = test_tools.get_tool(mod_settings.Tool(
                yatest_option_name='awacsctl2',
                yatest_path='infra/awacs/awacsctl2/bin/awacsctl2',
            ))
            bundle_path = function_fs_manager.create_dir('bundle-gen')
            res = function_process_manager.call([
                awacsctl_bin,
                'compile', yatest.common.source_path(namespace_path),
                '--templates-dir', yatest.common.source_path('infra/awacs/templates'),
                '--outdir', bundle_path,
            ])
            assert res.return_code == 0, res.stderr
            return FileSystemManager(bundle_path)
        except yatest_lib.ya.TestMisconfigurationException:
            return None
    elif l7heavy_project:
        bundle_path = function_fs_manager.create_dir('bundle-gen')
        lua_config_path = os.path.join(bundle_path, '{}.lua'.format(l7heavy_project))
        backends_json_path = os.path.join(bundle_path, 'backends.json')
        bin_path = yatest.common.binary_path('balancer/config/build/{}'.format(l7heavy_project))
        lua_file = open(lua_config_path, 'w')
        lua_file.write(open(os.path.join(bin_path, '{}.cfg'.format(l7heavy_project))).read())
        lua_file.close()
        backends_file = open(backends_json_path, 'w')
        backends_file.write(open(os.path.join(bin_path, 'backends.json')).read())
        backends_file.close()
        return FileSystemManager(bundle_path)
    return None


@pytest.fixture(scope='function')
def awacs_config_path_bundle(test_tools, settings, function_fs_manager, function_process_manager):
    bundle = __bundle_from_cmdline(test_tools, function_fs_manager)
    if bundle is None:
        return __build_bundle(test_tools, settings, function_fs_manager, function_process_manager)


@pytest.fixture(scope='function')
def awacs_config_path(test_tools, awacs_config_path_bundle, function_options):
    config_path = test_tools.get_tool(mod_settings.Tool(
        yatest_option_name='balancer_config',
    ))
    if config_path is None:
        assert awacs_config_path_bundle is not None, 'balancer config not found'
        assert hasattr(function_options, 'balancer_id'), 'balancer_id not specified'
        config_path = awacs_config_path_bundle.abs_path(function_options.balancer_id + '.lua')
    return config_path


class AwacsJsonConfig(BaseBalancerConfig):
    def __init__(self, path):
        super(AwacsJsonConfig, self).__init__()
        self.__path = path

    def get_path(self):
        return self.__path


@pytest.fixture(scope='function')
def awacs_parsed_config(awacs_config_path):
    conf = AwacsJsonConfig(awacs_config_path)
    return conf.as_json()
