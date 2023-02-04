from maps.garden.sdk import test_utils
from maps.garden.modules.renderer_design_testing.lib.graph import fill_graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(fill_graph)
