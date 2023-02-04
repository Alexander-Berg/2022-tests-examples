# -*- coding: utf-8 -*-
import os
import pytest
from balancer.test.util import settings
from balancer.test.util import multiscope
from balancer.test.util.dolbilo import Dexecutor


pytest_plugins = [
    'balancer.test.plugin.settings',
    'balancer.test.plugin.fs',
    'balancer.test.plugin.logger',
]


# __DPLANNER = settings.Tool(
#     pytest_option_name='dplanner',
#     tool_file_name='d-planner',
#     yatest_option_name='dplanner',
#     yatest_path='tools/dolbilo/planner/d-planner'
# )


__DEXECUTOR = settings.Tool(
    pytest_option_name='dexecutor',
    tool_file_name='d-executor',
    yatest_option_name='dexecutor',
    yatest_path='tools/dolbilo/executor/d-executor'
)


def pytest_addoption(parser):
    test_tools = parser.getgroup('test_tools')
    # test_tools.addoption('--dplanner', dest=__DPLANNER.pytest_option_name, action='store', help='path to d-planner executable')
    test_tools.addoption('--dexecutor', dest=__DEXECUTOR.pytest_option_name, action='store', help='path to d-executor executable')


# @pytest.fixture(scope='session')
# def dplanner_path(test_tools):
#     return test_tools.get_tool(__DPLANNER)


@pytest.fixture(scope='session')
def dexecutor_path(test_tools):
    return test_tools.get_tool(__DEXECUTOR)


@multiscope.fixture(pytest_fixtures=['dexecutor_path', 'logger'])
def dexecutor(fs_manager, dexecutor_path, logger):
    out_path = fs_manager.create_file('dexecutor.out')
    plan_path = settings.get_data(
        py_path=os.path.abspath(os.path.join(os.path.dirname(__file__), 'data', 'plan')),
        ya_path='balancer/test/plugin/dolbilo/data/plan',
    )
    return Dexecutor(dexecutor_path, logger, out_path, plan_path)
