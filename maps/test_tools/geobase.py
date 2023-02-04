import os.path
import tarfile

from yatest.common import build_path
import pytest

from maps.pylibs.yt.lib import YtContext

import maps.analyzer.pylibs.envkit.config as config


@pytest.fixture(scope='session')
def geobase(request):
    """
    ! Deprecated !
    Use `init_upload_geobase` with `tzdata`/`tzdata_small` instead
    """
    config.DEFAULT_GEOBASE_TZ_PATH = build_path('maps/data/test/geobase/zones_bin')
    config.DEFAULT_GEOBASE_BIN_PATH = build_path('maps/data/test/geobase/geodata5.bin')
    yield


def init_upload_geobase(ctx: YtContext) -> None:
    config.YT_DEFAULT_GEOBASE_BIN_PATH = '//data/geobase/geodata5.bin'
    if not ctx.exists(config.YT_DEFAULT_GEOBASE_BIN_PATH):
        ctx.smart_upload_file(
            filename=build_path('maps/data/test/geobase/geodata5.bin'),
            destination=config.YT_DEFAULT_GEOBASE_BIN_PATH,
            placement_strategy='ignore',
        )


@pytest.fixture(scope='session')
def tzdata(request):
    """
    Uses `maps/data/test/tzdata` instead of `maps/data/test/geobase` as it contains archive with all timezones
    Use it when you need all timezones

    NOTE: Fixtures are expanded in left-to-right order, so if you need both `tzdata` and `geobase`
    place `tzdata` argument second to `geobase` to overwrite path set by it
    """
    TZDATA_DIR = build_path('maps/data/test/tzdata/tzdata')

    with tarfile.open(build_path('maps/data/test/tzdata/tzdata.tar.gz'), 'r:gz') as tar:
        tar.extractall(TZDATA_DIR)

    config.DEFAULT_GEOBASE_TZ_PATH = os.path.join(TZDATA_DIR, 'zones_bin')
    config.YT_DEFAULT_GEOBASE_TZ_PATH = config.DEFAULT_GEOBASE_TZ_PATH
    yield


@pytest.fixture(scope='session')
def tzdata_small(request):
    config.DEFAULT_GEOBASE_TZ_PATH = build_path('maps/data/test/geobase/zones_bin')
    config.YT_DEFAULT_GEOBASE_TZ_PATH = config.DEFAULT_GEOBASE_TZ_PATH
    yield
