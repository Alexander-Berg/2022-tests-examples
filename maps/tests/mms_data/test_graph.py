from maps.garden.sdk import test_utils
from maps.garden.modules.masstransit_predictor_data.lib.mms_data import graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(graph.fill_graph)
