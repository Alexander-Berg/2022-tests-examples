import os

import pytest

import maps.analyzer.pylibs.yql.config as yql_config


@pytest.fixture(scope='module', autouse=True)
def yql_defaults(request):
    try:
        yql_api = request.getfixturevalue('yql_api')
        os.environ['ALZ_API_YQL_TOKEN'] = 'test'
        yql_config.DEFAULT_YQL_SERVER = 'localhost'
        yql_config.DEFAULT_YQL_PORT = yql_api.port
    except BaseException:
        pass
