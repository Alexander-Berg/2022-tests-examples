import pytest

import maps.analyzer.pylibs.test_tools as test_tools


__all__ = ['ytc']


# for YQL
@pytest.fixture(scope='session')
def yt_stuff(request):
    with test_tools.local_yt(local_cypress_dir='maps/analyzer/toolkit/tests/cypress') as stuff:
        yield stuff


@pytest.fixture(scope='session')
def ytc(request, yt_stuff):
    with test_tools.local_ytc(
        graph_version='graph3',
        proxy=yt_stuff.get_server(),
    ) as ctx:
        test_tools.init_upload_geoid(ctx)
        yield ctx
