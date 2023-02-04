from maps.garden.sdk import test_utils
from maps.garden.modules.pedestrian_tester.lib import pedestrian_validation


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(pedestrian_validation.fill_graph)
