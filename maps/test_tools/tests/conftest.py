import os
import pytest

import maps.analyzer.pylibs.test_tools as test_tools


TEST_ROOT = 'maps/analyzer/pylibs/test_tools/tests'


def path_to(local_path):
    return os.path.join(TEST_ROOT, local_path)


@pytest.fixture(scope='session')
def yt_stuff(request):
    with test_tools.local_yt(local_cypress_dir=path_to('cypress')) as stuff:
        yield stuff


@pytest.fixture(scope='session')
def ytc(request, yt_stuff):
    with test_tools.local_ytc(proxy=yt_stuff.get_server()) as ctx:
        yield ctx
