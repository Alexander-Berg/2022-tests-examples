import pytest
import shutil
import tempfile

from yatest.common import build_path, source_path

import maps.analyzer.pylibs.test_tools as test_tools


__all__ = ['ytc']


@pytest.fixture(scope='session')
def local_cypress_dir():
    with tempfile.TemporaryDirectory() as directory:
        local_cypress_dir = directory + '/cypress'

        shutil.copytree(
            source_path('maps/analyzer/toolkit/tests/cypress'),
            local_cypress_dir
        )

        # The following files are dynamically generated and should be placed
        # into the target directory explicitly.
        for filename in ('regions_1', 'regions_2'):
            shutil.copy(
                build_path(
                    'maps/analyzer/toolkit/tests/cypress/route_lost_regionwise/' + filename
                ),
                local_cypress_dir + '/route_lost_regionwise'
            )

        yield local_cypress_dir


@pytest.fixture(scope='session')
def yt_stuff(local_cypress_dir):
    with test_tools.local_yt(local_cypress_dir=local_cypress_dir) as stuff:
        yield stuff


@pytest.fixture(scope='session')
def ytc(yt_stuff):
    with test_tools.local_ytc(proxy=yt_stuff.get_server()) as ctx:
        yield ctx
