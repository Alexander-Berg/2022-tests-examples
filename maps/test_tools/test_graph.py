from contextlib import contextmanager
from typing import Optional

from yatest.common import build_path

import maps.pylibs.yt.lib.graph as yt_graph
import maps.analyzer.pylibs.envkit.graph as env_graph
import maps.analyzer.pylibs.envkit.config as env_config


@contextmanager
def test_graph(
    pkg_root: Optional[str] = None,  # [DEPRECATED], switch to bundles, MAPSJAMS-3693
    graph_version: Optional[str] = None,
):
    """
    Initialize test graph root

    Args:
        * graph_version - default graph version
    """
    yt_graph.set_local_graph_root(build_path('maps/data/test'))
    if pkg_root is not None:
        env_config.ROOT = build_path(pkg_root)
    env_config.USE_TMPFS = False
    env_config.MLOCK = False
    env_graph.VERSION = graph_version or 'graph3'
    yield
