from maps.garden.sdk import test_utils
from maps.garden.modules.ymapsdf_osm.lib import ymapsdf_osm


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(ymapsdf_osm.fill_graph)
