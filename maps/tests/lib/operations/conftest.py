import pytest

from yt.wrapper import ypath_split

import maps.analyzer.pylibs.envkit.graph as graph
import maps.analyzer.pylibs.test_tools as test_tools

import maps.analyzer.toolkit.lib as tk


__all__ = ['ytc']


# for YQL
@pytest.fixture(scope='session')
def yt_stuff(request):
    with test_tools.local_yt(local_cypress_dir='maps/analyzer/toolkit/tests/cypress') as stuff:
        yield stuff


@pytest.fixture(scope='session', autouse=True)
def auto(request, geobase, calendar):
    yield


@pytest.fixture(scope='module')
def graph4(request):
    graph.VERSION = 'graph4'
    yield
    graph.VERSION = 'graph3'


@pytest.fixture(scope='session')
def ytc(request, yt_stuff):
    with test_tools.local_ytc(
        graph_version='graph3',
        proxy=yt_stuff.get_server(),
    ) as ctx:
        test_tools.init_upload_geoid(ctx)
        yield ctx


@pytest.fixture(scope='session')
def schematize_match_signals(ytc):
    for t in ytc.search('//match_signals', node_type='table', path_filter=lambda s: 'signals' in ypath_split(s)[1]):
        test_tools.schematize_table(ytc, t, hints=tk.schema.SIGNALS_TABLE.columns)
    for t in ytc.search('//match_signals', node_type='table', path_filter=lambda s: 'travel_times' in ypath_split(s)[1]):
        test_tools.schematize_table(ytc, t, hints=tk.schema.TRAVEL_TIMES_TABLE.columns)
