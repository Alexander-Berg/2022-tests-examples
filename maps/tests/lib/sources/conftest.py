import pytest

from maps.analyzer.pylibs.test_tools.schema import schematize_table
import maps.analyzer.pylibs.test_tools as test_tools
import maps.analyzer.toolkit.lib as tk


__all__ = ['ytc']


@pytest.fixture(scope='session')
def ytc(request):
    with test_tools.local_ytc(
        graph_version='graph3',
        local_cypress_dir='maps/analyzer/toolkit/tests/cypress',
    ) as ctx:
        test_tools.init_upload_geoid(ctx)
        yield ctx


@pytest.fixture(scope='session')
def schematize(request, ytc):
    for t in ytc.search('//sources/cache/signals', node_type='table'):
        schematize_table(ytc, t, schema=tk.schema.SIGNALS_EXTRA_TABLE)
    for t in ytc.search('//sources/cache/travel_times', node_type='table'):
        schematize_table(ytc, t, schema=tk.schema.TRAVEL_TIMES_TABLE)


@pytest.fixture(scope='module')
def sources_fixture(request):
    cache_travel_times = tk.paths.Caches.TRAVEL_TIMES_CACHE.path
    cache_signals = tk.paths.Caches.SIGNALS_CACHE.path
    dispatcher_logs = tk.paths.Common.SIGNALS_LOGS.path

    tk.paths.Caches.TRAVEL_TIMES_CACHE.path = '//sources/cache/travel_times'
    tk.paths.Caches.SIGNALS_CACHE.path = '//sources/cache/signals'
    tk.paths.Common.SIGNALS_LOGS.path = '//sources/logs'
    yield
    tk.paths.Caches.TRAVEL_TIMES_CACHE.path = cache_travel_times
    tk.paths.Caches.SIGNALS_CACHE.path = cache_signals
    tk.paths.Common.SIGNALS_LOGS.path = dispatcher_logs
