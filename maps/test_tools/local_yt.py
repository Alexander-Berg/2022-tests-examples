from contextlib import contextmanager
from typing import Optional

from yatest.common import source_path

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

from maps.pylibs.yt.lib import YtContext

import maps.analyzer.pylibs.envkit as kit
from maps.pylibs.yt.lib.unwrap_yt_error import unwrap_yt_error

from .test_graph import test_graph


@contextmanager
def local_yt(local_cypress_dir: Optional[str] = None, **kwargs) -> YtStuff:
    """
    Start local YT and return initialized `YtStuff`

    Args:
        * local_cypress_dir - optional local cypress directory
    """
    kwargs.setdefault('node_chunk_store_quota', 16 * 1024**3)

    yt = YtStuff(YtConfig(
        local_cypress_dir=source_path(local_cypress_dir),
        **kwargs
    ))
    try:
        yt.start_local_yt()
        kit.config.YT_PROXY = yt.get_server()
        kit.config.YAMAPS_POOL = False
        kit.config.YAMAPS_TAG = False
        kit.yt.DEFAULT_ACL = []
        kit.yt.YAMAPS_TAG_SPEC = {}
    except Exception as e:
        yt.yt_local_err.seek(0)
        stderr = yt.yt_local_err.read()
        raise RuntimeError('Unable to start local yt with error: {}\nstderr: {}'.format(e, stderr))
    try:
        with unwrap_yt_error():
            yield yt
    finally:
        yt.stop_local_yt()


@contextmanager
def local_ytc(
    pkg_root: Optional[str] = None,  # [DEPRECATED], switch to bundles, MAPSJAMS-3693
    graph_version: Optional[str] = None,
    local_cypress_dir: Optional[str] = None,
    proxy: Optional[str] = None,
) -> YtContext:
    """
    Initialize toolkit with local YT

    Args:
        * pkg_root - package root where toolkit located, see `pkg` target of toolkit for example
        * graph_version - default graph version
        * local_cypress_dir - local cypress
        * proxy of existent local yt instance
            NOTE: If you want local YT + local YQL, you have to define `yt_stuff` fixture using `local_yt` (optionally pass `local_cypress_dir` there)
            and pass its `yt_stuff.get_server()` here as `proxy` arg
            Otherwise YQL will initialize its own local YT

    Returns:
        Local YT context like toolkit's `get_context()`
    """
    with test_graph(pkg_root=pkg_root, graph_version=graph_version):
        if proxy is not None:
            with kit.get_context(proxy=proxy) as ctx:
                yield ctx
        else:
            with local_yt(local_cypress_dir=local_cypress_dir) as yt:
                with kit.get_context(proxy=yt.get_server()) as ctx:
                    yield ctx
