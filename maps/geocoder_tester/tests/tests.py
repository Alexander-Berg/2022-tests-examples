from maps.garden.sdk import test_utils
from maps.garden.modules.geocoder_tester.lib.geocoder_index_tester import fill_graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(
        lambda graph_builder, regions: fill_graph(graph_builder))
