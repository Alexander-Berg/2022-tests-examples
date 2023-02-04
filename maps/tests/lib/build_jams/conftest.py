import pytest

from maps.analyzer.pylibs.test_tools.schema import schematize_table
import maps.analyzer.pylibs.test_tools as test_tools

import maps.analyzer.toolkit.lib as tk


__all__ = ['ytc']


@pytest.fixture(scope='session', autouse=True)
def auto(request, tzdata):
    yield


@pytest.fixture(scope='session')
def ytc(request):
    with test_tools.local_ytc(
        graph_version='graph3',
        local_cypress_dir='maps/analyzer/toolkit/tests/cypress',
    ) as ctx:
        test_tools.init_upload_geoid(ctx)
        test_tools.init_upload_geobase(ctx)
        for t in ctx.search('//build_historic_jams', node_type='table', path_filter=lambda s: s.endswith('.in')):
            schematize_table(ctx, t, schema=tk.schema.TRAVEL_TIMES_TABLE)
        yield ctx
