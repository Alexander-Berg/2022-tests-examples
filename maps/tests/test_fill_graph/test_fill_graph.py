from maps.garden.sdk import test_utils

from maps.garden.libs.matrix_router_data_builder import source_module


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(source_module.fill_graph)
