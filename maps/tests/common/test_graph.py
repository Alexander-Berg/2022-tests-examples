from maps.garden.sdk import test_utils
from maps.garden.modules.ymapsdf.lib import ymapsdf


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(ymapsdf.fill_graph)
