import pytest

import maps.analyzer.pylibs.test_tools as test_tools


__all__ = ['ytc']


@pytest.fixture(scope='session', autouse=True)
def auto(request, geobase):
    yield


@pytest.fixture(scope='session')
def ytc(request):
    with test_tools.local_ytc(
        graph_version='graph3',
        local_cypress_dir='maps/analyzer/toolkit/tests/cypress',
    ) as ctx:
        test_tools.init_upload_geoid(ctx)
        test_tools.init_upload_geobase(ctx)
        yield ctx
