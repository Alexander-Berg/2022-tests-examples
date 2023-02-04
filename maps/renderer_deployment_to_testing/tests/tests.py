from maps.garden.sdk import test_utils
from maps.garden.modules.renderer_deployment_to_testing.lib.graph import fill_graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(fill_graph)
