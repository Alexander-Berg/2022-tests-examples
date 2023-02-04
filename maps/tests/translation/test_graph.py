from maps.garden.sdk import test_utils
from maps.garden.modules.ymapsdf.lib.translation import translate as translation


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(translation.fill_graph)
