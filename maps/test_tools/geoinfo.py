from yatest.common import build_path
from yatest.common.runtime import work_path
import pytest

from maps.pylibs.yt.lib import YtContext

import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.envkit.config as config
import maps.analyzer.pylibs.envkit.data as data
import maps.analyzer.pylibs.envkit.paths as paths
import maps.analyzer.pylibs.geoinfo as GeoInfo

from .geoid import init_upload_geoid
from .geobase import init_upload_geobase

GEOINFO_DEST = '//data/geoinfo/libgeoinfo_udf.so'


@pytest.fixture(scope='session')
def geoinfo(request, yt_stuff, geobase):
    server = yt_stuff.get_server()

    LOCAL_YQL_ATTACH_PREFIX = 'http://{server}/api/v3/read_file?disposition=attachment&path='.format(server=server)

    _init_geoinfo(envkit.get_context(proxy=server))

    paths.update(
        data.YqlPaths,
        {
            'COVERAGE_URL': LOCAL_YQL_ATTACH_PREFIX + config.YT_DEFAULT_GEOID_PATH,
            'GEOBASE_URL': LOCAL_YQL_ATTACH_PREFIX + config.YT_DEFAULT_GEOBASE_BIN_PATH,
            'GEOINFO_URL': LOCAL_YQL_ATTACH_PREFIX + GEOINFO_DEST,
        },
    )

    yield _get_geoinfo()


def _get_geoinfo() -> GeoInfo.PyGeoInfo:
    return GeoInfo.PyGeoInfo(
        build_path('maps/data/test/geoid/geoid.mms.1'),
        config.DEFAULT_GEOBASE_BIN_PATH,
    )


def _init_geoinfo(ctx: YtContext) -> None:
    init_upload_geoid(ctx)
    init_upload_geobase(ctx)
    ctx.smart_upload_file(
        filename=work_path('libgeoinfo_udf.so'),
        destination=GEOINFO_DEST,
        placement_strategy='ignore',
    )
