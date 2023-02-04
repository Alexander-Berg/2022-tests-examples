# -*- coding: utf-8 -*-
import json
import os
import pytest

from library.python import resource

from balancer.test.util import settings as mod_settings

from balancer.test.util.fs import FileSystemManager, abs_path


__TEST_TOOLS_OPTION = 'test_tools_dir'


def pytest_addoption(parser):
    parser.addoption('--sandbox', action='store_true', default=False, help='run in sandbox context')

    test_tools = parser.getgroup('test_tools', description='test tools')
    test_tools.addoption('--test_tools_dir', dest=__TEST_TOOLS_OPTION, default=None,
                         help='path to directory with test tools')


def pytest_configure(config):
    config.root_cwd = os.getcwd()


@pytest.fixture(scope='session')
def settings(request):
    return mod_settings.Settings(
        request=request,
        sandbox=request.config.getoption('sandbox'),
        yatest=mod_settings.YATEST,
        root_fs=FileSystemManager(request.config.root_cwd),
        build_vars=json.loads(resource.find('/balancer/test/plugin/build_vars.json')),
    )


@pytest.fixture(scope='session')
def test_tools(settings, request):
    test_tools_dir = settings.get_param(__TEST_TOOLS_OPTION)
    if test_tools_dir is not None:
        test_tools = FileSystemManager(abs_path(test_tools_dir, cwd=request.config.root_cwd))
    else:
        test_tools = None
    return mod_settings.TestTools(
        tools_fs=test_tools,
        request_config=request.config,
        settings=settings,
    )
