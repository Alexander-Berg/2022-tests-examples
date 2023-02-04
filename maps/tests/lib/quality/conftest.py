import os

from yatest.common import work_path
import pytest

from maps.analyzer.pylibs.test_tools.schema import schematize_table
from maps.analyzer.toolkit.lib.schema import REGION_ID, SPEED_LIMIT, MATCH_TIME
from maps.analyzer.toolkit.lib.quality.schema import JAMS, JAMS_LAST_SIGNAL_TIME, JAMS_SIGNALS_ACCOUNTED, JAMS_CREATE_TIME
import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.test_tools as test_tools


__all__ = ['ytc']

os.environ["ALZ_API_YQL_TOKEN"] = "test"


# for YQL
@pytest.fixture(scope='session')
def yt_stuff(request):
    with test_tools.local_yt(local_cypress_dir='maps/analyzer/toolkit/tests/cypress') as stuff:
        yield stuff


@pytest.fixture(scope='module', autouse=True)
def yql_udf(request, geobase, yql_defaults, ytc):
    os.symlink(
        envkit.config.DEFAULT_GEOBASE_BIN_PATH,
        os.path.join(work_path(), 'geodata5.bin'),
    )  # for YQL Geo UDF


@pytest.fixture(scope='session', autouse=True)
def auto(request, calendar):
    yield


@pytest.fixture(scope='session')
def ytc(request, yt_stuff):
    with test_tools.local_ytc(
        graph_version='graph3',
        proxy=yt_stuff.get_server(),
    ) as ctx:
        test_tools.init_upload_geobase(ctx)
        test_tools.init_upload_calendar(ctx)
        for t in ctx.search('//join_travel_times', node_type='table'):
            schematize_table(ctx, t)
        for t in ctx.search('//evaluate_quality', node_type='table'):
            schematize_table(ctx, t, hints=[REGION_ID, SPEED_LIMIT, MATCH_TIME])
        for t in ctx.search('//calculate_metrics', node_type='table'):
            schematize_table(ctx, t)
        for t in ctx.search('//stats_with_stopwatch', node_type='table'):
            schematize_table(ctx, t, hints=[JAMS, JAMS_LAST_SIGNAL_TIME, JAMS_SIGNALS_ACCOUNTED, JAMS_CREATE_TIME])
        yield ctx
