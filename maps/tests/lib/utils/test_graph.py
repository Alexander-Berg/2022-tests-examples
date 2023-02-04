import os

import maps.analyzer.pylibs.envkit.graph as graph


def test_graph_paths():
    os.environ.update(
        ALZ_API_GRAPH_VERSION='version',
        ALZ_API_LOCAL_GRAPH_ROOT='/local',
        ALZ_API_SHARED_GRAPH_ROOT='//shared',
    )
    graph.reload()
    assert graph.local_graph_path('data') == '/local/version/data'
    assert graph.cypress_graph_path('data') == '//shared/version/data'
