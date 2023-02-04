from maps.garden.sdk import test_utils
from maps.analyzer.services.jams_external.garden.modules.here_tmc_locations_bundle.lib import graph


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(graph.fill_graph)
