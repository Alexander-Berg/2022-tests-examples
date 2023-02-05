import networkx as nx

import maps.garden.sandbox.server_autotests.lib.constants as const
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester


def _build_graph(modules: list[dict]) -> nx.DiGraph:
    g = nx.DiGraph()
    for module in modules:
        if "sources" in module:
            for parent in module["sources"]:
                g.add_edge(parent, module["name"])
        if "trigger_by" in module:
            for parent in module["trigger_by"]:
                g.add_edge(parent, module["name"])
    return g


def _is_subgraph(subgraph: nx.DiGraph, full_graph: nx.DiGraph):
    for edge in subgraph.edges:
        if edge not in full_graph.edges:
            return False
    return True


def _get_graph_modules(tester: ServerTester, module: str = ""):
    params = {
        "contour": tester.contour_name
    }
    if module:
        params["module"] = module
    return tester.get(const.MODULES_MODULE_GRAPH, params).json()


@test_case
def test_get_module_graph(tester: ServerTester):
    full_graph_modules = _get_graph_modules(tester)
    subgraph_modules = _get_graph_modules(tester, const.EXAMPLE_MAP)
    full_graph = _build_graph(full_graph_modules)
    subgraph = _build_graph(subgraph_modules)
    assert full_graph.size() > subgraph.size() > 0
    assert const.EXAMPLE_MAP in subgraph.nodes
    assert _is_subgraph(subgraph, full_graph)
