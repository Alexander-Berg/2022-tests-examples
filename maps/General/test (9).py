import unittest

from six.moves import zip_longest

from yatest.common import source_path

from yandex.maps.jams.level3 import create_from_file, traffic_state
from maps.routing.navigator_stopwatch.python import Stopwatch
from yandex.maps.jams.router import Jams
from maps.routing.turn_penalties.python import TurnPenalties
from yandex.maps.jams.graph4 import Graph, SegmentsRTree


TEST_GRAPH_DIR = "/usr/share/yandex/maps/test-graph3/"
TEST_GRAPH_TOPOLOGY = TEST_GRAPH_DIR + "topology.mms.2"
TEST_GRAPH_DATA = TEST_GRAPH_DIR + "data.mms.2"
TEST_GRAPH_RTREE = TEST_GRAPH_DIR + "segments_rtree.mms.2"
TEST_GRAPH_PENALTIES = TEST_GRAPH_DIR + "turn_penalties.mms.2"
ROUTES = create_from_file(source_path('maps/jams/levels/tests/routes.xml'))


def cmp_routes(routes_1, routes_2):
    for r1, r2 in zip_longest(routes_1, routes_2):
        if r1.route_id != r2.route_id:
            return False
        if r1.region_id != r2.region_id:
            return False
        if abs(r1.weight-r2.weight) > 1e-14:
            return False
        if r1.streets != r2.streets:
            return False
        if r1.geometry != r2.geometry:
            return False

    return True


class TestLib(unittest.TestCase):
    def test_routes(self):
        self.assertEqual(ROUTES.size, 45)
        self.assertEqual(ROUTES.count(213), 45)

        self.assertEqual(len(ROUTES.regions()), 1)
        self.assertEqual(ROUTES.regions()[0], 213)

        route = next(iter(ROUTES))
        self.assertEqual(route.route_id, 1)
        self.assertEqual(route.region_id, 213)
        self.assertAlmostEqual(route.weight, 1.0)
        self.assertEqual(route.streets, "ТТК")
        self.assertEqual(route.geometry.points_number, 870)

    # FIXME: repair test
    def _test_average_slowdown(self):
        graph = Graph(TEST_GRAPH_TOPOLOGY, TEST_GRAPH_DATA)
        rtree = SegmentsRTree(TEST_GRAPH_RTREE)
        penalties = TurnPenalties(graph, TEST_GRAPH_PENALTIES)
        assert(graph.version == "3.0.0-0")
        assert(rtree.version == "3.0.0-0")
        assert(penalties.version == "3.0.0-0")

        stopwatch = Stopwatch(graph, rtree, penalties)
        self.assertEqual(stopwatch.graph_version, "3.0.0-0")
        stopwatch.reload_jams(Jams("./genfiles/jams.pb"))
        self.assertEqual(stopwatch.jams_timestamp, 1342012336)

        state = traffic_state(stopwatch, graph, ROUTES, 213)

        self.assertAlmostEqual(int(state.length), 305256, places=-2)
        self.assertAlmostEqual(int(state.jams_length), 190929, places=-3)
        self.assertAlmostEqual(int(state.time), 23630, places=-3)
        self.assertAlmostEqual(int(state.jams_time), 18619, places=-3)
        self.assertAlmostEqual(int(state.unknown_time), 8478, places=-3)

        self.assertAlmostEqual(
            state.average_slowdown(threshold=0.3), 1.22, places=2)
        self.assertAlmostEqual(state.average_slowdown(), 1.22, places=2)
