from maps.garden.sdk import test_utils
from maps.garden.modules.geocoder_indexer.lib.geocoder_indexer import fill_graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(fill_graph)
