import os
import pytest

import maps.analyzer.pylibs.envkit.config as config
import maps.analyzer.pylibs.test_tools as test_tools
import maps.analyzer.pylibs.yql.config as yql_config

from yatest.common import build_path, output_path


@pytest.fixture(scope='session', autouse=True)
def geoid(request):
    config.DEFAULT_COVERAGE_PATH = build_path('maps/data/test/geoid/geoid.mms.1')


@pytest.fixture(scope='module', autouse=True)
def yql_defaults(request, yql_api):
    os.environ["ALZ_API_YQL_TOKEN"] = "test"
    config.SVN_REVISION = "test"
    yql_config.DEFAULT_YQL_SERVER = 'localhost'
    yql_config.DEFAULT_YQL_PORT = yql_api.port


# for YQL
@pytest.fixture(scope='session')
def yt_stuff(request):
    local_cypress_dir = output_path('cypress')
    if not os.path.exists(local_cypress_dir):
        os.makedirs(local_cypress_dir)
    with test_tools.local_yt(local_cypress_dir=local_cypress_dir) as stuff:
        yield stuff


@pytest.fixture(scope='session')
def ytc(request, yt_stuff):
    with test_tools.local_ytc(
        graph_version='graph3',
        proxy=yt_stuff.get_server()
    ) as ctx:
        required_graph_files = [
            'road_graph.fb',
            'rtree.fb',
            'edges_persistent_index.fb'
        ]

        for file_name in required_graph_files:
            ctx.smart_upload_file(
                filename=build_path('maps/data/test/graph3/{}'.format(file_name)),
                destination='//home/maps/graph/latest/{}'.format(file_name),
                placement_strategy="ignore"
            )

        yield ctx
