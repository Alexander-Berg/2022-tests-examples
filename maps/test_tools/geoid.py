from yatest.common import build_path

from maps.pylibs.yt.lib import YtContext

import maps.analyzer.pylibs.envkit.config as config


def init_upload_geoid(ctx: YtContext) -> None:
    """
    Init YT geoid path and also upload it
    """
    config.YT_DEFAULT_GEOID_PATH = '//data/geoid/geoid.mms.1'
    if not ctx.exists(config.YT_DEFAULT_GEOID_PATH):
        ctx.smart_upload_file(
            filename=build_path('maps/data/test/geoid/geoid.mms.1'),
            destination=config.YT_DEFAULT_GEOID_PATH,
            placement_strategy='ignore',
        )
